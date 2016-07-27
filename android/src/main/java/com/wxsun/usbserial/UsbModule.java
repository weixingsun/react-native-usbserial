package com.wxsun.usbserial;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

public class UsbModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "UsbSerial";
    private static final String UsbEventName="UsbSerialEvent";
    private static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    private static final String ACTION_USB_PERMISSION = "com.wxsun.usbserial.USB_PERMISSION";
    private ReactApplicationContext reactContext;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection usbConnection;
    private UsbSerialDevice serial;
    private int baudRate=9600;

    public UsbModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.usbManager = (UsbManager) this.reactContext.getSystemService(Context.USB_SERVICE);
        this.requestPermission();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        this.reactContext.registerReceiver(usbReceiver, filter);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    //UsbModule.open(data => {  console.log("received: ", data); });
    @ReactMethod
    public void open(final int baudRate) {
        this.baudRate=baudRate;
        this.listen();
    }

    private void listen(){
        if(device==null || usbConnection==null) return;
        serial = UsbSerialDevice.createUsbSerialDevice(device, usbConnection);
        if (serial != null) {
            if (serial.open()) {
                serial.setBaudRate(baudRate);
                serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serial.setParity(UsbSerialInterface.PARITY_ODD);
                serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                serial.read(mCallback);
            } else {
                //errcallback.invoke("DeviceOpenError:busy or driver not working");
            }
        } else {
            //errcallback.invoke("NoDriverError");
        }
    }
    private void sendEvent(String data) {
        WritableMap params = Arguments.createMap();
        params.putString("data", data);
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(UsbEventName, params);
    }

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] data) {
            try {
                String str = new String(data, "UTF-8");
                sendEvent(str);
                Log.d(MODULE_NAME,"received:"+str);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };
    @ReactMethod
    public void write(String data){
        if ( this.serial != null)
            this.serial.write(data.getBytes());
    }
    @ReactMethod
    public void close(){
        if ( this.serial != null)
            this.serial.close();
    }
    private void requestPermission(){
        PendingIntent permissionIntent = PendingIntent.getBroadcast(reactContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                UsbDevice d1 = entry.getValue();
                int deviceVID = d1.getVendorId();
                int devicePID = d1.getProductId();
                Log.d(MODULE_NAME,"VendorId:"+deviceVID+"ProductId:"+devicePID);
                if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    this.usbManager.requestPermission(d1, permissionIntent);
                    this.device = d1;
                }
            }
        }
    }
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if(granted){
                    usbConnection = usbManager.openDevice(device);
                    listen();
                    Log.d(MODULE_NAME,"Permission granted");
                }else{
                    Log.e(MODULE_NAME,"Permission not granted");
                }
            }else if (intent.getAction().equals(ACTION_USB_ATTACHED)) {
                requestPermission(); // A USB device has been attached. Try to open it as a Serial port
            } else if (intent.getAction().equals(ACTION_USB_DETACHED)) {
                if(serial != null) serial.close();
                else Log.e(MODULE_NAME,"USB_DETACHED:serial="+serial);
            }
        }
    };
}
