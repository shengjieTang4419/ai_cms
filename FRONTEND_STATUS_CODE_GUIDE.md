# 前端状态码处理指南

## 🎯 **状态码分类**

### **401 Unauthorized - 需要重新登录**
**场景：** 安全问题，不允许刷新Token

**触发条件：**
- `令牌验证不正确` - JWT格式错误或签名无效
- `令牌验证失败` - JWT信息不完整
- `登录状态已过期` - Redis中无记录
- `检测到设备变化，请重新登录` - 设备绑定验证失败
- `令牌验证失败` - 其他异常情况

**前端处理：**
```javascript
axios.interceptors.response.use(
  response => response,
  error => {
    const status = error.response?.status;
    const message = error.response?.data?.msg;
    
    if (status === 401) {
      // 401 - 安全问题，直接跳转登录
      console.error('认证失败，需要重新登录:', message);
      clearTokens();
      redirectToLogin();
      return Promise.reject(error);
    }
    
    // ... 其他状态码处理
  }
);
```

---

### **407 Proxy Authentication Required - 可以刷新Token**
**场景：** AccessToken过期，RefreshToken有效

**触发条件：**
- `令牌已过期，请刷新` - JWT时间过期但格式正确

**前端处理：**
```javascript
axios.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;
    const status = error.response?.status;
    
    if (status === 407 && !originalRequest._retry) {
      originalRequest._retry = true; // 标记已重试
      
      try {
        // 刷新Token
        const newTokens = await refreshToken();
        
        // 更新原请求的Authorization头
        originalRequest.headers['Authorization'] = `Bearer ${newTokens.access_token}`;
        
        // 重试原请求
        return axios(originalRequest);
        
      } catch (refreshError) {
        // RefreshToken也失效了，跳转登录
        console.error('RefreshToken失效，需要重新登录');
        clearTokens();
        redirectToLogin();
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);
```

---

## 🔄 **完整的前端处理逻辑**

```javascript
import axios from 'axios';

// Token管理
const TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';

// Axios实例
const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL,
  timeout: 10000,
});

// 请求拦截器 - 添加Authorization头
api.interceptors.request.use(config => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器 - 处理Token刷新
let isRefreshing = false;
let refreshPromise = null;

api.interceptors.response.use(
  // 成功响应
  response => {
    // 检查Token预警
    const tokenWarning = response.headers['x-token-warning'];
    if (tokenWarning === 'expiring-soon') {
      // 异步刷新，不阻塞当前请求
      refreshTokenAsync();
    }
    return response;
  },
  
  // 错误响应
  async error => {
    const originalRequest = error.config;
    const status = error.response?.status;
    const message = error.response?.data?.msg;
    
    // 401 - 安全问题，直接跳转登录
    if (status === 401) {
      console.error('认证失败，需要重新登录:', message);
      clearTokens();
      redirectToLogin();
      return Promise.reject(error);
    }
    
    // 407 - Token过期，尝试刷新
    if (status === 407 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        // 同步刷新Token
        const newTokens = await refreshTokenSync();
        
        // 更新原请求
        originalRequest.headers.Authorization = `Bearer ${newTokens.access_token}`;
        return api(originalRequest);
        
      } catch (refreshError) {
        // RefreshToken也失效了
        console.error('RefreshToken失效:', refreshError.response?.data?.msg);
        clearTokens();
        redirectToLogin();
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

// 同步刷新Token（阻塞式）
async function refreshTokenSync() {
  if (refreshPromise) {
    return refreshPromise; // 复用正在进行的刷新
  }
  
  refreshPromise = api.post('/auth/refresh', {
    refresh_token: getRefreshToken()
  })
  .then(response => {
    const { access_token, refresh_token } = response.data;
    setAccessToken(access_token);
    setRefreshToken(refresh_token);
    console.log('Token刷新成功');
    return response.data;
  })
  .catch(error => {
    console.error('Token刷新失败:', error.response?.data?.msg);
    throw error;
  })
  .finally(() => {
    refreshPromise = null;
  });
  
  return refreshPromise;
}

// 异步刷新Token（非阻塞式）
function refreshTokenAsync() {
  if (isRefreshing) return;
  
  isRefreshing = true;
  api.post('/auth/refresh', {
    refresh_token: getRefreshToken()
  })
  .then(response => {
    const { access_token, refresh_token } = response.data;
    setAccessToken(access_token);
    setRefreshToken(refresh_token);
    console.log('Token预警刷新成功');
  })
  .catch(error => {
    console.error('Token预警刷新失败:', error.response?.data?.msg);
  })
  .finally(() => {
    isRefreshing = false;
  });
}

// 工具函数
function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

function setAccessToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

function setRefreshToken(token) {
  localStorage.setItem(REFRESH_TOKEN_KEY, token);
}

function clearTokens() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

function redirectToLogin() {
  // 保存当前路由
  sessionStorage.setItem('redirect_after_login', window.location.pathname);
  window.location.href = '/login';
}

export default api;
```

## 📱 **移动端场景示例**

**场景：用户1天没用App**

1. **用户打开App** → 发送请求（携带过期的AccessToken）
2. **Gateway验证** → JWT过期 → 返回`407 令牌已过期，请刷新`
3. **前端捕获407** → 调用refresh接口
4. **刷新成功** → 获取新AccessToken → 重试原请求
5. **用户无感知** → 继续正常使用

**场景：RefreshToken也过期了**

1. **用户打开App** → 发送请求（AccessToken过期）
2. **Gateway返回407** → 前端调用refresh接口
3. **RefreshToken过期** → refresh接口返回401
4. **前端捕获401** → 清除Token → 跳转登录页
5. **用户体验** → 需要重新登录（合理，因为7天没用了）

## 🛡️ **安全优势**

1. **精确区分**：401和407明确区分不同场景
2. **防止攻击**：恶意请求无法通过刷新Token绕过验证
3. **用户体验**：正常过期无感刷新，安全问题强制登录
4. **日志清晰**：便于安全审计和问题排查

这样的设计既保证了安全性，又提供了良好的用户体验！
