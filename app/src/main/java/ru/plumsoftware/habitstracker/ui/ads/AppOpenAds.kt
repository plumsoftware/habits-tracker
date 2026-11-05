package ru.plumsoftware.habitstracker.ui.ads

import android.app.Activity
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadResult
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.ImpressionData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun showAppOpenAd(activity: Activity?, placementId: String, loading: AdLoadingState) {
    if (activity == null || placementId.isBlank()) return
    val lifecycle = (activity as? LifecycleOwner)?.lifecycle ?: return
    lifecycle.currentStateFlow.first { it == Lifecycle.State.RESUMED || it == Lifecycle.State.DESTROYED }
    if (!activity.canShowAd()) return

    loading.present {
        try {
            val loader = AppOpenAdLoader(activity.applicationContext)
            val request = AdRequest.Builder(placementId).build()
            val result = loading.load(loader::cancelLoading) { loader.loadAd(request) }
            if (result !is AppOpenAdLoadResult.Success || !activity.canShowAd()) return@present
            val ad = result.ad
            try {
                suspendCancellableCoroutine<Unit> { continuation ->
                    ad.setAdEventListener(object : AppOpenAdEventListener {
                        override fun onAdShown() = Unit
                        override fun onAdClicked() = Unit
                        override fun onAdImpression(impressionData: ImpressionData?) = Unit

                        override fun onAdDismissed() {
                            if (continuation.isActive) continuation.resume(Unit)
                        }

                        override fun onAdFailedToShow(adError: AdError) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    })
                    ad.show(activity)
                }
            } finally {
                ad.setAdEventListener(null)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("AppOpenAds", "App open ad failed", error)
        }
    }
}
