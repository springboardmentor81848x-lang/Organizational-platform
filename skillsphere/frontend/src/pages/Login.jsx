import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import AuthLayout from "../components/AuthLayout.jsx";
import FormInput from "../components/FormInput.jsx";
import PasswordInput from "../components/PasswordInput.jsx";
import Alert from "../components/Alert.jsx";
import Spinner from "../components/Spinner.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { dashboardPath, parseApiError } from "../services/authService.js";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function Login() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({ email: "", password: "", rememberMe: false });
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const notice = location.state?.notice;

  const update = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((f) => ({ ...f, [name]: type === "checkbox" ? checked : value }));
    setErrors((er) => ({ ...er, [name]: undefined }));
  };

  const validate = () => {
    const er = {};
    if (!form.email.trim()) er.email = "Enter your email address.";
    else if (!EMAIL_RE.test(form.email.trim())) er.email = "Enter a valid email address.";
    if (!form.password) er.password = "Enter your password.";
    return er;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError("");
    const er = validate();
    setErrors(er);
    if (Object.keys(er).length) return;

    setSubmitting(true);
    try {
      const user = await signIn({
        email: form.email.trim(),
        password: form.password,
        rememberMe: form.rememberMe,
      });
      navigate(dashboardPath(user.role), { replace: true });
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
      title="Welcome Back!"
      subtitle="Sign in to continue to SkillSphere."
      footer={<>New to SkillSphere? <Link to="/register">Create an account</Link></>}
    >
      <form onSubmit={onSubmit} noValidate className="form">
        <Alert type="success">{notice}</Alert>
        <Alert type="error">{apiError}</Alert>

        <FormInput
          label="Email address"
          name="email"
          type="email"
          autoComplete="email"
          placeholder="you@company.com"
          value={form.email}
          onChange={update}
          error={errors.email}
        />
        <PasswordInput
          label="Password"
          name="password"
          autoComplete="current-password"
          placeholder="Enter your password"
          value={form.password}
          onChange={update}
          error={errors.password}
        />

        <div className="form__row">
          <label className="check">
            <input type="checkbox" name="rememberMe" checked={form.rememberMe} onChange={update} />
            <span>Remember me</span>
          </label>
          <Link to="/forgot-password" className="link">Forgot password?</Link>
        </div>

        <button type="submit" className="btn btn--primary" disabled={submitting} aria-busy={submitting}>
          {submitting ? <><Spinner /> Signing in…</> : "Sign In"}
        </button>
      </form>
    </AuthLayout>
  );
}
