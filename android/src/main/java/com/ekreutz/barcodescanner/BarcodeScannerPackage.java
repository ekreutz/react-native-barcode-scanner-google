package com.ekreutz.barcodescanner;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BarcodeScannerPackage implements ReactPackage {

    private BarcodeScannerManager barcodeScannerManager;

    public BarcodeScannerPackage() {
        barcodeScannerManager = new BarcodeScannerManager();
    }

    // Deprecated in RN 0.47 - facebook/react-native@ce6fb33
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(
            barcodeScannerManager
        );
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        return Arrays.<NativeModule>asList(
            new BarcodeScannerModule(reactContext, barcodeScannerManager)
        );
    }
}
