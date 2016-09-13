package at.grabner.circleprogress;

import android.graphics.Color;
import android.support.annotation.ColorInt;

/**
 * Created by Jakob on 05.09.2015.
 */
public class ColorUtils {

    public static int getRGBGradient(@ColorInt int startColor, @ColorInt int endColor, float proportion) {

        int[] rgb = new int[3];
        rgb[0] = interpolate(Color.red(startColor), Color.red(endColor), proportion);
        rgb[1] = interpolate(Color.green(startColor), Color.green(endColor), proportion);
        rgb[2] = interpolate(Color.blue(startColor), Color.blue(endColor), proportion);
        return Color.argb(255, rgb[0], rgb[1], rgb[2]);
    }


    private static int interpolate(float a, float b, float proportion) {
        return Math.round((a * (proportion)) + (b * (1 - proportion)));
    }


// not finished
//    public static @ColorInt int getHSVGradient(@ColorInt int startColor,@ColorInt int endColor, float proportion, HSVColorDirection _direction) {
//        float[] startHSV = new float[3];
//        float[] endHSV = new float[3];
//        Color.colorToHSV(startColor, startHSV);
//        Color.colorToHSV(endColor, endHSV);
//
//        float brightness = (startHSV[2] + endHSV[2]) / 2;
//        float saturation = (startHSV[1] + endHSV[1]) / 2;
//
//        // determine clockwise and counter-clockwise distance between hues
//        float distCCW = (startHSV[0] >= endHSV[0]) ?  360 - startHSV[0] -   endHSV[0] : startHSV[0] -   endHSV[0];
//        float distCW =  (startHSV[0] >= endHSV[0]) ?  endHSV[0] - startHSV[0] :   360 - endHSV[0] - startHSV[0];
//        float hue = 0;
//        switch (_direction) {
//
//            case ClockWise:
//                hue = startHSV[0] + (distCW * proportion) % 360;
//                break;
//            case CounterClockWise:
//                hue = startHSV[0] + (distCCW * proportion) % 360;
//                break;
//            case Shortest:
//                break;
//            case Longest:
//                break;
//        }
//
//        // interpolate h
//        float hue = (float) ((distCW <= distCCW) ? startHSV[0] + (distCW * proportion) : startHSV[0] - (distCCW * proportion));
//        //reuse array
//        endHSV[0] = hue;
//        endHSV[1] = saturation;
//        endHSV[2] = brightness;
//        return Color.HSVToColor(endHSV);
//
//    }
//
//    enum HSVColorDirection{
//        ClockWise,
//        CounterClockWise,
//        Shortest,
//        Longest
//    }

}
