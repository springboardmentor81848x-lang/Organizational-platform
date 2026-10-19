import { Link } from "react-router-dom";

export default function AuthLayout({ children, title, description, mode }) {
  return (
    <main className="auth-shell">
      <section className="brand-panel">
        <div className="brand-mark">KG</div>
        <p className="eyebrow">Knowledge Gap Intelligence Platform</p>
        <h1>Make the unknown actionable.</h1>
        <p className="brand-copy">
          Build a clearer view of what your teams know, need, and can share.
        </p>
        <div className="brand-note">Organizational intelligence / 02</div>
      </section>
      <section className="form-panel">
        <div className="form-heading">
          <p className="eyebrow">Welcome to KGIP</p>
          <h2>{title}</h2>
          <p>{description}</p>
        </div>
        {children}
        <p className="switch-copy">
          {mode === "login" ? "New to KGIP?" : "Already have an account?"}{" "}
          <Link to={mode === "login" ? "/register" : "/login"}>
            {mode === "login" ? "Create an account" : "Sign in"}
          </Link>
        </p>
      </section>
    </main>
  );
}
