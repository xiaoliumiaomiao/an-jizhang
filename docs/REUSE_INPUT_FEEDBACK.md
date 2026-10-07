# 金额键盘与成功提示：复用方案记录

日期：2026-10-05。本轮只处理删除成功提示长期停留，以及金额改用应用内数字键盘、备注继续使用系统输入法。

本文记录固定版本源码和候选资产的方案审核，**不是 Android 原生界面通过或作者手机验收记录**。本轮未引入第三方代码、图片、字体、图标或其他 assets，也未增加库、网络权限或系统输入法服务。原 [复用审查](REUSE_AUDIT.md)、[0.1.3 复用决定](REUSE_V013.md)及现有 LICENSE/NOTICE 保留。

## 已有官方组件与固定源码

继续使用 Compose BOM 2025.04.01 对应的 Compose UI/Foundation 1.8.0、Material3 1.3.2。实际阅读 Google Maven 固定 sources JAR；内存解压审查，未安装候选库或执行其构建脚本。源文件具有 Apache-2.0 许可头，既有打包许可证继续保留。[AndroidX 许可](https://raw.githubusercontent.com/androidx/androidx/androidx-main/LICENSE.txt)

| 结论 | 固定源码依据 |
| --- | --- |
| 现有 String 金额栏可设 readOnly，避免启动金额的系统输入会话 | [Foundation 1.8.0 源码](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-android/1.8.0/foundation-android-1.8.0-sources.jar)：CoreTextField.kt 314 起只在 hasFocus、enabled 且非 readOnly 时启动 input session；468/655 禁止只读可编辑光标/手柄。 [Material3 1.3.2 源码](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.3.2/material3-android-1.3.2-sources.jar)：OutlinedTextField.kt String overload 将 readOnly 和同一 interactionSource 交给 BasicTextField。 |
| showKeyboardOnFocus=false 不能作为本轮方案 | 固定 BasicTextField.kt 639/796 的 String/TextFieldValue overload 文档明确不支持该选项；KeyboardOptions.kt 48–50 还说明它仅限制获取焦点时的显示，tap 仍可请求键盘。[KeyboardOptions 官方 API](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/KeyboardOptions) |
| 可通过原组件交互与焦点收起备注键盘 | CoreTextField.kt 369–392 的 tapPressTextFieldModifier 由 enabled 门控，readOnly 仍可发 Press/Release；TextFieldPressGestureFilter.kt 67–76 的 Release 发射在协程中，与内部 requestFocus 无严格顺序保证。使用原 interactionSource 观察 Release、焦点变化 hide，以及数字按钮显式 clearFocus/hide，避免额外 clickable 抢夺手势。hide 为 best effort，实际 IME 消失仍需原生验证。[SoftwareKeyboardController 官方 API](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/SoftwareKeyboardController)、[焦点控制](https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus) |
| 本轮不使用平台输入会话拦截器 | [UI 1.8.0 源码](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.8.0/ui-android-1.8.0-sources.jar)的 InterceptPlatformTextInput 仍需 ExperimentalComposeUiApi opt-in；虽然可局部阻止软键盘，但会扩大会话生命周期和编辑模型的审查范围。[官方 API/sample](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/InterceptPlatformTextInput.composable) |

Foundation 1.8.0 sources JAR 实际 SHA-256：D9E7045107F495CF5748B518AABAA6FD2E4AB4340AD435DDDA73831B477D89E4。版本可用性以固定源码为依据，不由后续滚动 API 文档反推。

## 社区候选与采用边界

实际检查 GitHub 页面、固定 commit、许可、源码、依赖与 manifest。匿名 REST 请求遇共享 IP rate limit，固定 SHA 由实际页面 currentOid 与 commit 页面核验；未把失败 API 调用作为成功证据。

| 项目与固定版本 | 许可、维护与本轮决定 |
| --- | --- |
| [davidmigloz/number-keyboard](https://github.com/davidmigloz/number-keyboard)，SHA 519daa07632e5a267008da04d00751a3fc56f067；[固定代码](https://github.com/davidmigloz/number-keyboard/blob/519daa07632e5a267008da04d00751a3fc56f067/lib/src/commonMain/kotlin/com/davidmiguel/numberkeyboard/NumberKeyboard.kt) | [固定 Apache-2.0 LICENSE](https://raw.githubusercontent.com/davidmigloz/number-keyboard/519daa07632e5a267008da04d00751a3fc56f067/LICENSE)。观察到最后 commit 为 2025-07-16，README 标 5.0.2；这些不能证明持续活跃或安全认证。仅参考三列数字键及回调模式。库采用 KMP/JitPack、不同 BOM，实际金额检查使用 Double/toDoubleOrNull 并可自动限制至最大值；不符合本项目整数分与拒绝非法金额的约定，故不引入库或复制计算逻辑。 |
| [kerimovscreations/android-keyboard-in-app](https://github.com/kerimovscreations/android-keyboard-in-app)，SHA 1133be6d71998fb3abb9cd0d43ead337b78180a0 | 根目录无独立 LICENSE，但[固定 README](https://raw.githubusercontent.com/kerimovscreations/android-keyboard-in-app/1133be6d71998fb3abb9cd0d43ead337b78180a0/README.md)实际含 MIT 全文和版权；观察到最后 commit 为 2025-05-28。其多语言、剪贴板、自管光标及输入拦截扩大范围，不整仓复制或引入依赖。README 的反键盘记录、zero trust 等安全宣称未验证，不作为本产品承诺。 |

两候选的所查 manifest 未见新增所需网络权限，但这不构成全供应链安全审计。标准键盘布局由现有 Material 组件自行实现；没有复制上述第三方具体代码或资产。若以后复制具体片段，须重新记录文件、改动与相应 MIT/Apache 署名义务。

## 采用的输入方案

金额保留现有 String 草稿和 rememberSaveable；readOnly 显示金额，同一滚动表单内放置 3×4 Material 数字、小数点、退格按钮，并另设清空。操作只在末尾追加、删除最后一个字符或全部清空，不提供任意光标插入、选区替换、金额粘贴或硬件键盘金额输入。只读字段仍可聚焦/复制，不能把其语义伪装为完整可编辑字段。

按键仅改草稿，不提交数据库，不重建记录 ID；保存继续用既有 Money.parse 和 LedgerRules，金额仍为整数分，不引入 Float/Double、自动四舍五入或超限 clamp。既有 MoneyBook MIT 片段与许可来源不变，见 [初版复用审查](REUSE_AUDIT.md)。

备注继续正常使用系统 IME；金额点按、金额焦点和数字按钮只处理金额路线的焦点/隐藏，不全局禁用输入法。Release 只代表完成点击，滚动产生的 Cancel 不算点击；TalkBack/键盘焦点不一定发 pointer Press，因此保留焦点与按钮 onClick 路线。按钮遵守 busy 保护，并保留原生 role/click/反馈、至少 48dp 可用触控区和中文退格/清空标签；窄屏、大字体下可滚动到全部按钮及保存。[官方可访问性默认行为](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)、[semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)

## 采用的成功提示生命周期

成功消息使用独立 messageId，并以 SystemClock.elapsedRealtime() + 3000ms 保存单调截止时间；系统墙上时钟调整不会改变剩余期限。[Android SystemClock 官方 API](https://developer.android.com/reference/android/os/SystemClock)

根界面的 effect 根据 messageId/截止时间处理剩余时长，到期调用按 ID 条件清除。旋转重建使用原截止时间，不重新赠送完整 3 秒；旧 effect/回调仅能清除其匹配的 messageId，不能误清新的同文案消息。只清 message、messageId、messageExpiresAt，保留 error、completedOperationId、failedOperationId 及保存/删除 pending 保护；VM 不为提示新增后台计时 Job。

此状态到 UI 再消费的分工参考[官方 UI events 指南](https://developer.android.com/topic/architecture/ui-layer/events)。3 秒是本轮简单消息条规格，并非 Material Snackbar 默认值；[固定 Material3 源码](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.3.2/material3-android-1.3.2-sources.jar)中 Snackbar Short 为 4 秒且可按可访问性延长，不能把本轮定时方案等同于 Snackbar 的全部行为。

本文不预填编译、设备回归或手机通过。新逻辑、原业务、真实 IME 交接、消息实际消失与字体/滚动效果分别验证，当前证据见 [本轮验证记录](VALIDATION_INPUT_FEEDBACK.md)，作者结果另见 [本轮手机清单](PHONE_INPUT_FEEDBACK.md)。
