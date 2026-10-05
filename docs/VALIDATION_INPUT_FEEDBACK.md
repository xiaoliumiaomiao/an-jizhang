# 0.1.3 金额键盘与成功提示修复验证

状态：本轮冻结源码的完整 CI、原始升级 runner、独立源码及视觉审查、实际 APK/截图交付已通过；最终完整交付审查与同步收尾中。作者手机验收仍待。

用户报告删除后“已删除”长期留在标题下，重启才消失；要求金额使用应用内数字键盘，备注使用系统IME。本轮仅处理这两项，原0.1.3技术交付79d0a89及旧APK450d保留，作者手机十三项仍待。主目录现有作者文档改动不覆盖。

## 实施过程与逻辑红绿

- 根因：ViewModel.work将成功文案写入message，LedgerScreen直接Text展示，无消费/清除；原完成ID还承担保存删除会话结束，不能为消除文案清掉这些ID。
- 原逻辑加可编译最小接口stub后，build-20261005-142837.log真实60用例/5失败；三个数字转换与两条消息身份/条件清除均按预期失败，原55通过。原XML/完整日志在ignored test-artifacts/fix-v013-input-feedback/jvm-red，不改历史失败。
- 最小状态实现：成功消息独立UUID与SystemClock.elapsedRealtime期限；按相同ID只清message相关字段，不新增VM定时Job，不动error/completed/failed/pending。数字转换只处理字符，保留Money.parse/LedgerRules最终精确校验。
- build-20261005-142954.log实际22秒成功，JVM XML60/0失败/0错误/0跳过，新AndroidTest编译。空串小数点helper期望调整为0.后，build-20261005-143130.log唯一AndroidTest编译11秒成功；后者不冒称重跑JVM或设备测试。
- 新3项仪器回归先准备真实删除提示出现→消失/保留非目标账目、实际数字按键/退格/清空/1分、备注系统IME正证→金额IME消失/草稿重建；原16及原native等待/提交观测保持。当时尚未取得设备结果，随后真实RED/GREEN见下文。

## 实施前确定的门禁

旧root与Editor先执行真实UI RED，保存原XML与日志，再实现root期限清除和只读金额/内联数字键盘。对原16只适配金额为用户实际按键；其他文本后严格closeKeyboard保留。随后完整构建、lint、独立审查及冻结源码CI，逐名核原55/16与新增用例、原升级runner和新图。

新包采用0.1.3/code5及新文件an-jizhang-0.1.3-fix1-debug.apk；先核公开签名同旧本机包、包身份/权限/完整许可，再复制双output及新校验文件，保留全部旧APK/校验前缀/截图。PR4继续draft，作者新手机检查未通过前不合main。

## 首次设备运行的环境失败

[CI 37272983725](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37272983725) 的首次尝试：build成功，device-smoke在Prepare emulator以124退出，未运行界面或覆盖升级测试。这不算界面回归RED。保存了原始job/emulator日志；AVD仍存活且boot completed可见，未开启shell tracing，无法据此确定哪条后续adb命令超时。重跑同一SHA的失败job，未扩大超时或改测试。
## 真实旧界面回归 RED

同一 [CI 37272983725](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37272983725) attempt2 在 e3eb3d014ce37741d45085854e9cc822eb48b822 上完成：模拟器准备恢复，JVM60/0失败/0错误/0跳过；UI19/3失败/0错误/0跳过，仅新增3项失败，原16实际通过。逐名对照上轮最终55/16，缺失0；lint0错误/9原有警告。

删除回归在“已删除”出现后等待10秒仍无法观察消失；金额及备注交接回归因amount-keypad不存在失败。原XML、job日志、run/attempt/SHA及摘要位于ignored test-artifacts/ci-input-red-37272983725。因connected失败，覆盖升级链未运行，不冒称升级RED或通过。

此冻结测试之后仅在原3方法补充实际旋转成功提示身份/期限保留，以及非法金额错误超过3.1秒仍显示；上述补充动作未在旧e3执行，实际修复后完整设备结果见下文。
## 新界面源码与本机检查

原截止时间root effect、金额readOnly/内联3×4数字键及Dialog内IME交接已实现。仅金额按键改String草稿，保留Money.parse、原字段/稳定ID/提交协议；无新依赖、权限或数据库变化。原16仅11金额动作改可见按键、5次金额专用IME关闭移除，其余严格IME/断言/10秒/Job与锁完整保留。

build-20261005-145721.log实际成功38秒，完整testDebugUnitTest/lintDebug/assembleDebug/assembleDebugAndroidTest；实际XML60/0失败/0错误/0跳过、原55缺失0，lint0E9W。Task2独立规格/质量Approved C0/I0/M0，当时尚不表示设备通过。新本机包9458013bytes、SHA256 cc131a6a3624e44b82f96589d399af7c2d3b33cbafe87780377f34f51ab12f15、0.1.3/code5；原生身份/权限/关闭云备份与传输/完整许可字节/公开签名审计成功，证书与旧本机0.1.3同为209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d。当时尚未复制输出，后续真实设备、升级及实物交付见下文。

## 当前冻结源码的完整 CI GREEN

受测源码 `ee9b718c6a7a6dc3b8a2ccd5d739196e61b07d65` 的 [CI37275582803](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37275582803) attempt1 两个 job 均 success。ROOT 下载本次 android-build/android-device 与原始 device-job.log，直接读取实际 XML：**60 JVM、19 UI，全 0 失败/错误/跳过**，原55 JVM/16 UI逐名缺失0，lint0错误/9原有提示。设备原日志实际 Starting/Finished 19 tests，未用上轮结果代替。材料保存在 ignored `test-artifacts/ci-input-green-37275582803`。

新增三条实际通过：删除反馈先出现，实际 Activity 重建保留同一 VM、反馈 ID 与原截止时间，再在允许调度观测余量内消失并保留另一笔全部字段；可见数字按键输入、小数点、退格、清空，非法金额超过3.1秒仍显示错误，0.01最终保存为整数1分；备注触摸后 native IME 窗口出现，再触摸金额后 IME 消失，重建保留金额、类型、日期、分类、中文多行备注。系统IME检验是真实窗口正/负观察；中文内容由测试文本动作写入，不冒称实体中文输入法手工验收。

原16本次实际通过，涵盖真实 Activity 重建、排队提交及旧完成ID、失败草稿、查询/月份/导航、详情与取消、大字滚动等。Recovery 的 StateRestorationTester 加新 VM 验证仍是状态恢复合同测试，不冒称真实 Android 进程杀死复活。

## 当前原始覆盖升级与图像

ROOT 直接读取本次 baseline/current/test 三份 badging、公开证书、三份实际安装输出、seed/verify 原始 runner、启动/result及安全设备标识，并对照固定脚本。CI 临时 `ci-ledger` QEMU、唯一 emulator-5554，三包同公开证书 `3c573d1128b9523eab4350c422e55095c9ab14da69f7262ec8d32ba0fc42fa5d`；此证书与本机测试包不同，CI包不用于覆盖作者账本。

原始 seed/verify 各唯一 `OK (1 test)` 与 `INSTRUMENTATION_CODE: -1`，无 runner 失败。旧 **0.1.2/code3 → 0.1.3/code5** 实际替换；两个阶段之间仅 force-stop 与 `install -r`，无卸载、清库或降级。schema1、三笔完整字段（含中文、引号、换行和1分）、十月及九月统计在升级后打开及重新打开均一致。实际启动 `Status: ok`，升级 exit0及PASS齐全。

ROOT 已逐张查看本次11张原始 device-ui-screens PNG及升级首页 device-screen.png，未裁剪/改图：金额内置数字区清晰且无系统键盘，删除图标题下已无“已删除”，保留收入56.78；统计是本次正确69.12分类图，设置为0.1.3。窄屏720×1280、320dpi、fontScale2.0的金额/按键/日期及滚动后保存取消清楚；大字测试实际恢复原size/density/font设置。

初始明细及升级首页的末行有部分被浮动按钮遮住，这是当前滚动位置的观察；独立 ledger-last-entry 图显示完整末行与金额，本次旧 Flow 测试仍实际断言末行文字全部位于viewport且不重叠FAB。Editor图为不同滚动位置；大字操作图保留此前非法0触发的校验文字，随后同一测试成功保存修正后的最大金额。图像只证明所测模拟器场景，不能替代作者手机。ROOT 场景/hash记录 `fix-v013-input-feedback/root-current-visual.json`；独立 runtime/visual 实读原XML、raw、native IME有界日志并逐view全部12图，**Spec/Quality Approved C0/I0/M0**，报告 `current-runtime-visual-review.md`。font2全部13键实际可见；该大字方法实际点击0/9/点/清空和保存取消，未逐个点击其他数字与退格；全部键实际输入覆盖来自正常字号回归，不扩大证据范围。

## 当前实物交付与保护

实际执行已独立静态预审的审计/复制工具。0.1.3/code5本机包 `an-jizhang-0.1.3-fix1-debug.apk` 为9458013bytes、SHA256 `cc131a6a3624e44b82f96589d399af7c2d3b33cbafe87780377f34f51ab12f15`；固定包名/min26、无新联网或存储权限、自动云备份/设备传输全域排除、三份完整许可字节/公开同签名209fe…均原生实核。

已复制至隔离工作区及 `D:/AIproject/an-jizhang/output`，两个新APK均与当前审计来源相同，分别新增 `SHA256SUMS-v0.1.3-fix1.txt`；总校验只追加一条fix1行，原376/282bytes前缀保留，实际总文件475/381bytes。旧7APK、旧18PNG及旧独立校验全部按本轮初始29条快照逐项保留。主目录作者HANDOFF与未跟踪project-plan-v1.0两文件hash/bytes保留，未清掉作者dirty状态。

同次11张已由ROOT及独立agent查看的原图复制至两处 `output/v0.1.3-fix1-screenshots`，22个唯一目标与各原图hash/bytes一致，旧18图保留；升级首页原图仍存本次CI材料。ROOT随后重算两APK/22PNG来源及目标和全部27项非总校验旧物/两作者文档，实际一致。ignored `apk-audit/audit.json`、`apk-delivery.json`、`screen-delivery.json`保存实物记录，不使用旧code4审计。

最终完整技术交付审查和local/remote/PR同步仍待实际记录。新[手机9项](PHONE_INPUT_FEEDBACK.md)及旧[手机13项](PHONE_ACCEPTANCE_V013.md)全未勾，main保持已验收0.1.2；先导出备份，再用本机同签名fix1覆盖安装。技术CI/原图不代替手机验收。
