import { BadgeIndianRupee, CalendarDays, CircleAlert, Hash, ShieldCheck, UserRound, WalletCards } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { CopyButton } from "../components/CopyButton";
import { StatusBadge } from "../components/StatusBadge";
import { useToast } from "../context/ToastContext";
import { queryKeys, useWalletQuery } from "../hooks/useFinPayData";
import { formatDateTime, formatMoney } from "../utils/format";

export function WalletPage() {
  const wallet = useWalletQuery().data;
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  const statusMutation = useMutation({
    mutationFn: (status) => api.wallets.updateStatus(wallet.walletId, status),
    onSuccess: (updatedWallet) => {
      queryClient.setQueryData(queryKeys.wallet, updatedWallet);
      showToast(`Wallet ${updatedWallet.status === "ACTIVE" ? "activated" : "deactivated"}`, "success");
    },
  });

  const isActive = wallet.status === "ACTIVE";

  return (
    <div className="wallet-page">
      <section className="wallet-hero">
        <div className="wallet-hero__icon"><WalletCards size={25} /></div>
        <div className="wallet-hero__main">
          <span>Available balance</span>
          <h2>{formatMoney(wallet.balance, wallet.currency)}</h2>
          <div><span>{wallet.walletNumber}</span><CopyButton value={wallet.walletNumber} label="Wallet number" /></div>
        </div>
        <StatusBadge status={wallet.status} />
      </section>

      <div className="wallet-grid">
        <section className="panel wallet-details">
          <header className="panel__header"><div><h3>Wallet details</h3><p>Account identifiers</p></div></header>
          <dl className="details-list">
            <div><dt><Hash size={17} /> Wallet ID</dt><dd>#{wallet.walletId}<CopyButton value={wallet.walletId} label="Wallet ID" /></dd></div>
            <div><dt><WalletCards size={17} /> Wallet number</dt><dd>{wallet.walletNumber}<CopyButton value={wallet.walletNumber} label="Wallet number" /></dd></div>
            <div><dt><UserRound size={17} /> Owner ID</dt><dd>#{wallet.userId}</dd></div>
            <div><dt><BadgeIndianRupee size={17} /> Currency</dt><dd>{wallet.currency}</dd></div>
            <div><dt><CalendarDays size={17} /> Created</dt><dd>{formatDateTime(wallet.createdAt)}</dd></div>
          </dl>
        </section>

        <section className="panel wallet-controls">
          <header className="panel__header"><div><h3>Wallet controls</h3><p>Manage transfer availability</p></div><ShieldCheck size={20} /></header>
          <div className="toggle-row">
            <div>
              <strong>Wallet active</strong>
              <span>{isActive ? "Transfers are enabled" : "Transfers are paused"}</span>
            </div>
            <label className="switch">
              <input type="checkbox" checked={isActive} onChange={(event) => statusMutation.mutate(event.target.checked ? "ACTIVE" : "INACTIVE")} disabled={statusMutation.isPending} />
              <span aria-hidden="true" />
              <em className="sr-only">Toggle wallet status</em>
            </label>
          </div>
          {!isActive && (
            <div className="inline-alert inline-alert--warning"><CircleAlert size={18} /> Outgoing and incoming transfers require an active wallet.</div>
          )}
          {statusMutation.error && <div className="inline-alert inline-alert--error" role="alert">{statusMutation.error.message}</div>}
          <div className="security-strip"><ShieldCheck size={18} /><span><strong>JWT protected</strong><small>Authenticated through the FinPay API Gateway</small></span></div>
        </section>
      </div>
    </div>
  );
}
