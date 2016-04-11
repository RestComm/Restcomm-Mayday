
/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.telestax.mayday_customer.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.telestax.mayday_customer.R;
import com.telestax.mayday_customer.utils.Constant;

import java.util.HashMap;

import timer.com.maydaysdk.MayDayRegister;

public class LoginActivity extends AppCompatActivity implements MayDayRegister.MayDayRegisterInterface {
    private static final int TYPE_NOT_CONNECTED = 0;
    private static final int TYPE_WIFI = 1;
    private static final int TYPE_MOBILE = 2;
    private EditText mEditTextUsername, mEditTextViewPassword;
    private HashMap<String, Object> mParams;
    private ProgressDialog mProgressDialog;
    private AlertDialog mAlertDialog;
    private String mDomainAddress = "", mAgentName = "";
    private Context mContext;
    private MayDayRegister mMayDayRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI
        Button buttonSignIn = (Button) findViewById(R.id.login_buttonSignIn);
        ImageView imageViewSetting = (ImageView) findViewById(R.id.login_setting);
        mEditTextUsername = (EditText) findViewById(R.id.editText_login_user);
        mEditTextViewPassword = (EditText) findViewById(R.id.editText_login_password);
        mAlertDialog = new AlertDialog.Builder(this).create();
        mContext = getApplicationContext();
        mMayDayRegister = new MayDayRegister();
        mMayDayRegister.setListener(this);

        //Initialize RCClient for remote connection
        mMayDayRegister.initialize(getApplicationContext());

        // Saving the domain ip and agent name in SharedPreferences
        SharedPreferences prefShare = getSharedPreferences(Constant.MY_PREFS_NAME, MODE_PRIVATE);
        String restoredText = prefShare.getString(Constant.DOMAIN, null);
        if (restoredText != null) {
            mDomainAddress = prefShare.getString(Constant.DOMAIN, null);
            mAgentName = prefShare.getString(Constant.AGENT_NAME, null);
        }

        // Login sign action
        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditTextUsername.length() == 0) {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.username_credential), Toast.LENGTH_LONG).show();
                } else if (mEditTextViewPassword.length() == 0) {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.password_credential), Toast.LENGTH_LONG).show();
                } else if (mDomainAddress.length() == 0) {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.domain), Toast.LENGTH_LONG).show();
                } else if (mAgentName.length() == 0) {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.agent_name), Toast.LENGTH_LONG).show();
                } else {
                    if (!hasNoInternetConnection()) {
                        mParams = new HashMap<>();
                        mParams.put("pref_proxy_domain", "sip:" + mDomainAddress);
                        mParams.put("pref_sip_user", mEditTextUsername.getText().toString().trim());
                        mParams.put("pref_sip_password", mEditTextViewPassword.getText().toString().trim());
                        mMayDayRegister.createDevice(mParams, getApplicationContext(), MainActivity.class);
                        mProgressDialog = new ProgressDialog(LoginActivity.this);
                        mProgressDialog.setMessage(getResources().getString(R.string.please_wait));
                        mProgressDialog.show();
                    } else {
                        Toast.makeText(LoginActivity.this, getResources().getString(R.string
                                .enable_internet), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        //alert dialog for the domain and agent name
        imageViewSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String POPUP_LOGIN_TITLE = getResources().getString(R.string.setting);
                String DOMAIN_HINT = getResources().getString(R.string.domain_name);
                String AGENT_HINT = getResources().getString(R.string.agent);
                AlertDialog.Builder alert = new AlertDialog.Builder(LoginActivity.this);

                alert.setTitle(POPUP_LOGIN_TITLE);

                // Set an EditText view to get user input
                final EditText editDomain = new EditText(LoginActivity.this);
                editDomain.setHint(DOMAIN_HINT);
                final EditText editAgentName = new EditText(LoginActivity.this);
                editAgentName.setHint(AGENT_HINT);
                LinearLayout layout = new LinearLayout(getApplicationContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.addView(editDomain);
                layout.addView(editAgentName);
                alert.setView(layout);
                SharedPreferences prefShare = getSharedPreferences(Constant.MY_PREFS_NAME, MODE_PRIVATE);
                String restoredText = prefShare.getString(Constant.DOMAIN, null);
                if (restoredText != null) {
                    mDomainAddress = prefShare.getString(Constant.DOMAIN, null);
                    mAgentName = prefShare.getString(Constant.AGENT_NAME, null);
                }

                if (mDomainAddress != null) {
                    editDomain.setText(mDomainAddress);
                }

                if (mAgentName != null) {
                    editAgentName.setText(mAgentName);
                }
                alert.setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mDomainAddress = editDomain.getText().toString();
                        mAgentName = editAgentName.getText().toString();
                        SharedPreferences.Editor editor = getSharedPreferences(Constant.MY_PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putString(Constant.DOMAIN, editDomain.getText().toString().trim());
                        editor.putString(Constant.AGENT_NAME, editAgentName.getText().toString().trim());
                        editor.apply();
                    }
                });

                alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

                alert.show();

            }
        });
    }

    private void showAlert() {

        if (!LoginActivity.this.isFinishing()) {
            mAlertDialog.setTitle(getResources().getString(R.string.error));
            mAlertDialog.setMessage(getResources().getString(R.string.register_failed));
            mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    MayDayRegister.mayDayShutDown();
                    finish();
                }
            });
            mAlertDialog.show();
        }
    }

    @Override
    public void onBackPressed() {
        // Clear RCClient connection
        MayDayRegister.mayDayShutDown();
        finish();
    }

    // Return if network connection available or not
    private boolean hasNoInternetConnection() {

        int connectivityStatus = getConnectivityStatus();
        return connectivityStatus == TYPE_NOT_CONNECTED;
    }

    // Get network status
    private int getConnectivityStatus() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context
                .CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return TYPE_WIFI;
            }

            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return TYPE_MOBILE;
            }
        }
        return TYPE_NOT_CONNECTED;
    }

    @Override
    public void onRegisterAction(boolean status) {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        if (status) {
            //Register success navigate to MainActivity
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        } else {
            //Register fail display error message
            showAlert();
        }
    }
}
