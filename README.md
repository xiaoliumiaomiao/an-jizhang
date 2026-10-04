# 安记账

中文安卓本地记账软件，当前候选0.1.3，支持Android8.0及以上。0.1.0、0.1.1和0.1.2均已由作者确认验收通过；0.1.3界面与功能拓扑重构正在完成技术验证，手机验收尚待。

- 收入/支出新增、编辑、确认删除，内置与自定义分类、日期、备注。
- 月份切换、收入/支出/结余、支出分类统计。金额始终使用整数分。
- 私有SQLite本地保存，不申请联网或广泛存储权限，无广告、遥测、自动云备份。
- 系统文件选择器导出明文JSON，恢复前完整验证并事务合并；重复记录跳过，同ID内容冲突整批拒绝。

项目规则见 [agent.md](agent.md)，实施和验证进度见 [docs/HANDOFF.md](docs/HANDOFF.md)。

## 0.1.3 当前迭代：界面和入口重构

底部导航为明细／统计／设置，三个主页面统一“记一笔”。明细按日期倒序分组，点击账目进入详情，新增和编辑使用全屏表单；月统计采用分类金额与比例条，备份恢复、隐私、容量和许可集中到设置。查询原文、类型、月份和编辑草稿按状态契约保存，筛选只影响明细。

依据成熟软件官方公开资料重组入口，来源、年代/平台限制及采用范围见 [REUSE_V013](docs/REUSE_V013.md)。沿用固定 AndroidX/Material3 与既有 MIT 片段，布局和图标用项目代码实现，schema1、备份format1、整数分及依赖/权限保持原样。

候选代码 `086caba8699a64d67cd393f7da5901f5875bac31` 的 [CI37234298023](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37234298023)已完整通过：55单元/16设备/旧版seed及新版verify均无失败、错误或跳过，lint0错误/9提示，覆盖升级字段和跨月统计保留。九图实际取回，但统计图抓到切页前设置画面，正在补测试截图绘制同步；完整技术交付审查仍待。[验证](docs/VALIDATION_V013.md)、[审查](docs/REVIEW_V013.md)、[本轮手机表](docs/PHONE_ACCEPTANCE_V013.md)。作者确认本轮前保持main为已验收0.1.2，不合并或正式发布。

## 0.1.2 已验收迭代

源码已增加所选月分类或备注的字面关键词搜索，可组合全部类型、仅收入、仅支出，并一键清除两个条件。导航、切月和Activity重建保留查询输入及类型；筛选只影响账目列表，月统计和整本导出继续使用完整数据。源码版本为0.1.2/code3，数据库schema1、备份format1、依赖和权限保持原样。

新增14项纯逻辑与四项真实UI测试均有有效RED/GREEN，原六项回归保留。[最终CI37220141121](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37220141121)在0076b6b代码上实际50 JVM、10 UI及旧版seed/新版verify全部通过，真实0.1.1→0.1.2替换保留完整账目与跨月统计。本机APK签名/权限/完整许可及两处复制哈希已核验；私有草稿[PR #3](https://github.com/xiaoliumiaomiao/an-jizhang/pull/3)已创建并同步，源码及最终技术交付独立审查已通过。详见[本轮验证](docs/VALIDATION_V012.md)、[独立审查](docs/REVIEW_V012.md)和[手机验收表](docs/PHONE_ACCEPTANCE_V012.md)。

## 安装与验证

本机debug测试APK为 `output/an-jizhang-0.1.2-debug.apk`，校验值在 `output/SHA256SUMS.txt`；原0.1.0/0.1.1包保留。新旧本机包已实际确认签名证书相同。也可以在[GitHub Actions](https://github.com/xiaoliumiaomiao/an-jizhang/actions)的成功运行中下载`android-build`产物用于独立验证；不同机器和不同CI运行可能使用不同签名，不能保证互相覆盖作者已安装的应用。

先在现有0.1.1中导出整本备份，再直接安装本机同签名0.1.2覆盖旧版，并执行[本轮手机验收表](docs/PHONE_ACCEPTANCE_V012.md)。安装报错保留原应用和账本；卸载会删除数据，不卸载/清库/强制降级。备份为明文，请保存到可信位置。[0.1.1手机记录](docs/PHONE_ACCEPTANCE_V011.md)已于2026-10-05确认全部通过，原[0.1.0记录](docs/PHONE_ACCEPTANCE.md)已通过；不继承为本轮结果。

构建步骤及当前0.1.2开发路径见[DEVELOPMENT](docs/DEVELOPMENT.md)，已交付0.1.1实际测试证据见[VALIDATION_V011](docs/VALIDATION_V011.md)，首版证据保存在[VALIDATION](docs/VALIDATION.md)。正式签名与应用商店发布另行决定。

0.1.1本机及[最终CI](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37213110853)的36项单元测试通过，CI另有6项Android15界面测试、旧版seed及新版verify各1项通过，均无失败或跳过；同签名0.1.0→0.1.1覆盖后全部账目字段和跨月统计保留。lint0错误/9提示，独立代码及修复增量审查通过。首次脚本输出格式问题及修复过程见验证记录，模拟器结果与作者确认通过的手机升级验收分别记录。

备份上限为整本50000条记录或10MiB，任一超限都会拒绝导出/恢复。当前没有分批导出；长期使用接近上限时应先保存合规备份，再安排容量扩展。单笔金额0.01至999999999.99元；首版人民币，不包含多账户、转账、云同步或应用锁。

## 复用与协作

先审查五个开源候选，实际只复用安卓官方Wrapper和MoneyBook MIT金额解析片段；来源、固定SHA、许可证、修改及不采用原因见[复用审查](docs/REUSE_AUDIT.md)及[NOTICE](third_party/NOTICE.txt)。完整许可证包含在APK设置页。

多subagent分工完成调研、环境准备、实现、安全检查及独立审查。每个实现任务均经过另一agent审查；发现的运行中重建问题已修复并复审。验证与作者验收分别记录。

0.1.1继续复用官方SDK和既有固定测试库，补充容量提示、6项JSON边界回归及同签名覆盖升级验证，没有增加依赖、权限或改变账本格式。[本轮复用决定](docs/REUSE_V011.md)、[独立审查](docs/REVIEW_V011.md)。

0.1.2沿用现有Kotlin标准库、Material3及状态保存API，不复制MoneyBook搜索模块或引入新库；固定来源及采用边界见[REUSE_V012](docs/REUSE_V012.md)。本轮源码与技术测试已通过，最终技术交付独立审查通过；作者已于2026-10-05确认手机校验通过。
