package com.example.example.myapplication.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.example.myapplication.R;
import com.example.example.myapplication.adapters.MainPageVerticalAdapter;
import com.example.example.myapplication.view.VerticalViewPager;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Fragment to hold a vertical pager. This contains the camera and is a child of the normal
 * horizontal scrolling pager fragment
 */
public class VerticalPagerFragment extends Fragment {

    @BindView(R.id.main_vertical_view_pager) VerticalViewPager _vertViewPager;

    private MainPageVerticalAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main_vertical, container, false);
        ButterKnife.bind(this, view);
        this.adapter = new MainPageVerticalAdapter(getChildFragmentManager(), 2);
        _vertViewPager.setAdapter(this.adapter);
        return view;
    }

    public void switchAdapterItem(int position) {
        _vertViewPager.setCurrentItem(position);
    }

}
