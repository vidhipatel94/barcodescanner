package me.dm7.barcodescanner.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public abstract class BarcodeScannerView extends FrameLayout implements Camera.PreviewCallback {

    private static final int SCANNER_LINE_HEIGHT = 9;
    private Camera mCamera;
    private CameraPreview mPreview;
    private IViewFinder mViewFinderView;
    private Rect mFramingRectInPreview;
    private CameraHandlerThread mCameraHandlerThread;
    private Boolean mFlashState;
    private boolean mAutofocusState = true;
    private boolean showScannerLine = false;

    public BarcodeScannerView(Context context) {
        super(context);
    }

    public BarcodeScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public final void setupLayout(Camera camera) {
        removeAllViews();

        mPreview = new CameraPreview(getContext(), camera, this);
        RelativeLayout relativeLayout = new RelativeLayout(getContext());
        relativeLayout.setGravity(Gravity.CENTER);
        relativeLayout.setBackgroundColor(Color.BLACK);
        relativeLayout.addView(mPreview);
        addView(relativeLayout);

        mViewFinderView = createViewFinderView(getContext());
        if (mViewFinderView instanceof View) {
            addView((View) mViewFinderView);

            if (showScannerLine) {
                getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        setScannerLine();
                    }
                });
            }

        } else {
            throw new IllegalArgumentException("IViewFinder object returned by " +
                    "'createViewFinderView()' should be instance of android.view.View");
        }

    }

    private void setScannerLine() {
        Rect rect = mViewFinderView.getFramingRect();
        int padding = MyViewFinderView.BORDER_STROKE_WIDTH;
        FrameLayout frameLayout = new FrameLayout(getContext());
        LayoutParams layoutParams = new LayoutParams(rect.width() - padding * 2, rect.height() - padding * 2);
        layoutParams.gravity = Gravity.CENTER;
        frameLayout.setLayoutParams(layoutParams);

        final View scannerLine = new View(getContext());
        scannerLine.setBackgroundColor(Color.parseColor("#FE863D"));
        scannerLine.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SCANNER_LINE_HEIGHT));
        frameLayout.addView(scannerLine);

        addView(frameLayout);

        final Animation mAnimation = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.RELATIVE_TO_PARENT, 0f,
                TranslateAnimation.RELATIVE_TO_PARENT, 1.0f);
        mAnimation.setDuration(3000);
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(new LinearInterpolator());
        scannerLine.setAnimation(mAnimation);
    }

    /**
     * <p>Method that creates view that represents visual appearance of a barcode scanner</p>
     * <p>Override it to provide your own view for visual appearance of a barcode scanner</p>
     *
     * @param context {@link Context}
     * @return {@link android.view.View} that implements {@link ViewFinderView}
     */
    protected IViewFinder createViewFinderView(Context context) {
        return new MyViewFinderView(context);
    }

    public void showScannerLine(boolean isSet) {
        this.showScannerLine = isSet;
    }

    public void startCamera(int cameraId) {
        if (mCameraHandlerThread == null) {
            mCameraHandlerThread = new CameraHandlerThread(this);
        }
        mCameraHandlerThread.startCamera(cameraId);
    }

    public void setupCameraPreview(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            setupLayout(mCamera);
            mViewFinderView.setupViewFinder();
            if (mFlashState != null) {
                setFlash(mFlashState);
            }
            setAutoFocus(mAutofocusState);
        }
    }

    public void startCamera() {
        startCamera(-1);
    }

    public void stopCamera() {
        if (mCamera != null) {
            mPreview.stopCameraPreview();
            mPreview.setCamera(null, null);
            mCamera.release();
            mCamera = null;
        }
        if (mCameraHandlerThread != null) {
            mCameraHandlerThread.quit();
            mCameraHandlerThread = null;
        }
    }

    public void stopCameraPreview() {
        if (mPreview != null) {
            mPreview.stopCameraPreview();
        }
    }

    protected void resumeCameraPreview() {
        if (mPreview != null) {
            mPreview.showCameraPreview();
        }
    }

    public synchronized Rect getFramingRectInPreview(int previewWidth, int previewHeight) {
        if (mFramingRectInPreview == null) {
            Rect framingRect = mViewFinderView.getFramingRect();
            int viewFinderViewWidth = mViewFinderView.getWidth();
            int viewFinderViewHeight = mViewFinderView.getHeight();
            if (framingRect == null || viewFinderViewWidth == 0 || viewFinderViewHeight == 0) {
                return null;
            }

            Rect rect = new Rect(framingRect);
            rect.left = rect.left * previewWidth / viewFinderViewWidth;
            rect.right = rect.right * previewWidth / viewFinderViewWidth;
            rect.top = rect.top * previewHeight / viewFinderViewHeight;
            rect.bottom = rect.bottom * previewHeight / viewFinderViewHeight;

            mFramingRectInPreview = rect;
        }
        return mFramingRectInPreview;
    }

    public void setFlash(boolean flag) {
        mFlashState = flag;
        if (mCamera != null && CameraUtils.isFlashSupported(mCamera)) {

            Camera.Parameters parameters = mCamera.getParameters();
            if (flag) {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            mCamera.setParameters(parameters);
        }
    }

    public boolean getFlash() {
        if (mCamera != null && CameraUtils.isFlashSupported(mCamera)) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void toggleFlash() {
        if (mCamera != null && CameraUtils.isFlashSupported(mCamera)) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            mCamera.setParameters(parameters);
        }
    }

    public void setAutoFocus(boolean state) {
        mAutofocusState = state;
        if (mPreview != null) {
            mPreview.setAutoFocus(state);
        }
    }
}
