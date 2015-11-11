package com.beeva.beaconsutils;

import java.util.List;

import uk.co.alt236.bluetoothlelib.device.IBeaconDevice;

public interface BeaconListener {
    /**
     * Returns all Devices seen during this Scan
     * @param beacons
     */
    void onBeaconScanFinished(List<IBeaconDevice> beacons);

    /**
     * Function called when no Beacons ranged after a scanning period
     */
    void noBeaconDevicesFound();

    /**
     * Method called whenever a new Beacon is ranged
     * @param beacon the new Beacon found
     */
    void onNewBeaconFound(IBeaconDevice beacon);

    /**
     * Method when a Beacon scanned value get updated
     * @param beacon alog with the new properties
     */
    void onUpdatedBeaconFound(IBeaconDevice beacon);
}
