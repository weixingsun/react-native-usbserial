'use strict';
//module.exports = NativeModules.UsbSerial;

import {DeviceEventEmitter,NativeModules,Platform,} from 'react-native';
const _UsbSerial = NativeModules.UsbSerial;
class USB {
    listen(rate,sep,func){
        if(Platform.OS === 'android' && Platform.Version > 22){
            _UsbSerial.open(rate,sep);
            DeviceEventEmitter.addListener('UsbSerialEvent', func);
        }
    }
    close() {
        if(Platform.OS === 'android' && Platform.Version > 22){
            DeviceEventEmitter.removeListener('UsbSerialEvent', func)
            _UsbSerial.close();
        }
    }
    write(data){
        if(Platform.OS === 'android' && Platform.Version > 22){
            _UsbSerial.write(data);
        }
    }
};
USB.open = _UsbSerial.open
USB.close= _UsbSerial.close
USB.write= _UsbSerial.write
export default new USB()
