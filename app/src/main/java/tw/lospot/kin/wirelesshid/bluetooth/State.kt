package tw.lospot.kin.wirelesshid.bluetooth

enum class State {
    /**
     * INITIALIZED is a target state
     * Next state: PROXYING if startProxy
     */
    INITIALIZED,

    /**
     * getProfileProxy is called and BluetoothHidDevice is not connected.
     * Next state: STOPPED
     */
    PROXYING,

    /**
     * BluetoothHidDevice is connected.
     * Next state: REGISTERING if registerApp
     * Next state: INITIALIZED if closeProxy
     */
    STOPPED,

    /**
     * unregisterApp is called and App Status is not changed yet.
     * Next state: STOPPED
     */
    STOPPING,

    /**
     * registerApp is called and App Status is not changed yet.
     * Next state: REGISTERED
     * Next state: REGISTER_FAIL
     */
    REGISTERING,

    /**
     * REGISTERED is a target state
     * HidCallback is registered.
     * Next state: STOPPING if unregisterApp
     * Next state: CONNECTING if connect to a bluetooth device
     */
    REGISTERED,

    /**
     * Next state: STOPPED after a delay of 5 seconds
     */
    REGISTER_FAIL,

    /**
     * connect is called and the bluetooth device is not connected.
     * Next state: CONNECTED
     * Next state: CONNECT_FAIL if fail to connecting
     */
    CONNECTING,

    /**
     * disconnect is called and the bluetooth device has not responded.
     * Next state: REGISTERED
     * Next state: DISCONNECT_TIMEOUT if the bluetooth device does not respond within 5 seconds
     */
    DISCONNECTING,

    /**
     * CONNECTED is a target state
     * the bluetooth device is connected.
     * Next state: DISCONNECTING if the user requests
     * Next state: REGISTERED if the bluetooth device disconnect without user requests
     */
    CONNECTED,

    /**
     * the bluetooth device is connected.
     * Next state: STOPPING if unregisterApp
     * Next state: REGISTERED after a delay of 5 seconds
     */
    CONNECT_FAIL,

    /**
     * the bluetooth device does not respond within 5 seconds after disconnect
     * Next state: STOPPING
     */
    DISCONNECT_TIMEOUT,
}