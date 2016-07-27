'use strict';
//module.exports = NativeModules.UsbSerial;

import {NativeModules,DeviceEventEmitter} from 'react-native';
const _UsbSerial = NativeModules.UsbSerial;
class USB {
    listen(rate,func){
        _UsbSerial.open(rate);
        DeviceEventEmitter.addListener('UsbSerialEvent', func)
    }
    close() {
        DeviceEventEmitter.removeListener('UsbSerialEvent', func)
        _UsbSerial.close();
    }
    write(data){
      _UsbSerial.write(data);
    }
};
USB.open = _UsbSerial.open
USB.close= _UsbSerial.close
USB.write= _UsbSerial.write
export default USB 
