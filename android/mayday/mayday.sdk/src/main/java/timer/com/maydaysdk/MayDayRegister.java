
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
package timer.com.maydaysdk;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.mobicents.restcomm.android.client.sdk.RCClient;
import org.mobicents.restcomm.android.client.sdk.RCDevice;
import org.mobicents.restcomm.android.client.sdk.RCDeviceListener;
import org.mobicents.restcomm.android.client.sdk.RCPresenceEvent;

import java.util.HashMap;

/**
 * MayDayRegister use to register the client name and device for videoCall and chat message using the pending intent.
 */

public class MayDayRegister implements RCDeviceListener {

    private static final String TAG = "MayDayRegister";
    private MayDayRegisterInterface mCallBack;

    public void initialize(Context context) {

        //set RCClient setLogLevel
        RCClient.setLogLevel(Log.VERBOSE);
        RCClient.initialize(context, new RCClient.RCInitListener() {
            public void onInitialized() {
                Log.i(TAG, "RCClient initialized");
            }

            public void onError(Exception exception) {
                Log.e(TAG, "RCClient initialization error");
            }
        });
    }

    public void createDevice(HashMap<String, Object> params, Context context, Class activityClass) {
        // Create a device for connection
        RCDevice device = RCClient.createDevice(params, this);
        assert device != null;
        device.setPendingIntents(new Intent(context, activityClass));
    }

    public static void mayDayShutDown() {
        // Shutdown RCClient
        RCClient.shutdown();
    }

    @Override
    public void onStartListening(RCDevice device) {

    }

    @Override
    public void onStopListening(RCDevice device) {

    }

    @Override
    public void onStopListening(RCDevice device, int errorCode, String errorText) {

    }

    @Override
    public void onConnectivityUpdate(RCDevice device, RCConnectivityStatus connectivityStatus) {

    }

    @Override
    public boolean receivePresenceEvents(RCDevice device) {
        return false;
    }

    @Override
    public void onPresenceChanged(RCDevice device, RCPresenceEvent presenceEvent) {

    }

    @Override
    public void onRegisterState(boolean connectivityStatus) {
        mCallBack.onRegisterAction(connectivityStatus);
    }

    public void setListener(MayDayRegisterInterface listener) {
        mCallBack = listener;
    }

    public interface MayDayRegisterInterface {
        void onRegisterAction(boolean status);

    }
}
