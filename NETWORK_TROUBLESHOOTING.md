# Android网络连接问题排查指南

## 问题描述
服务器端测试正常，但Android应用无法连接服务器。

## 排查步骤

### 1. 检查服务器配置
确认服务器信息：
- **IP地址**: 211.83.111.165
- **端口**: 8081 (已更新)
- **服务状态**: 运行中

### 2. 网络诊断工具
应用现在包含网络诊断功能，会逐步检查：
1. **DNS解析**: 检查IP地址是否可解析
2. **端口连通性**: 检查端口是否可访问
3. **HTTP连接**: 检查API端点是否响应

### 3. 常见问题及解决方案

#### 问题1: DNS解析失败
**症状**: "DNS解析失败，无法连接到服务器"
**可能原因**:
- 网络DNS配置问题
- 防火墙阻止DNS查询

**解决方案**:
```bash
# 在Android设备上测试
nslookup 211.83.111.165
ping 211.83.111.165
```

#### 问题2: 端口无法访问
**症状**: "端口 8081 无法访问"
**可能原因**:
- 服务器防火墙未开放8081端口
- 路由器/ISP防火墙阻止
- 服务器服务未启动

**解决方案**:
```bash
# 在服务器上检查端口状态
netstat -tlnp | grep 8081

# 检查防火墙
ufw status
iptables -L

# 开放端口
ufw allow 8081
```

#### 问题3: HTTP连接失败
**症状**: "HTTP连接失败"
**可能原因**:
- 服务器应用未启动
- 应用配置错误
- 网络代理问题

**解决方案**:
```bash
# 检查服务状态
ps aux | grep api_server

# 检查日志
tail -f api_server.log

# 本地测试
curl http://localhost:8081/health
```

### 4. Android端排查

#### 检查网络权限
确保 `AndroidManifest.xml` 包含：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

#### 检查网络类型
- **WiFi**: 确保连接到正确的网络
- **移动数据**: 检查是否有网络限制
- **代理**: 检查是否配置了网络代理

#### 测试网络连接
使用应用中的"测试连接"按钮进行诊断。

### 5. 服务器端排查

#### 检查服务状态
```bash
# 连接到服务器
ssh root@211.83.111.165 -p 15020

# 检查进程
ps aux | grep api_server

# 检查端口
netstat -tlnp | grep 8081

# 检查日志
tail -f /root/pixart/api_server.log
```

#### 重启服务
```bash
# 停止服务
pkill -f api_server.py

# 重新启动
cd /root/pixart
nohup python3 api_server.py > api_server.log 2>&1 &
```

#### 检查防火墙
```bash
# 检查防火墙状态
ufw status

# 开放端口
ufw allow 8081

# 重启防火墙
ufw reload
```

### 6. 网络测试命令

#### 从Android设备测试
```bash
# 测试网络连通性
ping 211.83.111.165

# 测试端口连通性
telnet 211.83.111.165 8081

# 测试HTTP连接
curl http://211.83.111.165:8081/health
```

#### 从其他设备测试
```bash
# 测试端口是否开放
nmap -p 8081 211.83.111.165

# 测试HTTP响应
curl -v http://211.83.111.165:8081/health
```

### 7. 调试信息收集

#### 应用日志
查看Android Studio的Logcat输出，寻找：
- 网络连接错误
- 超时信息
- 异常堆栈

#### 服务器日志
检查服务器端日志：
```bash
tail -f /root/pixart/api_server.log
```

### 8. 临时解决方案

如果问题无法立即解决，可以：

#### 使用不同的端口
修改服务器端口为常用端口（如80、443、8000等）

#### 使用内网地址
如果可能，使用内网IP地址进行测试

#### 使用HTTPS
如果服务器支持HTTPS，尝试使用HTTPS连接

### 9. 联系网络管理员

如果以上步骤都无法解决问题，可能需要：
1. 联系服务器网络管理员
2. 检查ISP网络限制
3. 确认服务器网络配置

## 验证步骤

### 成功标志
当网络连接正常时，应用会显示：
- "✅ 服务器和模型都正常"
- 能够正常生成图片

### 失败标志
常见失败信息：
- "DNS解析失败"
- "端口 8081 无法访问"
- "HTTP连接失败"
- "网络请求失败"

## 预防措施

1. **定期检查服务器状态**
2. **监控网络连接**
3. **配置网络监控工具**
4. **建立故障恢复流程**

























































































