import { useState } from "react";
import FormInput from "./FormInput.jsx";

export default function PasswordInput({ label = "Password", error, hint, ...inputProps }) {
  const [visible, setVisible] = useState(false);
  return (
    <FormInput label={label} error={error} hint={hint}>
      {(a11y) => (
        <div className="password-wrap">
          <input {...a11y} {...inputProps} type={visible ? "text" : "password"} />
          <button
            type="button"
            className="password-toggle"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? "Hide password" : "Show password"}
            aria-pressed={visible}
          >
            {visible ? (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M17.9 17.9A10.9 10.9 0 0 1 12 20C5 20 1 12 1 12a18.5 18.5 0 0 1 5.1-5.9M9.9 4.2A10.9 10.9 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.2 3.2M1 1l22 22M14.1 14.1a3 3 0 1 1-4.2-4.2" />
              </svg>
            ) : (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            )}
          </button>
        </div>
      )}
    </FormInput>
  );
}
