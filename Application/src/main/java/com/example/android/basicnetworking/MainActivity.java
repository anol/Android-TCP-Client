/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.basicnetworking;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogFragment;
import com.example.android.common.logger.LogWrapper;
import com.example.android.common.logger.MessageOnlyLogFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends FragmentActivity {

    public static final String TAG = "Basic Network Demo";
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;

    private static final String HOST_ADDR = "192.168.1.226";
    private static final int PORT_NUMBER = 8013;

    private Socket mySocket = null;
    private int iTestCounter = 0;

    // Reference to the fragment showing events, so we can clear it with a button
    // as necessary.
    private LogFragment mLogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);

        // Initialize text fragment that displays intro text.
        SimpleTextFragment introFragment = (SimpleTextFragment)
                getSupportFragmentManager().findFragmentById(R.id.intro_fragment);
        introFragment.setText(R.string.intro_message);
        introFragment.getTextView().setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);

        // Initialize the logging framework.
        initializeLogging();
        // Start receiver
        receiveMsg();
    }

    @Override
    protected void onDestroy() {
        if (null != mySocket) {
            try {
                mySocket.close();
            } catch (IOException e) {
                mySocket = null;
                Log.i(TAG, "Tx " + e.getMessage());
                e.printStackTrace();
            }
            mySocket = null;
            Log.i(TAG, "Disconnected");
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (null != mySocket) {
            try {
                mySocket.close();
                mySocket = null;
            } catch (IOException e) {
                mySocket = null;
                Log.i(TAG, "Tx " + e.getMessage());
                e.printStackTrace();
            }
            Log.i(TAG, "Disconnected");
        }
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // When the user clicks SEND ...
            case R.id.send_action:
                String str = "Test nr " + Integer.toString(iTestCounter++);
                sendMessageToServer(str);
                return true;
            // When the user clicks TEST, display the connection status.
            case R.id.test_action:
                checkNetworkConnection();
                return true;
            // Clear the log view fragment.
            case R.id.clear_action:
                mLogFragment.getLogView().setText("");
                return true;
            case R.id.settings_action:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.quit_action:
                return false;
        }
        return false;
    }

    public void sendMessageToServer(String str) {
        final String msg = str;
        new Thread(new Runnable() {
            @Override
            public void run() {
                PrintWriter out;
                try {
                    if (null == mySocket) {
//                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
//                        String hostAddress = sharedPref.getString("hostAddress", "");
                        mySocket = new Socket(HOST_ADDR, PORT_NUMBER);
                        Log.i(TAG, "Tx connected");
                    }
                    out = new PrintWriter(mySocket.getOutputStream());
                    out.println(msg);
                    out.flush();
                } catch (UnknownHostException e) {
                    Log.i(TAG, "Tx " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.i(TAG, "Tx " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void receiveMsg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader in = null;
                try {
//                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
//                    String hostAddress = sharedPref.getString("hostAddress", "");
                    mySocket = new Socket(HOST_ADDR, PORT_NUMBER);
                    Log.i(TAG, "Rx connected");
                } catch (UnknownHostException e) {
                    mySocket = null;
                    Log.i(TAG, "Rx " + e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    mySocket = null;
                    Log.i(TAG, "Rx " + e.getMessage());
                    e.printStackTrace();
                }
                Log.i(TAG, "Rx connected");
                try {
                    in = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
                } catch (IOException e) {
                    Log.i(TAG, "Rx " + e.getMessage());
                    e.printStackTrace();
                }
                while (true) {
                    String msg = null;
                    try {
                        msg = in.readLine();
                    } catch (IOException e) {
                        Log.i(TAG, "Rx " + e.getMessage());
                        e.printStackTrace();
                    }
                    if (msg == null) {
                        Log.i(TAG, "Rx BREAK");
                        break;
                    } else {
                        Log.i(TAG, msg);
                    }
                }
            }
        }).start();
    }

    /**
     * Check whether the device is connected, and if so, whether the connection
     * is wifi or mobile (it could be something else).
     */
    private void checkNetworkConnection() {
        // BEGIN_INCLUDE(connect)
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            Log.i(TAG, "NetworkInfo=" + activeInfo.toString());
            if (wifiConnected) {
                Log.i(TAG, getString(R.string.wifi_connection));
            } else if (mobileConnected) {
                Log.i(TAG, getString(R.string.mobile_connection));
            }
        } else {
            Log.i(TAG, getString(R.string.no_wifi_or_mobile));
        }
        // END_INCLUDE(connect)
    }

    /**
     * Create a chain of targets that will receive log data
     */
    public void initializeLogging() {

        // Using Log, front-end to the logging chain, emulates
        // android.util.log method signatures.

        // Wraps Android's native log framework
        LogWrapper logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);

        // A filter that strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        mLogFragment =
                (LogFragment) getSupportFragmentManager().findFragmentById(R.id.log_fragment);
        msgFilter.setNext(mLogFragment.getLogView());
    }
}
