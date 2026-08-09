import { Bell, CheckCheck, RefreshCw } from "lucide-react";
import { useMemo, useState } from "react";
import { ErrorState, SectionLoading } from "../components/LoadingState";
import { StatusBadge } from "../components/StatusBadge";
import { useNotificationsQuery, useWalletQuery } from "../hooks/useFinPayData";
import { formatDateTime, formatRelativeTime } from "../utils/format";

export function NotificationsPage() {
  const wallet = useWalletQuery().data;
  const notificationsQuery = useNotificationsQuery();
  const storageKey = `finpay.seenNotifications.${wallet.walletId}`;
  const [seenIds, setSeenIds] = useState(() => {
    try { return new Set(JSON.parse(localStorage.getItem(storageKey) || "[]")); } catch { return new Set(); }
  });
  const [filter, setFilter] = useState("all");

  const walletNotifications = useMemo(() => (notificationsQuery.data || [])
    .filter((item) => Number(item.walletId) === Number(wallet.walletId)), [notificationsQuery.data, wallet.walletId]);
  const visibleNotifications = filter === "unread"
    ? walletNotifications.filter((item) => !seenIds.has(item.id))
    : walletNotifications;
  const unreadCount = walletNotifications.filter((item) => !seenIds.has(item.id)).length;

  function persistSeen(nextSeen) {
    setSeenIds(nextSeen);
    localStorage.setItem(storageKey, JSON.stringify([...nextSeen]));
  }

  function markSeen(id) {
    const nextSeen = new Set(seenIds);
    nextSeen.add(id);
    persistSeen(nextSeen);
  }

  function markAllSeen() {
    persistSeen(new Set(walletNotifications.map((item) => item.id)));
  }

  return (
    <section className="panel notifications-page">
      <header className="notifications-toolbar">
        <div className="segmented-control">
          <button type="button" className={filter === "all" ? "active" : ""} onClick={() => setFilter("all")}>All <span>{walletNotifications.length}</span></button>
          <button type="button" className={filter === "unread" ? "active" : ""} onClick={() => setFilter("unread")}>Unread <span>{unreadCount}</span></button>
        </div>
        <div>
          <button className="button button--ghost button--small" type="button" onClick={markAllSeen} disabled={!unreadCount}><CheckCheck size={17} /> Mark all read</button>
          <button className="icon-button" type="button" onClick={() => notificationsQuery.refetch()} aria-label="Refresh notifications" title="Refresh"><RefreshCw size={18} /></button>
        </div>
      </header>

      {notificationsQuery.isLoading ? <SectionLoading label="Loading notifications" /> : notificationsQuery.error ? (
        <ErrorState message={notificationsQuery.error.message} onRetry={notificationsQuery.refetch} />
      ) : visibleNotifications.length ? (
        <div className="notification-feed">
          {visibleNotifications.map((notification) => {
            const unread = !seenIds.has(notification.id);
            return (
              <button type="button" className={`notification-feed__item ${unread ? "notification-feed__item--unread" : ""}`} key={notification.id} onClick={() => markSeen(notification.id)}>
                <span className="notification-feed__icon"><Bell size={19} /></span>
                <span className="notification-feed__body">
                  <strong>{notification.message}</strong>
                  <small>{notification.paymentReference}</small>
                  <em title={formatDateTime(notification.createdAt)}>{formatRelativeTime(notification.createdAt)}</em>
                </span>
                <StatusBadge status={notification.status} />
                {unread && <i className="unread-dot" aria-label="Unread" />}
              </button>
            );
          })}
        </div>
      ) : (
        <div className="empty-state"><span className="empty-state__icon"><Bell size={24} /></span><strong>{filter === "unread" ? "You are all caught up" : "No notifications yet"}</strong><p>Payment updates will appear here.</p></div>
      )}
    </section>
  );
}
