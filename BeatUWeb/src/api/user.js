import request from './request'

export const listUsers = () =>
  request.get('/users')
