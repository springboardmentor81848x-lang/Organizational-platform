const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

export async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options,
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(typeof body === "string" ? body : body?.message || "Request failed");
  }
  return body;
}

export function saveSession(user) {
  localStorage.setItem("kgipToken", user.token);
  localStorage.setItem("kgipUser", JSON.stringify(user));
}