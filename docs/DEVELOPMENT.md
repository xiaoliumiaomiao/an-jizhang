# 开发与构建

先读agent.md、REQUIREMENTS.md、HANDOFF.md。0.1.4/code6已由作者于2026-10-07验收，PR4实际合并；远端main为`294479fb0ff4c039e82e89842ab616fc025894c7`。主目录仍保留作者两份未提交文档及旧main，不覆盖或强制同步。隔离分支`codex/ledger-statistics-v0.1.5`制作**0.1.5/code7／V1.0 S2**，保持schema1、format1、整数分、依赖、权限和VM操作协议。验证见 [VALIDATION_V015](VALIDATION_V015.md)，作者手机结果单独见 [PHONE_ACCEPTANCE_V015](PHONE_ACCEPTANCE_V015.md)。

## 当前 Windows 构建路径

官方JDK和Android SDK保存在ignored `.tooling`，配置为`.tooling/toolchain.json`。脚本仅修改当前进程变量并恢复，不改系统PATH。当前JDK21.0.12.1+1、Platform35／Build-Tools35.0.0，固定Gradle8.11.1。

项目根目录PowerShell执行：

```powershell
& .\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest')
```

检查JVM／SQLite／备份／统计逻辑、lint并构建应用与设备测试APK。成功信号为`BUILD SUCCESSFUL`；APK位于`app/build/outputs/apk/debug/app-debug.apk`，日志位于ignored test-artifacts。新PowerShell进程多任务使用`-Command`保持数组，不能用`-File`把任务拆成位置参数：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -Command "& .\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest')"
```

失败时读取最后日志第一处FAILURE／failed，处理后复跑同一命令，不删除真实账本。记录实际执行与UP-TO-DATE，后者不称重跑全部测试。SDK XML3／4兼容告警及lint的8 GradleDependency、1 UseKtx既有提示保留，不为隐藏告警扩大依赖或工具链升级范围。

## 新机器

安装官方 [Android Studio](https://developer.android.com/studio/install)，用SDK Manager安装Platform35和Build-Tools35.0.0，再打开项目；用兼容JDK17或21。命令行使用本机路径：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1 -JavaHomePath '你的JDK目录' -SdkRootPath '你的Android SDK目录'
```

真实账本、工具、缓存、APK和签名密钥不提交Git。

## 当前 CI 设备和升级

冻结源码`7be948c40c96e7fe0d75448e284ff52ee8e45c03`／0.1.5/code7已获Source增量和candidateNative独审Approved。本机四任务build实际111 JVM全0、lint0E9W，应用与测试APK已打包；候选本机包9,834,364 bytes／SHA256 `072afe980ecdbeaae398c4630d49ecc7a6374ef465e013b05d31b998aeb43c46`。新 [CI37626498602 attempt1](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37626498602) 两job success：111 JVM／55 UI均0F0E0S、原81／35缺失0、lint0E9W；seed／verify各1通过。60张新原PNG独立审阅给Runtime／Upgrade／Visual Approved C0/I0/M0，三字号MAX钻取完整单行、RV-M1关闭。完整证据与旧失败过程见 [VALIDATION_V015](VALIDATION_V015.md)。

工作流监听main、feat/**、codex/**。常规55项UI由原35与新S2 20组成，只在合成账目的临时API35 QEMU／ci-ledger AVD运行；仪器测试会清理夹具库，禁止在作者手机运行。`ledgerCi`、QEMU和AVD guard在配置、截图、升级之前检查。三字号场景暂改尺寸／密度／字体并在finally还原；新的钻取单行断言已在7be本次设备三字号方法中实际执行并通过，旧原图的折行只构成人工视觉失败证据，不是该新断言的自动RED。

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.ledgerCi=true \
  -Pandroid.testInstrumentationRunnerArguments.notClass=com.xiaoliumiaomiao.anjizhang.LedgerUpgradeTest --no-daemon
```

升级基线固定已验收`c476eac220ba3f062200365671a674e5decfd06d`的0.1.4/code6。常规connected成功后，同job临时debug证书依次安装旧版、seed、`install -r`新版0.1.5/code7、verify并启动；seed至verify之间不清库、卸载或重seed。每次`am instrument -w -r`须唯一`OK (1 test)`、completion=-1和exit0。原81 JVM／35 UI名称与业务断言保留，旧测试有等待真实读取／滚动与输入焦点的最小兼容适配，未声称旧测试逐字节不变。

同源码本机APK与独立校验、59张成功原生业务PNG已复制到两处output，`delivery.json`记录120个新副本hash均与来源一致，107旧物、16合同与作者主目录文档原字节保持。文档提交cfa0594已实核整分支diff-check exit0与local／remote／PR一致、clean／private／draft；独立技术FullDelivery Approved C0/I0/M1（仅既有工具提示）。批准写回后的docs-only提交身份见ignored delivery-sync-final.json。作者先导出0.1.4备份，再正常覆盖安装本机同签名0.1.5；CI临时证书不代表本机或用户手机签名。手机20项待作者，S2验收前不合并，S3及正式发布另行确认。
