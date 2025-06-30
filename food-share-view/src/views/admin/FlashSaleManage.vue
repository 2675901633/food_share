<template>
  <div class="flash-sale-manage">
    <h2 class="page-title">秒杀商品管理</h2>
    
    <div class="operation-bar">
      <el-button type="primary" @click="openAddDialog">添加秒杀商品</el-button>
      <el-button type="success" @click="handlePreload">预热库存</el-button>
      <el-button type="info" @click="handleRefresh">刷新状态</el-button>
      <el-button @click="$router.push('/flashSaleOrderManage')">订单管理</el-button>
    </div>
    
    <div class="search-bar">
      <el-input
        v-model="searchName"
        placeholder="商品名称"
        clearable
        style="width: 200px;"
        class="filter-item"
      />
      <el-select v-model="searchStatus" placeholder="状态" clearable class="filter-item">
        <el-option label="未开始" :value="0" />
        <el-option label="进行中" :value="1" />
        <el-option label="已结束" :value="2" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        format="yyyy-MM-dd HH:mm"
        value-format="yyyy-MM-dd HH:mm:ss"
        class="filter-item date-picker"
      />
      <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
      <el-button icon="el-icon-refresh" @click="resetSearch">重置</el-button>
    </div>
    
    <el-table
      v-loading="tableLoading"
      :data="itemList"
      border
      style="width: 100%"
    >
      <el-table-column type="index" width="50" align="center" />
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="商品名称" min-width="150">
        <template slot-scope="scope">
          <el-popover
            placement="top-start"
            width="200"
            trigger="hover"
          >
            <div>
              <img 
                v-if="scope.row.image" 
                :src="scope.row.image" 
                alt="商品图片" 
                style="width: 100%;"
                @error="handleImageError"
              >
              <default-food-image v-else style="width: 100%; height: 100px;" />
            </div>
            <span slot="reference">{{ scope.row.name }}</span>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="价格信息" width="180">
        <template slot-scope="scope">
          <div>
            <div class="price-info">
              <span class="label">秒杀价:</span>
              <span class="flash-price">¥{{ scope.row.flashPrice }}</span>
            </div>
            <div class="price-info">
              <span class="label">原价:</span>
              <span class="original-price">¥{{ scope.row.originalPrice }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="库存/销量" width="120" align="center">
        <template slot-scope="scope">
          <div class="stock-count">
            <div>库存: {{ scope.row.stock >= 0 ? scope.row.stock : 0 }}</div>
            <div>已售: {{ scope.row.soldCount || 0 }}</div>
            <div>总量: {{ calculateTotal(scope.row) }}</div>
            <div v-if="scope.row.stock <= 0" class="stock-empty">已售罄</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="活动时间" width="300">
        <template slot-scope="scope">
          <div>
            <i class="el-icon-time"></i>
            开始: {{ formatDate(scope.row.startTime) }}
          </div>
          <div>
            <i class="el-icon-time"></i>
            结束: {{ formatDate(scope.row.endTime) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" align="center">
        <template slot-scope="scope">
          <div class="operation-buttons">
            <el-button
              size="mini"
              type="primary"
              @click="handleEdit(scope.row)"
              icon="el-icon-edit"
            >编辑</el-button>
            <el-button
              size="mini"
              type="danger"
              @click="handleDelete(scope.row)"
              :disabled="scope.row.status === 1 && scope.row.stock <= 0"
              icon="el-icon-delete"
            >删除</el-button>
            <el-button
              v-if="scope.row.status === 1"
              size="small"
              type="warning"
              @click="handleEndSale(scope.row)"
              style="margin-top: 8px; width: 100%; font-weight: bold;"
              icon="el-icon-time"
              :loading="endSaleLoading && endSaleItemId === scope.row.id"
              :disabled="endSaleLoading"
            >立即结束秒杀</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-container">
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page="queryParams.current"
        :page-sizes="[10, 20, 30, 50]"
        :page-size="queryParams.size"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
      />
    </div>
    
    <!-- 添加/编辑对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="50%">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input
            type="textarea"
            v-model="formData.description"
            placeholder="请输入商品描述"
            :rows="4"
          />
        </el-form-item>
        <el-form-item label="商品图片" prop="image">
          <el-upload
            class="avatar-uploader"
            action="http://localhost:21090/api/food-share-sys/v1.0/file/upload"
            :show-file-list="false"
            :on-success="handleImageSuccess"
            :before-upload="beforeImageUpload">
            <img 
              v-if="formData.image" 
              :src="formData.image" 
              class="avatar"
              @error="handleImageError"
            >
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
          <div class="image-tip">支持JPG、PNG格式，建议尺寸400x400像素</div>
          <el-input v-model="formData.image" placeholder="图片URL，可手动输入或通过上方上传" style="margin-top: 10px;"></el-input>
        </el-form-item>
        <el-form-item label="原价" prop="originalPrice">
          <el-input-number
            v-model="formData.originalPrice"
            :precision="2"
            :step="0.01"
            :min="0"
          />
        </el-form-item>
        <el-form-item label="秒杀价" prop="flashPrice">
          <el-input-number
            v-model="formData.flashPrice"
            :precision="2"
            :step="0.01"
            :min="0"
            :max="formData.originalPrice"
          />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number
            v-model="formData.stock"
            :min="1"
            :step="1"
          />
        </el-form-item>
        <el-form-item label="活动时间" prop="timeRange">
          <el-date-picker
            v-model="formData.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="yyyy-MM-dd HH:mm"
            value-format="yyyy-MM-dd HH:mm:ss"
            :default-time="['00:00:00', '23:59:59']"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listFlashSaleItems,
  createFlashSaleItem,
  updateFlashSaleItem,
  deleteFlashSaleItem,
  preloadFlashSaleStock,
  refreshFlashSaleStatus,
  endFlashSale,
  forceEndFlashSale
} from '@/api/flashSale';
import DefaultFoodImage from '@/components/DefaultFoodImage';
import { handleImageError } from '@/utils/defaultImage';

export default {
  name: 'FlashSaleManage',
  
  components: {
    DefaultFoodImage
  },
  
  data() {
    return {
      // 查询条件
      queryParams: {
        name: '',
        status: null,
        startTimeBegin: null,
        startTimeEnd: null,
        endTimeBegin: null,
        endTimeEnd: null,
        current: 1,
        size: 10
      },
      searchName: '',
      searchStatus: null,
      dateRange: [],
      
      // 表格数据
      tableLoading: false,
      itemList: [],
      total: 0,
      
      // 对话框
      dialogVisible: false,
      dialogTitle: '添加秒杀商品',
      submitLoading: false,
      
      // 是否为管理员
      isAdmin: true,
      
      // 表单数据
      formData: {
        id: null,
        name: '',
        description: '',
        image: '',
        originalPrice: 0,
        flashPrice: 0,
        stock: 1,
        timeRange: [],
        startTime: null,
        endTime: null
      },
      
      // 表单验证规则
      formRules: {
        name: [
          { required: true, message: '请输入商品名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        description: [
          { required: true, message: '请输入商品描述', trigger: 'blur' }
        ],
        originalPrice: [
          { required: true, message: '请输入商品原价', trigger: 'blur' }
        ],
        flashPrice: [
          { required: true, message: '请输入秒杀价格', trigger: 'blur' }
        ],
        stock: [
          { required: true, message: '请输入商品库存', trigger: 'blur' }
        ],
        timeRange: [
          { required: true, message: '请选择活动时间', trigger: 'change' }
        ]
      },
      
      // 操作状态
      endSaleLoading: false,
      endSaleItemId: null
    };
  },
  
  created() {
    this.getList();
    
    // 检查是否有查看特定商品的请求
    if (this.$route.query.id) {
      const itemId = parseInt(this.$route.query.id);
      if (!isNaN(itemId)) {
        // 单独加载商品详情
        this.loadItemDetail(itemId);
      }
    }

    // 添加fix滚动事件警告的处理
    this.addPassiveEventListeners();
  },
  
  beforeDestroy() {
    // 移除事件监听器
    this.removePassiveEventListeners();
  },
  
  mounted() {
    // 设置管理员权限
    this.isAdmin = true;
    
    // 监听路由变化，确保路由参数变化时也能正确加载数据
    this.$watch('$route.query.id', (newId) => {
      if (newId) {
        const itemId = parseInt(newId);
        if (!isNaN(itemId)) {
          this.loadItemDetail(itemId);
        }
      }
    });
  },
  
  methods: {
    // 添加被动事件监听器，修复Chrome警告
    addPassiveEventListeners() {
      // 全局添加被动事件选项
      const wheelOpt = { passive: true };
      const wheelEvent = 'onwheel' in document.createElement('div') ? 'wheel' : 'mousewheel';
      
      // 应用到常见的滚动容器
      const scrollContainers = document.querySelectorAll('.el-table__body-wrapper, .el-scrollbar__wrap');
      scrollContainers.forEach(container => {
        container.addEventListener(wheelEvent, () => {}, wheelOpt);
        container.addEventListener('touchstart', () => {}, wheelOpt);
      });
    },
    
    // 移除事件监听器
    removePassiveEventListeners() {
      const wheelEvent = 'onwheel' in document.createElement('div') ? 'wheel' : 'mousewheel';
      const scrollContainers = document.querySelectorAll('.el-table__body-wrapper, .el-scrollbar__wrap');
      scrollContainers.forEach(container => {
        container.removeEventListener(wheelEvent, () => {});
        container.removeEventListener('touchstart', () => {});
      });
    },
    
    // 加载单个商品详情
    loadItemDetail(itemId) {
      this.tableLoading = true;
      // 构造查询参数
      const params = {
        id: itemId,
        current: 1,
        size: 1,
        admin: true // 添加admin标识，确保从数据库获取最新数据
      };
      
      listFlashSaleItems(params)
        .then(res => {
          if (res.data && res.data.code === 200 && res.data.data && res.data.data.length > 0) {
            const item = res.data.data[0];
            this.handleEdit(item);
          } else {
            this.$message.warning(`未找到ID为${itemId}的商品`);
          }
        })
        .catch(err => {
          console.error('加载商品详情失败:', err);
          this.$message.error('加载商品详情失败');
        })
        .finally(() => {
          this.tableLoading = false;
        });
    },
    
    getList() {
      this.tableLoading = true;
      
      // 构造查询参数
      const params = {
        ...this.queryParams,
        admin: true // 添加admin标识，确保从数据库获取最新数据
      };
      
      listFlashSaleItems(params)
        .then(res => {
          if (res.data && res.data.code === 200) {
            // 处理返回的数据，确保库存和销量字段存在
            this.itemList = (res.data.data || []).map(item => {
              return {
                ...item,
                // 确保库存为数字类型
                stock: item.stock != null ? Number(item.stock) : 0,
                // 确保销量为数字类型
                soldCount: item.soldCount != null ? Number(item.soldCount) : 0
              };
            });
            this.total = res.data.total || this.itemList.length;
            
            console.log("处理后的商品列表数据:", this.itemList);
          } else {
            this.$message.error(res.data.msg || '获取秒杀商品列表失败');
            this.itemList = [];
            this.total = 0;
          }
        })
        .catch(err => {
          console.error(err);
          this.$message.error('获取秒杀商品列表失败');
          this.itemList = [];
          this.total = 0;
        })
        .finally(() => {
          this.tableLoading = false;
        });
    },
    
    handleSearch() {
      this.queryParams.current = 1;
      this.queryParams.name = this.searchName;
      this.queryParams.status = this.searchStatus;
      
      if (this.dateRange && this.dateRange.length === 2) {
        // 按开始时间过滤
        this.queryParams.startTimeBegin = this.dateRange[0];
        this.queryParams.startTimeEnd = this.dateRange[1];
      } else {
        this.queryParams.startTimeBegin = null;
        this.queryParams.startTimeEnd = null;
        this.queryParams.endTimeBegin = null;
        this.queryParams.endTimeEnd = null;
      }
      
      this.getList();
    },
    
    resetSearch() {
      this.searchName = '';
      this.searchStatus = null;
      this.dateRange = [];
      this.queryParams = {
        name: '',
        status: null,
        startTimeBegin: null,
        startTimeEnd: null,
        endTimeBegin: null,
        endTimeEnd: null,
        current: 1,
        size: 10
      };
      
      this.getList();
    },
    
    handleCurrentChange(page) {
      this.queryParams.current = page;
      this.getList();
    },
    
    handleSizeChange(size) {
      this.queryParams.size = size;
      this.getList();
    },
    
    openAddDialog() {
      this.dialogTitle = '添加秒杀商品';
      this.formData = {
        id: null,
        name: '',
        description: '',
        image: '',
        originalPrice: 0,
        flashPrice: 0,
        stock: 1,
        timeRange: [],
        startTime: null,
        endTime: null
      };
      this.dialogVisible = true;
      
      // 在对话框打开后，重置表单验证
      this.$nextTick(() => {
        if (this.$refs.formRef) {
          this.$refs.formRef.resetFields();
        }
      });
    },
    
    handleEdit(row) {
      this.dialogTitle = '编辑秒杀商品';
      this.formData = { ...row };
      
      // 设置时间范围
      this.formData.timeRange = [row.startTime, row.endTime];
      
      this.dialogVisible = true;
      
      // 在对话框打开后，重置表单验证
      this.$nextTick(() => {
        if (this.$refs.formRef) {
          this.$refs.formRef.resetFields();
        }
      });
    },
    
    handleDelete(row) {
      this.$confirm(`确认删除商品"${row.name}"吗？`, '警告', {
        type: 'warning'
      })
        .then(() => {
          deleteFlashSaleItem(row.id)
            .then(res => {
              if (res.data && res.data.code === 200) {
                this.$message.success('删除成功');
                this.getList();
              } else {
                this.$message.error(res.data.msg || '删除失败');
              }
            })
            .catch(err => {
              console.error(err);
              this.$message.error('删除失败');
            });
        })
        .catch(() => {});
    },
    
    handlePreload() {
      this.$confirm('确认预热所有秒杀商品库存？这将刷新Redis中的库存数据。', '确认', {
        type: 'info'
      })
        .then(() => {
          preloadFlashSaleStock()
            .then(res => {
              if (res.data && res.data.code === 200) {
                this.$message.success('库存预热成功');
              } else {
                this.$message.error(res.data.msg || '库存预热失败');
              }
            })
            .catch(err => {
              console.error(err);
              this.$message.error('库存预热失败');
            });
        })
        .catch(() => {});
    },
    
    handleRefresh() {
      this.$confirm('确认刷新所有秒杀商品状态？这将根据当前时间重新计算商品状态。', '确认', {
        type: 'info'
      })
        .then(() => {
          refreshFlashSaleStatus()
            .then(res => {
              if (res.data && res.data.code === 200) {
                this.$message.success('状态刷新成功');
                this.getList();
              } else {
                this.$message.error(res.data.msg || '状态刷新失败');
              }
            })
            .catch(err => {
              console.error(err);
              this.$message.error('状态刷新失败');
            });
        })
        .catch(() => {});
    },
    
    handleEndSale(row) {
      this.$confirm(`确认结束商品"${row.name}"的秒杀活动吗？此操作将立即停止该商品的秒杀。`, "警告", {
        confirmButtonText: "确认结束",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          // 设置加载状态
          this.endSaleLoading = true;
          this.endSaleItemId = row.id;
          
          // 显示加载提示
          const loadingInstance = this.$loading({
            lock: true,
            text: "正在结束秒杀活动...",
            spinner: "el-icon-loading",
            background: "rgba(0, 0, 0, 0.7)"
          });
          
          // 先尝试使用专门的结束秒杀API
          endFlashSale(row.id)
            .then(res => {
              loadingInstance.close();
              console.log("结束秒杀活动响应:", res);
              
              if (res.data && res.data.code === 200) {
                this.$notify({
                  title: "成功",
                  message: `秒杀活动"${row.name}"已成功结束`,
                  type: "success",
                  duration: 3000
                });
                
                // 短暂延迟后刷新数据
                setTimeout(() => {
                  this.getList();
                }, 500);
              } else {
                console.warn("专用API结束秒杀失败，尝试使用更新API:", res);
                // 如果专用API失败，尝试使用更新API
                this.fallbackEndSale(row, loadingInstance);
              }
            })
            .catch(err => {
              console.warn("专用API结束秒杀异常，尝试使用更新API:", err);
              // 如果专用API异常，尝试使用更新API
              this.fallbackEndSale(row, loadingInstance);
            });
        })
        .catch(() => {
          this.$message.info("已取消操作");
        });
    },
    
    // 添加后备结束秒杀方法
    fallbackEndSale(row, loadingInstance) {
      // 构造结束数据 - 只发送最少必要的字段
      const updateData = {
        id: row.id,
        status: 2 // 设置状态为已结束
      };
      
      console.log("尝试使用强制结束API，发送数据:", updateData);
      
      // 首先尝试使用专门的强制结束接口
      forceEndFlashSale(updateData)
        .then(res => {
          if (res.data && res.data.code === 200) {
            loadingInstance.close();
            this.$notify({
              title: "成功",
              message: `秒杀活动"${row.name}"已强制结束`,
              type: "success",
              duration: 3000
            });
            
            // 刷新状态缓存
            refreshFlashSaleStatus().then(() => {
              setTimeout(() => this.getList(), 500);
            }).catch(() => {
              setTimeout(() => this.getList(), 500);
            });
          } else {
            // 如果强制结束接口也失败，最后尝试常规更新接口
            console.warn("强制结束API失败，尝试使用常规更新API:", res);
            this.fallbackToUpdateApi(row, loadingInstance);
          }
        })
        .catch(err => {
          console.warn("强制结束API异常，尝试使用常规更新API:", err);
          this.fallbackToUpdateApi(row, loadingInstance);
        });
    },
    
    // 最后的备用方案 - 使用常规更新API
    fallbackToUpdateApi(row, loadingInstance) {
      // 构造结束数据
      const updateData = {
        id: row.id,
        status: 2, // 设置状态为已结束
        endTime: new Date().toISOString().slice(0, 19).replace("T", " ") // 将结束时间设置为当前时间
      };
      
      console.log("最后尝试常规更新API结束秒杀，发送数据:", updateData);
      
      updateFlashSaleItem(updateData)
        .then(res => {
          loadingInstance.close();
          console.log("常规更新API结束秒杀响应:", res);
          
          if (res.data && res.data.code === 200) {
            this.$notify({
              title: "成功",
              message: `秒杀活动"${row.name}"已使用备用方法成功结束`,
              type: "success",
              duration: 3000
            });
          } else {
            const errorMsg = res.data && res.data.msg ? res.data.msg : "未知错误";
            this.$notify({
              title: "警告",
              message: `无法结束秒杀活动: ${errorMsg}，请联系管理员手动处理`,
              type: "warning",
              duration: 5000
            });
          }
          
          // 无论成功失败都尝试刷新列表
          setTimeout(() => this.getList(), 1000);
        })
        .catch(err => {
          loadingInstance.close();
          console.error("结束秒杀活动最终失败:", err);
          
          this.$notify({
            title: "错误",
            message: `结束秒杀活动失败，所有方法均失败，请联系系统管理员`,
            type: "error",
            duration: 5000
          });
          
          setTimeout(() => this.getList(), 1000);
        })
        .finally(() => {
          this.endSaleLoading = false;
          this.endSaleItemId = null;
        });
    },
    
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) {
          return;
        }
        
        this.submitLoading = true;
        
        // 处理表单数据
        const formData = { ...this.formData };
        
        // 设置开始和结束时间
        if (formData.timeRange && formData.timeRange.length === 2) {
          formData.startTime = formData.timeRange[0];
          formData.endTime = formData.timeRange[1];
        }
        
        delete formData.timeRange; // 删除辅助字段
        
        // 添加或更新
        const request = formData.id ? updateFlashSaleItem(formData) : createFlashSaleItem(formData);
        const isCreate = !formData.id;
        
        request
          .then(res => {
            if (res.data && res.data.code === 200) {
              this.$message.success(formData.id ? '更新成功' : '添加成功');
              this.dialogVisible = false;
              
              // 如果是创建操作，预留时间等待后端处理完成
              if (isCreate) {
                // 显示加载提示
                const loadingInstance = this.$loading({
                  lock: true,
                  text: "正在更新商品列表...",
                  spinner: "el-icon-loading",
                  background: "rgba(255, 255, 255, 0.7)"
                });
                
                // 延迟一些时间确保后端处理完成
                setTimeout(() => {
                  // 刷新商品列表
                  this.getList();
                  loadingInstance.close();
                }, 1000);
              } else {
                this.getList();
              }
            } else {
              this.$message.error(res.data.msg || (formData.id ? '更新失败' : '添加失败'));
            }
          })
          .catch(err => {
            console.error(err);
            this.$message.error(formData.id ? '更新失败' : '添加失败');
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    
    getStatusText(status) {
      if (status === 0) return '未开始';
      if (status === 1) return '进行中';
      if (status === 2) return '已结束';
      return '未知';
    },
    
    getStatusType(status) {
      if (status === 0) return 'info';
      if (status === 1) return 'success';
      if (status === 2) return '';
      return 'warning';
    },
    
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    },
    
    handleImageSuccess(response) {
      if (response && response.code === 200) {
        this.formData.image = response.data;
        this.$message.success("图片上传成功");
      } else {
        this.$message.error("图片上传失败：" + (response && response.msg ? response.msg : "未知错误"));
      }
    },
    
    beforeImageUpload(file) {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png';
      const isLt2M = file.size / 1024 / 1024 < 2;
      
      if (!isJPG) {
        this.$message.error('上传图片只能是 JPG 或 PNG 格式!');
      }
      if (!isLt2M) {
        this.$message.error('上传图片大小不能超过 2MB!');
      }
      return isJPG && isLt2M;
    },
    
    // 处理图片加载错误
    handleImageError(e) {
      handleImageError(e);
    },
    
    // 计算商品总量（库存+已售）
    calculateTotal(item) {
      const stock = item.stock >= 0 ? item.stock : 0;
      const soldCount = item.soldCount || 0;
      return stock + soldCount;
    }
  }
};
</script>

<style scoped>
.flash-sale-manage {
  padding: 20px;
}

.page-title {
  margin-bottom: 20px;
}

.operation-bar {
  margin-bottom: 20px;
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

.price-info {
  line-height: 1.5;
}

.label {
  color: #909399;
}

.flash-price {
  color: #f56c6c;
  font-weight: bold;
}

.original-price {
  text-decoration: line-through;
}

.image-preview {
  margin-top: 10px;
  width: 100px;
  height: 100px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
}

.image-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.operation-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 5px;
}

.operation-buttons .el-button {
  margin-left: 0;
  margin-right: 0;
}

.stock-count {
  padding: 5px 0;
}

.stock-count div {
  line-height: 1.8;
  margin-bottom: 3px;
}

.stock-count div:first-child {
  font-weight: bold;
  color: #409EFF;
}

.stock-count div:nth-child(2) {
  color: #F56C6C;
}

.stock-count div:nth-child(3) {
  color: #606266;
  font-size: 12px;
  border-top: 1px dashed #EBEEF5;
  padding-top: 3px;
  margin-top: 3px;
}

.stock-empty {
  color: #F56C6C;
  font-weight: bold;
}
</style> 