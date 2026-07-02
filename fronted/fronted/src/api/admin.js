import request from '@/utils/request'

export function getUserList(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

export function updateUserStatus(id, status) {
  return request({
    url: `/admin/users/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function updateUserRole(id, role) {
  return request({
    url: `/admin/users/${id}/role`,
    method: 'put',
    params: { role }
  })
}

export function getResourceList(params) {
  return request({
    url: '/admin/resources',
    method: 'get',
    params
  })
}

export function approveResource(id) {
  return request({
    url: `/admin/resources/${id}/approve`,
    method: 'put'
  })
}

export function rejectResource(id, reason) {
  return request({
    url: `/admin/resources/${id}/reject`,
    method: 'put',
    data: { reason }
  })
}

export function deleteResource(id) {
  return request({
    url: `/resources/${id}`,
    method: 'delete'
  })
}

export function getStatistics() {
  return request({
    url: '/admin/statistics',
    method: 'get'
  })
}
