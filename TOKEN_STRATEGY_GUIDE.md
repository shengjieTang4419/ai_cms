# Token双策略架构设计指南

## 🎯 **设计背景**

在实际应用中，PC端和移动端对Token的需求差异很大：

- **PC端**：用户连续操作，安全性优先，短期Token即可满足需求
- **移动端**：用户间歇操作，体验优先，需要长期免登录

本方案通过**设备类型自动识别**，为不同端提供不同的Token策略，既保证了安全性，又兼顾了用户体验。

## 📋 **策略对比**

| 特性 | PC端策略 | 移动端策略 | 说明 |
|------|---------|-----------|------|
| **AccessToken有效期** | 60分钟 | 120分钟 | 移动端更长，减少刷新频率 |
| **RefreshToken有效期** | 60分钟 | 7天 | 移动端长期有效 |
| **刷新阈值** | 3/4 (45分钟) | 3/4 (90分钟) | 提前预警时间点 |
| **刷新窗口** | 45-60分钟 | 90-120分钟 | RefreshToken可用窗口 |
| **设备绑定** | ✅ 严格 | ✅ 严格 | 设备变化需重新登录 |
| **消费策略** | 一次性消费 | 重复消费 | PC端用后即焚，移动端可重复使用 |

## 🔄 **设备类型识别**

### **自动识别规则**

系统通过`deviceId`自动识别设备类型：

```java
// 移动端设备ID格式
mobile_xxxxx
ios_xxxxx
android_xxxxx

// PC端设备ID格式
pc_xxxxx
web_xxxxx

// 未知设备（默认使用PC策略）
其他格式
```

### **前端设备ID生成建议**

```javascript
// PC端（Web）
const deviceId = `pc_${fingerprint}_${timestamp}`;

// 移动端（iOS/Android）
const deviceId = `mobile_${platform}_${uuid}`;
// 或
const deviceId = `ios_${deviceUUID}`;
const deviceId = `android_${deviceUUID}`;
```

## 🚀 **使用流程**

### **1. 登录流程**

```java
// 前端传入deviceId
POST /auth/login
{
    "username": "admin",
    "password": "123456",
    "deviceId": "mobile_ios_xxxxx"  // 自动识别为移动端
}

// 响应
{
    "access_token": "xxx",
    "refresh_token": "xxx",
    "expires_in": 120,              // 移动端：120分钟
    "refresh_expires_in": 10080,    // 移动端：7天
    "device_type": "mobile",
    "strategy": "移动端策略: AccessToken=120分钟, RefreshToken=7天"
}
```

### **2. Gateway预警机制**

```
PC端：
- 0-45分钟：正常使用
- 45-60分钟：返回预警头 X-Token-Warning: expiring-soon
- 60分钟后：Token过期

移动端：
- 0-90分钟：正常使用
- 90-120分钟：返回预警头 X-Token-Warning: expiring-soon
- 120分钟后：AccessToken过期，使用RefreshToken刷新
- 7天后：双Token过期，需重新登录
```

### **3. RefreshToken刷新**

```java
POST /auth/refresh
{
    "refresh_token": "xxx"
}

// PC端策略：
// 1. 从Redis获取设备类型
// 2. 生成新的userKey和新Token
// 3. 删除旧Token（一次性消费）

// 移动端策略：
// 1. 从Redis获取设备类型
// 2. 复用旧的userKey，只更新JWT内容
// 3. 更新Redis中的Token信息（不删除）
// 4. 7天内可重复使用RefreshToken刷新
```

## 🎯 **核心优势**

### **1. 自动适配**
- 无需前端手动选择策略
- 根据deviceId自动识别设备类型
- 同一用户在不同设备上使用不同策略

### **2. 安全可控**
- PC端：60分钟统一TTL，15分钟风险窗口，一次性消费
- 移动端：设备绑定 + 重复消费，7天内安全可控
- 设备变化立即失效

### **3. 体验优化**
- PC端：连续操作无感知续期
- 移动端：7天免登录，间歇操作友好
- 预警机制：提前通知前端刷新

### **4. 架构优雅**
- 策略配置集中管理
- 代码职责清晰分离
- 易于扩展新策略

## 📝 **实现细节**

### **核心类**

```
DeviceType.java          - 设备类型枚举
TokenStrategy.java       - Token策略工具类
CacheConstants.java      - 策略配置常量
TokenService.java        - Token生成服务（支持策略）
AuthFilter.java          - Gateway预警（支持多策略）
```

### **扩展新策略**

如需添加新的设备类型（如平板），只需：

1. 在`DeviceType`添加枚举
2. 在`CacheConstants`添加配置
3. 在`TokenStrategy`添加策略逻辑
4. 在`DeviceType.fromDeviceId()`添加识别规则

## 🔄 **消费策略详解**

### **PC端：一次性消费策略**

```java
// 刷新流程
1. 用户使用RefreshToken请求刷新
2. 验证RefreshToken有效性
3. 生成新的userKey（UUID）
4. 生成新的AccessToken和RefreshToken
5. 删除旧的Redis记录（一次性消费）
6. 返回新Token

// 特点
- ✅ 每次刷新都生成全新的Token
- ✅ 旧RefreshToken立即失效
- ✅ 防止重放攻击
- ✅ 适合连续操作场景
```

### **移动端：重复消费策略**

```java
// 刷新流程
1. 用户使用RefreshToken请求刷新
2. 验证RefreshToken有效性
3. 复用旧的userKey（不变）
4. 生成新的AccessToken和RefreshToken（JWT内容更新）
5. 更新Redis记录（不删除，保持7天TTL）
6. 返回新Token

// 特点
- ✅ userKey保持不变
- ✅ 7天内可多次使用RefreshToken刷新
- ✅ 适合间歇操作场景
- ✅ 设备绑定保证安全性
- ⚠️ 注意：虽然可重复消费，但设备变化会立即失效
```

### **为什么要差异化？**

| 场景 | PC端 | 移动端 |
|------|------|--------|
| **使用模式** | 连续操作，会话明确 | 间歇操作，随时打开 |
| **安全要求** | 高（企业内网） | 中（设备绑定已保证） |
| **体验要求** | 中（操作中无感） | 高（长期免登录） |
| **最佳策略** | 一次性消费 | 重复消费 |

## 🛡️ **安全建议**

### **PC端（企业内网）**
- ✅ 短期Token，降低风险
- ✅ 严格设备绑定
- ✅ 一次性消费RefreshToken
- ✅ 45-60分钟刷新窗口限制

### **移动端（公网环境）**
- ✅ 设备绑定（设备变化失效）
- ✅ 重复消费（7天内可用）
- ✅ 7天有效期（平衡体验与安全）
- ✅ userKey不变，便于追踪
- ⚠️ 建议：敏感操作需二次验证

## 🎯 **最佳实践**

### **前端集成**

```javascript
// 统一Token管理器（自动处理预警）
class TokenManager {
    handleTokenWarning(response) {
        const warning = response.headers['x-token-warning'];
        if (warning === 'expiring-soon') {
            // PC端：45分钟预警
            // 移动端：90分钟预警
            this.refreshTokenProactively();
        }
    }
}
```

### **设备ID管理**

```javascript
// 持久化设备ID
const getDeviceId = () => {
    let deviceId = localStorage.getItem('deviceId');
    if (!deviceId) {
        const platform = detectPlatform(); // 'pc' or 'mobile'
        const uuid = generateUUID();
        deviceId = `${platform}_${uuid}`;
        localStorage.setItem('deviceId', deviceId);
    }
    return deviceId;
};
```

## 🎓 **学习价值**

这套双策略架构展示了：

1. **策略模式**：根据设备类型选择不同策略
2. **开闭原则**：易于扩展新策略，无需修改现有代码
3. **单一职责**：每个类职责清晰
4. **配置分离**：策略配置集中管理

这是一个**生产级别的架构设计**，既满足了学习目的，又具备实际应用价值。

## 💡 **总结**

这套方案完美解决了"左右脑互搏"的困境：

- ✅ **保留了精心设计的PC端安全机制**（45-60分钟窗口）
- ✅ **预留了移动端的扩展能力**（7天长期Token）
- ✅ **架构优雅，易于维护和扩展**
- ✅ **学习价值高，可作为项目亮点**

当你开始做移动端时，只需要确保deviceId格式正确，系统会自动使用移动端策略，无需修改任何业务代码！
