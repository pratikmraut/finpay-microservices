import { CheckCircle2, CircleAlert, Clock3, ShieldCheck } from "lucide-react";

const statusConfig = {
  ACTIVE: { label: "Active", tone: "success", icon: ShieldCheck },
  INACTIVE: { label: "Inactive", tone: "neutral", icon: CircleAlert },
  SUCCESS: { label: "Completed", tone: "success", icon: CheckCircle2 },
  FAILED: { label: "Failed", tone: "danger", icon: CircleAlert },
  COMPENSATED: { label: "Reversed", tone: "warning", icon: CircleAlert },
  PENDING: { label: "Pending", tone: "warning", icon: Clock3 },
  CREATED: { label: "Delivered", tone: "info", icon: CheckCircle2 },
};

export function StatusBadge({ status, showIcon = true }) {
  const config = statusConfig[status] || { label: status || "Unknown", tone: "neutral", icon: CircleAlert };
  const Icon = config.icon;
  return (
    <span className={`status-badge status-badge--${config.tone}`}>
      {showIcon && <Icon size={13} aria-hidden="true" />}
      {config.label}
    </span>
  );
}
