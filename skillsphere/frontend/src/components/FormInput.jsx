import { useId } from "react";

/** Labelled text input with accessible error + hint wiring. */
export default function FormInput({ label, error, hint, optional = false, children, ...inputProps }) {
  const id = useId();
  const errId = `${id}-err`;
  const hintId = `${id}-hint`;
  const describedBy = [error ? errId : null, hint ? hintId : null].filter(Boolean).join(" ") || undefined;

  return (
    <div className={`field${error ? " field--error" : ""}`}>
      <label htmlFor={id}>
        {label}
        {optional && <span className="field__optional"> (optional)</span>}
      </label>
      {children ? (
        children({ id, "aria-invalid": error ? "true" : "false", "aria-describedby": describedBy })
      ) : (
        <input id={id} aria-invalid={error ? "true" : "false"} aria-describedby={describedBy} {...inputProps} />
      )}
      {hint && !error && <p id={hintId} className="field__hint">{hint}</p>}
      {error && <p id={errId} className="field__error" role="alert">{error}</p>}
    </div>
  );
}
