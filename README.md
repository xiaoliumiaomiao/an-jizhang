# 安记账

中文安卓本地记账软件。**0.1.5（versionCode 7）／V1.0 S2 统计深化**实现月度、年度与逐笔来源回看；源码、运行、升级、视觉和本机APK候选审查已通过；两处output新交付物已逐项hash读回，最终技术FullDelivery独审已通过，文档提交cfa0594已完成Git同步核验。方案版本与应用版本分别记录。0.1.0、0.1.1、0.1.2、0.1.4已由作者确认通过；0.1.4的19项手机验收于2026-10-07据“全部予以通过”确认。0.1.5的20项手机验收仍待作者操作。

- 明细／统计／设置导航、日期分组、账目详情、全屏新增与编辑；金额使用按需弹出的应用数字键盘，备注使用系统输入法。
- 月度／年度完整收入、支出和结余；年度12个月收入／支出双柱与同源文字金额，点某月回看月统计。
- 收入和支出各自的分类排行、占比、笔数和平均每笔；点分类按期间、类型与精确分类查看账目。
- 历史期间比较上一完整期间；当前期间按相同天数比较，展示两段具体日期、金额和差额；没有基期记录时解释原因。
- 较大单笔前三项可进入详情。统计／分类来源的编辑和删除回到原来源，返回及旋转保留期间、类型与滚动位置。
- 分类／备注字面搜索与类型筛选只影响明细，统计和备份仍使用完整数据；私有SQLite保存，整数分计算，JSON备份先完整验证再事务合并，同ID冲突整批拒绝。

统计仅分析已记录账目，无记录不代表没有消费；结余是记录收入减记录支出，不等同账户余额。完整总览会计入所选期间的未来日期记录并提示，同期比较只取其明示窗口，不混用口径。

保持schema1、备份format1、稳定UUID、依赖和权限合同。无广告、遥测、云同步，不申请联网或广泛存储权限。整本备份最多50000条／10MiB，单笔金额0.01至999999999.99元，人民币、单账本，支持Android8.0及以上。API35合成模拟器技术验证与作者真实手机验收分开记录。

## 当前 S2 技术证据

冻结候选`7be948c40c96e7fe0d75448e284ff52ee8e45c03`已获Source增量与本机候选原生材料独审Approved。[CI37626498602 attempt1](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37626498602) 两job success：111 JVM、55 UI、升级seed／verify各1，均0失败／错误／跳过；旧81／35逐名保留，lint0E9W。独立审查新60张原PNG后给Runtime／Upgrade／Visual Approved C0/I0/M0，三字号MAX钻取金额完整单行，RV-M1关闭。完整证据见 [VALIDATION_V015](docs/VALIDATION_V015.md) 和 [REVIEW_V015](docs/REVIEW_V015.md)；技术FullDelivery Approved C0/I0/M1（仅既有工具提示），cfa0594已实核本地／远端／PR一致且工作树干净；批准写回后的身份见最终同步记录。

## 安装与手机验收

本机7be候选APK已原生审计：9,834,364 bytes，SHA256 `072afe980ecdbeaae398c4630d49ecc7a6374ef465e013b05d31b998aeb43c46`，与旧本机包公开debug证书相同。两个output已复制并读回；可从 [an-jizhang-0.1.5-debug.apk](output/an-jizhang-0.1.5-debug.apk) 和 [SHA256SUMS-v0.1.5.txt](output/SHA256SUMS-v0.1.5.txt) 获取。两处output各有同hash APK、独立校验文件及59张原业务PNG，120个新副本hash均与来源一致。

先在手机现有0.1.4导出有效备份，保存到可信位置，再正常覆盖安装本机同签名0.1.5，按 [20项手机验收表](docs/PHONE_ACCEPTANCE_V015.md) 操作。安装异常保留原应用、账本和备份，不卸载、清库或强制降级。复制后107原件／16合同hash保持。GitHub CI临时debug证书与本机交付包证书不同，不能把CI包当作者手机覆盖安装包。

## 开发文档

项目规则见 [agent.md](agent.md)，当前交接见 [HANDOFF](docs/HANDOFF.md)，构建路径见 [DEVELOPMENT](docs/DEVELOPMENT.md)，S2计划见 [实施计划](docs/superpowers/plans/2026-10-07-statistics-s2.md)。本轮未新增应用依赖、第三方源码或字体／图标；来源与许可见 [REUSE_V015](docs/REUSE_V015.md) 和 [NOTICE](third_party/NOTICE.txt)。S1及更早版本的技术记录保留在 [VALIDATION_V014](docs/VALIDATION_V014.md) 与 [历史交接](docs/HANDOFF.md)，S2历次失败和修复详见 [VALIDATION_V015](docs/VALIDATION_V015.md)。正式签名、商店发布和S3另行确认。
