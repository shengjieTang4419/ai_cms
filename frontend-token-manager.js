/**
 * 前端Token管理器 - 处理Token预警和自动刷新
 * 配合后端Gateway的Token过期预警机制
 */
class TokenManager {
    constructor() {
        this.isRefreshing = false;
        this.requestQueue = [];
        this.setupInterceptors();
    }

    /**
     * 设置请求和响应拦截器
     */
    setupInterceptors() {
        // 请求拦截器 - 添加Token
        axios.interceptors.request.use(
            (config) => {
                const token = localStorage.getItem('access_token');
                if (token) {
                    config.headers.Authorization = `Bearer ${token}`;
                }
                return config;
            },
            (error) => {
                return Promise.reject(error);
            }
        );

        // 响应拦截器 - 处理Token预警和刷新
        axios.interceptors.response.use(
            (response) => {
                this.handleTokenWarning(response);
                return response;
            },
            (error) => {
                if (error.response?.status === 401) {
                    return this.handleTokenExpired(error.config);
                }
                return Promise.reject(error);
            }
        );
    }

    /**
     * 处理Token预警响应头
     */
    handleTokenWarning(response) {
        const tokenWarning = response.headers['x-token-warning'];
        const tokenRemaining = response.headers['x-token-remaining'];

        if (tokenWarning === 'expiring-soon') {
            console.log(`Token即将过期，剩余时间: ${tokenRemaining}秒`);
            
            // 如果没有正在刷新，则主动刷新Token
            if (!this.isRefreshing) {
                this.refreshTokenProactively();
            }
        }
    }

    /**
     * 主动刷新Token（收到预警时）
     */
    async refreshTokenProactively() {
        if (this.isRefreshing) {
            return;
        }

        this.isRefreshing = true;
        
        try {
            const refreshToken = localStorage.getItem('refresh_token');
            if (!refreshToken) {
                throw new Error('RefreshToken不存在');
            }

            const response = await axios.post('/api/auth/refresh', {
                refresh_token: refreshToken
            });

            const { access_token, refresh_token } = response.data;
            
            // 更新本地存储
            localStorage.setItem('access_token', access_token);
            localStorage.setItem('refresh_token', refresh_token);
            
            console.log('Token预警刷新成功');

        } catch (error) {
            console.error('Token预警刷新失败:', error);
            // 刷新失败，可能需要重新登录
            this.handleRefreshFailure();
        } finally {
            this.isRefreshing = false;
            // 处理等待队列中的请求
            this.processRequestQueue();
        }
    }

    /**
     * 处理Token过期（401响应）
     */
    async handleTokenExpired(originalConfig) {
        // 如果已经在刷新，将请求加入队列
        if (this.isRefreshing) {
            return new Promise((resolve, reject) => {
                this.requestQueue.push({ config: originalConfig, resolve, reject });
            });
        }

        this.isRefreshing = true;

        try {
            const refreshToken = localStorage.getItem('refresh_token');
            if (!refreshToken) {
                throw new Error('RefreshToken不存在');
            }

            const response = await axios.post('/api/auth/refresh', {
                refresh_token: refreshToken
            });

            const { access_token, refresh_token } = response.data;
            
            // 更新本地存储
            localStorage.setItem('access_token', access_token);
            localStorage.setItem('refresh_token', refresh_token);
            
            console.log('Token过期刷新成功');

            // 重试原请求
            originalConfig.headers.Authorization = `Bearer ${access_token}`;
            return axios(originalConfig);

        } catch (error) {
            console.error('Token过期刷新失败:', error);
            this.handleRefreshFailure();
            throw error;
        } finally {
            this.isRefreshing = false;
            // 处理等待队列中的请求
            this.processRequestQueue();
        }
    }

    /**
     * 处理刷新失败的情况
     */
    handleRefreshFailure() {
        // 清除本地Token
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        
        // 清空请求队列
        this.requestQueue.forEach(({ reject }) => {
            reject(new Error('Token刷新失败，请重新登录'));
        });
        this.requestQueue = [];
        
        // 跳转到登录页面
        setTimeout(() => {
            window.location.href = '/login';
        }, 1000);
    }

    /**
     * 处理等待队列中的请求
     */
    processRequestQueue() {
        this.requestQueue.forEach(({ config, resolve, reject }) => {
            const token = localStorage.getItem('access_token');
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
                resolve(axios(config));
            } else {
                reject(new Error('Token刷新失败'));
            }
        });
        this.requestQueue = [];
    }

    /**
     * 获取Token剩余时间（可选功能）
     */
    getTokenRemainingTime() {
        const token = localStorage.getItem('access_token');
        if (!token) {
            return 0;
        }

        try {
            // 解析JWT获取过期时间
            const payload = JSON.parse(atob(token.split('.')[1]));
            const exp = payload.exp * 1000; // 转换为毫秒
            const now = Date.now();
            return Math.max(0, exp - now);
        } catch (error) {
            console.error('解析Token失败:', error);
            return 0;
        }
    }

    /**
     * 检查Token是否即将过期（可选的主动检查）
     */
    isTokenExpiringSoon() {
        const remainingTime = this.getTokenRemainingTime();
        const totalLifetime = 60 * 60 * 1000; // 60分钟
        const warningThreshold = totalLifetime * 0.25; // 剩余25%时预警
        
        return remainingTime <= warningThreshold && remainingTime > 0;
    }
}

// 创建全局Token管理器实例
const tokenManager = new TokenManager();

// 导出供其他模块使用
if (typeof module !== 'undefined' && module.exports) {
    module.exports = TokenManager;
} else if (typeof window !== 'undefined') {
    window.TokenManager = TokenManager;
    window.tokenManager = tokenManager;
}
