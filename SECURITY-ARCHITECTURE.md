# Security 架构说明

## 问题背景

在微服务架构中，不同模块对用户认证的需求不同：
- **System 模块**：需要完整的用户管理功能（登录、注册、权限验证）
- **Chat 模块**：只需要知道"谁"在调用，不需要复杂的权限管理

## 解决方案：JWT + 条件化 UserDetailsService

### 核心设计思路

1. **JWT 中存储关键用户信息**（userId, username）
2. **System 模块**：实现 `UserDetailsServiceAdapter`，从数据库加载完整用户信息
3. **Chat 模块**：不实现 `UserDetailsServiceAdapter`，直接从 JWT 解析用户信息

### 工作流程

#### System 模块（完整认证）

```
用户登录 
  ↓
UserService 验证用户名密码
  ↓
生成 JWT Token（包含 userId）
  ↓
返回给前端
  ↓
前端请求（带 Token）
  ↓
AuthTokenFilter 验证 Token
  ↓
UserService.loadUserByUsername() - 查询数据库
  ↓
加载用户详情（userId, username, email, roles, authorities）
  ↓
创建 Authentication 并放入 SecurityContext
  ↓
处理请求
```

#### Chat 模块（简化认证）

```
前端请求（带 Token）
  ↓
AuthTokenFilter 验证 Token 签名和有效期
  ↓
从 JWT 直接解析 userId 和 username
  ↓
创建简单的 CustomerUserDetail（不查询数据库）
  ↓
创建 Authentication 并放入 SecurityContext
  ↓
处理请求
```

### 两个模块的区别

| 功能 | System 模块 | Chat 模块 |
|------|------------|----------|
| Token 签名验证 | ✅ | ✅ |
| Token 过期验证 | ✅ | ✅ |
| 查询数据库 | ✅ | ❌ |
| 加载角色权限 | ✅ | ❌ |
| 获取 userId | ✅ | ✅ |
| 获取 username | ✅ | ✅ |
| 获取 email | ✅ | ❌ |
| 获取 roles | ✅ | ❌ |
| 登录注册功能 | ✅ | ❌ |

### 在业务代码中使用

#### 获取当前用户信息

```java
// 方式1：获取完整用户信息（推荐）
CustomerUserDetail userDetail = SecurityUtils.getCurrentUserDetail();
Long userId = userDetail.getUserId();
String username = userDetail.getUsername();

// 方式2：直接获取 userId
Long userId = SecurityUtils.getCurrentUserId();

// 方式3：直接获取 username
String username = SecurityUtils.getCurrentUsername();
```

#### System 模块可获取的信息

```java
CustomerUserDetail userDetail = SecurityUtils.getCurrentUserDetail();
userDetail.getUserId();      // ✅ 用户ID
userDetail.getUsername();    // ✅ 用户名
userDetail.getEmail();       // ✅ 邮箱
userDetail.getAuthorities(); // ✅ 角色权限列表
```

#### Chat 模块可获取的信息

```java
CustomerUserDetail userDetail = SecurityUtils.getCurrentUserDetail();
userDetail.getUserId();      // ✅ 用户ID（从 JWT 解析）
userDetail.getUsername();    // ✅ 用户名（从 JWT 解析）
userDetail.getEmail();       // ❌ null（未查询数据库）
userDetail.getAuthorities(); // ✅ 空列表（不需要权限）
```

### 核心代码说明

#### 1. JwtUtils.java

生成 Token 时，将 userId 存入 JWT claims：

```java
public String generateJwtToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    
    Long userId = null;
    if (userPrincipal instanceof CustomerUserDetail) {
        userId = ((CustomerUserDetail) userPrincipal).getUserId();
    }

    var builder = Jwts.builder()
            .subject(userPrincipal.getUsername())
            .issuedAt(new Date())
            .expiration(new Date((new Date()).getTime() + jwtExpirationMs * 1000L));
    
    if (userId != null) {
        builder.claim("userId", userId);
    }
    
    return builder.signWith(getSigningKey()).compact();
}
```

从 Token 解析 userId：

```java
public Long getUserIdFromJwtToken(String token) {
    Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    
    Object userIdObj = claims.get("userId");
    if (userIdObj != null) {
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
    }
    return null;
}
```

#### 2. AuthTokenFilter.java

条件化加载用户信息：

```java
// 如果有 UserDetailsService（System 模块）
if (userDetailsService != null) {
    // 从数据库加载完整用户信息
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    // ... 创建认证对象
} else {
    // 没有 UserDetailsService（Chat 模块）
    // 从 JWT 直接解析用户信息
    Long userId = jwtUtils.getUserIdFromJwtToken(jwt);
    
    CustomerUserDetail userDetails = new CustomerUserDetail();
    userDetails.setUserId(userId);
    userDetails.setUsername(username);
    userDetails.setAuthorities(Collections.emptyList());
    // ... 创建认证对象
}
```

#### 3. SecurityConfig.java

条件化创建 AuthenticationProvider：

```java
@Bean
@ConditionalOnBean(UserDetailsServiceAdapter.class)
public DaoAuthenticationProvider authenticationProvider() {
    // 只有 System 模块需要这个 Provider（用于登录验证）
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder);
    return authProvider;
}
```

### 优势

1. ✅ **安全性**：所有模块都验证 Token 的真实性和有效期
2. ✅ **性能**：Chat 模块不需要每次请求都查数据库
3. ✅ **灵活性**：新模块可以自由选择是否实现 UserDetailsService
4. ✅ **无状态**：用户信息存在 JWT 中，完全无状态
5. ✅ **简洁性**：不需要额外的 ThreadLocal 或缓存管理

### 与 Ruoyi 等框架的对比

| 特性 | Ruoyi（ThreadLocal） | 本方案（JWT Claims） |
|------|---------------------|---------------------|
| 用户信息存储 | Redis + ThreadLocal | JWT Claims |
| 状态管理 | 有状态（需要 Redis） | 无状态 |
| 性能 | 需要访问 Redis | 直接解析 JWT |
| 扩展性 | 水平扩展需要共享 Redis | 天然支持水平扩展 |
| 复杂度 | 较高（需要管理生命周期） | 较低（自包含） |

### 注意事项

1. **JWT Token 过期后需要重新登录**，此时会重新生成包含最新 userId 的 Token
2. **不要在 JWT 中存储敏感信息**（如密码、详细权限等）
3. **userId 在 JWT 有效期内不会自动更新**，如果需要实时性，应该在 System 模块实现
4. **Chat 模块不支持复杂的权限验证**，如需要权限，请使用 System 模块或扩展方案

---

更新时间：2025-10-01

