import { LoaderCircle, RefreshCw, WifiOff } from "lucide-react";

export function LoadingScreen({ label = "Loading your FinPay account" }) {
  return (
    <div className="loading-screen" role="status">
      <LoaderCircle className="spin" size={28} aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

export function SectionLoading({ label = "Loading" }) {
  return (
    <div className="section-state section-state--loading" role="status">
      <LoaderCircle className="spin" size={22} aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

export function ErrorState({ title = "We could not load this data", message, onRetry }) {
  return (
    <div className="section-state section-state--error" role="alert">
      <span className="section-state__icon"><WifiOff size={22} aria-hidden="true" /></span>
      <div>
        <strong>{title}</strong>
        <p>{message || "Check the service connection and try again."}</p>
      </div>
      {onRetry && (
        <button className="button button--secondary button--small" type="button" onClick={onRetry}>
          <RefreshCw size={16} aria-hidden="true" />
          Retry
        </button>
      )}
    </div>
  );
}
