package com.devpro.pizzatime.shared.location

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

object OsmdroidConfiguration {

    fun configure(
        context: Context,
        applicationId: String,
    ) {
        val appContext = context.applicationContext
        val basePath = File(appContext.cacheDir, CACHE_DIRECTORY)
        val tilePath = File(basePath, TILE_CACHE_DIRECTORY)
        val configuration = Configuration.getInstance()

        configuration.userAgentValue = userAgent(applicationId)
        configuration.osmdroidBasePath = basePath
        configuration.osmdroidTileCache = tilePath
        configuration.tileFileSystemCacheMaxBytes = TILE_CACHE_MAX_BYTES
        configuration.tileFileSystemCacheTrimBytes = TILE_CACHE_TRIM_BYTES
    }

    internal fun userAgent(applicationId: String): String {
        return applicationId.trim().ifBlank { FALLBACK_USER_AGENT }
    }

    private const val CACHE_DIRECTORY = "osmdroid"
    private const val TILE_CACHE_DIRECTORY = "tiles"
    private const val FALLBACK_USER_AGENT = "com.devpro.pizzatime"
    private const val TILE_CACHE_MAX_BYTES = 64L * 1024L * 1024L
    private const val TILE_CACHE_TRIM_BYTES = 48L * 1024L * 1024L
}
