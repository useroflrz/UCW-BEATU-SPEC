import request from './request'

export const register = (data) =>
  request.post('/auth/register', data)

export const login = (data) =>
  request.post('/auth/login', data)
