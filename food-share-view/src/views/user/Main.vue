<template>
    <div>
        <div class="top">
            <div class="top-left">
                <Logo sysName="橙子美食网" />
            </div>
            <div class="top-right">
                <ul>
                    <li @click="route('/gourmet')">
                        寻味
                    </li>
                    <li @click="route('/cityGourmet')">
                        同城美食
                    </li>
                    <li @click="route('/trending')">
                        热门排行榜
                    </li>
                    <li @click="route('/flashSale')">
                        限时秒杀
                    </li>
                    <li @click="route('/Save')">
                        我的收藏夹
                    </li>
                    <li>
                        <el-dropdown type="success" size="mini" :hide-on-click="false">
                            <span class="el-dropdown-link">
                                内容创作<i class="el-icon-arrow-down el-icon--right"></i>
                            </span>
                            <el-dropdown-menu slot="dropdown">
                                <el-dropdown-item @click.native="route('/createGourmet')">发布美食做法</el-dropdown-item>
                            </el-dropdown-menu>
                        </el-dropdown>
                    </li>
                </ul>
                <el-button type="primary" style="margin-right: 15px;" size="mini" @click="route('/service')"
                    round>内容中心</el-button>

                <!-- 消息通知 -->
                <el-popover
                    placement="bottom"
                    width="320"
                    trigger="click"
                    v-model="showNotifications">
                    <div class="notification-panel">
                        <div class="notification-header">
                            <span>消息通知</span>
                            <el-button
                                type="text"
                                size="mini"
                                @click="markAllAsRead"
                                v-if="unreadCount > 0">
                                全部已读
                            </el-button>
                        </div>

                        <div class="notification-list" v-if="notifications.length > 0">
                            <div
                                v-for="notification in notifications.slice(0, 5)"
                                :key="notification.id"
                                class="notification-item"
                                :class="{
                                    'unread': !notification.read,
                                    'broadcast': notification.isBroadcast,
                                    'flash-sale': notification.type === 'flash_sale_publish'
                                }"
                                @click="handleNotificationClick(notification)">

                                <!-- 通知图标 -->
                                <div class="notification-icon">
                                    <i v-if="notification.type === 'flash_sale_publish'" class="el-icon-lightning"></i>
                                    <i v-else-if="notification.isBroadcast" class="el-icon-bell"></i>
                                    <i v-else class="el-icon-message"></i>
                                </div>

                                <!-- 通知内容 -->
                                <div class="notification-content">
                                    <div class="notification-header">
                                        <div class="notification-title">{{ notification.title }}</div>
                                        <div class="notification-time">{{ notification.time }}</div>
                                    </div>
                                    <div class="notification-message">{{ notification.message }}</div>

                                    <!-- 秒杀通知的特殊标识 -->
                                    <div v-if="notification.type === 'flash_sale_publish'" class="flash-sale-tag">
                                        <span>点击查看详情</span>
                                    </div>
                                </div>

                                <!-- 未读标识 -->
                                <div v-if="!notification.read" class="unread-dot"></div>
                            </div>
                        </div>

                        <div v-else class="no-notifications">
                            <i class="el-icon-bell"></i>
                            <p>暂无新消息</p>
                        </div>

                        <div class="notification-footer" v-if="notifications.length > 5">
                            <el-button type="text" size="small">查看更多</el-button>
                        </div>
                    </div>

                    <el-badge
                        :value="unreadCount"
                        :hidden="unreadCount === 0"
                        slot="reference"
                        style="margin-right: 15px;">
                        <el-button
                            type="text"
                            size="medium"
                            class="notification-btn">
                            <i class="el-icon-bell" style="font-size: 18px;"></i>
                        </el-button>
                    </el-badge>
                </el-popover>

                <el-dropdown type="success" size="mini" class="user-dropdown">
                    <span class="el-dropdown-link" style="display: flex; align-items: center;cursor: pointer;">
                        <el-avatar
                            :size="30"
                            :src="userInfo.userAvatar"
                            @error="handleAvatarError"
                            style="margin-top: 0;">
                        </el-avatar>
                        <span class="user-name" style="margin-left: 5px;font-size: 14px;">{{ userInfo.userName }}</span>
                        <i class="el-icon-arrow-down el-icon--right" style="margin-left: 5px;"></i>
                    </span>
                    <el-dropdown-menu slot="dropdown">
                        <el-dropdown-item @click.native="route('/self')">个人资料</el-dropdown-item>
                        <el-dropdown-item @click.native="route('/resetPwd')">修改密码</el-dropdown-item>
                        <el-dropdown-item @click.native="loginOut">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </el-dropdown>
            </div>
        </div>
        <div class="router-view">
            <div class="item">
                <router-view></router-view>
            </div>
        </div>
    </div>
</template>
<script>
import Logo from "@/components/Logo"
export default {
    components: { Logo },
    name: "User",
    data() {
        return {
            key: '',
            defaultPath: '/gourmet',
            userInfo: {},
            defaultAvatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png', // 默认头像URL

            // 消息通知相关数据
            showNotifications: false,
            notifications: [],
            unreadCount: 0,
            notificationTimer: null
        }
    },
    created() {
        this.auth();
        // 默认加载景点页
        this.route(this.defaultPath);
        // 初始化消息通知
        this.initNotifications();
    },

    beforeDestroy() {
        // 清理定时器
        if (this.notificationTimer) {
            clearInterval(this.notificationTimer);
        }
    },
    methods: {
        // 路由跳转
        route(path) {
            if (this.$route.path !== path) {
                this.$router.push(path);
            };
        },
        // 退出登录
        async loginOut() {
            const confirmed = await this.$swalConfirm({
                title: '退出登录',
                text: `退出后需要重新登录哦？`,
                icon: 'warning',
            });
            if (confirmed) {
                sessionStorage.setItem('token', null);
                this.$router.push('/');
            }
        },
        // Token 检验
        async auth() {
            const { data } = await this.$axios.get('/user/auth');
            if (data.code !== 200) { // Token校验异常
                this.$router.push('/');
            } else {
                this.userInfo = data.data;
                // 存储用户信息
                sessionStorage.setItem('userInfo', JSON.stringify(this.userInfo));
            }
        },
        handleAvatarError() {
            // 当头像加载失败时，使用默认头像
            this.userInfo.userAvatar = this.defaultAvatar;
        },

        // === 消息通知功能 ===

        // 初始化消息通知
        async initNotifications() {
            await this.loadNotifications();
            // 启动定时轮询检查新消息
            this.startNotificationPolling();
        },

        // 加载用户通知
        async loadNotifications() {
            try {
                if (!this.userInfo.id) return;

                // 同时获取用户通知和广播通知
                const [userResponse, broadcastResponse] = await Promise.all([
                    this.$axios.get('/notifications/all', {
                        params: { page: 0, size: 20 }
                    }).catch(() => ({ data: { success: false, data: [] } })),
                    this.$axios.get('/notifications/broadcast', {
                        params: { page: 0, size: 10 }
                    }).catch(() => ({ data: { success: false, data: [] } }))
                ]);

                let allNotifications = [];

                // 处理用户通知
                if (userResponse.data.success) {
                    let userData = userResponse.data.data;
                    if (userData && typeof userData === 'object') {
                        let userNotifications = [];
                        if (userData.records) {
                            userNotifications = userData.records;
                        } else if (Array.isArray(userData)) {
                            userNotifications = userData;
                        }

                        // 转换用户通知格式
                        const formattedUserNotifications = userNotifications.map(notification => ({
                            id: notification.id || Date.now() + Math.random(),
                            title: notification.title || '用户通知',
                            message: notification.content || notification.message || '无内容',
                            time: this.formatNotificationTime(notification.createTime || notification.time),
                            read: notification.isRead !== undefined ? notification.isRead : (notification.read !== undefined ? notification.read : false),
                            isBroadcast: false,
                            type: notification.type,
                            createTime: notification.createTime
                        }));

                        allNotifications = [...allNotifications, ...formattedUserNotifications];
                    }
                }

                // 处理广播通知
                if (broadcastResponse.data.success) {
                    let broadcastData = broadcastResponse.data.data;
                    if (Array.isArray(broadcastData)) {
                        // 转换广播通知格式
                        const formattedBroadcastNotifications = broadcastData.map(notification => ({
                            id: notification.id || Date.now() + Math.random(),
                            title: notification.title || '系统通知',
                            message: notification.content || notification.message || '无内容',
                            time: this.formatNotificationTime(notification.createTime || notification.time),
                            read: notification.isRead !== undefined ? notification.isRead : (notification.read !== undefined ? notification.read : false),
                            isBroadcast: true,
                            type: notification.type,
                            createTime: notification.createTime
                        }));

                        allNotifications = [...allNotifications, ...formattedBroadcastNotifications];
                    }
                }

                // 按时间排序（最新的在前）
                allNotifications.sort((a, b) => {
                    const timeA = new Date(a.createTime || a.time);
                    const timeB = new Date(b.createTime || b.time);
                    return timeB - timeA;
                });

                this.notifications = allNotifications;
                this.updateUnreadCount();

                // 调试信息
                console.log('通知加载完成:', {
                    总数量: allNotifications.length,
                    未读数量: this.unreadCount,
                    通知列表: allNotifications
                });

            } catch (error) {
                console.error('加载通知失败:', error);
                // 降级处理：显示空通知列表
                this.notifications = [];
                this.unreadCount = 0;
            }
        },

        // 启动消息轮询
        startNotificationPolling() {
            // 每30秒检查一次新消息
            this.notificationTimer = setInterval(() => {
                this.loadNotifications();
            }, 30000);
        },

        // 处理通知点击事件
        async handleNotificationClick(notification) {
            // 先标记为已读
            await this.markAsRead(notification);

            // 关闭通知面板
            this.showNotifications = false;

            // 根据通知类型进行跳转
            if (notification.type === 'flash_sale_publish') {
                // 秒杀商品通知 - 跳转到秒杀页面
                await this.navigateToPage('/flashSale', '正在跳转到秒杀页面...');
            } else if (notification.contentId && notification.contentType === 'gourmet') {
                // 美食相关通知 - 跳转到美食详情
                await this.navigateToPage(`/gourmetDetail?id=${notification.contentId}`, '正在跳转到美食详情...');
            } else if (notification.contentId && notification.contentType === 'flash_sale') {
                // 秒杀商品详情通知
                await this.navigateToPage('/flashSale', '正在跳转到秒杀页面...');
            } else {
                // 其他通知 - 只标记已读
                this.$message.info('通知已查看');
            }
        },

        // 安全的页面导航方法
        async navigateToPage(targetPath, successMessage) {
            try {
                // 检查是否已经在目标页面
                const currentPath = this.$route.path;
                const targetBasePath = targetPath.includes('?') ? targetPath.split('?')[0] : targetPath;

                if (currentPath === targetBasePath) {
                    // 如果已经在目标页面，刷新页面内容
                    if (targetPath === '/flashSale') {
                        this.$message.success('正在刷新秒杀商品列表...');
                        // 触发页面刷新事件
                        this.$nextTick(() => {
                            window.location.reload();
                        });
                    } else {
                        this.$message.info('您已经在该页面了');
                    }
                    return;
                }

                // 执行导航
                await this.$router.push(targetPath);
                this.$message.success(successMessage);
            } catch (error) {
                // 捕获重复导航错误
                if (error.name === 'NavigationDuplicated') {
                    this.$message.info('您已经在该页面了');
                } else {
                    console.error('导航失败:', error);
                    this.$message.error('页面跳转失败');
                }
            }
        },

        // 标记单个消息为已读
        async markAsRead(notification) {
            if (notification.read) return;

            try {
                // 广播通知不需要标记已读（因为是系统消息）
                if (!notification.isBroadcast) {
                    await this.$axios.put('/notifications/read', {
                        params: { notificationId: notification.id }
                    });
                }
                notification.read = true;
                this.updateUnreadCount();
            } catch (error) {
                console.error('标记已读失败:', error);
                // 本地标记为已读
                notification.read = true;
                this.updateUnreadCount();
            }
        },

        // 标记所有消息为已读
        async markAllAsRead() {
            try {
                await this.$axios.put('/notifications/read/all');
                this.notifications.forEach(n => n.read = true);
                this.updateUnreadCount();
                this.$message.success('所有消息已标记为已读');
            } catch (error) {
                console.error('标记全部已读失败:', error);
                // 本地标记所有为已读
                this.notifications.forEach(n => n.read = true);
                this.updateUnreadCount();
            }
        },

        // 更新未读数量
        updateUnreadCount() {
            if (Array.isArray(this.notifications)) {
                this.unreadCount = this.notifications.filter(n => !n.read).length;
            } else {
                this.unreadCount = 0;
            }
        },

        // 添加新通知（供其他组件调用）
        addNotification(notification) {
            this.notifications.unshift({
                id: Date.now(),
                title: notification.title || '新消息',
                message: notification.message,
                time: '刚刚',
                read: false,
                ...notification
            });
            this.updateUnreadCount();

            // 显示桌面通知
            this.showDesktopNotification(notification);
        },

        // 显示桌面通知
        showDesktopNotification(notification) {
            if (Notification.permission === 'granted') {
                new Notification(notification.title || '橙子美食网', {
                    body: notification.message,
                    icon: '/favicon.ico'
                });
            }
        },

        // 格式化通知时间
        formatNotificationTime(timeStr) {
            if (!timeStr) return '刚刚';

            try {
                const time = new Date(timeStr);
                const now = new Date();
                const diff = now - time;

                if (diff < 60000) return '刚刚';
                if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
                if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
                if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`;

                // 超过一周显示具体日期
                return time.toLocaleDateString();
            } catch (error) {
                console.error('时间格式化失败:', error);
                return '刚刚';
            }
        }
    },
    watch: {
        // 监听路由变化
        '$route'(to, from) {
            // 使用 nextTick 确保 DOM 更新后再滚动
            this.$nextTick(() => {
                // 设置一个短暂的延时，等待内容加载
                setTimeout(() => {
                    window.scrollTo({
                        top: 0,
                        behavior: 'smooth'
                    });
                }, 100); // 100ms 的延时，可以根据实际情况调整
            });
        }
    }
};
</script>
<style scoped lang="scss">
.top {
    height: 65px;
    position: sticky;
    top: 0;
    background-color: rgb(255,255,255);
    z-index: 2000;
    line-height: 65px;
    padding: 4px 100px;
    box-sizing: border-box;
    display: flex;
    justify-content: space-between;
    border-bottom: 1px solid rgb(231, 231, 231);

    .top-right {
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 6px;

        ul {
            list-style: none;

            li {
                cursor: pointer;
                float: left;
                margin-right: 6px;
                padding: 5px 10px;

                min-width: 50px;
                max-width: 100px;
                color: rgb(111, 53, 71);
                font-size: 14px;
                box-sizing: border-box;
                font-weight: 500;
            }

            li:hover {
                color: rgb(25, 70, 160);
            }

            .redis-features-nav {
                background: linear-gradient(45deg, #667eea 0%, #764ba2 100%);
                color: white !important;
                border-radius: 20px;
                padding: 8px 15px !important;
                font-weight: bold;
                transition: all 0.3s ease;
                display: flex;
                align-items: center;
                gap: 5px;
            }

            .redis-features-nav:hover {
                background: linear-gradient(45deg, #764ba2 0%, #667eea 100%);
                color: white !important;
                transform: translateY(-2px);
                box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
            }

            /* 消息通知样式 */
            .notification-btn {
                color: #606266;
                transition: color 0.3s ease;
            }

            .notification-btn:hover {
                color: #409EFF;
            }

            .notification-panel {
                padding: 0;
                border-radius: 8px;
                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
                overflow: hidden;
            }

            .notification-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 16px 20px;
                background: linear-gradient(45deg, #409EFF, #64B5F6);
                color: white;
                font-weight: bold;
                font-size: 15px;
            }

            .notification-header .el-button--text {
                color: white;
                font-weight: 500;
            }

            .notification-header .el-button--text:hover {
                background-color: rgba(255, 255, 255, 0.1);
            }

            .notification-list {
                max-height: 400px;
                overflow-y: auto;
                background: white;
            }

            .notification-list::-webkit-scrollbar {
                width: 4px;
            }

            .notification-list::-webkit-scrollbar-track {
                background: #f1f1f1;
            }

            .notification-list::-webkit-scrollbar-thumb {
                background: #c1c1c1;
                border-radius: 2px;
            }

            .notification-list::-webkit-scrollbar-thumb:hover {
                background: #a8a8a8;
            }

            .notification-item {
                display: flex;
                align-items: flex-start;
                padding: 16px;
                border-bottom: 1px solid #f5f7fa;
                cursor: pointer;
                transition: all 0.3s ease;
                position: relative;
                gap: 12px;
            }

            .notification-item:hover {
                background-color: #f8f9fa;
                transform: translateX(2px);
            }

            .notification-item.unread {
                background-color: #ecf5ff;
                border-left: 4px solid #409EFF;
            }

            .notification-item.flash-sale {
                border-left: 4px solid #F56C6C;
            }

            .notification-item.flash-sale:hover {
                background-color: #fef0f0;
            }

            .notification-item.broadcast {
                border-left: 4px solid #67C23A;
            }

            .notification-icon {
                flex-shrink: 0;
                width: 40px;
                height: 40px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 18px;
                margin-top: 2px;
            }

            .notification-item.flash-sale .notification-icon {
                background: linear-gradient(45deg, #F56C6C, #FF8A80);
                color: white;
            }

            .notification-item.broadcast .notification-icon {
                background: linear-gradient(45deg, #67C23A, #81C784);
                color: white;
            }

            .notification-item:not(.flash-sale):not(.broadcast) .notification-icon {
                background: linear-gradient(45deg, #409EFF, #64B5F6);
                color: white;
            }

            .notification-content {
                flex: 1;
                min-width: 0;
            }

            .notification-header {
                display: flex;
                justify-content: space-between;
                align-items: flex-start;
                margin-bottom: 6px;
                gap: 8px;
            }

            .notification-title {
                font-weight: bold;
                color: #303133;
                font-size: 14px;
                line-height: 1.3;
                flex: 1;
                min-width: 0;
                word-break: break-word;
            }

            .notification-time {
                color: #c0c4cc;
                font-size: 11px;
                white-space: nowrap;
                flex-shrink: 0;
            }

            .notification-message {
                color: #606266;
                font-size: 13px;
                line-height: 1.4;
                margin-bottom: 8px;
                word-break: break-word;
            }

            .flash-sale-tag {
                display: inline-flex;
                align-items: center;
                padding: 2px 8px;
                background: linear-gradient(45deg, #F56C6C, #FF8A80);
                color: white;
                border-radius: 12px;
                font-size: 11px;
                font-weight: 500;
            }

            .flash-sale-tag span {
                animation: pulse 2s infinite;
            }

            @keyframes pulse {
                0%, 100% { opacity: 1; }
                50% { opacity: 0.7; }
            }

            .unread-dot {
                position: absolute;
                top: 12px;
                right: 12px;
                width: 10px;
                height: 10px;
                background: #F56C6C;
                border-radius: 50%;
                border: 2px solid white;
                box-shadow: 0 2px 4px rgba(245, 108, 108, 0.3);
                animation: unread-pulse 2s infinite;
            }

            @keyframes unread-pulse {
                0%, 100% {
                    transform: scale(1);
                    box-shadow: 0 2px 4px rgba(245, 108, 108, 0.3);
                }
                50% {
                    transform: scale(1.1);
                    box-shadow: 0 2px 8px rgba(245, 108, 108, 0.5);
                }
            }

            .no-notifications {
                text-align: center;
                padding: 60px 20px;
                color: #c0c4cc;
                background: white;
            }

            .no-notifications i {
                font-size: 48px;
                margin-bottom: 16px;
                display: block;
                color: #e6e6e6;
            }

            .no-notifications p {
                margin: 8px 0;
                font-size: 14px;
                color: #909399;
            }

            .notification-footer {
                text-align: center;
                padding: 16px;
                border-top: 1px solid #ebeef5;
                background: #fafafa;
            }

            .notification-footer .el-button--text {
                color: #409EFF;
                font-weight: 500;
            }
        }
    }

    .top-left {
        display: flex;
        justify-content: center;
        align-items: center;
    }
}

.router-view {
    padding: 4px 30px;
    box-sizing: border-box;
    //background-color: rgb(248,248,248);
    min-height: calc(100vh - 66px);

    .item {
        width: 93%;
        margin: 0 auto;
    }
}
</style>
