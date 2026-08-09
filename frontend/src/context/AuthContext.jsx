import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, clearSession, readSession, saveSession } from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession);
  const queryClient = useQueryClient();

  const logout = useCallback(() => {
    clearSession();
    setSession(null);
    queryClient.clear();
  }, [queryClient]);

  useEffect(() => {
    window.addEventListener("finpay:unauthorized", logout);
    return () => window.removeEventListener("finpay:unauthorized", logout);
  }, [logout]);

  const login = useCallback(async (credentials) => {
    const response = await api.auth.login(credentials);
    const nextSession = {
      accessToken: response.accessToken,
      tokenType: response.tokenType,
      userId: response.userId,
      email: response.email,
      expiresAt: Date.now() + response.expiresInSeconds * 1000,
    };
    saveSession(nextSession);
    setSession(nextSession);
    return nextSession;
  }, []);

  const register = useCallback((details) => api.auth.register(details), []);

  const value = useMemo(() => ({
    session,
    isAuthenticated: Boolean(session?.accessToken),
    login,
    register,
    logout,
  }), [session, login, register, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside AuthProvider");
  return value;
}
