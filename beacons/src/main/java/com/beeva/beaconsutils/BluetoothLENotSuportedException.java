package com.beeva.beaconsutils;

public class BluetoothLENotSuportedException extends Exception {
    public BluetoothLENotSuportedException() {
        super("Bluetooth LE Not supported on this device");
    }
}
