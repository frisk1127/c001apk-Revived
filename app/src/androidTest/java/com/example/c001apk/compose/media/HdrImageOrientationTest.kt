package com.example.c001apk.compose.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Gainmap
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.example.c001apk.media.MojitoMediaImageFactory
import net.mikaelzero.mojito.view.sketch.SketchImageLoadFactory
import net.mikaelzero.mojito.view.sketch.core.SketchImageView
import net.mikaelzero.mojito.view.sketch.core.cache.LruBitmapPool
import net.mikaelzero.mojito.view.sketch.core.decode.ImageAttrs
import net.mikaelzero.mojito.view.sketch.core.decode.ImageOrientationCorrector
import net.mikaelzero.mojito.view.sketch.core.request.CancelCause
import net.mikaelzero.mojito.view.sketch.core.request.DisplayListener
import net.mikaelzero.mojito.view.sketch.core.request.ErrorCause
import net.mikaelzero.mojito.view.sketch.core.request.ImageFrom
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

@SdkSuppress(minSdkVersion = 34)
class HdrImageOrientationTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val corrector = ImageOrientationCorrector()
    private val corners = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
    private val order = listOf(
        intArrayOf(0, 1, 2, 3), intArrayOf(1, 0, 3, 2), intArrayOf(3, 2, 1, 0),
        intArrayOf(2, 3, 0, 1), intArrayOf(0, 2, 1, 3), intArrayOf(2, 0, 3, 1),
        intArrayOf(3, 1, 2, 0), intArrayOf(1, 3, 0, 2),
    )

    private fun pattern(width: Int, height: Int): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        for (y in 0 until height) for (x in 0 until width) setPixel(x, y,
            corners[(if (y >= height / 2) 2 else 0) + if (x >= width / 2) 1 else 0])
    }

    private fun gainmap(): Gainmap = Gainmap(pattern(3, 2)).apply {
        setRatioMin(1f, 1.1f, 1.2f)
        setRatioMax(2f, 3f, 4f)
        setGamma(.8f, 1f, 1.2f)
        setEpsilonSdr(.001f, .002f, .003f)
        setEpsilonHdr(.004f, .005f, .006f)
        displayRatioForFullHdr = 4f
        minDisplayRatioForHdrTransition = 1.2f
    }

    private fun assertMetadata(expected: Gainmap, actual: Gainmap) {
        assertArrayEquals(expected.ratioMin, actual.ratioMin, .0001f)
        assertArrayEquals(expected.ratioMax, actual.ratioMax, .0001f)
        assertArrayEquals(expected.gamma, actual.gamma, .0001f)
        assertArrayEquals(expected.epsilonSdr, actual.epsilonSdr, .0001f)
        assertArrayEquals(expected.epsilonHdr, actual.epsilonHdr, .0001f)
        assertEquals(expected.displayRatioForFullHdr, actual.displayRatioForFullHdr, .0001f)
        assertEquals(expected.minDisplayRatioForHdrTransition, actual.minDisplayRatioForHdrTransition, .0001f)
    }

    private fun assertCorners(bitmap: Bitmap, orientation: Int) {
        val actual = intArrayOf(bitmap.getPixel(0, 0), bitmap.getPixel(bitmap.width - 1, 0),
            bitmap.getPixel(0, bitmap.height - 1), bitmap.getPixel(bitmap.width - 1, bitmap.height - 1))
        assertArrayEquals("Orientation $orientation", order[orientation - 1].map { corners[it] }.toIntArray(), actual)
    }

    @Test fun allExifOrientationsPreserveHdrAndRotateBothLayers() {
        val pool = LruBitmapPool(context, 1024 * 1024)
        for (orientation in 1..8) {
            val source = pattern(6, 4)
            val originalGainmap = gainmap()
            source.gainmap = originalGainmap
            val rotated = corrector.rotate(source, orientation, pool) ?: source
            assertTrue(rotated.hasGainmap())
            assertEquals(if (orientation >= 5) 4 else 6, rotated.width)
            assertEquals(if (orientation >= 5) 6 else 4, rotated.height)
            assertCorners(rotated, orientation)
            val transformedGainmap = requireNotNull(rotated.gainmap)
            assertEquals(if (orientation >= 5) 2 else 3, transformedGainmap.gainmapContents.width)
            assertEquals(if (orientation >= 5) 3 else 2, transformedGainmap.gainmapContents.height)
            assertCorners(transformedGainmap.gainmapContents, orientation)
            assertMetadata(originalGainmap, transformedGainmap)
            if (rotated !== source) rotated.recycle()
            source.recycle()
        }
        pool.close()
    }

    @Test fun android14GainmapFallbackPreservesGeometryAndParameters() {
        val implementation = ImageOrientationCorrector::class.java.declaredClasses.single { it.simpleName == "Api34Impl" }
        val rotate = implementation.getDeclaredMethod("rotateGainmap", Gainmap::class.java, Matrix::class.java).apply { isAccessible = true }
        for (orientation in 2..8) {
            val source = gainmap()
            val matrix = Matrix().also { ImageOrientationCorrector.initializeMatrixForExifRotation(orientation, it) }
            val result = rotate.invoke(null, source, matrix) as Gainmap
            assertCorners(result.gainmapContents, orientation)
            assertMetadata(source, result)
            result.gainmapContents.recycle()
            source.gainmapContents.recycle()
        }
    }

    @Test fun ordinaryImagesStillFollowAllExifOrientations() {
        val pool = LruBitmapPool(context, 1024 * 1024)
        for (orientation in 1..8) {
            val source = pattern(6, 4)
            val result = corrector.rotate(source, orientation, pool) ?: source
            assertCorners(result, orientation)
            assertFalse(result.hasGainmap())
            if (result !== source) result.recycle()
            source.recycle()
        }
        pool.close()
    }

    @Test fun hdrBitmapsAreNotReusedAsPlainDecodeBuffers() {
        val pool = LruBitmapPool(context, 1024 * 1024)
        val hdr = pattern(6, 4).apply { gainmap = gainmap() }
        val sdr = pattern(6, 4)
        assertFalse(pool.put(hdr))
        assertEquals(0, pool.size)
        assertTrue(pool.put(sdr))
        hdr.recycle()
        pool.close()
    }

    private fun sample(name: String): File {
        val directory = InstrumentationRegistry.getArguments().getString("orientationSampleDir")
        assumeNotNull(directory)
        return File(requireNotNull(directory), name).also { assertTrue("Missing reproduction sample: $it", it.isFile) }
    }

    @Test fun realPostOriginalMatchesPreviewWithoutLosingHdr() {
        val originalFile = sample("original-3.jpg")
        val preview = BitmapFactory.decodeFile(sample("preview-3.jpg").absolutePath)
        val original = BitmapFactory.decodeFile(originalFile.absolutePath, BitmapFactory.Options().apply { inSampleSize = 4 })
        assertTrue(original.width > original.height)
        assertTrue(original.hasGainmap())
        val orientation = originalFile.inputStream().use { corrector.readExifOrientation(it) }
        assertEquals(6, orientation)
        val pool = LruBitmapPool(context, 1024 * 1024)
        val result = requireNotNull(corrector.rotate(original, orientation, pool))
        assertEquals(original.height, result.width)
        assertEquals(original.width, result.height)
        assertTrue(result.hasGainmap())
        assertMetadata(requireNotNull(original.gainmap), requireNotNull(result.gainmap))
        assertTrue("Corrected original must match the upright CDN preview", difference(result, preview) < difference(original, preview) * .5)
        result.recycle(); original.recycle(); preview.recycle(); pool.close()
    }

    @Test fun viewerPreviewToHdrOriginalKeepsTheSameOrientation() {
        val original = sample("original-3.jpg")
        val preview = sample("preview-3.jpg")
        val factory = MojitoMediaImageFactory(SketchImageLoadFactory(), "https://example.invalid/sample-xhdr.jpg",
            deferMediaBindingUntilTarget = true)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var view: SketchImageView
            scenario.onActivity { activity ->
                view = SketchImageView(activity).apply {
                    isZoomEnabled = true
                    zoomer?.blockDisplayer?.setPause(true)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    options.setMaxSize(960, 1280)
                }
                activity.setContentView(view)
            }
            fun load(file: File): Bitmap {
                val done = CountDownLatch(1)
                val bitmap = AtomicReference<Bitmap>()
                val error = AtomicReference<String>()
                scenario.onActivity {
                    view.displayListener = object : DisplayListener {
                        override fun onStarted() = Unit
                        override fun onCompleted(drawable: Drawable, from: ImageFrom, attrs: ImageAttrs) {
                            bitmap.set((drawable as BitmapDrawable).bitmap); done.countDown()
                        }
                        override fun onError(cause: ErrorCause) { error.set(cause.toString()); done.countDown() }
                        override fun onCanceled(cause: CancelCause) { error.set(cause.toString()); done.countDown() }
                    }
                    factory.loadSillContent(view, Uri.fromFile(file))
                }
                assertTrue("Viewer decode timeout", done.await(15, TimeUnit.SECONDS))
                assertNull(error.get())
                return requireNotNull(bitmap.get())
            }
            val previewBitmap = load(preview)
            assertTrue(previewBitmap.width < previewBitmap.height)
            val target = load(original)
            assertTrue("HDR original must stay upright", target.width < target.height)
            assertTrue("Viewer must retain HDR", target.hasGainmap())
            scenario.onActivity { assertFalse(view.options.isCorrectImageOrientationDisabled) }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            android.os.SystemClock.sleep(300)
            val output = File(context.getExternalFilesDir(null), "orientation-qa").apply { mkdirs() }
            val copied = CountDownLatch(1)
            val copyResult = AtomicReference<Int>()
            lateinit var screenshot: Bitmap
            scenario.onActivity { activity ->
                val decor = activity.window.decorView
                screenshot = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(activity.window, screenshot, { result ->
                    copyResult.set(result); copied.countDown()
                }, Handler(Looper.getMainLooper()))
            }
            assertTrue(copied.await(10, TimeUnit.SECONDS))
            assertEquals(PixelCopy.SUCCESS, copyResult.get())
            File(output, "hdr-original-upright.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            screenshot.recycle()
        }
    }

    private fun difference(a: Bitmap, b: Bitmap): Double {
        var sum = 0.0
        for (y in 1..19) for (x in 1..19) {
            val first = a.getPixel(a.width * x / 20, a.height * y / 20)
            val second = b.getPixel(b.width * x / 20, b.height * y / 20)
            sum += abs(Color.red(first) - Color.red(second)) + abs(Color.green(first) - Color.green(second)) + abs(Color.blue(first) - Color.blue(second))
        }
        return sum / (19 * 19 * 3)
    }
}
