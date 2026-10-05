# 开发与构建

先读agent.md、REQUIREMENTS.md、HANDOFF.md。已验收main为0.1.2/code3，功能分支 `feat/ledger-ui-v0.1.3` 当前制作 **0.1.4/code6／V1.0 S1**；本轮只改变录入、导航与状态，保持schema1、format1、整数分、既有依赖及权限。实际结果见 [VALIDATION_V014](VALIDATION_V014.md)，技术测试不能代替 [手机验收](PHONE_ACCEPTANCE_V014.md)。

## 当前 Windows 构建路径

既有官方JDK和Android SDK保存在忽略的 `.tooling`，配置为 `.tooling/toolchain.json`。脚本仅设置当前进程变量并在结束时恢复，不修改系统PATH。本轮JDK21.0.12.1+1、Platform35／Build-Tools35.0.0，固定Gradle8.11.1。

在项目根目录的PowerShell中执行：

```powershell
& .\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest')
```

依次检查JVM／SQLite／备份逻辑、Android lint，并构建应用与设备测试APK。成功信号为 `BUILD SUCCESSFUL`；应用包位于 `app/build/outputs/apk/debug/app-debug.apk`，日志位于忽略的test-artifacts。要启动新PowerShell进程传多个任务，用 `-Command` 保留数组，不能用 `-File` 拆成额外位置参数：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -Command "& .\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest')"
```

失败时先看最后一份日志中第一处FAILURE／failed，处理报错后再运行同一命令，不删除真实数据库或备份。记录任务实际执行与UP-TO-DATE，后者不能写成重跑全部测试。SDK XML3／4已有兼容告警和lint9项提示保留，不盲目更新工具链来隐藏诊断。

## 新机器

安装官方 [Android Studio](https://developer.android.com/studio/install)，通过SDK Manager安装Platform35与Build-Tools35.0.0，再打开项目；使用兼容JDK17或21。命令行使用新机器自己的路径：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1 -JavaHomePath '你的JDK目录' -SdkRootPath '你的Android SDK目录'
```

账本、工具、缓存、APK和签名密钥不提交Git。

## 当前 CI 设备和升级

工作流监听main、feat/**、codex/**。界面测试仅在含合成账目的临时API35 QEMU／ci-ledger AVD运行；夹具会清理测试数据库，三字号用例会暂改尺寸、密度及字体并在finally还原。禁止在作者手机运行仪器测试命令。CI-only参数与QEMU／AVD guard在配置、截图及升级操作之前执行。

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.ledgerCi=true \
  -Pandroid.testInstrumentationRunnerArguments.notClass=com.xiaoliumiaomiao.anjizhang.LedgerUpgradeTest --no-daemon
```

当前connected共35项：Smoke6／Query4／Flow3／LargeText1／Recovery2／InputFeedback3／S1 16。恢复测试模拟保存Compose状态再创建新VM，未真实杀Android进程。三字号精确360dp viewport、按钮几何和13键输入值均保留严格断言，截图与XML／runner必须结合读取。

升级基线固定已验收 `ff8479f75cd917bc49e5ab2173ad7e9906dc480c`，在baseline-v012构建0.1.2/code3。connected结束后，同job临时debug证书依次安装旧版、seed、`install -r` 新版0.1.4/code6、verify、启动；seed到verify之间没有清库、卸载、再次connected、重seed或降级。每方法用 `am instrument -w -r`，唯一OK(1 test)与completion=-1同时成立才通过，见 [官方参数](https://developer.android.com/studio/test/command-line)。

最终 [CI 37301576955](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37301576955) 在 `3bf514249f6a920d2c3d8f89299830f72520eb82` 上真实81 JVM／35 UI及两个升级方法全部通过。原始日志、XML、原生PNG、安装版本和公开证书证据保存在忽略的 `test-artifacts/sdd-v1-s1/ci-s1-final-37301576955/`，未执行的检查不预填完成。后续仅文档提交不改变已受测源码，须核对diff及本机／远端／草稿PR HEAD同步。

本机包由同旧debug证书构建并原生审计，再以新文件名复制到两output；旧物hash保护。作者先导出备份，再正常覆盖安装，CI签名不能替代本机同签名包。卸载会删除私有账本；代码回退保留分支及作者改动，不用reset --hard清工作区，也不以强制降级作为账本回退方式。

0.1.3／输入修复／0.1.2／0.1.1历史实际路径与结果分别见 [VALIDATION_V013](VALIDATION_V013.md)、[VALIDATION_INPUT_FEEDBACK](VALIDATION_INPUT_FEEDBACK.md)、[VALIDATION_V012](VALIDATION_V012.md)、[VALIDATION_V011](VALIDATION_V011.md)，旧APK、校验及作者验收记录保留。
