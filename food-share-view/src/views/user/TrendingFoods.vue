<template>
  <div class="trending-foods-container">
    <div class="page-header">
      <h2 class="page-title">
        <i class="el-icon-trophy"></i>
        热门美食排行榜
      </h2>
      <p class="page-subtitle">基于Redis ZSet实时统计的热门美食排行</p>
    </div>



    <div class="filter-section">
      <div class="filter-left">
        <el-radio-group v-model="limit" @change="fetchTrendingFoods">
          <el-radio-button :label="5">Top 5</el-radio-button>
          <el-radio-button :label="10">Top 10</el-radio-button>
          <el-radio-button :label="20">Top 20</el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-right">
        <el-button type="primary" size="small" @click="refreshTrendingFoods" icon="el-icon-refresh">
          刷新排行榜
        </el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="trendingFoods"
      stripe
      style="width: 100%"
      :header-cell-style="{background:'#f5f7fa',color:'#606266'}">
      <el-table-column prop="rank" label="排名" width="80" align="center">
        <template slot-scope="scope">
          <div class="rank-badge" :class="{'top-three': scope.row.rank <= 3}">
            {{ scope.row.rank }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="美食" min-width="300">
        <template slot-scope="scope">
          <div class="food-info">
            <el-image 
              :src="scope.row.cover" 
              fit="cover"
              style="width: 80px; height: 60px; border-radius: 4px;"
              @click="viewDetail(scope.row.id)">
              <div slot="error" class="image-slot">
                <i class="el-icon-picture-outline"></i>
              </div>
            </el-image>
            <div class="food-details">
              <div class="food-title" @click="viewDetail(scope.row.id)">{{ scope.row.title }}</div>
              <div class="food-meta">
                <span><i class="el-icon-user"></i> {{ scope.row.userName }}</span>
                <span><i class="el-icon-date"></i> {{ formatDate(scope.row.createTime) }}</span>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览量" width="120" align="center">
        <template slot-scope="scope">
          <div class="view-count-container">
            <span class="view-count">{{ scope.row.viewCount }}</span>
            <el-button
              size="mini"
              type="text"
              @click="recordView(scope.row.id)"
              title="记录浏览">
              <i class="el-icon-view"></i>
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="UV统计" width="120" align="center">
        <template slot-scope="scope">
          <el-popover
            placement="top"
            width="200"
            trigger="hover">
            <div>
              <p>UV统计表示独立访问用户数</p>
              <p>今日UV: {{ scope.row.uvCount || '加载中...' }}</p>
            </div>
            <div slot="reference" class="uv-count">
              <el-button type="text" @click="fetchGourmetUV(scope.row)">
                查看UV
              </el-button>
            </div>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template slot-scope="scope">
          <el-button type="text" @click="viewDetail(scope.row.id)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="trendingFoods.length === 0 && !loading" class="empty-data">
      <i class="el-icon-warning-outline"></i>
      <p>暂无热门美食数据</p>
    </div>
  </div>
</template>

<script>
import { recommendApi } from '@/api/recommend'

export default {
  name: 'TrendingFoods',
  data() {
    return {
      trendingFoods: [],
      loading: false,
      limit: 10
    }
  },
  created() {
    this.fetchTrendingFoods()
  },
  methods: {
    async fetchTrendingFoods() {
      this.loading = true
      try {
        const response = await recommendApi.getTrendingFoods(this.limit)
        if (response.data.code === 200) {
          // 添加排名属性
          this.trendingFoods = response.data.data.map((item, index) => {
            return {
              ...item,
              rank: index + 1,
              uvCount: null // 初始化UV计数
            }
          })
        } else {
          this.$message.error('获取热门排行榜失败')
        }
      } catch (error) {
        console.error('获取热门排行榜异常:', error)
        this.$message.error('获取热门排行榜异常')
      } finally {
        this.loading = false
      }
    },
    async fetchGourmetUV(row) {
      if (row.uvCount !== null) return // 已加载过UV数据

      try {
        const response = await recommendApi.getGourmetUV(row.id)
        if (response.data.code === 200) {
          // 使用Vue.set确保响应式更新
          this.$set(row, 'uvCount', response.data.data)
        }
      } catch (error) {
        console.error('获取UV统计异常:', error)
        this.$set(row, 'uvCount', '获取失败')
      }
    },
    refreshTrendingFoods() {
      this.fetchTrendingFoods()
      this.$message.success('热门排行榜已刷新')
    },

    // 优化排行榜性能
    async testRedisFeatures() {
      this.$message.info('正在优化排行榜性能...')

      try {
        const userInfo = this.getCurrentUser()
        if (!userInfo) {
          this.$message.warning('请先登录')
          return
        }

        // 使用Pipeline批量更新排行榜数据
        const pipelineResponse = await this.$axios.post('/redis/advanced/pipeline/batch-set')

        // 预加载Lua脚本提升性能
        await this.$axios.post('/redis/advanced/lua/preload')

        this.$message.success('排行榜性能优化完成！')

        // 刷新排行榜显示最新数据
        setTimeout(() => {
          this.fetchTrendingFoods()
        }, 1000)

      } catch (error) {
        console.error('性能优化失败:', error)
        this.$message.error('性能优化失败')
      }
    },

    // 记录浏览量
    async recordView(gourmetId) {
      try {
        const userInfo = this.getCurrentUser()
        if (!userInfo) {
          this.$message.warning('请先登录')
          return
        }

        await this.$axios.post('/redis/advanced/uv/record', {
          gourmetId: gourmetId.toString(),
          userId: userInfo.id
        })

        this.$message.success('浏览记录已更新')

        // 更新对应行的UV显示
        const item = this.trendingFoods.find(food => food.id === gourmetId)
        if (item) {
          this.fetchGourmetUV(item)
        }

      } catch (error) {
        console.error('记录浏览失败:', error)
        this.$message.error('记录浏览失败')
      }
    },

    // 获取当前用户信息
    getCurrentUser() {
      try {
        const userInfo = sessionStorage.getItem('userInfo')
        return userInfo ? JSON.parse(userInfo) : null
      } catch (error) {
        console.error('获取用户信息失败:', error)
        return null
      }
    },
    viewDetail(id) {
      this.$router.push(`/gourmetDetail?id=${id}`)
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    }
  }
}
</script>

<style scoped lang="scss">
.trending-foods-container {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: 100vh;

  .page-header {
    text-align: center;
    margin-bottom: 30px;
    padding: 30px;
    background: white;
    border-radius: 15px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  }

  .page-title {
    margin: 0;
    color: #2c3e50;
    font-size: 28px;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;

    i {
      color: #f39c12;
      font-size: 32px;
    }
  }

  .page-subtitle {
    margin: 10px 0 0 0;
    color: #7f8c8d;
    font-size: 14px;
  }



  .filter-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 20px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  }

  .filter-left, .filter-right {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .rank-badge {
    display: inline-block;
    width: 35px;
    height: 35px;
    line-height: 35px;
    text-align: center;
    border-radius: 50%;
    background: linear-gradient(45deg, #bdc3c7, #95a5a6);
    color: #fff;
    font-weight: bold;
    font-size: 14px;

    &.top-three {
      background: linear-gradient(45deg, #f39c12, #e67e22);
      box-shadow: 0 4px 15px rgba(243, 156, 18, 0.4);
      animation: pulse 2s infinite;
    }
  }

  @keyframes pulse {
    0% { transform: scale(1); }
    50% { transform: scale(1.1); }
    100% { transform: scale(1); }
  }

  .view-count-container {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 5px;
  }

  .view-count {
    font-weight: bold;
    color: #3498db;
  }

  .food-info {
    display: flex;
    align-items: center;
  }

  .food-details {
    margin-left: 15px;
  }

  .food-title {
    font-weight: 500;
    color: #303133;
    margin-bottom: 5px;
    cursor: pointer;

    &:hover {
      color: #409EFF;
    }
  }

  .food-meta {
    font-size: 12px;
    color: #909399;

    span {
      margin-right: 15px;
    }

    i {
      margin-right: 5px;
    }
  }

  .view-count {
    font-weight: 500;
    color: #f56c6c;
  }

  .uv-count {
    color: #409EFF;
  }

  .empty-data {
    text-align: center;
    padding: 40px 0;
    color: #909399;

    i {
      font-size: 48px;
      margin-bottom: 10px;
    }
  }
}
</style> 