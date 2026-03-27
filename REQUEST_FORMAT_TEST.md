# 请求格式验证说明

## 测试步骤

### 1. 启用真实API
1. 打开应用
2. 点击"测试API"按钮
3. 如果测试成功，选择"启用"真实API

### 2. 发送测试问题
1. 在搜索框输入问题（例如："你好"）
2. 点击发送按钮
3. 查看Logcat日志

## 预期的请求格式

应用会发送以下格式的JSON请求到服务器：

```json
{
  "model": "glm-4.6",
  "messages": [
    {
      "role": "user",
      "content": "你好"
    }
  ],
  "max_tokens": 1024,
  "temperature": 0.01,
  "top_p": 0.1,
  "stream": false
}
```

### 请求详情
- **URL**: `http://222.19.82.57:8000/v1/chat/completions`
- **Method**: POST
- **Content-Type**: application/json

## 查看日志

在Android Studio的Logcat中过滤 `ChatFragment` 标签，你会看到：

### 发送请求时的日志
```
D/ChatFragment: ========== 发送问题到服务器 ==========
D/ChatFragment: 问题内容: 你好
D/ChatFragment: 使用真实API: true
D/ChatFragment: 构建真实API请求...
D/ChatFragment: ========== 请求详情 ==========
D/ChatFragment: URL: http://222.19.82.57:8000/v1/chat/completions
D/ChatFragment: Method: POST
D/ChatFragment: Content-Type: application/json
D/ChatFragment: 请求体JSON:
D/ChatFragment: {
D/ChatFragment:   "model": "glm-4.6",
D/ChatFragment:   "messages": [
D/ChatFragment:     {
D/ChatFragment:       "role": "user",
D/ChatFragment:       "content": "你好"
D/ChatFragment:     }
D/ChatFragment:   ],
D/ChatFragment:   "max_tokens": 1024,
D/ChatFragment:   "temperature": 0.01,
D/ChatFragment:   "top_p": 0.1,
D/ChatFragment:   "stream": false
D/ChatFragment: }
D/ChatFragment: ===================================
D/ChatFragment: 发送HTTP请求...
```

### 收到响应时的日志
```
D/ChatFragment: ========== 收到响应 ==========
D/ChatFragment: HTTP状态码: 200
D/ChatFragment: 响应体长度: xxx
D/ChatFragment: 响应内容: {...}
D/ChatFragment: ===================================
D/ChatFragment: 响应成功，开始处理
```

### 如果请求失败
```
E/ChatFragment: ========== 请求失败 ==========
E/ChatFragment: 错误类型: java.net.SocketTimeoutException
E/ChatFragment: 错误消息: timeout
E/ChatFragment: ===================================
```

## 验证清单

- [x] 请求URL正确：`http://222.19.82.57:8000/v1/chat/completions`
- [x] 请求方法：POST
- [x] Content-Type：application/json
- [x] JSON格式包含所有必需字段：
  - [x] model: "glm-4.6"
  - [x] messages: 数组格式，包含role和content
  - [x] max_tokens: 1024
  - [x] temperature: 0.01
  - [x] top_p: 0.1
  - [x] stream: false

## 常见问题

### 1. 看不到日志
- 确保在Logcat中选择了正确的设备
- 过滤器设置为 `ChatFragment`
- 日志级别设置为 `Debug` 或 `Verbose`

### 2. 请求格式不对
- 检查日志中的"请求体JSON"部分
- 确认所有字段都存在且格式正确

### 3. 服务器返回500错误
- 检查服务器端日志
- 确认服务器API实现正确
- 验证请求格式是否符合服务器要求

## 对比curl命令

应用发送的请求等同于以下curl命令：

```bash
curl --location 'http://222.19.82.57:8000/v1/chat/completions' \
--header 'Content-Type: application/json' \
--data '{
  "model": "glm-4.6",
  "messages": [
    {
      "role": "user",
      "content": "你好"
    }
  ],
  "max_tokens": 1024,
  "temperature": 0.01,
  "top_p": 0.1,
  "stream": false
}'
```

## 下一步

如果请求格式正确但服务器返回错误：
1. 检查服务器端日志
2. 确认API端点路径正确
3. 验证服务器是否正确处理该格式的请求
4. 检查是否需要额外的认证头或参数
