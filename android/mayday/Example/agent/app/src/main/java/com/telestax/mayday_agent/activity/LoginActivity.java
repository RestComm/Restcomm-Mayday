
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
package com.telestax.mayday_agent.activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.telestax.mayday_agent.R;
import com.telestax.mayday_agent.utils.Constant;

import java.util.HashMap;

import timer.com.maydaysdk.MayDayRegister;

public class LoginActivity extends AppCompatActivity implements MayDayRegister.MayDayRegisterInterface {
    private EditText mEditViewUserName, mEditViewPassword;
    private HashMap<String, Object> mParams;
    private SharedPreferences mPrefs;
    private ProgressDialog mProgressDialog;
    private AlertDialog mAlertDialog;
    private String mSetting = "";
    private Context mContext;
    private static final int TYPE_NOT_CONNECTED = 0;
    private static final int TYPE_WIFI = 1;
    private static final int TYPE_MOBILE = 2;
    private SharedPreferences.Editor mLoginPrefsEditor;
    private MayDayRegister mMayDayRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent_login);

        // Initialize UI
        Button mButton_SignIn = (Button) findViewById(R.id.login_buttonSignIn);
        ImageView mImageView_Setting = (ImageView) findViewById(R.id.login_setting);
        final CheckBox saveLoginCheckBox = (CheckBox) findViewById(R.id.checkBox_remember);
        mEditViewUserName = (EditText) findViewById(R.id.login_editTextUser);
        mEditViewPassword = (EditText) findViewById(R.id.login_editTextPassword);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAlertDialog = new AlertDialog.Builder(this).create();
        mContext = getApplicationContext();

        SharedPreferences loginPreferences = getSharedPreferences(Constant.MY_LOGIN_PREFS, MODE_PRIVATE);
        mLoginPrefsEditor = loginPreferences.edit();

        Boolean saveLogin = loginPreferences.getBoolean(Constant.SAVE_LOGIN, false);
        if (saveLogin) {
            mEditViewUserName.setText(loginPreferences.getString(Constant.USERNAME, ""));
            mEditViewPassword.setText(loginPreferences.getString(Constant.PASSWORD, ""));
            saveLoginCheckBox.setChecked(true);
        }

        mMayDayRegister = new MayDayRegister();
        mMayDayRegister.setListener(this);

        //Initialize RCClient for remote connection
        mMayDayRegister.initialize(getApplicationContext());

        // Saving the domain ip address in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(Constant.MY_PREFS_NAME, MODE_PRIVATE);
        String restoredDomainAddress = sharedPreferences.getString(Constant.DOMAIN, null);
        if (restoredDomainAddress != null) {
            mSetting = sharedPreferences.getString(Constant.DOMAIN, "No name defined");//"No name defined" is the default value.
        }

        // Login sign action
        mButton_SignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditViewUserName.length() == 0) {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.username_credential), Toast.LENGTH_LONG).show();
                } else if (mEditViewPassword.length() == 0) {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.password_credential), Toast.LENGTH_LONG).show();
                } else if (mSetting.length() == 0) {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.domain_address), Toast.LENGTH_LONG).show();
                } else {
                    if (hasNoInternetConnection()) {

                        if (saveLoginCheckBox.isChecked()) {
                            mLoginPrefsEditor.putBoolean(Constant.SAVE_LOGIN, true);
                            mLoginPrefsEditor.putString(Constant.USERNAME, mEditViewUserName.getText().toString().trim());
                            mLoginPrefsEditor.putString(Constant.PASSWORD, mEditViewPassword.getText().toString().trim());
                            mLoginPrefsEditor.apply();
                        } else {
                            mLoginPrefsEditor.clear();
                            mLoginPrefsEditor.apply();
                        }
                        mParams = new HashMap<>();
                        mParams.put("pref_proxy_domain", mPrefs.getString("pref_proxy_domain", "sip:" + mSetting));
                        mParams.put("pref_sip_user", mPrefs.getString("pref_sip_user", mEditViewUserName.getText().toString().trim()));
                        mParams.put("pref_sip_password", mPrefs.getString("pref_sip_password", mEditViewPassword.getText().toString().trim()));
                        // Register the device for video and chat incoming data
                        mMayDayRegister.createDevice(mParams, getApplicationContext(), AgentCallActivity.class);
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

        //alert dialog for the domain name
        mImageView_Setting.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                AlertDialog.Builder alert = new AlertDialog.Builder(LoginActivity.this);
                final EditText editText = new EditText(LoginActivity.this);
                alert.setTitle(getResources().getString(R.string.domain));
                alert.setView(editText);

                if (mSetting != null) {
                    editText.setText(mSetting);
                }
                alert.setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        mSetting = editText.getText().toString();
                        SharedPreferences.Editor editor = getSharedPreferences(Constant.MY_PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putString(Constant.DOMAIN, mSetting);
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

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void showAlert() {

        if (!LoginActivity.this.isFinishing()) {
            mAlertDialog.setTitle(getResources().getString(R.string.error));
            mAlertDialog.setMessage(getResources().getString(R.string.register_failed));
            mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
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
        // Shutdown MayDay connection
        MayDayRegister.mayDayShutDown();
        finish();
    }

    private boolean hasNoInternetConnection() {

        int connectivityStatus = getConnectivityStatus();
        return connectivityStatus != TYPE_NOT_CONNECTED;
    }

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
            Intent i = new Intent(LoginActivity.this, AgentDetailsActivity.class);
            startActivity(i);
            finish();
        } else {
            //Register fail display error message
            showAlert();
        }
    }

}
