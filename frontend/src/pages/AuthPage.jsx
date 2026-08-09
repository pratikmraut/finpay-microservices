import { ArrowRight, Eye, EyeOff, LockKeyhole, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { Brand } from "../components/Brand";
import { useAuth } from "../context/AuthContext";
import { isDemoMode } from "../api/client";

const emptyForm = { fullName: "", email: "", phone: "", password: "" };
const demoLoginForm = { ...emptyForm, email: "demo@finpay.app", password: "Demo@123" };

export function AuthPage({ mode }) {
  const isLogin = mode === "login";
  const [form, setForm] = useState(() => isLogin && isDemoMode ? demoLoginForm : emptyForm);
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const { isAuthenticated, login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    setForm(isLogin && isDemoMode ? demoLoginForm : emptyForm);
    setError("");
  }, [isLogin]);

  if (isAuthenticated) return <Navigate to="/dashboard" replace />;

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setError("");
  }

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      if (isLogin) {
        await login({ email: form.email.trim(), password: form.password });
        navigate(location.state?.from || "/dashboard", { replace: true });
      } else {
        await register({
          fullName: form.fullName.trim(),
          email: form.email.trim(),
          phone: form.phone.trim(),
          password: form.password,
        });
        navigate("/login", {
          replace: true,
          state: { success: "Account created. Sign in to set up your wallet." },
        });
      }
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-brand-panel" aria-label="FinPay">
        <Brand inverse />
        <div className="auth-brand-panel__content">
          <span className="auth-kicker"><ShieldCheck size={17} /> Digital wallet</span>
          <h1>Money in motion.<br />Always in view.</h1>
          <div className="auth-ledger" aria-hidden="true">
            <div className="auth-ledger__balance">
              <span>Available balance</span>
              <strong>INR</strong>
            </div>
            <div className="auth-ledger__line"><span /><span /></div>
            <div className="auth-ledger__line"><span /><span /></div>
            <div className="auth-ledger__line"><span /><span /></div>
          </div>
        </div>
        <p className="auth-brand-panel__footer"><LockKeyhole size={15} /> Protected account access</p>
      </section>

      <section className="auth-form-panel">
        <div className="auth-mobile-brand"><Brand /></div>
        <div className="auth-form-wrap">
          <header className="auth-form-header">
            <span className="eyebrow">{isLogin ? "Welcome back" : "Create your account"}</span>
            <h2>{isLogin ? "Sign in to FinPay" : "Join FinPay"}</h2>
            <p>{isLogin ? "Enter your account credentials." : "Start with your personal details."}</p>
          </header>

          {location.state?.success && isLogin && (
            <div className="inline-alert inline-alert--success" role="status">
              <ShieldCheck size={18} /> {location.state.success}
            </div>
          )}
          {error && <div className="inline-alert inline-alert--error" role="alert">{error}</div>}

          <form className="auth-form" onSubmit={submit}>
            {!isLogin && (
              <label className="field">
                <span>Full name</span>
                <input name="fullName" value={form.fullName} onChange={updateField} autoComplete="name" maxLength={120} required placeholder="Pratik Raut" />
              </label>
            )}
            <label className="field">
              <span>Email address</span>
              <input name="email" type="email" value={form.email} onChange={updateField} autoComplete="email" maxLength={160} required placeholder="name@example.com" />
            </label>
            {!isLogin && (
              <label className="field">
                <span>Phone number</span>
                <div className="phone-input">
                  <span>+91</span>
                  <input name="phone" inputMode="numeric" pattern="[0-9]{10}" value={form.phone} onChange={updateField} autoComplete="tel-national" maxLength={10} required placeholder="9876543210" />
                </div>
              </label>
            )}
            <label className="field">
              <span>Password</span>
              <div className="password-input">
                <input name="password" type={showPassword ? "text" : "password"} value={form.password} onChange={updateField} autoComplete={isLogin ? "current-password" : "new-password"} minLength={6} maxLength={80} required placeholder="Minimum 6 characters" />
                <button type="button" className="icon-button icon-button--small" onClick={() => setShowPassword((visible) => !visible)} aria-label={showPassword ? "Hide password" : "Show password"} title={showPassword ? "Hide password" : "Show password"}>
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </label>
            <button className="button button--primary button--wide" type="submit" disabled={submitting}>
              {submitting ? "Please wait..." : isLogin ? "Sign in" : "Create account"}
              {!submitting && <ArrowRight size={18} />}
            </button>
          </form>

          <p className="auth-switch">
            {isLogin ? "New to FinPay?" : "Already have an account?"}{" "}
            <Link to={isLogin ? "/register" : "/login"}>{isLogin ? "Create account" : "Sign in"}</Link>
          </p>
        </div>
      </section>
    </main>
  );
}
