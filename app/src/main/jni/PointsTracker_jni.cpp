/* DO NOT EDIT THIS FILE - it is machine generated */
#include <com_ramboo_pointstracker_PointsTracker.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/core/core.hpp>

#include <ctype.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "native:PointsTracker"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define MAX_COUNT 500
using namespace std;
using namespace cv;

// Supporting functions
inline void Mat_to_vector_Point2f( Mat& mat, vector<Point2f>& v_points)
{
    //v_points.clear();
    //CHECK_MAT(mat.type() == CV_32FC2 && mat.cols == 1);
    int i = 0;
    for (i=0; i < mat.rows; ++i)
    {
        Point2f p = mat.at<Point2f>(i,0);
        v_points.push_back(p);
    }
    //v_points = (vector<Point2f>) mat;
}

inline void vector_Point2f_to_Mat( vector<Point2f>& v_points, Mat& mat)
{
    int i = 0;
    mat.create(v_points.size(), 1, CV_32FC2);
    for (i=0; i < v_points.size(); i++)
    {
        mat.at<Point2f>(i,0) = v_points[i];
    }
    //mat = Mat(v_points, true);
}

/*
 * Class:     com_ramboo_pointstracker_PointsTracker
 * Method:    native_updateTrajectories
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_ramboo_pointstracker_PointsTracker_native_1updateTrajectories
  (JNIEnv *jenv, jclass, jlong ptrGray, jlong ptrPrevGray, jlong ptrPoints0, jlong ptrPoints1, jdouble x, jdouble y,
  jboolean needToInit, jboolean clear, jboolean addRemovePt)
  {
    LOGD("native_updateTrajectories entered");
    Mat gray = *((Mat*)ptrGray); //current frame
    Mat prevGray = *((Mat*)ptrPrevGray); // previous frame

    vector<Point2f> points[2];
    Mat_to_vector_Point2f(*((Mat*)ptrPoints0), points[0]); // current trajectories
    Mat_to_vector_Point2f(*((Mat*)ptrPoints1), points[1]); // current trajectories

    TermCriteria termcrit(TermCriteria::COUNT | TermCriteria::EPS, 20, 0.03);
    Size subPixWinSize(10, 10), winSize(31, 31);

    Point2f point(x,y);
//    jclass pointClass = (*jenv)->GetObjectClass(jenv, pickpoint);
//    //jclass pointClass = jenv->GetObjectClass(pickpoint);
//    LOGD("pointClass converted");
//    //jfieldID fidX = (*jenv)->GetFieldID(jenv, pointClass, "x", "D");
//    jfieldID fidX = jenv->GetFieldID(pointClass, "x", "D");
//    LOGD("GetFieldID done");
//    //jfieldID fidY = (*jenv)->GetFieldID(jenv, pointClass, "y", "D");
//    jfieldID fidY = jenv->GetFieldID(pointClass, "y", "D");
//    //point.x = (*jenv)->GetDoubleField(jenv, pointClass, fidX);
//    point.x = jenv->GetDoubleField(pointClass, fidX);
//    LOGD("GetDoubleField done");
//    //point.y = (*jenv)->GetDoubleField(jenv, pointClass, fidY);
//    point.y = jenv->GetDoubleField(pointClass, fidY);

    if (needToInit)
    {
        LOGD("Nedd to init");
        try
        {
            goodFeaturesToTrack(gray, points[1], MAX_COUNT, 0.01, 10, Mat(), 3, 0, 0.04);
//            LOGD("N.points detected= %d", points[1].size());
//            LOGD("Types %d", ((InputOutputArray)points[1]).getMat().depth());
            if (points[1].size() == 0)
                return;
            cornerSubPix(gray, points[1], subPixWinSize, Size(-1, -1), termcrit);
            addRemovePt = false;
            LOGD("Init done");
        }
        catch(cv::Exception& e)
        {
            LOGD("native_upadeTrajectories caught cv::Exception: %s", e.what());
            jclass je = jenv->FindClass("org/opencv/core/CvException");
            if(!je)
                je = jenv->FindClass("java/lang/Exception");
            jenv->ThrowNew(je, e.what());
        }
        catch (...)
        {
            LOGD("nativeDestroyObject caught unknown exception");
            jclass je = jenv->FindClass("java/lang/Exception");
            jenv->ThrowNew(je, "Unknown exception in JNI code of PointsTracker.native_updateTrajectories()");
        }

    }
    else if (!points[0].empty())
    {
        LOGD("Track points");
        vector<uchar> status;
        vector<float> err;

            calcOpticalFlowPyrLK(prevGray, gray, points[0], points[1],
                                status, err, winSize, 3, termcrit, 0, 0.001);

        size_t i, k;
        for ( i=k=0; i < points[1].size(); i++)
        {
            if (addRemovePt)
            {
                if (norm(point - points[1][i]) <= 5)
                {
                    addRemovePt = false;
                    continue;
                }
            }

            if (!status[i])
                continue;

            points[1][k++] = points[1][i];
        }
        points[1].resize(k);
    }

    if ( addRemovePt && points[1].size() < (size_t)MAX_COUNT )
    {
        vector<Point2f> tmp;
        tmp.push_back(point);
        cornerSubPix(gray, tmp, winSize, Size(-1,-1), termcrit);
        points[1].push_back(tmp[0]);
        addRemovePt = false;
    }

    std::swap(points[1], points[0]);
    cv::swap(prevGray, gray);

    // update results back to pointers
    vector_Point2f_to_Mat(points[0], *((Mat*)ptrPoints0));
    vector_Point2f_to_Mat(points[1], *((Mat*)ptrPoints1));
  }