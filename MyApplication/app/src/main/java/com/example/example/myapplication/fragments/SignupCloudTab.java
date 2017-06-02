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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.example.myapplication.R;
import com.example.example.myapplication.remote.DynRH2LevClientWrapper;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.Utils;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.crypto.sse.CryptoPrimitives;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

/**
 * The tab for the cloud signup.
 */
public class SignupCloudTab extends Fragment implements View.OnClickListener {

    @BindView(R.id.input_first_name) EditText _firstNameText;
    @BindView(R.id.input_last_name) EditText _lastNameText;
    @BindView(R.id.input_email) EditText _emailText;
    @BindView(R.id.input_password) EditText _passwordText;
    @BindView(R.id.btn_signup) Button _signupButton;
    @BindView(R.id.link_login) TextView _loginLink;
    @BindView(R.id.security_spinner) Spinner _securitySpinner;
    @BindView(R.id.security_spinner2) Spinner _securitySpinner2;
    @BindView(R.id.input_security) EditText _securityText;
    @BindView(R.id.input_security2) EditText _securityText2;

    private Activity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_signup_cloud, container, false);
        ButterKnife.bind(this, view);
        _signupButton.setOnClickListener(this);
        _loginLink.setOnClickListener(this);
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
            case R.id.btn_signup:
                signup();
                break;
            case R.id.link_login:
                backToLogin();
                break;
        }
    }

    public void signup() {

        if (!validate()) {
            onSignupFailed();
            return;
        }

        _signupButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(mActivity);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        String firstName = _firstNameText.getText().toString();
        String lastName = _lastNameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();
        String securityQuestion = _securitySpinner.getSelectedItem().toString();
        String securityAnswer = _securityText.getText().toString();
        String securityQuestion2 = _securitySpinner2.getSelectedItem().toString();
        String securityAnswer2 = _securityText2.getText().toString();
        String clientSalt = Utils.byteArrayToJSON(CryptoPrimitives.randomBytes(16));

        Utils.putCurrentEmailInSharedPref(mActivity, email);

        /*DynRH2LevClientWrapper.signup(firstName, lastName, email, password, securityQuestion, securityAnswer,
                securityQuestion2, securityAnswer2, clientSalt, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Log.i("SIGNUP", "status code success " + statusCode);
                        onSignupSuccess(new String(responseBody));
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.i("SIGNUP", "status code failure " + statusCode);
                        onSignupFailed();
                        progressDialog.dismiss();
                    }
                });*/
        DynRH2LevClientWrapper.signup(mActivity, firstName, lastName, email, password, securityQuestion, securityAnswer,
                securityQuestion2, securityAnswer2, clientSalt, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        Log.i("SIGNUP", "status code success " + statusCode);
                        onSignupSuccess(response);
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.i("SIGNUP", "status code failure " + statusCode);
                        onSignupFailed();
                        progressDialog.dismiss();
                    }
                });
    }

    public void backToLogin() {
        mActivity.finish();
    }

    public void onSignupSuccess(JSONObject salts) {
        _signupButton.setEnabled(true);
        // set our state
        try {
            SharedPreferences sharedPref = Utils.getUserSharedPreference(mActivity);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(Const.SHARED_PREF_STATE, Utils.stateToJSON(new HashMap<String, Integer>()));
            editor.putString(Const.SHARED_PREF_SALT, salts.getString(Const.SHARED_PREF_SALT));
            editor.putString(Const.LOCAL_PASSWORD_SALT_LABEL, salts.getString(Const.LOCAL_PASSWORD_SALT_LABEL));
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
            onSignupFailed();
            return;
        }

        // create user directory
        File dir = Utils.getCurrentUserDir(mActivity);
        if (!dir.exists()) {
            dir.mkdir();
        }

        Toast.makeText(mActivity.getBaseContext(), "Signed up! Go to your email to validate your account", Toast.LENGTH_LONG).show();
        Log.i("Signup", "success");
        mActivity.onBackPressed();
    }

    public void onSignupFailed() {
        Toast.makeText(mActivity.getBaseContext(), "Signup failed", Toast.LENGTH_LONG).show();
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

        return valid;
    }

}
