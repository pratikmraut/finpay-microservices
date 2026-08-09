import { ArrowDownLeft, ArrowUpRight, ReceiptText } from "lucide-react";
import { Link } from "react-router-dom";
import { formatDateTime, formatMoney, paymentDirection, shortReference } from "../utils/format";
import { StatusBadge } from "./StatusBadge";

export function TransactionTable({ transactions = [], walletId, limit, showAllLink = false }) {
  const rows = limit ? transactions.slice(0, limit) : transactions;

  if (!rows.length) {
    return (
      <div className="empty-state">
        <span className="empty-state__icon"><ReceiptText size={24} aria-hidden="true" /></span>
        <strong>No transactions yet</strong>
        <p>Your completed and pending transfers will appear here.</p>
        <Link className="button button--primary button--small" to="/send">Make a transfer</Link>
      </div>
    );
  }

  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th>Transaction</th>
            <th>Counterparty</th>
            <th>Status</th>
            <th className="align-right">Amount</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((transaction) => {
            const direction = paymentDirection(transaction, walletId);
            const incoming = direction === "incoming";
            const counterparty = incoming ? transaction.senderWalletId : transaction.receiverWalletId;
            return (
              <tr key={transaction.paymentReference}>
                <td data-label="Transaction">
                  <div className="transaction-cell">
                    <span className={`transaction-icon transaction-icon--${direction}`}>
                      {incoming ? <ArrowDownLeft size={18} /> : <ArrowUpRight size={18} />}
                    </span>
                    <div>
                      <strong>{incoming ? "Money received" : "Money sent"}</strong>
                      <span title={transaction.paymentReference}>{shortReference(transaction.paymentReference)}</span>
                    </div>
                  </div>
                </td>
                <td data-label="Counterparty">
                  <strong>Wallet #{counterparty}</strong>
                  <span>{formatDateTime(transaction.createdAt)}</span>
                </td>
                <td data-label="Status"><StatusBadge status={transaction.status} /></td>
                <td className={`align-right amount amount--${direction}`} data-label="Amount">
                  {incoming ? "+" : "-"}{formatMoney(transaction.amount, transaction.currency)}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      {showAllLink && transactions.length > rows.length && (
        <div className="table-footer"><Link to="/activity">View all transactions</Link></div>
      )}
    </div>
  );
}
