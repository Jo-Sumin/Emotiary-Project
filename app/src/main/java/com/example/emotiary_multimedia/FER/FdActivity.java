package com.example.emotiary_multimedia.FER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.emotiary_multimedia.R;
import org.tensorflow.lite.Interpreter;

public class FdActivity extends CameraActivity implements CvCameraViewListener2 {

    private static final String    TAG                 = "FdActivity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;


    private Mat                    mRgba;
    private Mat                    mGray;
    private Mat                    Rgba;
    private Mat                    Gray;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;
    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;
    private Bitmap                 bitmap;
    private CameraBridgeViewBase   mOpenCvCameraView;
    private String[] label=new String[]{"angry", "disgust", "fear", "happy", "sad", "surprise", "neutral"};
    private int [] label_index = new int[7];
    private int count=0;        //현재 인식 수
    private int maxcount=10;    //최대 인식 수
    private int res;            //감정 결과
    private Bitmap sbitmap = null;
    boolean album = false;
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    Rgba = new Mat();
                    Gray = new Mat();
                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);
        Intent intent = getIntent();
        Uri uri = intent.getParcelableExtra("ImageUri");
        album = intent.getBooleanExtra("album",false);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);

        if(album){
            Log.d(TAG,"album = true");
            try{
                sbitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e){
                e.printStackTrace();
            }
            detectEmotion(sbitmap);
            detect_result();
        }

        Log.d(TAG,"album = false");
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }




    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Log.d(TAG,"onCameraFrame");

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Mat rotImage= Imgproc.getRotationMatrix2D(new Point(mRgba.cols() /2,
                mRgba.rows() /2), 90, 1.0);
        Imgproc.warpAffine(mRgba, mRgba, rotImage, mRgba.size());
        Imgproc.warpAffine(mGray, mGray, rotImage, mRgba.size());
        Core.flip(mRgba, mRgba,1);
        Core.flip(mGray, mGray,1);
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }
        faceDetect();

        return mRgba;
    }
    private void faceDetect(){

        MatOfRect faces = new MatOfRect();
        Log.d(TAG,"faceDetect start");
        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);

        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();

        if(facesArray.length >= 1) {
            Log.d(TAG,"face detect");
            Imgproc.rectangle(mRgba, facesArray[0].tl(), facesArray[0].br(), FACE_RECT_COLOR, 3);
            bitmap =  Bitmap.createBitmap(mGray.cols(), mGray.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mGray,bitmap);

            Bitmap face_crop = Bitmap.createBitmap(bitmap, facesArray[0].x, facesArray[0].y, facesArray[0].width, facesArray[0].height);

            if(count < maxcount) {
                Log.d(TAG,"count = " + count);
                FdActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        count++;
                        detectEmotion(face_crop);
                    }
                });

            }else{
                detect_result();

            }
        }
    }

    private void detect_result(){
        Log.d(TAG,"detect_result()");
        int max=label_index[0];
        for (int i = 1; i<7; i++){
            if(max<label_index[i]){
                max = label_index[i];
                res = i;
            }
        }
        Log.d(TAG,"res = " + label[res]);
        Intent intent = getIntent();
        intent.putExtra("res", res);
        setResult(RESULT_OK, intent);
        finish();
    }
    private Interpreter getTfliteInterpreter(String modelPath) {
        try {
            return new Interpreter(loadModelFile(FdActivity.this, modelPath));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 모델을 읽어오는 함수
    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public Bitmap getResizedBitmap(Bitmap image, int bitmapWidth, int bitmapHeight) {
        return Bitmap.createScaledBitmap(image, bitmapWidth, bitmapHeight, true);
    }

    public void detectEmotion(Bitmap image){
        Bitmap gray = toGrayscale(image);
        Bitmap resizedImage=getResizedBitmap(gray,48,48);
        int pixelarray[];

        pixelarray = new int[resizedImage.getWidth()*resizedImage.getHeight()];

        resizedImage.getPixels(pixelarray, 0, resizedImage.getWidth(), 0, 0, resizedImage.getWidth(), resizedImage.getHeight());

        float[][][][] bytes_img = new float[1][48][48][1];

        //input shape에 맞게 변환
        for(int y = 0; y < 48; y++) {
            for (int x = 0; x < 48; x++) {
                int pixel = resizedImage.getPixel(x, y);
                bytes_img[0][y][x][0] = (pixel & 0xff) / (float) 255;
            }
        }

        System.out.println("create");
        Interpreter tf_lite = getTfliteInterpreter("converted_model.tflite");


        float[][] output = new float[1][7];
        tf_lite.run(bytes_img, output);

        float max = output[0][0];
        int index = 0;
        for(int i = 1; i < 7; i++) {
            if (max < output[0][i]){
                max = output[0][i];
                index = i;
            }
        }
        label_index[index]++;

        Log.d(TAG, "emotion = "+ label[index]);

    }

}
