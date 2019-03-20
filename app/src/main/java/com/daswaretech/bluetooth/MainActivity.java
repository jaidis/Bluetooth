package com.daswaretech.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter btAdapter;

    private Map<String,String> listado = new HashMap<>();
    private Integer contador = 0;
    private TextView textoVista;

    // A listiner if the advertising failure
    private BroadcastReceiver advertisingFailureReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init advertising failure listener
        advertisingFailure(this);

        // Movil del demonio: Android 8.0 Bluetooth 4.2
        // https://www.kimovil.com/es/donde-comprar-samsung-galaxy-j6-2018

        //CHECK ACCESS_FINE_LOCATION permission

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }

        // Devices with a display should not go to sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Init Bluetooth adapter
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btAdapter = mBluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (btAdapter == null) {
            Toast.makeText(this, "Not bluetooth device supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        else{
            if (btAdapter.isEnabled())
                comprobarBluetooth();
            else{
                //Turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1:
                if(resultCode == RESULT_OK){
                    comprobarBluetooth();
                }
                else{
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_LONG).show();
                    finish();
                }
            default:
                super.onActivityResult(requestCode,resultCode,data);
        }
    }

    protected void advertisingFailure(final Activity mainActivity){
        advertisingFailureReceiver = new BroadcastReceiver() {

            /**
             * Receives Advertising error codes from {@code AdvertiserService} and displays error messages
             * to the user. Sets the advertising toggle to 'false.'
             */
            @Override
            public void onReceive(Context context, Intent intent) {

                int errorCode = intent.getIntExtra(AdvertiserService.ADVERTISING_FAILED_EXTRA_CODE, -1);

                String errorMessage = getString(R.string.start_error_prefix);
                switch (errorCode) {
                    case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                        errorMessage += " " + getString(R.string.start_error_already_started);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                        errorMessage += " " + getString(R.string.start_error_too_large);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage += " " + getString(R.string.start_error_unsupported);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                        errorMessage += " " + getString(R.string.start_error_internal);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        errorMessage += " " + getString(R.string.start_error_too_many);
                        break;
                    case AdvertiserService.ADVERTISING_TIMED_OUT:
                        errorMessage = " " + getString(R.string.advertising_timedout);
                        break;
                    default:
                        errorMessage += " " + getString(R.string.start_error_unknown);
                }

                Toast.makeText(mainActivity, errorMessage, Toast.LENGTH_LONG).show();
            }
        };
    }

    private void comprobarBluetooth(){
        contador = 0;

        // Recovery information
        contador++;
        listado.put(contador.toString(), "Nombre dispositivo: "+btAdapter.getName());

        // Recovery real Mac Address from the Device
        contador++;
        String macAddress2 = Settings.Secure.getString(this.getContentResolver(), "bluetooth_address");
        listado.put(contador.toString(), "Dirección MAC: "+macAddress2);

        // Recovery a list of paired devices
        Set<BluetoothDevice> listVinculados =  btAdapter.getBondedDevices();

        for (BluetoothDevice dispositivo : listVinculados) {
            Map<String,String> listadoTemp = new HashMap<>();

            listadoTemp.put("Nombre dispositivo", dispositivo.getName());
            listadoTemp.put("Direccion Mac", dispositivo.getAddress());
            listadoTemp.put("Codigo Bluetooth", dispositivo.getBluetoothClass().toString());
            contador++;
            listado.put(contador.toString(), "Dispositivo vinculado: " +listadoTemp);
        }

        //Checking the default functions from Bluetooth Adapter

        contador++;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            listado.put(contador.toString(), "LE 2M PHY feature is supported: "+btAdapter.isLe2MPhySupported());
        }else{
            listado.put(contador.toString(), "LE 2M PHY feature not supported: ");
        }

        contador++;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            listado.put(contador.toString(), "LE Coded PHY feature is supported: "+btAdapter.isLeCodedPhySupported());
        }else{
            listado.put(contador.toString(), "LE Coded PHY feature not supported");
        }

        contador++;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            listado.put(contador.toString(), "LE Extended Advertising feature is supported:"+btAdapter.isLeExtendedAdvertisingSupported());
        }else{
            listado.put(contador.toString(), "LE Extended Advertising feature not supported");
        }

        contador++;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            listado.put(contador.toString(),"LE Periodic Advertising feature is supported: "+btAdapter.isLePeriodicAdvertisingSupported());
        }else{
            listado.put(contador.toString(), "LE Periodic Advertising feature not supported");
        }

        contador++;
        listado.put(contador.toString(),"Multi advertisement is supported: "+btAdapter.isMultipleAdvertisementSupported());

        //JSONObject json = new JSONObject(listado);
        textoVista = (TextView) findViewById(R.id.textoDefecto);
        Map<String, String> treeMap = new TreeMap<String, String>(listado);
        textoVista.setText(treeMap.toString());

        //JSONObject json = new JSONObject(treeMap);
        //Log.d("DASWARE-DEBUG", "onCreate: "+json.toString());
        //textoVista.setText(json.toString());
    }
    private void sendAdvertising(){



    }
}
