<template>
  <div class="city-gourmet-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2><i class="el-icon-location-outline"></i> 同城美食推荐</h2>
      <p class="subtitle">发现身边的美味，基于发布者位置的智能推荐</p>

      <!-- 功能说明 -->
      <el-alert
        title="💡 如何获得真实的同城美食数据？"
        type="info"
        :closable="false"
        show-icon
        class="feature-tip">
        <template slot="description">
          <div class="tip-content">
            <p><strong>方法一：</strong>发布美食时开启位置分享</p>
            <p>• 在<router-link to="/createGourmet" class="link">发布美食页面</router-link>开启"分享位置"开关</p>
            <p>• 允许浏览器获取您的位置信息</p>
            <p>• 发布后的美食将出现在同城推荐中</p>
            <br>
            <p><strong>方法二：</strong>使用演示数据体验功能</p>
            <p>• 点击"初始化演示数据"按钮</p>
            <p>• 系统会为现有美食随机分配位置信息</p>
          </div>
        </template>
      </el-alert>
    </div>

    <!-- 位置获取区域 -->
    <el-card class="location-card" shadow="hover">
      <div class="location-section">
        <div class="location-info">
          <i class="el-icon-location"></i>
          <span v-if="currentLocation.name">当前位置：{{ currentLocation.name }}</span>
          <span v-else class="no-location">未获取位置信息</span>
        </div>
        <div class="location-actions">
          <el-button 
            type="primary" 
            size="small" 
            @click="getCurrentLocation"
            :loading="locationLoading">
            <i class="el-icon-location"></i> 获取当前位置
          </el-button>
          <el-button 
            type="success" 
            size="small" 
            @click="initDemoData"
            :loading="initLoading">
            <i class="el-icon-magic-stick"></i> 初始化演示数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 搜索控制 -->
    <el-card class="search-card" shadow="hover" v-if="currentLocation.longitude">
      <div class="search-controls">
        <div class="search-item">
          <label>搜索范围：</label>
          <el-select v-model="searchRadius" size="small" @change="searchNearbyGourmets">
            <el-option label="1公里内" :value="1000"></el-option>
            <el-option label="3公里内" :value="3000"></el-option>
            <el-option label="5公里内" :value="5000"></el-option>
            <el-option label="10公里内" :value="10000"></el-option>
            <el-option label="同城范围" :value="50000"></el-option>
          </el-select>
        </div>
        <div class="search-item">
          <label>排序方式：</label>
          <el-select v-model="sortType" size="small" @change="sortGourmets">
            <el-option label="距离最近" value="distance"></el-option>
            <el-option label="最受欢迎" value="popularity"></el-option>
            <el-option label="最新发布" value="newest"></el-option>
          </el-select>
        </div>
        <el-button 
          type="primary" 
          size="small" 
          @click="searchNearbyGourmets"
          :loading="searchLoading">
          <i class="el-icon-search"></i> 搜索
        </el-button>
      </div>
    </el-card>

    <!-- 美食列表 -->
    <div class="gourmet-list" v-loading="searchLoading">
      <div v-if="gourmetList.length === 0 && !searchLoading" class="no-data">
        <i class="el-icon-location-outline"></i>
        <p>暂无附近的美食推荐</p>
        <p class="hint">试试扩大搜索范围或初始化演示数据</p>
      </div>

      <div v-for="gourmet in gourmetList" :key="gourmet.id" class="gourmet-item">
        <el-card shadow="hover" class="gourmet-card">
          <div class="gourmet-content">
            <!-- 美食图片 -->
            <div class="gourmet-image">
              <img :src="gourmet.cover" :alt="gourmet.title" @error="handleImageError">
            </div>

            <!-- 美食信息 -->
            <div class="gourmet-info">
              <h3 class="gourmet-title" @click="viewGourmet(gourmet.id)">{{ gourmet.title }}</h3>
              <p class="gourmet-detail">{{ gourmet.detail }}</p>
              
              <!-- 发布者信息 -->
              <div class="author-info">
                <el-avatar :size="24" :src="gourmet.userAvatar"></el-avatar>
                <span class="author-name">{{ gourmet.userName }}</span>
                <span class="publish-time">{{ formatTime(gourmet.createTime) }}</span>
              </div>

              <!-- 位置和距离信息 -->
              <div class="location-distance">
                <div class="location-info">
                  <i class="el-icon-location-outline"></i>
                  <span>{{ gourmet.locationName || '位置未知' }}</span>
                </div>
                <div class="distance-info" v-if="gourmet.distance !== null">
                  <i class="el-icon-position"></i>
                  <span class="distance">{{ formatDistance(gourmet.distance) }}</span>
                </div>
              </div>

              <!-- 统计信息 -->
              <div class="stats-info">
                <span class="stat-item">
                  <i class="el-icon-view"></i>
                  {{ gourmet.viewCount || 0 }}
                </span>
                <span class="stat-item">
                  <i class="el-icon-star-on"></i>
                  {{ gourmet.upvoteCount || 0 }}
                </span>
                <span class="stat-item">
                  <i class="el-icon-collection"></i>
                  {{ gourmet.saveCount || 0 }}
                </span>
                <span class="stat-item" v-if="gourmet.rating">
                  <i class="el-icon-trophy"></i>
                  {{ gourmet.rating.toFixed(1) }}
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 加载更多 -->
    <div class="load-more" v-if="gourmetList.length > 0 && hasMore">
      <el-button @click="loadMore" :loading="loadingMore">加载更多</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CityGourmet',
  data() {
    return {
      // 当前位置信息
      currentLocation: {
        longitude: null,
        latitude: null,
        name: ''
      },
      
      // 搜索参数
      searchRadius: 5000, // 默认5公里
      sortType: 'distance', // 默认按距离排序
      
      // 美食列表
      gourmetList: [],
      
      // 分页信息
      currentPage: 1,
      pageSize: 10,
      hasMore: true,
      
      // 加载状态
      locationLoading: false,
      searchLoading: false,
      loadingMore: false,
      initLoading: false,
      
      // 默认图片
      defaultImage: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    }
  },
  
  created() {
    // 页面加载时尝试获取位置
    this.getCurrentLocation();
  },
  
  methods: {
    // 获取当前位置
    async getCurrentLocation() {
      this.locationLoading = true;
      
      if (!navigator.geolocation) {
        this.$message.error('您的浏览器不支持地理位置获取');
        this.locationLoading = false;
        return;
      }

      try {
        const position = await new Promise((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(resolve, reject, {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 300000 // 5分钟缓存
          });
        });

        this.currentLocation.longitude = position.coords.longitude;
        this.currentLocation.latitude = position.coords.latitude;
        this.currentLocation.name = `${position.coords.latitude.toFixed(4)}, ${position.coords.longitude.toFixed(4)}`;
        
        this.$message.success('位置获取成功');
        
        // 自动搜索附近美食
        this.searchNearbyGourmets();
        
      } catch (error) {
        console.error('获取位置失败:', error);
        this.$message.warning('位置获取失败，将使用默认位置（北京）');
        
        // 使用北京作为默认位置
        this.currentLocation.longitude = 116.404;
        this.currentLocation.latitude = 39.915;
        this.currentLocation.name = '北京市（默认位置）';
        
        this.searchNearbyGourmets();
      } finally {
        this.locationLoading = false;
      }
    },

    // 搜索附近美食
    async searchNearbyGourmets() {
      if (!this.currentLocation.longitude || !this.currentLocation.latitude) {
        this.$message.warning('请先获取位置信息');
        return;
      }

      this.searchLoading = true;
      this.currentPage = 1;
      this.gourmetList = [];

      try {
        const response = await this.$axios.get('/location-gourmet/nearby', {
          params: {
            longitude: this.currentLocation.longitude,
            latitude: this.currentLocation.latitude,
            radius: this.searchRadius,
            limit: this.pageSize
          }
        });

        if (response.data.success) {
          this.gourmetList = response.data.data || [];
          this.sortGourmets();
          this.hasMore = this.gourmetList.length >= this.pageSize;
          
          if (this.gourmetList.length > 0) {
            this.$message.success(`找到 ${this.gourmetList.length} 个附近的美食`);
          } else {
            this.$message.info('附近暂无美食，试试扩大搜索范围');
          }
        } else {
          this.$message.error('搜索失败：' + response.data.message);
        }
      } catch (error) {
        console.error('搜索附近美食失败:', error);
        this.$message.error('搜索失败，请稍后重试');
      } finally {
        this.searchLoading = false;
      }
    },

    // 排序美食列表
    sortGourmets() {
      if (this.sortType === 'distance') {
        this.gourmetList.sort((a, b) => (a.distance || 0) - (b.distance || 0));
      } else if (this.sortType === 'popularity') {
        this.gourmetList.sort((a, b) => {
          const scoreA = (a.viewCount || 0) + (a.upvoteCount || 0) * 3 + (a.saveCount || 0) * 5;
          const scoreB = (b.viewCount || 0) + (b.upvoteCount || 0) * 3 + (b.saveCount || 0) * 5;
          return scoreB - scoreA;
        });
      } else if (this.sortType === 'newest') {
        this.gourmetList.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
      }
    },

    // 初始化演示数据
    async initDemoData() {
      this.initLoading = true;
      
      try {
        const response = await this.$axios.post('/location-gourmet/init-demo-locations');
        
        if (response.data.success) {
          this.$message.success('演示数据初始化成功');
          // 重新搜索
          if (this.currentLocation.longitude) {
            this.searchNearbyGourmets();
          }
        } else {
          this.$message.error('初始化失败：' + response.data.message);
        }
      } catch (error) {
        console.error('初始化演示数据失败:', error);
        this.$message.error('初始化失败，请稍后重试');
      } finally {
        this.initLoading = false;
      }
    },

    // 查看美食详情
    viewGourmet(gourmetId) {
      this.$router.push(`/gourmetDetail?id=${gourmetId}`);
    },

    // 格式化距离
    formatDistance(distance) {
      if (distance < 1000) {
        return `${Math.round(distance)}m`;
      } else {
        return `${(distance / 1000).toFixed(1)}km`;
      }
    },

    // 格式化时间
    formatTime(timeStr) {
      const time = new Date(timeStr);
      const now = new Date();
      const diff = now - time;
      
      if (diff < 60000) return '刚刚';
      if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
      return `${Math.floor(diff / 86400000)}天前`;
    },

    // 处理图片加载错误
    handleImageError(event) {
      event.target.src = this.defaultImage;
    },

    // 加载更多
    async loadMore() {
      // 实现分页加载逻辑
      this.loadingMore = true;
      // ... 加载更多的实现
      this.loadingMore = false;
    }
  }
}
</script>

<style scoped>
.city-gourmet-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h2 {
  color: #2c3e50;
  margin-bottom: 10px;
  font-size: 28px;
}

.subtitle {
  color: #7f8c8d;
  font-size: 16px;
  margin: 0;
}

.location-card {
  margin-bottom: 20px;
}

.location-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.location-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  color: #2c3e50;
}

.location-info i {
  color: #409EFF;
  font-size: 18px;
}

.no-location {
  color: #999;
}

.location-actions {
  display: flex;
  gap: 10px;
}

.search-card {
  margin-bottom: 20px;
}

.search-controls {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-item label {
  font-weight: bold;
  color: #606266;
  white-space: nowrap;
}

.gourmet-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.gourmet-card {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.gourmet-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.15);
}

.gourmet-content {
  display: flex;
  gap: 15px;
}

.gourmet-image {
  flex-shrink: 0;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
}

.gourmet-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gourmet-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.gourmet-title {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #2c3e50;
  cursor: pointer;
  transition: color 0.3s ease;
  line-height: 1.3;
}

.gourmet-title:hover {
  color: #409EFF;
}

.gourmet-detail {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #909399;
}

.author-name {
  font-weight: 500;
  color: #606266;
}

.location-distance {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #909399;
}

.location-info, .distance-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.distance {
  font-weight: bold;
  color: #67C23A;
}

.stats-info {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #909399;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-item i {
  color: #409EFF;
}

.no-data {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
  grid-column: 1 / -1;
}

.no-data i {
  font-size: 48px;
  margin-bottom: 15px;
  display: block;
  color: #ddd;
}

.no-data p {
  margin: 10px 0;
  font-size: 16px;
}

.hint {
  font-size: 14px !important;
  color: #c0c4cc !important;
}

/* 功能说明样式 */
.feature-tip {
  margin: 20px 0;
  border-radius: 8px;
}

.tip-content {
  line-height: 1.6;
}

.tip-content p {
  margin: 8px 0;
  font-size: 14px;
}

.tip-content .link {
  color: #409EFF;
  text-decoration: none;
  font-weight: 500;
}

.tip-content .link:hover {
  text-decoration: underline;
}

.load-more {
  text-align: center;
  padding: 20px;
}

@media (max-width: 768px) {
  .city-gourmet-container {
    padding: 15px;
  }

  .gourmet-list {
    grid-template-columns: 1fr;
  }

  .location-section {
    flex-direction: column;
    align-items: stretch;
  }

  .search-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .search-item {
    justify-content: space-between;
  }

  .gourmet-content {
    flex-direction: column;
  }

  .gourmet-image {
    width: 100%;
    height: 200px;
  }
}
</style>
