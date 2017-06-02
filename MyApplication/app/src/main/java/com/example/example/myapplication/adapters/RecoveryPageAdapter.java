package com.example.example.myapplication.adapters;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.example.example.myapplication.fragments.RecoveryCloudTab;
import com.example.example.myapplication.fragments.RecoveryLocalTab;

public class RecoveryPageAdapter extends FragmentStatePagerAdapter {

    private int numPages;

    public RecoveryPageAdapter(FragmentManager fm, int numPages) {
        super(fm);
        this.numPages = numPages;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new RecoveryCloudTab();
            case 1:
                return new RecoveryLocalTab();
        }
        return null;
    }

    @Override
    public int getCount() {
        return this.numPages;
    }

}
