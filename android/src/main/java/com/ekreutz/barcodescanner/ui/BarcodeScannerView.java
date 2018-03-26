package com.ekreutz.barcodescanner.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.ekreutz.barcodescanner.camera.CameraSource;
import com.ekreutz.barcodescanner.camera.CameraSourcePreview;
import com.ekreutz.barcodescanner.util.BarcodeFormat;
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
    private boolean hasAllCapabilities = false; // barcode scanner library and newest play services

    private static final String BARCODE_FOUND_KEY = "barcode_found";
    private static final String LOW_STORAGE_KEY = "low_storage";
    private static final String NOT_YET_OPERATIONAL = "not_yet_operational";
    private static final String NO_PLAY_SERVICES_KEY = "no_play_services";

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // For focusing we prefer two continuous methods first, and then finally the "auto" mode which is fired on tap.
    // A device should support at least one of these for scanning to be possible at all.
    private static final String[] PREFERRED_FOCUS_MODES = {
        Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
        Camera.Parameters.FOCUS_MODE_AUTO,
        Camera.Parameters.FOCUS_MODE_FIXED
    };

    // Since we are only implementig for scaning codes, we are only interested in off and torch mode.
    private static final String[] RELEVANT_FLASH_MODES = {
        Camera.Parameters.FLASH_MODE_OFF,
        Camera.Parameters.FLASH_MODE_TORCH
    };

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private BarcodeDetector mBarcodeDetector;
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

    private boolean hasNecessaryCapabilities() {
        return hasCameraPermission() && hasAllCapabilities;
    }

    public void init() {
        mPreview = new CameraSourcePreview(mContext, null);
        addView(mPreview);

        start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!hasCameraPermission()) {
            // No camera permission. Alert user.
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("No Camera permission")
                .setMessage("Enable camera permission in settings to use the scanner.")
                .setPositiveButton("Ok", null)
                .show();

            return;
        }

        /**
         * Check for a few other things that the device needs for the scanner to work.
         * And send a JS event if something goes wrongs.
         *
         * Checklist: (things are checked in this order)
         * 1. The device has the latest play services
         * 2. The device has sufficient storage
         * 3. The scanner dependencies are downloaded
         */

        // check that the device has (the latest) play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext.getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            sendNativeEvent(NO_PLAY_SERVICES_KEY, Arguments.createMap());
        } else if (mBarcodeDetector != null && !mBarcodeDetector.isOperational()) {
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
                // Detector dependencies can't be downloaded due to low storage
                sendNativeEvent(LOW_STORAGE_KEY, Arguments.createMap());
            } else {
                // Storage isn't low, but dependencies haven't been downloaded yet
                sendNativeEvent(NOT_YET_OPERATIONAL, Arguments.createMap());
            }
        } else {
            hasAllCapabilities = true;
            start();
        }
    }

    /**
     * Start the camera for the first time.
     */
    public void start() {
        if (!hasNecessaryCapabilities())
            return;

        createCameraSource();
        startCameraSource();
    }

    /**
     * Restarts the camera.
     */
    public void resume() {
        // start the camera only if it isn't already running
        if (mIsPaused && hasNecessaryCapabilities()) {
            startCameraSource();
        }
    }

    /**
     * Stops the camera.
     */
    public void pause() {
        if (mPreview != null && !mIsPaused && hasNecessaryCapabilities()) {
            mPreview.stop();
            mIsPaused = true;
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    public void release() {
        if (mPreview != null && hasNecessaryCapabilities()) {
            mPreview.release();
            mIsPaused = true;
        }
    }

    /**
     * Note: restarts the camera, so can be slow.
     * @param barcodeTypes: desired types bitmask
     */
    public void setBarcodeTypes(int barcodeTypes) {
        if (mBarcodeTypes == barcodeTypes) {
            return;
        }

        mBarcodeTypes = barcodeTypes;

        if (mPreview != null && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                mPreview.replaceBarcodeDetector(createBarcodeDetector(), !mIsPaused);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Set focus mode.
     * Possible values: 0 = continuous focus (if supported), 1 = tap-to-focus (if supported), 2 = fixed focus
     * @param focusMode
     */
    public boolean setFocusMode(int focusMode) {
        if (focusMode < 0 || focusMode > 2) {
            focusMode = 0;
        }

        return mCameraSource != null && mCameraSource.setFocusMode(PREFERRED_FOCUS_MODES[focusMode]);
    }

    /**
     * Sets torch mode.
     * Possible values: 0 = off, 1 = torch (always on)
     * @param torchMode
     */
    public boolean setTorchMode(int torchMode) {
        if (torchMode < 0 || torchMode > 1) {
            torchMode = 0;
        }

        return mCameraSource != null && mCameraSource.setFlashMode(RELEVANT_FLASH_MODES[torchMode]);
    }

    /**
     * Set camera fill mode.
     * Possible values:
     *   0 = camera stream will fill the entire view (possibly being cropped)
     *   1 = camera stream will fit snugly within the view (possibly showing fat borders around)
     */
    public void setCameraFillMode(int fillMode) {
        if (mPreview != null) {
            mPreview.setFillMode(fillMode);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0, len = getChildCount(); i < len; i++) {
            // tell the child to fill the whole view when layouting
            getChildAt(i).layout(0, 0, r - l, b - t);
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
        // set preferred mBarcodeTypes before this :)
        BarcodeDetector barcodeDetector = createBarcodeDetector();

        if (!hasNecessaryCapabilities()) {
            return;
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        mCameraSource = new CameraSource.Builder(mContext.getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 900)
                .setRequestedFps(15.0f)
                .setPreferredFocusModes(PREFERRED_FOCUS_MODES)
                .build();
    }

    private BarcodeDetector createBarcodeDetector() {
        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, and track the barcodes.
        // The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(mContext)
            .setBarcodeFormats(mBarcodeTypes)
            .build();

        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(this).build());

        return mBarcodeDetector = barcodeDetector;
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
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

                sendNativeEvent(BARCODE_FOUND_KEY, event);
            }
        };
    }

    private void sendNativeEvent(String key, WritableMap event) {
        if (getId() < 0) {
            Log.w(TAG, "Tried to send native event with negative id!");
            return;
        }

        event.putString("key", key);

        // Send the newly found data to the JS side
        ReactContext reactContext = (ReactContext) mContext;
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
            getId(),
            "topChange",
            event);
    }
}
