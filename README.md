# 安记账

中文安卓本地记账软件，首版0.1.0，支持Android8.0及以上。

- 收入/支出新增、编辑、确认删除，内置与自定义分类、日期、备注。
- 月份切换、收入/支出/结余、支出分类统计。金额始终使用整数分。
- 私有SQLite本地保存，不申请联网或广泛存储权限，无广告、遥测、自动云备份。
- 系统文件选择器导出明文JSON，恢复前完整验证并事务合并；重复记录跳过，同ID内容冲突整批拒绝。

项目规则见 [agent.md](agent.md)，实施和验证进度见 [docs/HANDOFF.md](docs/HANDOFF.md)。

## 安装与验证

本次交付的debug测试APK为 `output/an-jizhang-0.1.0-debug.apk`，校验值在 `output/SHA256SUMS.txt`；也可以在[GitHub Actions](https://github.com/xiaoliumiaomiao/an-jizhang/actions)的成功运行中下载`android-build`产物用于独立验证。后续覆盖安装须用同一份本机debug keystore构建；不同机器和不同CI运行可能使用不同签名，不能保证互相覆盖。作者手机验收尚未进行，先按[手机验收表](docs/PHONE_ACCEPTANCE.md)检查。

首先在安卓手机安装测试APK，新增一笔0.01元餐饮支出。正确结果是列表出现该账目，本月支出0.01、结余-0.01。卸载会删除本地账本，操作前先导出备份；导出是明文，请妥善保管。

构建步骤见[DEVELOPMENT](docs/DEVELOPMENT.md)，实际测试证据见[VALIDATION](docs/VALIDATION.md)。正式签名与应用商店发布不在本次测试交付范围。

最终[CI验证](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37191001385)：30项单元测试、6项Android15模拟器界面测试全部通过，lint0错误/9提示；独立代码审查无阻塞项。模拟器不能代替作者手机验收。

备份上限为整本50000条记录或10MiB，任一超限都会拒绝导出/恢复。当前没有分批导出；长期使用接近上限时应先保存合规备份，再安排容量扩展。单笔金额0.01至999999999.99元；首版人民币，不包含多账户、转账、云同步或应用锁。

## 复用与协作

先审查五个开源候选，实际只复用安卓官方Wrapper和MoneyBook MIT金额解析片段；来源、固定SHA、许可证、修改及不采用原因见[复用审查](docs/REUSE_AUDIT.md)及[NOTICE](third_party/NOTICE.txt)。完整许可证包含在APK设置页。

多subagent分工完成调研、环境准备、实现、安全检查及独立审查。每个实现任务均经过另一agent审查；发现的运行中重建问题已修复并复审。验证与作者验收分别记录。
