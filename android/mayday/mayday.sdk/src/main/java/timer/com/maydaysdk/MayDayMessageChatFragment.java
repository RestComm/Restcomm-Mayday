
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.mobicents.restcomm.android.client.sdk.RCClient;
import org.mobicents.restcomm.android.client.sdk.RCDevice;

import java.util.HashMap;

/**
 * MayDayMessageChatFragment used for chat conversation both customer and agent.
 * Each chat conversation is identified by incoming or outgoing key value which is passed through bundle.
 * Chat view can be minimised and maximise through onTouch listener
 */

public class MayDayMessageChatFragment extends Fragment implements View.OnClickListener,
        MayDayMessageListFragment.Callbacks {

    private TextView mTextViewAgentName;
    private EditText mEditTextChatMessage;
    private LinearLayout mLinearLayoutMessage;
    private boolean isChatFullScreen = false;
    private ImageButton mImageButtonChatResize;
    private AlertDialog mAlertDialog;
    private final HashMap<String, Object> mMessageParams = new HashMap<>();
    private RCDevice mDevice;
    private static MayDayMessageListFragment mListFragment;
    private MessageChatInterface mCallBackInterface;
    public static String INCOMING_MESSAGE = RCDevice.INCOMING_MESSAGE;
    private int MARGIN_TOP=75;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallBackInterface = (MessageChatInterface) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View viewInfo = inflater.inflate(R.layout.messagechat, container, false);

        // Initialize UI
        ImageButton imageButtonChatSend = (ImageButton) viewInfo.findViewById(R.id.imageButton_send);
        ImageButton imageButtonChatClose = (ImageButton) viewInfo.findViewById(R.id.imageButton_chat_close);
        mTextViewAgentName = (TextView) viewInfo.findViewById(R.id.textView_chat_agent_name);
        mLinearLayoutMessage = (LinearLayout) viewInfo.findViewById(R.id.linearLayout_message);
        mImageButtonChatResize = (ImageButton) viewInfo.findViewById(R.id.imageButton_chat_resize);
        mEditTextChatMessage = (EditText) viewInfo.findViewById(R.id.editText_chat_message);
        mAlertDialog = new AlertDialog.Builder(getActivity()).create();

        imageButtonChatClose.setOnClickListener(this);
        mImageButtonChatResize.setOnClickListener(this);
        imageButtonChatSend.setOnClickListener(this);
        mEditTextChatMessage.setOnClickListener(this);

        mListFragment = (MayDayMessageListFragment) getChildFragmentManager().findFragmentById(R.id.message_list);

        // Get RCClient device list
        try {
            mDevice = RCClient.listDevices().get(0);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        // Get device resolution width and height
        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        final int windowHeight = size.y;

        // Message chat resize from top to bottom
        mLinearLayoutMessage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                        mLinearLayoutMessage.getLayoutParams();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int y_cord = (int) event.getRawY();

                        if (y_cord > windowHeight) {
                            y_cord = windowHeight;
                        }

                        layoutParams.topMargin = y_cord - MARGIN_TOP;
                        mLinearLayoutMessage.setLayoutParams(layoutParams);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        Bundle bundle = getArguments();
        String agentName="",domainAddress="";
        // Get the event data from the fragment, which pass agent name and domain address
        if(bundle.getString(MayDayConstant.AGENT_NAME)!=null) {
            agentName = bundle.getString(MayDayConstant.AGENT_NAME);
        }
        if(bundle.getString(MayDayConstant.DOMAIN_ADDRESS)!=null) {
            domainAddress = bundle.getString(MayDayConstant.DOMAIN_ADDRESS);
        }
        initializeChat(agentName, domainAddress);

        return viewInfo;
    }

    private void initializeChat(String agentName, String domainAddress) {
        //send agent name and domain address to initiate chat.
        mMessageParams.put(MayDayConstant.USERNAME, MayDayConstant.SIP + agentName + MayDayConstant.SIPAT + domainAddress);
        String username = MayDayConstant.SIP + agentName + MayDayConstant.SIPAT + domainAddress;
        String shortName = username.replaceAll("^sip:", "").replaceAll("@.*$", "");
        mTextViewAgentName.setText(shortName);
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


    // onClick listener for the even action
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.imageButton_send) {
            // Tap on chat send button send message data to list fragment.
            HashMap<String, String> sendParams = new HashMap<>();
            sendParams.put(MayDayConstant.USERNAME, (String) mMessageParams.get(MayDayConstant.USERNAME));
            if (mDevice.sendMessage(mEditTextChatMessage.getText().toString(), sendParams)) {
                // also output the message in the wall
                mListFragment.addLocalMessage(mEditTextChatMessage.getText().toString());
                mEditTextChatMessage.setText("");
            } else {
                showAlert();
            }
        } else if (view.getId() == R.id.imageButton_chat_resize) {
            // Tap on chat resize to maximise and minimise.
            int height = getScreenResolution();
            if (!isChatFullScreen) {
                isChatFullScreen = true;
                mImageButtonChatResize.setImageResource(R.drawable.chat_minimize);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);

                layoutParams.addRule(RelativeLayout.ABOVE, R.id.linearLayout_chat_controls);
                mLinearLayoutMessage.setLayoutParams(layoutParams);
                mLinearLayoutMessage.setPadding(0, 0, 0, 0);
            } else {
                mImageButtonChatResize.setImageResource(R.drawable.chat_maximize);
                isChatFullScreen = false;
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        height);

                layoutParams.addRule(RelativeLayout.ABOVE, R.id.linearLayout_chat_controls);
                mLinearLayoutMessage.setLayoutParams(layoutParams);
                mLinearLayoutMessage.setPadding(30, 0, 30, 0);
            }
        } else if (view.getId() == R.id.imageButton_chat_close) {
            //chat message close pass encrypt key.
            HashMap<String, String> sendParams = new HashMap<>();
            sendParams.put(MayDayConstant.USERNAME, (String) mMessageParams.get(MayDayConstant.USERNAME));
            mDevice.sendMessage(MayDayConstant.CHAT_CLOSE_ENCRYPT_KEY, sendParams);
            hideSoftKeyboard(getActivity());
            mCallBackInterface.onMayDayClose();
        }
    }
    //Hide device keyboard when it is visible
    private void hideSoftKeyboard(Activity activity) {
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        //Incoming message handled
        Intent finalIntent = getActivity().getIntent();
        if (finalIntent.getExtras() != null) {
            //Get incoming message form other device/browser
            if (finalIntent.getAction().equals(RCDevice.INCOMING_MESSAGE)) {
                //Get incoming message text form other device/browser
                String message = finalIntent.getStringExtra(RCDevice.INCOMING_MESSAGE_TEXT);
                if (message != null) {
                    //If incoming message is chat close encrypt key,Then close the chat.
                    if (message.equalsIgnoreCase(MayDayConstant.CHAT_CLOSE_ENCRYPT_KEY)) {
                        mCallBackInterface.onMayDayClose();
                    } else {
                        //If incoming message is not chat close encrypt key(Some text) then display in the chat view.
                        HashMap<String, String> intentParams = (HashMap<String, String>) finalIntent.getSerializableExtra(RCDevice.INCOMING_MESSAGE_PARAMS);
                        String username = intentParams.get(MayDayConstant.USERNAME);
                        String shortName = username.replaceAll("^sip:", "").replaceAll("@.*$", "");
                        mMessageParams.put(MayDayConstant.USERNAME, username);
                        mListFragment.addRemoteMessage(message, shortName);
                        //Display chat name on the header of the chat view.
                        mTextViewAgentName.setText(shortName);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Clear intent
        getActivity().getIntent().removeExtra(RCDevice.INCOMING_MESSAGE);
        getActivity().getIntent().removeExtra(RCDevice.INCOMING_MESSAGE_TEXT);
    }

    // Helpers
    private void showAlert() {

        mAlertDialog.setTitle(getResources().getString(R.string.rc_device_error));
        mAlertDialog.setMessage(getResources().getString(R.string.connectivity_error));
        mAlertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        mAlertDialog.show();

    }

    public interface MessageChatInterface {
        void onMayDayClose();
    }
}
