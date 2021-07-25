package android.bluetooth.arduino.led.bluetooth;

public interface BluetoothEventListener {
    void bluetoothConnecting();
    void bluetoothConnected(String name);
    void bluetoothDisconnect();
    void bluetoothConnectionFailed();
    void bluetoothDataTransfer(byte[] buffer);
}