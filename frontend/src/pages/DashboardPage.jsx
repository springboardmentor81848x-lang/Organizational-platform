import { useNavigate } from "react-router-dom";

export default function DashboardPage() {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem("kgipUser") || "null");

  if (!user) {
    navigate("/login", { replace: true });
    return null;
  }

  function signOut() {
    localStorage.removeItem("kgipToken");
    localStorage.removeItem("kgipUser");
    navigate("/login", { replace: true });
  }

  return (
    <main className="dashboard-shell">
      <nav className="topbar">
        <span className="wordmark">
          KGIP<span>.</span>
        </span>
        <button className="quiet-button" onClick={signOut}>
          Sign out
        </button>
      </nav>
      <section className="dashboard-content">
        <p className="eyebrow">Workspace / Overview</p>
        <h1>Welcome back, {user.firstName || user.email}.</h1>
        <p className="dashboard-copy">
          Your knowledge intelligence workspace is ready to take shape.
        </p>
        <div className="dashboard-grid">
          <article>
            <span className="metric">01</span>
            <h2>Knowledge map</h2>
            <p>See where expertise lives across your organization.</p>
          </article>
          <article>
            <span className="metric">02</span>
            <h2>Open gaps</h2>
            <p>Surface the areas that need attention next.</p>
          </article>
        </div>
      </section>
    </main>
  );
}
