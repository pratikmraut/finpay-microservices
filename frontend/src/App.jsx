import { lazy, Suspense } from "react";
import { Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { ErrorState, LoadingScreen } from "./components/LoadingState";
import { useAuth } from "./context/AuthContext";
import { useWalletQuery } from "./hooks/useFinPayData";
import { AuthPage } from "./pages/AuthPage";
import { WalletSetupPage } from "./pages/WalletSetupPage";

const ActivityPage = lazy(() => import("./pages/ActivityPage").then((module) => ({ default: module.ActivityPage })));
const DashboardPage = lazy(() => import("./pages/DashboardPage").then((module) => ({ default: module.DashboardPage })));
const NotificationsPage = lazy(() => import("./pages/NotificationsPage").then((module) => ({ default: module.NotificationsPage })));
const ProfilePage = lazy(() => import("./pages/ProfilePage").then((module) => ({ default: module.ProfilePage })));
const SendMoneyPage = lazy(() => import("./pages/SendMoneyPage").then((module) => ({ default: module.SendMoneyPage })));
const WalletPage = lazy(() => import("./pages/WalletPage").then((module) => ({ default: module.WalletPage })));

function RequireAuth() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  return isAuthenticated
    ? <Outlet />
    : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

function RequireWallet() {
  const walletQuery = useWalletQuery();
  if (walletQuery.isLoading) return <LoadingScreen label="Opening your wallet" />;
  if (walletQuery.error?.status === 404) return <Navigate to="/setup" replace />;
  if (walletQuery.error) {
    return <div className="standalone-state"><ErrorState title="Wallet service unavailable" message={walletQuery.error.message} onRetry={walletQuery.refetch} /></div>;
  }
  return <Outlet />;
}

export default function App() {
  return (
    <Suspense fallback={<LoadingScreen label="Loading FinPay" />}>
      <Routes>
        <Route path="/login" element={<AuthPage mode="login" />} />
        <Route path="/register" element={<AuthPage mode="register" />} />
        <Route element={<RequireAuth />}>
          <Route path="/setup" element={<WalletSetupPage />} />
          <Route element={<RequireWallet />}>
            <Route element={<AppShell />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/send" element={<SendMoneyPage />} />
              <Route path="/activity" element={<ActivityPage />} />
              <Route path="/wallet" element={<WalletPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route path="/profile" element={<ProfilePage />} />
            </Route>
          </Route>
        </Route>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  );
}
