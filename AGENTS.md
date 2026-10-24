# 项目指引

## 项目定位
本机 Android 启动器，用于搜索、收藏和打开已安装的应用。

## 如何运行
使用 JDK 17 与 Android SDK；构建命令为 ./gradlew assembleDebug。

## 技术栈
Kotlin、Jetpack Compose、AndroidX DataStore、Gradle Kotlin DSL。

## 目录与约定
应用代码在 app/src/main/java/，资源在 app/src/main/res/，JVM 单元测试在 app/src/test/。

## 当前状态与下一步
applicationId 当前为 com.tencent.mobileqq，修改前先确认安装身份需求；应用列表行为需结合 Android 包可见性与设备版本核对。
