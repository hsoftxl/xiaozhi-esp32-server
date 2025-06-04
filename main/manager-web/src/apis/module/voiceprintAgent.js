import request from '@/utils/request'

export default {
  list: (params) => request.get('/api/voiceprints', { params }),
  delete: (id) => request.delete(`/api/voiceprints/${id}`),
  export: () => request.get('/api/voiceprints/export', { responseType: 'blob' }),
  create: (data) => request.post('/api/voiceprints', data) // 新增
}