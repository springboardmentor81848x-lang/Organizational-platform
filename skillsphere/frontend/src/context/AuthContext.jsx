import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import * as auth from "../services/authService.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Restore the session (if the cookie is still valid) when the app loads.
  useEffect(() => {
    let active = true;
    auth
      .getMe()
      .then((u) => active && setUser(u))
      .catch(() => active && setUser(null))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, []);

  const signIn = useCallback(async (credentials) => {
    const u = await auth.login(credentials);
    setUser(u);
    return u;
  }, []);

  const signOut = useCallback(async () => {
    try {
      await auth.logout();
    } finally {
      setUser(null);
    }
  }, []);

  const value = useMemo(() => ({ user, loading, signIn, signOut }), [user, loading, signIn, signOut]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
