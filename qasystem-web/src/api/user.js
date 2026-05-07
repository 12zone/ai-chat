import request from './request'

const userApi = {
  getProfile: () => request.get('/api/user/me'),
  updateProfile: (payload) => request.put('/api/user/me', payload)
}

export default userApi
