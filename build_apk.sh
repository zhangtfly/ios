#!/bin/bash

echo "正在构建APK安装包..."
echo

# 清理之前的构建
echo "1. 清理项目..."
./gradlew clean

# 构建APK
echo "2. 构建APK..."
./gradlew assembleDebug

# 检查构建结果
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo
    echo "✅ APK构建成功！"
    echo "📁 文件位置: app/build/outputs/apk/debug/app-debug.apk"
    echo
    echo "📱 安装方法："
    echo "1. 将APK文件发送给其他人"
    echo "2. 在手机上启用"未知来源"安装"
    echo "3. 点击APK文件进行安装"
    echo
else
    echo
    echo "❌ APK构建失败！"
    echo "请检查错误信息并重试"
    echo
fi


















































































