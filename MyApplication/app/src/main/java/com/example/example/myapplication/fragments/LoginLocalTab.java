package com.example.example.myapplication.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.example.myapplication.R;
import com.example.example.myapplication.remote.DynRH2LevClientWrapper;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginLocalTab extends Fragment implements View.OnClickListener {

    @BindView(R.id.input_password_local) EditText _passwordText;
    @BindView(R.id.btn_login_local) Button _loginButton;
    @BindView(R.id.link_signup_local) TextView _signupLink;
    @BindView(R.id.link_recovery_local) TextView _recoveryLink;

    private Activity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_login_local, container, false);
        ButterKnife.bind(this, view);
        _loginButton.setOnClickListener(this);
        _signupLink.setOnClickListener(this);
        _recoveryLink.setOnClickListener(this);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mActivity = (Activity) context;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login_local:
                login();
                break;
            case R.id.link_signup_local:
                goToSignup();
                break;
            case R.id.link_recovery_local:
                goToRecovery();
                break;
        }
    }

    public void login() {
        if (!validate()) {
            onLoginFailed("Validation failed");
        }

        _loginButton.setEnabled(false);

        SharedPreferences localPref = Utils.getLocalSharedPref(mActivity);
        String password = _passwordText.getText().toString();
        String passwordSalt = localPref.getString(Const.Local.PASSWORD_SALT_LABEL, "");
        // if the passwords match, log the user in
        if (Utils.hashPassword(password, passwordSalt).equals(localPref.getString(Const.Local.PASSWORD_HASH_LABEL, ""))) {
            Log.i("LOGIN", "SUCCESS");
            // set our shared preference to say that we're local
            SharedPreferences sharedPref = mActivity.getSharedPreferences(Const.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(Const.SHARED_PREF_EMAIL, "local");
            editor.commit();
            if (DynRH2LevClientWrapper.keyGen(mActivity, password)) {
                // start our camera
                Intent homepage = new Intent(mActivity, MainActivity.class);
                startActivity(homepage);
            } else {
                onLoginFailed("Could not generate key");
            }
        } else {
            // we have failed to login
            onLoginFailed("Password incorrect");
        }
    }

    public void goToSignup() {
        // Start the Signup activity
        Intent intent = new Intent(mActivity.getApplicationContext(), SignupActivity.class);
        startActivity(intent);
    }

    public void goToRecovery() {
        Intent intent = new Intent(mActivity.getApplicationContext(), PasswordRecoveryActivity.class);
        startActivity(intent);
    }

    /**
     * Validates that the input is correct.
     * @return
     */
    private boolean validate() {
        boolean valid = true;

        String password = _passwordText.getText().toString();

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    private void onLoginFailed(String message) {
        _loginButton.setEnabled(true);
        Toast.makeText(mActivity.getBaseContext(), message, Toast.LENGTH_LONG).show();
    }

}
