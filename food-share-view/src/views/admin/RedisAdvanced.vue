<template>
  <div class="redis-advanced-container">
    <div class="page-header">
      <h1>Redis高级功能演示</h1>
      <p>展示Lua脚本、GEO定位、PubSub消息、Pipeline等Redis高级特性</p>
    </div>

    <!-- 功能选项卡 -->
    <el-tabs v-model="activeTab" type="card" @tab-click="handleTabClick">
      <!-- Lua脚本功能 -->
      <el-tab-pane label="Lua脚本" name="lua">
        <el-card class="feature-card">
          <div slot="header">
            <span>Lua脚本原子操作</span>
            <el-button style="float: right; padding: 3px 0" type="text" @click="preloadLuaScripts">预加载脚本</el-button>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <h4>秒杀脚本测试</h4>
              <el-form :inline="true">
                <el-form-item label="商品ID">
                  <el-input v-model="luaForm.itemId" placeholder="输入商品ID" style="width: 120px"></el-input>
                </el-form-item>
                <el-form-item label="用户ID">
                  <el-input v-model="luaForm.userId" placeholder="输入用户ID" style="width: 120px"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="testFlashSaleLua" :loading="luaLoading">执行秒杀</el-button>
                </el-form-item>
              </el-form>
            </el-col>

            <el-col :span="12">
              <h4>热度更新脚本测试</h4>
              <el-form :inline="true">
                <el-form-item label="美食ID">
                  <el-input v-model="luaForm.gourmetId" placeholder="输入美食ID" style="width: 120px"></el-input>
                </el-form-item>
                <el-form-item label="用户ID">
                  <el-input v-model="luaForm.userId2" placeholder="输入用户ID" style="width: 120px"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="success" @click="testTrendingUpdateLua" :loading="luaLoading">更新热度</el-button>
                </el-form-item>
              </el-form>
            </el-col>
          </el-row>

          <div class="result-area" v-if="luaResult">
            <h4>执行结果：</h4>
            <el-alert :title="luaResult.message" :type="luaResult.success ? 'success' : 'error'" show-icon></el-alert>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- GEO地理位置功能 -->
      <el-tab-pane label="GEO定位" name="geo">
        <el-card class="feature-card">
          <div slot="header">
            <span>地理位置服务</span>
            <el-button style="float: right; padding: 3px 0" type="text" @click="batchAddRestaurants">批量添加示例餐厅</el-button>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <h4>添加餐厅位置</h4>
              <el-form label-width="80px">
                <el-form-item label="餐厅ID">
                  <el-input v-model="geoForm.restaurantId" placeholder="输入餐厅ID"></el-input>
                </el-form-item>
                <el-form-item label="餐厅名称">
                  <el-input v-model="geoForm.restaurantName" placeholder="输入餐厅名称"></el-input>
                </el-form-item>
                <el-form-item label="经度">
                  <el-input v-model="geoForm.longitude" placeholder="如：116.404"></el-input>
                </el-form-item>
                <el-form-item label="纬度">
                  <el-input v-model="geoForm.latitude" placeholder="如：39.915"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="addRestaurantLocation" :loading="geoLoading">添加位置</el-button>
                </el-form-item>
              </el-form>
            </el-col>

            <el-col :span="12">
              <h4>搜索附近餐厅</h4>
              <el-form label-width="80px">
                <el-form-item label="用户经度">
                  <el-input v-model="geoForm.searchLng" placeholder="如：116.404"></el-input>
                </el-form-item>
                <el-form-item label="用户纬度">
                  <el-input v-model="geoForm.searchLat" placeholder="如：39.915"></el-input>
                </el-form-item>
                <el-form-item label="搜索半径">
                  <el-input v-model="geoForm.radius" placeholder="单位：米">
                    <template slot="append">米</template>
                  </el-input>
                </el-form-item>
                <el-form-item label="返回数量">
                  <el-input v-model="geoForm.limit" placeholder="最多返回数量"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="success" @click="findNearbyRestaurants" :loading="geoLoading">搜索附近</el-button>
                </el-form-item>
              </el-form>
            </el-col>
          </el-row>

          <!-- 搜索结果 -->
          <div v-if="nearbyRestaurants.length > 0" class="result-area">
            <h4>附近餐厅：</h4>
            <el-table :data="nearbyRestaurants" style="width: 100%">
              <el-table-column prop="restaurantId" label="餐厅ID" width="80"></el-table-column>
              <el-table-column prop="restaurantName" label="餐厅名称" width="150"></el-table-column>
              <el-table-column prop="longitude" label="经度" width="100"></el-table-column>
              <el-table-column prop="latitude" label="纬度" width="100"></el-table-column>
              <el-table-column prop="distance" label="距离" width="100">
                <template slot-scope="scope">
                  {{ scope.row.distance ? scope.row.distance.toFixed(0) + 'm' : '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="address" label="地址"></el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- PubSub消息功能 -->
      <el-tab-pane label="PubSub消息" name="pubsub">
        <el-card class="feature-card">
          <div slot="header">
            <span>发布订阅消息</span>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <h4>用户通知</h4>
              <el-form label-width="80px">
                <el-form-item label="用户ID">
                  <el-input v-model="pubsubForm.userId" placeholder="输入用户ID"></el-input>
                </el-form-item>
                <el-form-item label="消息内容">
                  <el-input v-model="pubsubForm.userMessage" type="textarea" placeholder="输入通知消息"></el-input>
                </el-form-item>
                <el-form-item label="消息类型">
                  <el-select v-model="pubsubForm.userMessageType" placeholder="选择消息类型">
                    <el-option label="信息" value="info"></el-option>
                    <el-option label="警告" value="warning"></el-option>
                    <el-option label="成功" value="success"></el-option>
                    <el-option label="错误" value="error"></el-option>
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="sendUserNotification" :loading="pubsubLoading">发送通知</el-button>
                </el-form-item>
              </el-form>
            </el-col>

            <el-col :span="12">
              <h4>系统广播</h4>
              <el-form label-width="80px">
                <el-form-item label="广播消息">
                  <el-input v-model="pubsubForm.broadcastMessage" type="textarea" placeholder="输入广播消息"></el-input>
                </el-form-item>
                <el-form-item label="消息类型">
                  <el-select v-model="pubsubForm.broadcastType" placeholder="选择消息类型">
                    <el-option label="系统通知" value="system"></el-option>
                    <el-option label="维护通知" value="maintenance"></el-option>
                    <el-option label="活动通知" value="activity"></el-option>
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button type="warning" @click="sendSystemBroadcast" :loading="pubsubLoading">发送广播</el-button>
                </el-form-item>
              </el-form>

              <h4 style="margin-top: 30px;">秒杀通知</h4>
              <el-form label-width="80px">
                <el-form-item label="商品ID">
                  <el-input v-model="pubsubForm.flashSaleItemId" placeholder="输入商品ID"></el-input>
                </el-form-item>
                <el-form-item label="通知消息">
                  <el-input v-model="pubsubForm.flashSaleMessage" placeholder="输入秒杀通知"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="danger" @click="sendFlashSaleNotification" :loading="pubsubLoading">发送秒杀通知</el-button>
                </el-form-item>
              </el-form>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>

      <!-- Pipeline批处理功能 -->
      <el-tab-pane label="Pipeline批处理" name="pipeline">
        <el-card class="feature-card">
          <div slot="header">
            <span>Pipeline批处理操作</span>
          </div>

          <el-row :gutter="20">
            <el-col :span="8">
              <h4>批量SET操作</h4>
              <el-button type="primary" @click="testPipelineBatchSet" :loading="pipelineLoading">测试批量设置</el-button>
              <p class="operation-desc">批量设置100个键值对，测试Pipeline性能</p>
            </el-col>

            <el-col :span="8">
              <h4>批量GET操作</h4>
              <el-button type="success" @click="testPipelineBatchGet" :loading="pipelineLoading">测试批量获取</el-button>
              <p class="operation-desc">批量获取多个键的值，对比普通操作性能</p>
            </el-col>

            <el-col :span="8">
              <h4>批量ZSet操作</h4>
              <el-button type="warning" @click="testPipelineBatchZSet" :loading="pipelineLoading">测试排行榜批量更新</el-button>
              <p class="operation-desc">批量更新排行榜分数，测试ZSet批处理</p>
            </el-col>
          </el-row>

          <div class="result-area" v-if="pipelineResult">
            <h4>Pipeline执行结果：</h4>
            <el-alert :title="pipelineResult" type="success" show-icon></el-alert>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 综合测试 -->
      <el-tab-pane label="综合测试" name="comprehensive">
        <el-card class="feature-card">
          <div slot="header">
            <span>Redis高级功能综合测试</span>
          </div>

          <div class="comprehensive-test">
            <el-button type="primary" size="large" @click="runComprehensiveTest" :loading="comprehensiveLoading">
              <i class="el-icon-cpu"></i>
              运行综合测试
            </el-button>
            <p class="test-desc">一键测试所有Redis高级功能，包括Lua脚本、GEO定位、Pipeline、PubSub等</p>

            <div v-if="comprehensiveResult" class="comprehensive-result">
              <h4>综合测试结果：</h4>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="Lua脚本">{{ comprehensiveResult.lua_script }}</el-descriptions-item>
                <el-descriptions-item label="GEO搜索">{{ comprehensiveResult.geo_search }}</el-descriptions-item>
                <el-descriptions-item label="Pipeline">{{ comprehensiveResult.pipeline }}</el-descriptions-item>
                <el-descriptions-item label="PubSub">{{ comprehensiveResult.pubsub }}</el-descriptions-item>
                <el-descriptions-item label="测试状态">{{ comprehensiveResult.status }}</el-descriptions-item>
                <el-descriptions-item label="测试时间">{{ new Date(comprehensiveResult.timestamp).toLocaleString() }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
export default {
  name: 'RedisAdvanced',
  data() {
    return {
      activeTab: 'lua',

      // Lua脚本相关
      luaForm: {
        itemId: '9999',
        userId: '12345',
        gourmetId: '1',
        userId2: '12345'
      },
      luaLoading: false,
      luaResult: null,

      // GEO地理位置相关
      geoForm: {
        restaurantId: '1001',
        restaurantName: '测试餐厅',
        longitude: '116.404',
        latitude: '39.915',
        searchLng: '116.404',
        searchLat: '39.915',
        radius: '2000',
        limit: '10'
      },
      geoLoading: false,
      nearbyRestaurants: [],

      // PubSub消息相关
      pubsubForm: {
        userId: '1001',
        userMessage: '这是一条测试通知消息',
        userMessageType: 'info',
        broadcastMessage: '系统维护通知：将于今晚22:00进行系统升级',
        broadcastType: 'system',
        flashSaleItemId: '9999',
        flashSaleMessage: '限时秒杀即将开始，请做好准备！'
      },
      pubsubLoading: false,

      // Pipeline相关
      pipelineLoading: false,
      pipelineResult: null,

      // 综合测试相关
      comprehensiveLoading: false,
      comprehensiveResult: null
    }
  },

  methods: {
    handleTabClick(tab) {
      console.log('切换到标签页:', tab.name)
    },

    // ==================== Lua脚本功能 ====================
    async preloadLuaScripts() {
      try {
        const response = await this.$axios.post('/api/redis/advanced/lua/preload')
        if (response.data.code === 200) {
          this.$message.success('Lua脚本预加载成功')
        } else {
          this.$message.error('预加载失败：' + response.data.message)
        }
      } catch (error) {
        console.error('预加载Lua脚本失败:', error)
        this.$message.error('预加载失败')
      }
    },

    async testFlashSaleLua() {
      if (!this.luaForm.itemId || !this.luaForm.userId) {
        this.$message.warning('请填写商品ID和用户ID')
        return
      }

      this.luaLoading = true
      try {
        const response = await this.$axios.post('/api/redis/advanced/lua/flash-sale', {
          itemId: this.luaForm.itemId,
          userId: this.luaForm.userId
        })

        this.luaResult = {
          message: response.data.message,
          success: response.data.code === 200
        }

        if (response.data.code === 200) {
          this.$message.success('Lua脚本执行成功')
        } else {
          this.$message.error('执行失败：' + response.data.message)
        }
      } catch (error) {
        console.error('Lua脚本执行失败:', error)
        this.luaResult = {
          message: '网络错误或服务异常',
          success: false
        }
        this.$message.error('执行失败')
      } finally {
        this.luaLoading = false
      }
    },

    async testTrendingUpdateLua() {
      if (!this.luaForm.gourmetId || !this.luaForm.userId2) {
        this.$message.warning('请填写美食ID和用户ID')
        return
      }

      this.luaLoading = true
      try {
        const response = await this.$axios.post('/api/redis/advanced/lua/trending-update', {
          gourmetId: this.luaForm.gourmetId,
          userId: this.luaForm.userId2
        })

        this.luaResult = {
          message: response.data.message + (response.data.data ? ` (UV: ${response.data.data[0]}, 分数: ${response.data.data[1]})` : ''),
          success: response.data.code === 200
        }

        if (response.data.code === 200) {
          this.$message.success('热度更新成功')
        } else {
          this.$message.error('更新失败：' + response.data.message)
        }
      } catch (error) {
        console.error('热度更新失败:', error)
        this.luaResult = {
          message: '网络错误或服务异常',
          success: false
        }
        this.$message.error('更新失败')
      } finally {
        this.luaLoading = false
      }
    },

    // ==================== GEO地理位置功能 ====================
    async batchAddRestaurants() {
      try {
        const response = await this.$axios.post('/api/redis/advanced/geo/batch-add')
        if (response.data.code === 200) {
          this.$message.success('批量添加餐厅成功')
        } else {
          this.$message.error('添加失败：' + response.data.message)
        }
      } catch (error) {
        console.error('批量添加餐厅失败:', error)
        this.$message.error('添加失败')
      }
    },

    async addRestaurantLocation() {
      if (!this.geoForm.restaurantId || !this.geoForm.restaurantName ||
          !this.geoForm.longitude || !this.geoForm.latitude) {
        this.$message.warning('请填写完整的餐厅信息')
        return
      }

      this.geoLoading = true
      try {
        const response = await this.$axios.post('/api/redis/advanced/geo/add-restaurant', {
          restaurantId: this.geoForm.restaurantId,
          restaurantName: this.geoForm.restaurantName,
          longitude: parseFloat(this.geoForm.longitude),
          latitude: parseFloat(this.geoForm.latitude)
        })

        if (response.data.code === 200) {
          this.$message.success('餐厅位置添加成功')
        } else {
          this.$message.error('添加失败：' + response.data.message)
        }
      } catch (error) {
        console.error('添加餐厅位置失败:', error)
        this.$message.error('添加失败')
      } finally {
        this.geoLoading = false
      }
    },

    async findNearbyRestaurants() {
      if (!this.geoForm.searchLng || !this.geoForm.searchLat) {
        this.$message.warning('请填写搜索位置的经纬度')
        return
      }

      this.geoLoading = true
      try {
        const response = await this.$axios.get('/api/redis/advanced/geo/nearby', {
          params: {
            longitude: parseFloat(this.geoForm.searchLng),
            latitude: parseFloat(this.geoForm.searchLat),
            radius: parseFloat(this.geoForm.radius || 2000),
            limit: parseInt(this.geoForm.limit || 10)
          }
        })

        if (response.data.code === 200) {
          this.nearbyRestaurants = response.data.data || []
          this.$message.success(`找到 ${this.nearbyRestaurants.length} 个附近餐厅`)
        } else {
          this.$message.error('搜索失败：' + response.data.message)
          this.nearbyRestaurants = []
        }
      } catch (error) {
        console.error('搜索附近餐厅失败:', error)
        this.$message.error('搜索失败')
        this.nearbyRestaurants = []
      } finally {
        this.geoLoading = false
      }
    },

    // ==================== PubSub消息功能 ====================
    async sendUserNotification() {
      if (!this.pubsubForm.userId || !this.pubsubForm.userMessage) {
        this.$message.warning('请填写用户ID和消息内容')
        return
      }

      this.pubsubLoading = true
      try {
        const response = await this.$axios.post('/api/redis/advanced/pubsub/user-notification', {
          userId: this.pubsubForm.userId,
          message: this.pubsubForm.userMessage,
          messageType: this.pubsubForm.userMessageType
        })

        if (response.data.code === 200) {
          this.$message.success('用户通知发送成功')
        } else {
          this.$message.error('发送失败：' + response.data.message)
        }
      } catch (error) {
        console.error('发送用户通知失败:', error)
        this.$message.error('发送失败')
      } finally {
        this.pubsubLoading = false
      }
    },

    async sendSystemBroadcast() {
      if (!this.pubsubForm.broadcastMessage) {
        this.$message.warning('请填写广播消息内容')
        return
      }

      this.pubsubLoading = true
      try {
        const response = await this.$axios.post('/api/redis/advanced/pubsub/system-broadcast', {
          message: this.pubsubForm.broadcastMessage,
          messageType: this.pubsubForm.broadcastType
        })

        if (response.data.code === 200) {
          this.$message.success('系统广播发送成功')
        } else {
          this.$message.error('发送失败：' + response.data.message)
        }
      } catch (error) {
        console.error('发送系统广播失败:', error)
        this.$message.error('发送失败')
      } finally {
        this.pubsubLoading = false
      }
    },

    async sendFlashSaleNotification() {
      if (!this.pubsubForm.flashSaleItemId || !this.pubsubForm.flashSaleMessage) {
        this.$message.warning('请填写商品ID和通知消息')
        return
      }

      this.pubsubLoading = true
      try {
        const response = await this.$axios.post('/api/redis/advanced/pubsub/flash-sale-notification', {
          itemId: this.pubsubForm.flashSaleItemId,
          message: this.pubsubForm.flashSaleMessage
        })

        if (response.data.code === 200) {
          this.$message.success('秒杀通知发送成功')
        } else {
          this.$message.error('发送失败：' + response.data.message)
        }
      } catch (error) {
        console.error('发送秒杀通知失败:', error)
        this.$message.error('发送失败')
      } finally {
        this.pubsubLoading = false
      }
    },

    // ==================== Pipeline批处理功能 ====================
    async testPipelineBatchSet() {
      this.pipelineLoading = true
      try {
        const response = await this.$axios.post('/api/redis/advanced/pipeline/batch-set')

        if (response.data.code === 200) {
          this.pipelineResult = response.data.message
          this.$message.success('Pipeline批量设置测试完成')
        } else {
          this.$message.error('测试失败：' + response.data.message)
        }
      } catch (error) {
        console.error('Pipeline批量设置测试失败:', error)
        this.$message.error('测试失败')
      } finally {
        this.pipelineLoading = false
      }
    },

    async testPipelineBatchGet() {
      this.pipelineLoading = true
      try {
        const response = await this.$axios.get('/api/redis/advanced/pipeline/batch-get')

        if (response.data.code === 200) {
          this.pipelineResult = response.data.message
          this.$message.success('Pipeline批量获取测试完成')
        } else {
          this.$message.error('测试失败：' + response.data.message)
        }
      } catch (error) {
        console.error('Pipeline批量获取测试失败:', error)
        this.$message.error('测试失败')
      } finally {
        this.pipelineLoading = false
      }
    },

    async testPipelineBatchZSet() {
      this.pipelineLoading = true
      try {
        const response = await this.$axios.post('/api/redis/advanced/pipeline/batch-zset')

        if (response.data.code === 200) {
          this.pipelineResult = response.data.message
          this.$message.success('Pipeline ZSet批处理测试完成')
        } else {
          this.$message.error('测试失败：' + response.data.message)
        }
      } catch (error) {
        console.error('Pipeline ZSet测试失败:', error)
        this.$message.error('测试失败')
      } finally {
        this.pipelineLoading = false
      }
    },

    // ==================== 综合测试 ====================
    async runComprehensiveTest() {
      this.comprehensiveLoading = true
      try {
        const response = await this.$axios.post('/api/redis/advanced/comprehensive-test')

        if (response.data.code === 200) {
          this.comprehensiveResult = response.data.data
          this.$message.success('Redis高级功能综合测试完成')
        } else {
          this.$message.error('测试失败：' + response.data.message)
        }
      } catch (error) {
        console.error('综合测试失败:', error)
        this.$message.error('测试失败')
      } finally {
        this.comprehensiveLoading = false
      }
    }
  }
}
</script>

<style scoped>
.redis-advanced-container {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 10px;
}

.page-header h1 {
  margin: 0 0 10px 0;
  font-size: 28px;
  font-weight: 600;
}

.page-header p {
  margin: 0;
  font-size: 16px;
  opacity: 0.9;
}

.feature-card {
  margin-bottom: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.feature-card .el-card__header {
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  font-size: 16px;
}

.result-area {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.result-area h4 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.operation-desc {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.comprehensive-test {
  text-align: center;
  padding: 40px 20px;
}

.comprehensive-test .el-button {
  margin-bottom: 20px;
}

.test-desc {
  color: #606266;
  font-size: 14px;
  margin-bottom: 30px;
}

.comprehensive-result {
  margin-top: 30px;
  text-align: left;
}

.comprehensive-result h4 {
  margin-bottom: 15px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .redis-advanced-container {
    padding: 10px;
  }

  .page-header h1 {
    font-size: 24px;
  }

  .page-header p {
    font-size: 14px;
  }
}

/* 标签页样式优化 */
.el-tabs--card > .el-tabs__header .el-tabs__nav {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.el-tabs--card > .el-tabs__header .el-tabs__item {
  border-left: 1px solid #e4e7ed;
  border-top: none;
  border-bottom: none;
}

.el-tabs--card > .el-tabs__header .el-tabs__item.is-active {
  background-color: #409eff;
  color: white;
  border-color: #409eff;
}

/* 表格样式优化 */
.el-table {
  border-radius: 8px;
  overflow: hidden;
}

.el-table th {
  background-color: #fafafa;
  color: #303133;
  font-weight: 600;
}

/* 表单样式优化 */
.el-form-item {
  margin-bottom: 18px;
}

.el-input, .el-select {
  width: 100%;
}

/* 按钮样式优化 */
.el-button {
  border-radius: 6px;
  font-weight: 500;
}

.el-button--large {
  padding: 12px 24px;
  font-size: 16px;
}

/* 描述列表样式 */
.el-descriptions {
  margin-top: 20px;
}

.el-descriptions__label {
  font-weight: 600;
  color: #303133;
}

.el-descriptions__content {
  color: #606266;
}
</style>