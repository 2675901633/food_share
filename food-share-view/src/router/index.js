import Vue from "vue";
import VueRouter from "vue-router";
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import { getToken } from "@/utils/storage.js";
import echarts from 'echarts';
Vue.prototype.$echarts = echarts;
Vue.use(ElementUI);
Vue.use(VueRouter);

const routes = [
  {
    path: "*",
    redirect: "/login"
  },
  {
    path: "/login",
    component: () => import(`@/views/login/Login.vue`)
  },
  {
    path: "/register",
    component: () => import(`@/views/register/Register.vue`)
  },
  {
    path: "/createGourmet",
    component: () => import(`@/views/user/CreateGourmet.vue`)
  },
  {
    path: "/shareDetail",
    component: () => import(`@/views/user/ShareDetail.vue`)
  },
  {
    path: "/editGourmet",
    component: () => import(`@/views/user/EditGourmet.vue`)
  },
  {
    path: "/seeGourmetDetail",
    component: () => import(`@/views/admin/SeeGourmetDetail.vue`)
  },
  {
    path: "/admin",
    component: () => import(`@/views/admin/Home.vue`),
    meta: {
      requireAuth: true,
    },
    children: [
      {
        path: "/userManage",
        name: '用户管理',
        icon: 'el-icon-user',
        component: () => import(`@/views/admin/UserManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/categoryManage",
        name: '美食类别管理',
        icon: 'el-icon-paperclip',
        component: () => import(`@/views/admin/CategoryManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/gourmetManage",
        name: '美食做法管理',
        icon: 'el-icon-food',
        component: () => import(`@/views/admin/GourmetManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/contentNetManage",
        name: '内容分享管理',
        icon: 'el-icon-position',
        component: () => import(`@/views/admin/ContentNetManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/interactionManage",
        name: '互动信息管理',
        icon: 'el-icon-money',
        component: () => import(`@/views/admin/InteractionManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/evaluationsManage",
        name: '评论管理',
        icon: 'el-icon-chat-dot-round',
        component: () => import(`@/views/admin/EvaluationsManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/flashSaleManage",
        name: '秒杀商品管理',
        icon: 'el-icon-timer',
        component: () => import(`@/views/admin/FlashSaleManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/flashSaleOrderManage",
        name: '秒杀订单管理',
        icon: 'el-icon-shopping-cart-full',
        component: () => import(`@/views/admin/FlashSaleOrderManage.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/redisAdvanced",
        name: 'Redis高级功能',
        icon: 'el-icon-cpu',
        component: () => import(`@/views/admin/RedisAdvanced.vue`),
        meta: { requireAuth: true },
      },
    ]
  },
  {
    path: "/user",
    component: () => import(`@/views/user/Main.vue`),
    meta: {
      requireAuth: true,
    },
    children: [
      {
        path: "/gourmet",
        name: '美食做法',
        component: () => import(`@/views/user/Gourmet.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/gourmetDetail",
        name: '美食做法详情',
        component: () => import(`@/views/user/GourmetDetail.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/service",
        name: '服务中心',
        component: () => import(`@/views/user/Service.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/self",
        name: '个人中心',
        component: () => import(`@/views/user/Self.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/resetPwd",
        name: '重置密码',
        component: () => import(`@/views/user/ResetPwd.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/save",
        name: '我的收藏',
        component: () => import(`@/views/user/Save.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/trending",
        name: '热门排行榜',
        component: () => import(`@/views/user/TrendingFoods.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/flashSale",
        name: '限时秒杀',
        component: () => import(`@/views/user/FlashSale.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/flashSaleDetail",
        name: '秒杀详情',
        component: () => import(`@/views/user/FlashSaleDetail.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/myOrders",
        name: '我的秒杀订单',
        component: () => import(`@/views/user/MyFlashSaleOrders.vue`),
        meta: { requireAuth: true },
      },
      {
        path: "/cityGourmet",
        name: '同城美食推荐',
        component: () => import(`@/views/user/CityGourmet.vue`),
        meta: { requireAuth: true },
      },
    ]
  }
];
const router = new VueRouter({
  routes,
  mode: 'history'
});
router.beforeEach((to, from, next) => {
  if (to.meta.requireAuth) {
    const token = getToken();
    if (token !== null) {
      next();
    } else {
      next("/login");
    }
  }
  else {
    next();
  }
});
export default router;
