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
package com.telestax.mayday_customer.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.telestax.mayday_customer.R;
import com.telestax.mayday_customer.activity.MainActivity;
import com.telestax.mayday_customer.utils.Constant;

public class ProductFragment extends Fragment implements View.OnClickListener {

    private ImageView mImageViewCustomerMayDayCall;
    private LinearLayout mDotsLayout;
    private ProductInterface mCallBack;
    private TextView[] mDots;
    private BroadcastReceiver mInitReceiver;

    //ViewPager slider dot
    private final ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager
            .OnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            for (int i = 0; i < 4; i++) {
                mDots[i].setTextColor(getResources().getColor(android.R.color
                        .secondary_text_light_nodisable));
            }
            mDots[position].setTextColor(getResources().getColor(android.R.color.white));

        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallBack = (ProductInterface) activity;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View viewInfo = inflater.inflate(R.layout.product, container, false);

        ViewPager viewPager = (ViewPager) viewInfo.findViewById(R.id.view_pager);
        mDotsLayout = (LinearLayout) viewInfo.findViewById(R.id.viewPagerCountDots);

        mInitReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mImageViewCustomerMayDayCall.setVisibility(View.VISIBLE);
            }
        };

        //Register broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mInitReceiver, new IntentFilter(Constant.BROADCAST_INTENT));

        // Initialize UI
        ImageView ImageViewProductOne = (ImageView) viewInfo.findViewById(R.id.imageView_product);
        ImageView ImageViewProductTwo = (ImageView) viewInfo.findViewById(R.id.imageView_product_two);
        ImageView ImageViewProductThree = (ImageView) viewInfo.findViewById(R.id.imageView_product_three);
        ImageView ImageViewProductFour = (ImageView) viewInfo.findViewById(R.id.imageView_product_four);
        ImageView ImageViewProductFive = (ImageView) viewInfo.findViewById(R.id.imageView_product_five);
        ImageView ImageViewProductSix = (ImageView) viewInfo.findViewById(R.id.imageView_product_six);
        ImageView ImageViewProductSeven = (ImageView) viewInfo.findViewById(R.id.imageView_product_seven);

        mImageViewCustomerMayDayCall = (ImageView) viewInfo.findViewById(R.id.imageView_customer_mayday);

        ImageViewProductOne.setOnClickListener(this);
        ImageViewProductTwo.setOnClickListener(this);
        ImageViewProductThree.setOnClickListener(this);
        ImageViewProductFour.setOnClickListener(this);
        ImageViewProductFive.setOnClickListener(this);
        ImageViewProductSix.setOnClickListener(this);
        ImageViewProductSeven.setOnClickListener(this);

        String mayDayAction = MainActivity.getMaydaySharePref(getActivity());
        if (mayDayAction != null) {
            if (mayDayAction.equalsIgnoreCase(Constant.YES)) {
                mImageViewCustomerMayDayCall.setVisibility(View.INVISIBLE);
            } else {
                mImageViewCustomerMayDayCall.setVisibility(View.VISIBLE);
            }
        }

        ImagePagerAdapter adapter = new ImagePagerAdapter();
        viewPager.setAdapter(adapter);
        mDotsLayout.setBaselineAligned(true);
        setUiPageViewController();

        // ViewPager slider event
        viewPager.setOnPageChangeListener(viewPagerPageChangeListener);

        mImageViewCustomerMayDayCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CharSequence[] items = {
                        getResources().getString(R.string.video), getResources().getString(R.string.instant_message),
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.action));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) {
                            mCallBack.onVideoCall();
                            mImageViewCustomerMayDayCall.setVisibility(View.INVISIBLE);
                        } else if (item == 1) {
                            mCallBack.onChatMessage();
                            mImageViewCustomerMayDayCall.setVisibility(View.INVISIBLE);

                        }
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
        return viewInfo;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imageView_product || v.getId() == R.id.imageView_product_two
                || v.getId() == R.id.imageView_product_three || v.getId() == R.id.imageView_product_four
                || v.getId() == R.id.imageView_product_five || v.getId() == R.id.imageView_product_six
                || v.getId() == R.id.imageView_product_seven || v.getId() == R.id.imageView_product_eight) {
            mCallBack.onProductItemSelect();
        }
    }

    // Dot button below view pager slider
    private void setUiPageViewController() {

        mDots = new TextView[4];

        for (int i = 0; i < 4; i++) {
            mDots[i] = new TextView(getActivity());
            mDots[i].setText(Html.fromHtml("&#8226;"));
            mDots[i].setTextSize(30);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            llp.setMargins(0, -14, 0, 0); // llp.setMargins(left, top, right, bottom);
            mDots[i].setLayoutParams(llp);
            mDots[i].setTextColor(getResources().getColor(android.R.color.secondary_text_light_nodisable));
            mDotsLayout.addView(mDots[i]);
            mDots[0].setTextColor(getResources().getColor(R.color.beige));
        }
    }


    public interface ProductInterface {
        void onProductItemSelect();

        void onVideoCall();

        void onChatMessage();

    }

    // slider image
    private class ImagePagerAdapter extends PagerAdapter {
        private final int[] mImages = new int[]{
                R.drawable.banner1,
                R.drawable.banner2,
                R.drawable.banner3,
                R.drawable.banner1,
        };

        @Override
        public int getCount() {
            return mImages.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Context context = getActivity().getApplicationContext();
            ImageView imageView = new ImageView(context);
            int padding = context.getResources().getDimensionPixelSize(
                    R.dimen.padding_medium);
            imageView.setPadding(padding, padding, padding, padding);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setImageResource(mImages[position]);
            imageView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    //this will log the page number that was click
                    mCallBack.onProductItemSelect();
                }
            });
            container.addView(imageView, 0);
            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((ImageView) object);
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //UnRegister broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mInitReceiver);

    }

}

