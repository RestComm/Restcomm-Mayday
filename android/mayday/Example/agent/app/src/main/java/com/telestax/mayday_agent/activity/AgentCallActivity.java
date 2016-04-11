
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import com.telestax.mayday_agent.R;
import com.telestax.mayday_agent.fragment.AgentCallFragment;
import com.telestax.mayday_agent.utils.Constant;
import timer.com.maydaysdk.MayDayMessageChatFragment;
import timer.com.maydaysdk.MayDayVideoCallFragment;

public class AgentCallActivity extends AppCompatActivity implements MayDayVideoCallFragment.VideoCallInterface,
        MayDayMessageChatFragment.MessageChatInterface {

    private FragmentManager mFragmentManagerContent;
    private FragmentManager mFragmentManagerMayDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent_call);

        // Fragment Manager
        mFragmentManagerContent = getSupportFragmentManager();
        mFragmentManagerMayDay = getSupportFragmentManager();
        showAgentCallFragment();
    }

    private void showAgentCallFragment() {

        Bundle mayDayBundle = getAgentDetails();
        mFragmentManagerContent.beginTransaction()
                .replace(R.id.fragment_agent, new AgentCallFragment()).commit();

        final Intent intent = getIntent();
        if (intent.getExtras() != null) {
            if (intent.getAction().equals(MayDayVideoCallFragment.INCOMING_CALL)) {
                MayDayVideoCallFragment videoCallFragment = new MayDayVideoCallFragment();
                videoCallFragment.setArguments(mayDayBundle);
                mFragmentManagerMayDay.beginTransaction()
                        .replace(R.id.fragment_mayday, videoCallFragment).commit();
            } else if (intent.getAction().equals(MayDayMessageChatFragment.INCOMING_MESSAGE)) {
                MayDayMessageChatFragment messageChatFragment = new MayDayMessageChatFragment();
                messageChatFragment.setArguments(mayDayBundle);
                mFragmentManagerMayDay.beginTransaction()
                        .replace(R.id.fragment_mayday, messageChatFragment).commit();
            }
        }
    }

    private Bundle getAgentDetails() {
        SharedPreferences prefShare = getSharedPreferences(Constant.MY_PREFS_NAME, MODE_PRIVATE);
        String domainAddress = prefShare.getString(Constant.DOMAIN, null);
        String agentName = prefShare.getString(Constant.AGENT_NAME, null);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.AGENT_NAME, agentName);
        bundle.putString(Constant.DOMAIN_ADDRESS, domainAddress);
        return bundle;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onMayDayClose() {
        finish();
    }

    @Override
    public void onBackPressed() {
    }
}
