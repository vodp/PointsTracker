package com.ramboo.pointstracker;

import android.content.Context;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;
import android.hardware.Camera.Size;
import java.util.List;

/**
 * Created by pateheo on 12/04/15.
 */
public class TrackingCameraView extends JavaCameraView {

    private static final String TAG = "PointsTracker::TrackingCameraView";

    public TrackingCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }
}
