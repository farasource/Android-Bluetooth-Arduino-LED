package android.bluetooth.arduino.led;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.arduino.led.bluetooth.BluetoothConnectionController;
import android.bluetooth.arduino.led.bluetooth.BluetoothManager;
import android.bluetooth.arduino.led.bluetooth.BluetoothEventListener;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements BluetoothEventListener {

    private boolean hasConnection;
    private BluetoothManager bluetoothManager;
    private BluetoothConnectionController bluetoothConnectionController;
    private final int REQUEST_CODE = 1001;
    private TextView status;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item, menu);
        if (hasConnection) {
            menu.findItem(R.id.disconnect).setEnabled(true);
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect:
                if (hasConnection) {
                    Snack("The connection is now established");
                } else {
                    connect();
                }
                return true;
            case R.id.disconnect:
                destroyBluetoothController();
                return true;
            case R.id.about:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.app_name)
                        .setMessage("This application controls the LED via Bluetooth.")
                        .setPositiveButton("Ok", null)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void connect() {
        if (bluetoothManager == null) {
            bluetoothManager = new BluetoothManager(this) {
                @Override
                public void addBluetoothDevice(BluetoothDevice bluetoothDevice) {
                    startForBluetooth(bluetoothDevice);
                    destroy();
                    bluetoothManager = null;
                }
            };
        } else {
            bluetoothManager.initializeBluetooth();
        }
        if (!bluetoothManager.isEnabled()) {
            Snack("Not supported on device.");
        } else if (bluetoothManager.bluetoothPermissionIsFailed()) {
            Snack("Please confirm requests.");
        } else {
            bluetoothManager.startSearching();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.speak).setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'OFF' or 'ON' to control the Arduino");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_CODE);
            } else {
                Snack("Your Device Don't Support Speech Input");
            }
        });

        status = findViewById(R.id.status);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (bluetoothManager != null && bluetoothManager.handelActivityResult(requestCode, resultCode)) {
            return;
        }
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String code = result.get(0).toLowerCase();
                if (code.equals("off") || code.equals("on")) {
                    if (bluetoothConnectionController == null) {
                        Snack("Reconnect");
                        hasConnection = false;
                        return;
                    }
                    if (code.equals("on")) {
                        bluetoothConnectionController.write(1);
                    } else {
                        bluetoothConnectionController.write(0);
                    }
                } else {
                    Snack("just say 'off' or 'on'.");
                }
            }
        }
    }

    private void Snack(String message) {
        Snackbar.make(findViewById(R.id.root), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (bluetoothManager != null) {
            bluetoothManager.handelPermissionsResult(requestCode, grantResults); // bool
        }
    }

    @Override
    protected void onDestroy() {
        destroyBluetoothController();
        super.onDestroy();
    }

    private void startForBluetooth(BluetoothDevice device) {
        bluetoothConnectionController = new BluetoothConnectionController(this);
        bluetoothConnectionController.connect(device);
    }

    private void destroyBluetoothController() {
        if (bluetoothManager != null) {
            bluetoothManager.destroy();
            bluetoothManager = null;
        }
        if (bluetoothConnectionController != null) {
            bluetoothConnectionController.destroy();
            bluetoothConnectionController = null;
        }
        hasConnection = false;
    }

    @Override
    public void bluetoothConnected(String name) {
        Snack("Connected to '" + name + "'");
        status.setText("Connected to '" + name + "'");
        status.setBackgroundColor(0xff4CAF50);
        hasConnection = true;
    }

    @Override
    public void bluetoothDisconnect() {
        hasConnection = false;
        status.setText("No connection");
        status.setBackgroundColor(0xffE91E63);
    }

    @Override
    public void bluetoothConnecting() {
        Snack("connecting...");
    }

    @Override
    public void bluetoothConnectionFailed() {
        Snack("connection failed");
    }

    @Override
    public void bluetoothDataTransfer(byte[] buffer) {

    }

}