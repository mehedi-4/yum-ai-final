import axios from 'axios'

// All backend access goes through this client; the Vite dev server proxies /api.
const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('yumai_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('yumai_token')
      localStorage.removeItem('yumai_user')
      if (window.location.pathname !== '/login') window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export const errMsg = (e) => e.response?.data?.message ?? 'Something went wrong'

export default api
