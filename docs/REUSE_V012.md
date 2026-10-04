# 0.1.2 开发前复用审查

日期：2026-10-05（Asia/Shanghai）。作者要求继续迭代，本轮按已公开推荐的账目搜索与收支筛选推进。另一subagent在实施前真实只读查询GitHub和官方开发文档，检查许可、数据流、权限、依赖及采用成本；详细原始调研在忽略的`test-artifacts/sdd-v012/reuse-research.md`。

## GitHub候选与决定

核对[MoneyBook固定提交98c50341da262e839f68557e721eee38aca57067](https://github.com/AndersOnLin4/moneybook/tree/98c50341da262e839f68557e721eee38aca57067)的[MIT许可](https://github.com/AndersOnLin4/moneybook/blob/98c50341da262e839f68557e721eee38aca57067/LICENSE)、BillListScreen、ViewModel、DAO、依赖和Manifest。查询时仓库未归档，最近可见推送2026-08-14；维护信息不等于安全保证。

该项目已有文本框、收支FilterChip和重置交互，可参考控件选择；它用Room的SQL LIKE搜索分类或备注，月头统计来自筛后数据。LIKE的`%`和`_`为通配符，参见[SQLite官方文档](https://www.sqlite.org/lang_expr.html#like)。本轮要求字面关键词且统计维持整月，因此不直接复制搜索源码，也不引入Room、DataStore、通知或其他外围模块。

## 实际复用

| 已有资产 | 官方来源 | 本轮采用 |
|---|---|---|
| Kotlin stdlib 2.1.20 | [contains](https://kotlinlang.org/api/core/2.1/kotlin-stdlib/kotlin.text/contains.html)、[trim](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.text/trim.html) | 所选月、收支类型与分类/备注字面子串组合；匹配时trim、ignoreCase，不使用SQL或正则 |
| Material3控件 | [FilterChip](https://developer.android.com/develop/ui/compose/components/chip)、[文本输入](https://developer.android.com/develop/ui/compose/text/user-input) | 沿用value/onValueChange单行OutlinedTextField、受控FilterChip；以实际固定版本编译为证据，不照搬新state型API |
| Compose状态与测试 | [状态保存](https://developer.android.com/develop/ui/compose/state-saving)、[测试](https://developer.android.com/develop/ui/compose/testing/common-patterns) | rememberSaveable只保存查询String和类型名；复用真实Activity重建、滚动和IME操作，不保存账本或筛后列表 |

实读[Google Maven BOM 2025.04.01 POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.04.01/compose-bom-2025.04.01.pom)，现有Material3为1.3.2、Compose foundation/ui/runtime-saveable/ui-test-junit4为1.8.0；保持固定依赖。搜索不承诺拼音、繁简互转、去重音或Unicode规范化。查询与类型仅为界面状态，账本、整月统计和整本导出仍使用原数据。

本轮属于公共API及既有库复用，没有新增外部源码复制、新依赖或权限。首版固定SHA金额片段及MIT/Apache完整许可继续随APK保留。计划先为纯逻辑和界面行为分别取得真实RED，再实施并验证GREEN；实际结果另记VALIDATION_V012.md，不用调研推定功能通过。
