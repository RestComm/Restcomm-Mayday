
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
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MayDayMessageListFragment extends ListFragment {
    private SimpleAdapter mListViewAdapter;
    private ArrayList<Map<String, String>> mContactList;

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = null;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        //public void onItemSelected(HashMap<String, String> contact, ContactSelectionType type);
        //public void onContactUpdate(HashMap<String, String> contact, int type);
    }


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MayDayMessageListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContactList = new ArrayList<>();

        String[] from = {MayDayConstant.USERNAME, MayDayConstant.MESSAGE_TEXT};
        int[] to = {R.id.message_username, R.id.message_text};

        mListViewAdapter = new SimpleAdapter(getActivity().getApplicationContext(), mContactList,
                R.layout.message_row_layout, from, to);
        setListAdapter(mListViewAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }

        registerForContextMenu(getListView());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        /*if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }*/

        mCallbacks = (Callbacks) getTargetFragment();

    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = null;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // no actions for now when tapping on an item
        /*
        HashMap item = (HashMap)getListView().getItemAtPosition(position);
        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(item, ContactSelectionType.VIDEO_CALL);
        */
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    // Called by Activity when when new message is sent
    public void addLocalMessage(String message) {
        HashMap<String, String> item = new HashMap<>();
        item.put(MayDayConstant.USERNAME, "Me");
        item.put(MayDayConstant.MESSAGE_TEXT, message);
        mContactList.add(item);
        this.mListViewAdapter.notifyDataSetChanged();
        getListView().setSelection(mListViewAdapter.getCount() - 1);
    }

    // Called by Activity when when new message is sent
    public void addRemoteMessage(String message, String username) {
        HashMap<String, String> item = new HashMap<>();
        item.put(MayDayConstant.USERNAME, username);
        item.put(MayDayConstant.MESSAGE_TEXT, message);
        mContactList.add(item);
        this.mListViewAdapter.notifyDataSetChanged();
        getListView().setSelection(mListViewAdapter.getCount() - 1);
    }


}