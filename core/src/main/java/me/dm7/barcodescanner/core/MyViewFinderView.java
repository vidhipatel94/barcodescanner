package me.dm7.barcodescanner.core;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class MyViewFinderView extends View implements IViewFinder {
    public static final int BORDER_STROKE_WIDTH = 12;

    private static final String TAG = "MyViewFinderView";

    private Rect mFramingRect;

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;

    private static final float DEFAULT_LANDSCAPE_HEIGHT_RATIO = 5f / 8;
    private static final float DEFAULT_PORTRAIT_WIDTH_RATIO = 5.5f / 8;

    private final int mDefaultLaserColor = getResources().getColor(R.color.viewfinder_laser);
    private final int mDefaultMaskColor = getResources().getColor(R.color.viewfinder_mask);
    private final int mDefaultBorderColor = getResources().getColor(R.color.viewfinder_border);
    private final int mDefaultBorderStrokeWidth = getResources().getInteger(R.integer.viewfinder_border_width);
    private final int mDefaultBorderLineLength = 60;

    private float portraitWidthRatio = DEFAULT_PORTRAIT_WIDTH_RATIO;
    private float landscapeHeightRatio = DEFAULT_LANDSCAPE_HEIGHT_RATIO;

    private int topOffset;

    protected Paint mLaserPaint;
    protected Paint mFinderMaskPaint;
    protected Paint mBorderPaint;
    protected int mBorderLineLength;

    public MyViewFinderView(Context context) {
        super(context);
        init();
    }

    public MyViewFinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        //set up laser paint
        mLaserPaint = new Paint();
        mLaserPaint.setColor(mDefaultLaserColor);
        mLaserPaint.setStyle(Paint.Style.FILL);

        //finder mask paint
        mFinderMaskPaint = new Paint();
        mFinderMaskPaint.setColor(mDefaultMaskColor);

        //border paint
        mBorderPaint = new Paint();
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mDefaultBorderStrokeWidth);

        mBorderLineLength = mDefaultBorderLineLength;

        topOffset = DisplayUtils.getDeviceHeight(getContext()) / 4;
    }

    public void setLaserColor(int laserColor) {
        mLaserPaint.setColor(laserColor);
    }

    public void setMaskColor(int maskColor) {
        mFinderMaskPaint.setColor(maskColor);
    }

    public void setBorderColor(int borderColor) {
        mBorderPaint.setColor(borderColor);
    }

    public void setBorderLineLength(int borderLineLength) {
        mBorderLineLength = borderLineLength;
    }

    public void setTopOffset(int topOffset) {
        this.topOffset = topOffset;
    }

    public void setupViewFinder() {
        updateFramingRect();
        invalidate();
    }

    public Rect getFramingRect() {
        return mFramingRect;
    }

    @Override
    public void setPortraitWidthRatio(float portraitWidthRatio) {
        this.portraitWidthRatio = portraitWidthRatio;
    }

    @Override
    public void setLandscapeHeightRatio(float landscapeHeightRatio) {
        this.landscapeHeightRatio = landscapeHeightRatio;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mFramingRect == null) {
            return;
        }

        drawViewFinderMask(canvas);
        drawViewFinderBorder(canvas);
    }

    public void drawViewFinderMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        canvas.drawRect(0, 0, width, mFramingRect.top, mFinderMaskPaint);
        canvas.drawRect(0, mFramingRect.top, mFramingRect.left, mFramingRect.bottom + 1, mFinderMaskPaint);
        canvas.drawRect(mFramingRect.right + 1, mFramingRect.top, width, mFramingRect.bottom + 1, mFinderMaskPaint);
        canvas.drawRect(0, mFramingRect.bottom + 1, width, height, mFinderMaskPaint);
    }

    public void drawViewFinderBorder(Canvas canvas) {
        mBorderPaint.setColor(Color.parseColor("#50ffffff"));
        mBorderPaint.setStrokeWidth(1);
        canvas.drawRect(mFramingRect.left, mFramingRect.top, mFramingRect.right, mFramingRect.bottom, mBorderPaint);

        mBorderPaint.setColor(mDefaultBorderColor);
        mBorderPaint.setStrokeWidth(BORDER_STROKE_WIDTH);
        int padding = BORDER_STROKE_WIDTH / 2;
        int halfPadding = padding / 2 - 1;
        canvas.drawLine(mFramingRect.left + padding, mFramingRect.top + halfPadding, mFramingRect.left + padding, mFramingRect.top + halfPadding + mBorderLineLength, mBorderPaint);
        canvas.drawLine(mFramingRect.left + halfPadding, mFramingRect.top + padding, mFramingRect.left + halfPadding + mBorderLineLength, mFramingRect.top + padding, mBorderPaint);

        canvas.drawLine(mFramingRect.left + padding, mFramingRect.bottom - halfPadding, mFramingRect.left + padding, mFramingRect.bottom - halfPadding - mBorderLineLength, mBorderPaint);
        canvas.drawLine(mFramingRect.left + halfPadding, mFramingRect.bottom - padding, mFramingRect.left + halfPadding + mBorderLineLength, mFramingRect.bottom - padding, mBorderPaint);

        canvas.drawLine(mFramingRect.right - padding, mFramingRect.top + halfPadding, mFramingRect.right - padding, mFramingRect.top + halfPadding + mBorderLineLength, mBorderPaint);
        canvas.drawLine(mFramingRect.right - halfPadding, mFramingRect.top + padding, mFramingRect.right - halfPadding - mBorderLineLength, mFramingRect.top + padding, mBorderPaint);

        canvas.drawLine(mFramingRect.right - padding, mFramingRect.bottom - halfPadding, mFramingRect.right - padding, mFramingRect.bottom - halfPadding - mBorderLineLength, mBorderPaint);
        canvas.drawLine(mFramingRect.right - halfPadding, mFramingRect.bottom - padding, mFramingRect.right - halfPadding - mBorderLineLength, mFramingRect.bottom - padding, mBorderPaint);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        updateFramingRect();
    }

    public synchronized void updateFramingRect() {
        Point viewResolution = new Point(getWidth(), getHeight());
        int width;
        int height;
        int orientation = DisplayUtils.getScreenOrientation(getContext());

        if (orientation != Configuration.ORIENTATION_PORTRAIT) {
            int deviceHeight = DisplayUtils.getDeviceHeight(getContext());
            height = (int) (deviceHeight * landscapeHeightRatio);
//            int landscapeMaxFrameWidth = (int) (1080 * landscapeHeightRatio); // = 5/8 * 1080
//            height = findDesiredDimensionInRange(landscapeHeightRatio, viewResolution.y, MIN_FRAME_HEIGHT, landscapeMaxFrameWidth);
            width = height;

        } else {
            int deviceWidth = DisplayUtils.getDeviceWidth(getContext());
            width = (int) (deviceWidth * portraitWidthRatio);
//            int portraitMaxFrameWidth = (int) (1080 * portraitWidthRatio);
//            width = findDesiredDimensionInRange(portraitWidthRatio, viewResolution.x, MIN_FRAME_WIDTH, portraitMaxFrameWidth);
            height = width;
        }

        int leftOffset = (int) ((viewResolution.x - width) / 2);
        // int topOffset = (int) ((viewResolution.y - height) / 2.4);
        mFramingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
    }

    private static int findDesiredDimensionInRange(float ratio, int resolution, int hardMin, int hardMax) {
        int dim = (int) (ratio * resolution);
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

}
