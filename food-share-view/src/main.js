import Vue from "vue";
import App from "./App.vue";
import router from "./router";
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import { provinceAndCityData, regionData } from 'element-china-area-data';
import VueSweetalert2 from 'vue-sweetalert2';
import 'sweetalert2/dist/sweetalert2.min.css';
import './assets/css/editor.scss'
import './assets/css/button.scss'
import './assets/css/elementui-cover.scss'
import './assets/css/basic.scss'
import './assets/css/dialog.scss'
import './assets/css/input.scss'
import request from '@/utils/request'
import md5 from 'js-md5';
import ErrorHandler from '@/components/ErrorHandler'

Vue.config.productionTip = false;
Vue.use(ElementUI);
Vue.use(VueSweetalert2);
Vue.prototype.$md5 = md5;
Vue.prototype.$axios = request;
import swalPlugin from '@/utils/swalPlugin';
Vue.use(swalPlugin);

// 注册全局错误处理组件
Vue.component('error-handler', ErrorHandler)

// 全局错误处理
Vue.config.errorHandler = function (err, vm, info) {
  console.error('Vue错误:', err);
  console.error('错误信息:', info);

  // 向根组件发送错误事件
  if (vm.$root) {
    vm.$root.$emit('error', {
      message: '系统遇到了一个错误: ' + err.message,
      type: 'vue',
      showRecommendation: true,
      autoHide: true
    });
  }
}

// 未捕获的Promise错误
window.addEventListener('unhandledrejection', event => {
  // 忽略Vue Router的重复导航错误
  if (event.reason && event.reason.name === 'NavigationDuplicated') {
    event.preventDefault(); // 阻止错误显示在控制台
    return;
  }

  console.error('未处理的Promise错误:', event.reason);

  // 判断错误类型
  let errorType = 'network';
  let errorMessage = '网络请求失败，请检查网络连接';

  if (event.reason.message && event.reason.message.includes('timeout')) {
    errorType = 'timeout';
    errorMessage = '请求超时，服务器响应时间过长';
  } else if (event.reason.response && event.reason.response.status === 500) {
    errorType = 'server';
    errorMessage = '服务器内部错误，请稍后再试';
  }

  // 向根组件发送错误事件
  if (window.vueApp) {
    window.vueApp.$emit('error', {
      message: errorMessage,
      type: errorType,
      showRecommendation: true,
      autoHide: true
    });
  }
});

// 在全局存储Vue实例以便在非Vue上下文中访问
const app = new Vue({
  router,
  regionData,
  provinceAndCityData,
  VueSweetalert2,
  render: h => h(App)
}).$mount('#app')

window.vueApp = app;