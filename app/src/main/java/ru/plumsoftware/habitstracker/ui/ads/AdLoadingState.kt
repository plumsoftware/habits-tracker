package ru.plumsoftware.habitstracker.ui.ads

import android.app.Activity
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import ru.plumsoftware.habitstracker.ui.theme.AppTheme

internal class AdLoadingState {
    var isLoading by mutableStateOf(false)
        private set

    private var presenting = false

    suspend fun present(block: suspend () -> Unit) {
        if (presenting) return
        presenting = true
        try {
            block()
        } finally {
            presenting = false
        }
    }

    suspend fun <T> load(cancelLoading: () -> Unit, request: suspend () -> T): T? {
        isLoading = true
        try {
            // Give Compose a frame to attach the dialog before the request can complete.
            withFrameNanos { }
            val startedAt = SystemClock.elapsedRealtime()
            val result = withTimeoutOrNull(2_000L) { request() }
            if (result == null) cancelLoading()
            val remaining = 350L - (SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0) delay(remaining)
            return result
        } catch (cancelled: CancellationException) {
            cancelLoading()
            throw cancelled
        } finally {
            isLoading = false
        }
    }
}

internal fun Activity.canShowAd(): Boolean =
    !isFinishing && !isDestroyed &&
        (this as? LifecycleOwner)?.lifecycle?.currentState == Lifecycle.State.RESUMED

@Composable
internal fun AdLoadingDialog(state: AdLoadingState) {
    if (!state.isLoading) return
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = AppTheme.colors.surface) {
            Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(34.dp), color = AppTheme.colors.accent)
            }
        }
    }
}
