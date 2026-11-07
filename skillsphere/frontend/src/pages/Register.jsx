import { useState } from "react";
import { Link } from "react-router-dom";
import AuthLayout from "../components/AuthLayout.jsx";
import FormInput from "../components/FormInput.jsx";
import PasswordInput from "../components/PasswordInput.jsx";
import PasswordStrength, { scorePassword } from "../components/PasswordStrength.jsx";
import Alert from "../components/Alert.jsx";
import Spinner from "../components/Spinner.jsx";
import { parseApiError, register } from "../services/authService.js";

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const EMPLOYEE_ID_RE = /^[A-Za-z0-9._/-]{3,30}$/;
const ROLES = [
  { value: "EMPLOYEE", label: "Employee" },
  { value: "MANAGER", label: "Manager" },
  { value: "MENTOR", label: "Mentor" },
  { value: "HR_SPECIALIST", label: "HR Specialist" },
  { value: "DEPARTMENT_HEAD", label: "Department Head" },
  { value: "ADMIN", label: "Administrator" },
];
const EMPTY = { fullName: "", email: "", employeeId: "", role: "EMPLOYEE", password: "", confirmPassword: "" };

export default function Register() {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState("");
  const [success, setSuccess] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const update = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
    setErrors((er) => ({ ...er, [name]: undefined }));
  };

  const validate = () => {
    const er = {};
    if (form.fullName.trim().length < 2) er.fullName = "Enter your full name.";
    if (!form.email.trim()) er.email = "Enter your work email.";
    else if (!EMAIL_RE.test(form.email.trim())) er.email = "Enter a valid email address.";
    if (form.employeeId.trim() && !EMPLOYEE_ID_RE.test(form.employeeId.trim()))
      er.employeeId = "Use 3-30 letters, numbers, or . _ / -";
    if (!form.password) er.password = "Create a password.";
    else if (!scorePassword(form.password).valid)
      er.password = "Use 8+ characters with upper and lower case letters, a number, and a symbol.";
    if (!form.confirmPassword) er.confirmPassword = "Confirm your password.";
    else if (form.confirmPassword !== form.password) er.confirmPassword = "Passwords do not match.";
    return er;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setApiError("");
    setSuccess("");
    const er = validate();
    setErrors(er);
    if (Object.keys(er).length) return;

    setSubmitting(true);
    try {
      const res = await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        employeeId: form.employeeId.trim(),
        role: form.role,
        password: form.password,
        confirmPassword: form.confirmPassword,
      });
      setSuccess(res.message);
      setForm(EMPTY);
    } catch (err) {
      const parsed = parseApiError(err);
      setApiError(parsed.message);
      setErrors(parsed.fieldErrors);
    } finally {
      setSubmitting(false);
    }
  };

  const privileged = form.role !== "EMPLOYEE";

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Join SkillSphere to see where you stand and where to grow."
      wide
      footer={<>Already have an account? <Link to="/login">Sign in</Link></>}
    >
      {success ? (
        <div className="success-panel">
          <Alert type="success">{success}</Alert>
          <Link to="/login" className="btn btn--primary btn--link">Go to sign in</Link>
        </div>
      ) : (
        <form onSubmit={onSubmit} noValidate className="form">
          <Alert type="error">{apiError}</Alert>

          <FormInput label="Full name" name="fullName" autoComplete="name" placeholder="Jordan Rivera"
            value={form.fullName} onChange={update} error={errors.fullName} />

          <FormInput label="Work email" name="email" type="email" autoComplete="email" placeholder="you@company.com"
            value={form.email} onChange={update} error={errors.email} />

          <div className="form__grid">
            <FormInput label="Employee ID" optional name="employeeId" autoComplete="off" placeholder="EMP-1042"
              value={form.employeeId} onChange={update} error={errors.employeeId} />

            <FormInput
              label="User role"
              error={errors.role}
              hint={privileged
                ? "You will start as an Employee. An administrator reviews this request."
                : "Everyone starts as an Employee."}
            >
              {(a11y) => (
                <select {...a11y} name="role" value={form.role} onChange={update}>
                  {ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
                </select>
              )}
            </FormInput>
          </div>

          <PasswordInput label="Password" name="password" autoComplete="new-password" placeholder="Create a password"
            value={form.password} onChange={update} error={errors.password} />
          <PasswordStrength password={form.password} />

          <PasswordInput label="Confirm password" name="confirmPassword" autoComplete="new-password"
            placeholder="Re-enter your password" value={form.confirmPassword} onChange={update}
            error={errors.confirmPassword} />

          <button type="submit" className="btn btn--primary" disabled={submitting} aria-busy={submitting}>
            {submitting ? <><Spinner /> Creating account…</> : "Create account"}
          </button>
        </form>
      )}
    </AuthLayout>
  );
}
