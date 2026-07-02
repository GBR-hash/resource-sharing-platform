import request from '@/utils/request'

export function toggleFavorite(resourceId) {
  return request({
    url: `/favorites/toggle/${resourceId}`,
    method: 'post'
  })
}

export function getFavoriteStatus(resourceId) {
  return request({
    url: `/favorites/status/${resourceId}`,
    method: 'get'
  })
}

export function getMyFavorites() {
  return request({
    url: '/favorites/my',
    method: 'get'
  })
}
