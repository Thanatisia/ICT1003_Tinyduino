
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.nordicsemi.nrfUARTv2;




import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;


import com.nordicsemi.nrfUARTv2.UartService;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.nordicsemi.nrfUARTv2.DeviceListActivity.DeviceAdapter;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    public static final String accepted_bluetooth_name = "LOCK"; /* Tinyduino Device name */
//    public static final String accepted_bluetooth_ID = "DA:AB:1D:0E:AF:00"; /* Tinyduino Device's MAC Address */
    public static final String accepted_bluetooth_ID = "D1:DA:3B:AE:32:A7";
    public static String global_receiver_message = "";    /* Received message from Tinyduino Bluetooth */
    public static String global_target_bluetooth_ID = ""; /* Connected Tinyduino Bluetooth ID */
    public static String global_target_bluetooth_name = ""; /* Connected Tinyduino Bluetooth name */
    public boolean UART_SUPPORT_TOKEN = true; /* Token for checking */

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect,btnSend;
    private EditText edtMessage;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        messageListView = (ListView) findViewById(R.id.listMessage);
        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);
        btnConnectDisconnect=(Button) findViewById(R.id.btn_select);
        btnSend=(Button) findViewById(R.id.sendButton);
        edtMessage = (EditText) findViewById(R.id.sendText);
        service_init();

        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                	if (btnConnectDisconnect.getText().equals("Connect")){
                		
                		//Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                		
            			Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
            			startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
        			} else {
        				//Disconnect button pressed
        				if (mDevice!=null)
        				{
        					mService.disconnect();
        				}
        			}
                }
            }
        });
        /* Function to Send to BLE Tinyduino */
        // Handle Send button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	EditText editText = (EditText) findViewById(R.id.sendText);
            	String message = editText.getText().toString();
            	byte[] value;
				try {
					//send data to service
					value = message.getBytes("UTF-8");
					mService.writeRXCharacteristic(value);
					//Update the log with time stamp
					String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
					listAdapter.add("["+currentDateTimeString+"] TX: "+ message);
               	 	messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
               	 	edtMessage.setText("");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
            }
        });
        // Set initial UI state
    }
    
    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
        		mService = ((UartService.LocalBinder) rawBinder).getService();
        		Log.d(TAG, "onServiceConnected mService= " + mService);
        		if (!mService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }

        }

        public void onServiceDisconnected(ComponentName classname) {
       ////     mService.disconnect(mDevice);
        		mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        
        //Handler events that received from UART service 
        public void handleMessage(Message msg) {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        /* Function to receive from BLE Tinyduino */
        public void onReceive(Context context, Intent intent) {
            /*
                When information is received
                - Retrieve Actions
                    - i.e.
                        - intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
                        - intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
                        - intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
                        - intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
                        - intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
             */
            String action = intent.getAction();

            final Intent mIntent = intent;
           //*********************//
            /* After connecting to selected Bluetooth device - Apply appropriate action */
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
            	 runOnUiThread(new Runnable() {
                     public void run() {
                         	String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             Log.d(TAG, "UART_CONNECT_MSG");
                             btnConnectDisconnect.setText("Disconnect");
                             edtMessage.setEnabled(true);
                             btnSend.setEnabled(true);
                             ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                             global_target_bluetooth_name = mDevice.getName(); /* Retrieve connected device's bluetooth name */
                             showMessage("DEBUG:" + "\n" + "Connected Bluetooth ID:" + " " + global_target_bluetooth_ID + "\n" + "Connected Bluetooth Name:" + " " + global_target_bluetooth_name); //DEBUG: Testing output
                             listAdapter.add("["+currentDateTimeString+"] Connected to: "+ mDevice.getName());
                             messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                             mState = UART_PROFILE_CONNECTED;
                     }
            	 });
            }

          //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
            	 runOnUiThread(new Runnable() {
                     public void run() {
                    	 	 String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             Log.d(TAG, "UART_DISCONNECT_MSG");
                             btnConnectDisconnect.setText("Connect");
                             edtMessage.setEnabled(false);
                             btnSend.setEnabled(false);
                             ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
//                             listAdapter.add("["+currentDateTimeString+"] Disconnected to: "+ mDevice.getName());
                             listAdapter.add("["+currentDateTimeString+"] Disconnected from: "+ mDevice.getName());
                             mState = UART_PROFILE_DISCONNECTED;
                             mService.close();
                            //setUiState();

                     }
                 });
            }


          //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
             	 mService.enableTXNotification();
            }
          //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                 final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                 runOnUiThread(new Runnable() {
                     public void run() {
                         try {
                         	String text = new String(txValue, "UTF-8");
                         	if(text != null && !text.isEmpty() && !text.equals("null"))
                            {
                                global_receiver_message = text;
                            }
                         	/* Function/Action : If received message is '1' :
                         	Disconnect from current bluetooth
                         	Search for Bluetooth and
                         	Select
                         	*/

                             if(global_receiver_message.contains("1"))
                             {

                             }
//                             /* Disconnect from Watch */
//                             if (mDevice!=null)
//                             {
//                                 mService.disconnect();
//                                 mDevice = null;
//                             }
////                                 mState = UART_PROFILE_DISCONNECTED;
////                                 mService.disconnect();
//
//                             //Disconnect device
//                             //mService.disconnect();
////                                 if (mBtAdapter.isEnabled()) {
////                                     Log.i(TAG, "onResume - BT not enabled yet");
////                                     Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
////                                     startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
////                                 }
//
//                             //Connect to [Lock] Server device
//                             showMessage("Received 1 : Searching for bluetooth" + " " + "[" + accepted_bluetooth_ID + "]" + " " + "and connecting ");
//                             //global_receiver_message = "";
//                             //mService.connect(accepted_bluetooth_ID);
//
//                             /* Search for required Bluetooth ID */
//                             /* If connection to server is successful - Send message then disconnect */
//                             if(mDevice == null) {
//                                 mDevice = mBtAdapter.getRemoteDevice(global_target_bluetooth_ID);
//                                 showMessage("Device Address: " + mDevice.getAddress());
//                                 if (mService.connect(mDevice.getAddress())) {
//                                     try {
//                                         mState = UART_PROFILE_CONNECTED;
//                                         showMessage("Connected to:" + mDevice.getAddress());
//
//                                         //On Connect:
//                                         mService.writeRXCharacteristic("1".getBytes("UTF-8"));
//                                     } catch (Exception ex) {
//                                         showMessage(ex.getMessage().toString());
//                                         Log.e(TAG, "Exception [302]:" + ex.getMessage().toString());
//                                     }
//                                 }
//                                 else {
//                                     showMessage("Unable to connect to:" + mDevice.getAddress());
//                                     Log.e(TAG, "Unable to connect to:" + mDevice.getAddress() + "," + mDevice.getName());
//                                 }
//                             }
//                             else
//                             {
//                                 showMessage("Address : " + mDevice.getAddress() + "\n" + "Name : " + mDevice.getName());
//                             }

                             if(mDevice != null) {
                                 mService.disconnect();
                                 //mService = null;
                                 //mState = UART_PROFILE_DISCONNECTED;
                             }

                             String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             listAdapter.add("\n"); /* Add a new line */
                             listAdapter.add("["+currentDateTimeString+"] TX Acknowledgement: " + global_receiver_message);

                             mDevice = mBtAdapter.getRemoteDevice(accepted_bluetooth_ID);
                             //boolean connect_ret = mService.connect(accepted_bluetooth_ID);
                             boolean connect_ret = mService.connect(mDevice.getAddress());

                             if (connect_ret) /* If connected successfully */
                             {
                                 showMessage("Connection to [" + accepted_bluetooth_ID + "]" + ":" + "success");

                                 currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                                 listAdapter.add("["+currentDateTimeString+"] DEBUGGING: " + "CONNECTION_SUCCESS");

                                 //mState = UART_PROFILE_CONNECTED;

                                 //ERROR Device does not support UART: Sending message : mService.writeRXCharacteristic(("SMARTLOCK" + "_" + global_receiver_message).getBytes("UTF-8"));
                                 /* UPDATE 2019/11/25 0219 - Error [ This device doesn't support UART ] is produced from the following line
                                  * mService.writeRXCharacteristic(("SMARTLOCK" + "_" + global_receiver_message).getBytes("UTF-8"));
                                  *  -> if i remove,
                                  *  -> phone is able to redirect and connect to the server device, albeit on the second try - but able to connect
                                  * */

                                 String txMsg = "SMARTLOCK" + "_" + global_receiver_message;
                                 showMessage(txMsg);
                                 mService.writeRXCharacteristic(txMsg.getBytes("UTF-8"));
                                 currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                                 if (UART_SUPPORT_TOKEN)
                                 {
                                     listAdapter.add("[" + currentDateTimeString + "]" + " " + "[" + UART_SUPPORT_TOKEN + "]" + " " + ":" + " " + "Message Transmission Success!");
                                 }
                                 else
                                 {
                                     listAdapter.add("[" + currentDateTimeString + "]" + " " + "[" + UART_SUPPORT_TOKEN + "]" + " " + ":" + " " + "Message Transmission Failed!");
                                 }
//                                 try {
//
//                                 }
//                                 catch (Exception ex_transmission)
//                                 {
//                                     currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
//                                     listAdapter.add("["+currentDateTimeString+"] Unable to transmit message, error" + " " + "[" + ex_transmission.getMessage().toString() + "]");
//                                 }
                             }
                             else /* If connection failed */
                             {
                                 currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                                 showMessage("Connection to [" + accepted_bluetooth_ID + "]" + ":" + "failed");
                                 listAdapter.add("["+currentDateTimeString+"] DEBUGGING: " + "CONNECTION_FAILED");
                             }

//                            try {
//                             }
//                             catch (Exception ex)
//                             {
//                                 showMessage("Exception Met:" + ex.getMessage().toString());
//                                 listAdapter.add("[" + currentDateTimeString + "]" + "Connection to [" + accepted_bluetooth_ID + "]" + ":" + "error" + " " + "(" + ex.getMessage().toString() + ")");
//                                 Log.e(TAG, "Exception met:" + ex.getMessage().toString());
//                             }

                             if(mDevice != null) {
                                 mService.disconnect();
                                 //mState = UART_PROFILE_DISCONNECTED;
                             }

                             //showMessage("DEBUG:" + "\n" + global_receiver_message); /* DEBUG: Display retrieved message */
                         	 currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                             listAdapter.add("["+currentDateTimeString+"] RX: "+text);
                             messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                         } catch (Exception e) {
                             Log.e(TAG, e.toString());
                         }
                     }
                 });
             }
           //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                UART_SUPPORT_TOKEN = false;
            	showMessage("Device doesn't support UART. Disconnecting");
            	mService.disconnect();
            }
        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
  
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
    	 super.onDestroy();
        Log.d(TAG, "onDestroy()");
        
        try {
        	LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        } 
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
 
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

        case REQUEST_SELECT_DEVICE:
        	//When the DeviceListActivity return, with the selected device address
            if (resultCode == Activity.RESULT_OK && data != null) {
                String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                global_target_bluetooth_ID = deviceAddress; /* Retrieve connected device's bluetooth hex ID */
                mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
               
                Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                mService.connect(deviceAddress);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        default:
            Log.e(TAG, "wrong request code");
            break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
       
    }

    
    private void showMessage(String msg) {
        //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.popup_title)
            .setMessage(R.string.popup_message)
            .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
   	                finish();
                }
            })
            .setNegativeButton(R.string.popup_no, null)
            .show();
        }
    }
}
