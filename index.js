import React, { Component, PropTypes } from 'react';
import { requireNativeComponent, NativeModules, View, TouchableHighlight } from 'react-native';

const scannerModule = NativeModules['BarcodeScannerModule'];

class BarcodeScanner extends Component {
  static propTypes = {
    onBarcodeRead: PropTypes.func, // Callback that fires whenever a new barcode is read
    onBarCodeRead: PropTypes.func, // alias of onBarcodeRead (for compatibility with other libraries)

    barcodeTypes: PropTypes.number, // int
    focusMode: PropTypes.number, // int.
    ...View.propTypes
  };

  constructor(props) {
    super(props);
    this.onBarcodeRead = props.onBarcodeRead || props.onBarCodeRead;
  }

  _onChange(event:Event) {
    if (this.onBarcodeRead) {
      this.onBarcodeRead({
        data: event.nativeEvent.data, // the barcode itself
        type: event.nativeEvent.type  // the barcode type, eg "EAN_13"
      });
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

// Alternatives: ALL, CODE_128, CODE_39, CODE_93, CODABAR, DATA_MATRIX, EAN_13, EAN_8, ITF, QR_CODE, UPC_A, UPC_E, PDF417, AZTEC
export const BarcodeType = scannerModule.BarcodeType;

// Alternatives: AUTO, TAP, FIXED. Note: focusMode TAP won't work if you place a view on top of BarcodeScanner, that catches all touch events.
export const FocusMode = scannerModule.FocusMode;

export const pauseScanner = scannerModule.pause;

export const resumeScanner = scannerModule.resume;

export default BarcodeScanner;
