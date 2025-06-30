<template>
    <div style="background-color: rgb(255,255,255);padding: 20px;">
        <el-row>
            <el-col :span="18">
                <div style="margin: 40px 0;">
                    <span style="font-size: 24px;color: rgb(51,51,51);font-weight: bold;">集锦</span>
                    <el-input size="small" style="width: 266px;float: right;" v-model="gourtmetQueryDto.title"
                        placeholder="搜索美食" clearable @clear="handleFilterClear">
                        <el-button slot="append" @click="fetchGourmetData" icon="el-icon-search"></el-button>
                    </el-input>
                </div>
                <div style="margin-block: 20px;">
                    <span :style="{
                        backgroundColor: categoryClick.id === category.id ? 'rgb(38, 151, 70)' : '',
                        color: categoryClick.id === category.id ? 'rgb(245,245,245)' : ''
                    }" @click="categorySelected(category)" class="item-category"
                        v-for="(category, index) in categories" :key="index">
                        {{ category.name }}
                    </span>
                </div>
                <div v-if="gourmetList.length === 0">
                    <el-empty description="美食做法暂时未找到"></el-empty>
                </div>
                <div v-else>
                    <div style="display: flex;justify-content: left;" class="item-gourmet"
                        v-for="(gourmet, index) in gourmetList" :key="index">
                        <div class="left">
                            <img :src="gourmet.cover">
                        </div>
                        <div class="right">
                            <div class="info">
                                <img style="width: 20px;height: 20px;border-radius: 10px;" :src="gourmet.userAvatar"
                                    alt="">
                                <span>{{ gourmet.userName }}</span>
                            </div>
                            <div style="margin-left: 4px;font-size: 24px;" class="title" @click="readGourmet(gourmet)">
                                {{ gourmet.title }}
                            </div>
                            <div class="detail">
                                {{ gourmet.detail }}
                            </div>
                            <div class="detail">
                                <span>
                                    {{ gourmet.createTime }}
                                </span>
                                <span>
                                    <i class="el-icon-discount" style="margin-right: 4px;"></i>({{ gourmet.upvoteCount
                                    }})
                                </span>
                                <span>
                                    <i class="el-icon-view" style="margin-right: 4px;"></i>({{ gourmet.viewCount }})
                                </span>
                                <span>
                                    <i class="el-icon-star-off" style="margin-right: 4px;"></i>({{ gourmet.saveCount }})
                                </span>
                            </div>
                        </div>
                    </div>
                    <el-pagination style="margin: 10px 0;" @size-change="handleSizeChange"
                        @current-change="handleCurrentChange" :current-page="currentPage" :page-sizes="[10, 20]"
                        :page-size="pageSize" layout="total, sizes, prev, pager, next, jumper" :total="totalItems">
                    </el-pagination>
                </div>
            </el-col>
            <el-col :span="6" style="padding: 10px 30px;box-sizing: border-box;position: sticky;top: 60px;">
                <recommendation-system @recommend-click="readGourmet" />

                <!-- 附近美食推荐 -->
                <el-card class="nearby-restaurants-card" shadow="hover" style="margin-top: 20px;">
                    <div slot="header" class="card-header">
                        <span><i class="el-icon-location-outline"></i> 附近美食推荐</span>
                        <el-button
                            type="text"
                            size="mini"
                            @click="getCurrentLocation"
                            :loading="geoLoading">
                            <i class="el-icon-location"></i> 定位
                        </el-button>
                    </div>

                    <div v-if="!userLocation.latitude" class="location-prompt">
                        <i class="el-icon-location-information"></i>
                        <p>点击定位按钮获取附近美食推荐</p>
                    </div>

                    <div v-else>
                        <div class="location-info">
                            <small>当前位置: {{ userLocation.address || '已定位' }}</small>
                        </div>

                        <div v-if="nearbyRestaurants.length > 0" class="nearby-list">
                            <div
                                v-for="gourmet in nearbyRestaurants.slice(0, 3)"
                                :key="gourmet.id"
                                class="nearby-item"
                                @click="viewGourmetDetail(gourmet)">
                                <div class="gourmet-image">
                                    <img :src="gourmet.cover || '/default-food.jpg'" :alt="gourmet.title" />
                                </div>
                                <div class="gourmet-info">
                                    <h4>{{ gourmet.title }}</h4>
                                    <p><i class="el-icon-location-outline"></i> {{ gourmet.locationName || '附近' }}</p>
                                    <p v-if="gourmet.distance"><i class="el-icon-position"></i> {{ Math.round(gourmet.distance) }}m</p>
                                </div>
                            </div>
                        </div>

                        <div v-else class="no-nearby">
                            <p>附近暂无推荐美食</p>
                        </div>

                        <el-button
                            size="small"
                            type="text"
                            @click="refreshNearbyRestaurants"
                            style="width: 100%; margin-top: 10px;">
                            刷新附近推荐
                        </el-button>
                    </div>
                </el-card>


            </el-col>
        </el-row>
    </div>
</template>

<script>
import { timeAgo } from "@/utils/data"
import RecommendationSystem from '@/components/RecommendationSystem.vue'

export default {
    name: "Gourmet",
    components: {
        RecommendationSystem
    },
    data() {
        return {
            gourtmetQueryDto: {},
            gourmetList: [],
            categories: [],
            filterText: '',
            currentPage: 1,
            pageSize: 10,
            totalItems: 0,
            categoryClick: { id: null, name: '全部' },

            // Redis GEO 相关数据
            userLocation: {
                latitude: null,
                longitude: null,
                address: null
            },
            nearbyRestaurants: [],
            geoLoading: false,


        }
    },
    created() {
        this.fetchCategoryData();
        this.fetchGourmetData();
    },
    methods: {
        timeOut(time) {
            return timeAgo(time);
        },
        readGourmet(gourmet) {
            // 统一使用查询参数方式跳转
            this.$router.push(`/gourmetDetail?id=${gourmet.id}`);
        },
        handleFilterClear() {
            this.filterText = '';
            this.fetchGourmetData();
        },
        // 页面大小改变
        handleSizeChange(val) {
            this.pageSize = val;
            this.currentPage = 1;
            this.fetchGourmetData();
        },
        // 当前页切换
        handleCurrentChange(val) {
            this.currentPage = val;
            this.fetchGourmetData();
        },
        categorySelected(category) {
            this.categoryClick = category;
            this.gourtmetQueryDto.categoryId = category.id;
            this.fetchGourmetData();
        },
        // 查询美食类别
        fetchCategoryData() {
            this.$axios.post('/category/query', {}).then(res => {
                const { data } = res;
                if (data.code === 200) {
                    this.categories = res.data.data;
                    this.categories.unshift(this.categoryClick);
                }
            }).catch(error => {
                console.log(error);
            });
        },
        // 查询既公开又审核的美食做法
        fetchGourmetData() {
            const queryDto = {
                current: this.currentPage,
                size: this.pageSize,
                key: this.filterText,
                ...this.gourtmetQueryDto
            };
            this.$axios.post('/gourmet/queryList', queryDto).then(res => {
                const { data } = res;
                if (data.code === 200) {
                    this.gourmetList = res.data.data;
                    this.totalItems = data.total;
                }
            }).catch(error => {
                console.log(error);
            });
        },

        // === Redis GEO 地理位置功能 ===
        getCurrentLocation() {
            this.geoLoading = true;

            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(
                    (position) => {
                        this.userLocation.latitude = position.coords.latitude;
                        this.userLocation.longitude = position.coords.longitude;
                        this.userLocation.address = '已定位';
                        this.geoLoading = false;
                        this.searchNearbyRestaurants();
                        this.$message.success('定位成功');
                    },
                    (error) => {
                        this.geoLoading = false;
                        // 使用默认位置（北京天安门）
                        this.userLocation.latitude = 39.915;
                        this.userLocation.longitude = 116.404;
                        this.userLocation.address = '北京市东城区';
                        this.searchNearbyRestaurants();
                        this.$message.warning('定位失败，使用默认位置');
                    }
                );
            } else {
                this.geoLoading = false;
                this.$message.error('浏览器不支持地理定位');
            }
        },

        async searchNearbyRestaurants() {
            try {
                // 搜索附近美食（基于用户发布的真实位置）
                const response = await this.$axios.get('/location-gourmet/nearby', {
                    params: {
                        longitude: this.userLocation.longitude,
                        latitude: this.userLocation.latitude,
                        radius: 5000,
                        limit: 5
                    }
                });

                if (response.data.success) {
                    this.nearbyRestaurants = response.data.data || [];
                    console.log('附近美食:', this.nearbyRestaurants);
                } else {
                    this.nearbyRestaurants = [];
                }
            } catch (error) {
                console.error('搜索附近美食失败:', error);
                this.nearbyRestaurants = [];
            }
        },

        refreshNearbyRestaurants() {
            if (this.userLocation.latitude) {
                this.searchNearbyRestaurants();
                this.$message.success('附近推荐已刷新');
            }
        },

        // 查看美食详情
        viewGourmetDetail(gourmet) {
            this.$router.push(`/gourmetDetail?id=${gourmet.id}`);
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
.gourmet-item {
        display: flex;
        justify-content: left;
        padding: 10px 0;
        cursor: pointer;

        .left {
            padding: 5px;
            box-sizing: border-box;

            img {
                width: 108px;
                height: 80px;
                border-radius: 5px;
            }
        }

        .right {
            padding: 5px;
            box-sizing: border-box;

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
                width: 200px;
                font-weight: 800;
                padding-bottom: 6px;
                white-space: nowrap;
                /* 防止文本换行 */
                overflow: hidden;
                /* 隐藏超出容器的文本 */
                text-overflow: ellipsis;
                /* 使用省略号表示被截断的文本 */
            }

            .title:hover {
                text-decoration: underline;
                text-decoration-style: dashed;
            }

            .detail {
                font-size: 14px;
                color: rgb(131, 130, 130);
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
.item-category {
    display: inline-block;
    padding: 8px 25px;
    border-radius: 15px;
    margin-right: 2px;
    font-size: 14px;
    cursor: pointer;
}

.item-gourmet:hover {
    background-color: rgb(248, 248, 248);
}

.item-gourmet {
    display: flex;
    justify-content: left;
    gap: 10px;
    padding: 10px;
    border-radius: 5px;
    cursor: pointer;

    .left {
        img {
            width: 190px;
            height: 130px;
            border-radius: 5px;
        }
    }

    .right {
        .info {
            padding: 4px 6px;
            display: flex;
            justify-content: left;
            align-items: center;
            gap: 5px;
            font-size: 14px;
            color: rgb(90, 89, 89);
        }

        .title {
            font-size: 20px;
            font-weight: 800;
        }

        .title:hover {
            text-decoration: underline;
            text-decoration-style: dashed;
        }

        .detail {
            font-size: 12px;
            padding: 4px 6px;
            color: rgb(90, 89, 89);
            display: flex;
            justify-content: left;
            gap: 10px;
        }
    }
}
.metrics-dialog {
    .metrics-content {
        padding: 20px;
    }

    .metrics-card {
        display: flex;
        justify-content: space-between;
        margin-bottom: 30px;
        
        .metric-item {
            flex: 1;
            text-align: center;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 8px;
            margin: 0 10px;
            
            .metric-title {
                color: #606266;
                font-size: 16px;
                margin-bottom: 10px;
            }
            
            .metric-value {
                color: #409EFF;
                font-size: 28px;
                font-weight: bold;
                margin-bottom: 10px;
            }
            
            .metric-desc {
                color: #909399;
                font-size: 12px;
                line-height: 1.4;
            }
        }
    }

    .metrics-stats {
        .stat-box {
            display: flex;
            align-items: center;
            padding: 20px;
            background: #fff;
            border: 1px solid #ebeef5;
            border-radius: 8px;
            
            i {
                font-size: 32px;
                color: #409EFF;
                margin-right: 15px;
            }
            
            .stat-info {
                .stat-value {
                    font-size: 24px;
                    color: #303133;
                    font-weight: bold;
                    margin-bottom: 5px;
                }
                
                .stat-label {
                    font-size: 14px;
                    color: #606266;
                }
            }
        }
    }

    .metrics-empty {
        padding: 40px 0;
    }
}

::v-deep .el-dialog__header {
    padding: 20px 30px;
    border-bottom: 1px solid #ebeef5;
    background: linear-gradient(135deg, #409EFF 0%, #36D1DC 100%);
    border-radius: 8px 8px 0 0;
    text-align: center;
    position: relative;  // 添加相对定位
    
    .el-dialog__title {
        font-size: 20px;
        font-weight: bold;
        color: #ffffff;
        text-shadow: 0 1px 2px rgba(0,0,0,0.1);
        letter-spacing: 1px;
        display: inline-block;
        padding: 0 20px;
    }

    .el-dialog__headerbtn {
        position: absolute;  // 改为绝对定位
        top: 50%;           // 垂直居中
        transform: translateY(-50%);  // 垂直居中调整
        right: 20px;
        width: 20px;       // 设置固定宽度
        height: 20px;      // 设置固定高度
        z-index: 1;        // 确保在最上层
        
        .el-dialog__close {
            color: #ffffff;
            font-weight: bold;
            font-size: 20px;
            opacity: 0.8;   // 添加透明度
            transition: opacity 0.3s;  // 添加过渡效果
            
            &:hover {
                opacity: 1;  // 悬停时不透明
            }
        }
    }
}

::v-deep .el-dialog__body {
    padding: 0;
}
.disabled-icon {
    color: #C0C4CC;
    cursor: not-allowed !important;
}

// Redis功能相关样式
.view-count-item {
    cursor: pointer;
    transition: color 0.3s ease;

    &:hover {
        color: #409EFF;
    }

    .uv-badge {
        margin-left: 8px;
        background: linear-gradient(45deg, #667eea, #764ba2);
        color: white;
        padding: 2px 6px;
        border-radius: 10px;
        font-size: 10px;
        font-weight: bold;
    }
}

.nearby-restaurants-card {
    .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: bold;
        color: #2c3e50;
    }

    .location-prompt {
        text-align: center;
        padding: 20px;
        color: #7f8c8d;

        i {
            font-size: 32px;
            margin-bottom: 10px;
            display: block;
        }
    }

    .location-info {
        margin-bottom: 15px;
        color: #7f8c8d;
    }

    .nearby-list {
        max-height: 200px;
        overflow-y: auto;
    }

    .nearby-item {
        display: flex;
        align-items: center;
        padding: 12px;
        margin: 8px 0;
        background: #f8f9fa;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.3s ease;

        &:hover {
            background: #e9ecef;
            transform: translateX(5px);
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }

        .gourmet-image {
            width: 50px;
            height: 50px;
            margin-right: 12px;
            border-radius: 6px;
            overflow: hidden;
            flex-shrink: 0;

            img {
                width: 100%;
                height: 100%;
                object-fit: cover;
            }
        }

        .gourmet-info {
            flex: 1;

            h4 {
                margin: 0 0 5px 0;
                color: #2c3e50;
                font-size: 14px;
                font-weight: 600;
            }

            p {
                margin: 2px 0;
                color: #7f8c8d;
                font-size: 12px;
            }
        }
    }

    .no-nearby {
        text-align: center;
        padding: 20px;
        color: #bdc3c7;
    }
}


</style>
