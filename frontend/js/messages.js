/*
 * Messages: a chat icon (separate from the notification bell) plus a slide-in chat panel.
 * Loaded by employee-dashboard.html and trainer-dashboard.html, after guard.js.
 *
 *   Employee  <-> Employee : send a follow request, the other person accepts (like Instagram), then chat
 *   Employee  <-> Mentor   : chat any time, either side can start, no request needed
 *
 * Everything runs against /api/messages (MessagingController). The server decides who may chat with whom;
 * this file only shows what the server allows.
 */
(function () {
  "use strict";

  const API = "http://localhost:8080/api/messages";
  const EMPLOYEE = "EMPLOYEE", MENTOR = "TRAINER_MENTOR";

  function readSession() { try { return JSON.parse(localStorage.getItem("wki_session") || "null"); } catch (_) { return null; } }
  const sess = readSession();
  if (!sess || !sess.token || (sess.role !== EMPLOYEE && sess.role !== MENTOR)) return;
  const isEmployee = sess.role === EMPLOYEE;

  /* ------------------------------------------------------------ helpers */
  const esc = s => String(s == null ? "" : s).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  const initials = n => String(n || "?").trim().split(/\s+/).slice(0, 2).map(w => w[0]).join("").toUpperCase() || "?";
  const $ = (s, r) => (r || document).querySelector(s);

  function when(dt) {
    if (!dt) return "";
    const d = new Date(dt); if (isNaN(d)) return "";
    const now = new Date(), sameDay = d.toDateString() === now.toDateString();
    if (sameDay) return d.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
    const days = Math.floor((now - d) / 86400000);
    if (days < 7) return d.toLocaleDateString([], { weekday: "short" });
    return d.toLocaleDateString([], { day: "numeric", month: "short" });
  }
  function dayLabel(dt) {
    const d = new Date(dt); if (isNaN(d)) return "";
    const now = new Date(), y = new Date(now.getTime() - 86400000);
    if (d.toDateString() === now.toDateString()) return "Today";
    if (d.toDateString() === y.toDateString()) return "Yesterday";
    return d.toLocaleDateString([], { day: "numeric", month: "long", year: "numeric" });
  }

  async function call(path, method, body) {
    const s = readSession();
    if (!s || !s.token) { location.replace("login.html"); throw new Error("Please log in"); }
    let res;
    try {
      res = await fetch(API + path, {
        method: method || "GET",
        headers: Object.assign({ Authorization: "Bearer " + s.token }, body ? { "Content-Type": "application/json" } : {}),
        body: body ? JSON.stringify(body) : undefined
      });
    } catch (_) { throw new Error("Cannot reach the server. Is the backend running on port 8080?"); }
    if (res.status === 401) { try { localStorage.removeItem("wki_session"); } catch (_) {} location.replace("login.html"); throw new Error("Session expired, please log in again"); }
    const data = await res.json().catch(() => null);
    if (!res.ok) throw new Error((data && (data.message || data.error)) || "Request failed (" + res.status + ")");
    return data;
  }

  /* ------------------------------------------------------------ state */
  const S = {
    open: false, tab: "chats", q: "",
    sum: { unread: 0, requests: 0, total: 0 },
    chats: null, people: null, mentors: null, reqs: null,
    thread: null,            // { id, partner, messages: [], lastId }
    err: null, busy: false
  };
  const TABS = isEmployee
    ? [["chats", "Chats"], ["people", "Find people"], ["requests", "Requests"], ["mentors", "Mentors"]]
    : [["chats", "Chats"], ["people", "Employees"]];

  /* ------------------------------------------------------------ styles */
  const css = `
  .wm-btn{position:relative;background:transparent;border:0;color:#fff;cursor:pointer;padding:6px;border-radius:8px;display:inline-flex}
  .wm-btn:hover{background:rgba(255,255,255,.12)}
  .wm-badge{position:absolute;top:-2px;right:-4px;background:#C8372D;color:#fff;border-radius:999px;font-size:11px;padding:1px 6px;font-weight:600;line-height:1.3}
  .wm-scrim{position:fixed;inset:0;background:rgba(16,28,43,.35);z-index:900}
  .wm-panel{position:fixed;top:0;right:0;bottom:0;width:min(420px,100vw);background:#fff;z-index:901;display:flex;flex-direction:column;
    box-shadow:-8px 0 30px rgba(16,28,43,.18);font-family:'IBM Plex Sans','Segoe UI',system-ui,sans-serif;color:#1A2433;font-size:14px}
  .wm-panel *{box-sizing:border-box}
  .wm-panel button,.wm-panel input,.wm-panel textarea{font:inherit;color:inherit}
  .wm-head{display:flex;align-items:center;gap:10px;padding:14px 16px;background:#101C2B;color:#fff}
  .wm-head h2{margin:0;font-size:16px;font-weight:600;flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
  .wm-head .wm-sub{font-size:12px;opacity:.75;font-weight:400;display:block}
  .wm-icon{background:transparent;border:0;color:#fff;cursor:pointer;border-radius:6px;padding:6px 9px;font-size:18px;line-height:1}
  .wm-icon:hover{background:rgba(255,255,255,.14)}
  .wm-tabs{display:flex;border-bottom:1px solid #DDE2E9;background:#F8FAFB}
  .wm-tab{flex:1;background:transparent;border:0;border-bottom:3px solid transparent;padding:11px 4px;cursor:pointer;font-weight:500;color:#5B6778;position:relative;white-space:nowrap}
  .wm-tab:hover{color:#1A2433}
  .wm-tab[aria-selected=true]{color:#095A60;border-bottom-color:#0B6E75;font-weight:600}
  .wm-dot{display:inline-block;background:#C8372D;color:#fff;border-radius:999px;font-size:11px;padding:0 6px;margin-left:5px;font-weight:600}
  .wm-body{flex:1;overflow-y:auto;min-height:0}
  .wm-search{padding:12px 14px;border-bottom:1px solid #DDE2E9}
  .wm-search input{width:100%;padding:9px 11px;border:1px solid #C3CBD6;border-radius:6px;background:#fff}
  .wm-search input:focus,.wm-compose textarea:focus{outline:none;border-color:#0B6E75;box-shadow:0 0 0 3px rgba(11,110,117,.16)}
  .wm-row{display:flex;align-items:center;gap:12px;padding:12px 16px;border-bottom:1px solid #EEF1F5;width:100%;text-align:left;background:#fff;border-left:0;border-right:0;border-top:0}
  button.wm-row{cursor:pointer}
  button.wm-row:hover{background:#F3F7F8}
  .wm-av{flex:none;width:40px;height:40px;border-radius:50%;background:#E3F1F2;color:#095A60;display:flex;align-items:center;justify-content:center;font-weight:600;font-size:14px}
  .wm-av.mentor{background:#FFF1D6;color:#8A5A00}
  .wm-main{flex:1;min-width:0}
  .wm-name{font-weight:600;display:flex;align-items:center;gap:8px}
  .wm-name span.t{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
  .wm-meta{color:#5B6778;font-size:12.5px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
  .wm-meta.wm-unread{color:#1A2433;font-weight:600}
  .wm-time{flex:none;font-size:12px;color:#5B6778;text-align:right}
  .wm-count{background:#0B6E75;color:#fff;border-radius:999px;font-size:11px;padding:1px 7px;font-weight:600;display:inline-block;margin-top:4px}
  .wm-tag{font-size:11px;font-weight:600;border-radius:4px;padding:1px 7px;background:#FFF1D6;color:#8A5A00}
  .wm-tag.ok{background:#E5F3EB;color:#1F7A4D}
  .wm-btnsm{background:#0B6E75;color:#fff;border:0;border-radius:6px;padding:6px 12px;cursor:pointer;font-weight:600;font-size:13px;white-space:nowrap}
  .wm-btnsm:hover{background:#095A60}
  .wm-btnsm:disabled{opacity:.55;cursor:default}
  .wm-btnsm.ghost{background:#fff;color:#0B6E75;border:1px solid #9DB9BC}
  .wm-btnsm.ghost:hover{background:#E3F1F2}
  .wm-btnsm.danger{background:#fff;color:#B3261E;border:1px solid #E5B3AF}
  .wm-acts{display:flex;gap:6px;flex:none}
  .wm-h3{margin:0;padding:12px 16px 6px;font-size:12px;font-weight:600;color:#5B6778;text-transform:uppercase;letter-spacing:.4px}
  .wm-empty{padding:36px 22px;text-align:center;color:#5B6778}
  .wm-err{margin:12px 14px;background:#FBE7E5;color:#B3261E;border-radius:6px;padding:10px 12px}
  .wm-thread{display:flex;flex-direction:column;gap:6px;padding:14px 14px 8px}
  .wm-day{align-self:center;font-size:11.5px;color:#5B6778;background:#EEF1F5;border-radius:999px;padding:2px 10px;margin:8px 0 4px}
  .wm-msg{max-width:82%;padding:8px 12px;border-radius:14px;line-height:1.45;overflow-wrap:anywhere;white-space:pre-wrap}
  .wm-msg small{display:block;font-size:11px;opacity:.7;margin-top:2px;white-space:normal}
  .wm-msg.them{align-self:flex-start;background:#F0F3F6;border-bottom-left-radius:4px}
  .wm-msg.me{align-self:flex-end;background:#0B6E75;color:#fff;border-bottom-right-radius:4px}
  .wm-compose{display:flex;gap:8px;padding:10px 12px;border-top:1px solid #DDE2E9;background:#fff;align-items:flex-end}
  .wm-panel .wm-compose textarea{flex:1;resize:none;height:42px;min-height:42px;max-height:120px;margin:0;padding:9px 11px;border:1px solid #C3CBD6;border-radius:8px}
  .wm-locked{padding:14px 16px;border-top:1px solid #DDE2E9;background:#F8FAFB;display:flex;gap:12px;align-items:center;justify-content:space-between;flex-wrap:wrap}
  .wm-toast{position:fixed;bottom:22px;left:50%;transform:translateX(-50%);background:#1A2433;color:#fff;padding:10px 16px;border-radius:8px;z-index:950;font-size:14px;max-width:90vw}
  .wm-toast.err{background:#B3261E}
  @media(max-width:520px){.wm-panel{width:100vw}}
  `;
  const style = document.createElement("style"); style.textContent = css; document.head.appendChild(style);

  /* ------------------------------------------------------------ header icon */
  const btn = document.createElement("button");
  btn.type = "button"; btn.className = "wm-btn"; btn.id = "msgBtn";
  btn.setAttribute("aria-label", "Messages"); btn.setAttribute("aria-haspopup", "dialog");
  btn.innerHTML = '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 11.5a8.4 8.4 0 0 1-9 8.4 8.7 8.7 0 0 1-3.6-.8L3 21l1.9-5.4A8.4 8.4 0 0 1 3 11.5 8.5 8.5 0 0 1 12 3a8.5 8.5 0 0 1 9 8.5z"/></svg><span class="wm-badge" id="msgBadge" hidden>0</span>';
  const right = document.querySelector("header .right, .top .right");
  if (!right) return;
  const bell = right.querySelector(".bell");
  right.insertBefore(btn, bell || right.firstChild);
  btn.addEventListener("click", () => S.open ? close() : open());

  function paintBadge() {
    const b = $("#msgBadge"), n = S.sum.total || 0;
    b.hidden = !n; b.textContent = n > 99 ? "99+" : n;
    btn.setAttribute("aria-label", n ? "Messages, " + n + " new" : "Messages");
  }
  async function loadSummary() {
    try { S.sum = await call("/summary"); paintBadge(); } catch (_) { /* header badge stays as it was */ }
  }

  /* ------------------------------------------------------------ panel shell */
  let scrim = null, panel = null, lastFocus = null;

  function open() {
    if (S.open) return;
    S.open = true; lastFocus = document.activeElement;
    scrim = document.createElement("div"); scrim.className = "wm-scrim"; scrim.addEventListener("click", close);
    panel = document.createElement("div"); panel.className = "wm-panel";
    panel.setAttribute("role", "dialog"); panel.setAttribute("aria-modal", "true"); panel.setAttribute("aria-label", "Messages");
    document.body.append(scrim, panel);
    btn.setAttribute("aria-expanded", "true");
    S.thread = null; S.err = null; show();
    loadTab();
  }
  function close() {
    if (!S.open) return;
    S.open = false; S.thread = null;
    scrim.remove(); panel.remove(); scrim = panel = null;
    btn.setAttribute("aria-expanded", "false");
    loadSummary();
    if (lastFocus && lastFocus.focus) lastFocus.focus();
  }
  document.addEventListener("keydown", e => { if (e.key === "Escape" && S.open) close(); });

  function show() {
    if (!panel) return;
    panel.innerHTML = S.thread ? threadShell() : homeShell();
    if (S.thread) { paintThread(true); const t = $(".wm-compose textarea", panel); if (t) t.focus(); }
    else { const f = $(".wm-tab[aria-selected=true]", panel); if (f && !panel.contains(document.activeElement)) f.focus({ preventScroll: true }); }
  }

  /* ------------------------------------------------------------ home (tabs) */
  function homeShell() {
    const tabs = TABS.map(([id, label]) => {
      const n = id === "requests" ? S.sum.requests : id === "chats" ? S.sum.unread : 0;
      return `<button class="wm-tab" role="tab" data-wm="tab" data-tab="${id}" aria-selected="${id === S.tab}">${esc(label)}${n ? `<span class="wm-dot">${n}</span>` : ""}</button>`;
    }).join("");
    return `<div class="wm-head"><h2>Messages<span class="wm-sub">${isEmployee ? "Colleagues you follow and all mentors" : "Chat with employees"}</span></h2><button class="wm-icon" data-wm="close" aria-label="Close messages">&times;</button></div>
      <div class="wm-tabs" role="tablist">${tabs}</div>
      <div class="wm-body" id="wmBody">${tabBody()}</div>`;
  }
  function paintBody() {
    const body = $("#wmBody", panel); if (!body) return;
    const q = $("#wmQ", panel), keep = q && document.activeElement === q;   // keep typing in the search box while results refresh
    body.innerHTML = tabBody();
    if (keep) { const n = $("#wmQ", panel); if (n) { n.focus(); n.setSelectionRange(n.value.length, n.value.length); } }
  }
  function paintTabs() {
    $$(".wm-tab").forEach(t => {
      const id = t.dataset.tab, n = id === "requests" ? S.sum.requests : id === "chats" ? S.sum.unread : 0;
      t.setAttribute("aria-selected", id === S.tab);
      t.innerHTML = esc(TABS.find(x => x[0] === id)[1]) + (n ? `<span class="wm-dot">${n}</span>` : "");
    });
  }
  const $$ = s => panel ? Array.from(panel.querySelectorAll(s)) : [];

  const loading = '<div class="wm-empty">Loading…</div>';
  const errBox = () => S.err ? `<div class="wm-err" role="alert">${esc(S.err)}</div>` : "";

  function tabBody() {
    if (S.tab === "chats") return errBox() + chatsHtml();
    if (S.tab === "people") return peopleHtml();
    if (S.tab === "requests") return errBox() + requestsHtml();
    if (S.tab === "mentors") return errBox() + mentorsHtml();
    return "";
  }

  function avatar(name, role) { return `<span class="wm-av${role === MENTOR ? " mentor" : ""}" aria-hidden="true">${esc(initials(name))}</span>`; }

  function chatsHtml() {
    if (!S.chats) return S.err ? "" : loading;
    if (!S.chats.length) return `<div class="wm-empty"><b>No conversations yet</b><p>${isEmployee
      ? "Message a mentor from the Mentors tab, or follow a colleague from Find people. You can chat once they accept."
      : "Open the Employees tab to start a conversation with an employee."}</p></div>`;
    return S.chats.map(c => `<button class="wm-row" data-wm="open" data-id="${Number(c.id)}">${avatar(c.name, c.role)}
      <span class="wm-main"><span class="wm-name"><span class="t">${esc(c.name)}</span>${c.role === MENTOR ? '<span class="wm-tag">Mentor</span>' : ""}</span>
        <span class="wm-meta${Number(c.unread) ? " wm-unread" : ""}">${c.lastBody == null ? "Say hello" : (c.lastMine ? "You: " : "") + esc(c.lastBody)}</span></span>
      <span class="wm-time">${esc(when(c.lastAt))}${Number(c.unread) ? `<br><span class="wm-count">${Number(c.unread)}</span>` : ""}</span></button>`).join("");
  }

  function relationButtons(p) {
    if (!isEmployee) return `<button class="wm-btnsm" data-wm="open" data-id="${Number(p.id)}">Message</button>`;
    switch (p.relation) {
      case "CONNECTED": return `<span class="wm-acts"><button class="wm-btnsm" data-wm="open" data-id="${Number(p.id)}">Message</button></span>`;
      case "REQUESTED": return `<button class="wm-btnsm ghost" data-wm="unfollow" data-id="${Number(p.id)}" title="Cancel your request">Requested</button>`;
      case "INCOMING": return `<span class="wm-acts"><button class="wm-btnsm" data-wm="accept" data-id="${Number(p.id)}">Accept</button><button class="wm-btnsm ghost" data-wm="decline" data-id="${Number(p.id)}">Decline</button></span>`;
      default: return `<button class="wm-btnsm" data-wm="follow" data-id="${Number(p.id)}">Follow</button>`;
    }
  }

  function peopleHtml() {
    const search = `<div class="wm-search"><input type="search" id="wmQ" placeholder="Search employees by name" aria-label="Search employees by name" value="${esc(S.q)}" autocomplete="off"></div>`;
    let list;
    if (!S.people) list = S.err ? "" : loading;
    else if (!S.people.length) list = `<div class="wm-empty">${S.q ? "No employees match “" + esc(S.q) + "”." : "No other employees found yet."}</div>`;
    else list = S.people.map(p => `<div class="wm-row">${avatar(p.name, EMPLOYEE)}
        <span class="wm-main"><span class="wm-name"><span class="t">${esc(p.name)}</span>${p.relation === "CONNECTED" && isEmployee ? '<span class="wm-tag ok">Following</span>' : ""}</span>
          <span class="wm-meta">${esc([p.jobRole, p.department].filter(Boolean).join(" · ") || "Employee")}</span></span>
        ${relationButtons(p)}</div>`).join("");
    return errBox() + search + `<div id="wmList">${list}</div>`;
  }

  function requestsHtml() {
    if (!S.reqs) return S.err ? "" : loading;
    const inc = S.reqs.incoming || [], sent = S.reqs.sent || [];
    if (!inc.length && !sent.length) return `<div class="wm-empty"><b>No pending requests</b><p>Follow requests you send or receive show up here.</p></div>`;
    const row = (p, acts) => `<div class="wm-row">${avatar(p.name, EMPLOYEE)}<span class="wm-main"><span class="wm-name"><span class="t">${esc(p.name)}</span></span>
      <span class="wm-meta">${esc(p.jobRole || "Employee")}${p.createdAt ? " · " + esc(when(p.createdAt)) : ""}</span></span>${acts}</div>`;
    return (inc.length ? `<h3 class="wm-h3">Wants to follow you</h3>` + inc.map(p => row(p,
        `<span class="wm-acts"><button class="wm-btnsm" data-wm="accept" data-id="${Number(p.id)}">Accept</button><button class="wm-btnsm ghost" data-wm="decline" data-id="${Number(p.id)}">Decline</button></span>`)).join("") : "")
      + (sent.length ? `<h3 class="wm-h3">Requests you sent</h3>` + sent.map(p => row(p,
        `<button class="wm-btnsm ghost" data-wm="unfollow" data-id="${Number(p.id)}">Cancel</button>`)).join("") : "");
  }

  function mentorsHtml() {
    if (!S.mentors) return S.err ? "" : loading;
    if (!S.mentors.length) return `<div class="wm-empty"><b>No mentors yet</b><p>Mentors appear here as soon as one registers.</p></div>`;
    return `<h3 class="wm-h3">All mentors · message any of them directly</h3>` + S.mentors.map(m => `<button class="wm-row" data-wm="open" data-id="${Number(m.id)}">${avatar(m.name, MENTOR)}
      <span class="wm-main"><span class="wm-name"><span class="t">${esc(m.name)}</span>${Number(m.accepting) ? "" : '<span class="wm-tag">Not accepting mentees</span>'}</span>
        <span class="wm-meta">${esc(m.headline || "Mentor")}</span></span>
      ${Number(m.unread) ? `<span class="wm-count">${Number(m.unread)}</span>` : ""}</button>`).join("");
  }

  /* ------------------------------------------------------------ data loading */
  async function loadTab() {
    S.err = null;
    const tab = S.tab;
    try {
      if (tab === "chats") S.chats = await call("/conversations");
      else if (tab === "people") S.people = await call("/people?q=" + encodeURIComponent(S.q));
      else if (tab === "requests") S.reqs = await call("/requests");
      else if (tab === "mentors") S.mentors = await call("/mentors");
      await loadSummary();
    } catch (e) { S.err = e.message; }
    if (S.open && !S.thread && S.tab === tab) { paintBody(); paintTabs(); }
  }

  function flash(msg, isErr) {
    const t = document.createElement("div"); t.className = "wm-toast" + (isErr ? " err" : ""); t.setAttribute("role", "status"); t.textContent = msg;
    document.body.appendChild(t); setTimeout(() => t.remove(), 3200);
  }

  async function act(fn, okMsg) {
    try { await fn(); if (okMsg) flash(okMsg); }
    catch (e) { flash(e.message, true); }
    if (S.thread) { await openThread(S.thread.id, true); }
    else { await loadTab(); }
  }

  /* ------------------------------------------------------------ one conversation */
  function threadShell() {
    const p = S.thread.partner || {};
    const sub = p.role === MENTOR ? "Mentor" : "Employee";
    return `<div class="wm-head"><button class="wm-icon" data-wm="back" aria-label="Back to messages">&#8592;</button>
        <h2>${esc(p.name || "Loading…")}<span class="wm-sub">${sub}</span></h2><button class="wm-icon" data-wm="close" aria-label="Close messages">&times;</button></div>
      <div class="wm-body" id="wmThread" role="log" aria-live="polite" aria-label="Conversation"></div>
      <div id="wmFoot"></div>`;
  }

  function paintThread(forceBottom) {
    const box = $("#wmThread", panel), foot = $("#wmFoot", panel); if (!box) return;
    const T = S.thread, p = T.partner || {};
    const atBottom = forceBottom || box.scrollHeight - box.scrollTop - box.clientHeight < 80;
    if (!T.messages.length) {
      box.innerHTML = `<div class="wm-empty">${p.canChat ? "No messages yet. Say hello!" : ""}</div>`;
    } else {
      let day = "", html = "";
      T.messages.forEach(m => {
        const dl = dayLabel(m.createdAt);
        if (dl !== day) { day = dl; html += `<div class="wm-day">${esc(dl)}</div>`; }
        html += `<div class="wm-msg ${m.mine ? "me" : "them"}">${esc(m.body)}<small>${esc(when(m.createdAt))}${m.mine && m.readAt ? " · Seen" : ""}</small></div>`;
      });
      box.innerHTML = `<div class="wm-thread">${html}</div>`;
    }
    // footer: composer when allowed, otherwise explain why not
    const hadFocus = foot.contains(document.activeElement), draft = $("textarea", foot) ? $("textarea", foot).value : "";
    if (p.canChat) {
      if (!$(".wm-compose", foot)) {
        foot.innerHTML = `<form class="wm-compose" data-wm-form="send"><textarea name="body" rows="1" maxlength="2000" required aria-label="Type a message" placeholder="Type a message"></textarea><button class="wm-btnsm" type="submit">Send</button></form>`;
        if (hadFocus || forceBottom) $("textarea", foot).focus();
      }
      const ta = $("textarea", foot); if (ta && draft && !ta.value) ta.value = draft;
    } else {
      let msg, btnHtml = "";
      if (p.relation === "REQUESTED") { msg = "Your follow request is waiting. You can chat as soon as " + esc(p.name) + " accepts."; btnHtml = `<button class="wm-btnsm ghost" data-wm="unfollow" data-id="${Number(p.id)}">Cancel request</button>`; }
      else if (p.relation === "INCOMING") { msg = esc(p.name) + " sent you a follow request."; btnHtml = `<span class="wm-acts"><button class="wm-btnsm" data-wm="accept" data-id="${Number(p.id)}">Accept</button><button class="wm-btnsm ghost" data-wm="decline" data-id="${Number(p.id)}">Decline</button></span>`; }
      else { msg = "Follow " + esc(p.name) + " and wait for them to accept to start chatting."; btnHtml = `<button class="wm-btnsm" data-wm="follow" data-id="${Number(p.id)}">Follow</button>`; }
      foot.innerHTML = `<div class="wm-locked"><span>${msg}</span>${btnHtml}</div>`;
    }
    if (atBottom) box.scrollTop = box.scrollHeight;
  }

  async function openThread(id, keepScroll) {
    try {
      const r = await call("/with/" + id);
      const last = r.messages.length ? r.messages[r.messages.length - 1].id : 0;
      S.thread = { id, partner: r.partner, messages: r.messages, lastId: last };
      S.err = null;
      if (!$("#wmThread", panel)) show(); else { $(".wm-head h2", panel).firstChild.textContent = r.partner.name; paintThread(!keepScroll); }
      loadSummary();
    } catch (e) {
      flash(e.message, true);
      if (!S.thread) { S.thread = null; show(); }
    }
  }

  /* poll: new messages in the open chat every 4s, badge/list every 15s */
  let tick = 0;
  setInterval(async () => {
    if (document.hidden) return;
    tick++;
    if (S.open && S.thread) {
      try {
        const id = S.thread.id, r = await call("/with/" + id + "?after=" + S.thread.lastId);
        if (S.thread && S.thread.id === id) {
          const fresh = r.messages.filter(m => !S.thread.messages.some(x => x.id === m.id));
          const roleChanged = JSON.stringify(r.partner) !== JSON.stringify(S.thread.partner);
          if (fresh.length || roleChanged) {
            S.thread.partner = r.partner;
            S.thread.messages = S.thread.messages.concat(fresh);
            if (fresh.length) S.thread.lastId = fresh[fresh.length - 1].id;
            const foot = $("#wmFoot", panel); if (roleChanged && foot) foot.innerHTML = "";
            paintThread(false);
          }
        }
      } catch (_) {}
    }
    if (tick % 4 === 0) {
      await loadSummary();
      if (S.open && !S.thread && document.activeElement !== $("#wmQ", panel)) loadTab();
    }
  }, 4000);
  // outgoing "seen" ticks refresh with the poll above only when new messages arrive; reload the thread when the tab becomes visible again
  document.addEventListener("visibilitychange", () => { if (!document.hidden) { loadSummary(); if (S.open && S.thread) openThread(S.thread.id, true); } });

  /* ------------------------------------------------------------ events */
  document.addEventListener("click", e => {
    const el = e.target.closest("[data-wm]"); if (!el || !panel || !panel.contains(el)) return;
    const id = Number(el.dataset.id);
    switch (el.dataset.wm) {
      case "close": close(); break;
      case "back": S.thread = null; show(); loadTab(); break;
      case "tab": S.tab = el.dataset.tab; S.err = null; paintTabs(); paintBody(); loadTab(); break;
      case "open": S.thread = { id, partner: {}, messages: [], lastId: 0 }; show(); openThread(id); break;
      case "follow": act(() => call("/follow/" + id, "POST"), "Follow request sent"); break;
      case "accept": act(() => call("/follow/" + id + "/accept", "PUT"), "Accepted. You can chat now."); break;
      case "decline": act(() => call("/follow/" + id + "/decline", "PUT"), "Request declined"); break;
      case "unfollow": act(() => call("/follow/" + id, "DELETE"), "Done"); break;
    }
  });

  let qTimer = null;
  document.addEventListener("input", e => {
    if (e.target && e.target.id === "wmQ") {
      S.q = e.target.value; clearTimeout(qTimer); qTimer = setTimeout(loadTab, 250);
    }
    if (e.target && e.target.matches && e.target.matches(".wm-compose textarea")) {
      e.target.style.height = "42px"; e.target.style.height = Math.min(e.target.scrollHeight, 120) + "px";
    }
  });
  document.addEventListener("keydown", e => {
    if (e.key === "Enter" && !e.shiftKey && e.target.matches && e.target.matches(".wm-compose textarea")) { e.preventDefault(); e.target.form.requestSubmit(); }
  });
  document.addEventListener("submit", async e => {
    const f = e.target.closest("form[data-wm-form]"); if (!f) return;
    e.preventDefault();
    const ta = f.elements.body, text = ta.value.trim(); if (!text || !S.thread) return;
    const submit = f.querySelector("button[type=submit]"); submit.disabled = true;
    try {
      const m = await call("/with/" + S.thread.id, "POST", { body: text });
      ta.value = ""; ta.style.height = "42px";
      if (!S.thread.messages.some(x => x.id === m.id)) S.thread.messages.push(m);
      S.thread.lastId = Math.max(S.thread.lastId, m.id);
      paintThread(true);
    } catch (err) { flash(err.message, true); }
    submit.disabled = false; ta.focus();
  });

  /* ------------------------------------------------------------ start */
  loadSummary();
})();
