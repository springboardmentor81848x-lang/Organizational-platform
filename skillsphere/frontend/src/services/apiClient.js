import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
  headers: { Accept: "application/json" },
});

const hasCsrfCookie = () =>
  typeof document !== "undefined" &&
  document.cookie.split("; ").some((c) => c.startsWith("XSRF-TOKEN="));

export async function ensureCsrf() {
  if (!hasCsrfCookie()) {
    await api.get("/api/auth/csrf");
  }
}

api.interceptors.request.use(async (config) => {
  const method = (config.method || "get").toLowerCase();
  if (method !== "get" && method !== "head" && method !== "options") {
    await ensureCsrf();
  }
  return config;
});

export function parseApiError(error) {
  if (error.response) {
    const data = error.response.data || {};
    return {
      status: error.response.status,
      message: data.message || "Something went wrong. Please try again.",
      fieldErrors: data.fieldErrors || {},
    };
  }
  return {
    status: 0,
    message: "Cannot reach the server. Check that the backend is running and try again.",
    fieldErrors: {},
  };
}
