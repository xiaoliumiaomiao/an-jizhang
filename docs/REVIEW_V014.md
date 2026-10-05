# 0.1.4／V1.0 S1 独立审查

日期：2026-10-05。最新受测源码 `3bf514249f6a920d2c3d8f89299830f72520eb82`，固定已验收main基线 `ff8479f75cd917bc49e5ab2173ad7e9906dc480c`。每个实现任务由其他agent审查，完整分支与交付由独立最高能力审查者复核。作者手机验收、合main和正式发布不在技术批准内。

| 门禁 | 最新结论 |
|---|---|
| Task1草稿／最近分类规格与质量 | Approved；14项真实RED→77 JVM全通过 |
| Task2界面与状态规格／质量 | Approved；原行为合同、直接语义与即时防重已复核 |
| 专项测试与readiness完整增量 | Approved，C0/I0/M0；原35项／19项和严格断言保留 |
| 完整分支＋连续增量Source Spec／Quality | Approved，C0/I0/M0 |
| 最终真实Runtime／Visual | Runtime Approved / Visual Approved，C0/I0/M0 |
| 本机APK与两处实物／文档／同步FullDelivery | Pending final technical FullDelivery |

## 审查范围

完整主包ff8479f→3f2599d实际345045字符、5455行、51变更文件，全文读取；被工具截断部分单独补读。包含0.1.3导航／详情／全屏表单、成功消息修复和本轮S1。随后连续增量fbef最近类别定位、9f语义／同轮防重测试、0c55采图／版本定位、121d1ea／c06生产修复及月图门禁、4fdc／519两个测试就绪文件及203b77f同一UI轮读取几何的test-only增量，以及3bf5142窗口背景三资源／两个测试文件增量均全文复核，未以实现者描述或局部片段代替整分支审查。

另读取最新作者反馈、V1.0归档／S1计划、Task1／Task2及专项测试报告，定点核对未变Money／规则／DB／Codec／搜索字面合同。16项不可变manifest、备份排除、DB、Codec、domain、Wrapper及许可hash／长度一致。ignored审计／复制／CI核验脚本也完整静态附审；其执行结果与视觉必须另核，脚本下限／通过标志不替代人工读证据。

## 发现与关闭

整分支初审C0/I2/M0。I1金额只有pointer Release，辅助功能OnClick只获焦而不能输入；I2有效busy被提前算成Composition快照，同轮第二次save／delete和cancel可绕过锁。先加三项独立test-only回归并实际得到RED，121d1ea的最小修复让触摸／语义共享入口、回调执行时读取当前busy及pending ID，子级rememberUpdatedState保持最新守卫；操作结果匹配、稳定ID、旧草稿合同未改。源码增量复审Approved，新真实设备三回归全通过，源码与运行分别关闭。

旧测试的最近分类重复文本定位仅限定recent-categories祖先，保留字段内容断言；尚未compose的版本项使用既有LazyColumn查找，保留原名称／显示断言／等待。专项测试提出的几何、直接IME、逐键值和读取中断言已补齐；早期RED之后补强的测试没有冒称在原RED执行。

4fdc测试就绪增量初审C0/I1/M1：异步焦点回调异常可能逃出UI线程，备注waitUntil的ComposeTimeoutException直接继承Throwable而被catch AssertionError漏诊断。519以AtomicReference＋原latch交回测试线程，以及Throwable诊断／suppressed／重抛原错修正。c06→519完整2文件69增加／8删除，生产c06不变；同一个总10000ms预算内等待焦点及硬件frame，唯一当前根、同已提交窗口检查仍保留，没有重复tap／固定sleep／误差放宽。冻结519 Source／Test Spec／Quality Approved，活动C0/I0/M0后才push最终CI。

519的CI37295325938原19全部通过，35仅font2失败。原始几何日志与原PNG明确：初始viewport836／button852，滚动后row和viewport854／button870，目标整行没有覆盖，失败误用旧button852。最终增量保留初始严格检查，用同一UI轮的postscroll当前边界严格检查row和viewport均在当前按钮上方；没有容差、重复滚动或等待加长。布局移动18px原因以及更早缺现场的font2、c06备注点击失败根因未完整证实；后续GREEN不能反推这些原因。原失败和新实际bounds／诊断均保留，见 [VALIDATION_V014](VALIDATION_V014.md)。已有SDK XML3／4兼容提示和8 GradleDependency＋1 UseKtx归类为工具维护／已有建议，未见本次引入的安全漏洞证据，不blind升级或suppress；lint0 Error／Fatal。

203b77f的CI37298056158真实79／35及升级全通过，独立Runtime Approved C0/I0/M0；23场景图及升级首页逐张复核时另发现Visual I1：深色应用主体正确，但系统状态栏#FAFAFA浅底配近白时间与图标不可读。当时Visual Needs revision C0/I1/M0，ROOT没有审计或复制APK。原图未裁剪，后续修复及新实际结论另核；静态Light父主题与edge-to-edge组合为候选，不能仅靠绿色自动测试忽略原图缺陷。

根因随后以SDK35 Light资源链、固定activity1.10.1 AUTO透明系统栏实现、native PNG采色和两个真实window回归共同证实：native窗口旧FAFAFA与night白图标配对。3bf5142最小生产修复styles.windowBackground及日／夜两颜色资源；未改Activity、图标策略或manifest。完整5文件源／测试增量Approved C0/I0/M0；两新JVM先81／2F有效RED→81全GREEN，Robolectric sdk26模型不称实机。原35的night用例追加同全幅PNG背景／时钟前景对比及两栏flags合同，没有裁图、删原断言或延长10秒。新CI37301576955真实设备与全部24原图已获Runtime/Visual Approved C0/I0/M0，I1据新证据关闭，原reject不改写。

## 最终运行和视觉证据

独立实读 [CI 37301576955](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37301576955) exact SHA的81 JVM／35 UI原XML及全部身份：原77／19保留，16 S1与两privacy实际执行、无失败／错误／跳过。三新目标、严格三字号geometry和13键输入值均核；同job code3→6升级三个公开cert一致、三个安装Success、两原始runner唯一OK／completion=-1，全部字段／schema1／UUID／跨月统计及启动保留。

ROOT和独立reviewer逐张读取本轮23场景原生PNG与升级首页，核1900／1月选中、浅深、金额面板、普通／1.3／2.0字、明细末行、弃稿、最近分类、导出警告与旧主要路由。大字面板最后行键需要内部滚动；测试夹具的saved UI＋new VM和直接语义点击不等于真实进程kill或完整TalkBack自然验收。API26实机及作者手机没有本轮实测，19项手机清单保持未勾。

最终37301576955的新原PNG中，native状态栏高度136px；深色背景143314/146880像素匹配、时钟区contrast>=3共1272像素；浅色143240/146880及884。原35用例内两栏foreground flags断言通过。ROOT及独立最高审查者逐张实读24张新原图，Runtime和Visual均实际Approved C0/I0/M0，203旧Visual I1在新证据关闭，旧拒绝原件保留。

## APK、文档与同步

本机clean exact testedHEAD重新绑定assembleDebug，原生aapt／apksigner与ZIP核包名、0.1.4/code6、min26／target35、唯一应用内signature receiver权限、自动备份全域排除、旧本机公开debug证书和三完整许可。新APK 9918020 bytes、SHA256 `5f71a8bf1a9c1e617b6a87dd6852ee45c2e6cdf18b084dbf070381c34c656af6`；双output的APK／独立校验内容及46原图副本逐项hash读回。原57文件和16合同保持，作者主目录两文档及main未改。

最后技术交付审查读取当前README／DEVELOPMENT／HANDOFF／验证／审查／手机表、实际本机APK与副本、新CI原件／图、local／remote／PR HEAD和private／OPEN／draft同步。本节最终结论 Pending final technical FullDelivery，仅在审查者实际返回后写回；最后写回结论的文档提交和同步另作只读后置核验，不改变受测源码。

私有 [PR #4](https://github.com/xiaoliumiaomiao/an-jizhang/pull/4) 保持草稿，main为ff8479f已验收0.1.2。下一步作者先导出备份，正常覆盖安装本机同签名包执行 [19项手机验收](PHONE_ACCEPTANCE_V014.md)。S1手机通过再进S2；分类库、预算、format2及正式发布另行确认。
