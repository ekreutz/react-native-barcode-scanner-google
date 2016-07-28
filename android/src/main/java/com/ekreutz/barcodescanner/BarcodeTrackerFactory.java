package com.ekreutz.barcodescanner;

import android.util.Log;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Factory for creating a tracker to be associated with a new barcode.  The multi-processor
 * uses this factory to create barcode trackers as needed -- one for each barcode.
 */

class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
    @Override
    public Tracker<Barcode> create(Barcode barcode) {
        return new Tracker<Barcode>() {
            /**
             * Start tracking the detected item instance within the item overlay.
             */
            @Override
            public void onNewItem(int id, Barcode item) {
                // Act on new barcode found
                Log.d("BARCODE11", item.displayValue);
            }
        };
    }

}
