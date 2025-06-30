<template>
  <div class="my-orders-container">
    <div class="page-header">
      <el-breadcrumb separator-class="el-icon-arrow-right">
        <el-breadcrumb-item :to="{ path: '/flashSale' }">限时秒杀</el-breadcrumb-item>
        <el-breadcrumb-item>我的秒杀订单</el-breadcrumb-item>
      </el-breadcrumb>
      <h2>我的秒杀订单</h2>
    </div>

    <div class="orders-filter">
      <el-button type="primary" size="small" @click="loadData">刷新</el-button>
      <el-button type="success" size="small" @click="$router.push('/flashSale')">继续秒杀</el-button>
    </div>

    <div v-loading="loading" class="orders-content">
      <div v-if="orders.length === 0" class="empty-data">
        <i class="el-icon-tickets"></i>
        <p>暂无订单数据</p>
        <el-button type="primary" @click="$router.push('/flashSale')">去秒杀</el-button>
      </div>

      <div v-else class="orders-list">
        <el-card v-for="order in orders" :key="order.id" class="order-card" shadow="hover">
          <div class="order-header">
            <div class="order-id">
              <span class="label">订单号:</span>
              <span class="value">{{ order.orderId }}</span>
            </div>
            <div class="order-status" :class="getOrderStatusClass(order.status)">
              {{ getOrderStatusText(order.status) }}
            </div>
          </div>

          <div class="order-info">
            <div class="order-item-info">
              <div class="item-id">
                <span class="label">商品ID:</span>
                <span class="value">{{ order.itemId }}</span>
              </div>
              <div class="item-price">
                <span class="label">价格:</span>
                <span class="price-value">¥{{ order.price }}</span>
              </div>
            </div>
            <div class="order-time-info">
              <div class="order-time">
                <i class="el-icon-time"></i>
                <span>下单时间: {{ formatDateTime(order.orderTime) }}</span>
              </div>
              <div class="order-actions">
                <el-button 
                  type="primary" 
                  size="small"
                  :disabled="order.status !== 1" 
                  @click="handlePayOrder(order)"
                >
                  支付订单
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  plain
                  :disabled="order.status !== 1" 
                  @click="handleCancelOrder(order)"
                >
                  取消订单
                </el-button>
                <el-button 
                  type="info" 
                  size="small"
                  @click="viewItemDetail(order.itemId)"
                >
                  查看商品
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <div class="pagination" v-if="orders.length > 0">
        <el-pagination
          @current-change="handleCurrentChange"
          :current-page.sync="currentPage"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          :total="total"
        ></el-pagination>
      </div>
    </div>
    
    <!-- 操作确认对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="30%"
    >
      <span>{{ dialogMessage }}</span>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDialog" :loading="actionLoading">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { queryMyOrders, cancelOrder, payOrder } from '@/api/flashSale';

export default {
  name: 'MyFlashSaleOrders',
  
  data() {
    return {
      loading: false,
      actionLoading: false,
      orders: [],
      currentPage: 1,
      pageSize: 5,
      total: 0,
      dialogVisible: false,
      dialogTitle: '',
      dialogMessage: '',
      currentOrder: null,
      actionType: '' // 'pay' or 'cancel'
    };
  },
  
  created() {
    this.loadData();
  },
  
  methods: {
    loadData() {
      this.loading = true;
      
      queryMyOrders()
        .then(res => {
          if (res.data && res.data.code === 200) {
            this.orders = res.data.data || [];
            this.total = this.orders.length;
            
            // 简单的分页处理（前端分页）
            const start = (this.currentPage - 1) * this.pageSize;
            const end = start + this.pageSize;
            this.orders = this.orders.slice(start, end);
          } else {
            this.$message.error(res.data.msg || '获取订单失败');
            this.orders = [];
            this.total = 0;
          }
        })
        .catch(err => {
          console.error(err);
          this.$message.error('获取订单失败');
          this.orders = [];
          this.total = 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    
    handleCurrentChange() {
      this.loadData();
    },
    
    handlePayOrder(order) {
      this.currentOrder = order;
      this.dialogTitle = '支付确认';
      this.dialogMessage = `确定要支付订单 ${order.orderId} 吗？`;
      this.actionType = 'pay';
      this.dialogVisible = true;
    },
    
    handleCancelOrder(order) {
      this.currentOrder = order;
      this.dialogTitle = '取消确认';
      this.dialogMessage = `确定要取消订单 ${order.orderId} 吗？`;
      this.actionType = 'cancel';
      this.dialogVisible = true;
    },
    
    confirmDialog() {
      if (!this.currentOrder) {
        this.dialogVisible = false;
        return;
      }
      
      this.actionLoading = true;
      
      if (this.actionType === 'pay') {
        this.payOrder();
      } else if (this.actionType === 'cancel') {
        this.cancelOrder();
      }
    },
    
    payOrder() {
      console.log('准备支付订单:', this.currentOrder.orderId);
      payOrder(this.currentOrder.orderId)
        .then(res => {
          console.log('支付订单响应:', res);
          if (res.data && res.data.code === 200) {
            this.$message.success('订单支付成功');
            this.loadData();
            
            // 模拟支付成功后的操作
            setTimeout(() => {
              this.$notify({
                title: '支付成功',
                message: `订单${this.currentOrder.orderId}支付成功，感谢您的购买！`,
                type: 'success',
                duration: 3000
              });
            }, 500);
          } else {
            this.$message.error(res.data.msg || '订单支付失败');
            setTimeout(() => this.loadData(), 1000);
          }
        })
        .catch(err => {
          console.error('支付订单错误:', err);
          this.$message.error('订单支付失败');
        })
        .finally(() => {
          this.actionLoading = false;
          this.dialogVisible = false;
        });
    },
    
    cancelOrder() {
      cancelOrder(this.currentOrder.orderId)
        .then(res => {
          if (res.data && res.data.code === 200) {
            this.$message.success('订单取消成功');
            this.loadData();
          } else {
            this.$message.error(res.data.msg || '订单取消失败');
          }
        })
        .catch(err => {
          console.error(err);
          this.$message.error('订单取消失败');
        })
        .finally(() => {
          this.actionLoading = false;
          this.dialogVisible = false;
        });
    },
    
    viewItemDetail(itemId) {
      this.$router.push({
        path: '/flashSaleDetail',
        query: { id: itemId }
      });
    },
    
    getOrderStatusText(status) {
      if (status === 1) return '待支付';
      if (status === 2) return '已支付';
      if (status === 3) return '已取消';
      return '未知状态';
    },
    
    getOrderStatusClass(status) {
      if (status === 1) return 'pending';
      if (status === 2) return 'paid';
      if (status === 3) return 'canceled';
      return '';
    },
    
    formatDateTime(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    }
  }
};
</script>

<style scoped>
.my-orders-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin-top: 15px;
}

.orders-filter {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
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

.empty-data p {
  margin-bottom: 20px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
}

.order-card {
  border-left: 4px solid #409eff;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 15px;
}

.order-id {
  font-size: 14px;
}

.order-status {
  padding: 5px 12px;
  border-radius: 15px;
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.order-status.pending {
  background-color: #e6a23c;
}

.order-status.paid {
  background-color: #67c23a;
}

.order-status.canceled {
  background-color: #909399;
}

.label {
  color: #606266;
  margin-right: 5px;
}

.value {
  font-weight: bold;
}

.price-value {
  color: #f56c6c;
  font-weight: bold;
}

.order-info {
  display: flex;
  justify-content: space-between;
}

.order-item-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-time-info {
  text-align: right;
}

.order-time {
  margin-bottom: 15px;
  color: #606266;
  font-size: 14px;
}

.order-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}
</style> 