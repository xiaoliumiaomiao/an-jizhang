# 安记账

中文安卓本地记账软件，0.1.1测试版，支持Android8.0及以上。0.1.0已由作者确认验收全部通过。

- 收入/支出新增、编辑、确认删除，内置与自定义分类、日期、备注。
- 月份切换、收入/支出/结余、支出分类统计。金额始终使用整数分。
- 私有SQLite本地保存，不申请联网或广泛存储权限，无广告、遥测、自动云备份。
- 系统文件选择器导出明文JSON，恢复前完整验证并事务合并；重复记录跳过，同ID内容冲突整批拒绝。

项目规则见 [agent.md](agent.md)，实施和验证进度见 [docs/HANDOFF.md](docs/HANDOFF.md)。

## 安装与验证

本次debug测试APK为 `output/an-jizhang-0.1.1-debug.apk`，校验值在 `output/SHA256SUMS.txt`；原0.1.0包保留。新旧本机包已实际确认签名证书相同。也可以在[GitHub Actions](https://github.com/xiaoliumiaomiao/an-jizhang/actions)的成功运行中下载`android-build`产物用于独立验证；不同机器和不同CI运行可能使用不同签名，不能保证互相覆盖作者已安装的应用。

先在已安装的0.1.0中导出一份备份，再直接安装0.1.1覆盖旧版。确认原账目、分类、备注和月份统计保留，设置页显示版本0.1.1及备份容量提示。卸载会删除本地账本；导出是明文，请妥善保管。新[手机升级验收表](docs/PHONE_ACCEPTANCE_V011.md)待作者确认，原[0.1.0验收记录](docs/PHONE_ACCEPTANCE.md)已通过。

构建步骤见[DEVELOPMENT](docs/DEVELOPMENT.md)，本轮实际测试证据见[VALIDATION_V011](docs/VALIDATION_V011.md)，首版证据保存在[VALIDATION](docs/VALIDATION.md)。正式签名与应用商店发布另行决定。

本轮本机及[最终CI](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37213110853)的36项单元测试通过，CI另有6项Android15界面测试、旧版seed及新版verify各1项通过，均无失败或跳过；同签名0.1.0→0.1.1覆盖后全部账目字段和跨月统计保留。lint0错误/9提示，独立代码及修复增量审查通过。首次脚本输出格式问题及修复过程见验证记录，模拟器结果与待确认的新手机升级验收分别记录。

备份上限为整本50000条记录或10MiB，任一超限都会拒绝导出/恢复。当前没有分批导出；长期使用接近上限时应先保存合规备份，再安排容量扩展。单笔金额0.01至999999999.99元；首版人民币，不包含多账户、转账、云同步或应用锁。

## 复用与协作

先审查五个开源候选，实际只复用安卓官方Wrapper和MoneyBook MIT金额解析片段；来源、固定SHA、许可证、修改及不采用原因见[复用审查](docs/REUSE_AUDIT.md)及[NOTICE](third_party/NOTICE.txt)。完整许可证包含在APK设置页。

多subagent分工完成调研、环境准备、实现、安全检查及独立审查。每个实现任务均经过另一agent审查；发现的运行中重建问题已修复并复审。验证与作者验收分别记录。

0.1.1继续复用官方SDK和既有固定测试库，补充容量提示、6项JSON边界回归及同签名覆盖升级验证，没有增加依赖、权限或改变账本格式。[本轮复用决定](docs/REUSE_V011.md)、[独立审查](docs/REVIEW_V011.md)。
