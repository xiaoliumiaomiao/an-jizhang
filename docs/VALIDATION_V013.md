# 0.1.3 验证记录

状态：完整CI、九图独立视觉/运行审查及交付复制已完成；最终技术交付独立审查进行中，作者手机十三项待验收。

最终受测代码 `fb9ffd89d81e51c04716b86319416f900ad4e564` 的 [CI37236054231](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37236054231)已 completed / success，两job均成功。ROOT实读本次XML：55 JVM、16 UI全部0失败/错误/跳过，原50 JVM与Smoke6/Query4逐名保留；lint0错误/9既有提示。三次先前失败的Flow返回、非法金额及数据库失败保存用例本次均实际通过。Recovery2为保存Compose状态加新VM的模拟，不是Android进程kill实测。

ROOT直接实读本次seed/verify原始runner：各唯一`OK (1 test)`、唯一`INSTRUMENTATION_CODE: -1`、无失败标记；baseline/current/test公开证书均为`461437ab0fe0e5ff1febbdea491072e5435334fdbd67912c591f229eeaf1a970`。实际旧包0.1.2/code3、新包0.1.3/code4；三次安装Success、启动Status:ok、exit0。固定ff8479f旧seed → install-r新版 → 只读verify两次重开，保留schema1、三笔全部字段、1分、中文引号换行及9/10月统计；中途无清库、卸载、重seed、再次connected或降级。原始证据位于ignored `test-artifacts/ci-v013-final-37236054231/`。

本次pull.log实际9 files pulled、0 skipped。ROOT及独立审查者逐张查看九张UI原生PNG和升级首页；`final-frame-nine-screens-runtime-review.md`为Visual / Runtime Approved，Critical0、Important0。statistics.png本次确为月统计（支出69.12、交通56.78、餐饮12.34，统计选中），settings随后正确；旧统计旧帧I1仅对本次候选关闭，历史错误图和报告保留。正常字号末行金额/分类/备注完整视口及不与FAB相交的几何断言、实际滚动图均通过。初屏FAB可遮挡低处行金额的观察仍保留；不声称每种字体/手机初屏均无遮挡。窄屏font2实际用例通过；两图中的最大合法金额与先前零值错误提示并存，错误在重试前保留，不据此声称最大金额保存失败。

本机最终生产APK为9559162字节，SHA256 `450d48c0bb1aaf429a50d6b9fc30cc6482c46fec4be841c16b81b0ea22134d0a`，0.1.3/code4、min26/target35。公开本机证书`209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d`与旧本机0.1.2相同；v2签名有效，无联网/广泛存储权限，自动备份全域排除，三份完整许可与源码一致。审计位于ignored `apk-audit-owner/`；随后只有AndroidTest和文档调整，生产APK哈希未变。CI证书与作者本机证书分别记录。

已顺序执行独立批准的`copy-owner-reviewed-apk.ps1`和准确pin本CI的`copy-reviewed-screens.ps1`，均exit0。工作区与`D:/AIproject/an-jizhang/output`各交付新APK、新`SHA256SUMS-v0.1.3.txt`及九张原图；两APK哈希一致，18张图与来源字节哈希一致。五个旧APK哈希不变，旧SHA256SUMS.txt前缀原字节保留且新增校验行唯一。有界primitive记录在ignored `apk-delivery.json` / `screen-delivery.json`，没有序列化FileInfo元数据。私有draft [PR #4](https://github.com/xiaoliumiaomiao/an-jizhang/pull/4)已创建并附到任务；最终HEAD同步与全交付审查另记录，不把草稿当成作者验收。

## 历史统计截图候选与修正
历史已完成实测候选：`086caba8699a64d67cd393f7da5901f5875bac31`，[CI37234298023](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37234298023)已完成 success；55 JVM与16设备测试均0失败/错误/跳过，原50/10名称全部保留，lint0错误/9提示。最后一处数据库失败保存测试本次完整通过。下面旧候选中的“待”是对应当时的状态。

本轮首次实际完成同CI证书的0.1.2/code3→0.1.3/code4覆盖升级：ROOT实读seed/verify原始runner各唯一OK(1 test)、唯一INSTRUMENTATION_CODE:-1、无失败标记；三包公开证书98756a4ce45b13c7d3d10f46368254538fde254bd4703f20ae96bf45e06510cb相同，实际版本/安装Success/启动Status:ok/exit0均核实。三条记录的所有字段、schema1、1分、中文引号换行和跨月统计在两次reopen中保留，脚本原顺序无中途清库、卸载或重seed；正常中文升级首页已ROOT查看。原始证据位于ignored test-artifacts/ci-v013-final-37234298023/。

九个原生PNG确实全部pull，ROOT逐张及升级图查看，但发现视觉I1：statistics.png实际为切页前的设置画面，统计nav仍呈触摸态；不能将九个文件称为九个目标页面齐。独立视觉审查同样确认。其余八图与正常字号末行完整几何断言已取得实证；初屏FAB遮挡历史观察保留。当时未复制新APK/截图或创建本轮PR，FullDelivery仍待正确统计原生帧证据。下一步只诊断并修正测试截图同步，不改生产界面或业务断言；真实重新验证后再交付。

截图同步准备仅修改两个AndroidTest文件：guard后以公开WindowInspector选唯一实际attached/shown/focused硬件根窗，主线程注册frame commit并invalidate，测试线程最多10秒等待真实回调，finally注销，再执行一次原生全屏截图。统一覆盖九次调用，包括实际Dialog根窗；不固定使用后台Activity。统计调用保留列表显示断言并增加选中标签、月统计和支出分类。固定Compose1.8与Android15官方源码已核实；frame commit只承诺渲染提交，不承诺已经显示，最终原图必须复看。无sleep、隐藏API、裁切、重拍挑图或生产改动。

本机本项共两次AndroidTest编译均12秒成功：首次窄范围版本build-20261005-052149.log，在ROOT澄清统一九次范围前已开始；最终版本build-20261005-052238.log。两原记录分别保留，首次不充当最终源码验证。最终39保护源码与9559162字节的450d48c0…生产APK未变；最能力独立源码增量Spec / Quality Approved，Critical0、Important0，当时真实新CI图像仍待。

## 范围与基线

作者已确认 0.1.2 通过；已验收 main 为 `ff8479f75cd917bc49e5ab2173ad7e9906dc480c`。本轮在 `feat/ledger-ui-v0.1.3` 重构明细、统计、设置、详情及全屏编辑，不改变 schema1、备份 format1、整数分规则、applicationId、minSdk26、依赖或权限。

开发前的官方成熟产品比较与复用决定见 [REUSE_V013](REUSE_V013.md)，需求和状态契约见 [DESIGN_V013](DESIGN_V013.md)，作者检查见 [PHONE_ACCEPTANCE_V013](PHONE_ACCEPTANCE_V013.md)。所有设备测试使用隔离 CI AVD 与合成账目；不操作作者手机数据库。

## 已执行的本机验证

- 基线：`build-20261005-025531.log`，原 50 JVM 用例全通过，7 XML 已归档。
- 日期分组：4 个新用例实际 RED 为 3 失败，GREEN 为 4 通过；原 50 个名称保留，全套增加到 54。不为颜色值编写镜像测试。
- 失败 operation ID：新用例先实际失败，再通过；连续同文案失败仍产生独立 ID，旧成功不误消费，合法重试可完成。
- 新界面完整构建：`build-20261005-032114.log`，55 JVM、0 失败/错误/跳过，lint 0 错误/9 提示，应用和仪器测试 APK 编译成功。XML、原日志及 lint 在 ignored `test-artifacts/sdd-v013/final-local-after-palette/`。
- 两个新 VM 恢复用例：`build-20261005-032924.log` 编译成功；`StateRestorationTester` 只模拟保存的 Compose UI 状态恢复到新 VM，没有实际杀 Android 进程。

原 50 JVM、原 6 Smoke 和 4 Query 的断言、同 VM 与 10 秒等待均保留。旧 UI 只调整新页面访问路径，没有用 `assertExists` 替换可见性断言。新增恢复测试对真实排队任务、同 ID 保存及删除 sentinel 的数据库结果作断言。

## 已执行的 CI RED

[旧界面运行 37227250211](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37227250211)，代码 `0e0896f51173b9ff937519a63c3af0b2d76e723c`：build success，JVM 54 通过；connected 共 13，原 10 全通过，新增 Flow 3 在旧版缺少 `ledger-list`/`nav-settings` 等入口处失败，0 错误/跳过。升级链因 connected 失败未执行。原始日志/XML 和失败详情保存在 ignored `test-artifacts/ci-v013-red-37227250211/`。

最终源码审查发现 F1：保存 UI 的 pending IDs 恢复到新 VM 后，没有后台任务能产生其完成结果，可能永久禁用操作。两个恢复 fixture 经独立准备审查后随新 UI 提交到有意回归检查点 `fe2974467826e65b178fb6fab24d0a0c4755ce9d`；[运行 37228768041](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37228768041) 已完成，ROOT 下载并读取原始报告。此检查点不是交付版本。

该运行 JVM55 全通过、lint0错误/9提示；connected16、6失败、0错误/跳过，原10全部通过。Recovery 删除例在恢复后 `确认删除.assertIsEnabled` 因 Disabled 失败，是 F1 的真实行为 RED。保存例提前因“支出”同时匹配背景详情及编辑类型而失败，未到达解锁断言，不称保存恢复已取得行为 RED。Flow3 和 LargeText1 因截图 helper 的 `mkdir && cp && echo` 作为普通程序参数执行而缺少成功输出提前失败；截图 pull 为 0 个文件。connected 失败，升级链未执行。原始 XML、完整 CI/logcat 和摘要保存在 ignored `test-artifacts/ci-v013-recovery-red-37228768041/`。

截图工具根因按 [Android15 AOSP UiAutomationConnection](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-15.0.0_r1/core/java/android/app/UiAutomationConnection.java) 核实：`executeShellCommand` 调用 `Runtime.exec(String)`，不会解析 shell 操作符。修正为顺序执行单程序 mkdir/cp/stat、验证目标真实字节数；保存恢复选择器限定可选择的类型节点，保留显示、选中、enabled、数据库和10秒断言。测试工具修复后重新执行，不以重跑到绿反推前次通过。

owner 最小修复只增加两生产文件7行；完整本机 `build-20261005-033516.log` 实际55/0/0/0、lint0错误/9既有提示，应用和测试 APK 成功，39个其他源码/测试文件哈希不变。独立源码增量 Spec/Quality Approved，Critical0、Important0；F1 静态路径闭合，实际新 UI/恢复 GREEN 尚待。当时本机候选 APK 为 9559162 字节、SHA256 `450d48c0bb1aaf429a50d6b9fc30cc6482c46fec4be841c16b81b0ea22134d0a`，公开证书与旧包相同、权限及原完整许可已重审计；当时尚未复制交付。

## 历史候选失败与修正

当时候选为 `14796f3aae611a0df3798f432fd36bc4115b1f22`；两测试原生事件同步已获独立审查后推送，[CI37232888471](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37232888471)已完成但整体失败。ROOT实读55 JVM/0失败错误跳过、lint0E9W；connected16/1失败/0错误/0跳过，原50/10名称全部保留，9张原生PNG全部取回。前两失败（Flow详情返回和非法金额1分重试）本次均完整通过；恢复、窄屏等其余14项中的13项也通过，尚不称全部设备GREEN。

唯一失败为原Smoke `databaseSaveFailureAfterRecreationKeepsEditorInputs` 在`awaitSubmitted`（Smoke:185）等待新活动Job超10000ms。既有失败诊断明确`NOT submitted`、jobs0、busyfalse/errornull，尚未执行Activity重建或断言SQL失败；不能说数据库失败恢复已在本轮通过或生产DB失败被修复。此方法金额输入后缺少原生收键盘，成功queued save/delete方法已使用该步骤。下一步只补该用例真实点击输入、原10秒收键盘、保存按钮可见可用，完整保留故障表、真实提交、锁、同VM重建和输入/失败提示断言；准备后编译与独立审查，再真实CI。升级链此次仍未执行，最终交付仍待。下列候选失败均保留为历史实证，不反推此前通过。

该单方法同步现已准备并独立 Spec / Quality Approved，Critical0、Important0。原始语义树中金额12.34有效，保存按钮1651..1761px完全低于表单可视底1628px；支持IME/可见性准备不足的推断，日志缺实际触点，不能称百分百定因。仅补真实点击、既有收键盘及保存按钮可见可用；方法外全文与其余40源码哈希未变，原锁、DROP TABLE、真实排队观察、重建、失败输入与10秒等待均保留。`build-20261005-045629.log`唯一AndroidTest编译实际10秒成功，生产APK仍450d48c0…；实际设备GREEN及升级待下一次CI，不以编译替代。

候选 `e30c02d13b4938da6aa6f314952321e4b6b051c2` 包含真实第二次点击及末行完整几何补证；[CI37231650011](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37231650011)已完成但整体失败。最能力独立源码增量审查 Source Spec/Quality Approved，Critical0、Important0；这不等于运行或Delivery通过。ROOT实读8个JVM XML为55/0/0/0、lint0错误/9提示；connected16/2失败/0错误/0跳过，原50/10用例名称全部保留。

该次末行金额/分类/备注完整矩形在可视列表内且与FAB不相交的断言实际通过，9张原生PNG全部取回，详情及滚动后的末行图已ROOT直接查看。第二次搜索IME准备也已通过；Flow继续到取消编辑后native Back，再读明细时缺`ledger-list`（Flow:67）。原Smoke非法金额用例在读取错误节点处失败（Smoke:73，0 node而不是可见性失败）；其他14方法通过。升级链仍未执行，图片齐全不能代替完整设备通过。

Flow case原logcat显示取消Dialog后Back发向已无input channel的旧Dialog窗口，需等待编辑节点消失及Activity窗口焦点恢复，再发原Back并等待列表结果。Smoke日志显示保存触摸前IME已显示、没有收起；固定Compose1.8的performClick实际注入触摸，最支持IME/布局变化导致点击准备不足的假设，日志缺触点与当时语义树，不能称100%定因。最小测试同步修正使用真实点击后原10秒closeKeyboard、保存按钮可见/可用，再保留原非法错误/0/数据库未写/合法1分断言；仅失败时补已有语义树/VM诊断并重新抛出。无生产校验修改，`build-20261005-043541.log`测试APK编译23秒成功、39个其他保护文件及生产APK哈希未变；独立审查及新真实CI仍待。

两测试工具修正已编译并独立 Spec/Quality Approved，Critical0、Important0；候选受测代码 `45e2cf95105bfa7fe8cbb49d6ec471cfcca8c87e`，包含 owner 与工具修复。[运行37229801074](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37229801074) 已完成但整体失败：55 JVM 通过，lint0错误/9提示，connected16/1失败/0错误/0跳过。原10、Recovery2、LargeText1及Flow另外2例全部通过；删除恢复的F1行为已取得GREEN，保存恢复本次首次实际通过。唯一失败是Flow第二次替换搜索词后等待IME出现超10秒，发生在详情截图之前；7张原生PNG已取回，详情图缺失，升级链未执行。

原logcat显示第一次用户Back已关闭IME，第二次替换仍聚焦输入框内容没有触发重新显示；只在第二次输入前增加真实点击，保持helper、10秒等待和业务断言。测试APK编译10秒成功，40个其他保护文件及生产APK哈希未变，独立Spec/Quality Approved，C0/I0；真实GREEN尚待下一CI。七张图片的部分视觉审查发现明细初屏最后一条餐饮金额被FAB覆盖；当前末尾padding可能允许滚动查看，需补实际滚动、金额与FAB不重叠的几何断言及第9张`ledger-last-entry.png`，不能凭静态padding关闭该观测。 末行补证测试现已编译成功（build-20261005-041609.log，10秒），40保护文件和生产APK不变；只完成测试准备，实际几何断言及第9图仍待CI。

两个原生事件同步修正现已独立Spec / Quality Approved，Critical0、Important0。重新运行真实新 UI、恢复及窄屏 CI；核对同次9张原生合成截图（含最后一行滚动证据）；固定 0.1.2/code3 → 0.1.3/code4 的同证书替换、seed/verify 原始 runner、安装与启动；精确最终候选 APK 的新文件复制和旧文件哈希保留；私有 draft PR、最终三方 HEAD 同步及作者手机验收。尚不声称技术交付或作者验收完成。

## 最终交付边界

上述历史段落中的“待”均对应当时检查点；当前完整运行、视觉和实物见文首，最终独立全交付与同步记录将据实际结果补充。受测fb9ffd之后的收尾提交只允许正式文档变化，推送后逐一核验local/remote/PR HEAD、PRIVATE、OPEN/draft、clean与main仍ff8479f。

PHONE_ACCEPTANCE_V013.md十三项全部未勾，手机型号/Android版本未提供。作者先在0.1.2导出备份，再用本机同签名0.1.3正常覆盖安装；异常保留原应用与账本，不卸载、清库或强降级。作者确认本轮前不合main或正式发布。