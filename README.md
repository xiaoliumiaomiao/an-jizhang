# 安记账

中文安卓本地记账软件，当前测试候选为 **0.1.4（versionCode 6）**，按最新 [V1.0方案](docs/superpowers/plans/2026-10-05-project-plan-v1.0.md) 完成 S1。方案版本与应用版本分别记录。0.1.0、0.1.1、0.1.2已由作者确认通过；0.1.4手机验收待作者执行。

- 明细／统计／设置导航、日期分组、账目详情、全屏新增与编辑。
- 点金额后从底部弹出应用数字键盘，可收起；返回先收键盘，备注使用系统输入法。
- 新记录不预选分类，最近分类按收入／支出分别显示；备注可折叠，修改后退出须确认放弃。
- 点月份标题选择年月，跟随系统浅深主题，明细内容区域避开“记一笔”按钮。
- 分类／备注字面搜索与类型筛选只影响明细，月统计和备份仍使用完整数据。
- 私有SQLite本地保存，整数分计算；明文JSON备份先完整验证，再事务合并，同ID冲突整批拒绝。

schema1、备份format1、稳定记录ID、依赖和权限合同保持。无广告、遥测、云同步，不申请联网或广泛存储权限。整本备份最多50000条／10MiB，单笔金额0.01至999999999.99元，首版人民币。支持Android8.0及以上；本轮实际设备验证环境为API35。

## 当前 S1 技术交付

受测提交 `3bf514249f6a920d2c3d8f89299830f72520eb82` 的 [CI 37301576955](https://github.com/xiaoliumiaomiao/an-jizhang/actions/runs/37301576955)：81项单元测试、35项安卓界面测试，以及同签名0.1.2/code3→0.1.4/code6覆盖升级seed／verify各1项，全部0失败、0错误、0跳过；lint0错误、9项已有提示。原77单元／19界面测试逐名保留，新增16个S1界面场景及两项备份错误隐私回归、两项原生窗口浅深色回归已核对。实际失败、修复、原始记录及限制见 [VALIDATION_V014](docs/VALIDATION_V014.md)，独立审查见 [REVIEW_V014](docs/REVIEW_V014.md)。

金额面板初始隐藏，触摸和直接语义点击均可打开；三字号1.0／1.3／2.0逐个金额键及输入值通过。font2最后一行键需在面板内部滚动，不代表全部键同时出现在屏幕上。恢复场景模拟保存UI状态及新ViewModel，没有真实杀死Android进程。自动语义测试不代替作者完整TalkBack验收。

原生截图保存于 `output/v0.1.4-screenshots/`，本轮全部23张及升级首页已逐张核对。源码、运行／视觉、APK与最终技术交付独立审查均Approved C0/I0/M0。私有草稿 [PR #4](https://github.com/xiaoliumiaomiao/an-jizhang/pull/4) 保持OPEN/draft，main仍为已验收0.1.2；本轮没有合并或正式发布。

## 安装

本机同旧签名测试包：`output/an-jizhang-0.1.4-debug.apk`，校验文件：`output/SHA256SUMS-v0.1.4.txt`。隔离工作区output与 `D:/AIproject/an-jizhang/output` 各有一份，大小9918020 bytes，SHA-256：

```text
5f71a8bf1a9c1e617b6a87dd6852ee45c2e6cdf18b084dbf070381c34c656af6
```

先在手机当前应用导出有效备份，保存到可信位置，再正常覆盖安装此本机包，按 [19项手机验收表](docs/PHONE_ACCEPTANCE_V014.md) 操作。安装异常保留原应用和账本，不卸载、清库或强制降级。旧APK、截图、校验文件及主目录作者文档原字节保留。

GitHub CI包仅用于独立技术验证，本机与CI签名可能不同。这个包仍使用debug签名；正式签名和商店发布另行决定。S1手机验收通过后才进入S2月度／年度统计；分类库、预算和format2另行确认。

## 开发与历史

项目规则见 [agent.md](agent.md)，最新进度见 [HANDOFF](docs/HANDOFF.md)，当前构建路径见 [DEVELOPMENT](docs/DEVELOPMENT.md)。本轮仅复用既有固定AndroidX／Material3 API及原始图形，没有新增依赖或下载字体／图标；来源与取舍见 [REUSE_V014](docs/REUSE_V014.md)。完整既有许可证随APK提供，见 [NOTICE](third_party/NOTICE.txt)。

0.1.3/code5手机反馈为部分通过：成功提示自动消失已通过，页面式金额键盘不通过，本轮改为按需弹出。旧版技术交付和失败记录保留在 [输入修复验证](docs/VALIDATION_INPUT_FEEDBACK.md)、[原0.1.3验证](docs/VALIDATION_V013.md)；原手机表保持其实际状态，旧版技术通过不继承为0.1.4手机验收。

已验收0.1.2的搜索／筛选和覆盖升级见 [VALIDATION_V012](docs/VALIDATION_V012.md)；0.1.1容量／边界及升级见 [VALIDATION_V011](docs/VALIDATION_V011.md)，首版见 [VALIDATION](docs/VALIDATION.md)。多agent实现任务均有独立规格和质量审查，作者自然手机验收与技术验证分别记录。
