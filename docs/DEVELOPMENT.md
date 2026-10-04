# 开发与构建

当前仓库使用功能分支开发，账本、工具、缓存、APK及签名密钥不会提交Git。先读 agent.md、REQUIREMENTS.md、HANDOFF.md。

当前功能分支feat/ledger-query-v0.1.2为0.1.2/code3，完整技术测试通过，本机同签名APK已复制到output，作者手机验收待执行。新增所选月搜索/筛选仅影响列表，schema1、备份format1、依赖及权限保持原样。实际进度见[VALIDATION_V012](VALIDATION_V012.md)，技术测试不能替代[本轮手机验收](PHONE_ACCEPTANCE_V012.md)。

## 这台 Windows 的一条构建路径

本次会话下载的官方 JDK 与 Android SDK 保存在 `.tooling`；`.tooling/toolchain.json` 记录版本、路径、来源和校验值。脚本读取配置，仅设置当前进程变量，结束恢复原值，不改系统 PATH。

在项目根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

命令依次运行单元/SQLite测试、Android lint、debug APK编译。成功信号：`BUILD SUCCESSFUL`；APK位于 `app/build/outputs/apk/debug/app-debug.apk`，日志位于忽略的 `test-artifacts`。

如失败，先查看最后一个日志中的第一处 `FAILURE` / `failed`，按报错修复后重复同一条命令。不要删除数据库或真实备份来解决构建问题。

## 新机器

先安装官方 Android Studio 并在 SDK Manager 安装 Android SDK Platform 35、Build-Tools 35.0.0，然后在 Android Studio 打开项目。使用兼容的 JDK17或21；本次验证用的具体版本见 VALIDATION.md。IDE会用提交的 Gradle Wrapper 获取校验过的Gradle8.11.1。

如果需要命令行，传入机器现有工具路径，不复制本机绝对路径配置：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1 -JavaHomePath '你的JDK目录' -SdkRootPath '你的Android SDK目录'
```

官方安装说明：[Android Studio](https://developer.android.com/studio/install)。

## 测试与恢复边界

- JVM/domain：金额输入和统计；Robolectric：真实Android SQLite及JSON行为。两者不能替代手机验收。
- GitHub Actions：执行编译/lint/tests以及Android35模拟器测试；只有运行报告成功才能标为通过。
- 本地输出是debug测试APK。正式发布需要单独签名、版本策略与作者授权。
- 回退代码可保留当前分支，再切回此前提交；不要用 `git reset --hard` 清除未保存工作。
- 应用升级保留数据依赖签名及数据库迁移。卸载会删除账本，操作前先在应用中导出备份；备份是明文，选可信位置保管。

## 0.1.2当前CI设备与升级路径

当前工作流在仅含合成数据的API35临时模拟器执行原6项LedgerSmokeTest与新增4项LedgerQueryTest，共10项UI行为。它们会清理测试数据库，禁止在作者手机上运行这条仪器测试命令；升级fixture的两个方法要求不同真实安装版本，因此由后面的专门链分别执行。最终CI37220141121在0076b6b代码上实读10/0/0/0（四Query、六Smoke），详见VALIDATION_V012.md。CI的UI命令为：

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.notClass=com.xiaoliumiaomiao.anjizhang.LedgerUpgradeTest --no-daemon
```

工作流在同job从固定已验收基线`976817290302e712519297cc5b810ce019d2d383`构建0.1.1/code2旧APK，使用baseline-v011目录；先完成上述10项UI，再用同一job临时debug证书执行旧版seed、install-r 0.1.2/code3、verify。旧→新链中禁止清库、卸载、重seed、再次connected或降级。verify-upgrade.sh限定CI=true、唯一QEMU emulator且AVD名ci-ledger，三包公共证书相同，并严格检查两个runner结果。最终CI37220141121实际seed/verify各一项通过，原始runner唯一OK和completion=-1，真实字段/跨月统计/安装启动均已核验；首次37218934264失败与诊断历史保留，不用fixture编译或语法检查替代实际升级。

作者手机在主agent技术交付新包后按[PHONE_ACCEPTANCE_V012](PHONE_ACCEPTANCE_V012.md)执行正常覆盖安装与操作验收，先在0.1.1中导出备份。CI包签名与本机可能不同，不能用来覆盖作者已安装应用。APK哈希、公开证书、runner原始/规范化输出和截图保存在本轮android-device产物；真实手机结果单独记录。

脚本单方法测试使用`am instrument -w -r`：`-w`等待完成，`-r`提供原始runner协议；默认展示格式可能只有`OK (1 test)`而没有completion code。脚本要求两者同时成功，防止ADB退出0掩盖测试失败，参见[官方参数说明](https://developer.android.com/studio/test/command-line)。

本机最终构建使用`.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest'`；记录哪些任务实际执行，不把UP-TO-DATE写成重跑测试。本轮19秒构建沿用已通过的50项JVM结果，APK哈希与此前候选审计一致；两处output新文件已复制并验证SHA，旧APK和旧校验前缀原字节保留。SDK XML v4/v3、SQLite library already loaded及lint9项提示如实保留，不通过安装新工具或盲目升级依赖隐藏诊断。

0.1.1历史路径使用固定57799ca基线、6项smoke及0.1.0→0.1.1升级，真实结果见[VALIDATION_V011](VALIDATION_V011.md)，作者已确认全部通过。0.1.0证据仍在[VALIDATION](VALIDATION.md)；旧APK、旧校验行和验收表继续保留。
