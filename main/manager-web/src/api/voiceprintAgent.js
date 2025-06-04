import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json'
  }
});

export default {
  getVoiceprintsByAgentId(agentId) {
    console.log(`[API] Fetching voiceprints for agentId: ${agentId}`);
    return apiClient.get(`/voiceprints/${agentId}`);
  },

  deleteVoiceprint(agentId, voiceprintId) {
    console.log(`[API] Deleting voiceprint: ${voiceprintId} for agentId: ${agentId}`);
    return apiClient.delete(`/voiceprints/${agentId}/${voiceprintId}`);
  },

  addVoiceprint(agentId, formData) {
    console.log(`[API] Uploading voiceprint for agentId: ${agentId}`);
    return apiClient.post(`/voiceprints/${agentId}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },
  
  // 根据 VoiceprintController.java 新增的接口
  getAllVoiceprints() {
    console.log('[API] Fetching all voiceprints');
    return apiClient.get('/voiceprints');
  },

  exportVoiceprints() {
    console.log('[API] Exporting voiceprints');
    // 导出通常是文件下载，可能需要不同的处理方式
    // 例如，直接打开一个URL，或者接收blob数据并触发下载
    // 此处仅为示例，具体实现取决于后端如何提供导出功能
    return apiClient.get('/voiceprints/export', { responseType: 'blob' }); 
  }
};