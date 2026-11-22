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
    DEPT_HEAD: "dept-head-dashboard.html",
  };

  const ROLE_LABELS = {
    ADMIN: "Administrator",
    HR_MANAGER: "HR Specialist",
    MANAGER: "Team Manager",
    EMPLOYEE: "Employee",
    TRAINER_MENTOR: "Mentor / Trainer",
    TRAINING_COORDINATOR: "Training Coordinator",
    DEPT_HEAD: "Department Head",
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

  // Compute Initials
  function getInitials(name) {
    if (!name) return "U";
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  const initials = getInitials(session.fullName);
  const roleTitle = ROLE_LABELS[session.role] || session.role || "User";

  // Update text & avatars
  const uName = document.getElementById("userName");
  if (uName) uName.textContent = session.fullName;
  const uEmail = document.getElementById("userEmail");
  if (uEmail) uEmail.textContent = session.email;

  const uAvatar = document.getElementById("userAvatar");
  if (uAvatar) uAvatar.textContent = initials;
  const dAvatar = document.getElementById("dropdownAvatar");
  if (dAvatar) dAvatar.textContent = initials;
  const dName = document.getElementById("dropdownName");
  if (dName) dName.textContent = session.fullName;
  const dEmail = document.getElementById("dropdownEmail");
  if (dEmail) dEmail.textContent = session.email;
  const dRole = document.getElementById("dropdownRole");
  if (dRole) dRole.textContent = roleTitle;

  // Wire up profile dropdown menu
  const menuBtn = document.getElementById("userMenuBtn");
  const dropdown = document.getElementById("userDropdown");
  if (menuBtn && dropdown) {
    menuBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      const isHidden = dropdown.hasAttribute("hidden");
      if (isHidden) {
        dropdown.removeAttribute("hidden");
        menuBtn.setAttribute("aria-expanded", "true");
      } else {
        dropdown.setAttribute("hidden", "");
        menuBtn.setAttribute("aria-expanded", "false");
      }
    });

    document.addEventListener("click", (e) => {
      if (!dropdown.contains(e.target) && !menuBtn.contains(e.target)) {
        dropdown.setAttribute("hidden", "");
        menuBtn.setAttribute("aria-expanded", "false");
      }
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && !dropdown.hasAttribute("hidden")) {
        dropdown.setAttribute("hidden", "");
        menuBtn.setAttribute("aria-expanded", "false");
        menuBtn.focus();
      }
    });
  }

  // Logout actions
  const doLogout = () => {
    localStorage.removeItem("wki_session");
    window.location.href = "login.html";
  };

  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) logoutBtn.addEventListener("click", doLogout);
  const logoutDropdownBtn = document.getElementById("logoutDropdownBtn");
  if (logoutDropdownBtn) logoutDropdownBtn.addEventListener("click", doLogout);

  // Sidebar drawer initialization for responsive navigation
  const initSidebarDrawer = () => {
    const nav = document.getElementById("nav");
    if (!nav) return;

    let toggleBtn = document.getElementById("sidebarToggleBtn");
    if (!toggleBtn) {
      const brand = document.querySelector(".top .brand, .topbar .brand");
      if (brand) {
        toggleBtn = document.createElement("button");
        toggleBtn.className = "sidebar-toggle-btn";
        toggleBtn.id = "sidebarToggleBtn";
        toggleBtn.setAttribute("aria-label", "Toggle navigation drawer");
        toggleBtn.setAttribute("aria-expanded", "false");
        toggleBtn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>`;
        brand.insertBefore(toggleBtn, brand.firstChild);
      }
    }

    let backdrop = document.querySelector(".sidebar-backdrop");
    if (!backdrop) {
      backdrop = document.createElement("div");
      backdrop.className = "sidebar-backdrop";
      document.body.appendChild(backdrop);
    }

    const setDrawer = (open) => {
      document.body.classList.toggle("sidebar-open", open);
      if (toggleBtn) toggleBtn.setAttribute("aria-expanded", open ? "true" : "false");
    };

    if (toggleBtn) {
      toggleBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        setDrawer(!document.body.classList.contains("sidebar-open"));
      });
    }

    backdrop.addEventListener("click", () => setDrawer(false));

    document.addEventListener("click", (e) => {
      const item = e.target.closest("nav#nav button, nav#nav a");
      if (item && window.innerWidth <= 992) {
        setDrawer(false);
      }
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && document.body.classList.contains("sidebar-open")) {
        setDrawer(false);
        if (toggleBtn) toggleBtn.focus();
      }
    });

    window.addEventListener("resize", () => {
      if (window.innerWidth > 992 && document.body.classList.contains("sidebar-open")) {
        setDrawer(false);
      }
    });
  };

  // Desktop Collapsible Sidebar (Compact Icon-Only Mode)
  const initSidebarCollapse = () => {
    const nav = document.getElementById("nav");
    if (!nav) return;

    // Restore saved compact state on desktop
    const isCollapsed = localStorage.getItem("wki_sidebar_collapsed") === "true";
    if (isCollapsed && window.innerWidth > 992) {
      document.body.classList.add("sidebar-collapsed");
    }

    // Auto-populate data-tooltip attributes for compact icon hover states
    const updateTooltips = () => {
      const items = nav.querySelectorAll("button:not(#sidebarCollapseBtn), a");
      items.forEach(el => {
        if (!el.getAttribute("data-tooltip")) {
          const span = el.querySelector("span:not(.count):not(.cnt):not(.nav-badge)");
          const text = (span ? span.textContent : el.textContent).trim();
          if (text) el.setAttribute("data-tooltip", text);
        }
      });
    };

    // Inject Collapse Toggle Button at the bottom of the sidebar
    const ensureFooter = () => {
      let footer = nav.querySelector(".sidebar-footer");
      if (!footer) {
        footer = document.createElement("div");
        footer.className = "sidebar-footer";
        const isCollapsedNow = document.body.classList.contains("sidebar-collapsed");
        footer.innerHTML = `
          <button type="button" class="sidebar-collapse-toggle" id="sidebarCollapseBtn" title="Collapse / Expand sidebar (Ctrl+B)" aria-label="Toggle compact sidebar" aria-expanded="${isCollapsedNow ? 'false' : 'true'}">
            <svg class="collapse-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="11 17 6 12 11 7"/><polyline points="18 17 13 12 18 7"/></svg>
            <span class="collapse-label">Collapse</span>
          </button>
        `;
        nav.appendChild(footer);

        const collapseBtn = footer.querySelector("#sidebarCollapseBtn");
        if (collapseBtn) {
          collapseBtn.addEventListener("click", () => {
            const nowCollapsed = document.body.classList.toggle("sidebar-collapsed");
            localStorage.setItem("wki_sidebar_collapsed", nowCollapsed ? "true" : "false");
            collapseBtn.setAttribute("aria-expanded", nowCollapsed ? "false" : "true");
            window.dispatchEvent(new Event("resize"));
          });
        }
      }
    };

    updateTooltips();
    ensureFooter();

    // Re-run tooltip population and ensure footer if nav content updates dynamically
    const observer = new MutationObserver(() => {
      observer.disconnect();
      updateTooltips();
      ensureFooter();
      observer.observe(nav, { childList: true });
    });
    observer.observe(nav, { childList: true });

    // Keyboard shortcut: Ctrl + B or Cmd + B
    document.addEventListener("keydown", (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "b" && window.innerWidth > 992) {
        e.preventDefault();
        const nowCollapsed = document.body.classList.toggle("sidebar-collapsed");
        localStorage.setItem("wki_sidebar_collapsed", nowCollapsed ? "true" : "false");
        if (collapseBtn) collapseBtn.setAttribute("aria-expanded", nowCollapsed ? "false" : "true");
        window.dispatchEvent(new Event("resize"));
      }
    });
  };

  initSidebarDrawer();
  initSidebarCollapse();

  const appEl = document.getElementById("app");
  if (appEl) appEl.hidden = false;
})();
