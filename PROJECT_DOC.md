# 阳光清言 - 藏语大模型 Android 应用

## 项目概述

**项目名称**: 阳光清言 (Sunshine Chat)
**包名**: com.example.sunshine
**版本**: 1.2 (versionCode: 3)
**目标SDK**: Android 33 (minSdk: 21)
**开发语言**: Java

本项目是一款基于藏语大模型的智能对话应用，支持藏文、中文、英文三种语言界面，提供语音识别、语音合成、AI对话等功能。

---

## 项目结构

```
MyApplication2/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/myapplication/
│   │   │   │   ├── api/                    # 网络请求模块
│   │   │   │   │   ├── ApiService.java     # API接口定义
│   │   │   │   │   ├── RetrofitClient.java # Retrofit客户端
│   │   │   │   │   ├── BaseResponse.java   # 响应基类
│   │   │   │   │   └── model/              # 数据模型
│   │   │   │   │       ├── LoginRequest.java
│   │   │   │   │       ├── LoginResponse.java
│   │   │   │   │       └── RegisterRequest.java
│   │   │   │   ├── utils/
│   │   │   │   │   └── TokenManager.java   # 用户Token管理
│   │   │   │   ├── MainActivity.java       # 主Activity
│   │   │   │   ├── LoginActivity.java      # 登录页面
│   │   │   │   ├── RegisterActivity.java   # 注册页面
│   │   │   │   ├── SettingsActivity.java   # 设置页面
│   │   │   │   ├── ChatFragment.java       # 对话Fragment
│   │   │   │   ├── HistoryFragment.java    # 历史记录Fragment
│   │   │   │   ├── ProfileFragment.java    # 个人中心Fragment
│   │   │   │   ├── LanguageManager.java    # 语言管理器
│   │   │   │   ├── ThemeManager.java       # 主题管理器
│   │   │   │   ├── TtsManager.java         # 藏文语音合成
│   │   │   │   ├── RealtimeTtsManager.java # 实时语音合成
│   │   │   │   ├── SpeechRecognitionManager.java # 语音识别
│   │   │   │   ├── ChatHistory.java        # 对话历史数据模型
│   │   │   │   ├── ChatHistoryManager.java # 对话历史管理
│   │   │   │   ├── ChatHistoryAdapter.java # 历史列表适配器
│   │   │   │   ├── VoiceWaveformView.java  # 语音波形视图
│   │   │   │   ├── TibetanFontHelper.java  # 藏文字体辅助
│   │   │   │   └── NetworkDiagnostic.java  # 网络诊断
│   │   │   ├── res/
│   │   │   │   ├── layout/                 # 布局文件
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_login.xml      # 藏文登录
│   │   │   │   │   ├── activity_login_zh.xml   # 中文登录
│   │   │   │   │   ├── activity_login_en.xml   # 英文登录
│   │   │   │   │   ├── activity_register.xml
│   │   │   │   │   ├── activity_settings.xml
│   │   │   │   │   ├── fragment_chat.xml
│   │   │   │   │   ├── fragment_history.xml
│   │   │   │   │   ├── fragment_profile.xml
│   │   │   │   │   └── ...
│   │   │   │   ├── drawable/               # 图形资源
│   │   │   │   ├── values/                 # 值资源
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── themes.xml
│   │   │   │   │   └── attrs.xml
│   │   │   │   ├── color/                  # 颜色状态列表
│   │   │   │   └── xml/                    # XML配置
│   │   │   ├── assets/                     # 资源文件
│   │   │   └── AndroidManifest.xml         # 清单文件
│   │   ├── androidTest/                    # Android测试
│   │   └── test/                           # 单元测试
│   └── build.gradle                        # 模块构建配置
├── build.gradle                            # 项目构建配置
├── settings.gradle                         # 项目设置
└── gradlew / gradlew.bat                   # Gradle包装器
```

---

## 核心模块说明

### 1. 网络请求模块 (`api/`)

| 文件 | 说明 |
|------|------|
| `ApiService.java` | Retrofit API接口定义，包含登录、注册等接口 |
| `RetrofitClient.java` | Retrofit单例客户端，配置了日志拦截器和超时设置 |
| `BaseResponse.java` | 通用响应包装类 |
| `LoginRequest/Response.java` | 登录请求/响应数据模型 |
| `RegisterRequest.java` | 注册请求数据模型 |

**API配置**:
- 基础URL: `http://222.19.82.142:8101/api/`
- 连接超时: 30秒
- 读写超时: 30秒

### 2. 用户认证模块

| 文件 | 说明 |
|------|------|
| `LoginActivity.java` | 登录页面，支持账号/验证码登录、游客模式 |
| `RegisterActivity.java` | 用户注册页面 |
| `TokenManager.java` | 用户Token和信息的本地存储管理 |

### 3. 主要功能模块

| 文件 | 说明 |
|------|------|
| `MainActivity.java` | 主Activity，管理Fragment切换和语言切换 |
| `ChatFragment.java` | 对话界面，实现AI对话、语音输入等功能 |
| `HistoryFragment.java` | 对话历史记录列表 |
| `ProfileFragment.java` | 个人中心，语言切换、设置入口 |
| `SettingsActivity.java` | 设置页面 |

### 4. 语音模块

| 文件 | 说明 |
|------|------|
| `TtsManager.java` | 藏文语音合成，支持长文本分段合成，WebSocket流式传输 |
| `RealtimeTtsManager.java` | 实时语音合成 |
| `SpeechRecognitionManager.java` | 藏语语音识别，录音和识别请求处理 |

**TTS配置**:
- WebSocket URL: `ws://117.68.88.175:38086/tts/v1/streaming`
- 采样率: 24000Hz
- 最大分段: 500字符

**语音识别配置**:
- API URL: `http://222.19.82.143:7100/api/v1`
- 语言类型: `bo-CN` (藏语)
- 采样率: 16000Hz

### 5. 辅助模块

| 文件 | 说明 |
|------|------|
| `LanguageManager.java` | 多语言管理，支持藏文/中文/英文 |
| `ThemeManager.java` | 主题管理，支持亮色/暗色主题 |
| `TibetanFontHelper.java` | 藏文字体辅助类 |
| `VoiceWaveformView.java` | 自定义语音波形显示视图 |
| `ChatHistory.java` | 对话历史数据模型 |
| `ChatHistoryManager.java` | 对话历史本地存储管理 |
| `ChatHistoryAdapter.java` | 历史记录列表适配器 |
| `NetworkDiagnostic.java` | 网络诊断工具 |

---

## 多语言支持

项目支持三种语言，通过 `LanguageManager` 管理：

| 语言代码 | 语言名称 | 默认 |
|---------|---------|------|
| `bo` | 藏文 (བོད་ཡིག) | ✓ |
| `zh` | 中文 | |
| `en` | English | |

**布局文件命名规则**:
- 默认/藏文: `activity_login.xml`
- 中文: `activity_login_zh.xml`
- 英文: `activity_login_en.xml`

---

## 权限配置

```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 媒体权限 -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

<!-- 语音输入权限 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

---

## 依赖库

```gradle
// AndroidX
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.9.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.cardview:cardview:1.0.0'

// 网络请求
implementation 'com.squareup.okhttp3:okhttp:4.11.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// JSON
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'org.json:json:20231013'

// Markdown渲染
implementation 'io.noties.markwon:core:4.6.2'
```

---

## 构建配置

```gradle
android {
    namespace 'com.example.myapplication'
    compileSdk 33

    defaultConfig {
        applicationId "com.example.sunshine"
        minSdk 21
        targetSdk 33
        versionCode 3
        versionName "1.2"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

---

## 颜色方案

| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| `login_background` | #F8F5F0 | 登录页背景 |
| `login_primary_green` | #1F8A4F | 主按钮颜色 |
| `login_tab_green` | #28A85A | 标签绿色 |
| `login_orange` | #FF9800 | 强调色/橙色标签 |
| `login_link_blue` | #2D8CF0 | 链接颜色 |
| `login_text_primary` | #333333 | 主文本颜色 |
| `login_text_secondary` | #666666 | 次要文本颜色 |
| `login_text_hint` | #999999 | 提示文本颜色 |

---

## 启动项目

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK 33
- Gradle 8.x

### 构建步骤

1. **使用 Android Studio (推荐)**
   - 打开 Android Studio
   - 选择 File → Open
   - 选择项目根目录
   - 等待 Gradle 同步完成
   - 点击运行按钮

2. **命令行构建**
   ```bash
   # 构建 Debug APK
   ./gradlew assembleDebug

   # 安装到设备
   ./gradlew installDebug

   # 构建 Release APK
   ./gradlew assembleRelease
   ```

### 输出文件位置
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

---

## API 接口说明

### 用户登录
```
POST /api/user/login
Request: { "account": "用户名", "password": "密码" }
Response: { "code": 0, "data": { "token": "...", "userName": "..." } }
```

### 用户注册
```
POST /api/user/add
Request: { "userAccount": "账号", "password": "密码", "userName": "昵称" }
Response: { "code": 0, "data": userId }
```

---

## 功能特性

### 已实现功能
- ✅ 用户登录/注册
- ✅ 游客模式访问
- ✅ 多语言支持 (藏文/中文/英文)
- ✅ AI 对话功能
- ✅ 藏语语音识别
- ✅ 藏文语音合成 (TTS)
- ✅ 对话历史管理
- ✅ 深色/浅色主题
- ✅ Markdown 消息渲染

### 待开发功能
- ⏳ 验证码登录
- ⏳ 忘记密码
- ⏳ 升级计划
- ⏳ 退出登录

---

## 开发团队

**版权所有**: 西藏大学

---

## 更新日志

### v1.2
- 新增全新登录界面设计
- 支持账号/验证码切换登录
- 新增游客访问模式
- 优化多语言切换体验

### v1.1
- 添加语音识别功能
- 添加语音合成功能
- 优化对话界面

### v1.0
- 初始版本发布
- 基础对话功能
- 用户认证系统