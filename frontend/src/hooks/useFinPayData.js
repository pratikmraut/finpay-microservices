import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";

export const queryKeys = {
  profile: ["profile"],
  wallet: ["wallet"],
  transactions: (walletId) => ["transactions", walletId],
  notifications: ["notifications"],
};

export function useProfileQuery() {
  return useQuery({
    queryKey: queryKeys.profile,
    queryFn: api.auth.profile,
    staleTime: 5 * 60 * 1000,
  });
}

export function useWalletQuery() {
  return useQuery({
    queryKey: queryKeys.wallet,
    queryFn: api.wallets.current,
    retry: (attempt, error) => error.status !== 404 && error.status !== 401 && attempt < 2,
    staleTime: 20 * 1000,
  });
}

export function useTransactionsQuery(walletId) {
  return useQuery({
    queryKey: queryKeys.transactions(walletId),
    queryFn: () => api.payments.transactions(walletId),
    enabled: Boolean(walletId),
    staleTime: 10 * 1000,
  });
}

export function useNotificationsQuery() {
  return useQuery({
    queryKey: queryKeys.notifications,
    queryFn: api.notifications.all,
    refetchInterval: 5 * 1000,
    staleTime: 2 * 1000,
  });
}
