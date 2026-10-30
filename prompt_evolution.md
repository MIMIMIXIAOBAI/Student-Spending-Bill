# Prompt迭代记录

【序号】1
【时间】2026-10-30
【任务名称】工程初始化与基础配置
【完成情况】已完成：创建纯 Java Android 工程（namespace com.campusbill.app，minSdk 29，compileSdk 37，targetSdk 36），开启 ViewBinding；引入 Room 2.6.1（annotationProcessor 方式）、OpenCSV 5.9、MPAndroidChart v3.1.0；配置浅色简约主题与颜色/尺寸/字符串资源；清单仅保留启动 Activity，无网络与存储权限。assembleDebug 构建通过。补充说明：本机全局 Gradle 镜像脚本会清空仓库列表，故在 settings.gradle 内追加 jitpack 仓库以解析 MPAndroidChart。
【修改/新增文件】
- settings.gradle
- build.gradle
- gradle.properties
- local.properties（本机 SDK 路径，已加入 .gitignore）
- .gitignore
- gradlew、gradlew.bat、gradle/wrapper/gradle-wrapper.jar、gradle/wrapper/gradle-wrapper.properties
- app/build.gradle
- app/proguard-rules.pro
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/campusbill/app/MainActivity.java
- app/src/main/res/layout/activity_main.xml
- app/src/main/res/values/colors.xml
- app/src/main/res/values/strings.xml
- app/src/main/res/values/dimens.xml
- app/src/main/res/values/themes.xml
- app/src/main/res/drawable/ic_launcher_background.xml
- app/src/main/res/drawable/ic_launcher_foreground.xml
- app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
- prompt_evolution.md
