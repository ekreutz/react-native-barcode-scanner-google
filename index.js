import React, { Component, PropTypes } from 'react';
import { requireNativeComponent, NativeModules, View } from 'react-native';

const scannerModule = NativeModules['BarcodeScannerModule'];

class BarcodeScanner extends Component {
  static propTypes = {
    onBarCodeRead: PropTypes.func, // Callback that fires whenever a new barcode is read
    barcodeTypes: PropTypes.number, // int
    ...View.propTypes
  };

  _onChange(event: Event) {
    if (this.props.onBarCodeRead) {
      this.props.onBarCodeRead({
        data: event.nativeEvent.data, // the barcode itself
        type: event.nativeEvent.type  // the barcode type, eg "EAN_13"
      });
    }
  }

  render() {
    return (
      <NativeBarcodeScanner {...this.props} onChange={this._onChange.bind(this)} />
    );
  }
}

const NativeBarcodeScanner = requireNativeComponent('RCTBarcodeScannerManager', BarcodeScanner, {
  nativeOnly: {onChange: true}
});

// ALL, CODE_128, CODE_39, CODE_93, CODABAR, DATA_MATRIX, EAN_13, EAN_8, ITF, QR_CODE, UPC_A, UPC_E, PDF417, AZTEC
export const BarcodeType = scannerModule.BarcodeType;

export const pauseScanner = scannerModule.pause;

export const resumeScanner = scannerModule.resume;

export default BarcodeScanner;
