package com.example.example.myapplication.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.example.example.myapplication.fragments.BrowseFragment;
import com.example.example.myapplication.fragments.CameraFragment;
import com.example.example.myapplication.fragments.VerticalPagerFragment;

/**
 * This is a page adapter for the "main" part of the application. The "middle" view should be the camera,
 * the left should be the browse and query function at the same time, and the right should be the options
 */
public class MainPageAdapter extends FragmentPagerAdapter {

    private int numPages;

    public MainPageAdapter(FragmentManager fm, int numPages) {
        super(fm);
        this.numPages = numPages;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new BrowseFragment();
            case 1:
                //return new CameraFragment();
                return new VerticalPagerFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return this.numPages;
    }

}
