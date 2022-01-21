//
//  MainActivity
//  ArmBot Android Application
//
//  Created by Anthony FERREYROLLES on 18/01/2022.
//  Copyright © 2022 ArmBot. All rights reserved.
//

package com.example.armbot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.armbot.databinding.ActivityMainBinding;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    //Variables declaration
    private AppBarConfiguration mAppBarConfiguration;
    private Context context;
    private static BluetoothAdapter bluetoothAdapter;
    private static ArmConnection armConnection;

    private final int LOCATION_PERMISSION_REQUEST = 101;
    private final int SELECT_DEVICE = 102;

    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";
    private String connectedDevice;
    public static boolean bluetoothEnabled = false;

    /*
        Used to handle the messages sent by the ArmConnection.java.
     */
    public final Handler handler = new Handler(new Handler.Callback() {
        @SuppressLint("HardwareIds")
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (message.arg1) {
                        case ArmConnection.STATE_NONE:
                            /*
                            Calls the "setState" function to writes a message bellow the app's title.
                             */
                            setState("No Connection");
                            break;
                        case ArmConnection.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case ArmConnection.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case ArmConnection.STATE_CONNECTED:
                            setState("Connected: " + connectedDevice);

                            /*
                            Once connected to the Raspberry, sends a success message to it.
                             */
                            String messageSent = "Connection succeeded";
                            armConnection.write(messageSent.getBytes());

                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                /*case MESSAGE_WRITE:
                    byte[] buffer1 = (byte[]) message.obj;
                    String outputBuffer = new String(buffer1);
                    adapterMainChat.add("Me: " + outputBuffer);
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[]) message.obj;
                    String inputBuffer = new String(buffer, 0, message.arg1);
                    adapterMainChat.add(connectedDevice + ": " + inputBuffer);
                    break;*/
                case MESSAGE_TOAST:
                    Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    /*
    Used to get the bluetooth adapter in the HomeFragment.java fragment
     */
    public static BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public void setState(CharSequence subTitle) {
        /*
        Writes a message bellow the title in the app.
        */
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(subTitle);
    }

    public static void SendAction(String action){
        /*
        Sends the chosen action to the Raspberry. This function is used in ControllerButtonsFragment.java.
         */
        armConnection.write(action.getBytes());
    }

    /*
    Function called when the app is launched. Sets the app.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        /*
        Sets the menu in app/res/menu/activity_main_drawer used in app/res/layout/activity_main.xml
         */
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_controller_buttons, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();

        /*
        NavController in the app/res/layout/content_main.xml.
         */
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        context = this;

        /*
        While the Bluetooth connection isn't established, we can't enter the app
         */
        try{
            findViewById(R.id.enter_button).setEnabled(false);
        } catch (Exception ignored){

        }

        // Calls the ArmConnection constructor and passes the handler.
        armConnection = new ArmConnection(handler);

        // Initiates the bluetooth connection.
        initBluetooth();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // The menu is located in app/res/menu/main.xml
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        /*
        NavController in the app/res/layout/content_main.xml.
         */
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        /*
        Sets an action when clicking on the icons on the top right hand side of the app
         */
        switch (item.getItemId()){
            case R.id.menu_search_devices:
                checkPermission();
                return true;
            case R.id.menu_enable_bluetooth:
                enableBluetooth();
                return true;
            case R.id.menu_home:
                Intent intent = new Intent(context, MainActivity.class);
                startActivityForResult(intent,SELECT_DEVICE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initBluetooth(){
        /*
        Gets the bluetooth adapter
         */
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        /*
        Sets a subtitle with the bluetooth state or sets a Toast notification if no bluetooth is found
         */
        if (bluetoothAdapter == null){
            Toast.makeText(context, "No Bluetooth found", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()){
                Objects.requireNonNull(getSupportActionBar()).setSubtitle("Bluetooth Disabled");
            } else {
                if(bluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) != BluetoothAdapter.STATE_CONNECTED){
                    Objects.requireNonNull(getSupportActionBar()).setSubtitle("Not Connected");
                } else {
                    /*
                    Connection established, we can enter the app
                    */
                    try{
                        findViewById(R.id.enter_button).setEnabled(true);
                    } catch (Exception ignored){

                    }

                    Objects.requireNonNull(getSupportActionBar()).setSubtitle("Connected: " + connectedDevice);
                }
            }
        }
    }

    /*
    Enables the bluetooth when the Enable Bluetooth button is clicked
    */
    private void enableBluetooth(){
        if (bluetoothAdapter.isEnabled()){
            Toast.makeText(context, "Bluetooth already enabled", Toast.LENGTH_SHORT).show();
        }
        else {
            bluetoothAdapter.enable();
            Toast.makeText(context, "Bluetooth enabled", Toast.LENGTH_SHORT).show();

            if (Objects.requireNonNull(getSupportActionBar()).getSubtitle() == "Bluetooth Disabled"){
                getSupportActionBar().setSubtitle("Not Connected");
            }
        }
    }

    /*
    Checks if the app has the ACCESS_FINE_LOCATION permissions
     */
    private void checkPermission(){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new  String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else {
            /*
            Starts the DeviceListActivity activity and waits for a result
             */
            Intent intent = new Intent(context, DeviceListActivity.class);
            startActivityForResult(intent, SELECT_DEVICE);
        }
    }

    /*
    When the permissions are requested by the checkPermission method, if granted,
    starts the DeviceListActivity activity and waits for a result or, if rejected,
    asks again, and if it is once more rejected, closes the app.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(context, DeviceListActivity.class);
                startActivityForResult(intent, SELECT_DEVICE);
            } else {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission is required.\n Please grant")
                        .setPositiveButton("Grant", (dialogInterface, i) -> checkPermission())
                        .setNegativeButton("Deny", (dialogInterface, i) -> MainActivity.this.finish()).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /*
    Method used by the DeviceListActivity activity by returning a result and data.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int SELECT_DEVICE = 102;

        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            /*
            Gets the device address sent by the DeviceListActivity activity
             */
            String address = data.getStringExtra("deviceAddress");

            /*
            Tries to connect to the device by calling the ArmConnection activity's connect method
             */
            armConnection.connect(bluetoothAdapter.getRemoteDevice(address));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*
        Stops the connection
         */
        if (armConnection != null) {
            armConnection.stop();
        }
    }
}