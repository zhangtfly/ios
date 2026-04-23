@echo off
echo 正在构建APK安装包...
echo.

REM 清理之前的构建
echo 1. 清理项目...
call gradlew clean

REM 构建APK
echo 2. 构建APK...
call gradlew assembleDebug

REM 检查构建结果
if exist "app\build\outputs\apk\debug\cn.edu.utibet.sunshineglm.apk" (
    echo.
    echo ✅ APK构建成功！
    echo 📁 文件位置: app\build\outputs\apk\debug\cn.edu.utibet.sunshineglm.apk
    echo.
    echo 📱 安装方法：
    echo 1. 将APK文件发送给其他人
    echo 2. 在手机上启用"未知来源"安装
    echo 3. 点击APK文件进行安装
    echo.
    pause
) else (
    echo.
    echo ❌ APK构建失败！
    echo 请检查错误信息并重试
    echo.
    pause
)


















































































