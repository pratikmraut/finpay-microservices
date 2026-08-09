const STORAGE_KEY = "finpay.demo.state.v1";

function isoDate(daysAgo = 0, hoursAgo = 0) {
  const date = new Date();
  date.setDate(date.getDate() - daysAgo);
  date.setHours(date.getHours() - hoursAgo);
  return date.toISOString();
}

function createSeedState() {
  const walletId = 101;
  const transactions = [
    {
      paymentReference: "PAY-DEMO-260809-A1C7",
      senderWalletId: 204,
      receiverWalletId: walletId,
      amount: 4250,
      currency: "INR",
      status: "SUCCESS",
      message: "Payment completed successfully.",
      createdAt: isoDate(0, 2),
    },
    {
      paymentReference: "PAY-DEMO-260808-B9F2",
      senderWalletId: walletId,
      receiverWalletId: 318,
      amount: 1280,
      currency: "INR",
      status: "SUCCESS",
      message: "Payment completed successfully.",
      createdAt: isoDate(1, 4),
    },
    {
      paymentReference: "PAY-DEMO-260806-C4D8",
      senderWalletId: 412,
      receiverWalletId: walletId,
      amount: 7800,
      currency: "INR",
      status: "SUCCESS",
      message: "Payment completed successfully.",
      createdAt: isoDate(3, 1),
    },
    {
      paymentReference: "PAY-DEMO-260804-D6E3",
      senderWalletId: walletId,
      receiverWalletId: 226,
      amount: 950,
      currency: "INR",
      status: "SUCCESS",
      message: "Payment completed successfully.",
      createdAt: isoDate(5, 3),
    },
  ];

  return {
    profile: {
      userId: 1001,
      fullName: "Demo User",
      email: "demo@finpay.app",
      phone: "9876543210",
      role: "USER",
      createdAt: isoDate(120),
    },
    wallet: {
      walletId,
      userId: 1001,
      walletNumber: "WALLET-DEMO-0101",
      balance: 24820,
      currency: "INR",
      status: "ACTIVE",
      createdAt: isoDate(90),
    },
    transactions,
    notifications: transactions.slice(0, 3).map((transaction, index) => ({
      id: index + 1,
      walletId,
      paymentReference: transaction.paymentReference,
      message: Number(transaction.senderWalletId) === walletId
        ? `Payment ${transaction.paymentReference} completed. You sent INR ${transaction.amount.toLocaleString("en-IN")}.`
        : `Payment ${transaction.paymentReference} completed. You received INR ${transaction.amount.toLocaleString("en-IN")}.`,
      status: "SUCCESS",
      createdAt: transaction.createdAt,
    })),
  };
}

function clone(value) {
  return value === undefined ? undefined : JSON.parse(JSON.stringify(value));
}

function readState() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) return JSON.parse(stored);
  } catch {
    localStorage.removeItem(STORAGE_KEY);
  }
  const state = createSeedState();
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  return state;
}

function writeState(state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function respond(value, delay = 140) {
  return new Promise((resolve) => window.setTimeout(() => resolve(clone(value)), delay));
}

function demoError(message, status, errorCode) {
  const error = new Error(message);
  error.name = "ApiError";
  error.status = status;
  error.errorCode = errorCode;
  return error;
}

function requireWallet(state) {
  if (!state.wallet) throw demoError("Create a wallet to continue.", 404, "WALLET_NOT_FOUND");
  return state.wallet;
}

function paymentReference() {
  return `PAY-DEMO-${Date.now().toString(36).toUpperCase()}`;
}

export const demoApi = {
  health: () => respond({ status: "UP", mode: "DEMO" }),
  auth: {
    register: (input) => {
      const state = readState();
      state.profile = {
        userId: 1001,
        fullName: input.fullName,
        email: input.email,
        phone: input.phone,
        role: "USER",
        createdAt: new Date().toISOString(),
      };
      state.wallet = null;
      state.transactions = [];
      state.notifications = [];
      writeState(state);
      return respond({ userId: state.profile.userId, email: state.profile.email, fullName: state.profile.fullName });
    },
    login: (input) => {
      const state = readState();
      if (!input.email || !input.password || input.email.toLowerCase() !== state.profile.email.toLowerCase()) {
        return Promise.reject(demoError("Use the demo account shown in the form, or create a new account.", 401, "INVALID_CREDENTIALS"));
      }
      return respond({
        accessToken: "finpay-demo-session",
        tokenType: "Bearer",
        userId: state.profile.userId,
        email: state.profile.email,
        expiresInSeconds: 8 * 60 * 60,
      });
    },
    profile: () => respond(readState().profile),
  },
  wallets: {
    current: () => {
      const state = readState();
      try {
        return respond(requireWallet(state));
      } catch (error) {
        return Promise.reject(error);
      }
    },
    create: (input) => {
      const state = readState();
      state.wallet = {
        walletId: 101,
        userId: input.userId,
        walletNumber: "WALLET-DEMO-0101",
        balance: Number(input.initialBalance),
        currency: input.currency || "INR",
        status: "ACTIVE",
        createdAt: new Date().toISOString(),
      };
      writeState(state);
      return respond(state.wallet);
    },
    balance: () => {
      const wallet = requireWallet(readState());
      return respond({ walletId: wallet.walletId, balance: wallet.balance, currency: wallet.currency });
    },
    updateStatus: (walletId, status) => {
      const state = readState();
      const wallet = requireWallet(state);
      if (Number(wallet.walletId) !== Number(walletId)) {
        return Promise.reject(demoError("Wallet not found.", 404, "WALLET_NOT_FOUND"));
      }
      wallet.status = status;
      writeState(state);
      return respond(wallet);
    },
  },
  payments: {
    transfer: (input) => {
      const state = readState();
      const wallet = requireWallet(state);
      const amount = Number(input.amount);
      if (wallet.status !== "ACTIVE") {
        return Promise.reject(demoError("Activate the wallet before making a transfer.", 409, "WALLET_INACTIVE"));
      }
      if (!Number.isFinite(amount) || amount <= 0 || amount > Number(wallet.balance)) {
        return Promise.reject(demoError("The transfer amount is not available.", 400, "INVALID_AMOUNT"));
      }

      const transaction = {
        paymentReference: paymentReference(),
        senderWalletId: wallet.walletId,
        receiverWalletId: Number(input.receiverWalletId),
        amount,
        currency: input.currency || wallet.currency,
        status: "SUCCESS",
        message: "Payment completed successfully in demo mode.",
        createdAt: new Date().toISOString(),
      };
      wallet.balance = Number((Number(wallet.balance) - amount).toFixed(2));
      state.transactions.unshift(transaction);
      state.notifications.unshift({
        id: Date.now(),
        walletId: wallet.walletId,
        paymentReference: transaction.paymentReference,
        message: `Payment ${transaction.paymentReference} completed. You sent INR ${amount.toLocaleString("en-IN")}.`,
        status: "SUCCESS",
        createdAt: transaction.createdAt,
      });
      writeState(state);
      return respond(transaction, 300);
    },
    transactions: () => respond(readState().transactions),
    byReference: (reference) => {
      const transaction = readState().transactions.find((item) => item.paymentReference === reference);
      return transaction
        ? respond(transaction)
        : Promise.reject(demoError("Payment not found.", 404, "PAYMENT_NOT_FOUND"));
    },
  },
  notifications: {
    all: () => respond(readState().notifications),
  },
};
