@echo off
echo 正在构建发布版APK安装包...
echo.

REM 清理之前的构建
echo 1. 清理项目...
call gradlew clean

REM 构建发布版APK
echo 2. 构建发布版APK...
call gradlew assembleRelease

REM 检查构建结果
if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    echo.
    echo ✅ 发布版APK构建成功！
    echo 📁 文件位置: app\build\outputs\apk\release\app-release-unsigned.apk
    echo.
    echo ⚠️  注意：这是未签名的APK，需要签名才能安装
    echo.
    echo 📱 签名方法：
    echo 1. 使用Android Studio的签名工具
    echo 2. 或者使用debug版本（已自动签名）
    echo.
    echo 🔄 正在构建debug版本（可直接安装）...
    call gradlew assembleDebug
    
    if exist "app\build\outputs\apk\debug\cn.edu.utibet.sunshineglm.apk" (
        echo.
        echo ✅ Debug版APK构建成功！
        echo 📁 文件位置: app\build\outputs\apk\debug\cn.edu.utibet.sunshineglm.apk
        echo 📱 这个版本可以直接安装使用
    )
    echo.
    pause
) else (
    echo.
    echo ❌ APK构建失败！
    echo 请检查错误信息并重试
    echo.
    pause
)


















































































