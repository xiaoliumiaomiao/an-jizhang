# 0.1.3 金额键盘与成功提示修复验证

状态：本机逻辑红绿已取得；新界面仪器回归待真实运行。不得继承上一候选的55/16通过为本轮结果。

用户报告删除后“已删除”长期留在标题下，重启才消失；要求金额使用应用内数字键盘，备注使用系统IME。本轮仅处理这两项，原0.1.3技术交付79d0a89及旧APK450d保留，作者手机十三项仍待。主目录现有作者文档改动不覆盖。

## 已产生证据

- 根因：ViewModel.work将成功文案写入message，LedgerScreen直接Text展示，无消费/清除；原完成ID还承担保存删除会话结束，不能为消除文案清掉这些ID。
- 原逻辑加可编译最小接口stub后，build-20261005-142837.log真实60用例/5失败；三个数字转换与两条消息身份/条件清除均按预期失败，原55通过。原XML/完整日志在ignored test-artifacts/fix-v013-input-feedback/jvm-red，不改历史失败。
- 最小状态实现：成功消息独立UUID与SystemClock.elapsedRealtime期限；按相同ID只清message相关字段，不新增VM定时Job，不动error/completed/failed/pending。数字转换只处理字符，保留Money.parse/LedgerRules最终精确校验。
- build-20261005-142954.log实际22秒成功，JVM XML60/0失败/0错误/0跳过，新AndroidTest编译。空串小数点helper期望调整为0.后，build-20261005-143130.log唯一AndroidTest编译11秒成功；后者不冒称重跑JVM或设备测试。
- 新3项仪器回归先准备真实删除提示出现→消失/保留非目标账目、实际数字按键/退格/清空/1分、备注系统IME正证→金额IME消失/草稿重建；原16及原native等待/提交观测保持。尚未取得本轮设备结果。

## 后续门禁

旧root与Editor先执行真实UI RED，保存原XML与日志，再实现root期限清除和只读金额/内联数字键盘。对原16只适配金额为用户实际按键；其他文本后严格closeKeyboard保留。随后完整构建、lint、独立审查及冻结源码CI，逐名核原55/16与新增用例、原升级runner和新图。

新包采用0.1.3/code5及新文件an-jizhang-0.1.3-fix1-debug.apk；先核公开签名同旧本机包、包身份/权限/完整许可，再复制双output及新校验文件，保留全部旧APK/校验前缀/截图。PR4继续draft，作者新手机检查未通过前不合main。

## 首次设备运行的环境失败

[CI 37272983725](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37272983725) 的首次尝试：build成功，device-smoke在Prepare emulator以124退出，未运行界面或覆盖升级测试。这不算界面回归RED。保存了原始job/emulator日志；AVD仍存活且boot completed可见，未开启shell tracing，无法据此确定哪条后续adb命令超时。重跑同一SHA的失败job，未扩大超时或改测试。
## 真实旧界面回归 RED

同一 [CI 37272983725](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37272983725) attempt2 在 e3eb3d014ce37741d45085854e9cc822eb48b822 上完成：模拟器准备恢复，JVM60/0失败/0错误/0跳过；UI19/3失败/0错误/0跳过，仅新增3项失败，原16实际通过。逐名对照上轮最终55/16，缺失0；lint0错误/9原有警告。

删除回归在“已删除”出现后等待10秒仍无法观察消失；金额及备注交接回归因amount-keypad不存在失败。原XML、job日志、run/attempt/SHA及摘要位于ignored test-artifacts/ci-input-red-37272983725。因connected失败，覆盖升级链未运行，不冒称升级RED或通过。

此冻结测试之后仅在原3方法补充实际旋转成功提示身份/期限保留，以及非法金额错误超过3.1秒仍显示；上述补充动作未在旧e3执行，等待修复后的真实完整设备运行。
## 新界面源码与本机检查

原截止时间root effect、金额readOnly/内联3×4数字键及Dialog内IME交接已实现。仅金额按键改String草稿，保留Money.parse、原字段/稳定ID/提交协议；无新依赖、权限或数据库变化。原16仅11金额动作改可见按键、5次金额专用IME关闭移除，其余严格IME/断言/10秒/Job与锁完整保留。

build-20261005-145721.log实际成功38秒，完整testDebugUnitTest/lintDebug/assembleDebug/assembleDebugAndroidTest；实际XML60/0失败/0错误/0跳过、原55缺失0，lint0E9W。Task2独立规格/质量Approved C0/I0/M0，尚不表示设备通过。新本机包9458013bytes、SHA256 cc131a6a3624e44b82f96589d399af7c2d3b33cbafe87780377f34f51ab12f15、0.1.3/code5；原生身份/权限/关闭云备份与传输/完整许可字节/公开签名审计成功，证书与旧本机0.1.3同为209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d。尚未复制输出，需当前冻结源码真实设备与升级、原图和完整交付评审。