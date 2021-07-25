package android.bluetooth.arduino.led.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.arduino.led.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class BluetoothManager {
    private final int REQUEST_BLUETOOTH_CODE = 1733;
    private final int REQUEST_FIND_LOCATION_CODE = 1734;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothBroadcastReceiver discoveryReceiver;
    private final Activity activity;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    private AlertDialog alertDialog;
    private Runnable runnable;
    private Handler handler;

    public BluetoothManager(Activity activity) {
        this.activity = activity;
        initializeBluetooth();
    }

    public void initializeBluetooth() {
        bluetoothDevices = new ArrayList<>();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            destroy();
        }
    }

    public void startSearching() {
        if (bluetoothAdapter == null) {
            return;
        }
        if (bluetoothPermissionIsFailed()) {
            return;
        }
        bluetoothDevices.clear();
        alertDialog = new ProgressDialog(activity);
        alertDialog.setCancelable(false);
        alertDialog.setTitle("Searching ...");
        alertDialog.setMessage("Please wait");
        alertDialog.show();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        initDiscoveryReceiver();
        bluetoothAdapter.startDiscovery();
    }

    public boolean isEnabled() {
        return bluetoothAdapter != null;
    }

    public boolean bluetoothPermissionIsFailed() {
        if (!bluetoothAdapter.isEnabled()) {
            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_BLUETOOTH_CODE);
            return true;
        }
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FIND_LOCATION_CODE);
            return true;
        }
        return false;
    }

    public boolean handelActivityResult(int requestCode, int resultCode) {
        if (requestCode == REQUEST_BLUETOOTH_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                startSearching();
            } else {
                Toast.makeText(activity, "The request to use Bluetooth was rejected", Toast.LENGTH_SHORT).show();
                destroy();
            }
            return true;
        }
        return false;
    }

    public boolean handelPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_FIND_LOCATION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSearching();
            } else {
                Toast.makeText(activity, "Please confirm the requested permission.", Toast.LENGTH_SHORT).show();
                destroy();
            }
            return true;
        }
        return false;
    }

    private void initDiscoveryReceiver() {
        bluetoothDevices.clear();
        if (handler == null) {
            handler = new Handler();
        } else {
            if (runnable != null) {
                handler.removeCallbacks(runnable);
            }
        }
        runnable = () -> {
            destroyDiscoveryReceiver();
            finishDiscoveryReceiver();
        };
        handler.postDelayed(runnable, 30 * 1000);
        discoveryReceiver = new BluetoothBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    bluetoothDevices.add(device);
                } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                    // finish searching
                    destroyDiscoveryReceiver();
                    finishDiscoveryReceiver();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(discoveryReceiver, intentFilter);
    }

    private void finishDiscoveryReceiver() {
        View view = LayoutInflater.from(activity).inflate(R.layout.bluetooth_device, null);
        bluetoothDevices.addAll(bluetoothAdapter.getBondedDevices());
        ArrayList<String> name = new ArrayList<>();
        for (BluetoothDevice device : bluetoothDevices) {
            name.add(device.getName());
        }
        if (name.isEmpty()) {
            view.findViewById(R.id.devices).setVisibility(View.GONE);
            view.findViewById(R.id.not_found).setVisibility(View.VISIBLE);
            view.findViewById(R.id.research).setOnClickListener(v -> {
                alertDialog.dismiss();
                startSearching();
            });
        } else {
            ListView listView = view.findViewById(R.id.list_view);
            ListAdapter listAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, name);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener((parent, view1, position, id) -> {
                bluetoothAdapter.cancelDiscovery();
                alertDialog.dismiss();
                addBluetoothDevice(bluetoothDevices.get(position));
            });
        }

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
            alertDialog = null;
        }

        alertDialog = new AlertDialog.Builder(activity)
                .setView(view)
                .show();
    }

    public void destroy() {
        destroyDiscoveryReceiver();
        bluetoothAdapter = null;
        bluetoothDevices = null;
    }

    private void destroyDiscoveryReceiver() {
        if (discoveryReceiver != null) {
            activity.unregisterReceiver(discoveryReceiver);
            discoveryReceiver = null;
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
        alertDialog = null;
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        handler = null;
        runnable = null;
    }

    public void addBluetoothDevice(BluetoothDevice bluetoothDevice) {

    }
}
