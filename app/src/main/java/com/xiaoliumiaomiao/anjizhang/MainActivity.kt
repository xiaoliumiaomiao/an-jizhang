package com.xiaoliumiaomiao.anjizhang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoliumiaomiao.anjizhang.data.UiPreferencesStore
import com.xiaoliumiaomiao.anjizhang.ui.*
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val ledger by lazy { ViewModelProvider(this)[LedgerViewModel::class.java] }
    // Delivery correlation only. The root's one saveable holder owns every modal and operation gate.
    private var exportOwner = ""
    private var restoreOwner = ""
    private var documentResult by mutableStateOf<LedgerDocumentResult?>(null)
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        documentResult = LedgerDocumentResult(exportOwner, uri?.toString(), true)
        exportOwner = ""
    }
    private val restore = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        documentResult = LedgerDocumentResult(restoreOwner, uri?.toString(), false)
        restoreOwner = ""
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exportOwner = savedInstanceState?.getString("ledger-export-owner").orEmpty()
        restoreOwner = savedInstanceState?.getString("ledger-restore-owner").orEmpty()
        savedInstanceState?.getString("ledger-document-owner")?.let { owner ->
            documentResult = LedgerDocumentResult(owner, savedInstanceState.getString("ledger-document-uri"), savedInstanceState.getBoolean("ledger-document-export"))
        }
        val preferencesStore = UiPreferencesStore.getInstance(applicationContext)
        setContent {
            val preferences by preferencesStore.state.collectAsStateWithLifecycle()
            LedgerTheme(preferences) {
                SyncActivityWindowAppearance(this@MainActivity)
                LedgerScreen(ledger, {}, {}, {}, preferences = preferences, preferenceStore = preferencesStore,
                    onSelectExport = { owner ->
                        exportOwner = owner
                        export.launch("安记账-${LocalDate.now()}.json")
                    }, onSelectImport = { owner ->
                        restoreOwner = owner
                        restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    }, documentResult = documentResult, onDocumentConsumed = { documentResult = null })
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("ledger-export-owner", exportOwner)
        outState.putString("ledger-restore-owner", restoreOwner)
        documentResult?.let {
            outState.putString("ledger-document-owner", it.owner)
            outState.putString("ledger-document-uri", it.uri)
            outState.putBoolean("ledger-document-export", it.export)
        }
        super.onSaveInstanceState(outState)
    }
}