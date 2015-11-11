package com.beeva.beaconsutils;

public class BluetoothNotSuportedException extends Exception {
    public BluetoothNotSuportedException() {
        super("Bluetooth not supported on this device");
    }
}
