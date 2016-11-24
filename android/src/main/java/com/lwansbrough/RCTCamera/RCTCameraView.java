/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/3/16.
 */

package com.lwansbrough.RCTCamera;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View;
import android.graphics.Color;
import android.widget.LinearLayout;

import java.util.List;
import java.beans.PropertyChangeSupport;

public class RCTCameraView extends ViewGroup {
    private final OrientationEventListener _orientationListener;
    private final Context _context;
    private RCTCameraViewFinder _viewFinder = null;
    private int _actualDeviceOrientation = -1;
    private int _aspect = RCTCameraModule.RCT_CAMERA_ASPECT_FIT;
    private int _captureMode = RCTCameraModule.RCT_CAMERA_CAPTURE_MODE_STILL;
    private String _captureQuality = "high";
    private int _torchMode = -1;
    private int _flashMode = -1;
    private TestView testV;
    private FaceOverlayView mFaceView = null;
    private int mOrientation;
    private int mOrientationCompensation;

    public RCTCameraView(Context context) {
        super(context);
        this._context = context;
        RCTCamera.createInstance(getDeviceOrientation(context));

        _orientationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (setActualDeviceOrientation(_context)) {
                    layoutViewFinder();
                }
                // We keep the last known orientation. So if the user first orient
                // the camera then point the camera to floor or sky, we still have
                // the correct orientation.
                if (orientation == ORIENTATION_UNKNOWN) return;
                mOrientation = Util.roundOrientation(orientation, mOrientation);
                // When the screen is unlocked, display rotation may change. Always
                // calculate the up-to-date orientationCompensation.
                int orientationCompensation = mOrientation;
                    //+ Util.getDisplayRotation(CameraActivity.this);
                if (mOrientationCompensation != orientationCompensation) {
                    mOrientationCompensation = orientationCompensation;
                    mFaceView.setOrientation(mOrientationCompensation);
                }
            }
        };

        if (_orientationListener.canDetectOrientation()) {
            _orientationListener.enable();
        } else {
            _orientationListener.disable();
        }
        testV = new TestView(_context);
        this.testV.layout(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
        //addView(testV);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutViewFinder(left, top, right, bottom);
        this.testV.layout(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
        this.mFaceView.layout(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
    }

//    @Override
//    public void onViewAdded(View child) {
//        if (this._viewFinder == child) return;
//        // remove and readd view to make sure it is in the back.
//        // @TODO figure out why there was a z order issue in the first place and fix accordingly.
//        this.removeView(this._viewFinder);
//        this.addView(this._viewFinder, 0);
//        if (this.testV == child) return;
//        this.addView(this.testV, 1);
//    }

    public void setAspect(int aspect) {
        this._aspect = aspect;
        layoutViewFinder();
    }

    public void setCameraType(final int type) {
        if (null != this._viewFinder) {
            this._viewFinder.setCameraType(type);
            RCTCamera.getInstance().adjustPreviewLayout(type);
        } else {
            _viewFinder = new RCTCameraViewFinder(_context, type);
            if (-1 != this._flashMode) {
                _viewFinder.setFlashMode(this._flashMode);
            }
            if (-1 != this._torchMode) {
                _viewFinder.setFlashMode(this._torchMode);
            }
            addView(_viewFinder);
            //addView(testV);
            mFaceView = _viewFinder.getFaceView();
            this.mFaceView.layout(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
            addView(mFaceView);
        }
    }

    public void setCaptureMode(final int captureMode) {
        this._captureMode = captureMode;
        if (this._viewFinder != null) {
            this._viewFinder.setCaptureMode(captureMode);
        }
    }

    public void setCaptureQuality(String captureQuality) {
        this._captureQuality = captureQuality;
        if (this._viewFinder != null) {
            this._viewFinder.setCaptureQuality(captureQuality);
        }
    }

    public void setTorchMode(int torchMode) {
        this._torchMode = torchMode;
        if (this._viewFinder != null) {
            this._viewFinder.setTorchMode(torchMode);
        }
    }

    public void setFlashMode(int flashMode) {
        this._flashMode = flashMode;
        if (this._viewFinder != null) {
            this._viewFinder.setFlashMode(flashMode);
        }
    }

    public void setOrientation(int orientation) {
        RCTCamera.getInstance().setOrientation(orientation);
        if (this._viewFinder != null) {
            layoutViewFinder();
        }
    }

    public void setBarcodeScannerEnabled(boolean barcodeScannerEnabled) {
        RCTCamera.getInstance().setBarcodeScannerEnabled(barcodeScannerEnabled);
    }

    public void setBarCodeTypes(List<String> types) {
        RCTCamera.getInstance().setBarCodeTypes(types);
    }

    private boolean setActualDeviceOrientation(Context context) {
        int actualDeviceOrientation = getDeviceOrientation(context);
        if (_actualDeviceOrientation != actualDeviceOrientation) {
            _actualDeviceOrientation = actualDeviceOrientation;
            RCTCamera.getInstance().setActualDeviceOrientation(_actualDeviceOrientation);
            return true;
        } else {
            return false;
        }
    }

    private int getDeviceOrientation(Context context) {
        return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
    }

    private void layoutViewFinder() {
        layoutViewFinder(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
    }

    private void layoutViewFinder(int left, int top, int right, int bottom) {
        if (null == _viewFinder) {
            return;
        }
        float width = right - left;
        float height = bottom - top;
        int viewfinderWidth;
        int viewfinderHeight;
        double ratio;
        switch (this._aspect) {
            case RCTCameraModule.RCT_CAMERA_ASPECT_FIT:
                ratio = this._viewFinder.getRatio();
                if (ratio * height > width) {
                    viewfinderHeight = (int) (width / ratio);
                    viewfinderWidth = (int) width;
                } else {
                    viewfinderWidth = (int) (ratio * height);
                    viewfinderHeight = (int) height;
                }
                break;
            case RCTCameraModule.RCT_CAMERA_ASPECT_FILL:
                ratio = this._viewFinder.getRatio();
                if (ratio * height < width) {
                    viewfinderHeight = (int) (width / ratio);
                    viewfinderWidth = (int) width;
                } else {
                    viewfinderWidth = (int) (ratio * height);
                    viewfinderHeight = (int) height;
                }
                break;
            default:
                viewfinderWidth = (int) width;
                viewfinderHeight = (int) height;
        }

        int viewFinderPaddingX = (int) ((width - viewfinderWidth) / 2);
        int viewFinderPaddingY = (int) ((height - viewfinderHeight) / 2);

        this._viewFinder.layout(viewFinderPaddingX, viewFinderPaddingY, viewFinderPaddingX + viewfinderWidth, viewFinderPaddingY + viewfinderHeight);
        this.postInvalidate(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
    }
}
