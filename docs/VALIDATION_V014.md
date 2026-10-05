# 0.1.4／V1.0 S1 实际验证

日期：2026-10-05（Asia/Shanghai）。本轮技术交付状态：Pending final technical FullDelivery；作者手机验收待执行，19项未勾。V1.0为方案版本，应用为0.1.4/code6，保持私有draft PR4，不合main或正式发布。

## 最终受测来源与结果

冻结提交 `3bf514249f6a920d2c3d8f89299830f72520eb82`，源码与测试均经过独立审查。最终 [CI 37301576955](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37301576955) 两job完成/success，ROOT及独立审查者读取完整run、原始日志、XML和原生PNG。证据位于忽略的 `test-artifacts/sdd-v1-s1/ci-s1-final-37301576955/`，不是只凭绿色job标志判断。

| 检查 | 实际结果 |
|---|---|
| JVM／SQLite／JSON | 81 tests，0 failure／error／skip，11 suites |
| 常规安卓界面 | 35 tests，0 failure／error／skip；原19及新增16 S1逐名核对 |
| 原单元身份 | 原77名称保留；非法备份日期／kind两项隐私回归均通过 |
| Android lint | 0 Error／Fatal；9 Warning（8 GradleDependency、1 UseKtx） |
| 覆盖升级 | 固定ff8479f的0.1.2/code3→0.1.4/code6；seed、verify各1 test通过 |
| 升级runner | 原始与规范化一致，各唯一OK(1 test)、completion=-1；exit0 |
| 安装／启动 | 三次安装分别唯一Success；新版真实安装身份code6；启动Status: ok |
| 运行／视觉 | 独立审查 Runtime Approved / Visual Approved，C0/I0/M0；ROOT逐张查看23场景原图及升级首页 |

同一升级job的旧应用、新应用和测试包原生公开签名一致，证书SHA256为 `1eb79d16613cb73416a202b60d901a1237709e212f66d430dc5ffe34961deb29`。seed之后没有清库、卸载、再connected、重seed或降级。真实PackageManager读取旧0.1.2/code3、新0.1.4/code6；verify打开再重开SQLite，schema1、全部三条记录及UUID／类型／整数分／分类原文／日期／中文换行备注保留。2026-10收入10000分、支出1分、结余9999分；2026-09支出1234分、结余-1234分，分类统计一致。ROOT实读两阶段完整原始runner、三个certificate与安装文件、badging、launch、result和summary。

## 范围与行为

作者最新反馈：“成功提示自动消失已通过；金额键盘要弹出式，页面式不通过”。本轮S1包括按需底部金额面板、Back先收面板、备注／分类／日期与系统IME交接、草稿基线与放弃确认、空分类必填、按收支分开的最近6类、折叠备注、年月选择、系统浅深主题、真实内容viewport避开全局记账按钮。保存／删除即时guard读取当前busy和pending ID，辅助功能语义动作与触摸共享金额打开入口。

保留整数分、schema1／format1、稳定ID、搜索字面语义、完整月统计、保存失败草稿和原结果operationId匹配。UI草稿六原始字段使用独立Saver，不用备份Codec存UI，也不把整本账目放Bundle。详情／编辑／删除按稳定ID读取当前记录，读取中与完成后缺失分开，已不存在的记录不能由旧编辑器重建。

没有新增依赖、联网或广泛存储权限、广告、遥测、云同步、分类库管理表或预算。S2月度／年度统计等S1手机验收；S3分类库／预算／schema2／format2另行确认。

## 真实失败与修复历史

| 证据 | 当时实际结果与处理 |
|---|---|
| Task1行为RED→GREEN | 77 JVM／14F→77／0F；原60通过，新增17覆盖草稿与最近分类 |
| CI37280091275／61789d9 | 28 UI／9F，原19通过；77 JVM／1F，暴露S1与导出失败提示差距 |
| CI37281957639／11eaf45 | 31 UI／12F，原19通过；旧inline面板、详情快照、容量及viewport真实RED |
| 备份错误隐私本机 | 79 JVM／5F→79／0F；三项原断言加强及两项日期／kind隐私回归 |
| CI37286704226 | Prepare emulator退出124，UI与升级未执行；不是应用行为RED |
| CI37287399267／fbef3dc | 79 JVM全0，32 UI／1F：版本项尚未compose，旧直接performScrollTo无法定位；13 S1通过，升级未执行 |
| CI37290125922／0c55fcd | 79 JVM全0，35 UI／4F：金额语义不开、同轮save／delete各排队2 worker，以及font2行几何失败；原19通过，升级未执行 |
| CI37292564978／c06b100 | 79 JVM全0，35 UI／2F：第一次add点击添加备注后找不到field（是否成功展开未证实）、弃稿显示通过后截图focused窗口0；三目标和严格font2通过，升级未执行 |
| CI37295325938／519105c | 79 JVM全0，35 UI／1F：原19全通过，唯一font2使用旧按钮852与滚动后行854比较；当时当前按钮870／视口854，原图无重叠，升级未执行 |
| CI37298056158／203b77f | 79 JVM／35 UI／seed1／verify1全部0，两job success；三档同轮geometry通过。但24原图独立视觉复核发现深色系统状态栏浅底配白字（I1），APK审计及复制未放行 |
| native窗口回归／3bf5142 | 本机81 tests／2F真实RED（night／notnight均得到FAFAFA）→81／0F GREEN；3资源修复窗口日F5F7FB／夜101820，保留edge-to-edge AUTO，原35夜间测试增加全幅状态栏像素及两栏图标检查 |
| 最终CI 37301576955 | 精确3bf514249f6a920d2c3d8f89299830f72520eb82，81 JVM／35 UI／seed1／verify1全0，实际新图与升级原件独立复核 |

早期几何、直接IME交接、三字号逐键值、受控详情读取中等增强是在前两轮RED之后加入，不冒称已在最初RED执行。三个同步／语义回归先只提交测试，再以真实35项RED证明问题：直接Semantics OnClick后panel仍隐藏；持真实Mutex，同一主线程轮次save或delete两次各新增2个worker，期望1。121d1ea修复语义打开及即时guard，c06追加月份选中等待；三个回归在其后实际通过。

旧版本项定位仅加既有LazyColumn.performScrollToNode辅助，保留版本0.1.4、原显示断言和名称。旧font2仅有viewport=(0,169,720,836)，失败row／button现场不足，根因未证明；固定Compose1.8.0源码已否定“performScrollToNode只保证部分出现”的解释。先保留严格断言并补原／当前bounds及失败图，没有重复滚动、误差放宽或等待加长。c06的font2实际row.bottom854≤原／当前button.top870；不能据其GREEN倒推旧失败原因。

519真实失败取得完整现场：初始viewport.bottom836／button.top852，滚动后viewport.bottom854／row.bottom854／button.top870。误比较854>852失败，两个实际阶段分别保留16px间距，ROOT及独立reviewer读取原PNG确认目标完整行在按钮上方。本轮证明跨布局时刻使用旧Rect造成假失败；布局为何移动18px及更早缺现场的失败根因仍未知。最终测试保留初始严格viewport≤初始button；滚动后把row、当前viewport及当前button边界在同一UI轮读取，严格检查row≤当前button及当前viewport≤当前button。没有容差、重复滚动或额外等待；新增当前视口检查保持遮挡合同，修的是比较时刻。

c06第一次新增备注失败，stack为add():91／测试:144，不是第二add或edit():151。原生日志显示分类替换后IME正在show，但没有证实触摸是否送达，所以不定性为已查明的应用展开bug。4fdc／519仅改两个测试文件：既有IME显示→Back隐藏门禁后再原真实点击，检查添加／收起文本及备注出现，没有重复点击；失败诊断保留原异常。截图等待attached／shown窗口取得焦点，再frame commit，两阶段共享原10000ms总预算，保留唯一焦点、硬件和同窗口后置断言。异步callback异常用AtomicReference＋同latch交回测试线程，诊断失败suppressed后重抛原错。独立复审C0/I0/M0后才推最终CI，在readiness和203几何测试增量阶段，生产c06未变化。下一GREEN不证明旧备注点击或font2完整根因。

37285027905、37285208315等取消运行不计成功。旧失败XML／raw／原图和历史报告均保留，没有删测试、过滤失败或延长原10秒上限来获得GREEN。

203的本轮原生深色图可见时间／图标低对比。独立读取SDK35资源链，Light父主题windowBackground最终为FAFAFA；固定activity1.10.1的enableEdgeToEdge按uiMode选图标、API29+状态栏透明，所以露出旧Light背景。夹具已有night切换、recreate、配置等待与idle，归因不是缺重建。3bf仅styles的windowBackground引用及日／夜颜色资源三项生产修复，没有修改Activity、Insets flags、manifest或协议。两项MainActivityTest在Robolectric sdk26模型中先确认native night配置及ColorDrawable，再因旧真实FAFAFA得到RED；当时后续透明／flags断言未执行。资源修后81全通过，包括这些后续断言；Robolectric不等于API26实机。原systemNightMode测试保留名称／主体断言／10s／恢复，新增同张未裁剪全幅PNG真实statusInset范围背景>75%、左2/5时钟区contrast≥3像素>20及status/navigation foreground flags。该统计不声称OCR、所有字形或所有设备WCAG证明。

最终37301576955的新原PNG中，native状态栏高度136px；深色背景143314/146880像素匹配、时钟区contrast>=3共1272像素；浅色143240/146880及884。原35用例内两栏foreground flags断言通过。ROOT及独立最高审查者逐张实读24张新原图，Runtime和Visual均实际Approved C0/I0/M0，203旧Visual I1在新证据关闭，旧拒绝原件保留。

## 本机工具与构建

固定JDK21.0.12.1+1、Android Platform35／Build-Tools35.0.0，沿用既有.tooling/toolchain.json，不改系统PATH或安装新依赖。实际命令：

```powershell
& .\scripts\build.ps1 -Tasks @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest')
```

121d1ea的build-20261005-174728.log四任务27秒成功，19执行／63 UP-TO-DATE；79原XML全0、lint0E9W。c06的补全build-20261005-175107.log16秒，3执行／79 UP-TO-DATE。两个readiness本机补验各15秒，7执行／75 UP-TO-DATE，编译新AndroidTest且读取同生产源79项既有结果，不称重跑79。203b77f的build-20261005-183818.log同样15秒，7执行／75 UP-TO-DATE，重新编译AndroidTest，生产79项XML沿用既有结果。3bf5142的本机四任务22秒成功，28执行／54 UP-TO-DATE，81 XML全0，lint0E9W。最终远端build在新runner真实执行81项。SDK XML3／4兼容告警保留，8固定依赖更新建议和1既有DB UseKtx提示未隐藏或盲目升级。

本机交付包在clean精确受测HEAD再执行assembleDebug，前后tree／dirty和ignored业务输入核验，日志 `test-artifacts/sdd-v1-s1/apk-audit/frozen-local-build.log`，tree `c7ef6104ed6a716cc91a1d20792af95018880cf0`；实际6秒成功，37任务均UP-TO-DATE，仅重新核验同源APK，不称重跑测试。最终仅文档提交不改变受测生产／测试文件，精确同步详情另存ignored delivery-sync-final.json。

## 原生 APK 和实物副本

`an-jizhang-0.1.4-debug.apk`：9918020 bytes，SHA256 `5f71a8bf1a9c1e617b6a87dd6852ee45c2e6cdf18b084dbf070381c34c656af6`。公开本机证书 `209fe650f40d48d32167be84b0d59576ecc3fc90c3af4169fc811a13e700109d` 与旧本机包一致，apksigner原生验证通过；CI与本机签名分别记录。

aapt实际身份com.xiaoliumiaomiao.anjizhang、0.1.4/code6、min26／target35。唯一uses-permission为应用自己的签名级AndroidX动态receiver保护 `com.xiaoliumiaomiao.anjizhang.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，不称“完全无权限”；无INTERNET或广泛媒体／存储权限。allowBackup=false，旧备份五域及cloud／device transfer共十域全部path="."排除。APK ZIP中的Android-Architecture-Samples-LICENSE、MoneyBook-LICENSE、NOTICE.txt三完整文件与app资产及third_party逐字节／hash一致。

工作区output与 `D:/AIproject/an-jizhang/output` 各有新APK、独立 `SHA256SUMS-v0.1.4.txt` 和 `v0.1.4-screenshots` 的23张本轮原始PNG。46个图片副本逐项hash等于本次CI源图，两个APK及校验内容读回一致，拒绝覆盖不同字节的目标。原57文件（主目录作者两文档＋旧output共30，隔离旧output27）前后长度／SHA256不变；16个manifest／备份／DB／Codec／domain／wrapper／许可合同不变。主目录main仍ff8479f，原HANDOFF修改与未跟踪V1.0方案保留，归档方案原字节和原checkbox未改。

## 视觉和验收边界

ROOT及独立reviewer实际读取最终全部23张场景原PNG和升级首页：明细、末行、详情、编辑、统计、设置、空筛选、成功消息消失、普通／大字号、金额面板、浅深主题、年月、最近分类、弃稿确认及导出警告。月份图实际年份1900且1月选中；旧35RED图仍高亮10月的历史观察保留，不沿用为新结果。三字号在临时720×1280／320dpi AVD精确360dp检查真实viewport、行和按钮关系，13键逐个实际输入／值验证，finally还原尺寸、密度、字体。font2底行数字键需内部滚动，关闭后保存／取消可达；不声称全部键同屏。

只有API35合成模拟器设备执行证据；API26实机／作者手机、真实进程kill及完整自然TalkBack没有本轮执行。受控busy／读取中和保存UI状态＋新VM是技术夹具。19项PHONE_ACCEPTANCE_V014均未勾；部分0.1.3消息反馈通过不继承为0.1.4整体验收。

先在当前手机版本导出有效备份，再正常覆盖安装本机同旧签名包。安装异常保留原应用和账本，不卸载、清库或强制降级；明文备份存可信位置。S1手机通过后才进入S2，正式发布另行授权。
