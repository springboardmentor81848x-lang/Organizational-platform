import { useState } from "react";
import { useNavigate } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { request } from "../services/api";

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    role: "ROLE_EMPLOYEE",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const update = (event) =>
    setForm({ ...form, [event.target.name]: event.target.value });

  async function submit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      await request("/auth/register", {
        method: "POST",
        body: JSON.stringify(form),
      });
      navigate("/login", {
        state: { message: "Account created. Sign in to continue." },
      });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout
      mode="register"
      title="Create your account"
      description="Set up your profile and start mapping organizational knowledge."
    >
      <form className="auth-form" onSubmit={submit}>
        <div className="field-grid">
          <label>
            First name
            <input
              name="firstName"
              value={form.firstName}
              onChange={update}
              required
            />
          </label>
          <label>
            Last name
            <input
              name="lastName"
              value={form.lastName}
              onChange={update}
              required
            />
          </label>
        </div>
        <label>
          Email address
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={update}
            required
          />
        </label>
        <label>
          Register as
          <select
            className="mt-1 w-full cursor-pointer rounded-xl border border-[#294a6b] bg-[#102945] px-4 py-3 text-[#f3f7fb] outline-none transition focus:border-[#72c8e8]"
            name="role"
            value={form.role}
            onChange={update}
            required
          >
            <option value="ROLE_EMPLOYEE">Employee</option>
            <option value="ROLE_HR">HR</option>
          </select>
          <small>
            Employees manage knowledge contributions. HR teams oversee
            organizational insights.
          </small>
        </label>
        <label>
          Password
          <input
            type="password"
            name="password"
            value={form.password}
            onChange={update}
            minLength="8"
            required
          />
          <small>Use at least 8 characters.</small>
        </label>
        {error && <p className="feedback error">{error}</p>}
        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? "Creating account..." : "Create account"}
          <span>→</span>
        </button>
      </form>
    </AuthLayout>
  );
}
