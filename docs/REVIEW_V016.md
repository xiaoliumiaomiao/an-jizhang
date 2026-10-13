# 0.1.6 独立审查记录

记录日期：2026-10-08；候选 `0.1.6 / code 8`，开发分支 `codex/ledger-statistics-v0.1.5`，基础 HEAD `0cfbb8ec79c882cefc68615152e2ddde73c5b2e5`，受审对象为本轮未提交工作区及各报告绑定的文件快照。

当前Source、Native、Runtime、Upgrade及最终Visual均独立Approved，C0/I0/M0。118 JVM、64 UI与seed／verify各1实际全0；本轮未提交技术FullDelivery独立Approved，C0/I0/M0。见[验证记录](VALIDATION_V016.md)。

## 已取得的独立结论

| 范围 | 实际结论 | 证据与限制 |
|---|---|---|
| 最终产品源码 | Source Approved，Critical 0／Important 0／Minor 0 | [final-source-review.md](../test-artifacts/ui-v016/final-source-review.md)。原 Smoke 版本断言问题经严格 0.1.6 修正后复审；旧发现保留在报告历史中。 |
| 三项后续 test-only 修正 | Source Spec／Quality Approved，C0／I0／M0 | [incremental-test-source-review.md](../test-artifacts/ui-v016/incremental-test-source-review.md)，绑定 83 路径 manifest `769DF6…`；涵盖反馈时钟、Smoke 版本及月度截图定位。 |
| 最新两项测试就绪修正 | Source Spec／Quality Approved，C0／I0／M0 | [readiness-test-source-review.md](../test-artifacts/ui-v016/readiness-test-source-review.md)，绑定最终 `0F9CD835…` 快照，保留旧业务断言。 |
| 原生产品包 | Native Approved，C0／I0／M0 | [native-audit/report.md](../test-artifacts/ui-v016/native-audit/report.md)，产品 SHA `9F3FC66D…`；签名、SDK、权限、备份排除、许可证和依赖核验。随后 `55D0BD…` 测试包补充核验 PASS，见 [final-test](../test-artifacts/ui-v016/native-audit/final-test/summary.json)；最终 `C22A60…` 测试包 Test Native Increment Approved，C0／I0／M0，见 [最终增量](../test-artifacts/ui-v016/native-audit/final-test-readiness/report.md)。 |
| 第一轮 19 图 | Visual Issues，C0／I0／M1 | [visual-review-run1.md](../test-artifacts/ui-v016/visual-review-run1.md)。两张月度收入／支出图未取到比例条，属证据取景不足；其他 17 图无阻断发现。后续最终新19图已Approved，见下行；原缺口和修正证据仍保留。 |
| Runtime／完整设备 | Runtime Approved，C0/I0/M0；实际64／0F／0E／0S，55旧项逐名missing0、新9全部在 | [独立Runtime／Upgrade](../test-artifacts/ui-v016/final-runtime-upgrade-review.md)、[原件](../test-artifacts/ui-v016/device-final-green/results/)，历史失败／中断保留。 |
| 覆盖升级 | Upgrade Approved，C0/I0/M0；seed1／verify1全0，三笔每字段、schema1及跨月汇总保持，原生启动ok | [独立报告](../test-artifacts/ui-v016/final-runtime-upgrade-review.md)、[升级原件](../test-artifacts/ui-v016/upgrade-local/) |
| 最终19图 | Visual Approved，C0/I0/M0；首轮monthly取景M1关闭 | [逐张审阅](../test-artifacts/ui-v016/final-visual-review.md)，绑定修正后AFB63清单；不是手机／TalkBack／API26结论。 |
| 最终交付 | FullDelivery Approved，C0/I0/M0，限定本轮未提交技术交付 | [独立最终报告](../test-artifacts/ui-v016/final-full-delivery-review.md)、[42新副本和227旧输出读回](../test-artifacts/ui-v016/delivery.json)；手机／S3／发布不在批准范围。 |

## 最新快照与审查边界

产品 APK 一直保持 SHA-256 `9F3FC66DA998A9E0003588DABD48FCC14B4C27D7BDEE3D18362A18859FF3C76A`。两项测试就绪修正后的当前 83 路径 [source-manifest](../test-artifacts/ui-v016/candidate-build/source-manifest.json) SHA-256 为 `0F9CD83523481AA0A6DF77C7F23EE4D54EC05606626EDE9B9528CA2317EEBD2D`，测试 APK 为 `C22A60E21AF968648173A4845BACDB20E448DF8B10C5633FA168B96419C80777`。此快照已由最新独立源码及测试包原生增量报告分别批准，不将 769DF 的批准范围自动扩大到新快照。

独立源码审核确认的三项修正保留了实际业务断言：Smoke 两处严格显示 0.1.6；月度截图按 `category-title` 定位并要求两条真实比例条可见；反馈测试对齐真实／虚拟时钟但保留真实 5,000 ms 上限、原 10,000 ms 等待预算、原 expiry 之前不得消失、同一 ViewModel／消息 ID／数据库／导航验证。产品反馈 3,000 ms 期限未改变。

随后两项纯测试就绪修正分别等待 Activity 配置稳定与备注控件可操作；金额 `999999999.99`、20 字分类、200 字备注、保存／详情／恢复等业务断言保持。第三套两个失败的原因独立记录，不声称同一原因、产品缺陷或所有历史运行均不稳定。见 [分类配置报告](../test-artifacts/ui-v016/category-config-readiness-fix.md)、[LargeText 报告](../test-artifacts/ui-v016/large-text-readiness-report.md)。实际 [局部 4 项](../test-artifacts/ui-v016/device-readiness-local-green/summary.json) 已通过；最终完整64项随后实际全0，见device-final-green原件。

## 保持的范围

schema 1、format 1、版本升级数据兼容、现有依赖和授权边界保持。本轮未实施 S3 分类库／预算／schema 2 或 format 2 迁移。原生报告中的应用权限为既有自身 signature 权限，测试 APK 另有 REORDER_TASKS；“无 Android 系统权限／联网权限”不能改写为“绝对零权限”。

最终完整设备、覆盖升级、最新快照及19图已审查通过；FullDelivery独立Approved、C0/I0/M0，已据真实报告写回本条事实；交接4个test文件数量笔误已关闭。手机 [22 项清单](PHONE_ACCEPTANCE_V016.md) 属最终作者验收与 debug 阶段，全部保持未勾，不继承 0.1.4 的历史勾选。
