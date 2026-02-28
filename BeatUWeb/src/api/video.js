import request from './request'

export const listVideos = (params) =>
  request.get('/videos', { params })
