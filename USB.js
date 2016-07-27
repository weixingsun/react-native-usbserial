'use strict';
import React from 'react';
import ReactNative from 'react-native';
var DeviceEventEmitter = React.DeviceEventEmitter;
var UsbSerial = ReactNative.NativeModules.UsbSerial;
var Test = {a:'a',b:'b'}
var USB = {
    listeners: [],
    listen:function(rate,func){
        UsbSerial.open(rate);
        var watchID = this.listeners.length;
        var id = NativeAppEventEmitter.addListener('UsbSerialEvent', func)
        this.listeners.push(id)
        return watchID;
    },
    close:function() {
        this.listeners[0].remove();
        UsbSerial.close();
    },
    write:function(data){
      UsbSerial.write(data);
    },
};
module.export = Test;
