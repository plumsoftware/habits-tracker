package ru.plumsoftware.habitstracker.ui.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadResult
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import ru.plumsoftware.habitstracker.BuildConfig
import kotlin.coroutines.resume

internal object InterstitialAds {
    private const val defaultCooldownMillis = 45_000L
    private const val demoId = "demo-interstitial-yandex"

    private var loader: InterstitialAdLoader? = null
    private var lastShownAt: Long? = null
    private var lastCooldownMillis = defaultCooldownMillis
    private var inProgress = false

    suspend fun showIfAvailable(
        activity: Activity?,
        placementId: String,
        loading: AdLoadingState,
        cooldownMillis: Long = defaultCooldownMillis,
    ) {
        withContext(Dispatchers.Main.immediate) {
            if (activity == null || placementId.isBlank() || inProgress || !activity.canShowAd()) return@withContext
            val lastShow = lastShownAt
            val requiredInterval = maxOf(lastCooldownMillis, cooldownMillis)
            if (lastShow != null && SystemClock.elapsedRealtime() - lastShow < requiredInterval) return@withContext

            loading.present {
                inProgress = true
                try {
                    val adLoader = loader ?: InterstitialAdLoader(activity.applicationContext).also { loader = it }
                    val id = if (BuildConfig.DEBUG) demoId else placementId
                    val request = AdRequest.Builder(id).build()
                    val result = loading.load(adLoader::cancelLoading) { adLoader.loadAd(request) }
                    if (result is InterstitialAdLoadResult.Success && activity.canShowAd()) {
                        awaitDismissal(activity, result.ad, cooldownMillis)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.w("InterstitialAds", "Interstitial request failed", error)
                } finally {
                    inProgress = false
                }
            }
        }
    }

    private suspend fun awaitDismissal(activity: Activity, ad: InterstitialAd, cooldownMillis: Long) {
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                ad.setAdEventListener(object : InterstitialAdEventListener {
                    override fun onAdShown() {
                        lastShownAt = SystemClock.elapsedRealtime()
                        lastCooldownMillis = cooldownMillis
                    }

                    override fun onAdDismissed() {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onAdClicked() = Unit

                    override fun onAdImpression(impressionData: ImpressionData?) = Unit
                })
                ad.show(activity)
            }
        } finally {
            ad.setAdEventListener(null)
        }
    }
}
