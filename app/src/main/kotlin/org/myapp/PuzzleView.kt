package org.myapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

/**
 * A 10x10 (100 piece) tap-to-swap puzzle.
 *
 * Tap one piece, then tap another to swap them.
 * Pieces that sit in the right place get a green outline.
 */
class PuzzleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val GRID = 10
        const val PIECES = GRID * GRID
    }

    /** Called after every move with (pieces in place, total pieces). */
    var onProgress: ((Int, Int) -> Unit)? = null

    /** Called once when every piece is in place. */
    var onSolved: (() -> Unit)? = null

    private var source: Bitmap? = null

    // order[slot] = index of the piece currently sitting in that slot.
    // A piece is correct when order[slot] == slot.
    private var order = IntArray(PIECES) { it }

    private var selected = -1
    private var solved = false

    private val srcRect = Rect()
    private val dstRect = Rect()

    private val gridPaint = Paint().apply {
        color = Color.argb(70, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val selectPaint = Paint().apply {
        color = Color.parseColor("#FFB300")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val correctPaint = Paint().apply {
        color = Color.parseColor("#43A047")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val emptyPaint = Paint().apply { color = Color.parseColor("#1E2633") }

    val hasImage: Boolean get() = source != null

    /** Crops the image to a centered square and starts a shuffled game. */
    fun setImage(bitmap: Bitmap) {
        val side = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - side) / 2
        val y = (bitmap.height - side) / 2
        source = Bitmap.createBitmap(bitmap, x, y, side, side)
        shuffle()
    }

    fun shuffle() {
        if (source == null) return
        order = IntArray(PIECES) { it }
        // Fisher-Yates, retried until it isn't already solved.
        do {
            for (i in PIECES - 1 downTo 1) {
                val j = Random.nextInt(i + 1)
                val t = order[i]; order[i] = order[j]; order[j] = t
            }
        } while (countCorrect() == PIECES)
        selected = -1
        solved = false
        notifyProgress()
        invalidate()
    }

    private fun countCorrect(): Int {
        var n = 0
        for (i in 0 until PIECES) if (order[i] == i) n++
        return n
    }

    private fun notifyProgress() {
        onProgress?.invoke(countCorrect(), PIECES)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Keep the board square.
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) w
        else minOf(w, h)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = source
        if (bmp == null) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), emptyPaint)
            return
        }

        val cell = width / GRID.toFloat()
        val srcCell = bmp.width / GRID

        for (slot in 0 until PIECES) {
            val piece = order[slot]

            srcRect.set(
                (piece % GRID) * srcCell,
                (piece / GRID) * srcCell,
                (piece % GRID) * srcCell + srcCell,
                (piece / GRID) * srcCell + srcCell
            )
            val left = (slot % GRID) * cell
            val top = (slot / GRID) * cell
            dstRect.set(
                left.toInt(), top.toInt(),
                (left + cell).toInt(), (top + cell).toInt()
            )

            canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
            canvas.drawRect(dstRect, gridPaint)

            if (!solved && order[slot] == slot) {
                canvas.drawRect(
                    dstRect.left + 2f, dstRect.top + 2f,
                    dstRect.right - 2f, dstRect.bottom - 2f, correctPaint
                )
            }
        }

        if (selected >= 0) {
            val left = (selected % GRID) * cell
            val top = (selected / GRID) * cell
            canvas.drawRect(left + 3f, top + 3f, left + cell - 3f, top + cell - 3f, selectPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (source == null || solved) return false

        if (event.action == MotionEvent.ACTION_DOWN) return true

        if (event.action == MotionEvent.ACTION_UP) {
            val cell = width / GRID.toFloat()
            val col = (event.x / cell).toInt().coerceIn(0, GRID - 1)
            val row = (event.y / cell).toInt().coerceIn(0, GRID - 1)
            val slot = row * GRID + col

            when {
                selected == -1 -> selected = slot
                selected == slot -> selected = -1
                else -> {
                    val t = order[selected]
                    order[selected] = order[slot]
                    order[slot] = t
                    selected = -1

                    val correct = countCorrect()
                    notifyProgress()
                    if (correct == PIECES) {
                        solved = true
                        onSolved?.invoke()
                    }
                }
            }
            invalidate()
            performClick()
            return true
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
