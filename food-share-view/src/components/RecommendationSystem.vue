<template>
    <div>
        <div style="display: flex; align-items: center; justify-content: space-between;">
            <p style="font-size: 24px;">猜你想看</p>
            <div style="display: flex; align-items: center; gap: 15px;">
                <el-button type="text" @click="switchBatch" style="font-size: 14px;">
                    <i class="el-icon-refresh" style="margin-right: 3px;"></i>换一批
                </el-button>
                <el-tooltip :content="metrics === null ? '数据加载中' : '查看推荐效果'" placement="top">
                    <i class="el-icon-data-analysis" 
                       :class="{'disabled-icon': metrics === null}"
                       style="cursor: pointer; font-size: 20px;" 
                       @click="metrics !== null && showMetrics()"></i>
                </el-tooltip>
            </div>
        </div>
        <div v-if="loading" style="padding: 20px 0;">
            <el-skeleton :rows="3" animated />
            <el-skeleton :rows="3" animated style="margin-top: 20px"/>
        </div>
        <div v-else class="gourmet-item" v-for="(gourmet, index) in topList" :key="index" @click="readGourmet(gourmet)">
            <div class="left">
                <el-image 
                    :src="gourmet.cover"
                    fit="cover">
                    <div slot="error" class="image-slot">
                        <i class="el-icon-plus"></i>
                    </div>
                </el-image>
            </div>
            <div class="right">
                <div class="user">
                    <img :src="gourmet.userAvatar" alt="" srcset="">
                    <span>{{ gourmet.userName }}</span>
                </div>
                <div class="title">{{ gourmet.title }}</div>
                <div class="info">
                    <span>{{ gourmet.categoryName }}</span>
                    <span>{{ timeOut(gourmet.createTime) }}</span>
                </div>
            </div>
        </div>

        <!-- 统计报告对话框 -->
        <el-dialog
            title="推荐效果统计报告"
            :visible.sync="metricsDialogVisible"
            width="600px"
            custom-class="metrics-dialog"
            :close-on-click-modal="false"
            :append-to-body="true">
            <div v-if="metrics" class="metrics-content">
                <h3 style="margin-bottom: 20px; color: #303133;">当前用户</h3>
                <div class="metrics-card">
                    <div class="metric-item">
                        <div class="metric-title">准确率</div>
                        <div class="metric-value">{{ (metrics.current_user.precision * 100).toFixed(2) }}%</div>
                        <div class="metric-desc">推荐结果中用户实际喜欢的比例</div>
                    </div>
                    <div class="metric-item">
                        <div class="metric-title">召回率</div>
                        <div class="metric-value">{{ (metrics.current_user.recall * 100).toFixed(2) }}%</div>
                        <div class="metric-desc">用户喜欢的内容被推荐的比例</div>
                    </div>
                </div>
                
                <h3 style="margin: 30px 0 20px; color: #303133;">全局指标</h3>
                <div class="metrics-card">
                    <div class="metric-item">
                        <div class="metric-title">全局准确率</div>
                        <div class="metric-value">{{ (metrics.global_metrics.precision * 100).toFixed(2) }}%</div>
                        <div class="metric-desc">所有用户的平均准确率</div>
                    </div>
                    <div class="metric-item">
                        <div class="metric-title">全局召回率</div>
                        <div class="metric-value">{{ (metrics.global_metrics.adjusted_recall * 100).toFixed(2) }}%</div>
                        <div class="metric-desc">所有用户的平均召回率</div>
                    </div>
                </div>
            </div>
            <div v-else class="metrics-empty">
                <el-empty description="数据加载中"></el-empty>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import { timeAgo } from "@/utils/data"
import { recommendApi } from '@/api/recommend'

export default {
    name: "RecommendationSystem",
    data() {
        return {
            loading: true,
            retryCount: 0,
            maxRetries: 3,
            metricsDialogVisible: false,
            metrics: null,
            topList: [],
            defaultLoadItem: 5,
            currentBatch: 0, // 0表示第一批，1表示第二批
            allRecommendations: [] // 存储所有推荐内容
        }
    },
    computed: {
        displayList() {
            const start = this.currentBatch * this.defaultLoadItem;
            return this.allRecommendations.slice(start, start + this.defaultLoadItem);
        }
    },
    methods: {
        timeOut(time) {
            return timeAgo(time);
        },
        loadRecommend(item) {
            const token = localStorage.getItem('token');
            console.log("开始加载推荐数据，用户token:", token ? "已登录" : "未登录");
            
            // 检查用户是否登录
            if (!token) {
                console.warn("用户未登录，无法获取个性化推荐");
                this.loading = false;
                this.allRecommendations = [];
                this.topList = [];
                return;
            }
            
            this.loading = true;
            
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 60000);

            console.log("发送推荐请求到:", `/recommend/10`);
            recommendApi.getRecommendationList()
                .then(res => {
                    clearTimeout(timeoutId);
                    console.log("推荐请求响应:", res);
                    const { data } = res;
                    if (data && data.code === 200) {
                        this.handleRecommendData(res.data);
                        this.loadMetrics();
                    } else {
                        console.error("推荐请求返回错误:", data ? data.message : '未知错误');
                        this.handleRecommendError(data ? data.message : '未知错误');
                    }
                })
                .catch(error => {
                    clearTimeout(timeoutId);
                    console.error("推荐请求异常:", error);
                    this.handleRecommendError(error);
                    this.loadMetrics();
                });
        },

        loadMetrics() {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 45000);

            recommendApi.evaluateMetricsApi()
                .then(res => {
                    clearTimeout(timeoutId);
                    if (res.data && res.data.code === 200) {
                        this.metrics = res.data.data;
                    } else {
                        this.retryLoadMetrics(1);
                    }
                })
                .catch(error => {
                    clearTimeout(timeoutId);
                    console.warn("加载评估指标失败:", error.message || error);
                    this.retryLoadMetrics(1);
                });
        },

        retryLoadMetrics(retryCount) {
            if (retryCount <= 3) {
                console.log(`第${retryCount}次重试获取统计数据`);
                setTimeout(() => {
                    const controller = new AbortController();
                    const timeoutId = setTimeout(() => controller.abort(), 30000);

                    recommendApi.evaluateMetricsApi()
                        .then(res => {
                            clearTimeout(timeoutId);
                            if (res.data.code === 200) {
                                this.metrics = res.data.data;
                            } else {
                                this.retryLoadMetrics(retryCount + 1);
                            }
                        })
                        .catch(error => {
                            clearTimeout(timeoutId);
                            if (retryCount === 3) {
                                this.$message.error('获取统计数据失败，请稍后重试');
                                this.metrics = null;
                            } else {
                                this.retryLoadMetrics(retryCount + 1);
                            }
                        });
                }, 1000 * retryCount);
            }
        },

        showMetrics() {
            if (!this.metrics) {
                this.loadMetrics();
            }
            this.metricsDialogVisible = true;
        },
        
        handleRecommendData(data) {
            try {
                console.log("推荐系统收到的原始数据:", data);
                
                // 检查是否是Python推荐系统的响应
                if (data && data.data && data.data.recommended_gourmets) {
                    console.log("检测到Python推荐系统数据结构");
                    this.allRecommendations = data.data.recommended_gourmets;
                    console.log("使用Python推荐系统数据:", this.allRecommendations.length);
                }
                // 检查是否是Java后端的响应
                else if (data && data.data && Array.isArray(data.data)) {
                    console.log("检测到Java后端数据结构");
                    this.allRecommendations = data.data;
                    console.log("使用Java后端数据:", this.allRecommendations.length);
                }
                // 尝试处理其他可能的数据结构
                else if (data && Array.isArray(data)) {
                    console.log("检测到直接数组数据结构");
                    this.allRecommendations = data;
                    console.log("使用直接数组数据:", this.allRecommendations.length);
                }
                else if (data && typeof data === 'object') {
                    console.log("检测到对象数据结构，尝试提取数据");
                    // 尝试找到数据中的数组
                    let foundArray = null;
                    for (const key in data) {
                        if (Array.isArray(data[key])) {
                            foundArray = data[key];
                            console.log(`在键 "${key}" 中找到数组数据`);
                            break;
                        } else if (data[key] && typeof data[key] === 'object') {
                            for (const subKey in data[key]) {
                                if (Array.isArray(data[key][subKey])) {
                                    foundArray = data[key][subKey];
                                    console.log(`在键 "${key}.${subKey}" 中找到数组数据`);
                                    break;
                                }
                            }
                            if (foundArray) break;
                        }
                    }
                    
                    if (foundArray) {
                        this.allRecommendations = foundArray;
                        console.log("使用提取的数组数据:", this.allRecommendations.length);
                    } else {
                        console.warn("无法在对象中找到数组数据，设置为空数组");
                        this.allRecommendations = [];
                    }
                }
                else {
                    console.warn("未识别的数据结构，设置为空数组:", data);
                    this.allRecommendations = [];
                }
                
                // 确保allRecommendations始终是数组
                if (!Array.isArray(this.allRecommendations)) {
                    console.warn("推荐数据不是数组，强制转换为数组");
                    this.allRecommendations = [];
                }
                
                console.log("最终推荐数据:", this.allRecommendations);
                this.topList = this.displayList;
                console.log("显示推荐数据:", this.topList);
                this.retryCount = 0;
                this.loading = false;
            } catch (error) {
                console.error("处理推荐数据时出错:", error);
                this.allRecommendations = [];
                this.topList = [];
                this.loading = false;
            }
        },

        switchBatch() {
            this.currentBatch = this.currentBatch === 0 ? 1 : 0;
            this.topList = this.displayList;
        },
        
        readGourmet(gourmet) {
            console.log("点击美食，准备跳转:", gourmet.id);
            
            // 更新sessionStorage，确保与URL参数保持一致
            sessionStorage.setItem('gourmetId', gourmet.id);
            
            // 检查是否是当前页面
            const isSamePage = this.$route.path === '/gourmetDetail' && 
                               this.$route.query.id == gourmet.id;
            
            if (isSamePage) {
                // 已经在相同页面，强制刷新组件
                this.$router.replace({
                    path: '/gourmetDetail',
                    query: {
                        id: gourmet.id,
                        _t: new Date().getTime() // 添加时间戳确保刷新
                    }
                }).catch(() => {});
                return;
            }
            
            // 跳转到美食详情页
            this.$router.push({
                path: "/gourmetDetail",
                query: {
                    id: gourmet.id
                }
            }).catch(err => {
                // 忽略导航重复错误
                if (err.name !== 'NavigationDuplicated') {
                    console.error("路由错误:", err);
                }
            });
        },
        
        handleRecommendError(error) {
            console.error("推荐系统错误:", error);
            this.loading = false;
            this.retryCount++;
            
            if (this.retryCount <= this.maxRetries) {
                setTimeout(() => {
                    this.loadRecommend();
                }, 1000 * this.retryCount);
            } else {
                this.$message.error("获取推荐数据失败，请稍后重试");
                // 显示空数据
                this.allRecommendations = [];
                this.topList = [];
            }
        }
    },
    mounted() {
        this.loadRecommend();
    }
}
</script>

<style scoped lang="scss">
.gourmet-item {
    display: flex;
    justify-content: left;
    padding: 10px 0;
    cursor: pointer;

    .left {
        padding: 5px;
        box-sizing: border-box;
        width: 190px;
        height: 145px;
        flex-shrink: 0;

        .el-image {
            width: 180px;
            height: 135px;
            border-radius: 5px;
            display: block;
        }
    }

    .right {
        padding: 5px 0 5px 15px;
        box-sizing: border-box;
        flex: 1;

        .user {
            display: flex;
            justify-content: left;
            align-items: center;
            margin-block: 4px;

            img {
                width: 20px;
                height: 20px;
                border-radius: 10px;
            }

            span {
                margin-left: 4px;
                font-size: 12px;
            }
        }

        .title {
            font-size: 16px;
            width: 100%;
            font-weight: 800;
            padding-bottom: 6px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .title:hover {
            text-decoration: underline;
            text-decoration-style: dashed;
        }

        .info {
            font-size: 12px;
            margin-top: 4px;
            display: flex;
            justify-content: left;
            gap: 10px;

            span:first-child {
                display: inline-block;
                padding: 1px 3px;
                border-radius: 2px;
                background-color: rgb(237, 243, 249);
                color: rgb(136, 115, 233);
            }
        }
    }
}

// 对话框样式
::v-deep .metrics-dialog {
  .el-dialog {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);

    &__header {
      padding: 24px;
      border-bottom: 1px solid #EBEEF5;
      margin-right: 0;
      background: linear-gradient(135deg, #409EFF 0%, #36D1DC 100%);
      color: white;
      text-align: center;
      position: relative;
    }

    &__headerbtn {
      position: absolute;
      top: 50%;
      right: 20px;
      transform: translateY(-50%);
      width: 20px;
      height: 20px;
      padding: 0;
      background: rgba(255, 255, 255, 0.1);
      border: none;
      outline: none;
      cursor: pointer;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s ease;

      &:hover {
        background: rgba(255, 255, 255, 0.2);
      }

      .el-dialog__close {
        color: white;
        font-size: 18px;
        transition: all 0.3s ease;
        
        &:hover {
          transform: rotate(90deg);
        }
      }
    }

    &__title {
      line-height: 24px;
      font-size: 20px;
      color: white;
      font-weight: 600;
      padding: 0 40px;
    }

    &__body {
      padding: 0;
    }
  }

  .metrics-content {
    padding: 24px;
  }

  .metrics-card {
    display: flex;
    justify-content: space-between;
    margin-bottom: 30px;
    gap: 20px;
    
    .metric-item {
      flex: 1;
      text-align: center;
      padding: 24px;
      background: linear-gradient(135deg, #F5F7FA 0%, #E4E7EB 100%);
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
      }
      
      .metric-title {
        color: #606266;
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 12px;
      }
      
      .metric-value {
        color: #409EFF;
        font-size: 32px;
        font-weight: bold;
        margin-bottom: 12px;
      }
      
      .metric-desc {
        color: #909399;
        font-size: 14px;
      }
    }
  }
}

.disabled-icon {
    color: #ccc;
    cursor: not-allowed !important;
}

.el-button.el-button--text {
    padding: 0;
    height: auto;
    
    &:hover {
        color: #409EFF;
    }
    
    i {
        font-size: 14px;
    }
}
</style>