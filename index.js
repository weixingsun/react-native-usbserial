'use strict';
//module.exports = NativeModules.UsbSerial;

import {DeviceEventEmitter,NativeModules,Platform,} from 'react-native';
const _UsbSerial = NativeModules.UsbSerial;
type Device = {
  vendorId: number;   // The total amount of storage space on the device (in bytes).
  productId: number;    // The amount of available storage space on the device (in bytes).
};
class USB {
    monitor(func){
        if(this.eventListener) this.eventListener.remove();
        this.eventListener = DeviceEventEmitter.addListener('UsbSerialEvent', func);
    }
    listen(name,rate,sep,func){
        if(Platform.OS === 'android' && Platform.Version > 22){
            _UsbSerial.open(name,rate,sep);
            this.monitor(func);
        }
    }
    close() {
        if(Platform.OS === 'android' && Platform.Version > 22){
            //DeviceEventEmitter.removeListener('UsbSerialEvent', func)
            this.eventListener.remove()
            _UsbSerial.close();
        }
    }
    write(data){
        if(Platform.OS === 'android' && Platform.Version > 22){
            _UsbSerial.write(data);
        }
    }
    //USB.listDevices().then((list) => { ... })
    async listDevices(){
        if(Platform.OS === 'android' && Platform.Version > 22){
            return await _UsbSerial.listDevices();
        }else{
            return [];
        }
    }
};
USB.open = _UsbSerial.open
USB.close= _UsbSerial.close
USB.write= _UsbSerial.write
export default new USB()
