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
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
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
    private boolean open=false;
    private StringBuffer buffer = new StringBuffer();
    private String separator="\n";
    //private Map<String, UsbDevice> deviceList = new HashMap<String, UsbDevice>();

    public UsbModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.usbManager = (UsbManager) this.reactContext.getSystemService(Context.USB_SERVICE);
        //this.requestPermission();
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

    //UsbModule.open(9600, '\n');
    @ReactMethod
    public void open(final String name, int baudRate, String separator) {  //boolean sync
        this.baudRate=baudRate;
        this.separator = separator;
        this.device = usbManager.getDeviceList().get(name);
        this.usbConnection = usbManager.openDevice(device);
        this.listen(separator);
    }

    private void listen(String separator){
        if(device==null || usbConnection==null) return;
        serial = UsbSerialDevice.createUsbSerialDevice(device, usbConnection);
        if (serial != null) {
            boolean pass = serial.open(); //sync?serial.syncOpen():serial.open();
            if (pass) {
                serial.setBaudRate(baudRate);
                serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serial.setParity(UsbSerialInterface.PARITY_ODD);
                serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                //if(sync) serial.syncRead(buffer, timeout);
                serial.read(mCallback);
            } else {
                //Log.e("DeviceOpenError:busy or driver not working");
            }
        } else {
            //Log.e("NoDriverError");
        }
    }
    private void sendEvent(String key, String data) {
        WritableMap params = Arguments.createMap();
        params.putString(key, data);
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(UsbEventName, params);
    }
    private void sendEvent(String key, WritableMap data) {
        WritableMap params = Arguments.createMap();
        params.putMap(key, data);
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(UsbEventName, params);
    }
    private void sendEvent(String key, WritableArray data) {
        WritableMap params = Arguments.createMap();
        params.putArray(key, data);
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(UsbEventName, params);
    }

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] data) {
            try {
                String str = new String(data, "UTF-8");
                buffer.append(str);
                if(str.contains(separator)){
                    sendEvent("read", buffer.toString());
                    buffer.setLength(0);
                }
                Log.d(MODULE_NAME,"=====================================================UsbReadCallback.received:"+buffer.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };
    @ReactMethod
    public void write(String data){
        if ( this.serial != null )
            this.serial.write(data.getBytes());
    }
    @ReactMethod
    public void close(){
        if ( this.serial != null)
            this.serial.close();
    }
    private void requestPermission(UsbDevice d1){
        PendingIntent permissionIntent = PendingIntent.getBroadcast(reactContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        int deviceVID = d1.getVendorId();
        int devicePID = d1.getProductId();
        if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
            if(!this.usbManager.hasPermission(d1)){
                this.usbManager.requestPermission(d1, permissionIntent);
                Log.e(MODULE_NAME,"===========================================Ask for permission");
            }else{
                Log.e(MODULE_NAME,"===========================================Remember permission");
            }
        }
    }
    private void requestAllPermission(){
        Map<String, UsbDevice> usbList = usbManager.getDeviceList();
        if (!usbList.isEmpty()) {
            for (Map.Entry<String, UsbDevice> entry : usbList.entrySet()) {
                UsbDevice d1 = entry.getValue();
                requestPermission(d1);
            }
        }
    }
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if(granted && !open){
                    //openDevice(device);
                    Log.e(MODULE_NAME,"================Permission granted");
                }else{
                    Log.e(MODULE_NAME,"============Permission not granted");
                }
            }else if (intent.getAction().equals(ACTION_USB_ATTACHED)) {
                requestAllPermission(); // A USB device has been attached. Try to open it as a Serial port
                sendEvent("attached", generateDeviceList());
            } else if (intent.getAction().equals(ACTION_USB_DETACHED)) {
                close();
                sendEvent("attached", generateDeviceList());
            }
        }
    };
    private WritableArray generateDeviceList(){
        WritableArray list = Arguments.createArray();
        Map<String, UsbDevice> usbList = usbManager.getDeviceList();
        if (!usbList.isEmpty()) {
          for (Map.Entry<String, UsbDevice> entry : usbList.entrySet()) {
            WritableMap item = Arguments.createMap();
            UsbDevice d1 = entry.getValue();
            item.putString("device", d1.getDeviceName());
            item.putString("vendorId", ""+d1.getVendorId());
            item.putString("productId", ""+d1.getProductId());
            item.putString("vendor", d1.getManufacturerName());
            item.putString("product", d1.getProductName());
            item.putString("serialNumber", d1.getSerialNumber());
            list.pushMap(item);
          }
        }
        return list;
    }
    @ReactMethod
    public void listDevices(Promise promise) {
        promise.resolve(generateDeviceList());
    }
}
