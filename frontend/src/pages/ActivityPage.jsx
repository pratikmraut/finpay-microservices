import { Download, Search } from "lucide-react";
import { useMemo, useState } from "react";
import { ErrorState, SectionLoading } from "../components/LoadingState";
import { TransactionTable } from "../components/TransactionTable";
import { useTransactionsQuery, useWalletQuery } from "../hooks/useFinPayData";
import { paymentDirection } from "../utils/format";

const filters = ["all", "incoming", "outgoing"];

export function ActivityPage() {
  const wallet = useWalletQuery().data;
  const transactionsQuery = useTransactionsQuery(wallet.walletId);
  const [direction, setDirection] = useState("all");
  const [status, setStatus] = useState("all");
  const [search, setSearch] = useState("");

  const transactions = useMemo(() => (transactionsQuery.data || []).filter((transaction) => {
    const matchesDirection = direction === "all" || paymentDirection(transaction, wallet.walletId) === direction;
    const matchesStatus = status === "all" || transaction.status === status;
    const needle = search.trim().toLowerCase();
    const matchesSearch = !needle || transaction.paymentReference.toLowerCase().includes(needle)
      || String(transaction.senderWalletId).includes(needle)
      || String(transaction.receiverWalletId).includes(needle);
    return matchesDirection && matchesStatus && matchesSearch;
  }), [transactionsQuery.data, direction, status, search, wallet.walletId]);

  function exportCsv() {
    const header = "paymentReference,senderWalletId,receiverWalletId,amount,currency,status,createdAt";
    const rows = transactions.map((item) => [item.paymentReference, item.senderWalletId, item.receiverWalletId, item.amount, item.currency, item.status, item.createdAt].join(","));
    const blob = new Blob([[header, ...rows].join("\n")], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "finpay-transactions.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <section className="panel activity-panel">
      <header className="activity-toolbar">
        <div className="segmented-control" aria-label="Transaction direction">
          {filters.map((filter) => <button type="button" key={filter} className={direction === filter ? "active" : ""} onClick={() => setDirection(filter)}>{filter[0].toUpperCase() + filter.slice(1)}</button>)}
        </div>
        <div className="activity-toolbar__right">
          <label className="search-field">
            <Search size={17} />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search reference or wallet" aria-label="Search transactions" />
          </label>
          <select className="select-control" value={status} onChange={(event) => setStatus(event.target.value)} aria-label="Filter by status">
            <option value="all">All statuses</option>
            <option value="SUCCESS">Completed</option>
            <option value="PENDING">Pending</option>
            <option value="FAILED">Failed</option>
            <option value="COMPENSATED">Reversed</option>
          </select>
          <button className="icon-button" type="button" onClick={exportCsv} disabled={!transactions.length} aria-label="Download transactions as CSV" title="Download CSV"><Download size={18} /></button>
        </div>
      </header>
      <div className="activity-count"><strong>{transactions.length}</strong> {transactions.length === 1 ? "transaction" : "transactions"}</div>
      {transactionsQuery.isLoading ? <SectionLoading label="Loading activity" /> : transactionsQuery.error ? (
        <ErrorState message={transactionsQuery.error.message} onRetry={transactionsQuery.refetch} />
      ) : <TransactionTable transactions={transactions} walletId={wallet.walletId} />}
    </section>
  );
}
