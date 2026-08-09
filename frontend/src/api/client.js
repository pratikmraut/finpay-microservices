import { demoApi } from "./demo";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");
const SESSION_KEY = "finpay.session";
export const isDemoMode = import.meta.env.VITE_DEMO_MODE === "true";

export class ApiError extends Error {
  constructor(message, status, errorCode, details) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errorCode = errorCode;
    this.details = details;
  }
}

export function readSession() {
  try {
    const rawSession = sessionStorage.getItem(SESSION_KEY);
    if (!rawSession) return null;
    const session = JSON.parse(rawSession);
    if (session.expiresAt && Date.now() >= session.expiresAt) {
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
    return session;
  } catch {
    sessionStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function saveSession(session) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY);
}

async function request(path, options = {}) {
  const { method = "GET", body, authenticated = true, signal } = options;
  const session = readSession();
  const headers = { Accept: "application/json" };

  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (authenticated && session?.accessToken) {
    headers.Authorization = `Bearer ${session.accessToken}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    throw new ApiError(
      "FinPay services are unavailable. Check that the API Gateway is running.",
      0,
      "NETWORK_ERROR",
      error,
    );
  }

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    if (response.status === 401 && authenticated) {
      window.dispatchEvent(new CustomEvent("finpay:unauthorized"));
    }
    const responseMessage = typeof payload === "object"
      ? payload?.message || payload?.detail || payload?.error
      : null;
    const fallbackMessage = response.status >= 500
      ? "A FinPay service is unavailable. Check that all backend services are running."
      : typeof payload === "string" && payload.trim()
        ? payload.trim()
        : "The request could not be completed.";
    throw new ApiError(
      responseMessage || fallbackMessage,
      response.status,
      payload?.errorCode || "REQUEST_FAILED",
      payload,
    );
  }

  return payload?.data ?? payload;
}

const liveApi = {
  health: () => request("/actuator/health", { authenticated: false }),
  auth: {
    register: (input) => request("/api/v1/auth/register", { method: "POST", body: input, authenticated: false }),
    login: (input) => request("/api/v1/auth/login", { method: "POST", body: input, authenticated: false }),
    profile: () => request("/api/v1/auth/me"),
  },
  wallets: {
    current: () => request("/api/v1/wallets/me"),
    create: (input) => request("/api/v1/wallets", { method: "POST", body: input }),
    balance: (walletId) => request(`/api/v1/wallets/${walletId}/balance`),
    updateStatus: (walletId, status) => request(`/api/v1/wallets/${walletId}/status`, {
      method: "PATCH",
      body: { status },
    }),
  },
  payments: {
    transfer: (input) => request("/api/v1/payments/transfer", { method: "POST", body: input }),
    transactions: (walletId) => request(`/api/v1/wallets/${walletId}/transactions`),
    byReference: (paymentReference) => request(`/api/v1/payments/${paymentReference}`),
  },
  notifications: {
    all: () => request("/api/v1/notifications"),
  },
};

export const api = isDemoMode ? demoApi : liveApi;
