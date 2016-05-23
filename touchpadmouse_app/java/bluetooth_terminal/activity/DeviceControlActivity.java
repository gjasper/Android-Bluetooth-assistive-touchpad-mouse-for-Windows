package com.example.ghj.bluetooth_terminal.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.example.ghj.bluetooth_terminal.DeviceData;
import com.example.ghj.bluetooth_terminal.R;
import com.example.ghj.bluetooth_terminal.Utils;
import com.example.ghj.bluetooth_terminal.bluetooth.DeviceConnector;
import com.example.ghj.bluetooth_terminal.bluetooth.DeviceListActivity;

import java.lang.ref.WeakReference;

import static java.lang.Math.abs;
import static java.lang.Math.round;

public final class DeviceControlActivity extends BaseActivity {
    private static final String DEVICE_NAME = "DEVICE_NAME";

    private static String MSG_NOT_CONNECTED;
    private static String MSG_CONNECTING;
    private static String MSG_CONNECTED;

    private static int currentX;
    private static int currentY;

    int oldDeltaX = 0, oldDeltaY = 0;


    private static int sensibility = 5;

    private static DeviceConnector connector;
    private static BluetoothResponseHandler mHandler;

    private boolean hexMode, needClean;
    private String command_ending;
    private String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.settings_activity, false);

		sensibility = 5;

        if (mHandler == null) mHandler = new BluetoothResponseHandler(this);
        else mHandler.setTarget(this);

        MSG_NOT_CONNECTED = getString(R.string.msg_not_connected);
        MSG_CONNECTING = getString(R.string.msg_connecting);
        MSG_CONNECTED = getString(R.string.msg_connected);

        setContentView(R.layout.activity_terminal);
        if (isConnected() && (savedInstanceState != null)) {
            setDeviceName(savedInstanceState.getString(DEVICE_NAME));
        } else getSupportActionBar().setSubtitle(MSG_NOT_CONNECTED);

        View touchpadArea = findViewById(R.id.touchpadArea);
        touchpadArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent me) {

                String deltaMove = "";
                int deltaX, deltaY;

                switch(me.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        currentX = round(me.getX());
                        currentY = round(me.getY());

                        //Utils.log("ACTION DOWN cX,cY [:"+currentX+","+currentY+"]");

                        break;

                    case MotionEvent.ACTION_MOVE:
						deltaX = round(me.getX());
						deltaY = round(me.getY());

                        if(((abs(deltaX - oldDeltaX) < 5)) && ((abs(deltaY - oldDeltaY)) < 5)){
                            //Utils.log("MOVEMENT STOPPED ACTION MOVE cX,cY [:"+currentX+","+currentY+"]"+"dX,dY [:"+deltaX+","+deltaY+"]");

                            break;
                        }

                        oldDeltaX = deltaX;
                        oldDeltaY = deltaY;


                        //Utils.log("ACTION MOVE cX,cY [:"+currentX+","+currentY+"]"+"dX,dY [:"+deltaX+","+deltaY+"]");

                        deltaX = (currentX - deltaX)/sensibility;
                        deltaY = (currentY - deltaY)/sensibility;
                        deltaMove = deltaMove + String.format("%03d", deltaX) +":"+ String.format("%03d", deltaY);
                        sendCommand(deltaMove);



                        break;
                }

                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });

    }
    // ==========================================================================

    private boolean isConnected() {
        return (connector != null) && (connector.getState() == DeviceConnector.STATE_CONNECTED);
    }
    // ==========================================================================

    private void stopConnection() {
        if (connector != null) {
            connector.stop();
            connector = null;
            deviceName = null;
        }
    }
    // ==========================================================================

    private void startDeviceListActivity() {
        stopConnection();
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }
    // ============================================================================

    @Override
    public boolean onSearchRequested() {
        if (super.isAdapterReady()) startDeviceListActivity();
        return false;
    }
    // ==========================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.device_control_activity, menu);
        return true;
    }
    // ============================================================================

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_search:
                if (super.isAdapterReady()) {
                    if (isConnected()) stopConnection();
                    else startDeviceListActivity();
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                return true;

            case R.id.sensibility_1:
                sensibility = 7;

                return true;
            case R.id.sensibility_2:
                sensibility = 6;

                return true;
            case R.id.sensibility_3:
                sensibility = 5;

                return true;
            case R.id.sensibility_4:
                sensibility = 4;

               return true;
            case R.id.sensibility_5:
                sensibility = 3;

                return true;

            case R.id.menu_settings:
                final Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    // ============================================================================


    // ============================================================================

    @Override
    public void onStart() {
        super.onStart();

        // hex mode
        final String mode = Utils.getPrefence(this, getString(R.string.pref_commands_mode));
        this.hexMode = mode.equals("HEX");

        this.needClean = Utils.getBooleanPrefence(this, getString(R.string.pref_need_clean));
    }
    // ============================================================================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);
                    if (super.isAdapterReady() && (connector == null)) setupConnector(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                super.pendingRequestEnableBt = false;
                if (resultCode != Activity.RESULT_OK) {
                    Utils.log("BT not enabled");
                }
                break;
        }
    }
    // ==========================================================================

    private void setupConnector(BluetoothDevice connectedDevice) {
        stopConnection();
        try {
            String emptyName = getString(R.string.empty_device_name);
            DeviceData data = new DeviceData(connectedDevice, emptyName);
            connector = new DeviceConnector(data, mHandler);
            connector.connect();
        } catch (IllegalArgumentException e) {
            Utils.log("setupConnector failed: " + e.getMessage());
        }
    }
    // ==========================================================================

	public void leftButtonClick(View view) {
        sendCommand("mBTN_LT");
    }
    // ==========================================================================

    public void rightButtonClick(View view) {
        sendCommand("mBTN_RT");
    }
    // ==========================================================================

    public void sendCommand(String commandString) {

            // Дополнение команд в hex
            if (hexMode && (commandString.length() % 2 == 1)) {
                commandString = "0" + commandString;
            }
            byte[] command = (hexMode ? Utils.toHex(commandString) : commandString.getBytes());
            if (isConnected()) {
                connector.write(command);
            }

    }
    // ==========================================================================

    void appendLog(String message) {

        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();

    }
    // =========================================================================

    void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        getSupportActionBar().setSubtitle(deviceName);
    }
    // ==========================================================================

    private static class BluetoothResponseHandler extends Handler {
        private WeakReference<DeviceControlActivity> mActivity;

        public BluetoothResponseHandler(DeviceControlActivity activity) {
            mActivity = new WeakReference<DeviceControlActivity>(activity);
        }

        public void setTarget(DeviceControlActivity target) {
            mActivity.clear();
            mActivity = new WeakReference<DeviceControlActivity>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            DeviceControlActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:

                        Utils.log("MESSAGE_STATE_CHANGE: " + msg.arg1);
                        final ActionBar bar = activity.getSupportActionBar();
                        switch (msg.arg1) {
                            case DeviceConnector.STATE_CONNECTED:
                                bar.setSubtitle(MSG_CONNECTED);
                                break;
                            case DeviceConnector.STATE_CONNECTING:
                                bar.setSubtitle(MSG_CONNECTING);
                                break;
                            case DeviceConnector.STATE_NONE:
                                bar.setSubtitle(MSG_NOT_CONNECTED);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        final String readMessage = (String) msg.obj;
                        if (readMessage != null) {
                            activity.appendLog(readMessage);
                        }
                        break;

                    case MESSAGE_DEVICE_NAME:
                        activity.setDeviceName((String) msg.obj);
                        break;

                    case MESSAGE_WRITE:
                        // stub
                        break;

                    case MESSAGE_TOAST:
                        // stub
                        break;
                }
            }
        }
    }
    // ==========================================================================
}