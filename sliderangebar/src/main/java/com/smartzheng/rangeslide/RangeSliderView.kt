package com.smartzheng.rangeslide

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import java.util.concurrent.TimeUnit

class RangeSliderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = -1) :
    View(context, attrs, defStyleAttr) {

    protected var paint: Paint

    protected var ripplePaint: Paint

    protected var mRadius: Float = 0.toFloat()

    protected var slotRadius: Float = 0.toFloat()

    private var currentIndex: Int = 0

    private var currentSlidingX: Float = 0.toFloat()

    private var currentSlidingY: Float = 0.toFloat()

    private var selectedSlotX: Float = 0.toFloat()

    private var selectedSlotY: Float = 0.toFloat()

    private var gotSlot = false

    private val slotPositions: FloatArray

    private var filledColor = DEFAULT_FILLED_COLOR

    private var emptyColor = DEFAULT_EMPTY_COLOR
    private var selectedTextColor = DEFAULT_SELECTED_TEXT_COLOR
    private var normalTextColor = DEFAULT_NORMAL_TEXT_COLOR

    private var barHeightPercent = DEFAULT_BAR_HEIGHT_PERCENT

    private var rangeCount = DEFAULT_RANGE_COUNT

    private var barHeight: Int = 0

    private var listener: OnSlideListener? = null

    private var rippleRadius = 0.0f

    private var downX: Float = 0.toFloat()

    private var downY: Float = 0.toFloat()


    private var slotRadiusPercent = DEFAULT_SLOT_RADIUS_PERCENT

    private var sliderRadiusPercent = DEFAULT_SLIDER_RADIUS_PERCENT

    private var layoutHeight: Int = 0
    private var textList = listOf<String>()

    val heightWithPadding: Int
        get() = height - paddingBottom - paddingTop

    val widthWithPadding: Int
        get() = width - paddingLeft - paddingRight

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.RangeSliderView)
            val sa = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.layout_height))
            try {
                layoutHeight = sa.getLayoutDimension(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                rangeCount = a.getInt(
                    R.styleable.RangeSliderView_rangeCount, DEFAULT_RANGE_COUNT
                )
                filledColor = a.getColor(
                    R.styleable.RangeSliderView_filledColor, DEFAULT_FILLED_COLOR
                )
                emptyColor = a.getColor(
                    R.styleable.RangeSliderView_emptyColor, DEFAULT_EMPTY_COLOR
                )
                barHeightPercent = a.getFloat(
                    R.styleable.RangeSliderView_barHeightPercent, DEFAULT_BAR_HEIGHT_PERCENT
                )
                barHeightPercent = a.getFloat(
                    R.styleable.RangeSliderView_barHeightPercent, DEFAULT_BAR_HEIGHT_PERCENT
                )
                slotRadiusPercent = a.getFloat(
                    R.styleable.RangeSliderView_slotRadiusPercent, DEFAULT_SLOT_RADIUS_PERCENT
                )
                sliderRadiusPercent = a.getFloat(
                    R.styleable.RangeSliderView_sliderRadiusPercent, DEFAULT_SLIDER_RADIUS_PERCENT
                )
                selectedTextColor = a.getInt(
                    R.styleable.RangeSliderView_selectedTextColor, DEFAULT_SELECTED_TEXT_COLOR
                )
                normalTextColor = a.getInt(
                    R.styleable.RangeSliderView_normalTextColor, DEFAULT_NORMAL_TEXT_COLOR
                )
            } finally {
                a.recycle()
                sa.recycle()
            }
        }

        setBarHeightPercent(barHeightPercent)
        setRangeCount(rangeCount)
        setSlotRadiusPercent(slotRadiusPercent)
        setSliderRadiusPercent(sliderRadiusPercent)

        slotPositions = FloatArray(rangeCount)
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = DEFAULT_PAINT_STROKE_WIDTH.toFloat()
        paint.style = Paint.Style.FILL_AND_STROKE

        ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        ripplePaint.strokeWidth = 2.0f
        ripplePaint.style = Paint.Style.FILL_AND_STROKE

        viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewTreeObserver.removeOnPreDrawListener(this)

                // Update mRadius after we got new height
                updateRadius(height)

                // Compute drawing position again
                preComputeDrawingPosition()

                // Ready to draw now
                return true
            }
        })
        currentIndex = 0
    }

    fun setRangeList(list: List<String>) {
        textList = list
        invalidate()
    }

    private fun updateRadius(height: Int) {
        barHeight = (height * barHeightPercent).toInt()
        mRadius = height * sliderRadiusPercent
        slotRadius = height * slotRadiusPercent
    }

    fun getRangeCount(): Int {
        return rangeCount
    }

    fun setRangeCount(rangeCount: Int) {
        if (rangeCount < 2) {
            throw IllegalArgumentException("rangeCount must be >= 2")
        }
        this.rangeCount = rangeCount
    }

    fun getBarHeightPercent(): Float {
        return barHeightPercent
    }

    fun setBarHeightPercent(percent: Float) {
        if (percent <= 0.0 || percent > 1.0) {
            throw IllegalArgumentException("Bar height percent must be in (0, 1]")
        }
        this.barHeightPercent = percent
    }

    fun getSlotRadiusPercent(): Float {
        return slotRadiusPercent
    }

    fun setSlotRadiusPercent(percent: Float) {
        if (percent <= 0.0 || percent > 1.0) {
            throw IllegalArgumentException("Slot mRadius percent must be in (0, 1]")
        }
        this.slotRadiusPercent = percent
    }

    fun getSliderRadiusPercent(): Float {
        return sliderRadiusPercent
    }

    fun setSliderRadiusPercent(percent: Float) {
        if (percent <= 0.0 || percent > 1.0) {
            throw IllegalArgumentException("Slider mRadius percent must be in (0, 1]")
        }
        this.sliderRadiusPercent = percent
    }


    fun setOnSlideListener(listener: OnSlideListener) {
        this.listener = listener
    }

    /**
     * Perform all the calculation before drawing, should only run once
     */
    private fun preComputeDrawingPosition() {
        val w = widthWithPadding
        val h = heightWithPadding

        /** Space between each slot  */
        val spacing = w / rangeCount

        /** Center vertical  */
        val y = paddingTop + h / 2
        currentSlidingY = y.toFloat()
        selectedSlotY = y.toFloat()
        /**
         * Try to center it, so start by half
         * <pre>
         *
         * Example for 4 slots
         *
         * ____o____|____o____|____o____|____o____
         * --space--
         *
        </pre> *
         */
        var x = paddingLeft + spacing / 2

        /** Store the position of each slot index  */
        for (i in 0 until rangeCount) {
            slotPositions[i] = x.toFloat()
            if (i == currentIndex) {
                currentSlidingX = x.toFloat()
                selectedSlotX = x.toFloat()
            }
            x += spacing
        }
    }

    fun setInitialIndex(index: Int) {
        try {
            if (index < 0 || index >= rangeCount) {
                throw IllegalArgumentException("Attempted to set index=$index out of range [0,$rangeCount]")
            }
            currentIndex = index
            selectedSlotX = slotPositions[currentIndex]
            currentSlidingX = selectedSlotX
            invalidate()
        } catch (e: IndexOutOfBoundsException) {
        }
    }

    fun getFilledColor(): Int {
        return filledColor
    }

    fun setFilledColor(filledColor: Int) {
        this.filledColor = filledColor
        invalidate()
    }

    fun getEmptyColor(): Int {
        return emptyColor
    }

    fun setEmptyColor(emptyColor: Int) {
        this.emptyColor = emptyColor
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    /**
     * Measures height according to the passed measure spec
     *
     * @param measureSpec int measure spec to use
     * @return int pixel size
     */
    private fun measureHeight(measureSpec: Int): Int {
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)
        var result: Int
        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            val height: Int
            if (layoutHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                height = dpToPx(context, DEFAULT_HEIGHT_IN_DP.toFloat())
            } else if (layoutHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
                height = measuredHeight
            } else {
                height = layoutHeight
            }
            result = height + paddingTop + paddingBottom + 2 * DEFAULT_PAINT_STROKE_WIDTH
            if (specMode == View.MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize)
            }
        }
        return result
    }

    /**
     * Measures width according to the passed measure spec
     *
     * @param measureSpec int measure spec to use
     * @return int pixel size
     */
    private fun measureWidth(measureSpec: Int): Int {
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)
        var result: Int
        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = specSize + paddingLeft + paddingRight + 2 * DEFAULT_PAINT_STROKE_WIDTH + (2 * mRadius).toInt()
            if (specMode == View.MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize)
            }
        }
        return result
    }


    private fun updateCurrentIndex() {
        var min = java.lang.Float.MAX_VALUE
        var j = 0
        /** Find the closest to x  */
        for (i in 0 until rangeCount) {
            val dx = Math.abs(currentSlidingX - slotPositions[i])
            if (dx < min) {
                min = dx
                j = i
            }
        }
        /** This is current index of slider  */
        if (j != currentIndex) {
            if (listener != null) {
                listener!!.onSlide(j, textList[j])
            }
        }
        currentIndex = j
        /** Correct position  */
        currentSlidingX = slotPositions[j]
        selectedSlotX = currentSlidingX
        downX = currentSlidingX
        downY = currentSlidingY
        invalidate()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val y = event.y
        val x = event.x
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                gotSlot = isInSelectedSlot(y)
                downX = x
                downY = y
                if (gotSlot && x >= slotPositions[0] && x <= slotPositions[rangeCount - 1]) {
                    currentSlidingX = x
                    currentSlidingY = y
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> if (gotSlot) {
                if (x >= slotPositions[0] && x <= slotPositions[rangeCount - 1]) {
                    currentSlidingX = x
                    currentSlidingY = y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> if (gotSlot) {
                gotSlot = false
                currentSlidingX = x
                currentSlidingY = y
                updateCurrentIndex()
            }
        }
        return true
    }

    private fun isInSelectedSlot(y: Float): Boolean {
        return selectedSlotY - mRadius * 2 <= y && y <= selectedSlotY + mRadius * 2
    }

    private fun drawEmptySlots(canvas: Canvas) {
        paint.color = emptyColor
        val h = heightWithPadding
        val y = paddingTop + (h shr 1)
        for (i in 0 until rangeCount) {
            canvas.drawCircle(slotPositions[i], y.toFloat(), slotRadius, paint)
        }
    }

    private fun drawText(canvas: Canvas) {
        paint.textSize = context.sp(10).toFloat()
        paint.strokeWidth = 0.8f
        val h = heightWithPadding
        val y = paddingTop + (h shr 1)
        for (i in 0 until rangeCount) {
            if (i == currentIndex) {
                paint.color = Color.BLACK
            } else {
                paint.color = Color.GRAY
            }
            canvas.drawText(
                textList[i],
                slotPositions[i] - paint.measureText(textList[i]) / 2,
                y.toFloat() + context.dip(18) + slotRadius,
                paint
            )
        }
    }

    private fun drawFilledSlots(canvas: Canvas) {
        paint.color = filledColor
        val h = heightWithPadding
        val y = paddingTop + (h shr 1)
        for (i in 0 until rangeCount) {
            if (slotPositions[i] <= currentSlidingX) {
                canvas.drawCircle(slotPositions[i], y.toFloat(), slotRadius, paint)
            }
        }
    }

    private fun drawBar(canvas: Canvas, from: Int, to: Int, color: Int) {
        paint.color = color
        val h = heightWithPadding
        val half = barHeight shr 1
        val y = paddingTop + (h shr 1)
        canvas.drawRect(from.toFloat(), (y - half).toFloat(), to.toFloat(), (y + half).toFloat(), paint)
    }


    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = widthWithPadding
        val h = heightWithPadding
        val spacing = w / rangeCount
        val x0 = paddingLeft + (spacing shr 1)
        val y0 = paddingTop + (h shr 1)
        drawEmptySlots(canvas)
        drawText(canvas)
        drawFilledSlots(canvas)

        /** Draw empty bar  */
        drawBar(canvas, slotPositions[0].toInt(), slotPositions[rangeCount - 1].toInt(), emptyColor)

        /** Draw filled bar  */
        drawBar(canvas, x0, currentSlidingX.toInt(), filledColor)

        /** Draw the selected range circle  */
        paint.color = filledColor
        canvas.drawCircle(currentSlidingX, y0.toFloat(), mRadius, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(currentSlidingX, y0.toFloat(), mRadius / 4, paint)
    }


    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.saveIndex = this.currentIndex
        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        this.currentIndex = state.saveIndex
    }

    internal class SavedState : View.BaseSavedState {
        var saveIndex: Int = 0

        constructor(superState: Parcelable) : super(superState) {}

        private constructor(`in`: Parcel) : super(`in`) {
            this.saveIndex = `in`.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(this.saveIndex)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    /**
     * Interface to keep track sliding position
     */
    interface OnSlideListener {

        /**
         * Notify when slider change to new index position
         *
         * @param index The index value of range count [0, rangeCount - 1]
         */
        fun onSlide(index: Int, s: String)
    }

    companion object {

        private val TAG = RangeSliderView::class.java.simpleName

        private val RIPPLE_ANIMATION_DURATION_MS = TimeUnit.MILLISECONDS.toMillis(700)

        private const val DEFAULT_PAINT_STROKE_WIDTH = 5

        private val DEFAULT_FILLED_COLOR = Color.parseColor("#FFA500")

        private val DEFAULT_EMPTY_COLOR = Color.parseColor("#C3C3C3")
        private const val DEFAULT_NORMAL_TEXT_COLOR = Color.GRAY
        private const val DEFAULT_SELECTED_TEXT_COLOR = Color.BLACK

        private const val DEFAULT_BAR_HEIGHT_PERCENT = 0.10f

        private const val DEFAULT_SLOT_RADIUS_PERCENT = 0.125f

        private const val DEFAULT_SLIDER_RADIUS_PERCENT = 0.25f

        private const val DEFAULT_RANGE_COUNT = 5

        private const val DEFAULT_HEIGHT_IN_DP = 50

        /**
         * Helper method to convert pixel to dp
         *
         * @param context
         * @param px
         * @return
         */
        internal fun pxToDp(context: Context, px: Float): Int {
            return (px / context.resources.displayMetrics.density).toInt()
        }

        /**
         * Helper method to convert dp to pixel
         *
         * @param context
         * @param dp
         * @return
         */
        internal fun dpToPx(context: Context, dp: Float): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }
    }
}
