import request from '@/utils/request'
import axios from 'axios'

// 创建一个单独的axios实例用于Python推荐系统
const pythonRequest = axios.create({
  baseURL: 'http://localhost:5000',
  timeout: 8000, // 增加超时时间
  withCredentials: true // 允许跨域请求携带凭证
});

// 添加请求拦截器，为Python请求添加token
pythonRequest.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token !== null) {
    config.headers["token"] = token;
  }
  return config;
}, error => {
  return Promise.reject(error);
});

// 添加响应拦截器，处理跨域问题
pythonRequest.interceptors.response.use(response => {
  return response;
}, error => {
  console.warn("Python推荐系统请求错误:", error.message);
  // 添加更详细的错误日志
  if (error.response) {
    // 服务器返回了错误响应
    console.warn("错误状态:", error.response.status);
    console.warn("错误数据:", error.response.data);
  } else if (error.request) {
    // 请求已发出，但没有收到响应
    console.warn("未收到响应，可能是网络问题或服务未启动");
  }
  return Promise.reject(error);
});

// 基于内容的推荐API封装
export const recommendApi = {
  // 获取相似美食推荐
  getRecommendationList() {
    console.log('发送推荐请求到Python系统');
    // 首先尝试从Python推荐系统获取个性化推荐
    return pythonRequest.get('/recommend/list')
      .then(pythonRes => {
        console.log("Python推荐系统响应:", pythonRes);
        return pythonRes;
      })
      .catch(error => {
        console.warn("Python推荐系统请求失败，回退到Java后端:", error);
        // 如果Python系统失败，回退到Java后端
        return request({
          url: `/recommend/10`,
          method: 'get',
          timeout: 10000
        });
      });
  },

  // 获取效果
  evaluateMetricsApi() {
    console.log('发送评估请求到Python系统');
    // 首先尝试从Python推荐系统获取评估指标
    return pythonRequest.get('/recommend/evaluate')
      .then(pythonRes => {
        console.log("Python推荐系统评估响应:", pythonRes);
        return pythonRes;
      })
      .catch(error => {
        console.warn("Python推荐系统评估请求失败，回退到Java后端:", error);
        // 如果Python系统失败，回退到Java后端
        return request({
          url: `/recommend/metrics/evaluate`,
          method: 'get',
          timeout: 8000
        });
      });
  },

  // 获取热门美食排行榜
  getTrendingFoods(limit = 10) {
    return request({
      url: `/recommend/trending`,
      method: 'get',
      params: { limit }
    })
  },

  // 获取美食UV统计
  getGourmetUV(gourmetId) {
    return request({
      url: `/recommend/uv/${gourmetId}`,
      method: 'get'
    })
  }
}
