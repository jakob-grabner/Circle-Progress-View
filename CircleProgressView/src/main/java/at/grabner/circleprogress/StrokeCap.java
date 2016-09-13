package at.grabner.circleprogress;

import android.graphics.Paint;

public enum StrokeCap {
    /**
     * The stroke ends with the path, and does not project beyond it.
     */
    BUTT(Paint.Cap.BUTT),
    /**
     * The stroke projects out as a semicircle, with the center at the
     * end of the path.
     */
    ROUND(Paint.Cap.ROUND),
    /**
     * The stroke projects out as a square, with the center at the end
     * of the path.
     */
    SQUARE(Paint.Cap.SQUARE);

    final Paint.Cap paintCap;

    StrokeCap(Paint.Cap paintCap) {
        this.paintCap = paintCap;
    }
}
