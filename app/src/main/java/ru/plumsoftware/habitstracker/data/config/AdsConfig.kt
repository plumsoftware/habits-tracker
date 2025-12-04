package ru.plumsoftware.habitstracker.data.config

import ru.plumsoftware.habitstracker.BuildConfig

sealed class AdsConfig(
    open val openAdsId: String,
    open val interstitialAdsId: String,
    open val rewardedAdsId: String
) {
    data class RuStoreAds (
        override val openAdsId: String = if (BuildConfig.DEBUG) "demo-appopenad-yandex" else "R-M-17919102-1",
        override val interstitialAdsId: String = if (BuildConfig.DEBUG) "demo-interstitial-yandex" else "R-M-17919102-3",
        override val rewardedAdsId: String = if (BuildConfig.DEBUG) "demo-rewarded-yandex" else "R-M-17919102-2"
    ) : AdsConfig (
        openAdsId = openAdsId,
        interstitialAdsId = interstitialAdsId,
        rewardedAdsId = rewardedAdsId
    )

    data class HuaweiAppGalleryAds (
        override val openAdsId: String =  if (BuildConfig.DEBUG) "demo-appopenad-yandex" else "R-M-17919156-1",
        override val interstitialAdsId: String = if (BuildConfig.DEBUG) "demo-interstitial-yandex" else "R-M-17919156-2",
        override val rewardedAdsId: String = if (BuildConfig.DEBUG) "demo-rewarded-yandex" else "R-M-17919156-3"
    ) : AdsConfig (
        openAdsId = openAdsId,
        interstitialAdsId = interstitialAdsId,
        rewardedAdsId = rewardedAdsId
    )
}