# 最终独立代码审查

日期：2026-10-04。基线 d7ecaf1；结束 811e7bf7f4bed7236afe3bb619906f5af277a052。

## 结论

- **规格门禁：通过源码审查。** 已确认的首版收支、分类、月份统计、私有持久化、系统备份与合并恢复均有实现，没有发现阻塞性规格缺失。
- **质量门禁：Approved。阻塞项 0：Critical 0，Important 0。** 下列 2 项 Minor 不阻塞本次测试交付。
- **可交付测试 APK：是。** 主 agent 完成最终交付 APK 的签名/权限/许可核对、复制和 SHA-256 记录，并准确记录当时 CI 结果后，可交给作者安装验收。不要求作者先验收才允许提供测试包。
- 此结论不代表正式发布、所有设备兼容性或作者手机验收通过。设备 CI 运行 37186675598 的执行结果及作者手机验收由主 agent 分开记录；本审查未把运行中 CI 当作成功。

## 范围与方法

完整读取 agent.md、docs/HANDOFF.md、docs/REQUIREMENTS.md、docs/ARCHITECTURE.md、实施计划、progress 和两个任务的实现/独立审查报告。ARCHITECTURE 实际位于 docs/ 下。核对 review-final.diff 文件清单，并以当前最终源码独立检查 domain、data、MainActivity、四个 UI 文件、Manifest/排除备份规则、单元/设备测试、构建和 CI、复用通知及交付说明。

使用只读 git diff 811e7bf... -- app .github scripts build.gradle.kts settings.gradle.kts gradle.properties gradle/wrapper 确认所查实现/构建文件相对指定结束提交无变化。README/VALIDATION 是主 agent 正在补充的交付记录，其未完成的最终证据不误报为实现缺陷。未改源码、index、HEAD 或分支；仅写本审查报告。没有机械重跑已通过的全套测试。

## 独立核查结果

- Money.kt:8-24：输入正则、BigDecimal 精确换算、longValueExact 和上限共同避免浮点金额/静默截断；格式化同样不转 Double。MonthlySummary.kt:6-24：月隔离、分类汇总、安全加减和负结余符合合同。统计条形图的 Float 只用于视觉比例，没有参与账本金额计算。
- LedgerRules.kt:6-11：规范 UUID、正金额/上限、分类修剪后长度、备注长度与年份验证。LedgerDatabase.kt:31-37：私有 SQLite、参数化写入、date DESC/id ASC 稳定排序；升级显式报缺少迁移，不删除重建。
- LedgerDatabase.kt:44-69：整批验证/去重先于事务；同 ID 相同内容跳过，冲突抛异常，finally 结束尚未成功的事务，前面已插入的记录一起回滚。对应真实 SQLite 测试把冲突放批次末尾验证原子性。
- BackupCodec.kt:17-68：格式/币种/版本、严格字段和类型、整数金额、日期、数量/UTF-8 大小限制齐备；独立 JSON 语法门禁拦截 org.json 宽松扩展、重复字段、尾部垃圾和过深嵌套。
- LedgerViewModel.kt:29-78：后台 Mutex 串行访问；有错误状态、finally 释放 busy；导入最多读 MAX_BYTES+1，严格 UTF-8 解码、完整 decode 后才 merge。导出先编码验证，再打开输出流；打开/读写失败不修改原账本。数据库关闭与工作共用锁。
- LedgerViewModel.kt:35-49、LedgerScreen.kt:28-36/123-125：保存/删除返回唯一 ID，成功刷新后持久发布完成 ID；当前 Composition 使用 saveable pending IDs 消费匹配结果。Activity 重建不会再由 ViewModel 调用旧界面闭包，失败不会冒用旧成功，重新打开编辑器清 pending。过程死亡后可以重新尝试相同记录 ID，避免保存重复记录。
- EntryEditor.kt:22-58、LedgerScreen.kt:56-117：收入/支出、新增编辑、确认删除、自定义分类、日期/金额验证、按月列表/汇总/分类统计、设置与备份说明均存在；草稿 saveable、滚动及 IME padding 有实现。屏幕尺寸/字体的视觉质量仍需要 Android 执行和作者手机证据。
- MainActivity.kt:27-32/49-56：Activity Result API 连接系统文件选择器，取消 null URI 不启动恢复/导出；导出前说明明文风险，恢复前说明合并/冲突策略。
- Manifest 与两个 XML：无 INTERNET 或广泛存储权限，allowBackup=false，云和设备迁移均排除私有账本；生产路径未加入网络/遥测/广告库。assets 中保留第三方许可和通知，设置页可读取。
- CI：actions 固定 SHA、contents:read、checkout 不持久化凭据；固定官方 SDK 工具独立初始化，构建与模拟器任务分离，失败也上传证据。代码可读性/环境风险没有达到阻塞级别。设备 job 未重复 wrapper hash 检查，可作为将来供应链一致性加固，不声称当前有遭篡改证据。

## 发现

### Critical

无。

### Important

无。

### Minor 1：整本备份容量边界未在使用界面提前说明

位置：app/src/main/java/com/xiaoliumiaomiao/anjizhang/data/BackupCodec.kt:17、:28、:59；data/LedgerDatabase.kt:35；ui/LedgerScreen.kt:115。

可复现场景（静态确定，本次未构造五万条大库）：先导入一个合规的 50000 条小记录备份，再新增一条；保存允许继续增长，但整本 encode 会拒绝 50001 条。长备注也可能让未满 50000 条的账本先达到 10 MiB。此时已有账目仍完好，但界面直到尝试导出才显示无法备份，设置页只提示更换设备/卸载前先导出。

这是需求已规定的备份上限与长期账本容量之间的可用性边界，不是本次小规模测试 APK 的阻塞项。建议至少在交付限制和设置说明中公开“整本备份最多 50000 条/10 MiB”；将来扩充容量时设计可验证的分批导出/恢复或明确账本容量策略，不为本次验收临时扩展范围。

### Minor 2：自写 JSON 语法门禁的少数边界缺直接回归

位置：app/src/test/java/com/xiaoliumiaomiao/anjizhang/BackupCodecTest.kt:15-19；data/BackupCodec.kt:85-104。

现有测试已覆盖非整数、错类型、超限字节、重复 ID、未知版本、非法日期和多种非标准 JSON，但未直接覆盖重复对象字段（包括转义后同名）、超过 8 层嵌套、decode 入口的 50001 条记录。源码能静态确认这些输入被拒绝；建议后续用命名清晰、分行的定向测试保护此自写解析边界。当前没有发现拒绝逻辑失效，也不要求为消除 style 提示改生产实现。

## 证据与限制

直接读取当前 app/build/test-results/testDebugUnitTest/TEST-*.xml：BackupCodec 6、LedgerDatabase 5、LedgerViewModel 11、MainActivity 1、Money 5、Summary 2，共 30，failures/errors 均为 0。直接解析 app/build/reports/lint-results-debug.xml：9 个 Warning，没有 Error。直接计算 wrapper jar SHA-256 为 c5a643cf80162e665cc228f7b16f343fef868e47d3a4836f62e18b7e17ac018a，与记录/CI 校验值一致。这些是已存在构建产物的实读，并非本审查重新执行测试。

六个设备 smoke 测试源码已检查，覆盖基础导航、非法金额/一分钱记账、输入/页面重建、运行中保存删除重建、失败保留草稿和重选分类；本审查不把编译成功称为 Android 执行成功。系统文件 provider 的真实交互、IME/字体/Insets、作者自然手机验收仍待实际证据。SDK XML 和固定版本 lint 提示已有交付限制说明，未发现因提示导致的应用缺陷，不把提示重复提升为阻塞项。

## 追加复审：CI 模拟器磁盘与等待修复

范围：主 agent 在 811e7bf 之后尚未提交的 .github/workflows/android.yml 最小差异；仅复审，没有修改工作流、应用、HEAD 或提交。

**结论：Approved，新增 Critical 0 / Important 0。可提交此修复并重跑 CI；尚不能声称设备测试通过。** 原有可交付测试 APK 的结论不变，最终文档应使用新运行实际结果。

直接读取 test-artifacts/ci-37186675598/android-device/emulator.log：Android Emulator 37.2.12.0，GPU compatibility 检查通过，最终 FATAL 为创建 userdata 磁盘不足；可用 8370.43 MB、需要 12288.00 MB。故修复依据是已观察到的磁盘错误，不是推测 GPU 模式或 AVD 名称错误。另独立读取该运行下载的六个 TEST XML，合计 30 项、0 failures/0 errors；lint 文本明确 0 errors, 9 warnings。该 build 成功不代表被取消的 device job 通过。

核查本次差异：

- avdmanager 将新建测试 AVD 明确写入 RUNNER_TEMP/ci-ledger.avd，随后只修改此新 AVD 的 config.ini 数据分区为 4G。没有删除文件、覆盖作者设备或改应用数据库；--path 与随后 config.ini 路径一致，AVD 注册名称仍由 -avd ci-ledger 使用。
- 移除现有同名 disk.dataPartition.size 行再追加单一配置值；与 -no-snapshot 一起降低本次临时测试设备磁盘需求。4G 是否足够支持该镜像启动/安装仍应由新 CI 实测，静态审查不伪造成功证据。
- adb wait-for-device 现在有 180 秒上限；启动属性查询和后续 adb 操作有 10 秒上限；循环内检查 emulator PID，进程死亡会失败退出。即使 adb 查询反复卡住，Prepare emulator 的 12 分钟步骤上限仍可约束该阶段，并相对 25 分钟 job 上限为后续诊断上传留出空间。
- EXIT trap 在非零退出时保留进程、adb 和最近 emulator 日志；ps/adb 诊断失败不会掩盖原始操作失败，函数没有强制 exit 0。emulator.log 仍在 if: always() artifact 路径。Actions 强制取消时 EXIT trap 并非绝对保证执行，因此主要持久证据仍是已持续重定向写入的 emulator.log 和上传步骤；不把 trap 描述为任何取消下必定成功的收集机制。
- -verbose、版本/加速检查/AVD 列表和 df 输出可帮助下一次运行区分环境失败；参数均针对临时 CI 环境，没有扩大 GitHub 权限或引入新 action/依赖。

本追加为代码和已有证据的只读审查，未在本机启动模拟器，未重复全套 JVM 构建；六项设备测试须等待修复后新运行的实际 XML/报告。仍须保留作者手机验收未完成状态。

## 追加复审：CI Linux 运行库与并行任务（首次检查）

范围：.git/sdd/review-ci-runtime.diff 及对应当前 .github/workflows/android.yml。没有修改工作流或 HEAD。

实读 test-artifacts/ci-37187393827/failed-job.log：本次在 emulator -version 调用底层 qemu-system-x86_64 时，动态加载器报告 libpulse.so.0 缺失并退出 127，尚未启动 AVD 或进入设备等待。因此这轮结果不能证明 4G userdata 修复成功或失败。实读该运行下载的 XML：30 项、0 failures/errors；lint 报告 0 errors, 9 warnings。

固定 ubuntu-24.04 可减少 runner 主版本漂移，但不是不可变镜像。sudo apt-get update 与 --no-install-recommends libpulse0 仅影响一次性 CI runner，未关闭 apt 的正常源/签名验证，且明确补充已观测缺失库；没有更改用户本机。去掉 needs 后两个 job 各自 checkout 同一触发提交、安装工具和编译，没有消费对方产物，整体 workflow 仍需分别观察两项结果；并行化不存在数据/产物依赖错误。设备 job 运行 Gradle 前增加同一 wrapper SHA 校验、artifact 收集 emulator-libs.log 均合理。

**首次检查发现 1 Important（CI 误阻塞风险），应用源码 Critical/Important 数量不变：** .github/workflows/android.yml:83-85 直接对底层 QEMU 执行裸环境 ldd，并把任何 not found 判为失败，未模拟 emulator 启动器添加的库搜索路径。

官方 [AOSP emulator launcher 源码](https://android.googlesource.com/platform/external/qemu/+/emu-master-dev/android/emulator/main-emulator.cpp) 在启动底层进程前调用 updateLibrarySearchPath 和 Qt 环境配置；[官方开发文档](https://android.googlesource.com/platform/external/qemu/+/emu-master-dev/android/docs/DEVELOPMENT.md) 也明确启动器会加入自带 lib/lib64 库目录。因此裸 ldd 的环境与后面的 emulator -version 不等价。复现条件是发行包依赖仅由启动器路径找到的 bundled 库时，裸 ldd 报 not found，而正常启动器能够解析；当前 exit 1 会在真正验证启动器之前提前失败。这是依据官方加载代码确认的检查前提错误；本次没有运行该 Linux 二进制，不声称已在此 runner 实际复现哪一个 bundled 库误报。

建议最小修复：把裸 ldd 仅作为非阻塞诊断（保留 stdout/stderr），删除对任意 not found 的拒绝；保留真实 emulator -version / -accel-check 的失败退出门禁。不要为绕过检查而忽略真实 launcher 启动错误，也无需为诊断复制容易漂移的整套 launcher 路径逻辑。修复后复查此几行即可重跑 CI，新的六项设备测试结果仍待实际验证。

### Linux 运行库修复复审结果

主 agent 修改后再次读取更新的 review-ci-runtime.diff 与当前工作流：.github/workflows/android.yml:83-88 现在明确将裸 ldd 作为诊断，stdout/stderr 一起写入 emulator-libs.log，并只对 ldd 使用 || true；随后 emulator -version 和 -accel-check 仍直接执行，由 Actions bash -e 保留真实非零退出失败。原先 grep not found 的拒绝逻辑已移除，没有把忽略错误扩大到真实启动命令。

**复审 Approved。该 Important 已修复，当前未解决 Critical 0 / Important 0。** 可提交此 CI 修复并执行新运行。此次仅做差异/源码复查，没有再次运行 JVM 或在本机安装 Linux 库。main application 与 APK 的原审查结论不受此 CI-only 变动影响；4G AVD 实际启动和六项设备测试仍未因本复审而视为通过，须以新 CI 结果收尾。主 agent 报告 Bash -n/git diff --check 已通过，此处不冒称审查者重跑了这两项命令。

## 追加复审：设备测试就绪等待与失败证据收集

范围：.git/sdd/review-device-fixture.diff、af801da 的 AndroidTest 变更，以及当前尚未提交的 CI 诊断差异。只读检查实现，没有修改应用、测试、工作流、HEAD 或提交。

**结论：Approved；新增 Critical 0 / Important 0。** 可以提交诊断变更并推送新运行。此次允许继续验证，不代表设备 GREEN；启动就绪竞态仍是与源码/日志相符、待新 CI 验证的修复假设。

直接读取 run37187887902 的 failed-job.log，确认 Android 15/API35 已真正开始并完成 6 项测试，3 项失败：databaseSaveFailureAfterRecreationKeepsEditorInputs、reselectingExpenseKeepsCustomCategory、saveAndDeleteCompleteAfterActivityRecreationAndOldResultDoesNotCloseNewEditor。失败信息均在 perform text input 时缺少金额或分类节点；原测试源码的这些操作均位于第一次打开编辑器之后、重建/故障注入之前。因此不能把这轮失败说成运行中重建断言失败，也不能仅凭另 3 项通过宣称整个设备验证通过。

检查 af801da 和现行 LedgerSmokeTest.kt：readyEntryAction 等待同时具有“记一笔”文本、click action、enabled 的语义节点，最多 10 秒，随后仍 assertIsDisplayed/assertIsEnabled；openEditor 执行真实点击，再最多等 10 秒要求金额节点存在。与 LedgerViewModel 初始 busy=true、Dispatchers.IO 异步读库及按钮 enabled=!busy 的源码关系一致。修改没有使用 retry 整个测试、捕获吞掉断言、跳过测试、直接改业务状态或延长所有断言时间；只补齐用户可操作就绪条件。6 个 @Test 均保留，原金额错误/一分钱、页面输入重建、Mutex 暂停与保存删除重建、失败保留草稿、旧结果隔离、自定义分类断言均在。首次打开之后的实际行为断言没有降级。

直接读取 build-20261004-161626.log，见 compileDebugAndroidTestKotlin、dexBuilderDebugAndroidTest 和 packageDebugAndroidTest 实际执行，最终 BUILD SUCCESSFUL；不是只声称旧测试 APK 已编译。只读 git diff af801da^ af801da --stat 确认提交仅变更一个 AndroidTest 文件；app/src/main 差异为空。

CI capture_device_evidence 是 EXIT trap，在记录原始退出状态后，用有限 30 秒/15 秒分别收集 logcat/截图，并列出已生成设备测试结果文件。只有诊断命令使用 || true；connectedDebugAndroidTest 没有 continue-on-error、|| true 或退出码覆盖，Actions bash -e 在 Gradle 失败时仍中止主体。trap 内也没有 exit 0。审查者额外使用本机 Git Bash 做了最小同构探针：set -e 下主体 false，EXIT 函数执行诊断且最后成功返回，整个进程仍 exit_code=1；证明这种 EXIT trap 不会将原始失败改为成功。该探针没有运行真实 Gradle/adb。

artifact 的 if: always() 和新 logcat/结果清单路径均与写入文件名一致；已有测试 XML/HTML 路径保留。此修复增加定位资料，不保证任何强制取消、磁盘故障或进程杀死时一定能上传全部诊断，也没有把缺报告当通过。若新运行仍失败，应检查实际 XML/日志和截图，继续区分测试同步、业务行为与环境故障。
## 最终交付与截图流程只读复审（2026-10-04）

最新build报告实读30项JVM/0失败/0错误，lint9Warning无Error；output APK与本机构建APK、SHA256SUMS及核验JSON一致，3个license assets逐一hash匹配源码。README/HANDOFF/VALIDATION已把更新安装条件精确改为同一份本机debug keystore，不保证不同CI运行互相覆盖。此项复核阻塞0。

设备37188539169实际6项5过1超时；失败截图为Gradle卸载应用后的Launcher，不能视为测试失败瞬间。截图流程增量独立审查Approved，新增阻塞0：connected任务成功后才重新安装本job刚构建的固定debug APK路径，安装/启动各有60s/30s超时并保留失败退出码。Gradle失败仍直接EXIT诊断，不会被后续步骤变成成功。此复审不等于剩余设备失败已修复。

48c136c诊断夹具独立审查Approved，可发CI取证，新增阻塞0。实读XML/logcat确认旧唯一失败位于原line82保存锁释放后的金额表单消失等待，未到删除阶段。新child检查在Mutex持有时对比点击前Job集合，初始化已就绪且没有其他UI动作，旧job不能充当保存/删除提交证据。重建后重新取当前Activity的VM，assertSame真实。原6case、所有业务/重建断言及dialog-close10秒均保留；捕获记录后重抛，没有生产hook或吞掉失败。本机日志证实test Kotlin/APK实际重编译，设备结果仍待CI。

b36fb81滚动夹具独立审查Approved，可推送再CI，新增阻塞0。实读37189385205设备XML6项/1失败，保存删除重建case无failure，logcat有save/delete submitted和保留同一VM的新Activity；唯一失败为旧line60错误提示assertIsDisplayed。新diff通过真实performScrollTo带入视口并保留visible断言，增加amount仍0、entries空，再滚动金额改0.01；后续记录/月支出断言、6case和10秒均保留，无sleep/retry/skip。源码支持滚动修正，报告未把IME当已证实根因；设备GREEN仍待新运行。

c02c1ea键盘夹具独立审查Approved，可推CI，新增阻塞0。先确认TYPE_INPUT_METHOD存在，再一次真实BACK，等消失，finally恢复原UiAutomation flags。之后检查金额12.34仍在，误关Dialog会明确失败；保存增加Displayed/Enabled再真实点击，不绕过UI。原awaitSubmitted、6项、10秒、同VM/保存删除/旧结果均保留，开放键盘0.01场景未改。最新实际失败已定位为NOTsubmitted/jobs0，不能据此断言生产生命周期缺陷或精确点击瞬间的IME因果；夹具是否有效仍待新CI。仅测试文件，无新依赖或生产hook。

## 最终设备与交付证据复核

最终运行37191001385成功后，另一agent再次只读复核下载的XML、lint、运行日志、APK和当前交付文档，结论通过、阻塞0。实际30项JVM和6项Android15/API35设备测试全部通过，0失败/0错误/0跳过；lint0错误/9提示。日志确认测试后安装成功、MainActivity启动Status: ok、退出码0。交付APK SHA256与记录一致，生产实现相对811e7bf无变化。作者手机验收仍未勾选；设备通过不替代真实文件provider、字体和自然使用验收。复核原记录保存于忽略的`.git/sdd/review-delivery-final.md`。
