package com.ramboo.pointstracker;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.CvType;
import org.opencv.core.TermCriteria;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import android.hardware.Camera.Size;
import android.widget.Toast;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by pateheo on 09/04/15.
 */
public class PointsTracker extends Activity implements CvCameraViewListener2, View.OnTouchListener {

    public static enum DetectMode{DETECT_MODE_JNI_TBB, DETECT_MODE_JAVA, DETECT_MODE_JNI_GPU};
    public static final String[] DetectNames = {"JNI with TBB", "Java", "JNI with Tegra GPU"};

    private final int MAX_COUNT = 500;
    private static final String    TAG = "OCVSample::Activity";

    private DetectMode mDetectMode;
    private long mFrameCounter;
    private long mStartTime;

    private TrackingCameraView   mOpenCvCameraView;
    private Mat mGray, mPrevGray, mRgba;
    private MatOfPoint2f mOldTrajectories, mNewTrajectories;
    private org.opencv.core.Size mSubPixWinSize;
    private org.opencv.core.Size mWinSize;
    private TermCriteria mTermcrit;

    private Point mPtPick;

    private boolean mNeedToInit;
    private boolean mClear;
    private boolean mAddRemovePt;

    //private MenuItem mItemInit;
    //private MenuItem mItemClear;
    private MenuItem[] mAcceItems;
    private SubMenu mAcceMenu;

    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;
    private List<Size> mResolutionList;

    private LoaderCallbackInterface mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("lktracker");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(PointsTracker.this);
                } break;

                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public PointsTracker() {
        Log.i(TAG, "Instantiated new PointsTracker");
        mPtPick = new Point(0,0);
        mSubPixWinSize = new org.opencv.core.Size(10,10);
        mWinSize = new org.opencv.core.Size(31, 31);
        mTermcrit = new TermCriteria(TermCriteria.COUNT | TermCriteria.EPS, 20, 0.03);
        mNeedToInit = true;
        mClear = false;
        mAddRemovePt = false;
        mDetectMode = DetectMode.DETECT_MODE_JNI_TBB;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_surface_view);

        mOpenCvCameraView = (TrackingCameraView) findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mLoaderCallback);
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        //mItemInit = menu.add("Detect Points");
        //mItemClear = menu.add("Clear Points");
        mAcceMenu = menu.addSubMenu("Acceleration");
        mAcceItems = new MenuItem[PointsTracker.DetectNames.length];
        for (int i = 0; i < PointsTracker.DetectNames.length ; ++i) {
            mAcceItems[i] = mAcceMenu.add(1, i, Menu.NONE, PointsTracker.DetectNames[i]);
        }

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
        int idx = 0;
        while (resolutionItr.hasNext()) {
            Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString()  + "x" + Integer.valueOf(element.height).toString());
            ++idx;
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item.getGroupId() == 2) {

            int id = item.getItemId();
            Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" +
                    Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
            mFrameCounter = 0;
            mStartTime = System.currentTimeMillis();

        } else if (item.getGroupId() == 1) {

            if (item.getItemId() == 0)
                mDetectMode = DetectMode.DETECT_MODE_JNI_TBB;
            else if (item.getItemId() == 1)
                mDetectMode = DetectMode.DETECT_MODE_JAVA;
            else if (item.getItemId() == 2)
                mDetectMode = DetectMode.DETECT_MODE_JNI_GPU;
        }

        return true;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mPrevGray = new Mat(mGray.rows(), mGray.cols(), CvType.CV_32F);
        mOldTrajectories = new MatOfPoint2f();
        mNewTrajectories = new MatOfPoint2f();
        mNeedToInit = true;
        mFrameCounter = 0;
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mPrevGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        if (mPrevGray.empty())
            mGray.copyTo(mPrevGray);

        if (mDetectMode == DetectMode.DETECT_MODE_JNI_TBB) {
            //detect & update
            native_updateTrajectories(mGray.getNativeObjAddr(), mPrevGray.getNativeObjAddr(),
                    mOldTrajectories.getNativeObjAddr(), mNewTrajectories.getNativeObjAddr(),
                    mPtPick.x, mPtPick.y, mNeedToInit, mClear, mAddRemovePt);
            mClear = false;
            mAddRemovePt = false;

        } else if (mDetectMode == DetectMode.DETECT_MODE_JAVA) {

            if (mNeedToInit) {
                //MatOfPoint initPoints = new MatOfPoint();
                //MatOfPoint initPoints = new MatOfPoint();
                Imgproc.goodFeaturesToTrack(mGray, mNewTrajectories, this.MAX_COUNT, 0.01, 10); //, 10, new Mat(), 3, false, 0.04);
                //initPoints.convertTo(mNewTrajectories, CvType.CV_32FC2);
                if (mNewTrajectories.toList().size() == 0)
                    return mRgba;
                Imgproc.cornerSubPix(mGray, mNewTrajectories, mSubPixWinSize, new org.opencv.core.Size(-1,-1), mTermcrit);
                mNeedToInit = false;
            } else if (mOldTrajectories.toList().size() != 0) {
                MatOfByte status = new MatOfByte();
                MatOfFloat err = new MatOfFloat();
                Video.calcOpticalFlowPyrLK(mPrevGray, mGray, mOldTrajectories, mNewTrajectories, status, err, mWinSize, 3, mTermcrit, 0, 0.001);
            }

            mNewTrajectories.copyTo(mOldTrajectories);
            mGray.copyTo(mPrevGray);
        }

        // visualize results
        Point[] trajs = mNewTrajectories.toArray();
        for (int i = 0; i < trajs.length; ++i)
            Core.circle(mRgba, trajs[i], 3, new Scalar(0, 255, 0), -1);

        // timing
        ++mFrameCounter;
        Log.d(TAG, "num_frame" + mFrameCounter);
        String fps = String.format("%2.2f fps", (double) mFrameCounter / (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - mStartTime)));
        Core.putText(mRgba, fps, new Point(50, 50), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 0, 0, 255), 2);

        return mRgba;
    }

    public static native void native_updateTrajectories(
            long gray, long prevGray, long points_0, long points_1, double pickpoint_x, double pickpoint_y,
            boolean needToInit, boolean clear, boolean addRemovePt);


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        double cols = mRgba.cols();
        double rows = mRgba.rows();

        double xOffset = (mOpenCvCameraView.getWidth() - cols)/2;
        double yOffset = (mOpenCvCameraView.getHeight() - rows)/2;

        mPtPick.x = (double)(motionEvent).getX() - xOffset;
        mPtPick.y = (double)(motionEvent).getY() - yOffset;

        mAddRemovePt = true;

        return false;
    }
}
