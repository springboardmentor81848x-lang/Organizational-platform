// Loaded by every dashboard page. The <body data-role="..."> says who may view it.
(async function guard() {
  const API_BASE = "http://localhost:8080/api";
  const ROLE_HOME = {
    ADMIN: "admin-dashboard.html",
    HR_MANAGER: "hr-dashboard.html",
    MANAGER: "manager-dashboard.html",
    EMPLOYEE: "employee-dashboard.html",
    TRAINER_MENTOR: "trainer-dashboard.html",
    TRAINING_COORDINATOR: "coordinator-dashboard.html",
  };

  let session = null;
  try { session = JSON.parse(localStorage.getItem("wki_session") || "null"); } catch {}
  if (!session || !session.token) return (window.location.replace("login.html"));

  // Wrong role for this page -> send them to their own dashboard
  const required = document.body.dataset.role;
  if (required && session.role !== required) {
    return window.location.replace(ROLE_HOME[session.role] || "login.html");
  }

  // Ask the server to verify the token (catches expired / tampered tokens)
  try {
    const res = await fetch(API_BASE + "/me", {
      headers: { Authorization: "Bearer " + session.token },
    });
    if (!res.ok) throw new Error("unauthorised");
    const me = await res.json();
    if (required && me.role !== required) throw new Error("role mismatch");
  } catch {
    localStorage.removeItem("wki_session");
    return window.location.replace("login.html");
  }

  document.getElementById("userName").textContent = session.fullName;
  document.getElementById("userEmail").textContent = session.email;
  document.getElementById("app").hidden = false;

  document.getElementById("logoutBtn").addEventListener("click", () => {
    localStorage.removeItem("wki_session");
    window.location.href = "login.html";
  });
})();
