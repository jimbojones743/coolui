package com.example.example.myapplication.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.example.example.myapplication.fragments.LoginCloudTab;
import com.example.example.myapplication.fragments.LoginLocalTab;
import com.example.example.myapplication.fragments.SignupCloudTab;
import com.example.example.myapplication.fragments.SignupLocalTab;

public class LoginPageAdapter extends FragmentStatePagerAdapter {

    private int numPages;

    public LoginPageAdapter(FragmentManager fm, int numPages) {
        super(fm);
        this.numPages = numPages;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new LoginCloudTab();
            case 1:
                return new LoginLocalTab();
        }
        return null;
    }

    @Override
    public int getCount() {
        return this.numPages;
    }

}
