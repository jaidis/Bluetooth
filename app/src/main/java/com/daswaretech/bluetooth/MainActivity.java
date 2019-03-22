package com.daswaretech.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter btAdapter;
    private ArrayList<BluetoothDevice> mLeDevices;
    private ArrayList<String> infoFinal;
    private BluetoothLeScanner btScanner;
    private ScanCallback mScanCallback;

    private boolean estado = false;
    private Map<String,String> listado = new HashMap<>();
    private Integer contador = 0;
    private TextView textoVista;

    // A listener if the advertising failure
    private BroadcastReceiver advertisingFailureReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        saveLog();

        // Init advertising failure listener
        advertisingFailure(this);

        // Link textview from the view
        textoVista = (TextView) findViewById(R.id.textoDefecto);

        // Movil del demonio: Android 8.0 Bluetooth 4.2
        // https://www.kimovil.com/es/donde-comprar-samsung-galaxy-j6-2018

        // CHECK ACCESS_FINE_LOCATION permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            estado  = true;
        }


        // CHECK WRITE_EXTERNAL_STORAGE permission
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            estado  = true;
        }

        if (estado){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
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
            //finish();
            return;
        }
        else{
            if (btAdapter.isEnabled()){
                callbackInit();
                lanzarMensaje("Dispositivo preparado");
            }
            else{
                //Turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void lanzarMensaje(String message){
        Snackbar.make(findViewById(android.R.id.content).getRootView(),message,Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1:
                if(resultCode == RESULT_OK){
                    callbackInit();
                    lanzarMensaje("Dispositivo preparado");
                }
                else{
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_LONG).show();
                    finish();
                }
            default:
                super.onActivityResult(requestCode,resultCode,data);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        saveLog();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        saveLog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        saveLog();
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

    // Init the callback and manage the result from bluetooth scanner
    private void callbackInit(){
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if(!mLeDevices.contains(result.getDevice())) {
                    mLeDevices.add(result.getDevice());
                    //Log.d("DASDEBUG", result.getDevice().toString());
                }
                Log.d("DASDEBUG", mLeDevices.toString());
                textoVista.setText(mLeDevices.toString());
            }
        };
    }

    // Show bluetooth name, "real" mac address, paired devices and standard functions from bluetooth class
    public void mostrarInfo(View v){
        contador = 0;

        // Recovery information
        contador++;
        listado.put(contador.toString(), "Nombre dispositivo: "+btAdapter.getName());

        // Recovery real Mac Address from the Device
        contador++;
        String macAddress2 = Settings.Secure.getString(this.getContentResolver(), "bluetooth_address");
        listado.put(contador.toString(), "Dirección MAC: "+macAddress2);

        //Check if device are a Bluetooth Classic or BLE
        contador++;
        listado.put(contador.toString(), "Bluetooth Classic: " + getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH));

        contador++;
        listado.put(contador.toString(), "Bluetooth Low Energy: " + getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));

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
        try{
            contador++;
            listado.put(contador.toString(),"Multi advertisement is supported: "+btAdapter.isMultipleAdvertisementSupported());
        }catch (Exception e)
        {
            listado.put(contador.toString(),"Multi advertisement not supported ");
        }

        try {
            contador++;
            listado.put(contador.toString(), "Chipset supports on-chip filtering: " + btAdapter.isOffloadedFilteringSupported());
        }catch (Exception e){
            listado.put(contador.toString(),"Chipset on-chip filtering not supported ");
        }

        try {
            contador++;
            listado.put(contador.toString(), "Chipset supports on-chip scan batching: " + btAdapter.isOffloadedScanBatchingSupported ());
        }catch (Exception e){
            listado.put(contador.toString(),"Chipset on-chip scan batching not supported ");
        }

        //JSONObject json = new JSONObject(listado);

        Map<String, String> treeMap = new TreeMap<String, String>(listado);
        textoVista.setText(treeMap.toString());


        JSONObject json = new JSONObject(treeMap);
        Log.d("DASDEBUG", "onCreate: "+json.toString());
        //textoVista.setText(json.toString());

        infoFinal = new ArrayList<String>();

        for (Map.Entry<String,String> entry : treeMap.entrySet()) {
            //Log.d("DASDEBUG", entry.getKey() + "/" + entry.getValue());
            infoFinal.add(entry.getValue());
        }

        //Log.d("DASDEBUG", "mostrarInfo: "+infoFinal.size());

        RecyclerView rv =(RecyclerView) findViewById(R.id.showInfo);
        rv.setLayoutManager((RecyclerView.LayoutManager)(new LinearLayoutManager(getApplicationContext())));
        rv.setLayoutManager((RecyclerView.LayoutManager)(new GridLayoutManager(getApplicationContext(),1)));
        InfoAdapter l = new InfoAdapter(infoFinal,getApplicationContext());
        rv.setAdapter(l);

    }

    // Start Bluetooth LE scan with default parameters and no filters.
    public void btLanzarScanner(View v){
        //startActivity(new Intent(MainActivity.this, Scanner.class));

        mLeDevices = new ArrayList<BluetoothDevice>();
        btScanner = btAdapter.getBluetoothLeScanner();
        btScanner.startScan(mScanCallback);
        textoVista.setText(R.string.loading_text);
    }

    // Stops an ongoing Bluetooth LE scan.
    public void btPararScanner(View v){
        try{
            btScanner.stopScan(mScanCallback);
            textoVista.setText(mLeDevices.toString());
        }catch (Exception e) {
            Log.d("DASDEBUG", "No se ha iniciado el escaner, no se puede parar");
        }
    }

    /**
     * Returns Intent addressed to the {@code AdvertiserService} class.
     */
    private static Intent getServiceIntent(Context c) {
        return new Intent(c, AdvertiserService.class);
    }

    /**
     * Function that start the advertising service
     * @param v
     */
    public void sendAdvertising(View v){
        Context c = this;
        c.startService(getServiceIntent(c));
        textoVista.setText("Servicio Iniciado");
    }

    /**
     * Function that stop the advertising service
     * @param v
     */
    public void stopAdvertising(View v){
        Context c = this;
        c.stopService(getServiceIntent(c));
        textoVista.setText("Servicio Parado");
    }

    /**
     * Checks if external storage is available for read and write
     */

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ) {
            return true;
        }
        return false;
    }

    /**
     * Checks if external storage is available to at least read
     * @return
     */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals( state ) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals( state ) ) {
            return true;
        }
        return false;
    }

    /**
     * Function that allow save the current log
     */
    private void saveLog(){
        if ( isExternalStorageWritable() ) {

            File appDirectory = new File( Environment.getExternalStorageDirectory() + "/DaswareDebug" );
            File logDirectory = new File( appDirectory + "/log" );
            File logFile = new File( logDirectory, "logcat" + System.currentTimeMillis() + ".txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            // create log folder
            if ( !logDirectory.exists() ) {
                logDirectory.mkdir();
            }

            // clear the previous logcat and then write the new one to the file
            try {
                //process = Runtime.getRuntime().exec("logcat -c");
                Process process = Runtime.getRuntime().exec("logcat -f " + logFile);
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } else if ( isExternalStorageReadable() ) {
            Toast.makeText(this, "Sin permisos de escritura", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "No es posible acceder a los directorios", Toast.LENGTH_LONG).show();
        }
    }

    public void finishApp(View v){
        Toast.makeText(this, "Finish application", Toast.LENGTH_LONG).show();
        finish();
    }
}
