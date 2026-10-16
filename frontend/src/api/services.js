import api from './client'

const MOCK = import.meta.env.VITE_MOCK_AUTH === 'true'

export const authApi = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (payload) => (MOCK ? Promise.resolve({ data: {} }) : api.post('/auth/register', payload)),
  me: () => api.get('/auth/me'),
}