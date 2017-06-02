package com.example.example.myapplication.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.example.example.myapplication.R;
import com.example.example.myapplication.db.ImageDatabase;
import com.example.example.myapplication.remote.DynRH2LevClientWrapper;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.IndexBuilder;
import com.example.example.myapplication.utils.IndexPair;
import com.example.example.myapplication.utils.TagGen;
import com.example.example.myapplication.utils.TensorFlowImageClassifier;
import com.example.example.myapplication.utils.Utils;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.crypto.sse.CryptoPrimitives;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment for the camera class
 */
public class CameraFragment extends Fragment implements View.OnClickListener {

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_FINE_LOCATION_PERMISSION_RESULT = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 3;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int mCaptureState = STATE_PREVIEW;

    private static final int NUM_CLASSES = 1001;
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input:0";
    private static final String OUTPUT_NAME = "output:0";

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private static final int PICK_IMAGE_REQUEST = 1;

    @BindView(R.id.texture) TextureView _texture;
    @BindView(R.id.btn_take_picture) Button _button;
    @BindView(R.id.camera_preview_image) ImageView _imageView;
    @BindView(R.id.btn_view_query) Button _viewQuery;
    @BindView(R.id.btn_close_preview) Button _closePreview;
    @BindView(R.id.input_tags) EditText _inputTags;
    @BindView(R.id.input_tags_layout) TextInputLayout _inputTagsLayout;
    @BindView(R.id.btn_upload_image) Button _uploadImage;
    @BindView(R.id.btn_select_gallery) ImageButton _selectGallery;

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Toast.makeText(mActivity, "TextureView is available", Toast.LENGTH_SHORT);
            setupCamera(width, height);
            Handler handler = new Handler();
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private Bitmap permissionBitmap; // this bitmap should only be used when we are requesting permission for location
    private Bitmap bitmapNoRotation;
    private Bitmap thumbnailNoRotation;
    private Bitmap bitmapWithRotation;
    private Bitmap mediumWithRotation;
    private Bitmap thumbnailWithRotation;
    private Activity mActivity;
    private int imageCounter = 0;
    private String imageName;
    private String mBackCameraID;
    private Size mPreviewSize;
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private File mVideoFolder;
    private String mVideoFileName;
    private File mImageFolder;
    private File mThumbnailFolder;
    private File mMediumFolder;
    private String mImageFileName;
    private String mThumbnailFileName;
    private String mMediumFileName;
    private int mTotalRotation;
    private Size mVideoSize;
    private Size mImageSize;
    private ImageReader mImageReader;
    private MediaRecorder mMediaRecorder;
    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            //super.onCaptureCompleted(session, request, result);
            process(result);
        }

        private void process(CaptureResult captureResult) {
            switch (mCaptureState) {
                case STATE_PREVIEW:
                    // dont do anything
                    break;
                case STATE_WAIT_LOCK:
                    mCaptureState = STATE_PREVIEW;
                    Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        // Toast.makeText(getApplicationContext(), "AF Locked", Toast.LENGTH_SHORT).show();
                        Log.i("CAMERA", "STARTING STILL CAPTURE");
                        startStillCaptureRequest();
                    }
                    break;
            }
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            // we need the imageCounter because for some reason this listener is called twice
            Log.i("CAMERA", "set preview");
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage()));
            imageCounter++;
        }
    };

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long) o1.getWidth() * o1.getHeight() / (long) o2.getWidth() * o2.getHeight());
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mActivity = (Activity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_camera, container, false);
        ButterKnife.bind(this, view);
        mVideoFolder = Utils.getCurrentUserVideoDir(mActivity);
        mImageFolder = Utils.getCurrentUserImageDir(mActivity);
        mMediumFolder = Utils.getCurrentUserMediumDir(mActivity);
        mThumbnailFolder = Utils.getCurrentUserThumbnailDir(mActivity);
        mMediaRecorder = new MediaRecorder();

        _button.setOnClickListener(this);
        _closePreview.setOnClickListener(this);
        _uploadImage.setOnClickListener(this);
        _viewQuery.setOnClickListener(this);
        _selectGallery.setOnClickListener(this);

        // initialize tensorflow
        TensorFlowImageClassifier classifier = new TensorFlowImageClassifier();
        try {
            classifier.initializeTensorFlow(
                    mActivity.getAssets(), MODEL_FILE, LABEL_FILE, NUM_CLASSES, INPUT_SIZE, IMAGE_MEAN, IMAGE_STD,
                    INPUT_NAME, OUTPUT_NAME);
            TagGen.initClassifier(mActivity, classifier);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // set gallery picture
        if (checkExternalStoragePermission()) {
            setGalleryPicture();
        }

        return view;
    }

    public void setGalleryPicture() {
        // Find the last picture
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = getContext().getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

        // Put it in the image view
        if (cursor.moveToFirst()) {
            String imageLocation = cursor.getString(1);
            File imageFile = new File(imageLocation);
            if (imageFile.exists()) {   // TODO: is there a better way to do this?
                Glide.with(mActivity)
                        .load(imageFile)
                        .asBitmap()
                        .centerCrop()
                        .into(new BitmapImageViewTarget(_selectGallery) {
                            @Override
                            protected void setResource(Bitmap resource) {
                                RoundedBitmapDrawable circularBitmapDrawable =
                                        RoundedBitmapDrawableFactory.create(mActivity.getResources(), resource);
                                circularBitmapDrawable.setCircular(true);
                                _selectGallery.setImageDrawable(circularBitmapDrawable);
                            }
                        });
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        Log.i("USER VISIBLE CAMERA", "" + isVisibleToUser);
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && mActivity != null) {
            if (checkExternalStoragePermission()) {
                setGalleryPicture();
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        Log.i("CAMERA HIDDEN", "" + hidden);
        super.onHiddenChanged(hidden);
    }

    /**
     * When the user closes the image preview, set everything back to normal
     */
    public void closePreview() {
        _imageView.setVisibility(View.GONE);
        _texture.setVisibility(View.VISIBLE);
        _button.setVisibility(View.VISIBLE);
        _viewQuery.setVisibility(View.VISIBLE);
        _closePreview.setVisibility(View.GONE);
        _inputTags.setText("");
        _inputTags.setVisibility(View.GONE);
        _inputTagsLayout.setVisibility(View.GONE);
        _uploadImage.setVisibility(View.GONE);
        bitmapNoRotation = null;
        thumbnailNoRotation = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        imageCounter = 0;
        startBackgroundThread();
        if (_texture.isAvailable()) {
            setupCamera(_texture.getWidth(), _texture.getHeight());
            connectCamera();
        } else {
            _texture.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        imageCounter = 0;
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_take_picture:
                lockFocus();
                break;
            case R.id.btn_view_query:
                viewMenu();
                break;
            case R.id.btn_close_preview:
                closePreview();
                break;
            case R.id.btn_upload_image:
                uploadImage();
                break;
            case R.id.btn_select_gallery:
                selectGallery();
                break;
        }
    }

    private void selectGallery() {
        ((VerticalPagerFragment) getParentFragment()).switchAdapterItem(1);
    }

    private boolean checkExternalStoragePermission() {
        // first check if we have permission to read the URI
        int permissionCheck = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    public void requestExternalStoragePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK /*&& data != null && data.getData() != null*/) {
            String[] imagesPath = data.getStringArrayExtra("data");
            Bundle bundle = new Bundle();
            bundle.putStringArray(Const.IMAGE_FILE_BUNDLE_LABEL, imagesPath);
            // launch the batch tagging activity
            Intent batchTag = new Intent(mActivity, BatchTaggingActivity.class);
            batchTag.putExtras(bundle);
            startActivity(batchTag);
            //asyncUploadBatch(imagesPath);
        }
    }

    /*@Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }*/

    public void pressRecordButton(View view) {
        // TODO: eventually add video functionality
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                Log.i("CAMERA", id);
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                } else {
                    mBackCameraID = id;
                    int deviceOrientation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
                    mTotalRotation = sensorToDeviceRotation(characteristics, deviceOrientation);
                    boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                    int rotatedWidth = width;
                    int rotatedHeight = height;
                    if (swapRotation) {
                        rotatedWidth = height;
                        rotatedHeight = width;
                    }
                    Log.i("CAMERA", rotatedWidth + " " + rotatedHeight);
                    Log.i("CAMERA", "swap rotation " + swapRotation);
                    Log.i("CAMERA", "rotation " + mTotalRotation);
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                    mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                    mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                    mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);
                    mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
                    Log.i("CAMERA", "preview size" + mPreviewSize.getHeight() + ", " + mPreviewSize.getWidth());
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Camera");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mBackCameraID, mCameraDeviceStateCallback, null);
                } else {
                    // if we don't have permission, request it
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(mActivity, "Camera app needs camera permission", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
                }
            } else {
                // don't need to explicitly request permission
                cameraManager.openCamera(mBackCameraID, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startRecord() {
        try {
            setupMediaRecorder();
            // use this because we need to know what we are recording
            SurfaceTexture surfaceTexture = _texture.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = mMediaRecorder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(recordSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = _texture.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mPreviewCaptureSession = session;
                    // we just want to preview, but we don't need to do anything with that data
                    try {
                        mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(mActivity, "Unable to show camera preview", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startStillCaptureRequest() {
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mTotalRotation);

            CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    try {
                        createImageFileName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            Log.i("CAMERA", "CAPTURING");
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            return;
        }
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            // if this request is for our camera permission
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // if we didn't get the permission, show user that they need to enable it
                //Toast.makeText(this, "Application cannot run without camera", Toast.LENGTH_SHORT).show();
            }
            /*new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    connectCamera();
                }
            }, 500);*/
        } else if (requestCode == REQUEST_FINE_LOCATION_PERMISSION_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // if we didn't get the permission, show user that they need to enable it
                //Toast.makeText(this, "Geo tagging needs location", Toast.LENGTH_SHORT).show();
            }
            // generate the tags for the image
            final List<String> tags = TagGen.generateTagFromCustomTakenImage(mActivity, permissionBitmap);

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // display the tags to the user
                    _inputTags.setText(Utils.listToString(tags));
                }
            });
        } else if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                /*try {
                    Log.i("PERMISSON", "GOT HERE");
                    Intent intent = new Intent(mActivity, CustomPhotoGalleryActivity.class);
                    startActivityForResult(intent, PICK_IMAGE_REQUEST);
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
            }
        }
    }

    private File createVideoFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "VIDEO_" + timestamp + "_";
        File videoFile = File.createTempFile(prepend, ".mp4", mVideoFolder);
        mVideoFileName = videoFile.getAbsolutePath();
        return videoFile;
    }

    private File createImageFileName() throws IOException {
        imageName = IndexBuilder.csRandomAlphaNumericString(IndexBuilder.FILE_NAME_LENGTH);
        File imageFile = new File(mImageFolder, imageName + ".jpg");
        File thumbnailFile = new File(mThumbnailFolder, imageName + ".jpg");
        File mediumFile = new File(mMediumFolder, imageName + ".jpg");
        mImageFileName = imageFile.getAbsolutePath();
        mThumbnailFileName = thumbnailFile.getAbsolutePath();
        mMediumFileName = mediumFile.getAbsolutePath();
        return imageFile;
    }

    private void setupMediaRecorder() throws IOException {
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setVideoEncodingBitRate(1000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setOrientationHint(mTotalRotation);
        mMediaRecorder.prepare();
    }

    public void lockFocus() {
        Log.i("CAMERA", "TAKING PICTURE");
        if (imageCounter == 0) {
            mCaptureState = STATE_WAIT_LOCK;
        }
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private class ImageSaver implements Runnable {
        private final Image mImage;

        public ImageSaver(Image image) {
            this.mImage = image;
        }

        @Override
        public void run() {
            // put the image data into the byte array
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            mImage.close(); // free resource
            boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
            bitmapNoRotation = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            thumbnailNoRotation = TagGen.decodeSampledBitmapFromBytes(bytes, Const.THUMBNAIL_SCALE_SIZE, Const.THUMBNAIL_SCALE_SIZE);
            final Bitmap bitmap;
            if (swapRotation) {
                bitmap = Utils.rotateBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length), mTotalRotation);
                thumbnailWithRotation = Utils.rotateBitmap(thumbnailNoRotation, mTotalRotation);
                mediumWithRotation = Utils.rotateBitmap(TagGen.decodeSampledBitmapFromBytes(bytes, Const.MEDIUM_SCALE_SIZE, Const.MEDIUM_SCALE_SIZE), mTotalRotation);
            } else {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                thumbnailWithRotation = thumbnailNoRotation;
                mediumWithRotation = TagGen.decodeSampledBitmapFromBytes(bytes, Const.MEDIUM_SCALE_SIZE, Const.MEDIUM_SCALE_SIZE);
            }
            permissionBitmap = bitmap;
            bitmapWithRotation = bitmap;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _imageView.setImageBitmap(bitmap);
                    _imageView.setVisibility(View.VISIBLE);
                    _texture.setVisibility(View.GONE);
                    _button.setVisibility(View.GONE);
                    _viewQuery.setVisibility(View.GONE);
                    _closePreview.setVisibility(View.VISIBLE);
                    _inputTagsLayout.setVisibility(View.VISIBLE);
                    _inputTags.setVisibility(View.VISIBLE);
                    _uploadImage.setVisibility(View.VISIBLE);
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(mActivity, "Enable location for location based tagging", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION_PERMISSION_RESULT);
            } else {
                // generate the tags for the image
                final List<String> tags = TagGen.generateTagFromCustomTakenImage(mActivity, bitmap);

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // display the tags to the user
                        _inputTags.setText(Utils.listToString(tags));
                    }
                });
            }

        }
    }

    public void uploadImage() {
        // generate the inverted index
        String tagString = _inputTags.getText().toString().trim().toLowerCase();
        String paddedTagString = Utils.rightPadding(tagString, Const.TAG_TOTAL_MAX_CHARS);
        IndexPair pair = IndexBuilder.buildIndexFromCustomTaken(mActivity, tagString.split(" "), imageName);
        Log.i("CAMERA", tagString);

        // get our secret key
        byte[] sk = null;
        try {
            sk = Utils.getSk(mActivity);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // now we write the encrypted image to our file system
        try {
            ByteArrayOutputStream fullImageByteStream = new ByteArrayOutputStream();
            ByteArrayOutputStream thumbByteStream = new ByteArrayOutputStream();
            ByteArrayOutputStream mediumByteStream = new ByteArrayOutputStream();
            bitmapWithRotation.compress(Bitmap.CompressFormat.JPEG, 100, fullImageByteStream);
            thumbnailWithRotation.compress(Bitmap.CompressFormat.JPEG, 100, thumbByteStream);
            mediumWithRotation.compress(Bitmap.CompressFormat.JPEG, 100, mediumByteStream);
            bitmapWithRotation = null;
            thumbnailWithRotation = null;
            mediumWithRotation = null;
            System.gc(); // try to free up some memory
            byte[] imageBytes = fullImageByteStream.toByteArray();
            byte[] encImage = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), imageBytes);
            Files.write(encImage, new File(mImageFileName));
            byte[] thumbBytes = thumbByteStream.toByteArray();
            byte[] encThumb = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), thumbBytes);
            Files.write(encThumb, new File(mThumbnailFileName));
            byte[] mediumBytes = mediumByteStream.toByteArray();
            byte[] encMedium = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), mediumBytes);
            Files.write(encMedium, new File(mMediumFileName));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // save the tag in our local sqlite database as well
        try {
            byte[] encTags = CryptoPrimitives.encryptAES_CTR_Byte(sk, CryptoPrimitives.randomBytes(16), paddedTagString.getBytes());
            ImageDatabase idb = new ImageDatabase(mActivity);
            idb.addImageName(imageName);
            idb.addTags(imageName, encTags);
            idb.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // add the image and tag to our local index
        try {
            Multimap<String, String> multimap = Utils.getCurrentUserTagIndex(mActivity);
            for (String tag : tagString.split(" ")) {
                if (!multimap.get(tag).contains(imageName)) {
                    multimap.put(tag, imageName);
                }
            }
            Utils.saveCurrentUserTagIndex(mActivity, multimap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // upload to the server
        try {
            DynRH2LevClientWrapper.updateSingleCustom(mActivity, pair, mImageFileName, mThumbnailFileName, mMediumFileName, imageName, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.i("CAMERA UPLOAD", new String(responseBody));
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.i("CAMERA UPLOAD", new String(responseBody));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // clear all the buttons
        closePreview();
    }


    /*@Override
    public void onBackPressed() {
        // disable going back to the LoginActivity
        moveTaskToBack(true);
    }*/

    public void viewMenu() {
        ((MainActivity) mActivity).switchAdapterItem(0);
        /*Intent intent = new Intent(mActivity, MainActivity.class);
        startActivity(intent);*/
    }

    private static int sensorToDeviceRotation(CameraCharacteristics characteristics, int deviceOrientation) {
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if (!bigEnough.isEmpty()) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        }
        return choices[0];
    }

}
