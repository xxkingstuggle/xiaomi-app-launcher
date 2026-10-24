# Android 应用启动器

一个原生 Android 应用启动器：读取设备上可启动的应用，支持名称搜索、收藏、最近打开排序和按名称排序。数据保存在设备本地。

## 构建

需要 JDK 17 和 Android SDK。仓库使用 Gradle Wrapper：

    ./gradlew assembleDebug

应用的最低 SDK 为 26，当前 compileSdk 与 targetSdk 为 36。应用 ID 当前配置为 com.tencent.mobileqq；更改它会影响应用身份和升级安装。

## 主要目录

- app/src/main/java/：Compose 界面、ViewModel、应用查询与偏好存储
- app/src/main/res/：图标、主题与字符串资源
- app/src/test/：本地 JVM 单元测试
