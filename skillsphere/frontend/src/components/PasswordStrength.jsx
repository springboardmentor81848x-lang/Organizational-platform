export function scorePassword(pw) {
  const checks = {
    length: pw.length >= 8,
    lower: /[a-z]/.test(pw),
    upper: /[A-Z]/.test(pw),
    number: /\d/.test(pw),
    symbol: /[^A-Za-z0-9]/.test(pw),
  };
  const score = Object.values(checks).filter(Boolean).length;
  return { checks, score, valid: score === 5 && pw.length <= 72 };
}

const LABELS = ["Too short", "Weak", "Weak", "Fair", "Good", "Strong"];
const RULES = [
  ["length", "8+ characters"],
  ["lower", "lowercase letter"],
  ["upper", "uppercase letter"],
  ["number", "number"],
  ["symbol", "symbol"],
];

export default function PasswordStrength({ password }) {
  if (!password) return null;
  const { checks, score } = scorePassword(password);
  return (
    <div className="strength" aria-live="polite">
      <div className="strength__bar" aria-hidden="true">
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n} className={n <= score ? `on s${score}` : ""} />
        ))}
      </div>
      <p className="strength__label">Password strength: <strong>{LABELS[score]}</strong></p>
      <ul className="strength__rules">
        {RULES.map(([key, text]) => (
          <li key={key} className={checks[key] ? "ok" : ""}>
            <span aria-hidden="true">{checks[key] ? "✓" : "○"}</span> {text}
          </li>
        ))}
      </ul>
    </div>
  );
}
