package android.bluetooth.arduino.led.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothConnection {
    private static final String NAME = "Arduino";
    private final UUID UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    static final int STATE_CONNECTION_FAILED = -1;
    static final int STATE_NONE = 0;
    static final int STATE_LISTEN = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    private int state = STATE_NONE;
    private final BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private final Handler handler;

    public int getState() {
        return state;
    }

    synchronized void Connect(BluetoothDevice device) {
        if (getState() == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public void write(byte[] out) {
        ConnectedThread con;
        synchronized (this) {
            if (getState() != STATE_CONNECTED || connectedThread == null) {
                return;
            }
            con = connectedThread;
        }
        con.write(out);
    }


    private void setState(int state) {
        if (getState() == state) {
            return;
        }
        this.state = state;
        // send UI
        handler.obtainMessage(BluetoothConnectionController._STATE_CHANGED, state, -1).sendToTarget();
    }

    BluetoothConnection(Handler handler) {
        this.handler = handler;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private synchronized void connectFailed() {
        connectionStart();
    }

    private synchronized void connected(BluetoothDevice device, BluetoothSocket socket) {
        connectionStop(false);
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        Message message = handler.obtainMessage(BluetoothConnectionController._STATE_DEVICE_OBJ);
        Bundle bundle = new Bundle();
        bundle.putParcelable("device", device);
        message.setData(bundle);
        handler.sendMessage(message);
        setState(STATE_CONNECTED);
    }

    private class ConnectThread extends Thread {

        private BluetoothSocket socket;
        private BluetoothDevice device;

        ConnectThread(BluetoothDevice bluetoothDevice) {
            device = bluetoothDevice;
            BluetoothSocket socket = null;
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(UUID);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.socket = socket;
        }

        @Override
        public void run() {
            setName("ConnectThread");
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
            } catch (Exception e) {
                try {
                    socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                    socket.connect();
                } catch (Exception fallbackException) {
                    cancel();
                    // failed
                    handler.obtainMessage(BluetoothConnectionController._STATE_CHANGED, BluetoothConnection.STATE_CONNECTION_FAILED, -1).sendToTarget();
                    connectFailed();
                    return;
                }
            }

            synchronized (BluetoothConnection.this) {
                connectThread = null;
            }

            connected(device, socket);
        }

        public synchronized void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
                device = null;
                socket = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    synchronized void connectionStart() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (bluetoothAdapter.isEnabled()) {
            setState(STATE_LISTEN);
        }
    }

    synchronized void connectionStop() {
        connectionStop(true);
    }

    private synchronized void connectionStop(boolean report) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (report) {
            setState(STATE_NONE);
        }
    }

    private synchronized void connectionLost() {
        handler.obtainMessage(BluetoothConnectionController._STATE_CHANGED, STATE_NONE, -1).sendToTarget();
//        //
        connectionStart();
    }

    private class ConnectedThread extends Thread {

        private BluetoothSocket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        private byte[] bytes;

        ConnectedThread(BluetoothSocket bluetoothSocket) {
            socket = bluetoothSocket;
            InputStream tmpIn = null;
            OutputStream tmoOut = null;


            try {
                tmpIn = socket.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                tmoOut = socket.getOutputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }

            inputStream = tmpIn;
            outputStream = tmoOut;
        }

        @Override
        public void run() {
            setName("ConnectedThread");
            bytes = new byte[1024];
            int len;
            while (true) {
                try {
                    len = inputStream.read(bytes);
                    // send UI
                    handler.obtainMessage(BluetoothConnectionController._STATE_READ, len, -1, bytes).sendToTarget();
                } catch (Exception e) {
                    e.printStackTrace();
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public synchronized void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
                socket = null;
                bytes = null;
                inputStream = null;
                outputStream = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}