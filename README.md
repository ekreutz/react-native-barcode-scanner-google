# react-native-barcode-scanner-google


Barcode scanner view for React Native applications. Only for Android. Use something like `react-native-camera` for iOS.
The postfix `-google` is added since the native implementation is based on Google's Barcode API:
https://developers.google.com/vision/barcodes-overview

Compared to other barcode scanners for Android that don't rely on Google's Barcode API, this implementation should prove to be:

  - Faster
  - More accurate
  - More convenient (supports scanning in any direction)


## Properties

#### `barcodeTypes`

Bitmask of the different barcode formats you want to scan for.
For instance `BarcodeType.QR_CODE | BarcodeType.EAN_8 | BarcodeType.EAN_13` or just `BarcodeType.ALL` to search for all barcodes supported. Use `import { BarcodeType } from 'react-native-barcode-scanner-google';` to import the `BarcodeType` object.

All possible values are: (Bitwise OR can be used to select multiple formats)
- `BarcodeType.ALL` (default)
- `BarcodeType.CODE_128`
- `BarcodeType.CODE_39`
- `BarcodeType.CODE_93`
- `BarcodeType.CODABAR`
- `BarcodeType.DATA_MATRIX`
- `BarcodeType.EAN_13`
- `BarcodeType.EAN_8`
- `BarcodeType.ITF`
- `BarcodeType.QR_CODE`
- `BarcodeType.UPC_A`
- `BarcodeType.UPC_E`
- `BarcodeType.PDF417`
- `BarcodeType.AZTEC`

___

#### `focusMode`

Use `import { FocusMode } from 'react-native-barcode-scanner-google';` to import the `FocusMode` object.

Possible values for this prop are:
- `FocusMode.AUTO`: Continuous automatic focus. (default)
- `FocusMode.TAP`: Tap-to-focus
- `FocusMode.FIXED`: Fixed focus

___

#### `onBarcodeRead()`: function(obj: Object)

Alias `onBarCodeRead()`. Callback function that will be called every time the scanner detects a new barcode.
The parameter `obj` has the shape:

```js
{
    "data": "12345678", // the barcode itself
    "type": "EAN_13" // the format of data. will be one of the supported formats, or "UNKNOWN_FORMAT"
}
```

___

#### `onException`: function(key: String)

Google's Barcode API requires some native code to be downloaded to the device behind the scenes for it to work. This makes it possible to always keep the latest barcode scanner featurability available, but also comes with a few drawbacks. Namely, the barcode scanning features might not be available yet when the user opens the app to scan. This callback allows for those exceptions to be handled on the JS side.
Use `import { Exception } from 'react-native-barcode-scanner-google';` to import the `Exception` object.

Possible values for the parameter `key` are:

- `Exception.LOW_STORAGE`: Occurs when the native setup couldn't be completed because the user's device had low storage.
- `Exception.NOT_OPERATIONAL`: Occurs when the user did have enough storage, but opened the app before downloads where completed. Encourage the user to wait a bit or turn on their internet if this happens.

If any of the above events occur, the scanner will default to show a black screen instead of the camera preview.
