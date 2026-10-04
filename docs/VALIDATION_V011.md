# 0.1.1 验证记录

技术验证日期：2026-10-04（Asia/Shanghai）。作者于2026-10-05确认0.1.1手机升级验收“经验证全部通过”，PHONE_ACCEPTANCE_V011.md据本次确认勾选；手机型号和Android版本未提供。下方待验收字句属于2026-10-04技术交付的历史状态，本次作者确认已解除该门禁。

## 范围与基线

作者确认本轮为备份容量提示、JSON边界回归、覆盖升级保留账目。已验收基线57799ca1751a98e801c091f1cd6cc69f50cb4c77已快进合入main，PR #1已merged。开发在附到本任务的隔离工作区feat/ledger-hardening-v0.1.1。复用决定见REUSE_V011.md；不增加依赖或权限，不改SQLite schema1/备份version1。

## 当前本机证据

隔离基线`build-20261004-225042.log`实际执行30项JVM测试并成功。新增6项边界回归先在既有解析器上运行，`build-20261004-225336.log`实读36项/0失败/0错误/0跳过；这是有效GREEN基线，没有伪造RED或为制造失败破坏生产代码。覆盖根/记录重复字段、Unicode转义同名、5层与6层note数组的不同拒绝原因、50000完整Entry成功及50001记录条数拒绝；两种大批次均小于10MiB。

Task1实现69acd94后的综合命令：

```powershell
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest'
```

`build-20261004-225413.log`退出0，36项JVM/0失败/0错误/0跳过，lint0错误/9提示，应用及设备测试APK均编译成功。9条为8条固定依赖版本提示、1条现有UseKtx建议；SDK XML和未剥离符号提示已记录，未通过关检查或盲目升级隐藏。设置页公开双容量限制及版本0.1.1，新增真实滚动页面断言，6项原设备行为完整保留。这次本机构建本身不能证明设备执行和升级成功，后续实际结果见下。

## 升级技术验证与最终APK

首次真实[CI 37211853745](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37211853745)在6ed67e5运行：build成功，下载并实读XML为36项JVM和6项原界面测试全部通过，0失败/0错误/0跳过；新容量文案及版本页面断言通过。旧APK、test APK安装成功，seed在真实0.1.0/code1上通过，重开库后的三条合成记录全部字段及两个月统计正确。升级链随后停止于脚本错误`seed reported multiple or missing runner completion codes`，尚未覆盖安装新版或执行verify，不能称为升级通过。根因是命令只带`-w`，默认展示格式没有原始`INSTRUMENTATION_CODE`；按[Android官方参数说明](https://developer.android.com/studio/test/command-line)需显式加`-r`。首次模拟探针未模拟这项协议差异，补充相应RED/GREEN检查后再执行完整真实CI；生产APK不变。原始证据保存在`test-artifacts/ci-37211853745/android-build`和`android-device`。

本轮在临时API35模拟器中按旧APK→seed合成账目→同签名install-r新APK→verify执行。现有清库smoke先完成，升级链中不卸载、不清库。比较全部Entry字段、条数、跨月精确统计及真实安装版本1→2；两个阶段runner结果和完整日志保留。

新增升级fixture在`build-20261004-230035.log`实际compileDebugAndroidTestKotlin及package成功；生产APK未改变。JVM/lint任务复用Task1通过结果，实读XML仍36项/0失败/0错误/0跳过，lint0错误/9提示；没有把UP-TO-DATE称为重新执行36项。新脚本及4个工作流run块Bash-n通过，13个合成控制流探针通过，包含LF/CRLF正常、ADB退出0但runner失败/崩溃/缺失或多计数、错误completion、安装退出码23保留、证书不一致、多设备、非QEMU拒绝；这些探针不是实际安卓升级。

独立APK审计和主agent最终复制核对通过。最终本机APK与冻结候选完全同SHA256、9408857字节，包名com.xiaoliumiaomiao.anjizhang/code2/name0.1.1/min26/target35。aapt确认无INTERNET/广泛存储权限，仅应用内部signature权限；allowBackup=false及云/设备迁移各域排除；apksigner验证v2有效。新旧本机APK公开证书SHA256同为`209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d`，没有读取或上传本机私钥。3份assets许可与源码、third_party逐文件hash相同；平台checkout换行不影响完整许可保留，不要求不同机器生成的APK字节相同。

新包分别保存在本隔离工作区和原项目的`output/an-jizhang-0.1.1-debug.apk`，复制前后SHA256一致，异哈希目标会拒绝覆盖；0.1.0旧包及旧校验行保留。最终新包：

```text
81a92bf36f1867cab1aa2df9b318b5d42e18e16dd65eeb864209f6bd0e8d7448  an-jizhang-0.1.1-debug.apk
```

本机候选与最终实际核验输出在忽略的test-artifacts/sdd-v011/apk-audit.md、audit-*.txt、audit-licenses.json。CI在同一job构建旧/新包，用该job自身临时debug证书并实际比较；不同机器或CI运行的证书可能不同，不能用CI包替代本机包覆盖作者旧应用。

## 独立审查与版本控制

Task1及Task2代码独立规格/质量审查均Approved，全分支静态审查亦完成；首次审查未发现的raw/pretty协议问题由真实CI暴露并已修复，历史报告保留。19711990718adb5bee717db64e8aa76d2ccfbb09仅在共用runner命令添加-r；参数敏感探针先用真实pretty文本复现RED，修正后13项GREEN，再加有-r但缺completion负例形成14项GREEN。两个方法均带-r、缺码拒绝和安装退出23保留均有实际mock证据；脚本/探针Bash-n及diff检查通过，独立增量规格/质量Approved，无未解决Critical/Important。未把mock称为实际升级。

[PR #2](https://github.com/xiaoliumiaomiao/an-jizhang/pull/2)已建立并附到本任务，保持草稿测试交付。最终交付使用新文件名，保留0.1.0旧APK和已验收基线；PHONE_ACCEPTANCE_V011.md不预勾。固定依赖/lint及SDK提示继续记录。

## 最终真实CI证据

[CI 37213110853](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37213110853)测试提交`19711990718adb5bee717db64e8aa76d2ccfbb09`，2026-10-04 23:28:07开始、23:39:09结束（Asia/Shanghai）；build/device-smoke两个job均success。主agent下载并实读产物和完整日志：

- JVM六份XML合计36项，0失败/0错误/0跳过；lint XML仅9项Warning，0 Error。
- Android设备XML实际6项，0失败/0错误/0跳过。包括原运行中Activity重建、失败保留输入等行为；设置页容量文案和0.1.1版本通过实际滚动显示断言。
- 旧APK实际运行0.1.0/code1，新APK实际运行0.1.1/code2；旧/新/test三包apksigner公开证书均`41d7b6082546c6ab0cde5bc90543e6e281ece527320ede1ee50e9166d51f3505`。这是CI该job临时证书，与本机作者覆盖安装包证书不同。
- seed和verify分别实际1项，各有唯一`OK (1 test)`及`INSTRUMENTATION_CODE: -1`，无失败或跳过。新版verify不写入，打开及重开既有schema1数据库时，三条Entry全部字段/条数一致。10月收入10000分、支出1分、结余9999分；9月支出1234分、结余-1234分，分类汇总一致；中文、引号及换行备注保留。
- 覆盖安装`Success`，启动MainActivity `Status: ok`，Upgrade和Device validation exit均0。主agent查看升级后device-screen.png，中文首页显示2026年10月收入100.00、支出0.01、结余99.99及原收入/自定义分类记录；截图证明这次启动状态，不替代数据库字段或手机验收。

原始证据保存在忽略的`test-artifacts/ci-37213110853/android-build`、`android-device/upgrade-artifacts`、设备XML及`run.log/run.json`；独立解析计数在`jvm-summary.json/ui-summary.json/upgrade-summary.json`。代码测试HEAD为1971199；生产APK来源仍69acd94，此后只改测试/CI和文档，最终本机APK哈希不变。文档收尾不重复运行无变化的应用测试，最终远端同步证据另记录。

2026-10-04 23:44（Asia/Shanghai）实际核对：交付文档提交`2baed55a1db3b4bbc0655df92f2f961d4486558c`已推送，GitHub仍private=true，本机HEAD、远端功能分支HEAD、草稿PR #2 HEAD完全一致，工作区干净。1971199之后仅README/docs变更；最终独立规格、质量及交付文档审查全部Approved，0新增Critical/Important/Minor。原始同步快照`test-artifacts/sdd-v011/delivery-sync-first.json`。收尾仅补本段、交接同步说明及计划Step6勾选；推送后再次核对的最新HEAD、私有状态、干净工作区及新旧APK哈希保存在忽略的`final-sync.json`，不追溯预填作者手机验收。
