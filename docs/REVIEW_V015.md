# 0.1.5／V1.0 S2 独立审查

日期：2026-10-07（Asia/Shanghai）。冻结候选`7be948c40c96e7fe0d75448e284ff52ee8e45c03`；作者手机验收另记。修复前ff1的Runtime／Visual结论仅作历史，不自动授予7be。

| 门禁 | 当前实际结论与证据 |
|---|---|
| Task1纯统计 | Source Spec／Quality Approved C0/I0/M1；`task1-review.md`，既有SDK告警 |
| PhaseA新增设备测试 | 修复后Spec／Quality Approved C0/I0/M0；`task2-tests-review.md` |
| Task2界面／状态／就绪 | Source Spec／Quality Approved；`task2-review.md`，后续增量见`whole-source-review.md` |
| 7be全分支Source | 增量Spec／Quality Approved C0/I0/M0，接续全分支审查时C0/I0/M2；`max-drill-layout-review.md`。既有工具告警保留，EOF空行已在正式文档修正并经整分支diff-check验证 |
| 7be候选native | Approved C0/I0/M0；`max-drill-layout-review.md`追加原生审查，本机APK与源码绑定 |
| 新CI build | CI37626498602 attempt1已完成111 JVM／0F0E0S，原81 missing0；lint0E9W，ROOT已实读 |
| 7be Runtime／Upgrade | Approved C0/I0/M0；CI37626498602 attempt1新device55 UI全0、seed／verify各1全0，原安装／签名／完整字段保持，`s2-final-7be-runtime-visual-review.md` |
| 7be Visual | Approved C0/I0/M0；59业务＋1升级原PNG逐件审阅，三字号MAX钻取金额完整单行，RV-M1关闭；同独审报告 |
| 最终FullDelivery | Pending；`delivery.json`已核120新副本hash、107旧物及16合同不变；文档后同步与独立FullDelivery审查仍待完成 |

## 源码合同与历史问题关闭

Task1以真实111项／29个AssertionError取得行为RED，再实现111全通过，原81逐名保留；桌面合成基准仅证明纯逻辑开销。Task2保留整数分、schema1／format1、完整账目统计、精确trim分类、同期明确窗口、年度12桶、稳定UUID及原operationId返回协议；不加网络、依赖、云同步或S3范围。旧35 UI名称与业务断言保留，但Smoke／Flow实际滚动、Query分类tag和Recovery真实IME交接有最小测试适配，不能称测试源码逐字节未动。

历史审查关闭链：PhaseA初审的失败PNG命名、排行采样与真实viewport问题由c46159b修复；d17／958的旧长页可见性和异步钻取就绪问题由db03的原10000ms真实reading／list屏障关闭。db03设备55项9失败重新打开源码测试门禁；a137精确分类scope、b3读取屏障、f0图表完整描述、分类排名重叠扫描、Top3焦点和Recovery IME修复后，Whole Source回到Approved。f0设备旧35全过但3个字体用例停于余额语义overflow；固定Compose源码、原配置PNG和节点／行几何把当次误判绑定到String Text父max重建。ff1窄范围同帧字符box校验获源码批准，attempt1因Pixel Launcher ANR设备失败；同SHA attempt2完整55 UI及升级通过，独立Runtime／Upgrade Approved、Visual Approved with Minor RV-M1。上述当时失败与已通过范围的原件均在 [VALIDATION_V015](VALIDATION_V015.md)，不能据后续GREEN倒写早期RED覆盖。

RV-M1是ff1的360dp font1.0／1.3 MAX分类钻取金额小数尾部折行，负号和所有数字完整，因此审查定为Minor；此前已实际逐图看59业务PNG和升级首页。7be仅在统计钻取调用复用纵排EntryRow，普通明细保持原布局；新的三字号helper在已有MAX字面检查后要求实际金额Text**恰一行、完整lineStart／lineEnd、无ellipsis或raw overflow**，原viewport／48dp和后续年图／返回断言仍在。`max-drill-layout-review.md`对4文件+23/-3增量给Source Spec／Quality Approved C0/I0/M0；**新单行断言未在旧横排生产代码上自动运行RED**，ff1原图是人工视觉失败证据。7be新run三字号用例与单行断言实际通过，独立逐张审阅新原图后RV-M1已关闭；不能把旧图称为新自动断言的RED。

## 当前原生材料与最终审查边界

7be本机APK已由独立原生审查读回：9,834,364 bytes，SHA256 `072afe980ecdbeaae398c4630d49ecc7a6374ef465e013b05d31b998aeb43c46`，源码tree `5008b8fd219621f4cad7f2a95c598eb8a92eb3df`。原生identity为0.1.5/code7/min26/target35；只有既有signature receiver权限，无INTERNET；allowBackup=false及legacy/cloud/transfer全域排除；三完整许可在APK、assets、third_party逐字节一致。新旧本机公开debug证书SHA256均`209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d`。冻结assemble实际7秒／37 UP-TO-DATE，不能写作clean重编译；此前37秒构建实际执行19项任务、111 JVM与lint。candidateNative结论另由新CI同临时证书code6→7真实升级及两output复制报告补齐，证据归属分别见`ci-s2-37626498602-attempt1`与`delivery.json`。

新设备55 UI与seed／verify原件、三字号单行金额和60张新原PNG已由`s2-final-7be-runtime-visual-review.md`独立核准；普通明细、钻取滚动／返回／编辑来源也有对应通过用例与原图，不以单张截图替代行为测试。`delivery.json`记录两处output各1 APK／1校验／59 PNG逐项hash相同、107旧物／16合同及主目录两作者文档原字节保持。REUSE与S2计划EOF空行已在本轮文档应用中修正，本地整分支diff-check exit0；private draft PR5、本机／远端／PR HEAD一致、clean及FullDelivery独立报告仍待ROOT完成。

API35合成模拟器不代替API26实机、作者自然手机、真实进程kill、完整TalkBack或font2-dark受控交叉。20项 [PHONE_ACCEPTANCE_V015](PHONE_ACCEPTANCE_V015.md)保持未勾。作者先导出0.1.4有效备份，再同签名正常覆盖安装；S2手机验收前不合并，S3和正式发布另行确认。
