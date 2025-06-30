<template>
  <div class="order-manage">
    <h2 class="page-title">秒杀订单管理</h2>
    
    <div class="operation-bar">
      <el-button @click="$router.push('/flashSaleManage')">返回商品管理</el-button>
      <el-button type="primary" @click="loadData">刷新数据</el-button>
    </div>
    
    <div class="search-bar">
      <el-input
        v-model="searchOrderId"
        placeholder="订单号"
        clearable
        style="width: 200px;"
        class="filter-item"
      />
      <el-input
        v-model="searchUserId"
        placeholder="用户ID"
        clearable
        style="width: 100px;"
        class="filter-item"
      />
      <el-input
        v-model="searchItemId"
        placeholder="商品ID"
        clearable
        style="width: 100px;"
        class="filter-item"
      />
      <el-select v-model="searchStatus" placeholder="订单状态" clearable class="filter-item">
        <el-option label="待支付" :value="1" />
        <el-option label="已支付" :value="2" />
        <el-option label="已取消" :value="3" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="下单开始日期"
        end-placeholder="下单结束日期"
        format="yyyy-MM-dd HH:mm"
        value-format="yyyy-MM-dd HH:mm:ss"
        class="filter-item date-picker"
      />
      <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
      <el-button icon="el-icon-refresh" @click="resetSearch">重置</el-button>
    </div>
    
    <el-table
      v-loading="tableLoading"
      :data="orderList"
      border
      style="width: 100%"
    >
      <el-table-column type="index" width="50" align="center" />
      <el-table-column prop="orderId" label="订单号" width="240" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户ID" width="80" align="center" />
      <el-table-column prop="itemId" label="商品ID" width="80" align="center" />
      <el-table-column prop="price" label="价格" width="100" align="center">
        <template slot-scope="scope">
          <span class="price">¥{{ scope.row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="orderTime" label="下单时间" width="180">
        <template slot-scope="scope">
          {{ formatDate(scope.row.orderTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getOrderStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建/更新时间" width="280">
        <template slot-scope="scope">
          <div>创建: {{ formatDate(scope.row.createTime) }}</div>
          <div>更新: {{ formatDate(scope.row.updateTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="success"
            @click="handleViewItem(scope.row.itemId)"
          >查看商品</el-button>
          <el-button
            size="mini"
            type="danger"
            @click="handleDelete(scope.row)"
            v-if="scope.row.status === 3"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-container">
      <el-pagination
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
        :current-page="queryParams.current"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="queryParams.size"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
      />
    </div>

    <!-- 订单详情对话框 -->
    <el-dialog :title="'订单详情'" :visible.sync="dialogVisible" width="30%">
      <div v-if="selectedOrder" class="order-detail">
        <div class="detail-item">
          <span class="label">订单号:</span>
          <span class="value">{{ selectedOrder.orderId }}</span>
        </div>
        <div class="detail-item">
          <span class="label">用户ID:</span>
          <span class="value">{{ selectedOrder.userId }}</span>
        </div>
        <div class="detail-item">
          <span class="label">商品ID:</span>
          <span class="value">{{ selectedOrder.itemId }}</span>
        </div>
        <div class="detail-item">
          <span class="label">价格:</span>
          <span class="value price">¥{{ selectedOrder.price }}</span>
        </div>
        <div class="detail-item">
          <span class="label">下单时间:</span>
          <span class="value">{{ formatDate(selectedOrder.orderTime) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">状态:</span>
          <span class="value">
            <el-tag :type="getStatusType(selectedOrder.status)">
              {{ getOrderStatusText(selectedOrder.status) }}
            </el-tag>
          </span>
        </div>
        <div class="detail-item">
          <span class="label">创建时间:</span>
          <span class="value">{{ formatDate(selectedOrder.createTime) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">更新时间:</span>
          <span class="value">{{ formatDate(selectedOrder.updateTime) }}</span>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { queryMyOrders, getAllOrders } from '@/api/flashSale';

export default {
  name: 'FlashSaleOrderManage',
  
  data() {
    return {
      // 查询条件
      queryParams: {
        current: 1,
        size: 10,
        orderId: '',
        userId: null,
        itemId: null,
        status: null,
        orderTimeBegin: null,
        orderTimeEnd: null
      },
      searchOrderId: '',
      searchUserId: '',
      searchItemId: '',
      searchStatus: null,
      dateRange: [],
      
      // 表格数据
      tableLoading: false,
      orderList: [],
      total: 0,
      
      // 对话框
      dialogVisible: false,
      selectedOrder: null
    };
  },
  
  created() {
    this.loadData();
  },
  
  methods: {
    loadData() {
      this.tableLoading = true;
      
      // 使用管理员接口获取所有订单数据
      const params = {
        current: this.queryParams.current,
        size: this.queryParams.size,
        orderId: this.queryParams.orderId || null,
        userId: this.queryParams.userId || null,
        itemId: this.queryParams.itemId || null,
        status: this.queryParams.status || null,
        orderTimeBegin: this.queryParams.orderTimeBegin || null,
        orderTimeEnd: this.queryParams.orderTimeEnd || null
      };
      
      getAllOrders(params)
        .then(res => {
          if (res.data && res.data.code === 200) {
            this.orderList = res.data.data.data || [];
            this.total = res.data.data.total || this.orderList.length;
          } else {
            this.$message.error(res.data.msg || '获取订单数据失败');
            this.orderList = [];
            this.total = 0;
          }
        })
        .catch(err => {
          console.error(err);
          this.$message.error('获取订单数据失败');
          this.orderList = [];
          this.total = 0;
        })
        .finally(() => {
          this.tableLoading = false;
        });
    },
    
    handleSearch() {
      this.queryParams.current = 1;
      this.queryParams.orderId = this.searchOrderId;
      this.queryParams.userId = this.searchUserId ? parseInt(this.searchUserId) : null;
      this.queryParams.itemId = this.searchItemId ? parseInt(this.searchItemId) : null;
      this.queryParams.status = this.searchStatus;
      
      if (this.dateRange && this.dateRange.length === 2) {
        this.queryParams.orderTimeBegin = this.dateRange[0];
        this.queryParams.orderTimeEnd = this.dateRange[1];
      } else {
        this.queryParams.orderTimeBegin = null;
        this.queryParams.orderTimeEnd = null;
      }
      
      this.loadData();
    },
    
    resetSearch() {
      this.searchOrderId = '';
      this.searchUserId = '';
      this.searchItemId = '';
      this.searchStatus = null;
      this.dateRange = [];
      
      this.queryParams = {
        current: 1,
        size: 10,
        orderId: '',
        userId: null,
        itemId: null,
        status: null,
        orderTimeBegin: null,
        orderTimeEnd: null
      };
      
      this.loadData();
    },
    
    handleCurrentChange(page) {
      this.queryParams.current = page;
      this.loadData();
    },
    
    handleSizeChange(size) {
      this.queryParams.size = size;
      this.loadData();
    },
    
    handleViewOrder(row) {
      this.selectedOrder = row;
      this.dialogVisible = true;
    },
    
    handleViewItem(itemId) {
      this.$router.push({
        path: '/flashSaleManage',
        query: { id: itemId }
      });
    },
    
    handleDelete(row) {
      this.$confirm(`确认删除订单 ${row.orderId} 吗？`, '警告', {
        type: 'warning'
      })
        .then(() => {
          // 这里应该调用删除订单API，但由于我们没有实现，仅做提示
          this.$message({
            type: 'info',
            message: '该功能尚未实现，请联系系统管理员'
          });
        })
        .catch(() => {});
    },
    
    getOrderStatusText(status) {
      if (status === 1) return '待支付';
      if (status === 2) return '已支付';
      if (status === 3) return '已取消';
      return '未知状态';
    },
    
    getStatusType(status) {
      if (status === 1) return 'warning';
      if (status === 2) return 'success';
      if (status === 3) return 'info';
      return '';
    },
    
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    }
  }
};
</script>

<style scoped>
.order-manage {
  padding: 20px;
}

.page-title {
  margin-bottom: 20px;
}

.operation-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.filter-item {
  margin-right: 10px;
}

.date-picker {
  width: 380px;
}

.pagination-container {
  margin-top: 20px;
  text-align: center;
}

.price {
  color: #f56c6c;
  font-weight: bold;
}

.order-detail {
  padding: 10px;
}

.detail-item {
  margin-bottom: 15px;
  line-height: 1.5;
}

.label {
  color: #909399;
  margin-right: 10px;
  width: 80px;
  display: inline-block;
  text-align: right;
}

.value {
  font-weight: bold;
}
</style> 