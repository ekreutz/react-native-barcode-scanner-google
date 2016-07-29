package com.ekreutz.barcodescanner;

import com.ekreutz.barcodescanner.ui.BarcodeScannerView;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

/**
 * React Native ViewManager corresponding to BarcodeScannerView
 */

public class BarcodeScannerManager extends SimpleViewManager<BarcodeScannerView> {

    private BarcodeScannerView mBarcodeScannerView;

    @Override
    public String getName() {
        return "RCTBarcodeScannerManager";
    }

    @Override
    protected BarcodeScannerView createViewInstance(ThemedReactContext reactContext) {
        mBarcodeScannerView = new BarcodeScannerView(reactContext);

        // start the camera stream and barcode decoding
        mBarcodeScannerView.resume();

        return mBarcodeScannerView;
    }

    public BarcodeScannerView getBarcodeScannerView() {
        return mBarcodeScannerView;
    }

    /*
     * -----------------------------------
     * ------------- Props ---------------
     * -----------------------------------
     */

    // Barcode types as a bitmask
    @ReactProp(name = "barcodeTypes", defaultInt = 0)
    public void setBarcodeTypes(BarcodeScannerView view, int barcodeTypes) {
        view.setBarcodeTypes(barcodeTypes);
    }
}
