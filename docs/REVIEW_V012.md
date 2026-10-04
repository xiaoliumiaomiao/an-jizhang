# 0.1.2 独立审查记录（技术交付通过，待手机验收）

日期：2026-10-05（Asia/Shanghai），最初文档准备快照01:06:52；本记录已按实际最终报告更新。结论按实际返回报告的范围记录，不用实现者自检代替独立审查。当前Source Spec/Quality Approved、Critical0/Important0；Final technical Delivery Approved。最终CI设备GREEN、真实升级和两处output后置条件已取得实际证据；私有draft PR #3及三方HEAD同步已实核，最终delivery独立报告已返回Approved，作者手机验收仍未执行。

## 实现与独立审查分工

| gate | 实现者 | 实际独立reviewer | 报告及真实结论 |
| --- | --- | --- | --- |
| Task1纯筛选及14项测试 | `/root/v012_filter_logic` | `/root/v012_filter_review` | `test-artifacts/sdd-v012/task-1-review.md`：Spec/Quality Approved，Critical0/Important0 |
| Task2阶段A测试夹具初审 | `/root/v012_query_ui` | `/root/v012_filter_review` | `task-2-red-review.md`：Spec Approved、Quality Changes requested，1项Important重复IME逻辑 |
| Task2阶段A修复复审 | `/root/v012_query_ui` | `/root/v012_filter_review` | `task-2-red-fix-review.md`：Spec/Quality Approved，原Important已消除 |
| Task2阶段B UI接入 | `/root/v012_query_ui` | `/root/v012_filter_review` | `task-2-ui-review.md`：Spec/Quality Approved，Critical0/Important0；不代替设备GREEN |
| Task3阶段1升级链同步四文件 | `/root/v012_upgrade_prep` | `/root/v012_upgrade_review` | `task-3-upgrade-review.md`：Spec/Quality Approved，仅固定基线/版本/目录同步 |
| 完整源码分支 | 上述实现者，ROOT集成 | `/root/v012_final_review` | `final-code-review.md`：Source Spec/Quality Approved，Critical0/Important0；Final delivery Pending |
| 候选APK包装 | ROOT构建 | `/root/v012_delivery_prep` | `apk-candidate-audit.md`及8份工具原始输出：精确候选SHA的包信息/签名/权限/备份排除/完整许可符合要求；ROOT已实读 |
| 三行保存提交观测 | `/root/v012_query_ui` | `/root/v012_final_review` | `task-2-dispatch-observation-review.md`：Spec/Quality Approved，原断言/10秒/业务不变，根因不反推 |
| APK复制脚本快照 | ROOT准备 | `/root/v012_delivery_guard_review` | `apk-copy-script-review.md`：Spec/Quality Approved；执行前script SHA75f...，不等于已执行 |
| APK后置条件与compact补审 | ROOT执行/核验 | `/root/v012_delivery_guard_review` | `apk-delivery-compact-review.md`：Approved；原序列化审查遗漏已纠正，ReadAllLines一行fix未重跑，实际新旧hash/prefix/唯一新行相符 |
| 最终交付证据与文档 | ROOT收口 | `/root/v012_final_review`后续独立审查 | `final-delivery-review.md`：Source Spec/Quality与Final technical Delivery Approved，Critical0/Important0；作者手机gate仍待执行 |

表中无前缀报告均位于`test-artifacts/sdd-v012/`。stage tree不是提交；ROOT核验被审暂存树与随后提交树一致：Task1 `d16513239a92542a5db41fc666fd132c2f6b344f`、Task2修复`9fe45c1e0e1a83f7700b30661fd34afe8f53f3bf`、Task2 UI `5842ce90453b59f9dd5af1410fbe81c19af514b6`、Task3 `aa5adefe86c24c0efd4051cb9ad3b867d9ad86b4`。完整源码范围为已验收BASE `976817290302e712519297cc5b810ce019d2d383`→代码HEAD `8f1c4c902ff387ad1755b4d25600cff0ddcb5e8c`。

## 发现、修复及复审历史

Task2阶段A初审保留了计划规定的四项真实UI行为，但发现LedgerQueryTest与原Smoke逐行复制整块IME关闭/等待/flags恢复逻辑。按审查约定列为Important，不能因代码来自计划而忽略。ROOT实读后调整tests-only维护范围，抽取internal ComposeTestRule.closeKeyboard共用扩展，两组测试均委托；真实Back、10000ms等待、finally恢复和全部业务断言保留，没有直接修改筛选状态让测试通过。12秒修后编译成功，独立复审Spec/Quality Approved；原Changes requested文件保留，没有覆盖失败历史。

Task1审查指出SDK XML v4/v3及SQLite library already loaded两项Minor诊断，Task2复审亦要求最终证据列出SQLite提示。ROOT已实读本轮及976基线同XML并补记实现报告；两次MainActivityTest均1项通过。当前本轮验证文档继续如实保留，不称输出无警告，不扩张工具安装或数据库改动。

Task3 reviewer为核对暂存树误执行了一次git write-tree，违反该轮无Git写操作限制；结果与ROOT提供树相同，未改变HEAD/索引内容、源码或做commit/push。该过程偏差已报告ROOT并保留在独立报告，与四文件规格/质量结论分开记录，不改写为全程无操作偏差。

## 完整源码规格与质量结论

`/root/v012_final_review`完整读取规则、Global Constraints、三任务、差异和原始证据后实际返回Source Spec/Quality Approved。纯筛选保留所选月、分类OR备注、类型AND、trim/字面ignoreCase及原顺序/对象/输入；UI顶层rememberSaveable、raw输入无200字截断、计数/双空态、清除不改月、CRUD派生刷新符合契约。

顶部和统计页继续计算完整state.entries/月，导出仍读取完整数据库；未迁移原editing/pending IDs或完成消费，也未改生产VM/DB/Backup/Editor、schema/format、依赖或权限。原六Smoke业务/10000ms及新增四Query真实输入/滚动/IME/重建保留，VM仅用于只读同步。升级链固定976基线0.1.1/code2→0.1.2/code3，保留同证书三包、-w -r、唯一OK与completion=-1、失败扫描、单QEMU/ci-ledger、timeout和中途不清库/卸载等全部门禁。

reviewer实读纯逻辑RED14/12/0/0→GREEN14/0/0/0→全50；旧UI CI为10/4/0/0且四项均缺搜索控件、原六通过；本机UI接入50/0/0/0与lint0 Error/9 Warning、fixture22秒和语法exit0；候选公开证书/权限/许可原始证据也已读取。这些回答源码gate，不替代当前最终CI设备GREEN或作者手机结果。

## 当前非阻塞诊断与剩余gate

Critical：无。Important：无。现存Minor为SDK XML工具v3/遇v4、lint8条固定依赖更新提示及1条UseKtx、SQLite library already loaded。继续保留诊断，后续维护另行验证。

ROOT最后本机完整构建19秒、3执行/75 UP-TO-DATE，JVM并未重新执行50项；`final-apk-freeze.json`记录包9408857 bytes、SHA `8c82dbc30e482232b537868174ffb1aa2c5ec1e9c878420ee9fb7857b381ae42`与候选完全相同。原包装证据只针对该精确哈希可复用；不是output复制/最终交付完成结论。

首次完整CI37218934264确实失败，其原始失败/诊断保留。三行观测增量Approved后，最终[CI37220141121](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37220141121)在0076b6b11de015a81f7c2f4e2337f9408ccd9a41实际50 JVM/10 UI/seed1/verify1全0，字段/统计/同CI证书/安装启动/退出及截图已独立实读；本机同审计SHA已复制两处，旧文件/旧校验字节不变。PowerShell报告附加元数据膨胀已保留raw并生成基本类型compact记录，未改变APK；单行ReadAllLines报告修正没有重跑复制。最终/root/v012_final_review已完整读取正式文档、原始CI与实际APK/清单，并live核验private draft PR #3和三方HEAD/main边界，实际返回Final technical Delivery Approved。

[PHONE_ACCEPTANCE_V012](PHONE_ACCEPTANCE_V012.md)八项全未勾、技术与手机结果分开；作者本轮反馈尚未提供，不继承0.1.1通过，不合main或正式发布。极端长文本Bundle边界、手机字号/键盘体验仍待本轮实际验收，不添加未经需求确认的200字产品限制。

最终独立审查前同步快照：2026-10-05 01:39:40，[PR #3](https://github.com/xiaoliumiaomiao/an-jizhang/pull/3)实际OPEN/draft且已附，repo private=true；本机、远端和PR HEAD均ed803ff4415c0ded54881817c0ca3ae4627081b2，工作区干净、main仍已验收9768172。0076b6b之后仅文档变化。本次补充实际PR/补审事实也只改文档，最终commit/push后仍须核验同步。

最终报告final-delivery-review.md覆盖ed803ff4415c0ded54881817c0ca3ae4627081b2与四份仅文档事实补充，Source Spec/Quality/Final technical Delivery均Approved，Critical0/Important0；ROOT实际list_artifacts再次核验#3附件。原失败根因不反推、报告序列化遗漏及补审纠正、全部既有诊断和八项未勾手机表均保留。本次只把已返回结论及技术步骤完成项写回文档；最后commit/push后的三方HEAD、clean/private/draft/main与实物再核验记录在ignored delivery-sync-final.json，不重编译应用。
