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
