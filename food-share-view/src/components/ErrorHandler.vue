<template>
  <div class="error-handler" v-if="hasError">
    <div class="error-card">
      <div class="error-header">
        <i class="el-icon-warning"></i>
        <h3>系统提示</h3>
        <i class="el-icon-close close-icon" @click="clearErrors"></i>
      </div>
      <div class="error-content">
        <p>{{ errorMessage }}</p>
        <div v-if="showRecommendation" class="error-recommendation">
          <p>可能的解决方案:</p>
          <ul>
            <li v-if="isPythonError">Python推荐系统服务未启动或暂时不可用，已自动使用备选推荐方案</li>
            <li v-if="isImageError">外部图片资源暂时不可用，已使用本地图片替代</li>
            <li v-if="isNetworkError">网络连接不稳定，请检查网络连接后重试</li>
            <li v-if="isServerError">服务器暂时繁忙，请稍后再试</li>
          </ul>
        </div>
      </div>
      <div class="error-footer">
        <el-button type="primary" size="small" @click="clearErrors">知道了</el-button>
        <el-button v-if="canRetry" size="small" @click="retry">重试</el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ErrorHandler',
  
  data() {
    return {
      hasError: false,
      errorMessage: '',
      errorType: null,
      showRecommendation: true,
      canRetry: false,
      retryCallback: null
    };
  },
  
  computed: {
    isPythonError() {
      return this.errorType === 'python';
    },
    isImageError() {
      return this.errorType === 'image';
    },
    isNetworkError() {
      return this.errorType === 'network';
    },
    isServerError() {
      return this.errorType === 'server';
    }
  },
  
  created() {
    // 全局事件监听
    this.$root.$on('error', this.showError);
    this.$root.$on('clear-error', this.clearErrors);
  },
  
  beforeDestroy() {
    // 移除事件监听
    this.$root.$off('error', this.showError);
    this.$root.$off('clear-error', this.clearErrors);
  },
  
  methods: {
    showError(options) {
      this.hasError = true;
      this.errorMessage = options.message || '系统发生错误，请稍后再试';
      this.errorType = options.type || null;
      this.showRecommendation = options.showRecommendation !== false;
      this.canRetry = !!options.retry;
      this.retryCallback = options.retry;
      
      // 自动消失
      if (options.autoHide !== false) {
        setTimeout(() => {
          this.clearErrors();
        }, options.duration || 5000);
      }
    },
    
    clearErrors() {
      this.hasError = false;
      this.errorMessage = '';
      this.errorType = null;
      this.canRetry = false;
      this.retryCallback = null;
    },
    
    retry() {
      if (typeof this.retryCallback === 'function') {
        this.retryCallback();
      }
      this.clearErrors();
    }
  }
};
</script>

<style scoped>
.error-handler {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  transition: all 0.3s;
}

.error-card {
  width: 350px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.error-header {
  padding: 12px 15px;
  background-color: #f56c6c;
  color: white;
  display: flex;
  align-items: center;
}

.error-header i {
  font-size: 20px;
  margin-right: 10px;
}

.error-header h3 {
  flex: 1;
  margin: 0;
  font-size: 16px;
}

.close-icon {
  cursor: pointer;
  margin-right: 0;
}

.error-content {
  padding: 15px;
  color: #606266;
}

.error-content p {
  margin: 0 0 10px;
}

.error-recommendation {
  font-size: 13px;
  color: #909399;
}

.error-recommendation ul {
  margin: 5px 0 0;
  padding-left: 20px;
}

.error-footer {
  padding: 10px 15px;
  text-align: right;
  background-color: #f5f7fa;
}
</style> 