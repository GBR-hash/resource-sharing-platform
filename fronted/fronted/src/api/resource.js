import request from '@/utils/request'

export function getCategories() {
  return request({
    url: '/public/categories',
    method: 'get'
  })
}

export function getCompetitionTypes() {
  return request({
    url: '/public/competition-types',
    method: 'get'
  })
}

export function getResources(params) {
  return request({
    url: '/resources',
    method: 'get',
    params
  })
}

export function getResourceById(id) {
  return request({
    url: `/resources/${id}`,
    method: 'get'
  })
}

export function uploadResource(data) {
  return request({
    url: '/resources/upload',
    method: 'post',
    data,
    headers: {}
  })
}

export function downloadResource(id) {
  return request({
    url: `/resources/download/${id}`,
    method: 'get',
    responseType: 'blob'
  })
}

export function deleteResource(id) {
  return request({
    url: `/resources/${id}`,
    method: 'delete'
  })
}

export function updateResource(id, data) {
  return request({
    url: `/resources/${id}`,
    method: 'put',
    data
  })
}
