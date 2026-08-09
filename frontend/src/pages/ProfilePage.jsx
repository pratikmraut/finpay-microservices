import { CalendarDays, LogOut, Mail, Phone, ShieldCheck, UserRound, WalletCards } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { ErrorState, SectionLoading } from "../components/LoadingState";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";
import { useProfileQuery, useWalletQuery } from "../hooks/useFinPayData";
import { formatDateTime, initials } from "../utils/format";

export function ProfilePage() {
  const profileQuery = useProfileQuery();
  const wallet = useWalletQuery().data;
  const { logout } = useAuth();
  const navigate = useNavigate();

  if (profileQuery.isLoading) return <SectionLoading label="Loading profile" />;
  if (profileQuery.error) return <ErrorState message={profileQuery.error.message} onRetry={profileQuery.refetch} />;
  const profile = profileQuery.data;

  function signOut() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="profile-page">
      <section className="profile-header">
        <div className="profile-avatar">{initials(profile.fullName)}</div>
        <div><h2>{profile.fullName}</h2><p>{profile.email}</p></div>
        <span className="role-badge"><ShieldCheck size={15} /> {profile.role}</span>
      </section>
      <div className="profile-grid">
        <section className="panel">
          <header className="panel__header"><div><h3>Personal details</h3><p>FinPay account information</p></div><UserRound size={20} /></header>
          <dl className="details-list">
            <div><dt><UserRound size={17} /> Full name</dt><dd>{profile.fullName}</dd></div>
            <div><dt><Mail size={17} /> Email</dt><dd>{profile.email}</dd></div>
            <div><dt><Phone size={17} /> Phone</dt><dd>+91 {profile.phone}</dd></div>
            <div><dt><CalendarDays size={17} /> Member since</dt><dd>{formatDateTime(profile.createdAt)}</dd></div>
          </dl>
        </section>
        <section className="panel account-summary">
          <header className="panel__header"><div><h3>Account summary</h3><p>Linked FinPay wallet</p></div><WalletCards size={20} /></header>
          <div className="account-summary__wallet"><span>Wallet #{wallet.walletId}</span><strong>{wallet.walletNumber}</strong><StatusBadge status={wallet.status} /></div>
          <button className="button button--danger button--wide" type="button" onClick={signOut}><LogOut size={18} /> Sign out</button>
        </section>
      </div>
    </div>
  );
}
