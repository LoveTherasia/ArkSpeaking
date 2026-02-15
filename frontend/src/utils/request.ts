import axios from 'axios';

// 配置后端基础路径（根据你的后端实际端口调整，比如8080）
const request = axios.create({
  baseURL: 'http://localhost:8080', // 核心：指定后端服务器地址
  timeout: 5000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
});

// 响应拦截器：统一解析后端的Result包装对象
request.interceptors.response.use(
  (response) => {
    // 后端返回的是Result对象，取出data字段
    const res = response.data;
    // 成功码（根据你后端Result的成功码调整，比如200）
    if (res.code !== 200) {
      // 非200码视为失败，抛出错误
      return Promise.reject(new Error(res.msg || '请求失败'));
    }
    // 只返回Result中的data部分，供业务代码使用
    return res.data;
  },
  (error) => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

export default request;