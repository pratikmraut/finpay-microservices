import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const gatewayTarget = env.VITE_GATEWAY_PROXY_TARGET || "http://localhost:8080";
  const base = process.env.VITE_BASE_PATH || env.VITE_BASE_PATH || "/";

  return {
    base,
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        "/api": {
          target: gatewayTarget,
          changeOrigin: true,
        },
        "/actuator": {
          target: gatewayTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
