import { ArrowRight, BadgeIndianRupee, Check, ShieldCheck, WalletCards } from "lucide-react";
import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Navigate, useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { Brand } from "../components/Brand";
import { useAuth } from "../context/AuthContext";
import { useWalletQuery, queryKeys } from "../hooks/useFinPayData";

export function WalletSetupPage() {
  const [initialBalance, setInitialBalance] = useState("5000");
  const { session, logout } = useAuth();
  const walletQuery = useWalletQuery();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const createWallet = useMutation({
    mutationFn: () => api.wallets.create({
      userId: session.userId,
      initialBalance: Number(initialBalance),
      currency: "INR",
    }),
    onSuccess: (wallet) => {
      queryClient.setQueryData(queryKeys.wallet, wallet);
      navigate("/dashboard", { replace: true });
    },
  });

  if (walletQuery.data) return <Navigate to="/dashboard" replace />;

  return (
    <main className="setup-layout">
      <header className="setup-header">
        <Brand />
        <button className="button button--ghost button--small" type="button" onClick={logout}>Sign out</button>
      </header>
      <section className="setup-content">
        <div className="setup-copy">
          <span className="setup-step">Account ready <Check size={15} /></span>
          <h1>Create your FinPay wallet</h1>
          <p>Choose an opening balance for this development wallet. Your wallet will use INR.</p>
          <div className="setup-points">
            <div><WalletCards size={19} /><span><strong>One wallet</strong><small>Linked to your FinPay account</small></span></div>
            <div><ShieldCheck size={19} /><span><strong>Active by default</strong><small>Ready for wallet-to-wallet transfers</small></span></div>
          </div>
        </div>

        <form className="setup-form" onSubmit={(event) => { event.preventDefault(); createWallet.mutate(); }}>
          <span className="setup-form__icon"><BadgeIndianRupee size={24} /></span>
          <h2>Opening balance</h2>
          <p>Development account funding</p>
          <label className="money-field">
            <span>INR</span>
            <input type="number" min="0" step="0.01" max="10000000" value={initialBalance} onChange={(event) => setInitialBalance(event.target.value)} required autoFocus />
          </label>
          <div className="segmented-options" aria-label="Opening balance presets">
            {[0, 5000, 10000].map((amount) => (
              <button key={amount} type="button" className={Number(initialBalance) === amount ? "active" : ""} onClick={() => setInitialBalance(String(amount))}>
                {amount === 0 ? "Zero" : `INR ${amount.toLocaleString("en-IN")}`}
              </button>
            ))}
          </div>
          {createWallet.error && <div className="inline-alert inline-alert--error" role="alert">{createWallet.error.message}</div>}
          <button className="button button--primary button--wide" type="submit" disabled={createWallet.isPending}>
            {createWallet.isPending ? "Creating wallet..." : "Create wallet"}
            {!createWallet.isPending && <ArrowRight size={18} />}
          </button>
        </form>
      </section>
    </main>
  );
}
