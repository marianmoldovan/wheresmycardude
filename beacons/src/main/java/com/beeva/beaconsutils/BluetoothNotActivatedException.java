package com.beeva.beaconsutils;

public class BluetoothNotActivatedException extends Exception {
    public BluetoothNotActivatedException() {
        super("Bluetooth not activated on this device");
    }
}
