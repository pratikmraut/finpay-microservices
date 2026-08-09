import { ArrowLeft, ArrowRight, CheckCircle2, CircleAlert, SendHorizontal, ShieldCheck, WalletCards } from "lucide-react";
import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { StatusBadge } from "../components/StatusBadge";
import { useToast } from "../context/ToastContext";
import { queryKeys, useWalletQuery } from "../hooks/useFinPayData";
import { formatMoney } from "../utils/format";

export function SendMoneyPage() {
  const walletQuery = useWalletQuery();
  const wallet = walletQuery.data;
  const [form, setForm] = useState({ receiverWalletId: "", amount: "" });
  const [step, setStep] = useState("details");
  const [result, setResult] = useState(null);
  const [validationError, setValidationError] = useState("");
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  const transfer = useMutation({
    mutationFn: () => api.payments.transfer({
      senderWalletId: wallet.walletId,
      receiverWalletId: Number(form.receiverWalletId),
      amount: Number(form.amount),
      currency: wallet.currency,
      idempotencyKey: `web-${crypto.randomUUID()}`,
    }),
    onSuccess: (response) => {
      setResult(response);
      setStep("result");
      queryClient.invalidateQueries({ queryKey: queryKeys.wallet });
      queryClient.invalidateQueries({ queryKey: queryKeys.transactions(wallet.walletId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.notifications });
      showToast(response.status === "SUCCESS" ? "Transfer completed" : response.message, response.status === "SUCCESS" ? "success" : "error");
    },
  });

  function reviewTransfer(event) {
    event.preventDefault();
    const receiverId = Number(form.receiverWalletId);
    const amount = Number(form.amount);
    if (!Number.isInteger(receiverId) || receiverId <= 0) {
      setValidationError("Enter a valid receiver wallet ID.");
      return;
    }
    if (receiverId === Number(wallet.walletId)) {
      setValidationError("Sender and receiver wallets must be different.");
      return;
    }
    if (!Number.isFinite(amount) || amount <= 0) {
      setValidationError("Enter an amount greater than zero.");
      return;
    }
    if (amount > Number(wallet.balance)) {
      setValidationError("This amount is higher than your available balance.");
      return;
    }
    setValidationError("");
    setStep("review");
  }

  function startAnother() {
    setForm({ receiverWalletId: "", amount: "" });
    setResult(null);
    setStep("details");
    transfer.reset();
  }

  if (wallet.status !== "ACTIVE") {
    return (
      <div className="focused-state">
        <span className="focused-state__icon focused-state__icon--warning"><CircleAlert size={25} /></span>
        <h2>Your wallet is inactive</h2>
        <p>Activate the wallet before making a transfer.</p>
        <Link className="button button--primary" to="/wallet">Open wallet controls</Link>
      </div>
    );
  }

  return (
    <div className="send-page">
      <div className="step-indicator" aria-label="Transfer progress">
        <span className={step === "details" ? "active" : "complete"}><i>1</i> Details</span>
        <b />
        <span className={step === "review" ? "active" : step === "result" ? "complete" : ""}><i>2</i> Review</span>
        <b />
        <span className={step === "result" ? "active" : ""}><i>3</i> Receipt</span>
      </div>

      {step === "details" && (
        <div className="send-layout">
          <form className="panel transfer-form" onSubmit={reviewTransfer}>
            <header className="panel__header">
              <div><h3>Transfer details</h3><p>Send INR to another wallet</p></div>
              <span className="panel-icon"><SendHorizontal size={20} /></span>
            </header>
            <label className="field">
              <span>Receiver wallet ID</span>
              <div className="input-with-icon">
                <WalletCards size={18} />
                <input type="number" inputMode="numeric" min="1" step="1" value={form.receiverWalletId} onChange={(event) => { setForm((current) => ({ ...current, receiverWalletId: event.target.value })); setValidationError(""); }} placeholder="Example: 2" required autoFocus />
              </div>
            </label>
            <label className="field">
              <span>Amount</span>
              <div className="money-field money-field--form">
                <span>INR</span>
                <input type="number" inputMode="decimal" min="0.01" step="0.01" value={form.amount} onChange={(event) => { setForm((current) => ({ ...current, amount: event.target.value })); setValidationError(""); }} placeholder="0.00" required />
              </div>
            </label>
            <div className="available-row"><span>Available</span><strong>{formatMoney(wallet.balance, wallet.currency)}</strong></div>
            {validationError && <div className="inline-alert inline-alert--error" role="alert">{validationError}</div>}
            <button className="button button--primary button--wide" type="submit">Review transfer <ArrowRight size={18} /></button>
          </form>

          <aside className="transfer-aside">
            <span className="transfer-aside__icon"><ShieldCheck size={24} /></span>
            <h3>Protected transfer</h3>
            <dl>
              <div><dt>From</dt><dd>Wallet #{wallet.walletId}</dd></div>
              <div><dt>Currency</dt><dd>{wallet.currency}</dd></div>
              <div><dt>Wallet status</dt><dd><StatusBadge status={wallet.status} /></dd></div>
            </dl>
          </aside>
        </div>
      )}

      {step === "review" && (
        <section className="review-panel panel">
          <button className="button button--ghost button--small review-panel__back" type="button" onClick={() => setStep("details")}><ArrowLeft size={16} /> Edit details</button>
          <span className="review-panel__icon"><SendHorizontal size={24} /></span>
          <span className="eyebrow">Confirm transfer</span>
          <h2>{formatMoney(form.amount, wallet.currency)}</h2>
          <p>to Wallet #{form.receiverWalletId}</p>
          <div className="review-details">
            <div><span>From wallet</span><strong>{wallet.walletNumber}</strong></div>
            <div><span>Transfer amount</span><strong>{formatMoney(form.amount)}</strong></div>
            <div><span>Fee</span><strong>{formatMoney(0)}</strong></div>
            <div className="review-details__total"><span>Total debit</span><strong>{formatMoney(form.amount)}</strong></div>
          </div>
          {transfer.error && <div className="inline-alert inline-alert--error" role="alert">{transfer.error.message}</div>}
          <button className="button button--primary button--wide" type="button" onClick={() => transfer.mutate()} disabled={transfer.isPending}>
            {transfer.isPending ? "Processing transfer..." : "Confirm and send"}
            {!transfer.isPending && <ArrowRight size={18} />}
          </button>
        </section>
      )}

      {step === "result" && result && (
        <section className={`receipt-panel panel ${result.status === "SUCCESS" ? "receipt-panel--success" : "receipt-panel--failed"}`}>
          <span className="receipt-panel__icon">{result.status === "SUCCESS" ? <CheckCircle2 size={31} /> : <CircleAlert size={31} />}</span>
          <span className="eyebrow">{result.status === "SUCCESS" ? "Transfer complete" : "Transfer update"}</span>
          <h2>{formatMoney(result.amount, result.currency)}</h2>
          <p>{result.message}</p>
          <StatusBadge status={result.status} />
          <dl className="receipt-details">
            <div><dt>Reference</dt><dd>{result.paymentReference}</dd></div>
            <div><dt>Receiver</dt><dd>Wallet #{result.receiverWalletId}</dd></div>
            <div><dt>Sender</dt><dd>Wallet #{result.senderWalletId}</dd></div>
          </dl>
          <div className="receipt-actions">
            <button className="button button--secondary" type="button" onClick={startAnother}>Send another</button>
            <Link className="button button--primary" to="/activity">View activity</Link>
          </div>
        </section>
      )}
    </div>
  );
}
