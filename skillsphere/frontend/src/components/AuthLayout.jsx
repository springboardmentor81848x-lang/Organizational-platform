import IllustrationPanel from "./IllustrationPanel.jsx";
import Logo from "./Logo.jsx";

export default function AuthLayout({ title, subtitle, children, footer, wide = false }) {
  return (
    <div className="auth-page">
      <div className="auth-shell">
        <IllustrationPanel />
        <main className="auth-main">
          <div className={`auth-card${wide ? " auth-card--wide" : ""}`}>
            <div className="auth-card__logo"><Logo /></div>
            <header className="auth-card__head">
              <h1>{title}</h1>
              {subtitle && <p>{subtitle}</p>}
            </header>
            {children}
            {footer && <p className="auth-card__footer">{footer}</p>}
          </div>
        </main>
      </div>
    </div>
  );
}
