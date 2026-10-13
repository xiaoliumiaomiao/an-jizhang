# 0.1.5 / S2 统计复用与方法审查

日期：2026-10-07（Asia/Shanghai）。用户已验收0.1.4，授权进一步统计分析与自省。仅采用产品思路与现有公共API，自行写代码和中文文案；没有安装竞品、复制截图/品牌/源码、下载库或升级依赖。

| 实读官方来源 | 确认内容与采用边界 |
|---|---|
| [Realbyte Stats](https://help.realbyteapps.com/hc/en-us/articles/360042842734-Stats-tab) | 2020帮助描述收支分类、金额/笔数、期间/年度/趋势和分类详情。采用结构、排行与逐笔来源回看；旧帮助不证明当前安卓布局。 |
| [MoneyLover Home](https://moneylover.zendesk.com/hc/en-us/articles/34522203246745-Home-Screen) | 2024帮助说明月内累计对齐历史相同时段及较大单笔。采用明确时间窗口和金额排行思想，不复制其三月均值规则。 |
| [Compose绘制](https://developer.android.com/develop/ui/compose/graphics/draw/overview) | 现有Canvas/DrawScope足够12月双柱，自己组合已有组件；无图表依赖、远程字体或图片。 |
| [Compose语义](https://developer.android.com/develop/ui/compose/accessibility/semantics) | 自绘图形须补语义/文字，保留同源准确金额清单、可点击分类的可读名称，不依赖颜色识别。 |

固定依赖继续Kotlin2.1.20、Compose BOM2025.04.01、Material3、Lifecycle2.8.7、Coroutines1.10.2。AndroidX为Apache-2.0，既有Wrapper和MoneyBook片段许可继续完整随包；本轮没有新增第三方源码或资产，因而没有新的外部文件哈希。官方最新示例不等于当前固定版本兼容，须实际编译。现有库维护提醒保留，不用顺手升级扩大测试面。

分析口径由本项目自行定义：金额始终整数分、百分比/均值用BigDecimal显示舍入；整期总览与同期窗口分别注明，当前月/年按相同经过天数比较，基期无记录不虚构增长百分比；无记录不代表没有消费。类别金额/笔数和前3笔仅是事实，不能自动判定浪费/异常。结余不称账户余额，且本轮不引入储蓄率/预算/账户/AI建议。

正式规格与可执行测试样本见[实施计划](superpowers/plans/2026-10-07-statistics-s2.md)。许可与来源审查不代替实际测试、独立复核或作者手机验收。

## 实现与证据的分工

S2按上述口径自行实现纯Kotlin分析和既有Compose页面，年度12桶单遍累计，精确分类键为期间、类型与Kotlin trim后的名称；金额、比较差额和平均分保持整数／BigDecimal。图形只将已计算整数金额转为相对柱高，文字清单仍保留准确金额。既有Material3色板、系统字体、native窗口日／夜底色及完整三份随包许可继续使用。

已获得纯逻辑与界面任务、整分支源码的独立批准；最终设备／升级／原图与原生APK包装结果以 [VALIDATION_V015](VALIDATION_V015.md)、[REVIEW_V015](REVIEW_V015.md) 的实际记录为准。桌面1k／10k／50k合成基准只说明本机纯逻辑开销，不推定Android帧率或真实手机响应。方法与许可证审查不能替代作者手机验收。

## 字体测试诊断的固定版本资料

本轮为解释实际font场景误判，只在ignored test-artifacts下载并审查官方Maven固定Compose1.8.0 foundation／ui-text sources，结合现有runtime javap与恢复配置前原PNG／布局数值；没有把源码加入应用、引入依赖或改产品字体。String Text语义parent-max重建与自然node宽度的此次误判已被实测绑定；ff1受限几何helper已在API35执行；7be新增钻取单行断言在新CI三字号测试实际通过，60张原PNG独审确认MAX负金额完整单行；RV-M1关闭。该结论仅适用审查过的自然宽度场景，不能扩展为所有文本都可忽略overflow。应用复用、版本和三完整随包许可合同保持。
