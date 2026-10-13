package com.xiaoliumiaomiao.anjizhang.data

import android.content.Context
import android.content.SharedPreferences
import com.xiaoliumiaomiao.anjizhang.ui.Handedness
import com.xiaoliumiaomiao.anjizhang.ui.ThemeMode
import com.xiaoliumiaomiao.anjizhang.ui.UiPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

class UiPreferencesStore internal constructor(
    private val preferences: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableState = MutableStateFlow(readPreferences(preferences))
    val state: StateFlow<UiPreferences> = mutableState.asStateFlow()
    private val mutex = Mutex()
    // Android may update its preferences memory before reporting a failed disk write.
    // Equal requests must rewrite both last-confirmed values while any write is unconfirmed.
    private var dirty = false

    suspend fun setTheme(value: ThemeMode): Result<UiPreferences> = save { it.copy(theme = value) }
    suspend fun setHand(value: Handedness): Result<UiPreferences> = save { it.copy(hand = value) }

    private suspend fun save(change: (UiPreferences) -> UiPreferences): Result<UiPreferences> {
        currentCoroutineContext().ensureActive()
        return mutex.withLock {
            // Commit and publication form one short protected section. A cancelled caller can
            // receive CancellationException on return even though both have finished successfully.
            withContext(NonCancellable + ioDispatcher) {
                val confirmed = mutableState.value
                val candidate = change(confirmed)
                if (!dirty && candidate == confirmed) return@withContext Result.success(confirmed)
                dirty = true
                try {
                    val saved = preferences.edit()
                        .putString(THEME_KEY, candidate.theme.name.lowercase())
                        .putString(HAND_KEY, candidate.hand.name.lowercase())
                        .commit()
                    if (saved) {
                        mutableState.value = candidate
                        dirty = false
                        Result.success(candidate)
                    } else {
                        Result.failure(IOException("外观设置保存失败，请重试"))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Result.failure(IOException("外观设置保存失败，请重试", failure))
                }
            }
        }
    }

    companion object {
        private const val THEME_KEY = "theme_mode"
        private const val HAND_KEY = "handedness"
        @Volatile private var instance: UiPreferencesStore? = null

        /** Activity recreation reuses the same confirmed state and the same write mutex. */
        fun getInstance(context: Context): UiPreferencesStore = instance ?: synchronized(this) {
            instance ?: UiPreferencesStore(
                context.applicationContext.getSharedPreferences("ui-preferences", Context.MODE_PRIVATE)
            ).also { instance = it }
        }

        private fun readPreferences(preferences: SharedPreferences): UiPreferences {
            val values = preferences.all
            return UiPreferences(
                theme = when (values[THEME_KEY]) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                },
                hand = when (values[HAND_KEY]) {
                    "left" -> Handedness.LEFT
                    else -> Handedness.RIGHT
                },
            )
        }
    }
}
