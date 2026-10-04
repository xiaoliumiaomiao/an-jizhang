# 0.1.3 独立审查记录

状态：分任务及最终源码增量审查已完成，技术交付证据审查进行中；作者手机验收待执行。

实现者与审查者分开，原始报告保存在 ignored `test-artifacts/sdd-v013/`。下列 Approved 仅表示所列范围获得批准，不将静态检查冒充设备或作者验收。

| 范围 | 独立结论与记录 |
|---|---|
| 正式需求/计划 | 初审发现 Important：IO 排队尚未设置 busy 时可能重复提交；设计改为同步 pending ID 锁与明确 failedOperationId，增量规格审查 Approved |
| Task1 日期分组/主题 | Spec / Quality Approved，Critical 0、Important 0；4 个新增 JVM 用例实际 RED/GREEN，原 50 保留 |
| Task2A 新 UI 测试及截图路径 | Spec / Quality Approved，Critical 0、Important 0；旧 UI 的 3 个新增入口用例后续实际 CI RED，原 10 通过 |
| Task2B 根状态、三个页面、详情/全屏 Editor、提交保护 | Spec / Quality Approved，Critical 0、Important 0；55 JVM 通过与 AndroidTest 编译，实际设备验证待 CI |
| Task3/4 窄屏测试、固定基线、版本/升级脚本及截图取回 | 静态 Spec / Quality Approved，Critical 0、Important 0；保留 CI-only guard、唯一设备、证书与原始 runner 门禁；实际设备证据待 CI |
| 最终全源码 d375cdc | Needs revision，Critical 0、Important 1：F1，新 VM 恢复已保存 pending ID 时缺少执行者，可能永久锁定 |
| Recovery 两个测试的准备 | Spec / Quality Approved，Critical 0、Important 0；真实 queued Job、新 VM 和保存的 Compose UI 状态、同 ID / sentinel DB 断言。只完成编译，不是真实进程 kill 或仪器 GREEN |
| F1 owner 最小修复增量 | Source Spec / Quality Approved，Critical 0、Important 0；两文件7行、39保护文件未变，实读55 JVM及新候选APK。F1静态闭合，实际恢复与交付仍待 |
| 截图工具与恢复选择器修正 | Spec / Quality Approved，Critical 0、Important 0；两 AndroidTest 文件限定改动、39保护文件未变、11秒编译成功。保留CI-only guard、真实字节后置检查和全部业务断言，等待CI实际GREEN |
| 第二次搜索输入的真实点击 | Spec / Quality Approved，Critical 0、Important 0；一行测试动作修正，40保护文件与生产APK未变，10秒编译通过。独立实读IME show→用户Back→hide→无新show的原日志；保留helper/等待/全部业务断言，待真实GREEN |
| 候选七张图片部分视觉检查 | 详情图缺失，完整视觉未批准；发现明细初屏最后一条金额被FAB覆盖，需实际滚动可见性证据。正常/窄屏Editor操作可见，不替代作者自然验收 |
| 末行滚动与完整文字几何补证测试 | Spec / Quality Approved，Critical 0、Important 0；19行限定补证、pinned API统一Dp、完整文字在可视视口且与FAB不相交；40保护文件和生产APK不变，编译10秒成功，真实几何和第9图仍待CI |
| 最终可见性候选 e30c02d 源码增量 | Source Spec / Quality Approved，Critical 0、Important 0；最能力审查者独立比较冻结diff、40保护文件、完整原断言与APK哈希；真实运行、视觉、升级及Delivery仍待 |
| CI37231650011九张原生图 | 视觉范围Approved；末行正常字号滚动后金额/分类/备注完整且与FAB分离，详情字段/编辑删除可读。初屏FAB遮挡历史观测仍保留，不外推大字列表或手机验收；整体16项2失败、升级未执行，FullDelivery未批准 |
| 取消Dialog与保存点击的原生事件同步 | 两测试限定增量Spec / Quality Approved，Critical0、Important0；pinned API、主窗口焦点/页面条件等待、真实IME收起/按钮可见可用及失败诊断。39保护文件和生产APK不变，23秒编译成功；原全部业务/10秒断言保留，实际GREEN仍待 |

F1 的最小处理为 VM 实例身份与 root 保存的 pendingOwner；恢复到不同 VM 时仅清旧提交锁，保存草稿和浏览上下文，同 VM 旋转继续保护在途操作。完整本机构建和源码增量已独立批准。检查点CI37228768041实际原10 UI通过，删除恢复在disabled处取得真实F1 RED；保存恢复在文字选择器歧义处提前失败，4个Flow/窄屏用例在截图工具处提前失败。两个测试工具修正的独立复审与真实GREEN、最终UI/升级/原生截图、最终APK/私有PR及同步证据仍待，当前不批准交付或合main。

候选CI37229801074实际16/1失败，原10、Recovery2、LargeText1、Flow2均通过；唯一Flow第二次搜索输入键盘准备失败，详情缺图、升级未执行。上述真实点击已通过独立审查，下一次CI加最后行滚动及金额不被FAB遮挡的断言/第9图；当前不批准完整交付或合main。

最新CI37231650011完整几何断言通过且九图齐全，独立视觉范围批准；Flow在Dialog取消后原生Back发向旧窗口导致列表未出现，Smoke非法错误节点未出现需检验IME触摸准备假设。原生事件同步修正正在编译与复审，不修改生产校验，不因图片通过而批准完整交付。

实际验证及边界见 [VALIDATION_V013](VALIDATION_V013.md)，作者手机表 [PHONE_ACCEPTANCE_V013](PHONE_ACCEPTANCE_V013.md) 保持未勾。
