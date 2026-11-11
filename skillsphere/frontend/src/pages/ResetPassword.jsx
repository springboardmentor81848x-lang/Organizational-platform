import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import AuthLayout from "../components/AuthLayout.jsx";
import PasswordInput from "../components/PasswordInput.jsx";
import PasswordStrength, { scorePassword } from "../components/PasswordStrength.jsx";
import Alert from "../components/Alert.jsx";
import Spinner from "../components/Spinner.jsx";
import { parseApiError, resetPassword } from "../services/authService.js";

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get("token") || "";
  const navigate = useNavigate();

  const [form, setForm] = useState({ password: "", confirmPassword: "" });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const update = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
    setErrors((er) => ({ ...er, [e.target.name]: undefined }));
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError("");
    const er = {};
    if (!scorePassword(form.password).valid)
      er.password = "Use 8+ characters with upper and lower case letters, a number, and a symbol.";
    if (form.confirmPassword !== form.password) er.confirmPassword = "Passwords do not match.";
    setErrors(er);
    if (Object.keys(er).length) return;

    setSubmitting(true);
    try {
      const res = await resetPassword({ token, ...form });
      navigate("/login", { replace: true, state: { notice: res.message } });
    } catch (err) {
      const parsed = parseApiError(err);
      setApiError(parsed.message);
      setErrors(parsed.fieldErrors);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout
      title="Choose a new password"
      subtitle="Pick something you haven't used on SkillSphere before."
      footer={<Link to="/login">Back to sign in</Link>}
    >
      {!token ? (
        <Alert type="error">This reset link is incomplete. <Link to="/forgot-password">Request a new one.</Link></Alert>
      ) : (
        <form onSubmit={onSubmit} noValidate className="form">
          <Alert type="error">{apiError}</Alert>
          <PasswordInput label="New password" name="password" autoComplete="new-password" value={form.password}
            onChange={update} error={errors.password} />
          <PasswordStrength password={form.password} />
          <PasswordInput label="Confirm new password" name="confirmPassword" autoComplete="new-password"
            value={form.confirmPassword} onChange={update} error={errors.confirmPassword} />
          <button type="submit" className="btn btn--primary" disabled={submitting} aria-busy={submitting}>
            {submitting ? <><Spinner /> Updating…</> : "Update password"}
          </button>
        </form>
      )}
    </AuthLayout>
  );
}
