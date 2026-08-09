import { BadgeIndianRupee } from "lucide-react";
import { Link } from "react-router-dom";

export function Brand({ compact = false, inverse = false }) {
  return (
    <Link className={`brand ${inverse ? "brand--inverse" : ""}`} to="/dashboard" aria-label="FinPay dashboard">
      <span className="brand__mark" aria-hidden="true">
        <BadgeIndianRupee size={compact ? 21 : 24} strokeWidth={2.2} />
      </span>
      {!compact && <span className="brand__name">FinPay</span>}
    </Link>
  );
}
