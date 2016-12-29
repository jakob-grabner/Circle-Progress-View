package at.grabner.circleprogress;

import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;

/**
 * An circle view, similar to Android's ProgressBar.
 * Can be used in 'value mode' or 'spinning mode'.
 * <p/>
 * In spinning mode it can be used like a intermediate progress bar.
 * <p/>
 * In value mode it can be used as a progress bar or to visualize any other value.
 * Setting a value is fully animated. There are also nice transitions from animating to value mode.
 * <p/>
 * Typical use case would be to load a new value. During the loading time set the CircleView to spinning.
 * As soon as you get your value, just set it with {@link #setValueAnimated(float, long)}.
 *
 * @author Jakob Grabner, based on the Progress wheel of Todd Davies
 *         https://github.com/Todd-Davies/CircleView
 *         <p/>
 *         Licensed under the Creative Commons Attribution 3.0 license see:
 *         http://creativecommons.org/licenses/by/3.0/
 */
@SuppressWarnings("unused")
public class CircleProgressView extends View {

    /**
     * The log tag.
     */
    private final static String TAG = "CircleView";
    private static final boolean DEBUG = false;
    //----------------------------------
    //region members
    //Colors (with defaults)
    private final int mBarColorStandard = 0xff009688; //stylish blue
    protected int mLayoutHeight = 0;
    protected int mLayoutWidth = 0;
    //Rectangles
    protected RectF mCircleBounds = new RectF();
    protected RectF mInnerCircleBound = new RectF();
    protected PointF mCenter;
    /**
     * Maximum size of the text.
     */
    protected RectF mOuterTextBounds = new RectF();
    /**
     * Actual size of the text.
     */
    protected RectF mActualTextBounds = new RectF();
    protected RectF mUnitBounds = new RectF();
    protected RectF mCircleOuterContour = new RectF();
    protected RectF mCircleInnerContour = new RectF();
    //value animation
    Direction mDirection = Direction.CW;
    float mCurrentValue = 0;
    float mValueTo = 0;
    float mValueFrom = 0;
    float mMaxValue = 100;
    float mMinValueAllowed = 0;
    float mMaxValueAllowed = -1;
    // spinner animation
    float mSpinningBarLengthCurrent = 0;
    float mSpinningBarLengthOrig = 42;
    float mCurrentSpinnerDegreeValue = 0;
    //Animation
    //The amount of degree to move the bar by on each draw
    float mSpinSpeed = 2.8f;
    //Enable spin
    boolean mSpin = false;
    /**
     * The animation duration in ms
     */
    double mAnimationDuration = 900;
    //The number of milliseconds to wait in between each draw
    int mFrameDelayMillis = 10;
    // helper for AnimationState.END_SPINNING_START_ANIMATING
    boolean mDrawBarWhileSpinning;
    //The animation handler containing the animation state machine.
    AnimationHandler mAnimationHandler = new AnimationHandler(this);
    //The current state of the animation state machine.
    AnimationState mAnimationState = AnimationState.IDLE;
    AnimationStateChangedListener mAnimationStateChangedListener;
    private int mBarWidth = 40;
    private int mRimWidth = 40;
    private int mStartAngle = 270;
    private float mOuterContourSize = 1;
    private float mInnerContourSize = 1;

    // Bar start/end width and type
    private int mBarStartEndLineWidth = 0;
    private BarStartEndLine mBarStartEndLine = BarStartEndLine.NONE;
    private int mBarStartEndLineColor = 0xAA000000;
    private float mBarStartEndLineSweep = 10f;
    //Default text sizes
    private int mUnitTextSize = 10;
    private int mTextSize = 10;
    //Text scale
    private float mTextScale = 1;
    private float mUnitScale = 1;
    private int mOuterContourColor = 0xAA000000;
    private int mInnerContourColor = 0xAA000000;
    private int mSpinnerColor = mBarColorStandard; //stylish blue
    private int mBackgroundCircleColor = 0x00000000;  //transparent
    private int mRimColor = 0xAA83d0c9;
    private int mTextColor = 0xFF000000;
    private int mUnitColor = 0xFF000000;
    private boolean mIsAutoColorEnabled = false;
    private int[] mBarColors = new int[]{
            mBarColorStandard //stylish blue
    };
    //Caps
    private Paint.Cap mBarStrokeCap = Paint.Cap.BUTT;
    private Paint.Cap mSpinnerStrokeCap = Paint.Cap.BUTT;
    //Paints
    private Paint mBarPaint = new Paint();
    private Paint mShaderlessBarPaint;
    private Paint mBarSpinnerPaint = new Paint();
    private Paint mBarStartEndLinePaint = new Paint();
    private Paint mBackgroundCirclePaint = new Paint();
    private Paint mRimPaint = new Paint();
    private Paint mTextPaint = new Paint();
    private Paint mUnitTextPaint = new Paint();
    private Paint mOuterContourPaint = new Paint();
    private Paint mInnerContourPaint = new Paint();
    //Other
    // The text to show
    private String mText = "";
    private int mTextLength;
    private String mUnit = "";
    private UnitPosition mUnitPosition = UnitPosition.RIGHT_TOP;
    /**
     * Indicates if the given text, the current percentage, or the current value should be shown.
     */
    private TextMode mTextMode = TextMode.PERCENT;
    private boolean mIsAutoTextSize;
    private boolean mShowUnit = false;
    //clipping
    private Bitmap mClippingBitmap;
    private Paint mMaskPaint;
    /**
     * Relative size of the unite string to the value string.
     */
    private float mRelativeUniteSize = 1f;
    private boolean mSeekModeEnabled = false;
    private boolean mShowTextWhileSpinning = false;
    private boolean mShowBlock = false;
    private int mBlockCount = 18;
    private float mBlockScale = 0.9f;
    private float mBlockDegree = 360 / mBlockCount;
    private float mBlockScaleDegree = mBlockDegree * mBlockScale;
    private boolean mRoundToBlock = false;
    private boolean mRoundToWholeNumber = false;

    private int mTouchEventCount;
    private OnProgressChangedListener onProgressChangedListener;
    private float previousProgressChangedValue;


    private DecimalFormat decimalFormat = new DecimalFormat("0");

    // Text typeface
    private Typeface textTypeface;
    private Typeface unitTextTypeface;
    //endregion members
    //----------------------------------

    /**
     * The constructor for the CircleView
     *
     * @param context The context.
     * @param attrs   The attributes.
     */
    public CircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);

        parseAttributes(context.obtainStyledAttributes(attrs,
                R.styleable.CircleProgressView));

        if (!isInEditMode()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }

        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setFilterBitmap(false);
        mMaskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        setupPaints();

        if (mSpin) {
            spin();
        }
    }

    private static float calcTextSizeForRect(String _text, Paint _textPaint, RectF _rectBounds) {

        Matrix matrix = new Matrix();
        Rect textBoundsTmp = new Rect();
        //replace ones because for some fonts the 1 takes less space which causes issues
        String text = _text.replace('1', '0');

        //get current mText bounds
        _textPaint.getTextBounds(text, 0, text.length(), textBoundsTmp);

        RectF textBoundsTmpF = new RectF(textBoundsTmp);

        matrix.setRectToRect(textBoundsTmpF, _rectBounds, Matrix.ScaleToFit.CENTER);
        float values[] = new float[9];
        matrix.getValues(values);
        return _textPaint.getTextSize() * values[Matrix.MSCALE_X];
    }

    /**
     * @param _angle The angle in degree to normalize
     * @return the angle between 0 (EAST) and 360
     */
    private static float normalizeAngle(float _angle) {
        return (((_angle % 360) + 360) % 360);
    }

    /**
     * Calculates the angle from centerPt to targetPt in degrees.
     * The return should range from [0,360), rotating CLOCKWISE,
     * 0 and 360 degrees represents EAST,
     * 90 degrees represents SOUTH, etc...
     * <p/>
     * Assumes all points are in the same coordinate space.  If they are not,
     * you will need to call SwingUtilities.convertPointToScreen or equivalent
     * on all arguments before passing them  to this function.
     *
     * @param centerPt Point we are rotating around.
     * @param targetPt Point we want to calculate the angle to.
     * @return angle in degrees.  This is the angle from centerPt to targetPt.
     */
    public static double calcRotationAngleInDegrees(PointF centerPt, PointF targetPt) {
        // calculate the angle theta from the deltaY and deltaX values
        // (atan2 returns radians values from [-PI,PI])
        // 0 currently points EAST.
        // NOTE: By preserving Y and X param order to atan2,  we are expecting
        // a CLOCKWISE angle direction.
        double theta = Math.atan2(targetPt.y - centerPt.y, targetPt.x - centerPt.x);

        // rotate the theta angle clockwise by 90 degrees
        // (this makes 0 point NORTH)
        // NOTE: adding to an angle rotates it clockwise.
        // subtracting would rotate it counter-clockwise
//        theta += Math.PI/2.0;

        // convert from radians to degrees
        // this will give you an angle from [0->270],[-180,0]
        double angle = Math.toDegrees(theta);

        // convert to positive range [0-360)
        // since we want to prevent negative angles, adjust them now.
        // we can assume that atan2 will not return a negative value
        // greater than one partial rotation
        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }

    //----------------------------------
    //region getter/setter
    public BarStartEndLine getBarStartEndLine() {
        return mBarStartEndLine;
    }

    /**
     * Allows to add a line to the start/end of the bar
     *
     * @param _barWidth        The width of the stroke on the start/end of the bar in pixel.
     * @param _barStartEndLine The type of line on the start/end of the bar.
     * @param _lineColor       The line color
     * @param _sweepWidth      The sweep amount in degrees for the start and end bars to cover.
     */
    public void setBarStartEndLine(int _barWidth, BarStartEndLine _barStartEndLine, @ColorInt int _lineColor, float _sweepWidth) {
        mBarStartEndLineWidth = _barWidth;
        mBarStartEndLine = _barStartEndLine;
        mBarStartEndLineColor = _lineColor;
        mBarStartEndLineSweep = _sweepWidth;
    }

    public int[] getBarColors() {
        return mBarColors;
    }

    public Paint.Cap getBarStrokeCap() {
        return mBarStrokeCap;
    }

    /**
     * @param _barStrokeCap The stroke cap of the progress bar.
     */
    public void setBarStrokeCap(Paint.Cap _barStrokeCap) {
        mBarStrokeCap = _barStrokeCap;
        mBarPaint.setStrokeCap(_barStrokeCap);
        if (mBarStrokeCap != Paint.Cap.BUTT) {
            mShaderlessBarPaint = new Paint(mBarPaint);
            mShaderlessBarPaint.setShader(null);
            mShaderlessBarPaint.setColor(mBarColors[0]);
        }
    }

    public int getBarWidth() {
        return mBarWidth;
    }

    /**
     * @param barWidth The width of the progress bar in pixel.
     */
    public void setBarWidth(@IntRange(from = 0) int barWidth) {
        this.mBarWidth = barWidth;
        mBarPaint.setStrokeWidth(barWidth);
        mBarSpinnerPaint.setStrokeWidth(barWidth);
    }

    public int getBlockCount() {
        return mBlockCount;
    }

    public void setBlockCount(int blockCount) {
        if (blockCount > 1) {
            mShowBlock = true;
            mBlockCount = blockCount;
            mBlockDegree = 360.0f / blockCount;
            mBlockScaleDegree = mBlockDegree * mBlockScale;
        } else {
            mShowBlock = false;
        }
    }

    public void setRoundToBlock(boolean _roundToBlock) {
        mRoundToBlock = _roundToBlock;
    }

    public boolean getRoundToBlock() {
        return mRoundToBlock;
    }

    public void setRoundToWholeNumber(boolean roundToWholeNumber) {
        mRoundToWholeNumber = roundToWholeNumber;
    }

    public boolean getRoundToWholeNumber() {
        return mRoundToWholeNumber;
    }

    public float getBlockScale() {
        return mBlockScale;
    }

    public void setBlockScale(@FloatRange(from = 0.0, to = 1) float blockScale) {
        if (blockScale >= 0.0f && blockScale <= 1.0f) {
            mBlockScale = blockScale;
            mBlockScaleDegree = mBlockDegree * blockScale;
        }
    }

    public int getOuterContourColor() {
        return mOuterContourColor;
    }

    /**
     * @param _contourColor The color of the background contour of the circle.
     */
    public void setOuterContourColor(@ColorInt int _contourColor) {
        mOuterContourColor = _contourColor;
        mOuterContourPaint.setColor(_contourColor);
    }

    public float getOuterContourSize() {
        return mOuterContourSize;
    }

    /**
     * @param _contourSize The size of the background contour of the circle.
     */
    public void setOuterContourSize(@FloatRange(from = 0.0) float _contourSize) {
        mOuterContourSize = _contourSize;
        mOuterContourPaint.setStrokeWidth(_contourSize);
    }

    public int getInnerContourColor() {
        return mInnerContourColor;
    }

    /**
     * @param _contourColor The color of the background contour of the circle.
     */
    public void setInnerContourColor(@ColorInt int _contourColor) {
        mInnerContourColor = _contourColor;
        mInnerContourPaint.setColor(_contourColor);
    }

    public float getInnerContourSize() {
        return mInnerContourSize;
    }

    /**
     * @param _contourSize The size of the background contour of the circle.
     */
    public void setInnerContourSize(@FloatRange(from = 0.0) float _contourSize) {
        mInnerContourSize = _contourSize;
        mInnerContourPaint.setStrokeWidth(_contourSize);
    }

    /**
     * @return The number of ms to wait between each draw call.
     */
    public int getDelayMillis() {
        return mFrameDelayMillis;
    }

    /**
     * @param delayMillis The number of ms to wait between each draw call.
     */
    public void setDelayMillis(int delayMillis) {
        this.mFrameDelayMillis = delayMillis;
    }

    public int getFillColor() {
        return mBackgroundCirclePaint.getColor();
    }

    public float getCurrentValue() {
        return mCurrentValue;
    }

    public float getMinValueAllowed() {
        return mMinValueAllowed;
    }

    public float getMaxValueAllowed() {
        return mMaxValueAllowed;
    }

    public float getMaxValue() {
        return mMaxValue;
    }

    /**
     * The max value of the progress bar. Used to calculate the percentage of the current value.
     * The bar fills according to the percentage. The default value is 100.
     *
     * @param _maxValue The max value.
     */
    public void setMaxValue(@FloatRange(from = 0) float _maxValue) {
        mMaxValue = _maxValue;
    }

    /**
     * The min value allowed of the progress bar. Used to limit the min possible value of the current value.
     *
     * @param _minValueAllowed The min value allowed.
     */
    public void setMinValueAllowed(@FloatRange(from = 0) float _minValueAllowed) {
        mMinValueAllowed = _minValueAllowed;
    }

    /**
     * The max value allowed of the progress bar. Used to limit the max possible value of the current value.
     *
     * @param _maxValueAllowed The max value allowed.
     */
    public void setMaxValueAllowed(@FloatRange(from = 0) float _maxValueAllowed) {
        mMaxValueAllowed = _maxValueAllowed;
    }

    /**
     * @return The relative size (scale factor) of the unit text size to the text size
     */
    public float getRelativeUniteSize() {
        return mRelativeUniteSize;
    }

    public int getRimColor() {
        return mRimColor;
    }

    /**
     * @param rimColor The color of the rim around the Circle.
     */
    public void setRimColor(@ColorInt int rimColor) {
        mRimColor = rimColor;
        mRimPaint.setColor(rimColor);
    }

    public Shader getRimShader() {
        return mRimPaint.getShader();
    }

    public void setRimShader(Shader shader) {
        this.mRimPaint.setShader(shader);
    }

    public int getRimWidth() {
        return mRimWidth;
    }

    /**
     * @param rimWidth The width in pixel of the rim around the circle
     */
    public void setRimWidth(@IntRange(from = 0) int rimWidth) {
        mRimWidth = rimWidth;
        mRimPaint.setStrokeWidth(rimWidth);
    }

    public float getSpinSpeed() {
        return mSpinSpeed;
    }

    /**
     * The amount of degree to move the bar on every draw call.
     *
     * @param spinSpeed the speed of the spinner
     */
    public void setSpinSpeed(float spinSpeed) {
        mSpinSpeed = spinSpeed;
    }

    public Paint.Cap getSpinnerStrokeCap() {
        return mSpinnerStrokeCap;
    }

    /**
     * @param _spinnerStrokeCap The stroke cap of the progress bar in spinning mode.
     */
    public void setSpinnerStrokeCap(Paint.Cap _spinnerStrokeCap) {
        mSpinnerStrokeCap = _spinnerStrokeCap;
        mBarSpinnerPaint.setStrokeCap(_spinnerStrokeCap);
    }

    public int getStartAngle() {
        return mStartAngle;
    }

    public void setStartAngle(@IntRange(from = 0,to = 360) int _startAngle) {
        // get a angle between 0 and 360
        mStartAngle = (int) normalizeAngle(_startAngle);
    }

    public int calcTextColor() {
        return mTextColor;
    }

    /**
     * Sets the text color.
     * You also need to  set {@link #setTextColorAuto(boolean)} to false to see your color.
     *
     * @param textColor the color
     */
    public void setTextColor(@ColorInt int textColor) {
        mTextColor = textColor;
        mTextPaint.setColor(textColor);
    }

    /**
     * @return The scale value
     */
    public float getTextScale() {
        return mTextScale;
    }

    /**
     * Scale factor for main text in the center of the circle view.
     * Only used if auto text size is enabled.
     *
     * @param _textScale The scale value.
     */
    public void setTextScale(@FloatRange(from = 0.0) float _textScale) {
        mTextScale = _textScale;
    }

    public int getTextSize() {
        return mTextSize;
    }

    /**
     * Text size of the text string. Disables auto text size
     * If auto text size is on, use {@link #setTextScale(float)} to scale textSize.
     *
     * @param textSize The text size of the unit.
     */
    public void setTextSize(@IntRange(from = 0) int textSize) {
        this.mTextPaint.setTextSize(textSize);
        mTextSize = textSize;
        mIsAutoTextSize = false;
    }

    public String getUnit() {
        return mUnit;
    }

    /**
     * @param _unit The unit to show next to the current value.
     *              You also need to set {@link #setUnitVisible(boolean)} to true.
     */
    public void setUnit(String _unit) {
        if (_unit == null) {
            mUnit = "";
        } else {
            mUnit = _unit;
        }
        invalidate();
    }

    /**
     * @return The scale value
     */
    public float getUnitScale() {
        return mUnitScale;
    }

    /**
     * Scale factor for unit text next to the main text.
     * Only used if auto text size is enabled.
     *
     * @param _unitScale The scale value.
     */
    public void setUnitScale(@FloatRange(from = 0.0) float _unitScale) {
        mUnitScale = _unitScale;
    }

    public int getUnitSize() {
        return mUnitTextSize;
    }

    /**
     * Text size of the unit string. Only used if text size is also set. (So automatic text size
     * calculation is off. see {@link #setTextSize(int)}).
     * If auto text size is on, use {@link #setUnitScale(float)} to scale unit size.
     *
     * @param unitSize The text size of the unit.
     */
    public void setUnitSize(@IntRange(from = 0) int unitSize) {
        mUnitTextSize = unitSize;
        mUnitTextPaint.setTextSize(unitSize);
    }

    /**
     * @return true if auto text size is enabled, false otherwise.
     */
    public boolean isAutoTextSize() {
        return mIsAutoTextSize;
    }

    /**
     * @param _autoTextSize true to enable auto text size calculation.
     */
    public void setAutoTextSize(boolean _autoTextSize) {
        mIsAutoTextSize = _autoTextSize;
    }

    public boolean isSeekModeEnabled() {
        return mSeekModeEnabled;
    }

    public void setSeekModeEnabled(boolean _seekModeEnabled) {
        mSeekModeEnabled = _seekModeEnabled;
    }

    public boolean isShowBlock() {
        return mShowBlock;
    }

    public void setShowBlock(boolean showBlock) {
        mShowBlock = showBlock;
    }

    public boolean isShowTextWhileSpinning() {
        return mShowTextWhileSpinning;
    }

    /**
     * @param shouldDrawTextWhileSpinning True to show text in spinning mode, false to hide it.
     */
    public void setShowTextWhileSpinning(boolean shouldDrawTextWhileSpinning) {
        mShowTextWhileSpinning = shouldDrawTextWhileSpinning;
    }

    public boolean isUnitVisible() {
        return mShowUnit;
    }

    /**
     * @param _showUnit True to show unit, false to hide it.
     */
    public void setUnitVisible(boolean _showUnit) {
        if (_showUnit != mShowUnit) {
            mShowUnit = _showUnit;
            triggerReCalcTextSizesAndPositions(); // triggers recalculating text sizes
        }
    }

    /**
     * Sets the color of progress bar.
     *
     * @param barColors One or more colors. If more than one color is specified, a gradient of the colors is used.
     */
    public void setBarColor(@ColorInt int... barColors) {
        this.mBarColors = barColors;
        setupBarPaint();
    }

    /**
     * @param _clippingBitmap The bitmap used for clipping. Set to null to disable clipping.
     *                        Default: No clipping.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setClippingBitmap(Bitmap _clippingBitmap) {

        if (getWidth() > 0 && getHeight() > 0) {
            mClippingBitmap = Bitmap.createScaledBitmap(_clippingBitmap, getWidth(), getHeight(), false);
        } else {
            mClippingBitmap = _clippingBitmap;
        }
        if (mClippingBitmap == null) {
            // enable HW acceleration
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            // disable HW acceleration
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    /**
     * Sets the background color of the entire Progress Circle.
     * Set the color to 0x00000000 (Color.TRANSPARENT) to hide it.
     *
     * @param circleColor the color.
     */
    public void setFillCircleColor(@ColorInt int circleColor) {
        mBackgroundCircleColor = circleColor;
        mBackgroundCirclePaint.setColor(circleColor);
    }

    public void setOnAnimationStateChangedListener(AnimationStateChangedListener _animationStateChangedListener) {
        mAnimationStateChangedListener = _animationStateChangedListener;
    }

    public void setOnProgressChangedListener(OnProgressChangedListener listener) {
        onProgressChangedListener = listener;
    }

    /**
     * @param _color The color of progress the bar in spinning mode.
     */
    public void setSpinBarColor(@ColorInt int _color) {
        mSpinnerColor = _color;
        mBarSpinnerPaint.setColor(mSpinnerColor);
    }

    /**
     * Length of spinning bar in degree.
     *
     * @param barLength length in degree
     */
    public void setSpinningBarLength(@FloatRange(from = 0.0) float barLength) {
        this.mSpinningBarLengthCurrent = mSpinningBarLengthOrig = barLength;
    }

    /**
     * Set the text in the middle of the circle view.
     * You need also set the {@link TextMode} to TextMode.TEXT to see the text.
     *
     * @param text The text to show
     */
    public void setText(String text) {
        mText = text != null ? text : "";
        invalidate();
    }

    /**
     * If auto text color is enabled, the text color  and the unit color is always the same as the rim color.
     * This is useful if the rim has multiple colors (color gradient), than the text will always have
     * the color of the tip of the rim.
     *
     * @param isEnabled true to enable, false to disable
     */
    public void setTextColorAuto(boolean isEnabled) {
        mIsAutoColorEnabled = isEnabled;
    }

    /**
     * Sets the auto text mode.
     *
     * @param _textValue The mode
     */
    public void setTextMode(TextMode _textValue) {
        mTextMode = _textValue;
    }

    /**
     * @param typeface The typeface to use for the text
     */
    public void setTextTypeface(Typeface typeface) {
        mTextPaint.setTypeface(typeface);
    }

    /**
     * Sets the unit text color.
     * Also sets {@link #setTextColorAuto(boolean)} to false
     *
     * @param unitColor The color.
     */
    public void setUnitColor(@ColorInt int unitColor) {
        mUnitColor = unitColor;
        mUnitTextPaint.setColor(unitColor);
        mIsAutoColorEnabled = false;
    }

    public void setUnitPosition(UnitPosition _unitPosition) {
        mUnitPosition = _unitPosition;
        triggerReCalcTextSizesAndPositions(); // triggers recalculating text sizes
    }

    /**
     * @param typeface The typeface to use for the unit text
     */
    public void setUnitTextTypeface(Typeface typeface) {
        mUnitTextPaint.setTypeface(typeface);
    }

    /**
     * @param _relativeUniteSize The relative scale factor of the unit text size to the text size.
     *                           Only useful for autotextsize=true; Effects both, the unit text size and the text size.
     */
    public void setUnitToTextScale(@FloatRange(from = 0.0) float _relativeUniteSize) {
        mRelativeUniteSize = _relativeUniteSize;
        triggerReCalcTextSizesAndPositions();
    }

    /**
     * Sets the direction of circular motion (clockwise or counter-clockwise).
     */
    public void setDirection(Direction direction) {
        mDirection = direction;
    }

    /**
     * Set the value of the circle view without an animation.
     * Stops any currently active animations.
     *
     * @param _value The value.
     */
    public void setValue(float _value) {
        // round to block
        if (mShowBlock && mRoundToBlock) {
            float value_per_block = mMaxValue / (float) mBlockCount;
            _value = Math.round(_value / value_per_block) * value_per_block;

        } else if (mRoundToWholeNumber) { // round to whole number
            _value = Math.round(_value);
        }

        // respect min and max values allowed
        _value = Math.max(mMinValueAllowed, _value);

        if (mMaxValueAllowed >= 0)
            _value = Math.min(mMaxValueAllowed, _value);

        Message msg = new Message();
        msg.what = AnimationMsg.SET_VALUE.ordinal();
        msg.obj = new float[]{_value, _value};
        mAnimationHandler.sendMessage(msg);
        triggerOnProgressChanged(_value);
    }

    /**
     * Sets the value of the circle view with an animation.
     * The current value is used as the start value of the animation
     *
     * @param _valueTo value after animation
     */
    public void setValueAnimated(float _valueTo) {
        setValueAnimated(_valueTo, 1200);
    }

    /**
     * Sets the value of the circle view with an animation.
     * The current value is used as the start value of the animation
     *
     * @param _valueTo           value after animation
     * @param _animationDuration the duration of the animation in milliseconds.
     */
    public void setValueAnimated(float _valueTo, long _animationDuration) {
        setValueAnimated(mCurrentValue, _valueTo, _animationDuration);
    }

    /**
     * Sets the value of the circle view with an animation.
     *
     * @param _valueFrom         start value of the animation
     * @param _valueTo           value after animation
     * @param _animationDuration the duration of the animation in milliseconds
     */
    public void setValueAnimated(float _valueFrom, float _valueTo, long _animationDuration) {
        // round to block
        if (mShowBlock && mRoundToBlock) {
            float value_per_block = mMaxValue / (float) mBlockCount;
            _valueTo = Math.round(_valueTo / value_per_block) * value_per_block;

        } else if (mRoundToWholeNumber) {
            _valueTo = Math.round(_valueTo);
        }

        // respect min and max values allowed
        _valueTo = Math.max(mMinValueAllowed, _valueTo);

        if (mMaxValueAllowed >= 0)
            _valueTo = Math.min(mMaxValueAllowed, _valueTo);

        mAnimationDuration = _animationDuration;
        Message msg = new Message();
        msg.what = AnimationMsg.SET_VALUE_ANIMATED.ordinal();
        msg.obj = new float[]{_valueFrom, _valueTo};
        mAnimationHandler.sendMessage(msg);
        triggerOnProgressChanged(_valueTo);
    }


    public DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    public void setDecimalFormat(DecimalFormat decimalFormat) {
        if (decimalFormat == null) {
            throw new IllegalArgumentException("decimalFormat must not be null!");
        }
        this.decimalFormat = decimalFormat;
    }

    /**
     * Sets interpolator for value animations.
     *
     * @param interpolator the interpolator
     */
    public void setValueInterpolator(TimeInterpolator interpolator) {
        mAnimationHandler.setValueInterpolator(interpolator);
    }

    /**
     * Sets the interpolator for length changes of the bar.
     *
     * @param interpolator the interpolator
     */
    public void setLengthChangeInterpolator(TimeInterpolator interpolator) {
        mAnimationHandler.setLengthChangeInterpolator(interpolator);
    }

    //endregion getter/setter
    //----------------------------------


    /**
     * Parse the attributes passed to the view from the XML
     *
     * @param a the attributes to parse
     */
    private void parseAttributes(TypedArray a) {
        setBarWidth((int) a.getDimension(R.styleable.CircleProgressView_cpv_barWidth,
                mBarWidth));

        setRimWidth((int) a.getDimension(R.styleable.CircleProgressView_cpv_rimWidth,
                mRimWidth));

        setSpinSpeed((int) a.getFloat(R.styleable.CircleProgressView_cpv_spinSpeed,
                mSpinSpeed));

        setSpin(a.getBoolean(R.styleable.CircleProgressView_cpv_spin,
                mSpin));

        setDirection(Direction.values()[a.getInt(R.styleable.CircleProgressView_cpv_direction, 0)]);

        float value = a.getFloat(R.styleable.CircleProgressView_cpv_value, mCurrentValue);
        setValue(value);
        mCurrentValue = value;

        if (a.hasValue(R.styleable.CircleProgressView_cpv_barColor) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor1) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor2) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor3)) {
            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor1, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor2, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor3, mBarColorStandard)};

        } else if (a.hasValue(R.styleable.CircleProgressView_cpv_barColor) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor1) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor2)) {

            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor1, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor2, mBarColorStandard)};

        } else if (a.hasValue(R.styleable.CircleProgressView_cpv_barColor) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor1)) {

            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor1, mBarColorStandard)};

        } else {
            mBarColors = new int[]{a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard), a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard)};
        }

        if (a.hasValue(R.styleable.CircleProgressView_cpv_barStrokeCap)) {
            setBarStrokeCap(StrokeCap.values()[a.getInt(R.styleable.CircleProgressView_cpv_barStrokeCap, 0)].paintCap);
        }

        if (a.hasValue(R.styleable.CircleProgressView_cpv_barStartEndLineWidth) && a.hasValue(R.styleable.CircleProgressView_cpv_barStartEndLine)) {
            setBarStartEndLine((int) a.getDimension(R.styleable.CircleProgressView_cpv_barStartEndLineWidth, 0),
                    BarStartEndLine.values()[a.getInt(R.styleable.CircleProgressView_cpv_barStartEndLine, 3)],
                    a.getColor(R.styleable.CircleProgressView_cpv_barStartEndLineColor, mBarStartEndLineColor),
                    a.getFloat(R.styleable.CircleProgressView_cpv_barStartEndLineSweep, mBarStartEndLineSweep));
        }

        setSpinBarColor(a.getColor(R.styleable.CircleProgressView_cpv_spinColor, mSpinnerColor));
        setSpinningBarLength(a.getFloat(R.styleable.CircleProgressView_cpv_spinBarLength,
                mSpinningBarLengthOrig));

        if (a.hasValue(R.styleable.CircleProgressView_cpv_textSize)) {
            setTextSize((int) a.getDimension(R.styleable.CircleProgressView_cpv_textSize, mTextSize));
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_unitSize)) {
            setUnitSize((int) a.getDimension(R.styleable.CircleProgressView_cpv_unitSize, mUnitTextSize));
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_textColor)) {
            setTextColor(a.getColor(R.styleable.CircleProgressView_cpv_textColor, mTextColor));
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_unitColor)) {
            setUnitColor(a.getColor(R.styleable.CircleProgressView_cpv_unitColor, mUnitColor));
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_autoTextColor)) {
            setTextColorAuto(a.getBoolean(R.styleable.CircleProgressView_cpv_autoTextColor, mIsAutoColorEnabled));
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_autoTextSize)) {
            setAutoTextSize(a.getBoolean(R.styleable.CircleProgressView_cpv_autoTextSize, mIsAutoTextSize));
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_textMode)) {
            setTextMode(TextMode.values()[a.getInt(R.styleable.CircleProgressView_cpv_textMode, 0)]);
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_unitPosition)) {
            setUnitPosition(UnitPosition.values()[a.getInt(R.styleable.CircleProgressView_cpv_unitPosition, 3)]);
        }
        //if the mText is empty, show current percentage value
        if (a.hasValue(R.styleable.CircleProgressView_cpv_text)) {
            setText(a.getString(R.styleable.CircleProgressView_cpv_text));
        }

        setUnitToTextScale(a.getFloat(R.styleable.CircleProgressView_cpv_unitToTextScale, 1f));

        setRimColor(a.getColor(R.styleable.CircleProgressView_cpv_rimColor,
                mRimColor));

        setFillCircleColor(a.getColor(R.styleable.CircleProgressView_cpv_fillColor,
                mBackgroundCircleColor));

        setOuterContourColor(a.getColor(R.styleable.CircleProgressView_cpv_outerContourColor, mOuterContourColor));
        setOuterContourSize(a.getDimension(R.styleable.CircleProgressView_cpv_outerContourSize, mOuterContourSize));

        setInnerContourColor(a.getColor(R.styleable.CircleProgressView_cpv_innerContourColor, mInnerContourColor));
        setInnerContourSize(a.getDimension(R.styleable.CircleProgressView_cpv_innerContourSize, mInnerContourSize));

        setMaxValue(a.getFloat(R.styleable.CircleProgressView_cpv_maxValue, mMaxValue));

        setMinValueAllowed(a.getFloat(R.styleable.CircleProgressView_cpv_minValueAllowed, mMinValueAllowed));
        setMaxValueAllowed(a.getFloat(R.styleable.CircleProgressView_cpv_maxValueAllowed, mMaxValueAllowed));

        setRoundToBlock(a.getBoolean(R.styleable.CircleProgressView_cpv_roundToBlock, mRoundToBlock));
        setRoundToWholeNumber(a.getBoolean(R.styleable.CircleProgressView_cpv_roundToWholeNumber, mRoundToWholeNumber));

        setUnit(a.getString(R.styleable.CircleProgressView_cpv_unit));
        setUnitVisible(a.getBoolean(R.styleable.CircleProgressView_cpv_showUnit, mShowUnit));

        setTextScale(a.getFloat(R.styleable.CircleProgressView_cpv_textScale, mTextScale));
        setUnitScale(a.getFloat(R.styleable.CircleProgressView_cpv_unitScale, mUnitScale));

        setSeekModeEnabled(a.getBoolean(R.styleable.CircleProgressView_cpv_seekMode, mSeekModeEnabled));

        setStartAngle(a.getInt(R.styleable.CircleProgressView_cpv_startAngle, mStartAngle));

        setShowTextWhileSpinning(a.getBoolean(R.styleable.CircleProgressView_cpv_showTextInSpinningMode, mShowTextWhileSpinning));

        if (a.hasValue(R.styleable.CircleProgressView_cpv_blockCount)) {
            setBlockCount(a.getInt(R.styleable.CircleProgressView_cpv_blockCount, 1));
            setBlockScale(a.getFloat(R.styleable.CircleProgressView_cpv_blockScale, 0.9f));
        }

        if (a.hasValue(R.styleable.CircleProgressView_cpv_textTypeface)) {
            try {
                textTypeface = Typeface.createFromAsset(getContext().getAssets(), a.getString(R.styleable.CircleProgressView_cpv_textTypeface));
            } catch (Exception exception) {
                // error while trying to inflate typeface (is the path set correctly?)
            }
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_unitTypeface)) {
            try {
                unitTextTypeface = Typeface.createFromAsset(getContext().getAssets(), a.getString(R.styleable.CircleProgressView_cpv_unitTypeface));
            } catch (Exception exception) {
                // error while trying to inflate typeface (is the path set correctly?)
            }
        }

        if (a.hasValue(R.styleable.CircleProgressView_cpv_decimalFormat)) {
            try {
                String pattern = a.getString(R.styleable.CircleProgressView_cpv_decimalFormat);
                if (pattern != null) {
                    decimalFormat = new DecimalFormat(pattern);
                }

            } catch (Exception exception) {
                Log.w(TAG, exception.getMessage());
            }
        }

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
        int size;
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

    /**
     * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
     * because this method is called after measuring the dimensions of MATCH_PARENT and WRAP_CONTENT.
     * Use this dimensions to setup the bounds and paints.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Share the dimensions
        mLayoutWidth = w;
        mLayoutHeight = h;

        setupBounds();
        setupBarPaint();

        if (mClippingBitmap != null) {
            mClippingBitmap = Bitmap.createScaledBitmap(mClippingBitmap, getWidth(), getHeight(), false);
        }

        invalidate();
    }

    //----------------------------------
    // region helper
    private float calcTextSizeForCircle(String _text, Paint _textPaint, RectF _circleBounds) {

        //get mActualTextBounds bounds
        RectF innerCircleBounds = getInnerCircleRect(_circleBounds);
        return calcTextSizeForRect(_text, _textPaint, innerCircleBounds);

    }

    private RectF getInnerCircleRect(RectF _circleBounds) {

        double circleWidth = +_circleBounds.width() - (Math.max(mBarWidth, mRimWidth)) - mOuterContourSize - mInnerContourSize;
        double width = ((circleWidth / 2d) * Math.sqrt(2d));
        float widthDelta = (_circleBounds.width() - (float) width) / 2f;

        float scaleX = 1;
        float scaleY = 1;
        if (isUnitVisible()) {
            switch (mUnitPosition) {
                case TOP:
                case BOTTOM:
                    scaleX = 1.1f; // scaleX square to rectangle, so the longer text with unit fits better
                    scaleY = 0.88f;
                    break;
                case LEFT_TOP:
                case RIGHT_TOP:
                case LEFT_BOTTOM:
                case RIGHT_BOTTOM:
                    scaleX = 0.77f; // scaleX square to rectangle, so the longer text with unit fits better
                    scaleY = 1.33f;
                    break;
            }

        }
        return new RectF(_circleBounds.left + (widthDelta * scaleX), _circleBounds.top + (widthDelta * scaleY), _circleBounds.right - (widthDelta * scaleX), _circleBounds.bottom - (widthDelta * scaleY));

    }

    private void triggerOnProgressChanged(float value) {
        if (onProgressChangedListener != null && value != previousProgressChangedValue) {
            onProgressChangedListener.onProgressChanged(value);
            previousProgressChangedValue = value;
        }
    }

    private void triggerReCalcTextSizesAndPositions() {
        mTextLength = -1;
        mOuterTextBounds = getInnerCircleRect(mCircleBounds);
        invalidate();
    }

    private int calcTextColor(double value) {
        if (mBarColors.length > 1) {
            double percent = 1f / getMaxValue() * value;
            int low = (int) Math.floor((mBarColors.length - 1) * percent);
            int high = low + 1;
            if (low < 0) {
                low = 0;
                high = 1;
            } else if (high >= mBarColors.length) {
                low = mBarColors.length - 2;
                high = mBarColors.length - 1;
            }
            return ColorUtils.getRGBGradient(mBarColors[low], mBarColors[high], (float) (1 - (((mBarColors.length - 1) * percent) % 1d)));
        } else if (mBarColors.length == 1) {
            return mBarColors[0];
        } else {
            return Color.BLACK;
        }
    }

    private void setTextSizeAndTextBoundsWithAutoTextSize(float unitGapWidthHalf, float unitWidth, float unitGapHeightHalf, float unitHeight, String text) {
        RectF textRect = mOuterTextBounds;

        if (mShowUnit) {

            //shrink text Rect so that there is space for the unit
            switch (mUnitPosition) {

                case TOP:
                    textRect = new RectF(mOuterTextBounds.left, mOuterTextBounds.top + unitHeight + unitGapHeightHalf, mOuterTextBounds.right, mOuterTextBounds.bottom);
                    break;
                case BOTTOM:
                    textRect = new RectF(mOuterTextBounds.left, mOuterTextBounds.top, mOuterTextBounds.right, mOuterTextBounds.bottom - unitHeight - unitGapHeightHalf);
                    break;
                case LEFT_TOP:
                case LEFT_BOTTOM:
                    textRect = new RectF(mOuterTextBounds.left + unitWidth + unitGapWidthHalf, mOuterTextBounds.top, mOuterTextBounds.right, mOuterTextBounds.bottom);
                    break;
                case RIGHT_TOP:
                case RIGHT_BOTTOM:
                default:
                    textRect = new RectF(mOuterTextBounds.left, mOuterTextBounds.top, mOuterTextBounds.right - unitWidth - unitGapWidthHalf, mOuterTextBounds.bottom);
                    break;
            }

        }

        mTextPaint.setTextSize(calcTextSizeForRect(text, mTextPaint, textRect) * mTextScale);
        mActualTextBounds = calcTextBounds(text, mTextPaint, textRect); // center text in text rect
    }

    private void setTextSizeAndTextBoundsWithFixedTextSize(String text) {
        mTextPaint.setTextSize(mTextSize);
        mActualTextBounds = calcTextBounds(text, mTextPaint, mCircleBounds); //center text in circle
    }

    private void setUnitTextBoundsAndSizeWithAutoTextSize(float unitGapWidthHalf, float unitWidth, float unitGapHeightHalf, float unitHeight) {
        //calc the rectangle containing the unit text
        switch (mUnitPosition) {

            case TOP: {
                mUnitBounds = new RectF(mOuterTextBounds.left, mOuterTextBounds.top, mOuterTextBounds.right, mOuterTextBounds.top + unitHeight - unitGapHeightHalf);
                break;
            }
            case BOTTOM:
                mUnitBounds = new RectF(mOuterTextBounds.left, mOuterTextBounds.bottom - unitHeight + unitGapHeightHalf, mOuterTextBounds.right, mOuterTextBounds.bottom);
                break;
            case LEFT_TOP:
            case LEFT_BOTTOM: {
                mUnitBounds = new RectF(mOuterTextBounds.left, mOuterTextBounds.top, mOuterTextBounds.left + unitWidth - unitGapWidthHalf, mOuterTextBounds.top + unitHeight);
                break;
            }
            case RIGHT_TOP:
            case RIGHT_BOTTOM:
            default: {
                mUnitBounds = new RectF(mOuterTextBounds.right - unitWidth + unitGapWidthHalf, mOuterTextBounds.top, mOuterTextBounds.right, mOuterTextBounds.top + unitHeight);
            }
            break;
        }

        mUnitTextPaint.setTextSize(calcTextSizeForRect(mUnit, mUnitTextPaint, mUnitBounds) * mUnitScale);
        mUnitBounds = calcTextBounds(mUnit, mUnitTextPaint, mUnitBounds); // center text in rectangle and reuse it

        switch (mUnitPosition) {


            case LEFT_TOP:
            case RIGHT_TOP: {
                //move unite to top of text
                float dy = mActualTextBounds.top - mUnitBounds.top;
                mUnitBounds.offset(0, dy);
                break;
            }
            case LEFT_BOTTOM:
            case RIGHT_BOTTOM: {
                //move unite to bottom of text
                float dy = mActualTextBounds.bottom - mUnitBounds.bottom;
                mUnitBounds.offset(0, dy);
                break;
            }
        }
    }

    private void setUnitTextBoundsAndSizeWithFixedTextSize(float unitGapWidth, float unitGapHeight) {
        mUnitTextPaint.setTextSize(mUnitTextSize);
        mUnitBounds = calcTextBounds(mUnit, mUnitTextPaint, mOuterTextBounds); // center text in rectangle and reuse it

        switch (mUnitPosition) {

            case TOP:
                mUnitBounds.offsetTo(mUnitBounds.left, mActualTextBounds.top - unitGapHeight - mUnitBounds.height());
                break;
            case BOTTOM:
                mUnitBounds.offsetTo(mUnitBounds.left, mActualTextBounds.bottom + unitGapHeight);
                break;
            case LEFT_TOP:
            case LEFT_BOTTOM:
                mUnitBounds.offsetTo(mActualTextBounds.left - unitGapWidth - mUnitBounds.width(), mUnitBounds.top);
                break;
            case RIGHT_TOP:
            case RIGHT_BOTTOM:
            default:
                mUnitBounds.offsetTo(mActualTextBounds.right + unitGapWidth, mUnitBounds.top);
                break;
        }

        switch (mUnitPosition) {
            case LEFT_TOP:
            case RIGHT_TOP: {
                //move unite to top of text
                float dy = mActualTextBounds.top - mUnitBounds.top;
                mUnitBounds.offset(0, dy);
                break;
            }
            case LEFT_BOTTOM:
            case RIGHT_BOTTOM: {
                //move unite to bottom of text
                float dy = mActualTextBounds.bottom - mUnitBounds.bottom;
                mUnitBounds.offset(0, dy);
                break;
            }
        }
    }


    /**
     * Returns the bounding rectangle of the given _text, with the size and style defined in the _textPaint centered in the middle of the _textBounds
     *
     * @param _text       The text.
     * @param _textPaint  The paint defining the text size and style.
     * @param _textBounds The rect where the text will be centered.
     * @return The bounding box of the text centered in the _textBounds.
     */
    private RectF calcTextBounds(String _text, Paint _textPaint, RectF _textBounds) {

        Rect textBoundsTmp = new Rect();

        //get current text bounds
        _textPaint.getTextBounds(_text, 0, _text.length(), textBoundsTmp);
        float width = textBoundsTmp.left + textBoundsTmp.width();
        float height = textBoundsTmp.bottom + textBoundsTmp.height() * 0.93f; // the height of calcTextBounds is a bit to high, therefore  * 0.93
        //center in circle
        RectF textRect = new RectF();
        textRect.left = (_textBounds.left + ((_textBounds.width() - width) / 2));
        textRect.top = _textBounds.top + ((_textBounds.height() - height) / 2);
        textRect.right = textRect.left + width;
        textRect.bottom = textRect.top + height;


        return textRect;
    }

    //endregion helper
    //----------------------------------

    //----------------------------------
    //region Setting up stuff

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
        float paddingTop = this.getPaddingTop() + (yOffset / 2);
        float paddingBottom = this.getPaddingBottom() + (yOffset / 2);
        float paddingLeft = this.getPaddingLeft() + (xOffset / 2);
        float paddingRight = this.getPaddingRight() + (xOffset / 2);

        int width = getWidth(); //this.getLayoutParams().width;
        int height = getHeight(); //this.getLayoutParams().height;

        float circleWidthHalf = mBarWidth / 2f > mRimWidth / 2f + mOuterContourSize ? mBarWidth / 2f : mRimWidth / 2f + mOuterContourSize;

        mCircleBounds = new RectF(paddingLeft + circleWidthHalf,
                paddingTop + circleWidthHalf,
                width - paddingRight - circleWidthHalf,
                height - paddingBottom - circleWidthHalf);


        mInnerCircleBound = new RectF(paddingLeft + (mBarWidth),
                paddingTop + (mBarWidth),
                width - paddingRight - (mBarWidth),
                height - paddingBottom - (mBarWidth));
        mOuterTextBounds = getInnerCircleRect(mCircleBounds);
        mCircleInnerContour = new RectF(mCircleBounds.left + (mRimWidth / 2.0f) + (mInnerContourSize / 2.0f), mCircleBounds.top + (mRimWidth / 2.0f) + (mInnerContourSize / 2.0f), mCircleBounds.right - (mRimWidth / 2.0f) - (mInnerContourSize / 2.0f), mCircleBounds.bottom - (mRimWidth / 2.0f) - (mInnerContourSize / 2.0f));
        mCircleOuterContour = new RectF(mCircleBounds.left - (mRimWidth / 2.0f) - (mOuterContourSize / 2.0f), mCircleBounds.top - (mRimWidth / 2.0f) - (mOuterContourSize / 2.0f), mCircleBounds.right + (mRimWidth / 2.0f) + (mOuterContourSize / 2.0f), mCircleBounds.bottom + (mRimWidth / 2.0f) + (mOuterContourSize / 2.0f));

        mCenter = new PointF(mCircleBounds.centerX(), mCircleBounds.centerY());
    }

    private void setupBarPaint() {
        if (mBarColors.length > 1) {
            mBarPaint.setShader(new SweepGradient(mCircleBounds.centerX(), mCircleBounds.centerY(), mBarColors, null));
            Matrix matrix = new Matrix();
            mBarPaint.getShader().getLocalMatrix(matrix);

            matrix.postTranslate(-mCircleBounds.centerX(), -mCircleBounds.centerY());
            matrix.postRotate(mStartAngle);
            matrix.postTranslate(mCircleBounds.centerX(), mCircleBounds.centerY());
            mBarPaint.getShader().setLocalMatrix(matrix);
            mBarPaint.setColor(mBarColors[0]);
        } else if (mBarColors.length == 1) {
            mBarPaint.setColor(mBarColors[0]);
            mBarPaint.setShader(null);
        } else {
            mBarPaint.setColor(mBarColorStandard);
            mBarPaint.setShader(null);
        }

        mBarPaint.setAntiAlias(true);
        mBarPaint.setStrokeCap(mBarStrokeCap);
        mBarPaint.setStyle(Style.STROKE);
        mBarPaint.setStrokeWidth(mBarWidth);

        if (mBarStrokeCap != Paint.Cap.BUTT) {
            mShaderlessBarPaint = new Paint(mBarPaint);
            mShaderlessBarPaint.setShader(null);
            mShaderlessBarPaint.setColor(mBarColors[0]);
        }
    }


    /**
     * Setup all paints.
     * Call only if changes to color or size properties are not visible.
     */
    public void setupPaints() {
        setupBarPaint();
        setupBarSpinnerPaint();
        setupOuterContourPaint();
        setupInnerContourPaint();
        setupUnitTextPaint();
        setupTextPaint();
        setupBackgroundCirclePaint();
        setupRimPaint();
        setupBarStartEndLinePaint();
    }

    private void setupBarStartEndLinePaint() {
        mBarStartEndLinePaint.setColor(mBarStartEndLineColor);
        mBarStartEndLinePaint.setAntiAlias(true);
        mBarStartEndLinePaint.setStyle(Style.STROKE);
        mBarStartEndLinePaint.setStrokeWidth(mBarStartEndLineWidth);
    }

    private void setupOuterContourPaint() {
        mOuterContourPaint.setColor(mOuterContourColor);
        mOuterContourPaint.setAntiAlias(true);
        mOuterContourPaint.setStyle(Style.STROKE);
        mOuterContourPaint.setStrokeWidth(mOuterContourSize);
    }

    private void setupInnerContourPaint() {
        mInnerContourPaint.setColor(mInnerContourColor);
        mInnerContourPaint.setAntiAlias(true);
        mInnerContourPaint.setStyle(Style.STROKE);
        mInnerContourPaint.setStrokeWidth(mInnerContourSize);
    }

    private void setupUnitTextPaint() {
        mUnitTextPaint.setStyle(Style.FILL);
        mUnitTextPaint.setAntiAlias(true);
        if (unitTextTypeface != null) {
            mUnitTextPaint.setTypeface(unitTextTypeface);
        }
    }

    private void setupTextPaint() {
        mTextPaint.setSubpixelText(true);
        mTextPaint.setLinearText(true);
        mTextPaint.setTypeface(Typeface.MONOSPACE);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setStyle(Style.FILL);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(mTextSize);
        if (textTypeface != null) {
            mTextPaint.setTypeface(textTypeface);
        } else {
            mTextPaint.setTypeface(Typeface.MONOSPACE);
        }

    }

    private void setupBackgroundCirclePaint() {
        mBackgroundCirclePaint.setColor(mBackgroundCircleColor);
        mBackgroundCirclePaint.setAntiAlias(true);
        mBackgroundCirclePaint.setStyle(Style.FILL);
    }

    private void setupRimPaint() {
        mRimPaint.setColor(mRimColor);
        mRimPaint.setAntiAlias(true);
        mRimPaint.setStyle(Style.STROKE);
        mRimPaint.setStrokeWidth(mRimWidth);
    }

    private void setupBarSpinnerPaint() {
        mBarSpinnerPaint.setAntiAlias(true);
        mBarSpinnerPaint.setStrokeCap(mSpinnerStrokeCap);
        mBarSpinnerPaint.setStyle(Style.STROKE);
        mBarSpinnerPaint.setStrokeWidth(mBarWidth);
        mBarSpinnerPaint.setColor(mSpinnerColor);
    }

    //endregion Setting up stuff
    //----------------------------------

    //----------------------------------
    //region draw all the things

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (DEBUG) {
            drawDebug(canvas);
        }

        float degrees = (360f / mMaxValue * mCurrentValue);

        // Draw the background circle
        if (mBackgroundCircleColor != 0) {
            canvas.drawArc(mInnerCircleBound, 360, 360, false, mBackgroundCirclePaint);
        }
        //Draw the rim
        if (mRimWidth > 0) {
            if (!mShowBlock) {
                canvas.drawArc(mCircleBounds, 360, 360, false, mRimPaint);
            } else {
                drawBlocks(canvas, mCircleBounds, mStartAngle, 360, false, mRimPaint);
            }
        }

        //Draw outer contour
        if (mOuterContourSize > 0) {
            canvas.drawArc(mCircleOuterContour, 360, 360, false, mOuterContourPaint);
        }

        //Draw outer contour
        if (mInnerContourSize > 0) {
            canvas.drawArc(mCircleInnerContour, 360, 360, false, mInnerContourPaint);
        }

        //Draw spinner
        if (mAnimationState == AnimationState.SPINNING || mAnimationState == AnimationState.END_SPINNING) {
            drawSpinner(canvas);
            if (mShowTextWhileSpinning) {
                drawTextWithUnit(canvas);
            }

        } else if (mAnimationState == AnimationState.END_SPINNING_START_ANIMATING) {
            //draw spinning arc
            drawSpinner(canvas);

            if (mDrawBarWhileSpinning) {
                drawBar(canvas, degrees);
                drawTextWithUnit(canvas);
            } else if (mShowTextWhileSpinning) {
                drawTextWithUnit(canvas);
            }

        } else {
            drawBar(canvas, degrees);
            drawTextWithUnit(canvas);
        }

        if (mClippingBitmap != null) {
            canvas.drawBitmap(mClippingBitmap, 0, 0, mMaskPaint);
        }

        if (mBarStartEndLineWidth > 0 && mBarStartEndLine != BarStartEndLine.NONE) {
            drawStartEndLine(canvas, degrees);
        }

    }

    private void drawStartEndLine(Canvas _canvas, float _degrees) {
        if (_degrees == 0f)
            return;

        float startAngle = mDirection == Direction.CW ? mStartAngle : mStartAngle - _degrees;

        startAngle -= mBarStartEndLineSweep / 2f;

        if (mBarStartEndLine == BarStartEndLine.START || mBarStartEndLine == BarStartEndLine.BOTH) {
            _canvas.drawArc(mCircleBounds, startAngle, mBarStartEndLineSweep, false, mBarStartEndLinePaint);
        }

        if (mBarStartEndLine == BarStartEndLine.END || mBarStartEndLine == BarStartEndLine.BOTH) {
            _canvas.drawArc(mCircleBounds, startAngle + _degrees, mBarStartEndLineSweep, false, mBarStartEndLinePaint);
        }
    }

    private void drawDebug(Canvas canvas) {
        Paint innerRectPaint = new Paint();
        innerRectPaint.setColor(Color.YELLOW);
        canvas.drawRect(mCircleBounds, innerRectPaint);
    }

    private void drawBlocks(Canvas _canvas, RectF circleBounds, float startAngle, float _degrees, boolean userCenter, Paint paint) {
        float tmpDegree = 0.0f;
        while (tmpDegree < _degrees) {
            _canvas.drawArc(circleBounds, startAngle + tmpDegree, Math.min(mBlockScaleDegree, _degrees - tmpDegree), userCenter, paint);
            tmpDegree += mBlockDegree;
        }
    }

    private void drawSpinner(Canvas canvas) {
        if (mSpinningBarLengthCurrent < 0) {
            mSpinningBarLengthCurrent = 1;
        }

        float startAngle;
        if (mDirection == Direction.CW) {
            startAngle = mStartAngle + mCurrentSpinnerDegreeValue - mSpinningBarLengthCurrent;
        } else {
            startAngle = mStartAngle - mCurrentSpinnerDegreeValue;
        }

        canvas.drawArc(mCircleBounds, startAngle, mSpinningBarLengthCurrent, false,
                mBarSpinnerPaint);
    }

    private void drawTextWithUnit(Canvas canvas) {

        final float relativeGapHeight;
        final float relativeGapWidth;
        final float relativeHeight;
        final float relativeWidth;

        switch (mUnitPosition) {
            case TOP:
            case BOTTOM:
                relativeGapWidth = 0.05f; //gap size between text and unit
                relativeGapHeight = 0.025f; //gap size between text and unit
                relativeHeight = 0.25f * mRelativeUniteSize;
                relativeWidth = 0.4f * mRelativeUniteSize;
                break;
            default:
            case LEFT_TOP:
            case RIGHT_TOP:
            case LEFT_BOTTOM:
            case RIGHT_BOTTOM:
                relativeGapWidth = 0.05f; //gap size between text and unit
                relativeGapHeight = 0.025f; //gap size between text and unit
                relativeHeight = 0.55f * mRelativeUniteSize;
                relativeWidth = 0.3f * mRelativeUniteSize;
                break;
        }

        float unitGapWidthHalf = mOuterTextBounds.width() * relativeGapWidth / 2f;
        float unitWidth = (mOuterTextBounds.width() * relativeWidth);

        float unitGapHeightHalf = mOuterTextBounds.height() * relativeGapHeight / 2f;
        float unitHeight = (mOuterTextBounds.height() * relativeHeight);


        boolean update = false;
        //Draw Text
        if (mIsAutoColorEnabled) {
            mTextPaint.setColor(calcTextColor(mCurrentValue));
        }

        //set text
        String text;
        switch (mTextMode) {
            case TEXT:
            default:
                text = mText != null ? mText : "";
                break;
            case PERCENT:
                text = decimalFormat.format(100f / mMaxValue * mCurrentValue);
                break;
            case VALUE:
                text = decimalFormat.format(mCurrentValue);
                break;
        }


        // only re-calc position and size if string length changed
        if (mTextLength != text.length()) {

            update = true;
            mTextLength = text.length();
            if (mTextLength == 1) {
                mOuterTextBounds = getInnerCircleRect(mCircleBounds);
                mOuterTextBounds = new RectF(mOuterTextBounds.left + (mOuterTextBounds.width() * 0.1f), mOuterTextBounds.top, mOuterTextBounds.right - (mOuterTextBounds.width() * 0.1f), mOuterTextBounds.bottom);
            } else {
                mOuterTextBounds = getInnerCircleRect(mCircleBounds);
            }
            if (mIsAutoTextSize) {
                setTextSizeAndTextBoundsWithAutoTextSize(unitGapWidthHalf, unitWidth, unitGapHeightHalf, unitHeight, text);

            } else {
                setTextSizeAndTextBoundsWithFixedTextSize(text);
            }
        }

        if (DEBUG) {
            Paint rectPaint = new Paint();
            rectPaint.setColor(Color.MAGENTA);
            canvas.drawRect(mOuterTextBounds, rectPaint);
            rectPaint.setColor(Color.GREEN);
            canvas.drawRect(mActualTextBounds, rectPaint);

        }

        canvas.drawText(text, mActualTextBounds.left - (mTextPaint.getTextSize() * 0.02f), mActualTextBounds.bottom, mTextPaint);

        if (mShowUnit) {

            if (mIsAutoColorEnabled) {
                mUnitTextPaint.setColor(calcTextColor(mCurrentValue));
            }
            if (update) {
                //calc unit text position
                if (mIsAutoTextSize) {
                    setUnitTextBoundsAndSizeWithAutoTextSize(unitGapWidthHalf, unitWidth, unitGapHeightHalf, unitHeight);

                } else {
                    setUnitTextBoundsAndSizeWithFixedTextSize(unitGapWidthHalf * 2f, unitGapHeightHalf * 2f);
                }
            }

            if (DEBUG) {
                Paint rectPaint = new Paint();
                rectPaint.setColor(Color.RED);
                canvas.drawRect(mUnitBounds, rectPaint);
            }

            canvas.drawText(mUnit, mUnitBounds.left - (mUnitTextPaint.getTextSize() * 0.02f), mUnitBounds.bottom, mUnitTextPaint);
        }
    }

    private void drawBar(Canvas _canvas, float _degrees) {
        float startAngle = mDirection == Direction.CW ? mStartAngle : mStartAngle - _degrees;
        if (!mShowBlock) {

            if (mBarStrokeCap != Paint.Cap.BUTT && _degrees > 0 && mBarColors.length > 1) {
                if (_degrees > 180) {
                    _canvas.drawArc(mCircleBounds, startAngle, _degrees / 2, false, mBarPaint);
                    _canvas.drawArc(mCircleBounds, startAngle, 1, false, mShaderlessBarPaint);
                    _canvas.drawArc(mCircleBounds, startAngle + (_degrees / 2), _degrees / 2, false, mBarPaint);
                } else {
                    _canvas.drawArc(mCircleBounds, startAngle, _degrees, false, mBarPaint);
                    _canvas.drawArc(mCircleBounds, startAngle, 1, false, mShaderlessBarPaint);
                }

            } else {
                _canvas.drawArc(mCircleBounds, startAngle, _degrees, false, mBarPaint);
            }
        } else {
            drawBlocks(_canvas, mCircleBounds, startAngle, _degrees, false, mBarPaint);
        }
    }

    //endregion draw
    //----------------------------------


    /**
     * Turn off spinning mode
     */
    public void stopSpinning() {
        setSpin(false);
        mAnimationHandler.sendEmptyMessage(AnimationMsg.STOP_SPINNING.ordinal());
    }

    /**
     * Puts the view in spin mode
     */
    public void spin() {
        setSpin(true);
        mAnimationHandler.sendEmptyMessage(AnimationMsg.START_SPINNING.ordinal());
    }

    private void setSpin(boolean spin) {
        mSpin = spin;
    }

    //----------------------------------
    //region touch input
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {

        if (mSeekModeEnabled == false) {
            return super.onTouchEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP: {
                mTouchEventCount = 0;
                PointF point = new PointF(event.getX(), event.getY());
                float angle = getRotationAngleForPointFromStart(point);
                setValueAnimated(mMaxValue / 360f * angle, 800);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                mTouchEventCount++;
                if (mTouchEventCount > 5) { //touch/move guard
                    PointF point = new PointF(event.getX(), event.getY());
                    float angle = getRotationAngleForPointFromStart(point);
                    setValue(mMaxValue / 360f * angle);
                    return true;
                } else {
                    return false;
                }

            }
            case MotionEvent.ACTION_CANCEL:
                mTouchEventCount = 0;
                return false;
        }


        return super.onTouchEvent(event);
    }

    private float getRotationAngleForPointFromStart(PointF point) {
        long angle = Math.round(calcRotationAngleInDegrees(mCenter, point));
        float fromStart = mDirection == Direction.CW ? angle - mStartAngle : mStartAngle - angle;
        return normalizeAngle(fromStart);
    }


    //endregion touch input
    //----------------------------------


    //-----------------------------------
    //region listener for progress change


    public interface OnProgressChangedListener {
        void onProgressChanged(float value);
    }

    //endregion listener for progress change
    //--------------------------------------

}


