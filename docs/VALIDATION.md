# 首版验证记录

日期：2026-10-04（Asia/Shanghai）。技术交付验证完成，作者手机验收未执行。此记录以实际工具输出为依据。

最终 [CI 37191001385](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37191001385) 在提交 `c02c1ea9aa6a269de5742cacee236e26af46d717` 上两项job均成功：实际XML为30项JVM测试及6项Android15/API35设备测试，均0失败/0错误/0跳过；lint0错误/9提示。后续提交仅整理文档，不改此轮验证的应用、测试或CI。

## 本机工具与构建

JDK Temurin21.0.12.1+1；Android SDK35 revision2、Build Tools35.0.0；Gradle8.11.1；AGP8.9.2；Kotlin2.1.20；Compose BOM2025.04.01。官方工具下载路径、固定版本和校验值在忽略的`.tooling/toolchain.json`。

CI固定Google Linux命令行工具23.0/build16111833，按官方仓库元数据的SHA1校验通过。本次下载实际SHA256为`0877a1d048fe4a24efe2eff536ca4223f7adeb58648bb81909d33c446918cfa8`，记录在已保存的CI build日志；这不是未查证的“官方公布SHA256”声明。

最后一次应用实现验证命令：

```powershell
.\scripts\build.ps1 -Tasks ':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest'
```

`test-artifacts/build-20261004-154120.log`：BUILD SUCCESSFUL，30个JVM测试、0失败、0错误；lint0错误/9提示；应用debug和设备测试APK均生成。最终许可修改后的检查日志`build-20261004-154723.log`成功；代码未变化，测试任务复用已通过的输出，重新读取XML仍为30项/0失败/0错误。

- 金额/统计/实际SQLite/JSON：RobolectricSDK28及纯Kotlin，精确金额、编辑删除、重开持久化、备份往返、重复恢复、批末冲突回滚、格式与数据边界。
- ViewModel：读取失败后恢复、非法保存保留输入与重试、导出内容、重复恢复计数、冲突拒绝、最多MAX_BYTES+1字节、malformed UTF8、写出失败、稳定唯一操作完成ID。
- MainActivity：RobolectricSDK26真实启动，覆盖Android8.0最低版本的初始化。
- 设备测试：实际执行6项全通过，包含导航、0.01元支出、非法金额、输入重建、运行中保存/删除重建、失败保留草稿、旧结果不关闭新编辑器和自定义分类；系统文件选择器的手机交互仍待作者验收。

首轮核心测试真实RED为17项/16失败，再GREEN；长前导零补回归先18项/1失败再通过。界面首轮RED只证明缺少组件，较强的行为测试在实施期间补充，不声称所有行为都经历预实现RED。操作结果回归真实RED27项/1失败，再GREEN30项/0失败。

## 独立审查

- Task1规格/质量通过，无阻塞项。
- Task2首次发现运行中Activity重建后回调修改旧页面的Important；修为唯一操作ID与当前页面消费，复审通过，无阻塞项。
- 交付脚本审查通过；实际Windows PowerShell5.1测试调用成功。stderr与UTF8-BOM兼容修复有前后探针。
- 指定直接运行时依赖及复用资产供应链审查无阻塞发现，详情见SECURITY.md。
- 全分支独立审查：d7ecaf1至811e7bf，规格通过、质量Approved，0 Critical/0 Important。两个非阻塞Minor为整本备份容量提示及少数JSON边界定向测试，详见REVIEW.md。容量限制已补入README，测试建议保留为后续维护项。

## 最终APK检查与复制

应用包名com.xiaoliumiaomiao.anjizhang，versionName0.1.0，minSdk26，targetSdk35；aapt确认无INTERNET/广泛存储权限，仅AndroidX签名级内部receiver权限。merged Manifest allowBackup=false且带cloud/device-transfer排除规则。

apksigner验证最终debug APK v2签名有效；逐文件SHA256对比确认assets/licenses内完整MIT、Apache及NOTICE与源码一致。

已复制到`output/an-jizhang-0.1.0-debug.apk`，大小9835648字节；复制前后SHA256相同，已有异哈希文件会拒绝覆盖。`output/SHA256SUMS.txt`记录：

```text
6fb6bfebf726da15d4fa9756d3917253173e2e444710caaca62b7f677e748280  an-jizhang-0.1.0-debug.apk
```

实现/构建来源提交811e7bf7f4bed7236afe3bb619906f5af277a052；最终交付文档提交不改变此APK的实现。权限、签名及JSON证据在`test-artifacts/apk-verification-20261004-1547*`。APK和工具日志都被Git忽略。

## CI与设备

最终运行37191001385的build与device-smoke均success。主agent下载并直接读取`test-artifacts/ci-37191001385/android-build/test-results/testDebugUnitTest/TEST-*.xml`及`android-device/app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`，确认30项/6项均无失败、错误或跳过。设备6项逐项结果为：

- invalidAmountCannotSaveThenOneCentExpenseAppears：通过，13.326秒。
- emptyLedgerAndNavigation：通过，4.776秒。
- databaseSaveFailureAfterRecreationKeepsEditorInputs：通过，12.858秒。
- editorInputSurvivesActivityRecreation：通过，8.286秒。
- reselectingExpenseKeepsCustomCategory：通过，6.993秒。
- saveAndDeleteCompleteAfterActivityRecreationAndOldResultDoesNotCloseNewEditor：通过，17.691秒。

logcat记录save和delete均submitted/jobs1；保存/删除跨Activity使用同一VM13694755，真实同VM、结果消费和旧结果隔离断言均通过。整轮未跳过测试或吞掉失败。`run.log`记录connected任务BUILD SUCCESSFUL，随后重新安装本job APK并启动MainActivity，`Status: ok`、`Device validation exit: 0`。主agent查看`android-device/device-screen.png`，确认为新安装后中文安记账空账本首页，导航和记账入口可见；这是成功后启动截图，不冒称为所有交互瞬间截图。设备HTML/XML、完整运行日志及logcat均保存在该忽略的证据目录。

以下保留此前失败与修复的历史证据，不能替代上述最终实际结果。

基线 [37186536560](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37186536560) 环境准备失败：runner没有预装sdkmanager，退出127。已加入Google官方固定23.0命令行工具下载/摘要校验与SDK安装。

[37186675598](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37186675598) 的build job成功；实际下载报告确认30项JVM测试/0失败/0错误，lint0错误/9提示。设备准备长时间等待后由主agent取消以提取日志；实际Emulator37.2.12日志显示userdata要求12288MB，而runner可用8370.43MB，模拟器启动即退出，无超时ADB仍等待。设备测试未开始，取消不是设备测试通过。

c3bdb62仅修改CI：在RUNNER_TEMP新建4G数据分区AVD、关闭快照，加连接/ADB超时、PID检查和失败日志。实际Bash语法检查与独立追加审查通过。[37187393827](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37187393827)的build通过30项JVM/0失败/0错误、lint0错误/9提示；device在真实启动器版本查询时因缺少libpulse.so.0退出127，设备测试未开始。

90fcc0b继续仅修CI：固定Ubuntu24.04，临时runner安装Ubuntu签名软件源的libpulse0，采集运行库日志，构建和设备作业各自从同一提交编译并行验证。追加审查发现裸ldd与启动器库路径不同、可能误阻塞；已改为仅诊断，真实启动器退出码仍决定失败，复审通过。Bash语法和diff检查通过。[37187887902](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37187887902)的构建30项JVM通过、lint0错误/9提示；模拟器成功启动，实际6项设备测试3通过/3失败。

3项失败发生于首次输入前，没有金额/分类节点；另外3项含编辑器重建、一分钱保存与导航通过。源码证实初始加载在后台、入口busy时禁用，而旧测试直接点击。af801da仅修测试夹具，等待可点击且启用的入口，再点击并等待编辑器；全部业务/重建/Mutex断言保留，无固定sleep或跳过。初始竞态诊断需后续设备通过佐证，不从源代码假设直接标为修复成功。本机日志`build-20261004-161626.log`中设备测试重新编译成功，已有JVM/生产APK/lint输出复用。

0ee4634仅完善CI失败时logcat、截图和结果文件清单收集；Bash小范围探针及独立复审确认没有吞掉Gradle失败码。[37188539169](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37188539169)的build实际报告为30项JVM/0失败/0错误、lint0错误/9提示；设备XML实际6项/1失败/0错误/0跳过，即5通过、1等待超时。全部XML、HTML、逐项logcat及结果清单保存在`test-artifacts/ci-37188539169`。

该轮唯一失败位于LedgerSmokeTest.kt:82：保存释放Mutex后等待金额表单消失，尚未走到新editor/删除段。当时证据不足以确定是点击未提交还是操作结果消费，因此补充诊断；没有凭5项通过声称整个设备流程成功。应用及本机APK未变化。

本轮logcat真实记录Gradle测试结束后卸载主包和test包；EXIT截图是Launcher，不能作为失败瞬间的应用画面。c388e4c仅修成功测试后的交付截图：重新安装本job刚构建的debug APK，再带超时启动；独立只读审查Approved、0新增阻塞，Gradle失败仍直接退出，不被安装/截图掩盖。

48c136c增强设备提交等待及诊断：持有Mutex时确认点击后新增active协程（排除旧job），重建后重新获取当前Activity的ViewModel并assertSame，输出阶段/状态/语义树；原6项、全部业务断言和10秒弹窗关闭等待保留，失败记录后仍抛出。本机`build-20261004-163254.log`设备测试Kotlin/APK重新编译成功，生产输出未变。独立增量审查Approved、0新增阻塞。

[37189385205](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37189385205)构建实际30项JVM全过、lint0错误/9提示；设备XML实际6项/1失败/0错误/0跳过，保存删除重建测试通过。logcat确认save/delete均已提交，同一VM 88016495在三次Activity中保留，且成功、旧结果隔离、删除和空账本原断言通过。新的唯一失败在line60非法金额提示assertIsDisplayed：节点存在但未显示，尚未到本测试一分钱段；正在检查滚动与外部布局，不声称全部设备通过。报告和逐项日志在`test-artifacts/ci-37189385205`。

b36fb81仅修测试：滚动到点击保存后新插入的错误行，再保留assertIsDisplayed；补金额仍为0/entries空的未入账检查，然后滚动金额并修正0.01。6项、后续一分钱及月支出断言、10秒等待均保留，无sleep/retry/skip。源码滚动容器与错误插入位置支持此操作，失败时没有完整bounds证据，不把IME说成确定根因。独立增量复审Approved、0新增阻塞；本机`build-20261004-164724.log`设备测试Kotlin/APK实际重编译成功，生产APK未变。已推送再验证，尚未标记新设备通过。

[37190154648](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37190154648)的build实际30项JVM通过、lint0错误/9提示；设备XML实际6项/1失败/0错误/0跳过，非法金额显示和0.01入账已通过。唯一失败在awaitSubmitted:87/case112，logcat明确`save NOT submitted`、jobs0、entries空、busyfalse，尚未执行recreate。失败时语义树的Dialog滚动区域底部1492、scroll0/max342，保存按钮纵坐标1658..1768在clip外，金额12.34仍有焦点；这是失败时而非点击瞬间的几何证据。此前超时并不足以说明保存结果消费故障，继续修测试的键盘/布局就绪条件，保留真实点击及提交断言。报告在`test-artifacts/ci-37190154648`。

c02c1ea仅在重建场景用现有原生Instrumentation收键盘：临时开启UiAutomation窗口检索，10秒内确认输入法窗口出现，再一次真实BACK，等待窗口消失，finally恢复原flags；随后金额12.34仍在、保存按钮显示且启用，再真实点击和等待提交。6项、10秒、重建/同VM/旧结果/删除断言均保留，其它开放键盘0.01场景未改。独立增量审查Approved、0新增阻塞，本机`build-20261004-170248.log`设备测试Kotlin/APK重新编译通过。Espresso不可用的编译尝试`170159`失败已撤回，没有增加依赖或生产hook。该修正随后在37191001385实际6项全通过，生产APK未变。

## 已知边界

lint提示为固定版本更新建议和现有UseKtx风格建议，不通过盲目升级或关闭lint掩盖错误。SDK XML v4/v3工具提示不阻塞本机构建，已如实记录。

JVM/模拟器不能代替作者手机验收。真实系统文件选择器、手机存储provider、IME/大字体及自然使用仍需按PHONE_ACCEPTANCE.md由作者检查。测试APK使用debug签名；后续覆盖安装须用同一份本机debug keystore构建。本机与CI、不同CI运行的签名可能不同，不保证互相覆盖安装；更换签名前应先导出备份。正式签名、应用锁/加密、多账户、商店发布均不在首版范围。

整本备份最多50000条/10MiB，超限会拒绝导出/恢复；目前没有分批导出。保存账目没有同等总条数限制，需在长期使用接近备份上限前安排容量策略。

## 版本控制交付

私有仓库的功能分支为feat/local-ledger-v1；[PR #1](https://github.com/xiaoliumiaomiao/an-jizhang/pull/1)保留为测试交付草稿，作者手机验收未执行。最终文档仅补充证据，不修改已验证的应用实现。

交付文档提交`0f2b0cd61c7af25e676f6385dede2f57f5bee7f0`已成功推送。随后实读GitHub isPrivate=true、PR draft=true，PR附件存在；git ls-remote与本机HEAD均为该提交，git status --short为空。最后仅补记本同步结果与计划勾选，不改变已通过CI的代码；此文档收尾提交后再次核验远端一致与工作区干净。
