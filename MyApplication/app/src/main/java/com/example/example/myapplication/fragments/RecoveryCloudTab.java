package com.example.example.myapplication.fragments;

import android.app.Activity;
import android.content.Context;
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
import com.example.example.myapplication.utils.Tuple;
import com.example.example.myapplication.utils.Utils;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.crypto.sse.CryptoPrimitives;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

public class RecoveryCloudTab extends Fragment implements View.OnClickListener {

    @BindView(R.id.input_code) EditText _code;
    @BindView(R.id.input_email_recovery) EditText _email;
    @BindView(R.id.recover_password_next) Button _next;
    @BindView(R.id.recover_password_next2) Button _next2;
    @BindView(R.id.question_1_text) TextView _question1Text;
    @BindView(R.id.question_2_text) TextView _question2Text;
    @BindView(R.id.question_1_answer) EditText _question1Answer;
    @BindView(R.id.question_2_answer) EditText _question2Answer;
    @BindView(R.id.recover_password_final) Button _final;
    @BindView(R.id.password_text) TextView _passwordText;

    private byte[] enc;
    private byte[] salt;
    private Activity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_recover_password_cloud, container, false);
        ButterKnife.bind(this, view);
        _next.setOnClickListener(this);
        _next2.setOnClickListener(this);
        _final.setOnClickListener(this);
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
            case R.id.recover_password_next:
                requestRecovery();
                break;
            case R.id.recover_password_next2:
                recoverPassword();
                break;
            case R.id.recover_password_final:
                decryptPassword();
                break;
        }
    }

    // TODO: find a cleaner way to make a dynamic UI
    private void requestRecovery() {
        String email = _email.getText().toString();
        DynRH2LevClientWrapper.requestRecovery(mActivity, email, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.i("RECOVERY", new String(responseBody));
                Toast.makeText(mActivity, "Check your email for your code", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.i("RECOVERY", new String(responseBody));
                Toast.makeText(mActivity, "Could not send email", Toast.LENGTH_SHORT).show();
            }
        });

        _email.setText("");
        _email.setHint("");
        _email.setVisibility(View.GONE);
        ((ViewGroup) _email.getParent()).removeView(_email);
        _code.setVisibility(View.VISIBLE);
        ((ViewGroup) _next.getParent()).removeView(_next);
        _next2.setVisibility(View.VISIBLE);
    }

    private void recoverPassword() {
        String code = _code.getText().toString();
        DynRH2LevClientWrapper.recoveryPassword(mActivity, code, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String encString = response.getString("enc");
                    String saltString = response.getString("salt");
                    enc = Utils.JSONToByteArray(encString);
                    salt = Utils.JSONToByteArray(saltString);
                    List<Tuple<String, Integer>> questions = new ArrayList<>();
                    JSONArray questionsJson = response.getJSONArray("questions");
                    int numQuestions = questionsJson.length();
                    for (int i = 0; i < numQuestions; i++) {
                        JSONObject temp = questionsJson.getJSONObject(i);
                        questions.add(new Tuple<>(temp.getString("question"), temp.getInt("number")));
                    }
                    Collections.sort(questions, new Comparator<Tuple<String, Integer>>() {
                        @Override
                        public int compare(Tuple<String, Integer> o1, Tuple<String, Integer> o2) {
                            return o1.v - o2.v;
                        }
                    });
                    Log.i("QUESTIONS",questions.toString());

                    _code.setText("");
                    _code.setHint("");
                    ((ViewGroup) _code.getParent()).removeView(_code);
                    ((ViewGroup) _next2.getParent()).removeView(_next2);;
                    _question1Text.setText(questions.get(0).k);
                    _question2Text.setText(questions.get(1).k);
                    _question1Text.setVisibility(View.VISIBLE);
                    _question2Text.setVisibility(View.VISIBLE);
                    _question1Answer.setVisibility(View.VISIBLE);
                    _question2Answer.setVisibility(View.VISIBLE);
                    _final.setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.i("RECOVERY", responseString);
            }
        });
    }

    private void decryptPassword() {
        String question1Answer = _question1Answer.getText().toString();
        String question2Answer = _question2Answer.getText().toString();
        _question1Answer.setText("");
        _question2Answer.setText("");
        ((ViewGroup) _question1Answer.getParent()).removeView(_question1Answer);
        ((ViewGroup) _question2Answer.getParent()).removeView(_question2Answer);
        ((ViewGroup) _question1Text.getParent()).removeView(_question1Text);
        ((ViewGroup) _question2Text.getParent()).removeView(_question2Text);
        ((ViewGroup) _final.getParent()).removeView(_final);
        _passwordText.setVisibility(View.VISIBLE);

        try {
            byte[] key = CryptoPrimitives.keyGenSetM(question1Answer + question2Answer, salt, 100, 128);
            byte[] passwordBytes = CryptoPrimitives.decryptAES_CTR_String(enc, key);
            _passwordText.setText(new String(passwordBytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
