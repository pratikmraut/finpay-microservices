import {
  Bell,
  LayoutDashboard,
  LogOut,
  Menu,
  ReceiptText,
  SendHorizontal,
  UserRound,
  WalletCards,
  X,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useNotificationsQuery, useProfileQuery, useWalletQuery } from "../hooks/useFinPayData";
import { initials } from "../utils/format";
import { isDemoMode } from "../api/client";
import { Brand } from "./Brand";

const navigation = [
  { to: "/dashboard", label: "Overview", icon: LayoutDashboard },
  { to: "/send", label: "Send money", icon: SendHorizontal },
  { to: "/activity", label: "Activity", icon: ReceiptText },
  { to: "/wallet", label: "Wallet", icon: WalletCards },
  { to: "/notifications", label: "Notifications", icon: Bell },
  { to: "/profile", label: "Profile", icon: UserRound },
];

const pageTitles = {
  "/dashboard": ["Overview", "Your FinPay account at a glance"],
  "/send": ["Send money", "Transfer securely to another FinPay wallet"],
  "/activity": ["Activity", "Track every movement in your wallet"],
  "/wallet": ["Wallet", "Balance, identifiers, and wallet controls"],
  "/notifications": ["Notifications", "Payment updates from FinPay"],
  "/profile": ["Profile", "Your personal account details"],
};

export function AppShell() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { session, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const profileQuery = useProfileQuery();
  const walletQuery = useWalletQuery();
  const notificationsQuery = useNotificationsQuery();
  const [title, subtitle] = pageTitles[location.pathname] || ["FinPay", "Digital wallet"];

  useEffect(() => setMobileOpen(false), [location.pathname]);

  const notificationCount = useMemo(() => {
    const walletId = walletQuery.data?.walletId;
    if (!walletId) return 0;
    return (notificationsQuery.data || []).filter((item) => Number(item.walletId) === Number(walletId)).length;
  }, [notificationsQuery.data, walletQuery.data]);

  function signOut() {
    logout();
    navigate("/login", { replace: true });
  }

  const displayName = profileQuery.data?.fullName || session?.email || "FinPay User";

  return (
    <div className="app-layout">
      <aside className={`sidebar ${mobileOpen ? "sidebar--open" : ""}`}>
        <div className="sidebar__top">
          <Brand inverse />
          <button className="icon-button sidebar__close" onClick={() => setMobileOpen(false)} aria-label="Close navigation">
            <X size={20} />
          </button>
        </div>

        <nav className="sidebar__nav" aria-label="Main navigation">
          {navigation.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={({ isActive }) => `nav-item ${isActive ? "nav-item--active" : ""}`}>
              <Icon size={19} aria-hidden="true" />
              <span>{label}</span>
              {to === "/notifications" && notificationCount > 0 && (
                <span className="nav-count">{Math.min(notificationCount, 99)}</span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar__account">
          <div className="avatar avatar--dark">{initials(displayName)}</div>
          <div className="sidebar__identity">
            <strong>{displayName}</strong>
            <span>{session?.email}</span>
          </div>
          <button className="icon-button icon-button--inverse" onClick={signOut} aria-label="Sign out" title="Sign out">
            <LogOut size={18} />
          </button>
        </div>
      </aside>

      {mobileOpen && <button className="sidebar-backdrop" onClick={() => setMobileOpen(false)} aria-label="Close navigation" />}

      <main className="app-main">
        <header className="topbar">
          <button className="icon-button topbar__menu" onClick={() => setMobileOpen(true)} aria-label="Open navigation">
            <Menu size={21} />
          </button>
          <div className="topbar__titles">
            <div className="topbar__heading"><h1>{title}</h1>{isDemoMode && <span className="environment-badge">Demo</span>}</div>
            <p>{subtitle}</p>
          </div>
          <div className="topbar__actions">
            <NavLink className="icon-button notification-button" to="/notifications" aria-label={`${notificationCount} notifications`}>
              <Bell size={19} />
              {notificationCount > 0 && <span>{Math.min(notificationCount, 9)}</span>}
            </NavLink>
            <NavLink className="avatar" to="/profile" aria-label="Open profile">{initials(displayName)}</NavLink>
          </div>
        </header>
        <div className="page-content"><Outlet /></div>
      </main>

      <nav className="mobile-nav" aria-label="Mobile navigation">
        {navigation.slice(0, 4).map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} className={({ isActive }) => `mobile-nav__item ${isActive ? "mobile-nav__item--active" : ""}`}>
            <Icon size={20} />
            <span>{label === "Send money" ? "Send" : label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
