
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import org.mobicents.restcomm.android.client.sdk.RCClient;
import org.mobicents.restcomm.android.client.sdk.RCConnection;
import org.mobicents.restcomm.android.client.sdk.RCConnectionListener;
import org.mobicents.restcomm.android.client.sdk.RCDevice;
import org.mobicents.restcomm.android.sipua.impl.DeviceImpl;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoTrack;
import java.util.HashMap;

public class MayDayVideoCallFragment extends Fragment implements RCConnectionListener, View.OnClickListener {

    // Local preview screen position before videoChat is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;
    // Local preview screen position after videoChat is connected.
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 2;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    //  SharedPreferences prefs;
    private static final String TAG = "MayDayVideoCallFragment";
    private final HashMap<String, Object> mConnectParams = new HashMap<>();
    private GLSurfaceView mVideoView;
    private VideoRenderer.Callbacks mLocalRender = null;
    private VideoRenderer.Callbacks mRemoteRender = null;
    private VideoRendererGui.ScalingType mScalingType;
    private RCConnection mConnection, mPendingConnection;
    private RCDevice mDevice;
    private boolean mPendingError = false;
    private boolean mMuteAudio = false;
    private LinearLayout mLinearLayoutControls, mLinearLayoutVideo;
    private ImageView mImageViewMute, mImageViewFullScreen, mImageViewAnswer;
    private boolean isVideoFullScreen=false;
    private AlertDialog mAlertDialog;
    private VideoCallInterface mCallback;
    private String mAgentName, mDomainAddress, mVideoCall;
    public static String INCOMING_CALL = RCDevice.INCOMING_CALL;
    private int LEFT_MARGIN=25;
    private int TOP_MARGIN=75;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallback = (VideoCallInterface) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        View viewInfo = inflater.inflate(R.layout.videochat, container, false);

        // Initialize view
        mLinearLayoutControls = (LinearLayout) viewInfo.findViewById(R.id.linearLayout_Controls);
        mLinearLayoutVideo = (LinearLayout) viewInfo.findViewById(R.id.linearLayout_video);
        mImageViewMute = (ImageView) viewInfo.findViewById(R.id.imageView_mute_audio);
        mImageViewFullScreen = (ImageView) viewInfo.findViewById(R.id.imageView_fullscreen);
        mImageViewAnswer = (ImageView) viewInfo.findViewById(R.id.imageView_answer);
        ImageView imageViewHang = (ImageView) viewInfo.findViewById(R.id.imageView_hangup);
        mAlertDialog = new AlertDialog.Builder(getActivity()).create();

        mImageViewFullScreen.setOnClickListener(this);
        mImageViewMute.setOnClickListener(this);
        imageViewHang.setOnClickListener(this);
        mImageViewAnswer.setOnClickListener(this);

        Bundle bundle = getArguments();
        if(bundle.getString(MayDayConstant.AGENT_NAME)!=null) {
            mAgentName = bundle.getString(MayDayConstant.AGENT_NAME);
        }

        if(bundle.getString(MayDayConstant.DOMAIN_ADDRESS)!=null) {
            mDomainAddress = bundle.getString(MayDayConstant.DOMAIN_ADDRESS);
        }

        if(bundle.getString(MayDayConstant.VIDEO_CALL)!=null) {
            mVideoCall = bundle.getString(MayDayConstant.VIDEO_CALL);
        }

        try {
            //get last device register name
            mDevice = RCClient.listDevices().get(0);
        } catch (NullPointerException e) {
           e.printStackTrace();
        }

        mScalingType = VideoRendererGui.ScalingType.SCALE_ASPECT_FILL;
        mVideoView = (GLSurfaceView) viewInfo.findViewById(R.id.glView_call);

        final Intent intent = getActivity().getIntent();
        if (intent.getExtras() != null) {
            if (intent.getAction().equals(RCDevice.OUTGOING_CALL) || intent.getAction().equals(RCDevice.INCOMING_MESSAGE)) {
                mImageViewAnswer.setVisibility(View.INVISIBLE);
            } else {
                mImageViewAnswer.setVisibility(View.VISIBLE);
            }
        }

        // Get device resolution width and height
        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        final int windowWidth = size.x;
        final int windowHeight = size.y;

        // GLVideo resize from top to left corner
        mLinearLayoutVideo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mLinearLayoutVideo.getLayoutParams();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int x_cord = (int) event.getRawX();
                        int y_cord = (int) event.getRawY();

                        if (x_cord > windowWidth) {
                            x_cord = windowWidth;
                        }
                        if (y_cord > windowHeight) {
                            y_cord = windowHeight;
                        }

                        layoutParams.leftMargin = x_cord - LEFT_MARGIN;
                        layoutParams.topMargin = y_cord - TOP_MARGIN;

                        mLinearLayoutVideo.setLayoutParams(layoutParams);
                        break;
                    default:
                        break;
                }
                return true;

            }
        });

        // Setup video stuff
        VideoRendererGui.setView(mVideoView, new Runnable() {

            @Override
            public void run() {
                if (mVideoCall != null) {
                    if (mVideoCall.equalsIgnoreCase(MayDayConstant.OUTGOING)) {
                        callAgent();
                    }
                } else {
                    videoContextReady(intent);
                }
            }
        });

        // Create video renderers.
        mRemoteRender = VideoRendererGui.create(
                REMOTE_X, REMOTE_Y,
                REMOTE_WIDTH, REMOTE_HEIGHT, mScalingType, false);
        mLocalRender = VideoRendererGui.create(
                LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
                LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING, mScalingType, true);
        mImageViewMute.setVisibility(View.INVISIBLE);

        return viewInfo;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.imageView_hangup) {
            if (mPendingConnection != null) {
                // incoming ringing
                mPendingConnection.reject();
                mPendingConnection = null;
               // mLinearLayoutControls.setVisibility(View.INVISIBLE);

            } else {
                if (mConnection != null) {
                    // incoming established or outgoing any state (pending, connecting, connected)
                    mConnection.disconnect();
                    mConnection = null;
                    mPendingConnection = null;
                } else {
                    //mLinearLayoutControls.setVisibility(View.INVISIBLE);
                    Log.e(TAG, "Error: not connected/connecting/pending");
                }
            }
             mCallback.onMayDayClose();
        } else if (view.getId() == R.id.imageView_answer) {
            // Tap on answer button accept the call
            if (mPendingConnection != null) {
                mImageViewAnswer.setVisibility(View.INVISIBLE);
                HashMap<String, Object> params = new HashMap<>();
                params.put(MayDayConstant.VIDEO_ENABLE, true);
                mPendingConnection.accept(params);
                mConnection = this.mPendingConnection;
                mPendingConnection = null;
                DeviceImpl.GetInstance().soundManager.stopRinging();
            }
        } else if (view.getId() == R.id.imageView_mute_audio) {
            //Tap on mute disable and enable mic
            if (mConnection != null) {
                if (!mMuteAudio) {
                    mImageViewMute.setImageResource(R.drawable.speaker_mute);
                } else {
                    mImageViewMute.setImageResource(R.drawable.speaker_icon);
                }
                mMuteAudio = !mMuteAudio;
                mConnection.setAudioMuted(mMuteAudio);
            }
        } else if (view.getId() == R.id.imageView_fullscreen) {
            //Maximise and minimise the video screen
            int height = getScreenResolution();
            if (!isVideoFullScreen) {
                isVideoFullScreen = true;
                mImageViewFullScreen.setImageResource(R.drawable.minimize);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);

                layoutParams.addRule(RelativeLayout.ABOVE, R.id.linearLayout_Controls);
                mLinearLayoutVideo.setLayoutParams(layoutParams);
                mLinearLayoutVideo.setPadding(0, 0, 0, 0);
            } else {
                mImageViewFullScreen.setImageResource(R.drawable.maximize);
                isVideoFullScreen = false;
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        height);

                layoutParams.addRule(RelativeLayout.ABOVE, R.id.linearLayout_Controls);
                mLinearLayoutVideo.setLayoutParams(layoutParams);
                mLinearLayoutVideo.setPadding(30, 0, 30, 0);
            }
        }
    }

    // Get device resolution by screen density and fix the minimise height
    private int getScreenResolution() {
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        int height;
        switch (screenSize) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                height = MayDayConstant.SIZE_LARGE;
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                height = MayDayConstant.SIZE_NORMAL;
                break;
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                height = MayDayConstant.SIZE_SMALL;
                break;
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                height = MayDayConstant.SIZE_XLARGE;
                break;
            default:
                height = MayDayConstant.SIZE_SMALL;
        }
        return height;
    }

    // Call to agent pass name and domain address
    private void callAgent() {
        mImageViewFullScreen.setVisibility(View.GONE);
        Intent intent = new Intent();
        intent.setAction(RCDevice.OUTGOING_CALL);
        intent.putExtra(RCDevice.EXTRA_DID, "sip:" + mAgentName + "@" + mDomainAddress);
        intent.putExtra(RCDevice.EXTRA_VIDEO_ENABLED, true);
        videoContextReady(intent);
    }

    // video initialization
    private void videoContextReady(Intent intent) {
        final Intent finalIntent = intent;
        final MayDayVideoCallFragment finalActivity = this;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // Important note: I used to set visibility in Create(), to avoid the flashing of the GL view when it gets added and then removed right away.
                // But if I make the video view invisible when VideoRendererGui.create() is called, then videoContextReady is never called. Need to figure
                // out a way to work around this
                mVideoView.setVisibility(View.GONE);
                if (finalIntent.getExtras() != null) {
                    if (finalIntent.getAction().equals(RCDevice.OUTGOING_CALL)) {
                        mLinearLayoutControls.setVisibility(View.VISIBLE);

                        mConnectParams.put(MayDayConstant.USERNAME, finalIntent.getStringExtra(RCDevice.EXTRA_DID));
                        mConnectParams.put(MayDayConstant.VIDEO_ENABLE, finalIntent.getBooleanExtra(RCDevice.EXTRA_VIDEO_ENABLED, false));
                        mConnection = mDevice.connect(mConnectParams, finalActivity);

                        if (mConnection == null) {
                            Log.e(TAG, "Error: error connecting");
                            showOkAlert(getResources().getString(R.string.rc_device_error), getResources().getString(R.string.connectivity_error));
                        }
                    }
                    if (finalIntent.getAction().equals(RCDevice.INCOMING_CALL)) {
                        mPendingConnection = mDevice.getPendingConnection();
                        mPendingConnection.setConnectionListener(finalActivity);
                    }
                }
            }
        });
    }

    // RCConnection Listeners
    public void onConnecting(RCConnection connection) {
        Log.i(TAG, "RCConnection connecting");
    }

    // When video call get connected stop the ringing sound and display the video view.
    public void onConnected(RCConnection connection) {
        Log.i(TAG, "RCConnection connected");
        mImageViewFullScreen.setVisibility(View.VISIBLE);
        mImageViewMute.setVisibility(View.VISIBLE);
        // reset to no mute at beginning of new videoChat.
        mMuteAudio = false;
        getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    //When video call get disconnected close the video view.
    public void onDisconnected(RCConnection connection) {
        Log.i(TAG, "RCConnection disconnected");
        mImageViewMute.setVisibility(View.INVISIBLE);
        this.mConnection = null;
        mPendingConnection = null;
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (!mPendingError) {

            mVideoView.setVisibility(View.GONE);
            mLinearLayoutControls.setVisibility(View.INVISIBLE);
        } else {
            mPendingError = false;
            mVideoView.setVisibility(View.GONE);
            mLinearLayoutControls.setVisibility(View.INVISIBLE);
        }
        mCallback.onMayDayClose();
    }

    //when call get cancelled close the connection.
    public void onCancelled(RCConnection connection) {
        Log.i(TAG, "RCConnection cancelled");
        mVideoView.setVisibility(View.GONE);
        mLinearLayoutControls.setVisibility(View.INVISIBLE);
        this.mConnection = null;
        mPendingConnection = null;
        mCallback.onMayDayClose();
    }

    //when call get declined close the connection.
    public void onDeclined(RCConnection connection) {
        Log.i(TAG, "RCConnection declined");
        this.mConnection = null;
        mPendingConnection = null;
        mVideoView.setVisibility(View.GONE);
        mLinearLayoutControls.setVisibility(View.INVISIBLE);
        mCallback.onMayDayClose();
    }

    //when call get disconnected close mConnection and mPendingConnection.
    public void onDisconnected(RCConnection connection, int errorCode, String errorText) {
        mPendingError = true;
        showOkAlert("RCConnection Error", errorText);
        this.mConnection = null;
        mPendingConnection = null;
        mVideoView.setVisibility(View.GONE);
        mLinearLayoutControls.setVisibility(View.INVISIBLE);
        mCallback.onMayDayClose();
    }

    public void onReceiveLocalVideo(RCConnection connection, VideoTrack videoTrack) {
        if (videoTrack != null) {
            //show media on screen
            videoTrack.setEnabled(true);
            videoTrack.addRenderer(new VideoRenderer(mLocalRender));
        }
    }

    public void onReceiveRemoteVideo(RCConnection connection, VideoTrack videoTrack) {
        if (videoTrack != null) {
            //show media on screen
            videoTrack.setEnabled(true);
            videoTrack.addRenderer(new VideoRenderer(mRemoteRender));

            VideoRendererGui.update(mRemoteRender,
                    REMOTE_X, REMOTE_Y,
                    REMOTE_WIDTH, REMOTE_HEIGHT, mScalingType, false);
            VideoRendererGui.update(mLocalRender,
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                    LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, true);
            mVideoView.setVisibility(View.VISIBLE);
        }
    }

    // Helpers
    private void showOkAlert(final String title, final String detail) {

        mAlertDialog.setTitle(title);
        mAlertDialog.setMessage(detail);
        mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        mAlertDialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPendingConnection != null) {
            // incoming ringing
            mPendingConnection.reject();
            mPendingConnection = null;
           // mLinearLayoutControls.setVisibility(View.INVISIBLE);

        } else {
            if (mConnection != null) {
                // incoming established or outgoing any state (pending, connecting, connected)
                mConnection.disconnect();
                mConnection = null;
                mPendingConnection = null;

            }
        }
        //getActivity().finish();
    }

    public interface VideoCallInterface {
        void onMayDayClose();
    }
}



