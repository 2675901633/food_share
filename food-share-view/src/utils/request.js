import axios from "axios"
import { getToken } from "@/utils/storage.js";
// ... existing code ...
const request = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // url = base url + request url
  withCredentials: true, // 跨域请求时发送cookies
  timeout: 30000, // 请求超时时间毫秒
});
// ... existing code ...
//全局拦截器
request.interceptors.request.use(config => {
  // 记录请求信息便于调试
  console.log(`请求API: ${config.url}`, config.method.toUpperCase(), config.data || config.params);

  const token = getToken();

  // 设置统一的baseURL，所有接口都走Java后端
  config.baseURL = "http://localhost:21090/api/food-share-sys/v1.0"; // Java 服务地址

  // 添加token到请求头
  if (token !== null) {
    config.headers["token"] = token;
  }

  return config;
}, error => {
  console.error("请求错误:", error);
  return Promise.reject(error);
});

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 请求成功，但需检查业务状态码
    const res = response.data;

    // 记录响应信息便于调试
    console.log(`响应API: ${response.config.url}`, response.status, res);

    // 如果业务状态码不是200，给出警告
    if (res.code !== 200) {
      console.warn(`API响应异常: ${response.config.url}`, res.code, res.msg);
    }

    return response;
  },
  error => {
    // 请求失败处理
    console.error("响应错误:", error);
    if (error.response) {
      // 服务器返回错误状态码
      console.error(`服务器错误: ${error.response.status}`, error.response.data);
    } else if (error.request) {
      // 请求发送但没有收到响应
      console.error("请求超时或网络错误:", error.request);
    } else {
      // 请求配置错误
      console.error("请求配置错误:", error.message);
    }

    return Promise.reject(error);
  }
);
// ... existing code ...
export default request;