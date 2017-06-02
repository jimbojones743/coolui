package com.example.example.myapplication.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
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
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.crypto.sse.CryptoPrimitives;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

/**
 * Tab to let user log into their cloud account.
 */
public class LoginCloudTab extends Fragment implements View.OnClickListener {

    @BindView(R.id.input_email) EditText _emailText;
    @BindView(R.id.input_password) EditText _passwordText;
    @BindView(R.id.btn_login) Button _loginButton;
    @BindView(R.id.link_signup) TextView _signupLink;
    @BindView(R.id.link_recovery) TextView _recoveryLink;
    @BindView(R.id.link_setup_new_device) TextView _setupNewDeviceLink;

    private Activity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_login_cloud, container, false);
        ButterKnife.bind(this, view);
        _loginButton.setOnClickListener(this);
        _signupLink.setOnClickListener(this);
        _recoveryLink.setOnClickListener(this);
        _setupNewDeviceLink.setOnClickListener(this);
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
            case R.id.btn_login:
                login();
                break;
            case R.id.link_signup:
                goToSignup();
                break;
            case R.id.link_recovery:
                goToRecovery();
                break;
            case R.id.link_setup_new_device:
                setupNewDevice();
                break;
        }
    }

    public void login() {
        Log.i("LOGIN", "pressed");
        if (!validate()) {
            onLoginFailed("Please try again");
            return;
        }

        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(mActivity);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        String email = _emailText.getText().toString();
        final String password = _passwordText.getText().toString();

        Utils.putCurrentEmailInSharedPref(mActivity, email);

        DynRH2LevClientWrapper.login(mActivity, email, password, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                progressDialog.setMessage("Generating Key...");
                // generate the key
                if (DynRH2LevClientWrapper.keyGen(mActivity, password)) {
                    progressDialog.dismiss();
                    onLoginSuccess();
                } else {
                    progressDialog.dismiss();
                    onLoginFailed("Could not generate key, perhaps due to login on new device.");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                progressDialog.dismiss();
                if (responseBody != null) {
                    onLoginFailed(new String(responseBody));
                } else {
                    onLoginFailed("Could not connect to server.");
                }
            }
        });
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

    public void setupNewDevice() {
        Intent intent = new Intent(mActivity.getApplicationContext(), SetupNewDeviceActivity.class);
        startActivity(intent);
    }

    /**
     * Validates that the input is correct.
     * @return
     */
    private boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    private void onLoginSuccess() {
        // create a directory for this user if they are new
        // we do this in login because we want people to be able to set up their stuff from different devices
        Utils.makeUserDirectory(mActivity);
        _loginButton.setEnabled(true);

        // set up the state for the user in the case that the encrypted state exists and the normal state doesn't
        SharedPreferences pref = Utils.getUserSharedPreference(mActivity);
        String encState = pref.getString(Const.ENC_STATE_SHARED_PREF_LABEL, null);
        try {
            if (pref.getString(Const.SHARED_PREF_STATE, null) == null && encState != null) {
                SharedPreferences.Editor editor = pref.edit();
                byte[] sk = Utils.getSk(getActivity());
                String stateString = new String(CryptoPrimitives.decryptAES_CTR_String(Utils.JSONToByteArray(encState), sk));
                editor.putString(Const.SHARED_PREF_STATE, stateString);
                editor.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mActivity, "Bad error", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent homepage = new Intent(mActivity, MainActivity.class);
        startActivity(homepage);
    }

    private void onLoginFailed(String message) {
        _loginButton.setEnabled(true);
        Toast.makeText(mActivity.getBaseContext(), message, Toast.LENGTH_LONG).show();
    }

}
