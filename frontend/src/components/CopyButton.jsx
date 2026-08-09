import { Check, Copy } from "lucide-react";
import { useState } from "react";
import { useToast } from "../context/ToastContext";

export function CopyButton({ value, label = "Copy" }) {
  const [copied, setCopied] = useState(false);
  const { showToast } = useToast();

  async function copyValue() {
    await navigator.clipboard.writeText(String(value));
    setCopied(true);
    showToast(`${label} copied`, "success");
    window.setTimeout(() => setCopied(false), 1600);
  }

  return (
    <button className="icon-button icon-button--small" type="button" onClick={copyValue} aria-label={label} title={label}>
      {copied ? <Check size={16} /> : <Copy size={16} />}
    </button>
  );
}
