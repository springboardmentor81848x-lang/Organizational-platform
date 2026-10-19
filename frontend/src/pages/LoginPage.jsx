import { useState } from "react";
import { useNavigate } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { request, saveSession } from "../services/api";

export default function LoginPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const update = (event) =>
    setForm({ ...form, [event.target.name]: event.target.value });

  async function submit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      const user = await request("/auth/login", {
        method: "POST",
        body: JSON.stringify(form),
      });
      saveSession(user);
      navigate("/dashboard");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout
      mode="login"
      title="Sign in to your workspace"
      description="Continue where your team's knowledge becomes visible."
    >
      <form className="auth-form" onSubmit={submit}>
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
          Password
          <input
            type="password"
            name="password"
            value={form.password}
            onChange={update}
            required
          />
        </label>
        {error && <p className="feedback error">{error}</p>}
        <button className="primary-button" type="submit" disabled={loading}>
          {loading ? "Signing in..." : "Sign in"}
          <span>→</span>
        </button>
      </form>
    </AuthLayout>
  );
}
