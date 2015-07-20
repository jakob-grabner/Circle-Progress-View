package at.grabner.circleprogress;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.lang.ref.WeakReference;

/**
 * An circle view, similar to Android's ProgressBar.
 * Can be used in 'value mode' or 'spinning mode'.
 *
 * In spinning mode it can be used like a intermediate progress bar.
 *
 * In value mode it can be used as a progress bar or to visualize any other value.
 * Setting a value is fully animated. There are also nice transitions from animating to value mode.
 *
 * Typical use case would be to load a new value. During the loading time set the CircleView to spinning.
 * As soon as you got your nur value, just set it {@link #setValueAnimated(float, long) setValueAnimated}, it automatically animated.
 *
 *
 * @author Jakob Grabner, based on the Progress wheel of Todd Davies
 *         https://github.com/Todd-Davies/CircleView
 *
 *         Licensed under the Creative Commons Attribution 3.0 license see:
 *         http://creativecommons.org/licenses/by/3.0/
 */
public class CircleProgressView extends View {

    /**
     * The log tag.
     */
    private final static String TAG = "CircleView";
    private static final boolean DEBUG =  false;

    //region members
    //value animation
    private float mCurrentValue = 15;
    private float mValueTo = 0;
    private float mValueFrom = 0;
    private float mMaxValue = 100;

    // spinner animation
    private float mSpinningBarLengthCurrent = 0;
    private float mSpinningBarLengthOrig = 42;
    private float mCurrentSpinnerDegreeValue = 0;


    //Sizes (with defaults)
    private int mLayoutHeight = 0;
    private int mLayoutWidth = 0;
    private int mFullRadius = 100;
    private int mCircleRadius = 80;
    private int mBarWidth = 40;
    private int mRimWidth = 40;
    private int mTextSize = 20;
    private float mContourSize = 1;
    private float mTextScale = 1;
    private float mUnitScale = 1;

    //Padding (with defaults)
    private int mPaddingTop = 5;
    private int mPaddingBottom = 5;
    private int mPaddingLeft = 5;
    private int mPaddingRight = 5;
    //Colors (with defaults)
    private final int mBarColorStandard =  0xff009688; //stylish blue
    private int mContourColor = 0xAA000000;
    private int mSpinnerColor = mBarColorStandard; //stylish blue
    private int mFillColor = 0x00000000;  //transparent
    private int mRimColor = 0xAA83d0c9;
    private int mTextColor = 0xFF000000;
    private int mUnitColor = 0xFF000000;
    private int[] mBarColors = new int[]{
            mBarColorStandard, //stylish blue
            mBarColorStandard, //stylish blue
    };
    //Caps
    private Paint.Cap mBarStrokeCap = Paint.Cap.BUTT;
    private Paint.Cap mSpinnerStrokeCap = Paint.Cap.BUTT;
    //Paints
    private Paint mBarPaint = new Paint();
    private Paint mBarSpinnerPaint = new Paint();
    private Paint mCirclePaint = new Paint();
    private Paint mRimPaint = new Paint();
    private Paint mTextPaint = new Paint();
    private Paint mUnitTextPaint = new Paint();
    private Paint mContourPaint = new Paint();
    //Rectangles
    private RectF mCircleBounds = new RectF();
    private RectF mInnerCircleBounds = new RectF();
    private RectF mTextBounds = new RectF();
    private RectF mUnitBounds = new RectF();
    private RectF mCircleOuterContour = new RectF();
    private RectF mCircleInnerContour = new RectF();
    //Animation
    //The amount of degree to move the bar by on each draw
    private float mSpinSpeed = 2.8f;
    private double mAnimationDuration = 900;

    //The number of milliseconds to wait in between each draw
    private int mDelayMillis = 15;

    // helper for AnimationState.END_SPINNING_START_ANIMATING
    private boolean mDrawArcWhileSpinning;

    //The animation handler containing the animation state machine.
    private Handler mAnimationHandler = new AnimationHandler(this);

    //The current state of the animation state machine.
    private AnimationState mAnimationState = AnimationState.IDLE;

    // The interpolator for value animations
    private TimeInterpolator mInterpolator = new AccelerateDecelerateInterpolator();

    //Other
    // The text to show
    private String mText = "";

    private String mUnit = "";
    private int mTextLength;
    /**
     * Indicates if the given text or the current percentage value should be shown.
     * true: current value (percentage or value)
     * false: given text
     */
    private boolean mAutoTextValue;
    private boolean mAutoTextSize;
    private boolean mShowPercentAsAutoValue = true;


    private boolean mShowUnit = false;
    /**
     * Relative size of the unite string to the value string.
     */
    private float mRelativeUniteSize = 0.3f;




    //endregion members

    /**
     * The constructor for the CircleView
     *
     * @param context
     * @param attrs
     */
    public CircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);

        parseAttributes(context.obtainStyledAttributes(attrs,
                R.styleable.CircleProgressView));
    }

    /**
     * Parse the attributes passed to the view from the XML
     *
     * @param a the attributes to parse
     */
    private void parseAttributes(TypedArray a) {
        setBarWidth((int) a.getDimension(R.styleable.CircleProgressView_barWidth,
                mBarWidth));

        setRimWidth((int) a.getDimension(R.styleable.CircleProgressView_rimWidth,
                mRimWidth));

        setSpinSpeed((int) a.getDimension(R.styleable.CircleProgressView_spinSpeed,
                mSpinSpeed));


        if (a.hasValue(R.styleable.CircleProgressView_barColor) && a.hasValue(R.styleable.CircleProgressView_barColor1) && a.hasValue(R.styleable.CircleProgressView_barColor2) && a.hasValue(R.styleable.CircleProgressView_barColor3)) {
            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_barColor1, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_barColor2, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_barColor3, mBarColorStandard)};

        } else if (a.hasValue(R.styleable.CircleProgressView_barColor) && a.hasValue(R.styleable.CircleProgressView_barColor1) && a.hasValue(R.styleable.CircleProgressView_barColor2)) {

            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_barColor1, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_barColor2, mBarColorStandard)};

        } else if (a.hasValue(R.styleable.CircleProgressView_barColor) && a.hasValue(R.styleable.CircleProgressView_barColor1)) {

            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_barColor1, mBarColorStandard)};

        } else {
            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_barColor, mBarColorStandard)};
        }

        setSpinBarColor(a.getColor(R.styleable.CircleProgressView_spinColor, mSpinnerColor));

        setSpinningBarLength(mSpinningBarLengthOrig = a.getDimension(R.styleable.CircleProgressView_spinBarLength,
                mSpinningBarLengthOrig));

        setTextSize((int) a.getDimension(R.styleable.CircleProgressView_textSize, -1));


        setTextColor(a.getColor(R.styleable.CircleProgressView_textColor,
                -1));

        setUnitColor(a.getColor(R.styleable.CircleProgressView_unitColor,
                -1));

        //if the mText is empty, show current percentage value
        setText(a.getString(R.styleable.CircleProgressView_text));


        setRimColor(a.getColor(R.styleable.CircleProgressView_rimColor,
                mRimColor));

        setFillColor(a.getColor(R.styleable.CircleProgressView_fillColor,
                mFillColor));

        setContourColor(a.getColor(R.styleable.CircleProgressView_contourColor, mContourColor));
        setContourSize(a.getDimension(R.styleable.CircleProgressView_contourSize, mContourSize));

        setMaxValue(a.getDimension(R.styleable.CircleProgressView_maxValue, mMaxValue));

        setUnit(a.getString(R.styleable.CircleProgressView_unit));
        setShowUnit(a.getBoolean(R.styleable.CircleProgressView_showUnit, mShowUnit));

        setTextScale(a.getDimension(R.styleable.CircleProgressView_textScale, mTextScale));
        setUnitScale(a.getDimension(R.styleable.CircleProgressView_unitScale, mUnitScale));

        // Recycle
        a.recycle();
    }

    /*
 * When this is called, make the view square.
 * From: http://www.jayway.com/2012/12/12/creating-custom-android-views-part-4-measuring-and-how-to-force-a-view-to-be-square/
 *
 */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The first thing that happen is that we call the superclass
        // implementation of onMeasure. The reason for that is that measuring
        // can be quite a complex process and calling the super method is a
        // convenient way to get most of this complexity handled.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // We can’t use getWidth() or getHeight() here. During the measuring
        // pass the view has not gotten its final size yet (this happens first
        // at the start of the layout pass) so we have to use getMeasuredWidth()
        // and getMeasuredHeight().
        int size = 0;
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
        int heightWithoutPadding = height - getPaddingTop() - getPaddingBottom();


        // Finally we have some simple logic that calculates the size of the view
        // and calls setMeasuredDimension() to set that size.
        // Before we compare the width and height of the view, we remove the padding,
        // and when we set the dimension we add it back again. Now the actual content
        // of the view will be square, but, depending on the padding, the total dimensions
        // of the view might not be.
        if (widthWithoutPadding > heightWithoutPadding) {
            size = heightWithoutPadding;
        } else {
            size = widthWithoutPadding;
        }

        // If you override onMeasure() you have to call setMeasuredDimension().
        // This is how you report back the measured size.  If you don’t call
        // setMeasuredDimension() the parent will throw an exception and your
        // application will crash.
        // We are calling the onMeasure() method of the superclass so we don’t
        // actually need to call setMeasuredDimension() since that takes care
        // of that. However, the purpose with overriding onMeasure() was to
        // change the default behaviour and to do that we need to call
        // setMeasuredDimension() with our own values.
        setMeasuredDimension(size + getPaddingLeft() + getPaddingRight(), size + getPaddingTop() + getPaddingBottom());
    }


    private RectF getInnerCircleRect(RectF _circleBounds) {

        double circleWidth = +_circleBounds.width()-(Math.max(mBarWidth, mRimWidth)) - (mContourSize *2);
//        circleWidth = _circleBounds.width();
        double width = ((circleWidth/2d) * Math.sqrt(2d));
        float widthDelta = (_circleBounds.width() - (float) width) / 2f;

        float scaleX = 1;
        float scaleY = 1;
        if (isShowUnit()) {
            scaleX = 0.77f; // scaleX square to rectangle, so the longer text with unit fits better
            scaleY = 1.33f;
        }

        return new RectF(_circleBounds.left + (widthDelta*scaleX), _circleBounds.top + (widthDelta*scaleY), _circleBounds.right - (widthDelta*scaleX), _circleBounds.bottom -  (widthDelta*scaleY));

    }

    private  float calcTextSizeForCircle(String _text, Paint _textPaint, RectF _circleBounds) {

        //get mTextBounds bounds
        RectF innerCircleBounds = getInnerCircleRect(_circleBounds);
        return calcTextSizeForRect(_text, _textPaint, innerCircleBounds);

    }

    private static float calcTextSizeForRect(String _text, Paint _textPaint, RectF _rectBounds) {

        Matrix matrix = new Matrix();
        Rect textBoundsTmp = new Rect();


        //get current mText bounds
        _textPaint.getTextBounds(_text, 0, _text.length(), textBoundsTmp);
        RectF textBoundsTmpF = new RectF(textBoundsTmp);

        matrix.setRectToRect(textBoundsTmpF, _rectBounds, Matrix.ScaleToFit.CENTER);
        float values[] = new float[9];
        matrix.getValues(values);
        return _textPaint.getTextSize() * values[Matrix.MSCALE_X];


    }

    /**
     * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
     * because this method is called after measuring the dimensions of MATCH_PARENT & WRAP_CONTENT.
     * Use this dimensions to setup the bounds and paints.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Share the dimensions
        mLayoutWidth = w;
        mLayoutHeight = h;

        setupBounds();
        setupPaints();
        invalidate();
    }

    //region setter / getter


    public Paint.Cap getSpinnerStrokeCap() {
        return mSpinnerStrokeCap;
    }

    public void setSpinnerStrokeCap(Paint.Cap _spinnerStrokeCap) {
        mSpinnerStrokeCap = _spinnerStrokeCap;
    }

    public Paint.Cap getBarStrokeCap() {
        return mBarStrokeCap;
    }

    public void setBarStrokeCap(Paint.Cap _barStrokeCap) {
        mBarStrokeCap = _barStrokeCap;
    }

    public int getContourColor() {
        return mContourColor;
    }

    public void setContourColor(int _contourColor) {
        mContourColor = _contourColor;
    }

    public float getContourSize() {
        return mContourSize;
    }

    public void setContourSize(float _contourSize) {
        mContourSize = _contourSize;
    }

    /**
     * Set the text in the mCurrentValue bar. If no text (null or empty) is specified,
     * * the current percentage value is used
     * * (max value and current value are mandatory to do so).
     * *
     */
    public void setText(String text) {
        if (text == null || text.isEmpty()) {
            mText = "";
            mAutoTextValue = true;
        } else {
            mText = text;
            mAutoTextValue = false;
        }
        invalidate();
    }

    public String getUnit() {
        return mUnit;
    }

    public void setUnit(String _unit) {
        if (_unit == null) {
            mUnit = "";
        } else {
            mUnit = _unit;
        }
        invalidate();
    }

    /**
     * Returns the bounding rectangle of the given _text, with the size and style defined in the _textPaint centered in the middle of the _textBounds
     * @param _text   The text.
     * @param _textPaint The paint defining the text size and style.
     * @param _textBounds  The rect where the text will be centered.
     * @return The boinding box of the text centered in the _textBounds.
     */
    private  RectF getTextBounds(String _text, Paint _textPaint, RectF _textBounds) {

        Rect textBoundsTmp = new Rect();

        //get current text bounds
        _textPaint.getTextBounds(_text, 0, _text.length(), textBoundsTmp);

        //center in circle
        RectF textRect = new RectF();
        textRect.left = (_textBounds.left + ((_textBounds.width() - textBoundsTmp.width()) / 2));
        textRect.top = _textBounds.top + ((_textBounds.height() - textBoundsTmp.height()) / 2);
        textRect.right = textRect.left + textBoundsTmp.width();
        textRect.bottom = textRect.top + textBoundsTmp.height();



        return textRect;
    }

    public boolean isShowPercentAsAutoValue() {
        return mShowPercentAsAutoValue;
    }

    /**
     * If true and no text was specified. The percentage (max value, current value) is shown.
     * If false the current value is shown.
     * @param _showPercentAsAutoValue bool
     */
    public void setShowPercentAsAutoValue(boolean _showPercentAsAutoValue) {
        mShowPercentAsAutoValue = _showPercentAsAutoValue;
    }

    public int getTextSize() {
        return mTextSize;
    }

    /**
     * If text size was not set, it is automatically calculated
     * @param textSize The text size or 0 enable automatic size (default)
     */
    public void setTextSize(int textSize) {
        if (textSize > 0) {
            this.mTextSize = textSize;
            mAutoTextSize = false;
        } else {
            mAutoTextSize = true;
        }
    }

    public double getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(float _maxValue) {
        mMaxValue = _maxValue;
    }

    public int getPaddingTop() {
        return mPaddingTop;
    }

    public void setPaddingTop(int paddingTop) {
        this.mPaddingTop = paddingTop;
    }

    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.mPaddingBottom = paddingBottom;
    }

    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.mPaddingLeft = paddingLeft;
    }

    public int getPaddingRight() {
        return mPaddingRight;
    }

    public void setPaddingRight(int paddingRight) {
        this.mPaddingRight = paddingRight;
    }

    public int getCircleRadius() {
        return mCircleRadius;
    }

    public boolean isShowUnit() {
        return mShowUnit;
    }

    public void setShowUnit(boolean _showUnit) {
        mShowUnit = _showUnit;
        mTextLength = 0; // triggers recalculating text sizes
        invalidate();
        mInnerCircleBounds = getInnerCircleRect(mCircleBounds);
    }


    /**
     * @return The scale value
     */
    public float getUnitScale() {
        return mUnitScale;
    }

    /**
     * Scale factor for unit text next to the main text
     * @param _unitScale The scale value between. 1 >= value > 0
     */
    public void setUnitScale(float _unitScale) {
        mUnitScale = _unitScale;
    }

    /**
     * @return The scale value
     */
    public float getTextScale() {
        return mTextScale;
    }
    /**
     * Scale factor for main text in the center of the circle view.
     * Only relevant if auto text size is enabled.
     * @param _textScale The scale value.
     */
    public void setTextScale(float _textScale) {
        mTextScale = _textScale;
    }

    /**
     * Length of spinning bar in degree.
     *
     * @param barLength length in degree
     */
    public void setSpinningBarLength(float barLength) {
        this.mSpinningBarLengthCurrent = mSpinningBarLengthOrig = barLength;
    }

    public int getBarWidth() {
        return mBarWidth;
    }

    public void setBarWidth(int barWidth) {
        this.mBarWidth = barWidth;
    }

    public int[] getBarColors() {
        return mBarColors;
    }

    public void setBarColor(int... barColors) {
        if (barColors.length == 1) {
            this.mBarColors = new int[]{barColors[0], barColors[0]};
        } else {
            this.mBarColors = barColors;
        }

    }

    public void setSpinBarColor(int _color) {
        mSpinnerColor = _color;
    }

    public int getFillColor() {
        return mFillColor;
    }

    public void setFillColor(int circleColor) {
        this.mFillColor = circleColor;
    }

    public int getRimColor() {
        return mRimColor;
    }

    public void setRimColor(int rimColor) {
        this.mRimColor = rimColor;
    }

    public Shader getRimShader() {
        return mRimPaint.getShader();
    }

    public void setRimShader(Shader shader) {
        this.mRimPaint.setShader(shader);
    }

    public int getTextColor(double value) {

        double percent = 1f / getMaxValue() * value;

        int index = (int) (mBarColors.length * percent);
        return mBarColors[index >= mBarColors.length ? mBarColors.length - 1 : index];
    }

    public int getTextColor() {
        return mTextColor;
    }

    /**
     * *
     * @param textColor the color or -1 to use auto color (depending on bar colors)
     */
    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
    }

    public void setUnitColor(int textColor) {
        this.mUnitColor = textColor;
    }

    public float getSpinSpeed() {
        return mSpinSpeed;
    }

    /**
     *  The amount of degree to move the bar by on each draw
     * @param spinSpeed the speed of the spinner
     */
    public void setSpinSpeed(float spinSpeed) {
        this.mSpinSpeed = spinSpeed;
    }

    public int getRimWidth() {
        return mRimWidth;
    }

    public void setRimWidth(int rimWidth) {
        this.mRimWidth = rimWidth;
    }

    public int getDelayMillis() {
        return mDelayMillis;
    }

    public void setDelayMillis(int delayMillis) {
        this.mDelayMillis = delayMillis;
    }

    //endregion getter / setter

    //----------------------------------
    //region Setting up stuff
    //----------------------------------

    /**
     * Set the properties of the paints we're using to
     * draw all the things.
     *
     * Call this if you change colors after the view was drawn
     */
    public void setupPaints() {
        mBarPaint.setShader(new SweepGradient(mCircleBounds.centerX(), mCircleBounds.centerY(), mBarColors, null));
        Matrix matrix = new Matrix();
        mBarPaint.getShader().getLocalMatrix(matrix);

        matrix.postTranslate(-mCircleBounds.centerX(), -mCircleBounds.centerY());
        matrix.postRotate(-90);
        matrix.postTranslate(mCircleBounds.centerX(), mCircleBounds.centerY());
        mBarPaint.getShader().setLocalMatrix(matrix);


        mBarPaint.setAntiAlias(true);
        mBarPaint.setStrokeCap(mBarStrokeCap);
        mBarPaint.setStyle(Style.STROKE);
        mBarPaint.setStrokeWidth(mBarWidth);

        mBarSpinnerPaint.setAntiAlias(true);
        mBarSpinnerPaint.setStrokeCap(mSpinnerStrokeCap);
        mBarSpinnerPaint.setStyle(Style.STROKE);
        mBarSpinnerPaint.setStrokeWidth(mBarWidth);
        mBarSpinnerPaint.setColor(mSpinnerColor);

        mRimPaint.setColor(mRimColor);
        mRimPaint.setAntiAlias(true);
        mRimPaint.setStyle(Style.STROKE);
        mRimPaint.setStrokeWidth(mRimWidth);

        mCirclePaint.setColor(mFillColor);
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Style.FILL);

        mTextPaint.setColor(mTextColor);
        mTextPaint.setStyle(Style.FILL);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(getTextSize());

        mUnitTextPaint.setColor(mUnitColor);
        mUnitTextPaint.setStyle(Style.FILL);
        mUnitTextPaint.setAntiAlias(true);
        mUnitTextPaint.setTextSize(getTextSize());

        mContourPaint.setColor(mContourColor);
        mContourPaint.setAntiAlias(true);
        mContourPaint.setStyle(Style.STROKE);
        mContourPaint.setStrokeWidth(mContourSize);
    }

    /**
     * Set the bounds of the component
     */
    private void setupBounds() {
        // Width should equal to Height, find the min value to setup the circle
        int minValue = Math.min(mLayoutWidth, mLayoutHeight);

        // Calc the Offset if needed
        int xOffset = mLayoutWidth - minValue;
        int yOffset = mLayoutHeight - minValue;

        // Add the offset
        mPaddingTop = this.getPaddingTop() + (yOffset / 2);
        mPaddingBottom = this.getPaddingBottom() + (yOffset / 2);
        mPaddingLeft = this.getPaddingLeft() + (xOffset / 2);
        mPaddingRight = this.getPaddingRight() + (xOffset / 2);

        int width = getWidth(); //this.getLayoutParams().width;
        int height = getHeight(); //this.getLayoutParams().height;


        mCircleBounds = new RectF(mPaddingLeft + mBarWidth,
                mPaddingTop + mBarWidth,
                width - mPaddingRight - mBarWidth,
                height - mPaddingBottom - mBarWidth);
        mInnerCircleBounds = getInnerCircleRect(mCircleBounds);
        mCircleInnerContour = new RectF(mCircleBounds.left + (mRimWidth / 2.0f) + (mContourSize / 2.0f), mCircleBounds.top + (mRimWidth / 2.0f) + (mContourSize / 2.0f), mCircleBounds.right - (mRimWidth / 2.0f) - (mContourSize / 2.0f), mCircleBounds.bottom - (mRimWidth / 2.0f) - (mContourSize / 2.0f));
        mCircleOuterContour = new RectF(mCircleBounds.left - (mRimWidth / 2.0f) - (mContourSize / 2.0f), mCircleBounds.top - (mRimWidth / 2.0f) - (mContourSize / 2.0f), mCircleBounds.right + (mRimWidth / 2.0f) + (mContourSize / 2.0f), mCircleBounds.bottom + (mRimWidth / 2.0f) + (mContourSize / 2.0f));

        mFullRadius = (width - mPaddingRight - mBarWidth) / 2;
        mCircleRadius = (mFullRadius - mBarWidth) + 1;
    }
    //endregion Setting up stuff

    //-----------------------
    //region draw all the things
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (DEBUG) {

            drawDebug(canvas);
        }

        float degrees = (360f / mMaxValue * mCurrentValue);

        //Draw the inner circle
        canvas.drawArc(mCircleBounds, 360, 360, false, mCirclePaint);
        //Draw the rim
        if(mRimWidth > 0){
            canvas.drawArc(mCircleBounds, 360, 360, false, mRimPaint);
        }
        if(mContourSize > 0){
            canvas.drawArc(mCircleOuterContour, 360, 360, false, mContourPaint);
            canvas.drawArc(mCircleInnerContour, 360, 360, false, mContourPaint);
        }


        //Draw spinner
        if (mAnimationState == AnimationState.SPINNING || mAnimationState == AnimationState.END_SPINNING) {
            drawSpinningArc(canvas);

        } else if (mAnimationState == AnimationState.END_SPINNING_START_ANIMATING) {
            //draw spinning arc
            drawSpinningArc(canvas);

            if (mDrawArcWhileSpinning) {
                drawCircleWithNumber(canvas, degrees);
            }

        } else {


            drawCircleWithNumber(canvas, degrees);
        }

    }

    private void drawSpinningArc(Canvas canvas) {

        if (mSpinningBarLengthCurrent < 0)
            mSpinningBarLengthCurrent = 1;
        float startAngle = (mCurrentSpinnerDegreeValue - 90 - mSpinningBarLengthCurrent);
        canvas.drawArc(mCircleBounds, startAngle, mSpinningBarLengthCurrent, false,
                mBarSpinnerPaint);

    }

    private void drawDebug(Canvas canvas){
        Paint innerRectPaint = new Paint();
        innerRectPaint.setColor(Color.YELLOW);
        canvas.drawRect(mCircleBounds, innerRectPaint);

        innerRectPaint.setColor(Color.MAGENTA);
        canvas.drawRect(mInnerCircleBounds, innerRectPaint);
    }

    private void drawCircleWithNumber(Canvas canvas, float _degrees) {

        float relativeGap = 1.03f; //gap size between text and unit
        boolean update = false;
        canvas.drawArc(mCircleBounds, -90, _degrees, false, mBarPaint);
        mTextPaint.setSubpixelText(true);
        mTextPaint.setLinearText(true);
        mTextPaint.setTypeface(Typeface.MONOSPACE);
        //Draw Text
        if (mTextColor == -1) //undefined
        {
            mTextPaint.setColor(getTextColor(mCurrentValue));
        }

        //set text
        String text = mText;
        if (mAutoTextValue) {
            if (mShowPercentAsAutoValue) {
                int percent = (int) (100 / mMaxValue * mCurrentValue);
                text = String.valueOf(percent);
            } else {
                text = String.valueOf((int) mCurrentValue);
            }
        }

        //set text size and position


        if (mAutoTextSize) {  // only re-calc position and size if string length changed
            if (mTextLength != text.length()) {
                update = true;
                mTextLength = text.length();
                if (mTextLength == 1) {
                    mInnerCircleBounds = new RectF(mInnerCircleBounds.left + (mInnerCircleBounds.width() * 0.1f), mInnerCircleBounds.top, mInnerCircleBounds.right - (mInnerCircleBounds.width() * 0.1f), mInnerCircleBounds.bottom);
                }else {
                    mInnerCircleBounds = getInnerCircleRect(mCircleBounds);
                }
                RectF textRect = mInnerCircleBounds;

                if (mShowUnit) {
                    //shrink text Rect so that there is space for the unit
                    textRect = new RectF(mInnerCircleBounds.left, mInnerCircleBounds.top, mInnerCircleBounds.right - ((mInnerCircleBounds.width() * (mRelativeUniteSize))*relativeGap), mInnerCircleBounds.bottom);
                }

                mTextPaint.setTextSize(calcTextSizeForRect(text, mTextPaint, textRect)* mTextScale);
                mTextBounds = getTextBounds(text, mTextPaint, textRect); // center text in text rect
            }


        } else {
            mTextBounds = getTextBounds(text, mTextPaint, mCircleBounds); //center text in circle
        }

        if (DEBUG){
            Paint rectPaint = new Paint();
            rectPaint.setColor(Color.GREEN);
            canvas.drawRect(mTextBounds, rectPaint);
        }

        canvas.drawText(text, mTextBounds.left-(mTextPaint.getTextSize()*0.09f), mTextBounds.bottom, mTextPaint);


        if (mShowUnit) {


            if (mUnitColor == -1) //undefined
            {
                mUnitTextPaint.setColor(getTextColor(mCurrentValue));
            }
            if (update) {
                //calc unit text position
                mUnitBounds = new RectF(mInnerCircleBounds.left + (mInnerCircleBounds.width() * (1 - mRelativeUniteSize) * relativeGap), mInnerCircleBounds.top, mInnerCircleBounds.right, mInnerCircleBounds.bottom);
                mUnitTextPaint.setTextSize(calcTextSizeForRect(mUnit, mUnitTextPaint, mUnitBounds) * mUnitScale);
                mUnitBounds = getTextBounds(mUnit, mUnitTextPaint, mUnitBounds);
                //move unite to top of text
                float dy = mTextBounds.top - mUnitBounds.top;
                mUnitBounds.offset(0, dy);
            }

            if (DEBUG){
                Paint rectPaint = new Paint();
                rectPaint.setColor(Color.RED);
                canvas.drawRect(mUnitBounds, rectPaint);
            }

            canvas.drawText(mUnit, mUnitBounds.left, mUnitBounds.bottom, mUnitTextPaint);
        }
    }

    //endregion draw

    //----------------------------------
    //region important getter / setter
    //----------------------------------
    /**
     * Turn off spin mode
     */
    public void stopSpinning() {
        mAnimationHandler.sendEmptyMessage(AnimationMsg.STOP_SPINNING.ordinal());
    }

    /**
     * Puts the view on spin mode
     */
    public void spin() {
        mAnimationHandler.sendEmptyMessage(AnimationMsg.START_SPINNING.ordinal());
    }

    /**
     * Set the value of the circle view without an animation.
     * Stops any currently active animations.
     */
    public void setValue(float _value) {
        Message msg = new Message();
        msg.what = AnimationMsg.SET_VALUE.ordinal();
        msg.obj = new float[]{_value, _value};
        mAnimationHandler.sendMessage(msg);
    }

    /**
     * Sets the value of the circle view with an animation.
     *
     * @param _valueFrom         start value of the animation
     * @param _valueTo           value after animation
     * @param _animationDuration the duration of the animation
     */
    public void setValueAnimated(float _valueFrom, float _valueTo, long _animationDuration) {
        mAnimationDuration = _animationDuration;
        Message msg = new Message();
        msg.what = AnimationMsg.SET_VALUE_ANIMATED.ordinal();
        msg.obj = new float[]{_valueFrom, _valueTo};
        mAnimationHandler.sendMessage(msg);
    }

    /**
     * Sets the value of the circle view with an animation.
     * The current value is used as the start value of the animation
     *
     * @param _valueTo           value after animation
     * @param _animationDuration the duration of the animation
     */
    public void setValueAnimated(float _valueTo, long _animationDuration) {

        mAnimationDuration = _animationDuration;
        Message msg = new Message();
        msg.what = AnimationMsg.SET_VALUE_ANIMATED.ordinal();
        msg.obj = new float[]{mCurrentValue, _valueTo};
        mAnimationHandler.sendMessage(msg);
    }

    /**
     * Sets the value of the circle view with an animation.
     * The current value is used as the start value of the animation
     *
     * @param _valueTo   value after animation
     */
    public void setValueAnimated(float _valueTo) {

        mAnimationDuration = 1200;
        Message msg = new Message();
        msg.what = AnimationMsg.SET_VALUE_ANIMATED.ordinal();
        msg.obj = new float[]{mCurrentValue, _valueTo};
        mAnimationHandler.sendMessage(msg);
    }
    //endregion important getter / setter


    //----------------------------------
    //region Animation stuff
    //----------------------------------

    private enum AnimationState {

        IDLE,
        SPINNING,
        END_SPINNING,
        END_SPINNING_START_ANIMATING,
        ANIMATING

    }

    private enum AnimationMsg {

        START_SPINNING,
        STOP_SPINNING,
        SET_VALUE,
        SET_VALUE_ANIMATED,
        TICK

    }


    private static class AnimationHandler extends Handler {
        private final WeakReference<CircleProgressView> mCircleViewWeakReference;
        // Spin bar length in degree at start of animation
        private float mSpinningBarLengthStart;
        private long mAnimationStartTime;
        private long mLengthChangeAnimationStartTime;
        private TimeInterpolator mLengthChangeInterpolator = new DecelerateInterpolator();
        private double mLengthChangeAnimationDuration;


        AnimationHandler(CircleProgressView _circleView) {
            super(_circleView.getContext().getMainLooper());
            mCircleViewWeakReference = new WeakReference<CircleProgressView>(_circleView);
        }

        @Override
        public void handleMessage(Message msg) {
            CircleProgressView circleView = mCircleViewWeakReference.get();
            if (circleView == null) {
                return;
            }
            AnimationMsg msgType = AnimationMsg.values()[msg.what];
            if (msgType == AnimationMsg.TICK)
                removeMessages(AnimationMsg.TICK.ordinal()); // necessary to remove concurrent ticks.

            //if (msgType != AnimationMsg.TICK)
            //    Log.d("JaGr", TAG + "LOG00099: State:" + circleView.mAnimationState + "     Received: " + msgType);

            switch (circleView.mAnimationState) {


                case IDLE:
                    switch (msgType) {

                        case START_SPINNING:
                            enterSpinning(circleView);

                            break;
                        case STOP_SPINNING:
                            //IGNORE not spinning
                            break;
                        case SET_VALUE:
                            setValue(msg, circleView);
                            break;
                        case SET_VALUE_ANIMATED:

                            enterSetValueAnimated(msg, circleView);
                            break;
                        case TICK:
                            removeMessages(AnimationMsg.TICK.ordinal()); // remove old ticks
                            //IGNORE nothing to do
                            break;
                    }
                    break;
                case SPINNING:
                    switch (msgType) {

                        case START_SPINNING:
                            //IGNORE already spinning
                            break;
                        case STOP_SPINNING:
                            enterEndSpinning(circleView);

                            break;
                        case SET_VALUE:
                            setValue(msg, circleView);
                            break;
                        case SET_VALUE_ANIMATED:
                            enterEndSpinningStartAnimating(circleView, msg);
                            break;
                        case TICK:
                            // set length

                            float length_delta = circleView.mSpinningBarLengthCurrent - circleView.mSpinningBarLengthOrig;
                            float t = (float) ((System.currentTimeMillis() - mLengthChangeAnimationStartTime)
                                                                / mLengthChangeAnimationDuration);
                            t = t > 1.0f ? 1.0f : t;
                            float interpolatedRatio = mLengthChangeInterpolator.getInterpolation(t);

                            if (Math.abs(length_delta) < 1) {
                                //spinner length is within bounds
                                circleView.mSpinningBarLengthCurrent = circleView.mSpinningBarLengthOrig;
                            } else if (circleView.mSpinningBarLengthCurrent < circleView.mSpinningBarLengthOrig) {
                                //spinner to short, --> grow
                                circleView.mSpinningBarLengthCurrent = mSpinningBarLengthStart + ((circleView.mSpinningBarLengthOrig - mSpinningBarLengthStart) * interpolatedRatio);
                            } else {
                                //spinner to long, --> shrink
                                circleView.mSpinningBarLengthCurrent = (mSpinningBarLengthStart - ((mSpinningBarLengthStart - circleView.mSpinningBarLengthOrig) * interpolatedRatio));
                            }

                            circleView.mCurrentSpinnerDegreeValue += circleView.mSpinSpeed; // spin speed value (in degree)

                            if (circleView.mCurrentSpinnerDegreeValue > 360) {
                                circleView.mCurrentSpinnerDegreeValue = 0;
                            }
                            circleView.mAnimationHandler.sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);
                            circleView.invalidate();
                            break;
                    }

                    break;
                case END_SPINNING:
                    switch (msgType) {

                        case START_SPINNING:
                            circleView.mAnimationState = AnimationState.SPINNING;
                            circleView.mAnimationHandler.sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);

                            break;
                        case STOP_SPINNING:
                            //IGNORE already stopping
                            break;
                        case SET_VALUE:
                            setValue(msg, circleView);
                            break;
                        case SET_VALUE_ANIMATED:
                            enterEndSpinningStartAnimating(circleView, msg);

                            break;
                        case TICK:

                            float t = (float) ((System.currentTimeMillis() - mLengthChangeAnimationStartTime)
                                    / mLengthChangeAnimationDuration);
                            t = t > 1.0f ? 1.0f : t;
                            float interpolatedRatio = mLengthChangeInterpolator.getInterpolation(t);
                            circleView.mSpinningBarLengthCurrent = (mSpinningBarLengthStart) * (1f - interpolatedRatio);

                            circleView.mCurrentSpinnerDegreeValue += circleView.mSpinSpeed; // spin speed value (not in percent)
                            if (circleView.mSpinningBarLengthCurrent < 0.01f) {
                                //end here, spinning finished
                                circleView.mAnimationState = AnimationState.IDLE;
                            }
                            circleView.mAnimationHandler.sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);
                            circleView.invalidate();
                            break;
                    }

                    break;
                case END_SPINNING_START_ANIMATING:
                    switch (msgType) {

                        case START_SPINNING:
                            circleView.mDrawArcWhileSpinning = false;
                            enterSpinning(circleView);

                            break;
                        case STOP_SPINNING:
                            //IGNORE already stopping
                            break;
                        case SET_VALUE:
                            circleView.mDrawArcWhileSpinning = false;
                            setValue(msg, circleView);

                            break;
                        case SET_VALUE_ANIMATED:
                            circleView.mValueFrom = 0; // start from zero after spinning
                            circleView.mValueTo = ((float[]) msg.obj)[1];
                            circleView.mAnimationHandler.sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);

                            break;
                        case TICK:
                            //shrink spinner till it has its original length
                            if (circleView.mSpinningBarLengthCurrent > circleView.mSpinningBarLengthOrig && !circleView.mDrawArcWhileSpinning) {
                                //spinner to long, --> shrink
                                float t = (float) ((System.currentTimeMillis() - mLengthChangeAnimationStartTime)
                                        / mLengthChangeAnimationDuration);
                                t = t > 1.0f ? 1.0f : t;
                                float interpolatedRatio = mLengthChangeInterpolator.getInterpolation(t);
                                circleView.mSpinningBarLengthCurrent = (mSpinningBarLengthStart) * (1f - interpolatedRatio);
                            }

                            // move spinner for spin speed value (not in percent)
                            circleView.mCurrentSpinnerDegreeValue += circleView.mSpinSpeed;

                            //if the start of the spinner reaches zero, start animating the value
                            if (circleView.mCurrentSpinnerDegreeValue > 360 && !circleView.mDrawArcWhileSpinning) {
                                mAnimationStartTime = System.currentTimeMillis();
                                circleView.mDrawArcWhileSpinning = true;
                                initReduceAnimation(circleView);
                            }

                            //value is already animating, calc animation value and reduce spinner
                            if (circleView.mDrawArcWhileSpinning) {
                                circleView.mCurrentSpinnerDegreeValue = 360;
                                circleView.mSpinningBarLengthCurrent -= circleView.mSpinSpeed;
                                calcNextAnimationValue(circleView);

                                float t = (float) ((System.currentTimeMillis() - mLengthChangeAnimationStartTime)
                                        / mLengthChangeAnimationDuration);
                                t = t > 1.0f ? 1.0f : t;
                                float interpolatedRatio = mLengthChangeInterpolator.getInterpolation(t);
                                circleView.mSpinningBarLengthCurrent = (mSpinningBarLengthStart) * (1f - interpolatedRatio);
                            }

                            //spinner is no longer visible switch state to animating
                            if (circleView.mSpinningBarLengthCurrent < 0.1) {
                                //spinning finished, start animating the current value
                                circleView.mAnimationState = AnimationState.ANIMATING;
                                circleView.invalidate();
                                circleView.mDrawArcWhileSpinning = false;
                                circleView.mSpinningBarLengthCurrent = circleView.mSpinningBarLengthOrig;

                            } else {
                                circleView.invalidate();
                            }
                            circleView.mAnimationHandler.sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);
                            break;
                    }

                    break;
                case ANIMATING:
                    switch (msgType) {

                        case START_SPINNING:
                            enterSpinning(circleView);
                            break;
                        case STOP_SPINNING:
                            //Ignore, not spinning
                            break;
                        case SET_VALUE:
                            setValue(msg, circleView);
                            break;
                        case SET_VALUE_ANIMATED:
                            mAnimationStartTime = System.currentTimeMillis();
                            //restart animation from current value
                            circleView.mValueFrom = circleView.mCurrentValue;
                            circleView.mValueTo = ((float[]) msg.obj)[1];

                            break;
                        case TICK:
                            if(calcNextAnimationValue(circleView)){
                                //animation finished
                                circleView.mAnimationState = AnimationState.IDLE;
                                circleView.mCurrentValue = circleView.mValueTo;
                            }
                            circleView.mAnimationHandler.sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);
                            circleView.invalidate();
                            break;
                    }

                    break;

            }
        }

        private void enterSetValueAnimated(Message msg, CircleProgressView _circleView) {
            _circleView.mValueFrom = ((float[]) msg.obj)[0];
            _circleView.mValueTo = ((float[]) msg.obj)[1];
            mAnimationStartTime = System.currentTimeMillis();
            _circleView.mAnimationState = AnimationState.ANIMATING;

            _circleView.mAnimationHandler.sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), _circleView.mDelayMillis);
        }

        private void enterEndSpinningStartAnimating(CircleProgressView circleView, Message msg) {
            circleView.mAnimationState = AnimationState.END_SPINNING_START_ANIMATING;

            circleView.mValueFrom = 0; // start from zero after spinning
            circleView.mValueTo = ((float[]) msg.obj)[1];

            mLengthChangeAnimationStartTime = System.currentTimeMillis();
            mSpinningBarLengthStart = circleView.mSpinningBarLengthCurrent;

            circleView.mAnimationHandler.sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);

        }

        private void enterEndSpinning(CircleProgressView circleView) {
            circleView.mAnimationState = AnimationState.END_SPINNING;

            initReduceAnimation(circleView);
            circleView.mAnimationHandler.sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);
        }

        private void initReduceAnimation(CircleProgressView circleView) {
            float degreesTillFinish = circleView.mSpinningBarLengthCurrent;
            float stepsTillFinish = degreesTillFinish / circleView.mSpinSpeed;
            mLengthChangeAnimationDuration = (stepsTillFinish * circleView.mDelayMillis) * 2f;

            mLengthChangeAnimationStartTime = System.currentTimeMillis();
            mSpinningBarLengthStart = circleView.mSpinningBarLengthCurrent;
        }

        private void enterSpinning(CircleProgressView circleView) {
            circleView.mAnimationState = AnimationState.SPINNING;
            circleView.mSpinningBarLengthCurrent = (360f / circleView.mMaxValue * circleView.mCurrentValue);
            circleView.mCurrentSpinnerDegreeValue = (360f / circleView.mMaxValue * circleView.mCurrentValue);
            mLengthChangeAnimationStartTime = System.currentTimeMillis();
            mSpinningBarLengthStart = circleView.mSpinningBarLengthCurrent;


            //calc animation time
            float stepsTillFinish = circleView.mSpinningBarLengthOrig / circleView.mSpinSpeed;
            mLengthChangeAnimationDuration = ((stepsTillFinish * circleView.mDelayMillis) * 2f);


            circleView.mAnimationHandler.sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mDelayMillis);
        }

        /**
         * *
         * @param _circleView the circle view
         * @return false if animation still running, true if animation is finished.
         */
        private boolean calcNextAnimationValue(CircleProgressView _circleView) {
            float t = (float) ((System.currentTimeMillis() - mAnimationStartTime)
                    / _circleView.mAnimationDuration);
            t = t > 1.0f ? 1.0f : t;
            float interpolatedRatio = _circleView.mInterpolator.getInterpolation(t);

            _circleView.mCurrentValue = (_circleView.mValueFrom + ((_circleView.mValueTo - _circleView.mValueFrom) * interpolatedRatio));

            return t >= 1;
        }

        private void setValue(Message msg, CircleProgressView _circleView) {
            _circleView.mValueFrom = _circleView.mValueTo;
            _circleView.mCurrentValue = _circleView.mValueTo = ((float[]) msg.obj)[0];
            _circleView.mAnimationState = AnimationState.IDLE;
            _circleView.invalidate();
        }
    }

    //endregion Animation stuff
    //----------------------------------
}
