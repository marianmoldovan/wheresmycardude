package com.beeva.beaconsutils;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.device.IBeaconDevice;
import uk.co.alt236.bluetoothlelib.util.IBeaconUtils;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BeaconManagerLollipop extends BeaconManager {

    private BluetoothLeScanner bluetoothLeScanner;
    private BeaconScanCallbackLollipop scanCallback;
    private BeaconScanStopRunnable beaconScanStopRunnable;
    private BeaconScanStartRunnable beaconScanStartRunnable;
    private AtomicBoolean scanning;
    private Handler handler;

    private Map<String, IBeaconDevice> temporalBeaconMap;
    private Map<String, IBeaconDevice> beaconMap;

    /**
     *
     * Constructor for Beacon Manager
     *
     * @param context
     * @throws com.beeva.beaconsutils.BluetoothNotActivatedException
     * @throws com.beeva.beaconsutils.BluetoothNotSuportedException
     * @throws com.beeva.beaconsutils.BluetoothLENotSuportedException
     */
    protected BeaconManagerLollipop(Context context) throws BluetoothNotSuportedException, BluetoothLENotSuportedException, BluetoothNotActivatedException {
        super(context);
        bluetoothLeScanner = getBluetoothAdapter().getBluetoothLeScanner();
        beaconMap = new HashMap<String, IBeaconDevice>();
        scanCallback = new BeaconScanCallbackLollipop();
        beaconScanStopRunnable = new BeaconScanStopRunnable();
        beaconScanStartRunnable = new BeaconScanStartRunnable();
        handler = new Handler();
        scanning = new AtomicBoolean(false);
    }
    
    class BeaconScanCallbackLollipop extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //super.onScanResult(callbackType, result);
            BluetoothLeDevice deviceLe = new BluetoothLeDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes(), System.currentTimeMillis());
            if(IBeaconUtils.isThisAnIBeacon(deviceLe)){
                // If device first seen...
                if(!temporalBeaconMap.containsKey(result.getDevice().getAddress())) {
                    IBeaconDevice iBeaconDevice = new IBeaconDevice(deviceLe);
                    if(getFilteredUUID() == null || getFilteredUUID().equalsIgnoreCase(iBeaconDevice.getUUID())) {
                        temporalBeaconMap.put(result.getDevice().getAddress(), iBeaconDevice);
                        if (getBeaconListener() != null)
                            getBeaconListener().onNewBeaconFound(iBeaconDevice);
                    }
                }
                // Else update the device
                else{
                    IBeaconDevice iBeaconDevice = temporalBeaconMap.get(result.getDevice().getAddress());
                    iBeaconDevice.updateRssiReading(deviceLe.getTimestamp(), deviceLe.getRssi());
                    if(getBeaconListener() != null) getBeaconListener().onUpdatedBeaconFound(iBeaconDevice);
                }
            }
        }
    }

    class BeaconScanStopRunnable implements Runnable {

        @Override
        public void run() {
            if(getBluetoothAdapter().isEnabled()) {
                bluetoothLeScanner.stopScan(scanCallback);
                scanning.set(false);

                if (getBeaconListener() != null) {
                    if (temporalBeaconMap.size() > 0)
                        getBeaconListener().onBeaconScanFinished(new ArrayList<IBeaconDevice>(temporalBeaconMap.values()));
                    else getBeaconListener().noBeaconDevicesFound();
                }
                beaconMap.putAll(temporalBeaconMap);

                long pauseTime = PAUSE_PERIOD;
                if(temporalBeaconMap.size() <= 0)
                    pauseTime = LARGE_PAUSE_PERIOD;
                handler.postDelayed(beaconScanStartRunnable, pauseTime);
            }
        }

    }

    class BeaconScanStartRunnable implements Runnable {
        @Override
        public void run() {
            proceedScanning();
            handler.postDelayed(beaconScanStopRunnable, SCAN_PERIOD);
        }
    }

    /**
     * Start periodicall scanning of Beacons
     */
    public void startPeriodicallScan() {
        proceedScanning();
        handler.postDelayed(beaconScanStopRunnable, SCAN_PERIOD);
    }

    private void proceedScanning() {
        if(getBluetoothAdapter().isEnabled()) {
            temporalBeaconMap = new HashMap<String, IBeaconDevice>();
            scanning.set(true);
            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
            bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
        }
        else handler.postDelayed(beaconScanStartRunnable, PAUSE_PERIOD);
        //bluetoothLeScanner.startScan(scanCallback);
    }


    /**
     * Start psingle scan of nearby Beacons. Scanning lasts for 5s.
     */

    public void startSingleScan() {
        temporalBeaconMap = new HashMap<String, IBeaconDevice>();
        scanning.set(true);
        bluetoothLeScanner.startScan(scanCallback);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(scanCallback);
                scanning.set(false);
                if(getBeaconListener() != null){
                    if(temporalBeaconMap.size() > 0 ) getBeaconListener().onBeaconScanFinished(new ArrayList<IBeaconDevice>(temporalBeaconMap.values()));
                    else getBeaconListener().noBeaconDevicesFound();
                }
            }
        }, SCAN_PERIOD * 2);
    }

    public void stopScanning() {
        if(scanning.get() && getBluetoothAdapter() != null && getBluetoothAdapter().isEnabled()) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
        handler.removeCallbacks(beaconScanStartRunnable);
        handler.removeCallbacks(beaconScanStopRunnable);
        scanning.set(false);
    }

    public boolean isScanning(){
        return scanning.get();
    }


    public List<IBeaconDevice> getAllBeaconSeen() {
        return new ArrayList<IBeaconDevice>(beaconMap.values());
    }
}
