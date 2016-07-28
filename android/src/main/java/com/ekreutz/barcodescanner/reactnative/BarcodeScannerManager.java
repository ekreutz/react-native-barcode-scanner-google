package com.ekreutz.barcodescanner.reactnative;

import com.ekreutz.barcodescanner.BarcodeScannerView;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

/**
 * React Native ViewManager corresponding to BarcodeScannerView
 */

public class BarcodeScannerManager extends SimpleViewManager<BarcodeScannerView> implements LifecycleEventListener {

    public static final String REACT_CLASS = "RCTBarcodeScannerView";
    private BarcodeScannerView barcodeScannerView;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected BarcodeScannerView createViewInstance(ThemedReactContext reactContext) {
        reactContext.addLifecycleEventListener(this);

        barcodeScannerView = new BarcodeScannerView(reactContext);
        return barcodeScannerView;
    }



    /*
     * -----------------------------------
     * ------------- Props ---------------
     * -----------------------------------
     */

    @ReactMethod
    public void pausePreview() {
        if (barcodeScannerView != null) {
            barcodeScannerView.pause();
        }
    }

    @ReactMethod
    public void resumePreview() {
        if (barcodeScannerView != null) {
            barcodeScannerView.resume();
        }
    }

    @ReactMethod
    public void stopPreview() {
        if (barcodeScannerView != null) {
            barcodeScannerView.release();
        }
    }

    @ReactMethod
    public void startPreview() {
        if (barcodeScannerView != null) {
            barcodeScannerView.start();
        }
    }



    /*
     * ----------------------------------------------
     * ------------- Lifecycle events ---------------
     * ----------------------------------------------
     */

    @Override
    public void onHostResume() {
        resumePreview();
    }

    @Override
    public void onHostPause() {
        pausePreview();
    }

    @Override
    public void onHostDestroy() {
        stopPreview();
    }
}
