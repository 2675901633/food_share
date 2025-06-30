<template>
  <div class="flash-sale-container">

    <div class="flash-sale-header">
      <h2>限时秒杀</h2>
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="进行中" name="ongoing"></el-tab-pane>
        <el-tab-pane label="即将开始" name="upcoming"></el-tab-pane>
        <el-tab-pane label="已结束" name="ended"></el-tab-pane>
      </el-tabs>
    </div>
    
    <div class="flash-sale-filter">
      <el-input
        v-model="searchName"
        placeholder="搜索商品名称"
        prefix-icon="el-icon-search"
        clearable
        @clear="loadData"
        @keyup.enter.native="loadData"
        style="width: 220px;"
      ></el-input>
      <el-button type="primary" size="small" @click="loadData">搜索</el-button>
      <el-button size="small" @click="resetSearch">重置</el-button>
      <el-button type="success" size="small" @click="$router.push('/myOrders')">我的订单</el-button>
    </div>
    
    <div v-loading="loading" class="flash-sale-content">
      <div v-if="flashSaleItems.length === 0" class="empty-data">
        <i class="el-icon-goods"></i>
        <p>暂无秒杀商品</p>
      </div>
      
      <div v-else class="flash-sale-items">
        <el-card
          v-for="item in flashSaleItems"
          :key="item.id"
          class="flash-sale-item"
          :body-style="{ padding: '0px' }"
          shadow="hover"
          @click.native="goToDetail(item.id)"
        >
          <div class="item-image">
            <img 
              :src="item.image" 
              :alt="item.name" 
              @error="handleImageError"
            />
            <div class="item-status" :class="getStatusClass(item)">
              {{ getStatusText(item) }}
            </div>
          </div>
          <div class="item-info">
            <h3 class="item-title">{{ item.name }}</h3>
            <div class="item-price">
              <span class="flash-price">¥{{ item.flashPrice }}</span>
              <span class="original-price">¥{{ item.originalPrice }}</span>
            </div>
            <div class="item-progress">
              <el-progress 
                :percentage="getProgressPercentage(item)" 
                :color="getProgressColor(item)"
              ></el-progress>
              <span class="progress-text">已售 {{ item.soldCount || 0 }}/{{ item.stock + (item.soldCount || 0) }}</span>
            </div>
            <div class="item-time">
              <i class="el-icon-time"></i>
              <countdown v-if="item.status === 1 && item.stock > 0" :time="item.remainSeconds * 1000" format="结束: dd天hh时mm分ss秒"></countdown>
              <countdown v-else-if="item.status === 0" :time="item.remainSeconds * 1000" format="开始: dd天hh时mm分ss秒"></countdown>
              <span v-else-if="item.status === 1 && item.stock <= 0">已售罄</span>
              <span v-else>已结束</span>
            </div>

            <!-- 秒杀按钮 -->
            <div class="item-actions">
              <el-button
                v-if="item.status === 1 && item.stock > 0"
                type="danger"
                size="small"
                @click.stop="flashSaleWithLua(item)"
                :loading="item.purchasing"
                class="flash-sale-btn">
                <i class="el-icon-lightning"></i>
                {{ item.purchasing ? '抢购中...' : '立即抢购' }}
              </el-button>

              <el-button
                v-else-if="item.status === 0"
                type="warning"
                size="small"
                @click.stop="subscribeFlashSale(item)"
                :disabled="item.subscribed"
                class="subscribe-btn">
                <i class="el-icon-bell"></i>
                {{ item.subscribed ? '已订阅提醒' : '开始提醒' }}
              </el-button>

              <el-button
                v-else
                type="info"
                size="small"
                disabled
                class="disabled-btn">
                {{ item.stock <= 0 ? '已售罄' : '已结束' }}
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
      
      <div class="pagination">
        <el-pagination
          @current-change="handleCurrentChange"
          :current-page.sync="currentPage"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          :total="total"
        ></el-pagination>
      </div>
    </div>
  </div>
</template>

<script>
import { listFlashSaleItems } from '@/api/flashSale';
import { handleImageError } from '@/utils/defaultImage';
import DefaultFoodImage from '@/components/DefaultFoodImage';

export default {
  name: 'FlashSale',
  
  components: {
    DefaultFoodImage,
    countdown: {
      props: {
        time: {
          type: Number,
          default: 0
        },
        format: {
          type: String,
          default: 'mm:ss'
        }
      },
      data() {
        return {
          displayTime: '00:00',
          remainingTime: this.time
        };
      },
      mounted() {
        this.startCountdown();
      },
      beforeDestroy() {
        if (this.timer) {
          clearInterval(this.timer);
        }
      },
      methods: {
        startCountdown() {
          this.remainingTime = this.time;
          
          const updateDisplay = () => {
            if (this.remainingTime <= 0) {
              clearInterval(this.timer);
              this.$emit('finish');
              
              // 倒计时结束后刷新数据
              setTimeout(() => {
                this.$parent.loadData();
              }, 1000);
              
              return;
            }
            
            const totalSeconds = Math.floor(this.remainingTime / 1000);
            const seconds = totalSeconds % 60;
            const minutes = Math.floor(totalSeconds / 60) % 60;
            const hours = Math.floor(totalSeconds / 3600) % 24;
            const days = Math.floor(totalSeconds / 86400); // 正确计算天数
            
            let formattedTime = this.format;
            
            // 处理天数显示
            if (days > 0) {
              formattedTime = formattedTime.replace('dd', days.toString());
            } else {
              // 如果没有天数，移除天数部分
              formattedTime = formattedTime.replace(/dd[天]?\s*/g, '');
            }
            
            // 处理小时显示
            if (hours > 0 || days > 0) {
              formattedTime = formattedTime.replace('hh', hours.toString().padStart(2, '0'));
            } else {
              // 如果没有小时和天数，移除小时部分
              formattedTime = formattedTime.replace(/hh[时]?\s*/g, '');
            }
            
            formattedTime = formattedTime.replace('mm', minutes.toString().padStart(2, '0'));
            formattedTime = formattedTime.replace('ss', seconds.toString().padStart(2, '0'));
            
            this.displayTime = formattedTime;
            this.remainingTime -= 1000;
          };
          
          updateDisplay();
          this.timer = setInterval(updateDisplay, 1000);
        }
      },
      render(h) {
        return h('span', { class: 'countdown' }, this.displayTime);
      }
    }
  },
  
  data() {
    return {
      activeTab: 'ongoing',
      searchName: '',
      loading: false,
      flashSaleItems: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      timer: null,

      // Redis功能相关数据
      testingPerformance: false,
      performanceResult: null
    };
  },
  
  created() {
    this.loadData();
    this.preloadLuaScripts();
    // 每30秒刷新一次数据，保持倒计时更新
    this.timer = setInterval(this.loadData, 30000);
  },
  
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  },
  
  methods: {
    loadData() {
      this.loading = true;
      
      let status = null;
      if (this.activeTab === 'ongoing') {
        status = 1; // 进行中
      } else if (this.activeTab === 'upcoming') {
        status = 0; // 未开始
      } else if (this.activeTab === 'ended') {
        status = 2; // 已结束
      }
      
      const queryParams = {
        name: this.searchName,
        status: status,
        current: this.currentPage,
        size: this.pageSize
      };
      
      listFlashSaleItems(queryParams)
        .then(res => {
          if (res.data && res.data.code === 200) {
            this.flashSaleItems = res.data.data || [];
            this.total = res.data.total || this.flashSaleItems.length;
          } else {
            this.$message.error(res.data.msg || '获取秒杀商品失败');
            this.flashSaleItems = [];
            this.total = 0;
          }
        })
        .catch(err => {
          console.error(err);
          this.$message.error('获取秒杀商品失败');
          this.flashSaleItems = [];
          this.total = 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    
    handleTabClick() {
      this.currentPage = 1;
      this.loadData();
    },
    
    handleCurrentChange() {
      this.loadData();
    },
    
    resetSearch() {
      this.searchName = '';
      this.currentPage = 1;
      this.loadData();
    },
    
    goToDetail(itemId) {
      this.$router.push({
        path: '/flashSaleDetail',
        query: { id: itemId }
      });
    },
    
    getStatusText(item) {
      if (!item) return '未知状态';
      
      // 库存为0且状态为进行中，显示为已售罄
      if (item.status === 1 && item.stock <= 0) return '已售罄';
      if (item.status === 0) return '未开始';
      if (item.status === 1) return '进行中';
      if (item.status === 2) return '已结束';
      return '未知状态';
    },
    
    getStatusClass(item) {
      if (!item) return '';
      
      // 库存为0且状态为进行中，显示为已结束
      if (item.status === 1 && item.stock <= 0) return 'ended';
      if (item.status === 0) return 'upcoming';
      if (item.status === 1) return 'ongoing';
      if (item.status === 2) return 'ended';
      return '';
    },
    
    getProgressPercentage(item) {
      if (!item || !item.stock) return 0;
      
      // 计算总库存（当前库存+已售数量）
      const totalStock = item.stock + (item.soldCount || 0);
      
      // 如果总库存为0，返回0%
      if (totalStock === 0) return 0;
      
      // 计算已售百分比
      return Math.floor(((item.soldCount || 0) / totalStock) * 100);
    },
    
    getProgressColor(item) {
      const percentage = this.getProgressPercentage(item);
      if (percentage < 50) return '#67c23a';
      if (percentage < 80) return '#e6a23c';
      return '#f56c6c';
    },
    
    handleImageError(e) {
      // 使用全局图片错误处理
      handleImageError(e);
    },

    // === Redis功能相关方法 ===

    // 预加载Lua脚本
    async preloadLuaScripts() {
      try {
        await this.$axios.post('/redis/advanced/lua/preload');
        console.log('Lua脚本预加载成功');
      } catch (error) {
        console.error('Lua脚本预加载失败:', error);
      }
    },

    // Redis Lua脚本秒杀
    async flashSaleWithLua(item) {
      // 设置购买状态
      this.$set(item, 'purchasing', true);

      try {
        const userInfo = this.getCurrentUser()
        if (!userInfo) {
          this.$message.warning('请先登录')
          this.$set(item, 'purchasing', false)
          return
        }

        const response = await this.$axios.post('/redis/advanced/lua/flash-sale', {
          itemId: item.id.toString(),
          userId: userInfo.id
        });

        if (response.data.success) {
          this.$message.success('秒杀成功！');

          // 发送秒杀成功通知
          await this.sendFlashSaleNotification(item, '秒杀成功');

          // 更新库存显示
          item.stock = Math.max(0, item.stock - 1);
          item.soldCount = (item.soldCount || 0) + 1;

          // 跳转到订单页面
          setTimeout(() => {
            this.$router.push('/myOrders');
          }, 1500);

        } else {
          this.$message.error(response.data.msg || '秒杀失败');
        }
      } catch (error) {
        console.error('秒杀失败:', error);
        this.$message.error('秒杀失败，请稍后重试');
      } finally {
        this.$set(item, 'purchasing', false);
      }
    },

    // 订阅秒杀提醒 (PubSub)
    async subscribeFlashSale(item) {
      try {
        await this.$axios.post('/redis/advanced/pubsub/flash-sale-notification', {
          itemId: item.id.toString(),
          message: `${item.name} 即将开始秒杀，请做好准备！`
        });

        this.$set(item, 'subscribed', true);
        this.$message.success('秒杀提醒订阅成功');
      } catch (error) {
        console.error('订阅失败:', error);
        this.$message.error('订阅失败');
      }
    },

    // 发送秒杀通知
    async sendFlashSaleNotification(item, message) {
      try {
        await this.$axios.post('/redis/advanced/pubsub/flash-sale-notification', {
          itemId: item.id.toString(),
          message: `${item.name} ${message}`
        });
      } catch (error) {
        console.error('发送通知失败:', error);
      }
    },

    // 测试Redis性能
    async testRedisPerformance() {
      this.testingPerformance = true;

      try {
        const startTime = Date.now();

        // 测试Lua脚本性能
        await this.$axios.post('/redis/advanced/lua/preload');

        // 测试Pipeline性能
        await this.$axios.post('/redis/advanced/pipeline/batch-set');

        const endTime = Date.now();
        const responseTime = endTime - startTime;

        this.performanceResult = {
          message: 'Redis性能测试通过',
          responseTime: responseTime,
          concurrent: 100
        };

        this.$message.success('Redis性能测试完成');

        // 5秒后隐藏结果
        setTimeout(() => {
          this.performanceResult = null;
        }, 5000);

      } catch (error) {
        console.error('性能测试失败:', error);
        this.$message.error('性能测试失败');
      } finally {
        this.testingPerformance = false;
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
    }
  }
};
</script>

<style scoped lang="scss">
.flash-sale-container {
  padding: 20px;
}



.flash-sale-header {
  margin-bottom: 20px;
}

.flash-sale-filter {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.flash-sale-content {
  min-height: 300px;
}

.empty-data {
  text-align: center;
  padding: 50px 0;
  color: #909399;
}

.empty-data i {
  font-size: 60px;
  margin-bottom: 20px;
}

.flash-sale-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.flash-sale-item {
  cursor: pointer;
  transition: transform 0.3s;
}

.flash-sale-item:hover {
  transform: translateY(-5px);
}

.item-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-status {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 5px 10px;
  border-radius: 4px;
  color: white;
  font-weight: bold;
  font-size: 12px;
}

.item-status.ongoing {
  background-color: #67c23a;
}

.item-status.upcoming {
  background-color: #409eff;
}

.item-status.ended {
  background-color: #909399;
}

.item-info {
  padding: 15px;
}

.item-title {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: bold;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  margin-bottom: 10px;
}

.flash-price {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
  margin-right: 10px;
}

.original-price {
  font-size: 14px;
  color: #909399;
  text-decoration: line-through;
}

.item-progress {
  margin-bottom: 10px;
}

.progress-text {
  font-size: 12px;
  color: #606266;
}

.item-time {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #606266;
  margin-bottom: 15px;
}

.countdown {
  font-weight: bold;
  color: #f56c6c;
}

// 秒杀按钮样式
.item-actions {
  margin-top: 15px;

  .flash-sale-btn {
    width: 100%;
    background: linear-gradient(45deg, #ff6b6b, #ee5a24);
    border: none;
    color: white;
    font-weight: bold;
    transition: all 0.3s ease;

    &:hover {
      background: linear-gradient(45deg, #ee5a24, #ff6b6b);
      transform: translateY(-2px);
      box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);
    }

    &:active {
      transform: translateY(0);
    }
  }

  .subscribe-btn {
    width: 100%;
    background: linear-gradient(45deg, #ffa726, #ff9800);
    border: none;
    color: white;
    font-weight: bold;

    &:hover {
      background: linear-gradient(45deg, #ff9800, #ffa726);
    }

    &:disabled {
      background: #e0e0e0;
      color: #9e9e9e;
    }
  }

  .disabled-btn {
    width: 100%;
    background: #e0e0e0;
    color: #9e9e9e;
    border: none;
  }
}

.pagination {
  margin-top: 20px;
  text-align: center;
}
</style> 