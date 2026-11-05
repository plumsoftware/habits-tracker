package ru.plumsoftware.habitstracker.data.config

import ru.plumsoftware.habitstracker.BuildConfig

data class InterstitialPlacements(
    val habitCreation: String = "",
    val workoutCreation: String = "",
    val sleepEntry: String = "",
    val stepCounterExit: String = "",
)

sealed class AdsConfig(
    open val openAdsId: String,
    open val interstitial: InterstitialPlacements,
    open val rewardedAdsId: String
) {
    data class RuStoreAds (
        override val openAdsId: String = if (BuildConfig.DEBUG) "demo-appopenad-yandex" else "R-M-17919102-1",
        override val interstitial: InterstitialPlacements = InterstitialPlacements(
            habitCreation = "R-M-17919102-3",
            workoutCreation = "R-M-17919102-4",
            sleepEntry = "R-M-17919102-5",
            stepCounterExit = "R-M-17919102-6",
        ),
        override val rewardedAdsId: String = if (BuildConfig.DEBUG) "demo-rewarded-yandex" else "R-M-17919102-2"
    ) : AdsConfig (
        openAdsId = openAdsId,
        interstitial = interstitial,
        rewardedAdsId = rewardedAdsId
    )

    data class HuaweiAppGalleryAds (
        override val openAdsId: String =  if (BuildConfig.DEBUG) "demo-appopenad-yandex" else "R-M-17919156-1",
        override val interstitial: InterstitialPlacements = InterstitialPlacements(
            habitCreation = "R-M-17919156-2",
            workoutCreation = "R-M-17919156-6",
            sleepEntry = "R-M-17919156-5",
            stepCounterExit = "R-M-17919156-4",
        ),
        override val rewardedAdsId: String = if (BuildConfig.DEBUG) "demo-rewarded-yandex" else "R-M-17919156-3"
    ) : AdsConfig (
        openAdsId = openAdsId,
        interstitial = interstitial,
        rewardedAdsId = rewardedAdsId
    )

    data class GooglePlay (
        override val openAdsId: String =  if (BuildConfig.DEBUG) "demo-appopenad-yandex" else "R-M-17944291-1",
        override val interstitial: InterstitialPlacements = InterstitialPlacements(),
        override val rewardedAdsId: String = if (BuildConfig.DEBUG) "demo-rewarded-yandex" else "R-M-17944291-2"
    ) : AdsConfig (
        openAdsId = openAdsId,
        interstitial = interstitial,
        rewardedAdsId = rewardedAdsId
    )
}