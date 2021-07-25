package android.bluetooth.arduino.led.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;

public class BluetoothConnectionController {

    private final BluetoothConnection bluetoothConnection;
    private Handler handler;
    private final BluetoothEventListener bluetoothEventListener;
    private BluetoothDevice bluetoothDevice;

    static final int _STATE_CHANGED = 0;
    static final int _STATE_READ = 1;
    static final int _STATE_DEVICE_OBJ = 2;

    public BluetoothConnectionController(BluetoothEventListener bluetoothEventListener) {
        this.bluetoothEventListener = bluetoothEventListener;
        initHandler();
        bluetoothConnection = new BluetoothConnection(handler);
        connectionStart();
    }

    private void initHandler() {
        handler = new Handler(msg -> {
            switch (msg.what) {
                case BluetoothConnectionController._STATE_CHANGED:
                    switch (msg.arg1) {
                        case BluetoothConnection.STATE_NONE:
                        case BluetoothConnection.STATE_LISTEN:
                            bluetoothDevice = null;
                            bluetoothEventListener.bluetoothDisconnect();
                            break;
                        case BluetoothConnection.STATE_CONNECTED:
                            bluetoothEventListener.bluetoothConnected(bluetoothDevice.getName());
                            break;
                        case BluetoothConnection.STATE_CONNECTING:
                            bluetoothEventListener.bluetoothConnecting();
                            break;
                        case BluetoothConnection.STATE_CONNECTION_FAILED:
                            bluetoothDevice = null;
                            bluetoothEventListener.bluetoothConnectionFailed();
                            break;
                    }
                    break;
                case BluetoothConnectionController._STATE_READ:
                    byte[] in = (byte[]) msg.obj;
                    bluetoothEventListener.bluetoothDataTransfer(in);
                    break;
                case BluetoothConnectionController._STATE_DEVICE_OBJ:
                    bluetoothDevice = msg.getData().getParcelable("device");
                    break;
            }
            return false;
        });
    }

    public void connect(BluetoothDevice device) {
        bluetoothConnection.Connect(device);
    }

    public void write(byte[] bytes) {
        if (bluetoothConnection != null) {
            bluetoothConnection.write(bytes);
        }
    }

    public void write(int value) {
        write(new byte[]{(byte) value});
    }

    private void connectionStart() {
        bluetoothConnection.connectionStart();
    }

    public void destroy() {
        handler = null;
        bluetoothDevice = null;
        if (bluetoothConnection != null) {
            bluetoothConnection.connectionStop();
        }
    }
}
