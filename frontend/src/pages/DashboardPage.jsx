import {
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  Bell,
  Copy,
  Eye,
  EyeOff,
  RefreshCw,
  SendHorizontal,
  WalletCards,
} from "lucide-react";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { CopyButton } from "../components/CopyButton";
import { ErrorState, SectionLoading } from "../components/LoadingState";
import { StatusBadge } from "../components/StatusBadge";
import { TransactionTable } from "../components/TransactionTable";
import { useNotificationsQuery, useProfileQuery, useTransactionsQuery, useWalletQuery } from "../hooks/useFinPayData";
import { formatMoney, formatRelativeTime, paymentDirection } from "../utils/format";

function localDateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function buildChartData(transactions, walletId) {
  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date();
    date.setHours(0, 0, 0, 0);
    date.setDate(date.getDate() - (6 - index));
    return {
      key: localDateKey(date),
      label: new Intl.DateTimeFormat("en", { weekday: "short" }).format(date),
      incoming: 0,
      outgoing: 0,
    };
  });

  const byDay = new Map(days.map((day) => [day.key, day]));
  transactions.filter((item) => item.status === "SUCCESS").forEach((item) => {
    const day = byDay.get(localDateKey(new Date(item.createdAt)));
    if (!day) return;
    const direction = paymentDirection(item, walletId);
    day[direction] += Number(item.amount);
  });
  return days;
}

function ChartTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="chart-tooltip">
      <strong>{label}</strong>
      {payload.map((entry) => <span key={entry.dataKey} style={{ color: entry.color }}>{entry.name}: {formatMoney(entry.value)}</span>)}
    </div>
  );
}

export function DashboardPage() {
  const [balanceVisible, setBalanceVisible] = useState(true);
  const walletQuery = useWalletQuery();
  const profileQuery = useProfileQuery();
  const wallet = walletQuery.data;
  const transactionsQuery = useTransactionsQuery(wallet?.walletId);
  const notificationsQuery = useNotificationsQuery();
  const transactions = transactionsQuery.data || [];

  const metrics = useMemo(() => {
    return transactions.reduce((result, transaction) => {
      if (transaction.status !== "SUCCESS") return result;
      const direction = paymentDirection(transaction, wallet?.walletId);
      result[direction] += Number(transaction.amount);
      return result;
    }, { incoming: 0, outgoing: 0 });
  }, [transactions, wallet?.walletId]);

  const chartData = useMemo(() => buildChartData(transactions, wallet?.walletId), [transactions, wallet?.walletId]);
  const notifications = useMemo(() => (notificationsQuery.data || [])
    .filter((item) => Number(item.walletId) === Number(wallet?.walletId))
    .slice(0, 4), [notificationsQuery.data, wallet?.walletId]);

  if (walletQuery.isLoading) return <SectionLoading label="Loading wallet" />;
  if (walletQuery.error) return <ErrorState message={walletQuery.error.message} onRetry={walletQuery.refetch} />;

  return (
    <div className="dashboard-page">
      <section className="welcome-row">
        <div>
          <span className="eyebrow">{new Intl.DateTimeFormat("en-IN", { weekday: "long", day: "numeric", month: "long" }).format(new Date())}</span>
          <h2>Hello, {profileQuery.data?.fullName?.split(" ")[0] || "there"}</h2>
        </div>
        <button className="button button--secondary button--small" type="button" onClick={() => { walletQuery.refetch(); transactionsQuery.refetch(); notificationsQuery.refetch(); }}>
          <RefreshCw size={16} /> Refresh
        </button>
      </section>

      <section className="balance-layout">
        <article className="balance-card">
          <div className="balance-card__header">
            <div>
              <span>Available balance</span>
              <StatusBadge status={wallet.status} showIcon={false} />
            </div>
            <button className="icon-button icon-button--inverse" onClick={() => setBalanceVisible((visible) => !visible)} aria-label={balanceVisible ? "Hide balance" : "Show balance"} title={balanceVisible ? "Hide balance" : "Show balance"}>
              {balanceVisible ? <EyeOff size={19} /> : <Eye size={19} />}
            </button>
          </div>
          <strong className="balance-card__amount">{balanceVisible ? formatMoney(wallet.balance, wallet.currency) : "INR  ****"}</strong>
          <div className="balance-card__footer">
            <span>{wallet.walletNumber}</span>
            <CopyButton value={wallet.walletNumber} label="Wallet number" />
          </div>
        </article>

        <div className="quick-actions" aria-label="Quick actions">
          <Link className="quick-action quick-action--primary" to="/send">
            <span><SendHorizontal size={21} /></span>
            <strong>Send money</strong>
            <ArrowRight size={17} />
          </Link>
          <button className="quick-action" type="button" onClick={() => navigator.clipboard.writeText(String(wallet.walletId))}>
            <span><Copy size={21} /></span>
            <strong>Copy wallet ID</strong>
            <small>#{wallet.walletId}</small>
          </button>
          <Link className="quick-action" to="/wallet">
            <span><WalletCards size={21} /></span>
            <strong>Wallet details</strong>
            <ArrowRight size={17} />
          </Link>
        </div>
      </section>

      <section className="metric-grid" aria-label="Wallet summary">
        <article className="metric-card">
          <span className="metric-card__icon metric-card__icon--incoming"><ArrowDownLeft size={19} /></span>
          <div><span>Total received</span><strong>{formatMoney(metrics.incoming)}</strong></div>
        </article>
        <article className="metric-card">
          <span className="metric-card__icon metric-card__icon--outgoing"><ArrowUpRight size={19} /></span>
          <div><span>Total sent</span><strong>{formatMoney(metrics.outgoing)}</strong></div>
        </article>
        <article className="metric-card">
          <span className="metric-card__icon metric-card__icon--activity"><RefreshCw size={19} /></span>
          <div><span>Transactions</span><strong>{transactions.length}</strong></div>
        </article>
      </section>

      <section className="dashboard-grid">
        <article className="panel cashflow-panel">
          <header className="panel__header">
            <div><h3>Cash flow</h3><p>Last 7 days</p></div>
            <div className="chart-legend"><span className="chart-legend__incoming">Received</span><span className="chart-legend__outgoing">Sent</span></div>
          </header>
          <div className="chart-area">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 8, right: 0, left: -16, bottom: 0 }} barGap={5}>
                <CartesianGrid vertical={false} stroke="#e6edf0" />
                <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: "#6b7b83", fontSize: 12 }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: "#6b7b83", fontSize: 11 }} tickFormatter={(value) => value >= 1000 ? `${value / 1000}k` : value} />
                <Tooltip content={<ChartTooltip />} cursor={{ fill: "#f2f7f9" }} />
                <Bar dataKey="incoming" name="Received" fill="#0c927d" radius={[3, 3, 0, 0]} maxBarSize={22} />
                <Bar dataKey="outgoing" name="Sent" fill="#df6b58" radius={[3, 3, 0, 0]} maxBarSize={22} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </article>

        <article className="panel notification-preview">
          <header className="panel__header">
            <div><h3>Notifications</h3><p>Latest payment updates</p></div>
            <Link className="text-link" to="/notifications">View all</Link>
          </header>
          {notificationsQuery.isLoading ? <SectionLoading label="Loading notifications" /> : notifications.length ? (
            <div className="notification-list notification-list--compact">
              {notifications.map((notification) => (
                <div className="notification-item" key={notification.id}>
                  <span className="notification-item__icon"><Bell size={17} /></span>
                  <div><strong>{notification.message}</strong><span>{formatRelativeTime(notification.createdAt)}</span></div>
                </div>
              ))}
            </div>
          ) : (
            <div className="compact-empty"><Bell size={20} /><span>No notifications yet</span></div>
          )}
        </article>
      </section>

      <section className="panel transactions-panel">
        <header className="panel__header">
          <div><h3>Recent activity</h3><p>Your latest wallet transactions</p></div>
          <Link className="text-link" to="/activity">View all</Link>
        </header>
        {transactionsQuery.isLoading ? <SectionLoading label="Loading transactions" /> : transactionsQuery.error ? (
          <ErrorState message={transactionsQuery.error.message} onRetry={transactionsQuery.refetch} />
        ) : <TransactionTable transactions={transactions} walletId={wallet.walletId} limit={5} />}
      </section>
    </div>
  );
}
