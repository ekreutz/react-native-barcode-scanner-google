import React, { Component, PropTypes } from 'react';
import { requireNativeComponent, NativeModules, View, TouchableHighlight } from 'react-native';

const scannerModule = NativeModules['BarcodeScannerModule'];

const BARCODE_FOUND_KEY = "barcode_found";
const LOW_STORAGE_EXCEPTION = "low_storage";
const NOT_YET_OPERATIONAL_EXCEPTION = "not_yet_operational";

class BarcodeScanner extends Component {
  static propTypes = {
    onBarcodeRead: PropTypes.func, // Callback that fires whenever a new barcode is read
    onBarCodeRead: PropTypes.func, // alias of onBarcodeRead (for compatibility with other libraries)
    onException: PropTypes.func, // function(reason)

    barcodeTypes: PropTypes.number, // int
    focusMode: PropTypes.number, // int
    ...View.propTypes
  };

  _onChange(event: Event) {
    switch (event.nativeEvent.key) {
      case BARCODE_FOUND_KEY:
        const onBarcodeRead = this.props.onBarcodeRead || this.props.onBarCodeRead;
        if (onBarcodeRead) {
          onBarcodeRead({
            data: event.nativeEvent.data, // the barcode itself
            type: event.nativeEvent.type  // the barcode type, eg "EAN_13"
          });
        }
        break;
      case NOT_YET_OPERATIONAL_EXCEPTION:
      case LOW_STORAGE_EXCEPTION:
        if (this.props.onException) this.props.onException(event.nativeEvent.key);
        break;
    }
  }

  render() {
    return (
      <NativeBarcodeScanner
        {...this.props}
        onChange={this._onChange.bind(this)}
      />
    );
  }
}

const NativeBarcodeScanner = requireNativeComponent('RCTBarcodeScannerManager', BarcodeScanner, {
  nativeOnly: {onChange: true}
});



/* --------------------------------------
 * ------------- Exports ----------------
 * --------------------------------------
 */

// Alternatives: ALL, CODE_128, CODE_39, CODE_93, CODABAR, DATA_MATRIX, EAN_13, EAN_8, ITF, QR_CODE, UPC_A, UPC_E, PDF417, AZTEC
export const BarcodeType = scannerModule.BarcodeType;
// Alternatives: AUTO, TAP, FIXED. Note: focusMode TAP won't work if you place a view on top of BarcodeScanner, that catches all touch events.
export const FocusMode = scannerModule.FocusMode;

export const Exception = { LOW_STORAGE: LOW_STORAGE_EXCEPTION, NOT_OPERATIONAL: NOT_YET_OPERATIONAL_EXCEPTION };


export const pauseScanner = scannerModule.pause;
export const resumeScanner = scannerModule.resume;

export default BarcodeScanner;
