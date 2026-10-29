// ===== Config =====
const API_BASE = "http://localhost:8080/api";

// Where each role lands after login
const ROLE_HOME = {
  ADMIN: "admin-dashboard.html",
  HR_MANAGER: "hr-dashboard.html",
  MANAGER: "manager-dashboard.html",
  EMPLOYEE: "employee-dashboard.html",
  TRAINER_MENTOR: "trainer-dashboard.html",
  TRAINING_COORDINATOR: "coordinator-dashboard.html",
};

// ===== Helpers =====
function getSession() {
  try { return JSON.parse(localStorage.getItem("wki_session") || "null"); }
  catch { return null; }
}
function saveSession(s) { localStorage.setItem("wki_session", JSON.stringify(s)); }
function clearSession() { localStorage.removeItem("wki_session"); }

async function api(path, { method = "GET", body, auth = false } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (auth) {
    const s = getSession();
    if (s && s.token) headers["Authorization"] = "Bearer " + s.token;
  }
  let res;
  try {
    res = await fetch(API_BASE + path, {
      method, headers, body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error("Cannot reach the server. Is the backend running on port 8080?");
  }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const err = new Error(data.message || "Something went wrong");
    err.status = res.status;
    throw err;
  }
  return data;
}

// If already logged in, skip the login/register pages
(function redirectIfLoggedIn() {
  const onAuthPage = document.getElementById("loginForm") || document.getElementById("registerForm");
  const s = getSession();
  if (onAuthPage && s && s.token && ROLE_HOME[s.role]) {
    window.location.replace(ROLE_HOME[s.role]);
  }
})();

// Show / hide password
document.querySelectorAll(".eye").forEach((btn) => {
  btn.addEventListener("click", () => {
    const input = document.getElementById(btn.dataset.target);
    input.type = input.type === "password" ? "text" : "password";
  });
});

const msg = document.getElementById("msg");
function show(text, ok = false) {
  if (!msg) return;
  msg.textContent = text;
  msg.className = "msg " + (ok ? "ok" : "error");
}

// ===== Login =====
const loginForm = document.getElementById("loginForm");
if (loginForm) {
  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    if (!email || !password) return show("Please enter your email and password");

    const btn = loginForm.querySelector(".btn");
    btn.disabled = true;
    try {
      const data = await api("/auth/login", { method: "POST", body: { email, password } });
      saveSession({
        token: data.token, role: data.role, fullName: data.fullName,
        email: data.email, employeeId: data.employeeId,
      });
      window.location.href = ROLE_HOME[data.role] || "employee-dashboard.html";
    } catch (err) {
      show(err.message);
    } finally {
      btn.disabled = false;
    }
  });
}

// ===== Register =====
const registerForm = document.getElementById("registerForm");
if (registerForm) {
  registerForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const fullName = document.getElementById("fullName").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const confirmPassword = document.getElementById("confirmPassword").value;
    const roleEl = document.querySelector("input[name=role]:checked");

    if (!fullName || !email || !password || !confirmPassword) return show("Please fill all required fields");
    if (!/^\S+@\S+\.\S+$/.test(email)) return show("Enter a valid email address");
    if (password.length < 8) return show("Password must be at least 8 characters");
    if (password !== confirmPassword) return show("Passwords do not match");
    if (!roleEl) return show("Please select a role");

    const btn = registerForm.querySelector(".btn");
    btn.disabled = true;
    try {
      await api("/auth/register", {
        method: "POST",
        body: { fullName, email, password, confirmPassword, role: roleEl.value },
      });
      show("Registration successful! Redirecting to login...", true);
      setTimeout(() => (window.location.href = "login.html"), 1500);
    } catch (err) {
      show(err.message);
    } finally {
      btn.disabled = false;
    }
  });
}
