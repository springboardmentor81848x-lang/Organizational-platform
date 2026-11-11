import { useState } from "react";
import { Link } from "react-router-dom";
import AuthLayout from "../components/AuthLayout.jsx";
import FormInput from "../components/FormInput.jsx";
import Alert from "../components/Alert.jsx";
import Spinner from "../components/Spinner.jsx";
import { forgotPassword, parseApiError } from "../services/authService.js";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [apiError, setApiError] = useState("");
  const [done, setDone] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError("");
    if (!EMAIL_RE.test(email.trim())) {
      setError("Enter a valid email address.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      const res = await forgotPassword(email.trim());
      setDone(res.message);
    } catch (err) {
      setApiError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      title="Reset your password"
      subtitle="Enter your work email and we'll send you a reset link."
      footer={<Link to="/login">Back to sign in</Link>}
    >
      {done ? (
        <Alert type="success">{done}</Alert>
      ) : (
        <form onSubmit={onSubmit} noValidate className="form">
          <Alert type="error">{apiError}</Alert>
          <FormInput label="Email address" type="email" name="email" autoComplete="email" placeholder="you@company.com"
            value={email} onChange={(e) => { setEmail(e.target.value); setError(""); }} error={error} />
          <button type="submit" className="btn btn--primary" disabled={submitting} aria-busy={submitting}>
            {submitting ? <><Spinner /> Sending…</> : "Send reset link"}
          </button>
        </form>
      )}
    </AuthLayout>
  );
}
