package at.grabner.circleprogress;

import android.animation.TimeInterpolator;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.lang.ref.WeakReference;

public class AnimationHandler extends Handler {

    private final WeakReference<CircleProgressView> mCircleViewWeakReference;
    // Spin bar length in degree at start of animation
    private float mSpinningBarLengthStart;
    private long mAnimationStartTime;
    private long mLengthChangeAnimationStartTime;
    private TimeInterpolator mLengthChangeInterpolator = new DecelerateInterpolator();
    // The interpolator for value animations
    private TimeInterpolator mInterpolator = new AccelerateDecelerateInterpolator();
    private double mLengthChangeAnimationDuration;
    private long mFrameStartTime = 0;

    AnimationHandler(CircleProgressView circleView) {
        super(circleView.getContext().getMainLooper());
        mCircleViewWeakReference = new WeakReference<CircleProgressView>(circleView);
    }


    /**
     * Sets interpolator for value animations.
     *
     * @param mInterpolator the m interpolator
     */
    public void setValueInterpolator(TimeInterpolator mInterpolator) {
        this.mInterpolator = mInterpolator;
    }


    /**
     * Sets the interpolator for length changes of the bar.
     *
     * @param mLengthChangeInterpolator the m length change interpolator
     */
    public void setLengthChangeInterpolator(TimeInterpolator mLengthChangeInterpolator) {
        this.mLengthChangeInterpolator = mLengthChangeInterpolator;
    }

    @Override
    public void handleMessage(Message msg) {
        CircleProgressView circleView = mCircleViewWeakReference.get();
        if (circleView == null) {
            return;
        }
        AnimationMsg msgType = AnimationMsg.values()[msg.what];
        if (msgType == AnimationMsg.TICK) {
            removeMessages(AnimationMsg.TICK.ordinal()); // necessary to remove concurrent ticks.
        }

        //if (msgType != AnimationMsg.TICK)
        //    Log.d("JaGr", TAG + "LOG00099: State:" + circleView.mAnimationState + "     Received: " + msgType);
        mFrameStartTime = SystemClock.uptimeMillis();
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
                        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime));
                        circleView.invalidate();
                        break;
                }

                break;
            case END_SPINNING:
                switch (msgType) {

                    case START_SPINNING:
                        circleView.mAnimationState = AnimationState.SPINNING;
                        if (circleView.mAnimationStateChangedListener != null) {
                            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
                        }
                        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime));

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
                            if (circleView.mAnimationStateChangedListener != null) {
                                circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
                            }
                        }
                        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime));
                        circleView.invalidate();
                        break;
                }

                break;
            case END_SPINNING_START_ANIMATING:
                switch (msgType) {

                    case START_SPINNING:
                        circleView.mDrawBarWhileSpinning = false;
                        enterSpinning(circleView);

                        break;
                    case STOP_SPINNING:
                        //IGNORE already stopping
                        break;
                    case SET_VALUE:
                        circleView.mDrawBarWhileSpinning = false;
                        setValue(msg, circleView);

                        break;
                    case SET_VALUE_ANIMATED:
                        circleView.mValueFrom = 0; // start from zero after spinning
                        circleView.mValueTo = ((float[]) msg.obj)[1];
                        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime));

                        break;
                    case TICK:
                        //shrink spinner till it has its original length
                        if (circleView.mSpinningBarLengthCurrent > circleView.mSpinningBarLengthOrig && !circleView.mDrawBarWhileSpinning) {
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
                        if (circleView.mCurrentSpinnerDegreeValue > 360 && !circleView.mDrawBarWhileSpinning) {
                            mAnimationStartTime = System.currentTimeMillis();
                            circleView.mDrawBarWhileSpinning = true;
                            initReduceAnimation(circleView);
                            if (circleView.mAnimationStateChangedListener != null) {
                                circleView.mAnimationStateChangedListener.onAnimationStateChanged(AnimationState.START_ANIMATING_AFTER_SPINNING);
                            }
                        }

                        //value is already animating, calc animation value and reduce spinner
                        if (circleView.mDrawBarWhileSpinning) {
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
                            if (circleView.mAnimationStateChangedListener != null) {
                                circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
                            }
                            circleView.invalidate();
                            circleView.mDrawBarWhileSpinning = false;
                            circleView.mSpinningBarLengthCurrent = circleView.mSpinningBarLengthOrig;

                        } else {
                            circleView.invalidate();
                        }
                        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime));
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
                        if (calcNextAnimationValue(circleView)) {
                            //animation finished
                            circleView.mAnimationState = AnimationState.IDLE;
                            if (circleView.mAnimationStateChangedListener != null) {
                                circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
                            }
                            circleView.mCurrentValue = circleView.mValueTo;
                        }
                        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime));
                        circleView.invalidate();
                        break;
                }

                break;

        }
    }

    private void enterSetValueAnimated(Message msg, CircleProgressView circleView) {
        circleView.mValueFrom = ((float[]) msg.obj)[0];
        circleView.mValueTo = ((float[]) msg.obj)[1];
        mAnimationStartTime = System.currentTimeMillis();
        circleView.mAnimationState = AnimationState.ANIMATING;
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
        }
        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime));
    }

    private void enterEndSpinningStartAnimating(CircleProgressView circleView, Message msg) {
        circleView.mAnimationState = AnimationState.END_SPINNING_START_ANIMATING;
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
        }
        circleView.mValueFrom = 0; // start from zero after spinning
        circleView.mValueTo = ((float[]) msg.obj)[1];

        mLengthChangeAnimationStartTime = System.currentTimeMillis();
        mSpinningBarLengthStart = circleView.mSpinningBarLengthCurrent;

        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime));

    }

    private void enterEndSpinning(CircleProgressView circleView) {
        circleView.mAnimationState = AnimationState.END_SPINNING;
        initReduceAnimation(circleView);
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
        }
        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime));
    }

    private void initReduceAnimation(CircleProgressView circleView) {
        float degreesTillFinish = circleView.mSpinningBarLengthCurrent;
        float stepsTillFinish = degreesTillFinish / circleView.mSpinSpeed;
        mLengthChangeAnimationDuration = (stepsTillFinish * circleView.mFrameDelayMillis) * 2f;

        mLengthChangeAnimationStartTime = System.currentTimeMillis();
        mSpinningBarLengthStart = circleView.mSpinningBarLengthCurrent;
    }

    private void enterSpinning(CircleProgressView circleView) {
        circleView.mAnimationState = AnimationState.SPINNING;
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
        }
        circleView.mSpinningBarLengthCurrent = (360f / circleView.mMaxValue * circleView.mCurrentValue);
        circleView.mCurrentSpinnerDegreeValue = (360f / circleView.mMaxValue * circleView.mCurrentValue);
        mLengthChangeAnimationStartTime = System.currentTimeMillis();
        mSpinningBarLengthStart = circleView.mSpinningBarLengthCurrent;


        //calc animation time
        float stepsTillFinish = circleView.mSpinningBarLengthOrig / circleView.mSpinSpeed;
        mLengthChangeAnimationDuration = ((stepsTillFinish * circleView.mFrameDelayMillis) * 2f);


        sendEmptyMessageDelayed(AnimationMsg.TICK.ordinal(), circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime));
    }


    /**
     * *
     *
     * @param circleView the circle view
     * @return false if animation still running, true if animation is finished.
     */
    private boolean calcNextAnimationValue(CircleProgressView circleView) {
        float t = (float) ((System.currentTimeMillis() - mAnimationStartTime)
                / circleView.mAnimationDuration);
        t = t > 1.0f ? 1.0f : t;
        float interpolatedRatio = mInterpolator.getInterpolation(t);

        circleView.mCurrentValue = (circleView.mValueFrom + ((circleView.mValueTo - circleView.mValueFrom) * interpolatedRatio));

        return t >= 1;
    }

    private void setValue(Message msg, CircleProgressView circleView) {
        circleView.mValueFrom = circleView.mValueTo;
        circleView.mCurrentValue = circleView.mValueTo = ((float[]) msg.obj)[0];
        circleView.mAnimationState = AnimationState.IDLE;
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState);
        }
        circleView.invalidate();
    }
}
