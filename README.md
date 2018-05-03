# react-native-barcode-scanner-google

Very fast barcode scanner view for React Native applications. Only for Android. Use something like `react-native-camera` for iOS.
The postfix `-google` is added since the native implementation is based on Google's Barcode API:
https://developers.google.com/vision/barcodes-overview

Compared to other barcode scanners for Android that don't rely on Google's Barcode API, this implementation should prove to be:

* Faster
* More accurate
* More convenient (supports scanning in any direction)

Note that this barcode scanner doesn't ship with a fancy overlay to display a scanning interface to the user. It's just a fast scanner view that shows the camera stream, ontop of which you can overlay your own UI.

## Instructions

To include the latest version (1.3.0) `react-native-barcode-scanner-google` in your project, run the following terminal commands in your React Native project root folder:

1.

```
npm install git+https://github.com/ekreutz/react-native-barcode-scanner-google.git#v1.3.0 --save
```

2.

```
react-native link react-native-barcode-scanner-google
```

## Simple usage example

Example `index.android.js` of an app named BarcodeApp.

```js
import React, { Component } from 'react';
import { AppRegistry, StyleSheet, Text, View, Alert } from 'react-native';
import BarcodeScanner from 'react-native-barcode-scanner-google';

export default class BarcodeApp extends Component {
    render() {
        return (
            <View style={{ flex: 1 }}>
                <BarcodeScanner
                    style={{ flex: 1 }}
                    onBarcodeRead={({ data, type }) => {
                        // handle your scanned barcodes here!
                        // as an example, we show an alert:
                        Alert.alert(
                            `Barcode '${data}' of type '${type}' was scanned.`
                        );
                    }}
                />
            </View>
        );
    }
}

AppRegistry.registerComponent('BarcodeApp', () => BarcodeApp);
```

## Advanced usage example (all properties used)

Note: even though they're not used in this example, notice how we import the functions `pauseScanner` and `resumeScanner`. These can be used in complex UIs to start or stop the camera stream to the scanner. Read more about how they work in _Properties_ down below.

```js
import React, { Component } from 'react';
import { AppRegistry, StyleSheet, Text, View, Alert } from 'react-native';

import BarcodeScanner, {
    Exception,
    FocusMode,
    TorchMode,
    CameraFillMode,
    BarcodeType,
    pauseScanner,
    resumeScanner
} from 'react-native-barcode-scanner-google';

export default class BarcodeApp extends Component {
    render() {
        return (
            <View style={{ flex: 1 }}>
                <BarcodeScanner
                    style={{ flex: 1 }}
                    onBarcodeRead={({ data, type }) => {
                        // handle your scanned barcodes here!
                        // as an example, we show an alert:
                        Alert.alert(
                            `Barcode '${data}' of type '${type}' was scanned.`
                        );
                    }}
                    onException={exceptionKey => {
                        // check instructions on Github for a more detailed overview of these exceptions.
                        switch (exceptionKey) {
                            case Exception.NO_PLAY_SERVICES:
                            // tell the user they need to update Google Play Services
                            case Exception.LOW_STORAGE:
                            // tell the user their device doesn't have enough storage to fit the barcode scanning magic
                            case Exception.NOT_OPERATIONAL:
                            // Google's barcode magic is being downloaded, but is not yet operational.
                            default:
                                break;
                        }
                    }}
                    focusMode={FocusMode.AUTO /* could also be TAP or FIXED */}
                    torchMode={TorchMode.ON /* could be the default OFF */}
                    cameraFillMode={
                        CameraFillMode.COVER /* could also be FIT */
                    }
                    barcodeType={
                        BarcodeType.CODE_128 |
                        BarcodeType.EAN_13 |
                        BarcodeType.EAN_8 /* replace with ALL for all alternatives */
                    }
                />
            </View>
        );
    }
}

AppRegistry.registerComponent('BarcodeApp', () => BarcodeApp);
```

## Properties

#### `barcodeTypes`

Bitmask of the different barcode formats you want to scan for.
For instance `BarcodeType.QR_CODE | BarcodeType.EAN_8 | BarcodeType.EAN_13` or just `BarcodeType.ALL` to search for all barcodes supported. Use `import { BarcodeType } from 'react-native-barcode-scanner-google';` to import the `BarcodeType` object.

All possible values are: (Bitwise OR can be used to select multiple formats)

* `BarcodeType.ALL` (default)
* `BarcodeType.CODE_128`
* `BarcodeType.CODE_39`
* `BarcodeType.CODE_93`
* `BarcodeType.CODABAR`
* `BarcodeType.DATA_MATRIX`
* `BarcodeType.EAN_13`
* `BarcodeType.EAN_8`
* `BarcodeType.ITF`
* `BarcodeType.QR_CODE`
* `BarcodeType.UPC_A`
* `BarcodeType.UPC_E`
* `BarcodeType.PDF417`
* `BarcodeType.AZTEC`

---

#### `focusMode`

Use `import { FocusMode } from 'react-native-barcode-scanner-google';` to import the `FocusMode` object.

Possible values for this prop are:

* `FocusMode.AUTO`: Continuous automatic focus. (default)
* `FocusMode.TAP`: Tap-to-focus
* `FocusMode.FIXED`: Fixed focus

---

#### `torchMode`

Use `import { TorchMode } from 'react-native-barcode-scanner-google';` to import the `TorchMode` object.

Possible values for this prop are:

* `TorchMode.OFF`: Disables flashlight. (default)
* `TorchMode.ON`: Enables flashlight.

---

#### `cameraFillMode`

Use `import { CameraFillMode } from 'react-native-barcode-scanner-google';` to import the `CameraFillMode` object.

Possible values for this prop are:

* `CameraFillMode.COVER`: Make the camera stream fill the entire view, possibly cropping out a little bit on some side. (default)
* `CameraFillMode.FIT`: Make the camera stream fit snugly inside the view, possibly showing wide bars on some side.

---

#### `onBarcodeRead()`: function(obj: Object)

Alias `onBarCodeRead()`. Callback function that will be called every time the scanner detects a new barcode.
The parameter `obj` has the shape:

```js
{
    "data": "12345678", // the barcode itself
    "type": "EAN_13" // the format of data. will be one of the supported formats, or "UNKNOWN_FORMAT"
}
```

---

#### `onException`: function(key: String)

Google's Barcode API requires some native code to be downloaded to the device behind the scenes for it to work. This makes it possible to always keep the latest barcode scanner featurability available, but also comes with a few drawbacks. Namely, the barcode scanning features might not be available yet when the user opens the app to scan. This callback allows for those exceptions to be handled on the JS side.
Use `import { Exception } from 'react-native-barcode-scanner-google';` to import the `Exception` object.

Possible values for the parameter `key` are:

* `Exception.NO_PLAY_SERVICES`: Occurs when the user doesn't have the latest version of Google Play Services. If this is the case, ask the user to update Play Services.
* `Exception.LOW_STORAGE`: Occurs when the native setup couldn't be completed because the user's device had low storage.
* `Exception.NOT_OPERATIONAL`: Occurs when the user did have enough storage, but opened the app before downloads were completed. Encourage the user to wait a bit or turn on their internet if this happens.

If any of the above events occur, the scanner will default to show a black screen instead of the camera preview.

---

#### Utility functions `resumeScanner` and `pauseScanner`: function()

Methods that can be used to pause/resume the camera stream of the barcode scanner JS-side. Most often you will not need these at all! They're meant to give advanced users more control over the scanner view.
Use `import { resumeScanner, pauseScanner } from 'react-native-barcode-scanner-google';` to import these utility functions.

Both methods return a `Promise` object and are used similarly. Example usage of `resumeScanner`:

```js
resumeScanner()
    .then(() => {
        // do something after the scanner (camera) stream was resumed.
    })
    .catch(e => {
        // Print error if scanner stream could not be resumed.
        console.log(e);
    });
```

---

## License

[MIT License](LICENSE)
