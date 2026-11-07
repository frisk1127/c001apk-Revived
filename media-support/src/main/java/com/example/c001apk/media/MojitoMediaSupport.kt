package com.example.c001apk.media

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.DrawableRes
import net.mikaelzero.mojito.interfaces.ActivityCoverLoader
import net.mikaelzero.mojito.interfaces.IMojitoActivity
import net.mikaelzero.mojito.interfaces.ImageViewLoadFactory
import net.mikaelzero.mojito.loader.ContentLoader
import net.mikaelzero.mojito.view.sketch.core.SketchImageView
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

enum class RichMediaHint {
    NONE,
    ULTRA_HDR,
    LIVE_PHOTO,
}

fun String.coolApkRichMediaHint(): RichMediaHint = when {
    contains("-livepic", ignoreCase = true) -> RichMediaHint.LIVE_PHOTO
    contains("-uhdr", ignoreCase = true) ||
        contains("-xhdr", ignoreCase = true) -> RichMediaHint.ULTRA_HDR
    else -> RichMediaHint.NONE
}

fun String.mayContainCoolApkRichMedia(): Boolean =
    coolApkRichMediaHint() != RichMediaHint.NONE

class MojitoMediaImageFactory(
    private val delegate: ImageViewLoadFactory,
    private val imageUrl: String,
    private val videoUrlResolver: LivePhotoVideoUrlResolver? = null,
    private val mediaHint: RichMediaHint = imageUrl.coolApkRichMediaHint(),
    private val playbackSession: MojitoMediaPlaybackSession = MojitoMediaPlaybackSession(),
    private val pagePosition: Int = 0,
    private val deferMediaBindingUntilTarget: Boolean = false,
) : ImageViewLoadFactory {

    private var contentLoader: MediaContentLoader? = null
    private var lastBoundFileKey: String? = null
    private var previewFileKey: String? = null
    private var parseGeneration = 0
    private val parsedMedia = ConcurrentHashMap<String, EmbeddedMediaInfo>()

    override fun loadSillContent(view: View, uri: Uri) {
        val file = uri.path?.let(::File)
        val fileKey = file?.let { "${it.absolutePath}:${it.length()}" }
        if (isPreviewLoad(fileKey)) {
            delegate.loadSillContent(view, uri)
            file?.let { contentLoader?.bindExitPreview(it) }
            return
        }

        val expectUltraHdr = mediaHint == RichMediaHint.ULTRA_HDR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            expectUltraHdr && view is SketchImageView
        ) {
            view.options
                .setBitmapConfig(Bitmap.Config.ARGB_8888)
                .setBitmapPoolDisabled(true)
                .setCacheProcessedImageInDisk(false)
                .setCorrectImageOrientationDisabled(false)
        }

        delegate.loadSillContent(view, uri)
        if (file == null || fileKey == null) return

        val generation = ++parseGeneration
        val targetLoader = contentLoader
        parsedMedia[fileKey]?.let { mediaInfo ->
            bindParsedMedia(view, file, fileKey, mediaInfo, wasCached = true)
            return
        }
        MEDIA_PARSE_EXECUTOR.execute {
            val mediaInfo = runCatching {
                MotionPhotoParser.parse(
                    file = file,
                    expectMotionPhoto = mediaHint == RichMediaHint.LIVE_PHOTO,
                )
            }.onFailure {
                Log.w(TAG, "Unable to parse rich media file ${file.name}", it)
            }.getOrNull() ?: return@execute
            parsedMedia[fileKey] = mediaInfo
            view.post {
                if (generation == parseGeneration && contentLoader === targetLoader) {
                    bindParsedMedia(view, file, fileKey, mediaInfo, wasCached = false)
                }
            }
        }
    }

    override fun loadContentFail(view: View, @DrawableRes drawableResId: Int) {
        delegate.loadContentFail(view, drawableResId)
    }

    override fun newContentLoader(): ContentLoader {
        contentLoader?.dispose()
        parseGeneration++
        lastBoundFileKey = null
        previewFileKey = null
        return MediaContentLoader(
            delegate.newContentLoader(),
            playbackSession,
            imageUrl,
            videoUrlResolver,
            mediaHint,
            pagePosition,
        ).also {
            contentLoader = it
        }
    }

    val supportsLivePhotoExport: Boolean
        get() = mediaHint == RichMediaHint.LIVE_PHOTO || contentLoader?.hasLivePhoto == true

    fun exportLivePhoto(
        destination: File,
        onComplete: (Result<File>) -> Unit,
    ): Boolean = contentLoader?.exportLivePhoto(destination, onComplete) == true

    private fun findBitmap(drawable: Drawable?): Bitmap? = when (drawable) {
        is BitmapDrawable -> drawable.bitmap
        is TransitionDrawable -> (drawable.numberOfLayers - 1 downTo 0)
            .firstNotNullOfOrNull { findBitmap(drawable.getDrawable(it)) }
        else -> null
    }

    private fun isPreviewLoad(fileKey: String?): Boolean {
        if (!deferMediaBindingUntilTarget || fileKey == null) return false
        val knownPreviewFileKey = previewFileKey
        if (knownPreviewFileKey == null) {
            previewFileKey = fileKey
            return true
        }
        return knownPreviewFileKey == fileKey
    }

    private fun bindParsedMedia(
        view: View,
        file: File,
        fileKey: String,
        mediaInfo: EmbeddedMediaInfo,
        wasCached: Boolean,
    ) {
        if (BuildConfig.DEBUG && !wasCached) {
            Log.d(
                TAG,
                "parsed file=${file.name} bytes=${file.length()} hdr=${mediaInfo.hasUltraHdr} " +
                    "motion=${mediaInfo.hasMotionPhoto} declared=${mediaInfo.declaredVideoLength} " +
                    "embedded=${mediaInfo.embeddedVideoLength}",
            )
        }
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            mediaInfo.hasUltraHdr && view is SketchImageView
        ) {
            view.postDelayed({
                val bitmap = findBitmap(view.drawable)
                Log.d(
                    TAG,
                    "decoded file=${file.name} config=${bitmap?.config} gainmap=${bitmap?.hasGainmap()}",
                )
            }, 1200L)
        }
        if ((mediaInfo.hasMotionPhoto || mediaInfo.hasUltraHdr) && fileKey != lastBoundFileKey) {
            lastBoundFileKey = fileKey
            contentLoader?.bind(file, mediaInfo)
        }
    }

    private companion object {
        const val TAG = "MojitoMedia"
        val MEDIA_PARSE_EXECUTOR = Executors.newSingleThreadExecutor()
    }
}

class HdrMojitoActivityCoverLoader(
    private val playbackSession: MojitoMediaPlaybackSession,
) : ActivityCoverLoader {

    private var coverView: View? = null

    override fun attach(context: IMojitoActivity) {
        val activity = context.getContext() as? Activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity?.window?.colorMode = ActivityInfo.COLOR_MODE_HDR
        }
        coverView = View(context.getContext()).apply {
            isClickable = false
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun providerView(): View = checkNotNull(coverView)

    override fun move(moveX: Float, moveY: Float) = Unit

    override fun fingerRelease(isToMax: Boolean, isToMin: Boolean) = Unit

    override fun pageChange(totalSize: Int, position: Int) {
        playbackSession.selectPage(position)
    }
}
