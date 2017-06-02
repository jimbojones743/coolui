package com.example.example.myapplication.fragments;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.example.myapplication.R;
import com.example.example.myapplication.remote.DynRH2LevClientWrapper;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.IndexBuilder;
import com.example.example.myapplication.utils.Utils;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.crypto.sse.CryptoPrimitives;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

/**
 * This tab allows the user to set up a local account
 */
public class SignupLocalTab extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    @BindView(R.id.input_password_local) EditText _passwordText;
    @BindView(R.id.btn_signup_local) Button _signupButton;
    @BindView(R.id.link_login_local) TextView _loginLink;
    @BindView(R.id.security_spinner_local) Spinner _securitySpinner;
    @BindView(R.id.security_spinner2_local) Spinner _securitySpinner2;
    @BindView(R.id.input_security_local) EditText _securityText;
    @BindView(R.id.input_security2_local) EditText _securityText2;
    @BindView(R.id.check_backup_cloud) CheckBox _checkbox;
    @BindView(R.id.layout_backup_cloud) LinearLayout _layoutBackupCloud;
    @BindView(R.id.input_first_name_local) EditText _firstNameText;
    @BindView(R.id.input_last_name_local) EditText _lastNameText;
    @BindView(R.id.input_email_local) EditText _emailText;

    private Activity mActivity;
    private String clientSalt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_signup_local, container, false);
        ButterKnife.bind(this, view);
        _signupButton.setOnClickListener(this);
        _loginLink.setOnClickListener(this);
        _checkbox.setOnCheckedChangeListener(this);
        ArrayAdapter<CharSequence> securityAdapter = ArrayAdapter.createFromResource(mActivity, R.array.security_questions, android.R.layout.simple_spinner_item);
        securityAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        _securitySpinner.setAdapter(securityAdapter);
        ArrayAdapter<CharSequence> securityAdapter2 = ArrayAdapter.createFromResource(mActivity, R.array.security_questions2, android.R.layout.simple_spinner_item);
        securityAdapter2.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        _securitySpinner2.setAdapter(securityAdapter2);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mActivity = (Activity) context;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_signup_local:
                signup();
                break;
            case R.id.link_login_local:
                backToLogin();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.check_backup_cloud:
                checkedSignupCloud();
                break;
        }
    }

    private void checkedSignupCloud() {
        if (_checkbox.isChecked()) {
            // if the box is check, that means the user wants to backup their password on the cloud
            _layoutBackupCloud.setVisibility(View.VISIBLE);
        } else {
            // the box isn't checked
            _layoutBackupCloud.setVisibility(View.GONE);
        }
    }

    private void signup() {
        File dir = Utils.getLocalUsecaseDirectory(mActivity);
        if (dir.exists()) {
            onSignupFailed("Local account already created!");
            return;
        }

        if (!validate()) {
            onSignupFailed("Validating failed!");
            return;
        }
        _signupButton.setEnabled(false);

        // get the password
        String password = _passwordText.getText().toString();
        String passwordSalt = IndexBuilder.csRandomAlphaNumericString(Const.Local.SALT_SIZE);

        String securityQuestion = _securitySpinner.getSelectedItem().toString();
        String securityAnswer = _securityText.getText().toString();
        String securityQuestion2 = _securitySpinner2.getSelectedItem().toString();
        String securityAnswer2 = _securityText2.getText().toString();

        SharedPreferences localPref = Utils.getLocalSharedPref(mActivity);
        SharedPreferences.Editor editor = localPref.edit();

        // set up our local state
        editor.putString(Const.SHARED_PREF_STATE, Utils.stateToJSON(new HashMap<String, Integer>()));
        editor.putString(Const.SHARED_PREF_SALT, Utils.byteArrayToJSON(CryptoPrimitives.randomBytes(Const.Local.SALT_SIZE)));
        editor.putString(Const.Local.PASSWORD_HASH_LABEL, Utils.hashPassword(password, passwordSalt));
        editor.putString(Const.Local.PASSWORD_SALT_LABEL, passwordSalt);
        editor.commit();
        // set up our local directories
        try {
            Utils.makeLocalUsecaseDirectory(mActivity);
        } catch (IOException e) {
            e.printStackTrace();
            onSignupFailed("Error creating user directories");
            return;
        }

        if (!_checkbox.isChecked()) {
            // set up local password recovery
            byte[] salt = CryptoPrimitives.randomBytes(8);
            byte[] passwordRecoveryKey;
            byte[] encPassword;
            try {
                passwordRecoveryKey = CryptoPrimitives.keyGenSetM(securityAnswer + securityAnswer2, salt, 100, 128);
                encPassword = CryptoPrimitives.encryptAES_CTR_Byte(passwordRecoveryKey, CryptoPrimitives.randomBytes(16), password.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                onSignupFailed("Error encrypting password");
                return;
            }
            editor.putString(Const.Local.PASSWORD_RECOVERY_MODE_LABEL, Const.Local.PASSWORD_RECOVERY_LOCAL);
            editor.putString(Const.Local.PASSWORD_RECOVERY_SALT_LABEL, Utils.byteArrayToJSON(salt));
            editor.putString(Const.Local.PASSWORD_ENC_LABEL, Utils.byteArrayToJSON(encPassword));
            editor.putString(Const.Local.SECURITY_QUESTION_1_LABEL, securityQuestion);
            editor.putString(Const.Local.SECURITY_QUESTION_2_LABEL, securityQuestion2);
            editor.commit();
            onSignupSuccess("");
        } else {
            // setup password recovery with the cloud and also make a cloud account
            editor.putString(Const.Local.PASSWORD_RECOVERY_MODE_LABEL, Const.Local.PASSWORD_RECOVERY_CLOUD);
            editor.commit();
            String firstName = _firstNameText.getText().toString();
            String lastName = _lastNameText.getText().toString();
            String email = _emailText.getText().toString();
            clientSalt = Utils.byteArrayToJSON(CryptoPrimitives.randomBytes(16));

            Utils.putCurrentEmailInSharedPref(mActivity, email);

            final ProgressDialog progressDialog = new ProgressDialog(mActivity);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Creating Account...");
            progressDialog.show();

            DynRH2LevClientWrapper.signup(mActivity, firstName, lastName, email, password, securityQuestion, securityAnswer,
                    securityQuestion2, securityAnswer2, clientSalt, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Log.i("SIGNUP LOCAL", "SUCCESS");
                            progressDialog.dismiss();
                            onSignupSuccess(new String(responseBody));
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Log.i("SIGNUP LOCAL", "FAILED");
                            progressDialog.dismiss();
                            onSignupFailed(new String(responseBody));
                        }
                    });
        }
    }

    private void backToLogin() {
        mActivity.finish();
    }

    private void onSignupSuccess(String salt) {
        // if we got here, we have successfully signed up locally
        if (_checkbox.isChecked()) {
            SharedPreferences sharedPref = Utils.getUserSharedPreference(mActivity);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(Const.SHARED_PREF_STATE, Utils.stateToJSON(new HashMap<String, Integer>()));
            editor.putString(Const.SHARED_PREF_SALT, salt);
            editor.commit();

            // create user directory
            File dir = Utils.getCurrentUserDir(mActivity);
            if (!dir.exists()) {
                dir.mkdir();
            }

            Log.i("SIGNUP SALT", salt);
            Toast.makeText(mActivity.getBaseContext(), "Signed up! Go to your email to validate your account", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mActivity.getBaseContext(), "You've set up your local account!", Toast.LENGTH_LONG).show();
            Log.i("Signup", "success");
        }
        mActivity.onBackPressed();
    }

    private void onSignupFailed(String message) {
        Toast.makeText(mActivity.getBaseContext(), message, Toast.LENGTH_LONG).show();
        _signupButton.setEnabled(true);
    }

    private boolean validate() {
        boolean valid = true;
        String firstName = _firstNameText.getText().toString();
        String lastName = _lastNameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();
        String answer1 = _securityText.getText().toString();
        String answer2 = _securityText2.getText().toString();

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        if (answer1.isEmpty()) {
            _securityText.setError("need an answer");
            valid = false;
        } else {
            _securityText.setError(null);
        }

        if (answer2.isEmpty()) {
            _securityText2.setError("need an answer");
            valid = false;
        } else {
            _securityText2.setError(null);
        }

        // if we check the cloud setup then we need to verify the other strings
        if (_checkbox.isChecked()) {
            if (firstName.isEmpty() || firstName.length() < 1) {
                _firstNameText.setError("at least 3 characters");
                valid = false;
            } else {
                _firstNameText.setError(null);
            }

            if (lastName.isEmpty() || lastName.length() < 1) {
                _lastNameText.setError("at least 3 characters");
                valid = false;
            } else {
                _lastNameText.setError(null);
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _emailText.setError("enter a valid email address");
                valid = false;
            } else {
                _emailText.setError(null);
            }
        }

        return valid;
    }

}
