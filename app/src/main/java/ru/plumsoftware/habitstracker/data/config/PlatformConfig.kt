package ru.plumsoftware.habitstracker.data.config

sealed class PlatformConfig (
    open val adsConfig: AdsConfig,
) {
    data class RuStoreConfig (
        override val adsConfig: AdsConfig = AdsConfig.RuStoreAds()
    ) : PlatformConfig (
        adsConfig = adsConfig,
    )

    data class HuaweiConfig (
        override val adsConfig: AdsConfig = AdsConfig.HuaweiAppGalleryAds()
    ) : PlatformConfig (
        adsConfig = adsConfig,
    )

    data class GooglePlayConfig (
        override val adsConfig: AdsConfig = AdsConfig.GooglePlay()
    ) : PlatformConfig (
        adsConfig = adsConfig,
    )
}