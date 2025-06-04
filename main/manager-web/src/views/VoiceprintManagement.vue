<template>
  <div class="welcome">
    <HeaderBar />

    <div class="operation-bar">
      <h2 class="page-title">声纹管理</h2>
      <div class="right-operations" v-if="agentId">
        <span class="device-info">设备 ID: {{ agentId }}</span>
      </div>
    </div>

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="voiceprint-card" shadow="never" v-if="agentId">
            <div class="operation-buttons">
              <el-button class="btn-primary" @click="fetchVoiceprints">获取声纹列表</el-button>
              <el-button class="btn-upload" @click="handleFileUpload">上传新声纹</el-button>
            </div>
      
            <!-- 声纹列表 -->
            <el-table :data="voiceprints" class="transparent-table" v-if="voiceprints.length" v-loading="loading">
              <el-table-column label="声纹名称" prop="name" align="center"></el-table-column>
              <el-table-column label="声纹ID" prop="id" align="center"></el-table-column>
              <el-table-column label="操作" align="center">
                <template slot-scope="scope">
                  <el-button size="mini" type="text" @click="deleteVoiceprint(scope.row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            
            <div v-if="!voiceprints.length && !loading" class="no-data">
              <p>暂无声纹数据</p>
            </div>
            
            <!-- 文件上传区域 -->
            <div class="upload-section">
              <input type="file" ref="fileInput" @change="handleFileSelect" accept="audio/*" class="file-input">
              <div class="upload-info">
                <div class="file-name" v-if="selectedFile">已选择文件: {{ selectedFile.name }}</div>
                <div class="file-name error" v-if="uploadError">{{ uploadError }}</div>
              </div>
              <div class="upload-actions">
                <el-button class="btn-upload" :disabled="!selectedFile" @click="uploadVoiceprint">确认上传</el-button>
              </div>
            </div>
          </el-card>
          
          <div v-else class="no-device">
            <el-card class="device-card" shadow="never">
              <div class="no-device-content">
                <p>请选择一个设备进行声纹管理</p>
              </div>
            </el-card>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import HeaderBar from "@/components/HeaderBar.vue";
import voiceprintAgent from "@/api/voiceprintAgent";

export default {
  components: {
    HeaderBar
  },
  data() {
    return {
      agentId: this.$route.query.agentId,
      voiceprints: [],
      selectedFile: null,
      uploadError: '',
      loading: false
    };
  },
  methods: {
    fetchVoiceprints() {
      if (!this.agentId) return;
      
      this.loading = true;
      console.log('Fetching voiceprints for agentId:', this.agentId);
      voiceprintAgent.getVoiceprintsByAgentId(this.agentId)
        .then(response => {
          this.voiceprints = response.data;
        })
        .catch(error => {
          console.error('获取声纹列表失败:', error);
          this.$message.error('获取声纹列表失败');
        })
        .finally(() => {
          this.loading = false;
        });
    },
    deleteVoiceprint(voiceprintId) {
      this.$confirm('确定要删除这个声纹吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        console.log('Deleting voiceprint:', voiceprintId, 'for agentId:', this.agentId);
        voiceprintAgent.deleteVoiceprint(this.agentId, voiceprintId)
          .then(() => {
            this.voiceprints = this.voiceprints.filter(vp => vp.id !== voiceprintId);
            this.$message.success('声纹删除成功');
          })
          .catch(error => {
            console.error('删除声纹失败:', error);
            this.$message.error('声纹删除失败');
          });
      });
    },
    handleFileSelect(event) {
      this.selectedFile = event.target.files[0];
      this.uploadError = '';
      
      if (this.selectedFile && !this.selectedFile.type.startsWith('audio/')) {
        this.uploadError = '请选择音频文件';
        this.selectedFile = null;
      }
    },
    handleFileUpload() {
      this.$refs.fileInput.click();
    },
    uploadVoiceprint() {
      if (!this.selectedFile) {
        this.uploadError = '请选择要上传的音频文件';
        return;
      }

      const formData = new FormData();
      formData.append('file', this.selectedFile);

      console.log('Uploading voiceprint for agentId:', this.agentId, 'file:', this.selectedFile.name);
      voiceprintAgent.addVoiceprint(this.agentId, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }).then(response => {
        this.voiceprints.push(response.data);
        this.selectedFile = null;
        this.uploadError = '';
        this.$message.success('声纹上传成功');
      }).catch(error => {
        console.error('上传声纹失败:', error);
        this.uploadError = '声纹上传失败';
      });
    }
  },
  mounted() {
    console.log('VoiceprintManagement mounted, agentId:', this.agentId);
    if (this.agentId) {
      this.fetchVoiceprints();
    }
  }
};
</script>

<style scoped>
.welcome {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.operation-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 30px;
  background: white;
  border-bottom: 1px solid #e8f0ff;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.right-operations {
  display: flex;
  align-items: center;
  gap: 15px;
}

.device-info {
  font-size: 14px;
  color: #666;
  background: #f0f2f5;
  padding: 8px 12px;
  border-radius: 4px;
}

.main-wrapper {
  flex: 1;
  padding: 20px 30px;
  display: flex;
  flex-direction: column;
}

.content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.voiceprint-card {
  border: none;
  box-shadow: none;
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

::v-deep .el-card__body {
  padding: 15px;
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.operation-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.btn-primary {
  background: #5778ff;
  border-color: #5778ff;
  color: white;
}

.btn-upload {
  background: #67c23a;
  border-color: #67c23a;
  color: white;
}

.transparent-table {
  background: transparent;
}

::v-deep .transparent-table .el-table__header {
  background: transparent;
}

::v-deep .transparent-table .el-table__body {
  background: transparent;
}

::v-deep .transparent-table .el-table__row {
  background: transparent;
}

::v-deep .transparent-table .el-table__row:hover {
  background: rgba(87, 120, 255, 0.05);
}

::v-deep .transparent-table th {
  background: transparent;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  border-right: none !important;
}

::v-deep .transparent-table td {
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  border-right: none !important;
}

.upload-section {
  margin-top: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px dashed #d9d9d9;
}

.file-input {
  display: none;
}

.upload-info {
  margin-bottom: 15px;
}

.file-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
}

.file-name.error {
  color: #f56c6c;
}

.upload-actions {
  display: flex;
  justify-content: center;
}

.no-data {
  padding: 40px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.no-device-content {
  padding: 40px;
  text-align: center;
  color: #999;
  font-size: 16px;
}

.device-card {
  border: none;
  box-shadow: none;
}
</style>