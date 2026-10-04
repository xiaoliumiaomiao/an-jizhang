package com.xiaoliumiaomiao.anjizhang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.xiaoliumiaomiao.anjizhang.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val ledger by lazy { ViewModelProvider(this)[LedgerViewModel::class.java] }
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { ledger.export { contentResolver.openOutputStream(it, "wt") } }
    }
    private val restore = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { ledger.restore { contentResolver.openInputStream(it) } }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerTheme {
                var exportWarning by rememberSaveable { mutableStateOf(false) }
                var restoreWarning by rememberSaveable { mutableStateOf(false) }
                var licenses by rememberSaveable { mutableStateOf<String?>(null) }
                LedgerScreen(ledger, onExport = { exportWarning = true }, onImport = { restoreWarning = true }, onLicenses = {
                    lifecycleScope.launch {
                        licenses = withContext(Dispatchers.IO) {
                            try {
                                assets.list("licenses").orEmpty().sorted().joinToString("\n\n") { name ->
                                    "$name\n" + assets.open("licenses/$name").bufferedReader().use { it.readText() }
                                }
                            } catch (failure: Exception) { "许可读取失败：${failure.message}" }
                        }
                    }
                })
                if (exportWarning) AlertDialog(onDismissRequest = { exportWarning = false }, title = { Text("导出明文备份") }, text = { Text("备份文件包含明文账目，请保存在可信位置。") }, confirmButton = {
                    TextButton(onClick = { exportWarning = false; export.launch("安记账-${LocalDate.now()}.json") }) { Text("选择保存位置") }
                }, dismissButton = { TextButton(onClick = { exportWarning = false }) { Text("取消") } })
                if (restoreWarning) AlertDialog(onDismissRequest = { restoreWarning = false }, title = { Text("合并恢复账目") }, text = { Text("恢复会新增账目，相同记录跳过；内容冲突时整批拒绝，现有账目不被替换。请选择可信的安记账备份。") }, confirmButton = {
                    TextButton(onClick = { restoreWarning = false; restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("选择备份文件") }
                }, dismissButton = { TextButton(onClick = { restoreWarning = false }) { Text("取消") } })
                licenses?.let { text -> AlertDialog(onDismissRequest = { licenses = null }, title = { Text("第三方许可") }, text = { Column(Modifier.verticalScroll(rememberScrollState()).padding(4.dp)) { Text(text) } }, confirmButton = { TextButton(onClick = { licenses = null }) { Text("关闭") } }) }
            }
        }
    }
}
