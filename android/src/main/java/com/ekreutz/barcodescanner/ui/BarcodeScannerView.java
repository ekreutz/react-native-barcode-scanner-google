package com.ekreutz.barcodescanner.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ekreutz.barcodescanner.camera.CameraSource;
import com.ekreutz.barcodescanner.camera.CameraSourcePreview;
import com.ekreutz.barcodescanner.util.Utils;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class BarcodeScannerView extends ViewGroup implements CameraSource.AutoFocusCallback, MultiProcessor.Factory<Barcode> {

    private final static String TAG = "BARCODE_CAPTURE_VIEW";
    private final Context mContext;

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // For focusing we prefer two continuous methods first, and then finally the "auto" mode which is fired on tap.
    // A device should support at least one of these for scanning to be possible at all.
    private static final String[] PREFERRED_FOCUS_MODES = {Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, Camera.Parameters.FOCUS_MODE_AUTO};

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private boolean mIsPaused = true;

    private int mBarcodeTypes = 0; // 0 for all supported types

    public BarcodeScannerView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public BarcodeScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public BarcodeScannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private boolean hasCameraPermission() {
        int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
        return rc == PackageManager.PERMISSION_GRANTED;
    }

    private void init() {
        mPreview = new CameraSourcePreview(mContext, null);
        addView(mPreview);

        start();
    }

    /**
     * Start the camera for the first time.
     */
    public void start() {
        createCameraSource();
        startCameraSource();
    }

    /**
     * Restarts the camera.
     */
    public void resume() {
        // start the camera only if it isn't already running
        if (mIsPaused) {
            startCameraSource();
        }
    }

    /**
     * Stops the camera.
     */
    public void pause() {
        if (mPreview != null && !mIsPaused) {
            mPreview.stop();
            mIsPaused = true;
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    public void release() {
        if (mPreview != null) {
            mPreview.release();
            mIsPaused = true;
        }
    }

    /**
     * Note: restarts the camera, so can be slow.
     * @param barcodeTypes: desired types bitmask
     */
    public void setBarcodeTypes(int barcodeTypes) {
        mBarcodeTypes = barcodeTypes;

        Log.d("BARCODETYPE", "Set to " + barcodeTypes);

        if (mPreview != null && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                mPreview.replaceBarcodeDetector(createBarcodeDetector(), !mIsPaused);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0, len = getChildCount(); i < len; i++) {
            getChildAt(i).layout(l, t, r, b);
        }
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        // Check permission again and create the source if it wasn't created already
        if (!hasCameraPermission())
            return;

        // set preferred mBarcodeTypes before this :)
        BarcodeDetector barcodeDetector = createBarcodeDetector();

        if (!barcodeDetector.isOperational()) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = mContext.registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(mContext, "Low storage!" /* TODO: replace with proper string handling */, Toast.LENGTH_LONG).show();
                Log.w(TAG, "Low storage!");
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        mCameraSource = new CameraSource.Builder(mContext.getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f)
                .setPreferredFocusModes(PREFERRED_FOCUS_MODES)
                .setFlashMode( /* //[CURRENTLY DON'T USE FLASH!]// useFlash ? Camera.Parameters.FLASH_MODE_TORCH : */ null)
                .build();
    }

    private BarcodeDetector createBarcodeDetector() {
        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, and track the barcodes.
        // The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        Log.d("BARCODETYPE", "Was " + mBarcodeTypes);
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(mContext)
            .setBarcodeFormats(mBarcodeTypes)
            .build();

        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(this).build());

        return barcodeDetector;
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext.getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            final Activity activity = Utils.scanForActivity(mContext);
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(activity, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
                mIsPaused = false;
            } catch (IOException e) {
                mCameraSource.release();
                mCameraSource = null;
            }
        } else {
            Log.d(TAG, "Camera source is null!");
        }
    }

    private void tryAutoFocus() {
        if (mCameraSource != null) {
            mCameraSource.autoFocus(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCameraSource != null && mCameraSource.getFocusMode() != null && mCameraSource.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
            tryAutoFocus();
            return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void onAutoFocus(boolean success) {
        // No actions needed for the focus callback.
        Log.d(TAG, "Did autofocus.");
    }

    @Override
    public Tracker<Barcode> create(Barcode barcode) {
        return new Tracker<Barcode>() {
            /**
             * Start tracking the detected item instance within the item overlay.
             */
            @Override
            public void onNewItem(int id, Barcode item) {
                // Act on new barcode found
                WritableMap event = Arguments.createMap();
                event.putString("data", item.displayValue);
                event.putString("type", BarcodeFormat.get(item.format));

                // Send the newly found data to the JS side
                ReactContext reactContext = (ReactContext) mContext;
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                    getId(),
                    "topChange",
                    event);
            }
        };
    }
}
