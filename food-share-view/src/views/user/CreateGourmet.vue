<template>
    <div class="container">
        <div class="top">
            <el-tooltip class="item" effect="dark" content="返回首页" placement="bottom">
                <span class="last-page" @click="goBack">
                    <i class="el-icon-back"></i>
                </span>
            </el-tooltip>
            <span class="text">发布美食做法</span>
        </div>
        <div class="buttom">
            <div>
                <el-col :span="4" style="padding-left: 10px;border-right: 1px solid rgb(231, 231, 231);min-height: calc(100vh - 80px);">
                    <div style="padding-inline: 20px;">
                        <p style="font-size: 32px;">系列</p>
                        <el-select size="small" style="font-size: 20px;" v-model="gourmet.categoryId" placeholder="菜系">
                            <el-option style="font-size: 16px;" v-for="item in categories" :key="item.id" :label="item.name" :value="item.id">
                            </el-option>
                        </el-select>
                    </div>
                    <div style="padding-inline: 20px;">
                        <p style="font-size: 32px;">封面</p>
                        <el-upload class="avatar-uploader"
                            action="http://localhost:21090/api/food-share-sys/v1.0/file/upload" :show-file-list="false"
                            :on-success="handleCoverSuccess">
                            <img v-if="cover" :src="cover" style="width: 180px;height: 130px;">
                            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
                        </el-upload>
                    </div>
                    <div style="padding-inline: 20px;">
                        <p style="font-size: 32px;">公开权限</p>
                        <el-switch v-model="gourmet.isPublish" active-color="#13ce66" inactive-color="#3c3f41">
                        </el-switch>
                    </div>

                    <!-- 位置信息采集 -->
                    <div style="padding-inline: 20px; margin-top: 20px;">
                        <p style="font-size: 32px;">位置信息</p>
                        <div class="location-section">
                            <el-switch
                                v-model="enableLocation"
                                active-text="分享位置"
                                inactive-text="不分享位置"
                                active-color="#13ce66"
                                inactive-color="#3c3f41"
                                @change="handleLocationToggle">
                            </el-switch>

                            <div v-if="enableLocation" class="location-info">
                                <div class="location-display">
                                    <i class="el-icon-location-outline"></i>
                                    <span v-if="locationInfo.name">{{ locationInfo.name }}</span>
                                    <span v-else class="no-location">未获取位置</span>
                                </div>

                                <el-button
                                    size="mini"
                                    type="primary"
                                    @click="getCurrentLocation"
                                    :loading="locationLoading">
                                    <i class="el-icon-location"></i> 获取当前位置
                                </el-button>

                                <div v-if="locationInfo.name" class="location-tip">
                                    <i class="el-icon-info"></i>
                                    <span>分享位置后，其他用户可以在同城美食中发现您的作品</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </el-col>
                <el-col :span="20">
                    <div style="padding-block: 20px;border-bottom: 1px solid rgb(234, 232, 232);">
                        <input v-model="gourmet.title" style="width: 80%;font-size: 40px;padding: 8px 30px;" type="text"
                            class="input-title" placeholder="请输入标题">
                    </div>
                    <div style="border-bottom: 1px solid rgb(234, 232, 232);">
                        <Editor height="calc(100vh - 300px)" :receiveContent="gourmet.content"
                            @on-receive="onReceive" />
                    </div>
                </el-col>
            </div>
            <div style="text-align: center;">
                <el-button @click="postGourmet" style="margin: 20px 0;" plain>发布做法</el-button>
            </div>
        </div>
    </div>
</template>
<script>
import Editor from "@/components/Editor"
export default {
    components: { Editor },
    name: "CreateGourmet",
    data() {
        return {
            gourmet: {},
            categories: [], // 美食做法的数据集
            cover: null,

            // 位置相关数据
            enableLocation: false,
            locationLoading: false,
            locationInfo: {
                longitude: null,
                latitude: null,
                name: ''
            }
        }
    },
    created() {
        this.fetchFreshCategories();
    },
    methods: {
        // 发布美食
        postGourmet() {
            // 基本信息
            this.gourmet.cover = this.cover;

            // 位置信息
            if (this.enableLocation && this.locationInfo.longitude && this.locationInfo.latitude) {
                this.gourmet.longitude = this.locationInfo.longitude;
                this.gourmet.latitude = this.locationInfo.latitude;
                this.gourmet.locationName = this.locationInfo.name;
            }

            this.$axios.post('/gourmet/save', this.gourmet).then(res => {
                console.log('美食保存响应:', res.data);

                if (res.data.code === 200) {
                    const gourmetId = res.data.data; // 获取新创建的美食ID
                    console.log('新创建的美食ID:', gourmetId);

                    this.$notify({
                        duration: 1000,
                        title: '美食做法新增',
                        message: '新增成功',
                        type: 'success'
                    });

                    // 如果包含位置信息，同时添加到Redis GEO
                    if (this.enableLocation && this.locationInfo.longitude && this.locationInfo.latitude && gourmetId) {
                        this.addToRedisGeo(gourmetId);
                    }

                    this.goBack();
                } else {
                    console.error('美食保存失败:', res.data);
                    this.$message.error('保存失败: ' + (res.data.msg || '未知错误'));
                }
            }).catch(error => {
                console.error("新增美食做法异常：", error);
                this.$message.error('保存失败，请检查网络连接或稍后重试');
            });
        },
        // 封面上传回调函数
        handleCoverSuccess(res, file) {
            this.$notify({
                duration: 1500,
                title: '封面上传',
                message: res.code === 200 ? '上传成功' : '上传失败',
                type: res.code === 200 ? 'success' : 'error'
            });
            // 上传成功则更新用户头像
            if (res.code === 200) {
                this.cover = res.data;
            }
        },
        // 查询美食做法的信息
        fetchFreshCategories() {
            this.$axios.post('/category/query', {}).then(res => {
                if (res.data.code === 200) {
                    this.categories = res.data.data;
                    const allCategory = { id: null, name: '全部' };
                    // 头插
                    this.categories.unshift(allCategory);
                }
            }).catch(error => {
                console.log("查询美食做法异常：", error);
            });
        },
        // 返回上一页
        goBack() {
            this.$router.go(-1);
        },
        onReceive(content) {
            this.gourmet.content = content;
        },

        // === 位置相关方法 ===

        // 位置开关切换
        handleLocationToggle(enabled) {
            if (enabled) {
                this.getCurrentLocation();
            } else {
                this.locationInfo = {
                    longitude: null,
                    latitude: null,
                    name: ''
                };
            }
        },

        // 获取当前位置
        async getCurrentLocation() {
            if (!navigator.geolocation) {
                this.$message.error('您的浏览器不支持地理位置获取');
                this.enableLocation = false;
                return;
            }

            this.locationLoading = true;

            try {
                const position = await new Promise((resolve, reject) => {
                    navigator.geolocation.getCurrentPosition(resolve, reject, {
                        enableHighAccuracy: true,
                        timeout: 10000,
                        maximumAge: 300000 // 5分钟缓存
                    });
                });

                this.locationInfo.longitude = position.coords.longitude;
                this.locationInfo.latitude = position.coords.latitude;

                // 获取地址描述
                await this.getLocationName(position.coords.latitude, position.coords.longitude);

                this.$message.success('位置获取成功');

            } catch (error) {
                console.error('获取位置失败:', error);
                this.$message.warning('位置获取失败，您可以选择不分享位置');
                this.enableLocation = false;
            } finally {
                this.locationLoading = false;
            }
        },

        // 获取位置名称（逆地理编码）
        async getLocationName(latitude, longitude) {
            try {
                // 这里可以调用地图API获取地址，暂时使用坐标显示
                this.locationInfo.name = `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;

                // 如果有高德地图或百度地图API，可以这样调用：
                // const response = await this.$axios.get(`/api/geocoding/reverse?lat=${latitude}&lng=${longitude}`);
                // this.locationInfo.name = response.data.address;

            } catch (error) {
                console.error('获取地址失败:', error);
                this.locationInfo.name = `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`;
            }
        },

        // 添加到Redis GEO
        async addToRedisGeo(gourmetId) {
            try {
                const response = await this.$axios.post('/location-gourmet/add-location', {
                    gourmetId: gourmetId,
                    longitude: this.locationInfo.longitude,
                    latitude: this.locationInfo.latitude,
                    locationName: this.locationInfo.name
                });

                if (response.data.success) {
                    console.log('美食位置信息已添加到Redis GEO');
                    this.$message.success('位置信息已保存，您的美食将出现在同城推荐中！');
                } else {
                    console.warn('添加位置信息到Redis失败:', response.data.msg);
                }
            } catch (error) {
                console.error('添加位置信息到Redis失败:', error);
                // 不影响主流程，只记录错误
            }
        }
    }
};
</script>
<style scoped lang="scss">
.container {
    min-height: 100vh;

    .buttom {
        margin-top: 6px;
    }

    .top {
        line-height: 64px;
        padding-inline: 40px;
        background-color: rgb(255, 255, 255);
        border-bottom: 1px solid rgb(231, 231, 231);

        .text {
            font-size: 18px;
            font-weight: 900;
            margin-left: 10px;
        }

        .last-page:hover {
            background-color: rgb(232, 232, 232);
        }

        .last-page {
            background-color: rgb(245, 245, 245);
            padding: 2px 4px;
            border-radius: 12px;
            border: 1px solid rgb(235, 235, 235);
            cursor: pointer;
        }
    }

    /* 位置信息样式 */
    .location-section {
        margin-top: 15px;
    }

    .location-info {
        margin-top: 15px;
        padding: 15px;
        background: #f8f9fa;
        border-radius: 8px;
        border: 1px solid #e9ecef;
    }

    .location-display {
        display: flex;
        align-items: center;
        margin-bottom: 10px;
        font-size: 14px;
        color: #333;
    }

    .location-display i {
        margin-right: 8px;
        color: #409EFF;
        font-size: 16px;
    }

    .no-location {
        color: #999;
        font-style: italic;
    }

    .location-tip {
        margin-top: 10px;
        padding: 8px 12px;
        background: #e7f3ff;
        border-radius: 4px;
        font-size: 12px;
        color: #666;
        display: flex;
        align-items: flex-start;
    }

    .location-tip i {
        margin-right: 6px;
        color: #409EFF;
        margin-top: 1px;
        flex-shrink: 0;
    }
}
</style>
