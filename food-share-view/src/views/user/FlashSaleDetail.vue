<template>
  <div class="flash-sale-detail" v-loading="loading">
    <div class="page-header">
      <el-breadcrumb separator-class="el-icon-arrow-right">
        <el-breadcrumb-item :to="{ path: '/flashSale' }">限时秒杀</el-breadcrumb-item>
        <el-breadcrumb-item>秒杀详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div v-if="flashSaleItem" class="detail-content">
      <div class="detail-left">
        <div class="product-image">
          <img 
            v-if="flashSaleItem.image" 
            :src="flashSaleItem.image" 
            alt="商品图片"
            @error="handleImageError"
          >
          <default-food-image v-else />
          <div class="status-badge" :class="getStatusClass(flashSaleItem)">
            {{ getStatusText(flashSaleItem) }}
          </div>
        </div>
      </div>
      
      <div class="detail-right">
        <h1 class="product-name">{{ flashSaleItem.name }}</h1>
        
        <div class="product-price">
          <div class="price-container">
            <div class="price-label">秒杀价</div>
            <div class="flash-price">¥{{ flashSaleItem.flashPrice }}</div>
            <div class="original-price">原价: ¥{{ flashSaleItem.originalPrice }}</div>
          </div>
          <div class="discount">
            {{ getDiscountRate(flashSaleItem.flashPrice, flashSaleItem.originalPrice) }}折
          </div>
        </div>

        <div class="countdown-box" :class="getStatusClass(flashSaleItem)">
          <div class="countdown-label">{{ getStatusText(flashSaleItem) }}</div>
          <div class="countdown-timer" v-if="flashSaleItem.status !== 2 && !isStockEmpty(flashSaleItem)">
            <countdown 
              :time="flashSaleItem.remainSeconds * 1000" 
              :format="flashSaleItem.status === 1 ? '结束倒计时: dd天hh时mm分ss秒' : '开始倒计时: dd天hh时mm分ss秒'"
              @finish="loadData"
            ></countdown>
          </div>
          <div class="countdown-timer" v-else-if="flashSaleItem.status === 1 && isStockEmpty(flashSaleItem)">
            商品已抢完
          </div>
          <div class="countdown-timer" v-else>
            活动已结束
          </div>
        </div>

        <div class="product-stock">
          <div class="stock-info">
            <span class="stock-label">库存:</span>
            <span class="stock-value">{{ flashSaleItem.stock }}</span>
          </div>
          <div class="stock-info">
            <span class="stock-label">已售:</span>
            <span class="stock-value">{{ flashSaleItem.soldCount || 0 }}</span>
          </div>
          <el-progress 
            :percentage="getProgressPercentage(flashSaleItem)" 
            :color="getProgressColor(flashSaleItem)"
            :format="percentFormat"
            :stroke-width="20"
          ></el-progress>
        </div>

        <div class="product-actions">
          <el-button 
            type="danger" 
            size="large" 
            round 
            :disabled="!canBuy(flashSaleItem)" 
            @click="handleFlashSale"
            :loading="buyLoading"
          >
            {{ getBuyButtonText(flashSaleItem) }}
          </el-button>
          <el-button 
            type="info" 
            size="large" 
            plain
            round 
            @click="$router.push('/myOrders')"
          >
            我的订单
          </el-button>
        </div>

        <div class="sale-time">
          <div class="time-item">
            <span class="time-label">开始时间:</span>
            <span class="time-value">{{ formatDateTime(flashSaleItem.startTime) }}</span>
          </div>
          <div class="time-item">
            <span class="time-label">结束时间:</span>
            <span class="time-value">{{ formatDateTime(flashSaleItem.endTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="product-details">
      <div class="section-title">商品详情</div>
      <div class="product-description" v-html="flashSaleItem && flashSaleItem.description ? flashSaleItem.description : '暂无详情'"></div>
    </div>
    
    <!-- 秒杀结果对话框 -->
    <el-dialog 
      :title="dialogTitle" 
      :visible.sync="dialogVisible" 
      width="30%"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="dialog-content">
        <div class="dialog-icon" :class="dialogSuccess ? 'success' : 'error'">
          <i :class="dialogSuccess ? 'el-icon-success' : 'el-icon-error'"></i>
        </div>
        <div class="dialog-message">{{ dialogMessage }}</div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="handleDialogConfirm">{{ dialogButtonText }}</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getFlashSaleItemDetail, doFlashSale } from '@/api/flashSale';
import DefaultFoodImage from '@/components/DefaultFoodImage';
import { handleImageError } from '@/utils/defaultImage';

export default {
  name: 'FlashSaleDetail',
  
  data() {
    return {
      loading: false,
      buyLoading: false,
      flashSaleItem: null,
      dialogVisible: false,
      dialogTitle: '',
      dialogMessage: '',
      dialogSuccess: false,
      dialogButtonText: '确定',
      saleOrder: null,
      timer: null
    };
  },
  
  created() {
    this.loadData();
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
      const itemId = this.$route.query.id;
      if (!itemId) {
        this.$message.error('商品ID不存在');
        this.$router.push('/flashSale');
        return;
      }
      
      getFlashSaleItemDetail(itemId)
        .then(res => {
          if (res.data && res.data.code === 200) {
            this.flashSaleItem = res.data.data;
          } else {
            this.$message.error(res.data.msg || '获取秒杀商品详情失败');
          }
        })
        .catch(err => {
          console.error(err);
          this.$message.error('获取秒杀商品详情失败');
        })
        .finally(() => {
          this.loading = false;
        });
    },
    
    handleFlashSale() {
      if (!this.flashSaleItem) return;
      
      this.buyLoading = true;
      doFlashSale(this.flashSaleItem.id)
        .then(res => {
          if (res.data && res.data.code === 200) {
            this.dialogSuccess = true;
            this.dialogTitle = '秒杀成功';
            this.dialogMessage = '恭喜您，秒杀成功！';
            this.dialogButtonText = '查看订单';
            this.saleOrder = res.data.data;
          } else {
            this.dialogSuccess = false;
            this.dialogTitle = '秒杀失败';
            this.dialogMessage = res.data.msg || '秒杀失败，请稍后再试';
            this.dialogButtonText = '确定';
            this.saleOrder = null;
          }
          this.dialogVisible = true;
        })
        .catch(err => {
          console.error(err);
          this.dialogSuccess = false;
          this.dialogTitle = '秒杀失败';
          this.dialogMessage = '系统繁忙，请稍后再试';
          this.dialogButtonText = '确定';
          this.saleOrder = null;
          this.dialogVisible = true;
        })
        .finally(() => {
          this.buyLoading = false;
          this.loadData(); // 刷新数据
        });
    },
    
    handleDialogConfirm() {
      this.dialogVisible = false;
      if (this.dialogSuccess && this.saleOrder) {
        this.$router.push('/myOrders');
      }
    },
    
    getStatusText(item) {
      if (!item) return '未知状态';
      
      // 使用isStockEmpty判断是否售罄
      if (item.status === 1 && this.isStockEmpty(item)) return '已售罄';
      if (item.status === 0) return '未开始';
      if (item.status === 1) return '秒杀进行中';
      if (item.status === 2) return '已结束';
      return '未知状态';
    },
    
    getStatusClass(item) {
      if (!item) return '';
      
      // 使用isStockEmpty判断是否售罄
      if (item.status === 1 && this.isStockEmpty(item)) return 'ended';
      if (item.status === 0) return 'upcoming';
      if (item.status === 1) return 'ongoing';
      if (item.status === 2) return 'ended';
      return '';
    },
    
    getBuyButtonText(item) {
      if (!item) return '立即秒杀';
      
      // 使用isStockEmpty判断是否售罄
      if (item.status === 1 && this.isStockEmpty(item)) return '已售罄';
      if (item.status === 0) return '即将开始';
      if (item.status === 1) {
        if (item.canBuy === false) return '已参与';
        return '立即秒杀';
      }
      if (item.status === 2) return '已结束';
      return '立即秒杀';
    },
    
    canBuy(item) {
      if (!item) return false;
      
      // 秒杀进行中，未售罄，用户未参与过
      return item.status === 1 && !this.isStockEmpty(item) && item.canBuy !== false;
    },
    
    // 判断商品是否售罄
    isStockEmpty(item) {
      if (!item) return false;
      
      // 库存为0，或者已售数量等于初始总库存时，认为售罄
      return item.stock <= 0 || (item.soldCount > 0 && item.stock === 0);
    },
    
    getProgressPercentage(item) {
      if (!item || (item.stock === undefined)) return 0;
      
      // 计算总库存（当前库存+已售数量）
      const totalStock = item.stock + (item.soldCount || 0);
      
      // 如果总库存为0，返回0%
      if (totalStock === 0) return 0;
      
      // 计算已售百分比
      return Math.floor(((item.soldCount || 0) / totalStock) * 100);
    },
    
    percentFormat(percent) {
      return `${percent}%已售`;
    },
    
    getProgressColor(item) {
      const percentage = this.getProgressPercentage(item);
      if (percentage < 50) return '#67c23a';
      if (percentage < 80) return '#e6a23c';
      return '#f56c6c';
    },
    
    getDiscountRate(flashPrice, originalPrice) {
      if (!flashPrice || !originalPrice || originalPrice <= 0) return '10';
      const rate = (flashPrice / originalPrice) * 10;
      return rate.toFixed(1);
    },
    
    formatDateTime(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    },
    
    handleImageError(e) {
      // 使用全局图片错误处理
      handleImageError(e);
    }
  },
  
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
          remainingTime: this.time,
          timer: null
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
  }
};
</script>

<style scoped>
.flash-sale-detail {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.detail-content {
  display: flex;
  margin-bottom: 30px;
}

.detail-left {
  width: 400px;
  margin-right: 30px;
}

.detail-right {
  flex: 1;
}

.product-image {
  position: relative;
  width: 100%;
  height: 400px;
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  position: absolute;
  top: 20px;
  right: 20px;
  padding: 8px 16px;
  border-radius: 20px;
  color: white;
  font-weight: bold;
}

.status-badge.ongoing {
  background-color: #67c23a;
}

.status-badge.upcoming {
  background-color: #409eff;
}

.status-badge.ended {
  background-color: #909399;
}

.product-name {
  font-size: 24px;
  margin-bottom: 20px;
}

.product-price {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.price-container {
  flex: 1;
}

.price-label {
  font-size: 14px;
  color: #f56c6c;
  margin-bottom: 5px;
}

.flash-price {
  font-size: 28px;
  font-weight: bold;
  color: #f56c6c;
}

.original-price {
  font-size: 14px;
  color: #909399;
  text-decoration: line-through;
  margin-top: 5px;
}

.discount {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f56c6c;
  color: white;
  font-size: 18px;
  font-weight: bold;
  border-radius: 50%;
}

.countdown-box {
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.countdown-box.ongoing {
  background-color: rgba(103, 194, 58, 0.1);
  border: 1px solid #67c23a;
}

.countdown-box.upcoming {
  background-color: rgba(64, 158, 255, 0.1);
  border: 1px solid #409eff;
}

.countdown-box.ended {
  background-color: rgba(144, 147, 153, 0.1);
  border: 1px solid #909399;
}

.countdown-label {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
}

.countdown-timer {
  font-size: 18px;
}

.product-stock {
  margin-bottom: 20px;
}

.stock-info {
  display: inline-block;
  margin-right: 20px;
  margin-bottom: 10px;
}

.stock-label {
  font-size: 14px;
  color: #606266;
}

.stock-value {
  font-size: 14px;
  font-weight: bold;
}

.product-actions {
  margin-bottom: 20px;
  display: flex;
  gap: 15px;
}

.sale-time {
  background-color: #f8f8f8;
  padding: 15px;
  border-radius: 8px;
}

.time-item {
  margin-bottom: 10px;
}

.time-item:last-child {
  margin-bottom: 0;
}

.time-label {
  color: #606266;
  margin-right: 10px;
}

.time-value {
  font-weight: bold;
}

.product-details {
  margin-top: 30px;
}

.section-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.product-description {
  min-height: 200px;
  line-height: 1.8;
}

/* 对话框样式 */
.dialog-content {
  text-align: center;
  padding: 20px 0;
}

.dialog-icon {
  font-size: 60px;
  margin-bottom: 20px;
}

.dialog-icon.success {
  color: #67c23a;
}

.dialog-icon.error {
  color: #f56c6c;
}

.dialog-message {
  font-size: 18px;
  margin-bottom: 10px;
}

.dialog-footer {
  text-align: center;
  display: block;
}
</style> 