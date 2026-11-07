import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  withCredentials: true, // send the HttpOnly session cookie
  withXSRFToken: true, // send X-XSRF-TOKEN even though the API is on another port
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
  headers: { Accept: "application/json" },
});

const hasCsrfCookie = () => document.cookie.split("; ").some((c) => c.startsWith("XSRF-TOKEN="));

/** Makes sure the CSRF cookie exists before any state-changing request. */
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

/** Turns any Axios error into { message, fieldErrors, status } for the UI. */
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

export const register = (payload) => api.post("/api/auth/register", payload).then((r) => r.data);
export const login = (payload) => api.post("/api/auth/login", payload).then((r) => r.data);
export const logout = () => api.post("/api/auth/logout").then((r) => r.data);
export const getMe = () => api.get("/api/auth/me").then((r) => r.data);
export const forgotPassword = (email) => api.post("/api/auth/forgot-password", { email }).then((r) => r.data);
export const resetPassword = (payload) => api.post("/api/auth/reset-password", payload).then((r) => r.data);

export const getDashboard = (role) => api.get(`/api/dashboard/${role.toLowerCase()}`).then((r) => r.data);
export const listUsers = () => api.get("/api/admin/users").then((r) => r.data);
export const updateUserRole = (id, role) => api.patch(`/api/admin/users/${id}/role`, { role }).then((r) => r.data);
export const updateUserStatus = (id, status) => api.patch(`/api/admin/users/${id}/status`, { status }).then((r) => r.data);

/** Asks the backend (which calls Google Gemini) to recommend courses for an assessment result. */
export const getAiRecommendations = (payload) =>
  api.post("/api/ai/recommendations", payload, { timeout: 70000 }).then((r) => r.data);

export const dashboardPath = (role) => {
  const r = String(role || "employee").toUpperCase();
  if (r === "HR_SPECIALIST" || r === "HR") return "/dashboard/hr";
  if (r === "DEPARTMENT_HEAD") return "/dashboard/department-head";
  if (r === "MANAGER") return "/dashboard/manager";
  if (r === "MENTOR") return "/dashboard/mentor";
  if (r === "ADMIN") return "/dashboard/admin";
  return "/dashboard/employee";
};
