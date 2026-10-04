# 0.1.3 成熟界面与安全复用决定

调研日期：2026-10-05。目标是重组现有功能入口和呈现方式；不复制商业软件代码、品牌、字体、图标或截图素材。以下只根据本次实际访问的官方公开资料提出方案，不把旧帮助图或 iPhone 截图描述成最新 Android 实机。

| 来源 | 可确认的通用模式 | 本轮采用与边界 |
|---|---|---|
| [钱迹主界面指南](https://docs.qianjiapp.com/home/index.html)、[收支录入](https://docs.qianjiapp.com/ji-zhang-jie-mian/tian-jia-shou-zhi.html) | 月收入/支出/结余、按日期倒序分组、独立添加、查看记录详情；新增修改共用字段 | 日期分组和详情；不采用云同步、资产、预算、日期点击补账。指南图含2018.08，仅作模式依据 |
| [Realbyte Android Home](https://help.realbyteapps.com/hc/en-us/articles/360046150874-Home-tab)、[Stats](https://help.realbyteapps.com/hc/en-us/articles/360045310273-Stats-Tab)、[备份恢复](https://help.realbyteapps.com/hc/en-us/articles/360042747934-How-to-backup-and-restore-data) | 查阅记录、月统计和数据维护分工，分类金额排行 | 明细/统计/设置三目的地；仍用现有支出分类统计，不引入账户、复式记账、复杂筛选或SQL恢复。Home示例图含2020 Sep |
| [鲨鱼官网](https://www.shayujizhang.com/)、[官方 App Store](https://apps.apple.com/cn/app/id1079718756) | 官方静态图中的显式底栏和醒目记账动作 | 单独“记一笔”动作及明确收支；不复制黄色品牌、插画、发现/会员/广告。商店图为iPhone，官网日历图含2023-06，未实测Android交互 |
| [随手记官方 App Store](https://apps.apple.com/cn/app/id372353614)、[隐私说明](https://www.sui.com/help/privacy.jsp) | “记一笔”、流水/报表及设置分工 | 行为命名和低频维护分区；不引入多账本、账户、AI或多人协作。商店来源为iPhone，未确认Android完整一级菜单 |
| [Android Compose Navigation bar](https://developer.android.com/develop/ui/compose/components/navigation-bar) | 紧凑屏幕适合3至5个主要目的地，标签和选中状态明确 | 沿用现有三职责，新增不是可选中页面；图标由本项目代码绘制 |

## 复用资产、版本与许可

继续使用已锁定的 Compose BOM 2025.04.01、Material3 1.3.2、Compose UI/Foundation 1.8.0、Activity Compose 1.10.1、Lifecycle 2.8.7（AndroidX Apache-2.0），以及现有 Kotlin、协程与平台 API。不增加 Navigation、图表、图标、字体或第三方运行时依赖；完整 Apache/MIT/NOTICE 继续随 APK 打包。

现有 MoneyBook MIT 金额转换片段和默认分类来源不变，固定 SHA 与修改记录仍见 [初版复用审查](REUSE_AUDIT.md)。本轮不导入竞品备份、搜索或存储模块。金额仍为整数分，schema1/备份format1、全本导出、严格验证及事务合并规则不变。

浅色 Material3 主题和原创图标以代码实现，可由 Git 回退；未下载商业图片或字体，也未执行第三方安装脚本。依赖、权限与打包许可证须在新包生成后实际复核，本文不预填通过。

## 证据边界

竞品资料提供信息架构与视觉模式，不能证明安记账的操作速度、可用性或作者偏好。新界面需通过现有业务回归、新导航/重建测试、窄屏大字实际渲染和作者手机验收。新技术验证与作者验收分别记录，未获得作者本轮确认前不合入main。
