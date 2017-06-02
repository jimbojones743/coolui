package com.example.example.myapplication.adapters;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.example.myapplication.fragments.CameraFragment;
import com.example.example.myapplication.fragments.GalleryFragment;

public class MainPageVerticalAdapter extends FragmentPagerAdapter {

    private int numPages;

    public MainPageVerticalAdapter(FragmentManager fm, int numPages) {
        super(fm);
        this.numPages = numPages;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new CameraFragment();
            case 1:
                return new GalleryFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return this.numPages;
    }

}
