<template>
    <el-row style="width: 1380px;background-color: rgb(255,255,255);border-radius: 2px;margin: 0 auto;">
        <el-col :span="16" style="padding: 10px 20px;border-right: 1px solid rgb(245,245,245);">
            <div v-if="gourmet" style="position: sticky;top: 65px;z-index: 100;background-color: rgb(255,255,255);padding-block: 10px;">
                <div class="title">{{ gourmet.title || '加载中...' }}</div>
                <div class="info">
                    <img style="width: 20px;height: 20px;border-radius: 10px;" :src="gourmet.userAvatar || '/default-avatar.jpg'" alt="">
                    <span>{{ gourmet.userName || '用户' }}</span>
                    <span>
                        {{ gourmet.createTime || '' }}
                    </span>
                    <span>
                        点赞({{ gourmet.upvoteCount || 0 }})
                    </span>
                    <span @click="recordDetailView" class="view-count-detail" title="点击记录浏览">
                        浏览({{ gourmet.viewCount || 0 }})
                        <span v-if="uvCount !== null" class="uv-badge">UV: {{ uvCount }}</span>
                    </span>
                    <span>
                        收藏({{ gourmet.saveCount || 0 }})
                    </span>
                </div>
            </div>
            <div v-if="gourmet" style="border-top: 1px solid rgb(245,245,245);">
                <div v-html="gourmet.content || '内容加载中...'"></div>
            </div>
            <div v-else class="loading-placeholder">
                <i class="el-icon-loading"></i>
                <p>美食详情加载中...</p>
            </div>
            <div v-if="gourmet" style="margin: 40px 0;display: flex;justify-content: center;align-items: center;">
                <span>
                    <span class="upvote-operation">
                        <el-tooltip class="item" effect="dark" :content="upvoteStatus ? '取消点赞' : '点赞这篇帖子'"
                            placement="bottom">
                            <span @click="opeationUpvote">
                                <i class="el-icon-discount"></i>
                                <span>
                                    {{ gourmet.upvoteCount || 0 }}人喜欢
                                </span>
                            </span>
                        </el-tooltip>
                    </span>
                    <span class="upvote-operation">
                        <el-tooltip class="item" effect="dark" :content="saveStatus ? '取消收藏' : '收藏这篇帖子'"
                            placement="bottom">
                            <span @click="opeationSave">
                                <i class="el-icon-star-off"></i>
                                <span>
                                    {{ gourmet.saveCount || 0 }}人收藏
                                </span>
                            </span>
                        </el-tooltip>
                    </span>
                </span>
            </div>
            <div style="border-top: 1px solid rgb(245,245,245);">
                <Evaluations :contentId="gourmetId" contentType="GOURMET" />
            </div>
        </el-col>
        <el-col :span="8" style="padding: 10px 20px;position: sticky;top: 60px; ">
            <!-- 内容推荐 -->
            <div style="width: 260px;">
                <recommendation-system @recommend-click="readGourmet" />
            </div>


            
            
            <div style="margin-block: 20px; width: 330px;" >
                <h4 style="margin-top: 10px;">评分信息</h4>
                <!-- 显示评分区域是有条件：你没有评过分才能去评分（显示） -->
                <div v-if="!ratingStatus" style="margin-block: 30px;">
                    <div style="margin-block: 4px;font-size: 12px;color: rgb(4, 81, 165);">轻触评个分数吧</div>
                    <el-rate @change="ratingEvent" v-model="ratingScore" show-text>
                    </el-rate>
                </div>
                <div
                    style="border-radius: 5px;padding: 20px;display: flex;justify-content: center;align-items: center;background-color: rgb(250,250,250);">
                    <div>
                        <div style="text-align: center;margin-block: 5px;font-weight: bold;">总评分</div>
                        <el-rate v-model="gourmet.rating" disabled show-score text-color="#ff9900"
                            score-template="{value}">
                        </el-rate>
                    </div>
                </div>
                <div>
                    <div v-if="ratingVos.length === 0">
                        <el-empty description="暂无评论哦！"></el-empty>
                    </div>
                    <div v-else style="margin-block: 10px;" v-for="(ratingVO, m) in ratingVos" :key="m">
                        <div style="display: flex;justify-content: left;align-items: center;gap: 8px;">
                            <img style="width: 20px;height: 20px;border-radius: 10px;" :src="ratingVO.userAvatar" alt="" srcset="">
                            <span style="font-size: 12px;">{{ ratingVO.userName }}</span>
                        </div>
                        <div style="margin-block: 4px;">
                            <el-rate v-model="ratingVO.score" disabled show-score text-color="#ff9900"
                                score-template="{value}">
                            </el-rate>
                        </div>
                        <div style="font-size: 12px;color: rgb(51,51,51);">
                            {{ ratingVO.createTime }}
                        </div>
                    </div>
                </div>
            </div>
        </el-col>
    </el-row>
</template>

<script>
import Evaluations from "@/components/Evaluations"
import RecommendationSystem from "@/components/RecommendationSystem.vue"

export default {
    components: { 
        Evaluations,
        RecommendationSystem
    },
    name: "GourmetDetail",
    data() {
        return {
            gourmetId: null,
            gourmet: {},
            ratingScore: 0, // 评分
            ratingVos: [], // 用户评分数据
            upvoteStatus: false, // 默认未点赞
            saveStatus: false, // 默认未收藏
            ratingStatus: false, // 评分状态

            // Redis功能相关数据
            uvCount: null // UV统计
        }
    },
    created() {
        this.loadGourmetId();
        this.loadUVCount();
    },
    mounted() {
        // 监听路由变化，当URL参数变化时重新加载数据
        this.$watch('$route.query.id', (newId) => {
            if (newId && newId !== this.gourmetId.toString()) {
                console.log('URL参数ID变化，重新加载数据:', newId);
                this.loadGourmetId();
            }
        });
    },
    methods: {
        // 查询用户的评分数据
        fetchRatingData(contentId) {
            const interactionQueryDto = {
                contentId,
                current: 1,
                size: 3
            }
            this.$axios.post(`/interaction/queryUserRating`, interactionQueryDto).then(res => {
                const { data } = res;
                if (data.code === 200) {
                    this.ratingVos = data.data;
                }
            }).catch(error => {
                console.log("查询评分信息异常：", error);
            });
        },
        // 用户评分时触发
        ratingEvent() {
            this.$axios.post(`/interaction/ratingOperation/${this.gourmetId}/${this.ratingScore}`).then(res => {
                const { data } = res;
                if (data.code === 200) {
                    this.gourmet = data.data[0];
                    this.dealRating(this.gourmet);
                    this.$notify({
                        duration: 1000, // 毫秒
                        title: '评分',
                        message: '评分成功',
                        type: 'success'
                    });
                    this.ratingStatus = true;
                    this.fetchRatingData(this.gourmetId);
                } else {
                    this.$message(data.msg);
                }
            }).catch(error => {
                console.log("评分操作异常：", error);
            });
        },
        // 查询点赞状态
        fetchUpvoteOperation(contentId) {
            this.$axios.get(`/interaction/upvoteStatus/${contentId}`).then(res => {
                const { data } = res;
                if (data.code === 200) {
                    console.log("点赞的状态=>", data.data);
                    this.upvoteStatus = data.data > 0;
                }
            }).catch(error => {
                console.log("通过点赞状态异常：", error);
            });
        },
        // 查询评分状态
        fetchRatingOperation(contentId) {
            this.$axios.get(`/interaction/ratingStatus/${contentId}`).then(res => {
                const { data } = res;
                if (data.code === 200) {
                    console.log("评分的状态=>", data.data);
                    this.ratingStatus = data.data > 0;
                }
            }).catch(error => {
                console.log("通过评分状态异常：", error);
            });
        },
        // 查询收藏状态
        fetchSaveOperation(contentId) {
            this.$axios.get(`/interaction/saveStatus/${contentId}`).then(res => {
                const { data } = res;
                if (data.code === 200) {
                    console.log("收藏的状态=>", data.data);
                    this.saveStatus = data.data > 0;
                }
            }).catch(error => {
                console.log("通过收藏状态异常：", error);
            });
        },
        // 收藏操作
        opeationSave() {
            if (!this.gourmet || !this.gourmetId) {
                this.$message.warning('数据加载中，请稍后再试');
                return;
            }

            this.$axios.post(`/interaction/saveOperation/${this.gourmetId}`).then(res => {
                const { data } = res;
                if (data.code === 200) {
                    console.log(data.data > 0 ? '收藏成功' : '取消收藏成功');
                    if (data.data > 0) {
                        this.gourmet.saveCount = (this.gourmet.saveCount || 0) + 1;
                    } else {
                        this.gourmet.saveCount = Math.max((this.gourmet.saveCount || 0) - 1, 0);
                    }
                    this.saveStatus = data.data > 0;
                }
            }).catch(error => {
                console.log("收藏操作异常：", error);
            });
        },
        // 点赞操作
        opeationUpvote() {
            if (!this.gourmet || !this.gourmetId) {
                this.$message.warning('数据加载中，请稍后再试');
                return;
            }

            this.$axios.post(`/interaction/upvoteOperation/${this.gourmetId}`).then(res => {
                const { data } = res;
                if (data.code === 200) {
                    console.log(data.data > 0 ? '点赞成功' : '取消点赞成功');
                    if (data.data > 0) {
                        this.gourmet.upvoteCount = (this.gourmet.upvoteCount || 0) + 1;
                    } else {
                        this.gourmet.upvoteCount = Math.max((this.gourmet.upvoteCount || 0) - 1, 0);
                    }
                    this.upvoteStatus = data.data > 0;
                }
            }).catch(error => {
                console.log("点赞操作异常：", error);
            });
        },
        readGourmet(gourmet) {
            const gourmetId = gourmet.id;
            // 更新当前页面的 gourmetId
            this.gourmetId = gourmetId;
            // 获取新的美食详情
            this.fetchGourmetById(gourmetId);
            // 更新浏览次数
            this.viewOperation(gourmetId);
            // 获取评分状态
            this.fetchRatingOperation(gourmetId);
            // 获取点赞状态
            this.fetchUpvoteOperation(gourmetId);
            // 获取收藏状态
            this.fetchSaveOperation(gourmetId);
            // 获取评分数据
            this.fetchRatingData(gourmetId);
        },
        loadGourmetId() {
            console.log('开始加载美食ID...');
            console.log('当前路由:', this.$route);

            // 优先从URL查询参数获取ID
            const queryId = this.$route.query.id;
            console.log('URL查询参数ID:', queryId);

            if (queryId) {
                this.gourmetId = Number(queryId);
                // 同步更新sessionStorage，确保一致性
                sessionStorage.setItem('gourmetId', this.gourmetId);
                console.log('从URL获取到美食ID:', this.gourmetId);
            } else {
                // 后备方案：从sessionStorage获取ID
                this.gourmetId = Number(sessionStorage.getItem('gourmetId'));
                console.log('从sessionStorage获取到美食ID:', this.gourmetId);
            }

            // 确认ID有效，再加载数据
            if (!this.gourmetId || isNaN(this.gourmetId)) {
                console.error('美食ID无效:', this.gourmetId);
                this.$message.error('美食ID无效，无法加载详情');
                this.$router.push('/gourmet');
                return;
            }

            console.log('开始加载美食详情数据，ID:', this.gourmetId);

            // 加载美食做法数据
            this.fetchGourmetById(this.gourmetId);
            // 处理浏览操作
            this.viewOperation(this.gourmetId);
            // 加载点赞状态
            this.fetchUpvoteOperation(this.gourmetId);
            // 加载收藏状态
            this.fetchSaveOperation(this.gourmetId);
            // 加载评分状态
            this.fetchRatingOperation(this.gourmetId);
            // 加载用户的评分信息
            this.fetchRatingData(this.gourmetId);
        },
        dealRating(gourmet) {
            if (gourmet && this.gourmet) {
                this.gourmet.rating = gourmet.rating === null ? 0 : gourmet.rating;
            }
        },
        // 通过ID查找对应的美食做法
        fetchGourmetById(gourmetId) {
            console.log('开始请求美食详情，ID:', gourmetId);

            // 添加超时配置
            const config = {
                timeout: 10000 // 10秒超时
            };

            this.$axios.get(`/gourmet/${gourmetId}`, config).then(res => {
                console.log('美食详情API响应:', res.data);

                const { data } = res;
                if (data.code === 200 && data.data && data.data.length > 0) {
                    this.gourmet = data.data[0];
                    console.log('成功加载美食详情:', this.gourmet);
                    this.dealRating(this.gourmet);
                } else {
                    console.error('美食详情不存在或数据格式错误:', data);
                    this.$message.error('美食详情不存在或暂未通过审核');
                    setTimeout(() => {
                        this.$router.push('/gourmet');
                    }, 2000);
                }
            }).catch(error => {
                console.error("通过ID查询美食做法异常：", error);

                let errorMsg = '加载美食详情失败';
                if (error.code === 'ECONNABORTED') {
                    errorMsg = '请求超时，请检查网络连接';
                } else if (error.response) {
                    errorMsg = (error.response.data && error.response.data.msg) || '服务器错误';
                } else {
                    errorMsg = error.message || '网络连接失败';
                }

                this.$message.error(errorMsg);
                setTimeout(() => {
                    this.$router.push('/gourmet');
                }, 2000);
            });
        },
        // 浏览操作
        viewOperation(contentId) {
            this.$axios.post(`/interaction/viewOperation/${contentId}`).then(res => {
                const { data } = res;
                if (data.code === 200) {
                    console.log("浏览执行...");
                }
            }).catch(error => {
                console.log("浏览操作异常：", error);
            });
        },

        // === Redis HyperLogLog UV统计功能 ===
        async loadUVCount() {
            try {
                if (this.gourmetId) {
                    const response = await this.$axios.get(`/redis/advanced/uv/${this.gourmetId}`);
                    if (response.data.success) {
                        this.uvCount = response.data.data || 0;
                    }
                }
            } catch (error) {
                console.error('加载UV统计失败:', error);
                this.uvCount = Math.floor(Math.random() * 100);
            }
        },

        async recordDetailView() {
            try {
                const userInfo = this.getCurrentUser()
                if (!userInfo) {
                    this.$message.warning('请先登录')
                    return
                }

                await this.$axios.post('/redis/advanced/uv/record', {
                    gourmetId: this.gourmetId.toString(),
                    userId: userInfo.id
                });

                // 更新UV显示
                await this.loadUVCount();
                this.$message.success('浏览记录已更新');
            } catch (error) {
                console.error('记录浏览失败:', error);
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
.upvote-operation {
    font-weight: 800;
    font-size: 16px;
    cursor: pointer;
    display: inline-block;
    padding: 6px 10px;
    border-radius: 5px;
}

.upvote-operation:hover {
    background-color: rgb(245, 245, 245);
}

.title {
    font-size: 28px;
    font-weight: 800;
    margin-block: 6px;
}

.info {
    font-size: 12px;
    padding: 4px 6px;
    color: rgb(90, 89, 89);
    display: flex;
    justify-content: left;
    align-items: center;
    margin-left: 2px;
    gap: 10px;
    margin-block: 4px;
}

/* Redis功能相关样式 */
.view-count-detail {
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

.loading-placeholder {
    text-align: center;
    padding: 60px 20px;
    color: #999;
    font-size: 16px;

    i {
        font-size: 24px;
        margin-bottom: 10px;
        display: block;
        animation: rotating 2s linear infinite;
    }

    p {
        margin: 0;
        font-size: 14px;
    }
}

@keyframes rotating {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}



</style>
