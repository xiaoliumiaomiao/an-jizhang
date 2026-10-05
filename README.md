# 安记账

中文安卓本地记账软件，当前测试候选为 **0.1.3-fix1（versionCode 5）**，支持Android8.0及以上。0.1.0、0.1.1和0.1.2均已由作者确认验收通过；本轮界面重构、金额内置键盘与成功短提示已通过完整CI和独立源码、运行/视觉审查，最终交付审查、同步及作者手机验收仍待。

- 收入/支出新增、编辑、确认删除，内置与自定义分类、日期、备注。
- 月份切换、收入/支出/结余、支出分类统计。金额始终使用整数分。
- 私有SQLite本地保存，不申请联网或广泛存储权限，无广告、遥测、自动云备份。
- 系统文件选择器导出明文JSON，恢复前完整验证并事务合并；重复记录跳过，同ID内容冲突整批拒绝。

项目规则见 [agent.md](agent.md)，实施和验证进度见 [docs/HANDOFF.md](docs/HANDOFF.md)。

## 0.1.3 当前候选：界面重构与 fix1 修复

底部导航为明细／统计／设置，三个主页面统一“记一笔”。明细按日期倒序分组，点击账目进入详情，新增和编辑使用全屏表单；月统计采用分类金额与比例条，备份恢复、隐私、容量和许可集中到设置。查询原文、类型、月份和编辑草稿按状态契约保存，筛选只影响明细。

依据成熟软件官方公开资料重组入口，来源、年代/平台限制及采用范围见 [REUSE_V013](docs/REUSE_V013.md)。沿用固定 AndroidX/Material3 与既有 MIT 片段，布局和图标用项目代码实现，schema1、备份format1、整数分及依赖/权限保持原样。

fix1 将删除等成功反馈改为约3秒自动清除；旋转按原单调截止时间处理，旧消息回调不能误清新消息。金额使用表单内数字、小数点、退格和清空按钮，在末尾编辑；点金额不唤系统键盘，从备注切回金额会收起系统IME。备注继续使用系统输入法，金额仍经原精确校验后保存，不改变账目或备份格式。[本轮复用与取舍](docs/REUSE_INPUT_FEEDBACK.md)

受测源码 `ee9b718c6a7a6dc3b8a2ccd5d739196e61b07d65` 的 [CI37275582803](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37275582803) attempt1 两个job均成功：**60单元/19设备测试全部0失败、0错误、0跳过**，原55/16逐名保留，lint0错误/9原有提示。严格0.1.2/code3→0.1.3/code5同证书覆盖升级的seed/verify各1项及安装、启动通过，账目字段与跨月统计保留。11张本轮原生场景图及升级首页已逐张查看，独立源码与运行/视觉审查Approved，C0/I0/M0；最终完整交付审查和同步尚待。[实际验证](docs/VALIDATION_INPUT_FEEDBACK.md)、[本轮审查](docs/REVIEW_INPUT_FEEDBACK.md)

当前本机fix1包已通过身份、公开签名、权限及完整许可审计，并以新文件名复制到两个output；旧7个APK与原校验前缀保留。两处 `output/v0.1.3-fix1-screenshots` 各保存11张本轮原始PNG，22副本与本次CI源图hash相同，旧18图保留。作者[本轮9项手机清单](docs/PHONE_INPUT_FEEDBACK.md)全部待验，[原0.1.3十三项](docs/PHONE_ACCEPTANCE_V013.md)保持原状态。私有草稿[PR #4](https://github.com/xiaoliumiaomiao/an-jizhang/pull/4)继续待作者确认；main保留已验收0.1.2，不合并或正式发布。

模拟器正常字号实际覆盖所有金额键；fontScale2的图显示全部13键可见，保存/取消滚动操作通过，不代表每种字号都逐键点击。明细初屏末行仍可能部分被FAB遮住，正常字号滚动末行后完整文字/金额可见且有几何断言；保留此观察，不称初屏遮挡已修复。模拟器与作者手机结果分别记录。

### 修复前 0.1.3 技术证据（保留）

修复前受测代码 `fb9ffd89d81e51c04716b86319416f900ad4e564` 的 [CI37236054231](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37236054231)已完整通过：55单元/16设备/旧版seed及新版verify均无失败、错误或跳过，lint0错误/9提示；同证书0.1.2→0.1.3覆盖升级保留全部字段及跨月统计。九张原生场景截图已取回并由ROOT逐张查看，统计页旧帧问题已取得正确新图；当轮独立源码、视觉/运行、实物副本和最终技术交付审查均Approved，Critical0、Important0。[当轮验证](docs/VALIDATION_V013.md)、[当轮审查](docs/REVIEW_V013.md)。这些历史证据与原0.1.3包保留，不替代当前fix1的验证或手机验收。

## 0.1.2 已验收迭代

源码已增加所选月分类或备注的字面关键词搜索，可组合全部类型、仅收入、仅支出，并一键清除两个条件。导航、切月和Activity重建保留查询输入及类型；筛选只影响账目列表，月统计和整本导出继续使用完整数据。源码版本为0.1.2/code3，数据库schema1、备份format1、依赖和权限保持原样。

新增14项纯逻辑与四项真实UI测试均有有效RED/GREEN，原六项回归保留。[最终CI37220141121](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37220141121)在0076b6b代码上实际50 JVM、10 UI及旧版seed/新版verify全部通过，真实0.1.1→0.1.2替换保留完整账目与跨月统计。本机APK签名/权限/完整许可及两处复制哈希已核验；[PR #3](https://github.com/xiaoliumiaomiao/an-jizhang/pull/3)已在作者验收后合并，源码及最终技术交付独立审查已通过。详见[本轮验证](docs/VALIDATION_V012.md)、[独立审查](docs/REVIEW_V012.md)和[手机验收表](docs/PHONE_ACCEPTANCE_V012.md)。

## 安装与验证

当前本机debug候选文件为 `output/an-jizhang-0.1.3-fix1-debug.apk`（0.1.3/code5），校验文件为 `output/SHA256SUMS-v0.1.3-fix1.txt`；主目录 `D:/AIproject/an-jizhang/output` 也有同一副本。包大小9458013 bytes，SHA-256为 `cc131a6a3624e44b82f96589d399af7c2d3b33cbafe87780377f34f51ab12f15`，与旧本机包使用同一公开签名证书。原0.1.0/0.1.1/0.1.2及修复前0.1.3包、旧校验文件保留。

待ROOT完成本轮交付确认后，先在手机现有应用中导出整本备份，再正常安装本机同签名fix1/code5覆盖现有应用，按[本轮手机验收清单](docs/PHONE_INPUT_FEEDBACK.md)验证。安装报错保留原应用和账本；卸载会删除数据，不卸载/清库/强制降级。备份为明文，请保存到可信位置；旧版通过不继承为本轮结果，原0.1.3手机表不代勾或重置。

[GitHub Actions](https://github.com/xiaoliumiaomiao/an-jizhang/actions)的`android-build`产物用于独立技术验证；本机与CI签名可能不同，CI包不用于覆盖作者手机账本。

构建步骤及当前0.1.3开发路径见[DEVELOPMENT](docs/DEVELOPMENT.md)，已交付0.1.1实际测试证据见[VALIDATION_V011](docs/VALIDATION_V011.md)，首版证据保存在[VALIDATION](docs/VALIDATION.md)。正式签名与应用商店发布另行决定。

0.1.1本机及[最终CI](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37213110853)的36项单元测试通过，CI另有6项Android15界面测试、旧版seed及新版verify各1项通过，均无失败或跳过；同签名0.1.0→0.1.1覆盖后全部账目字段和跨月统计保留。lint0错误/9提示，独立代码及修复增量审查通过。首次脚本输出格式问题及修复过程见验证记录，模拟器结果与作者确认通过的手机升级验收分别记录。

备份上限为整本50000条记录或10MiB，任一超限都会拒绝导出/恢复。当前没有分批导出；长期使用接近上限时应先保存合规备份，再安排容量扩展。单笔金额0.01至999999999.99元；首版人民币，不包含多账户、转账、云同步或应用锁。

## 复用与协作

先审查五个开源候选，实际只复用安卓官方Wrapper和MoneyBook MIT金额解析片段；来源、固定SHA、许可证、修改及不采用原因见[复用审查](docs/REUSE_AUDIT.md)及[NOTICE](third_party/NOTICE.txt)。完整许可证包含在APK设置页。

多subagent分工完成调研、环境准备、实现、安全检查及独立审查。每个实现任务均经过另一agent审查；发现的运行中重建问题已修复并复审。验证与作者验收分别记录。

0.1.1继续复用官方SDK和既有固定测试库，补充容量提示、6项JSON边界回归及同签名覆盖升级验证，没有增加依赖、权限或改变账本格式。[本轮复用决定](docs/REUSE_V011.md)、[独立审查](docs/REVIEW_V011.md)。

0.1.2沿用现有Kotlin标准库、Material3及状态保存API，不复制MoneyBook搜索模块或引入新库；固定来源及采用边界见[REUSE_V012](docs/REUSE_V012.md)。本轮源码与技术测试已通过，最终技术交付独立审查通过；作者已于2026-10-05确认手机校验通过。
