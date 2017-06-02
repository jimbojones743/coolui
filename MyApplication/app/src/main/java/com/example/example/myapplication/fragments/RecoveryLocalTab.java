package com.example.example.myapplication.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.example.myapplication.R;
import com.example.example.myapplication.utils.Const;
import com.example.example.myapplication.utils.Utils;

import org.crypto.sse.CryptoPrimitives;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecoveryLocalTab extends Fragment implements View.OnClickListener {

    @BindView(R.id.question_1_text_local) TextView _question1Text;
    @BindView(R.id.question_2_text_local) TextView _question2Text;
    @BindView(R.id.question_1_answer_local) EditText _question1Answer;
    @BindView(R.id.question_2_answer_local) EditText _question2Answer;
    @BindView(R.id.recover_password_final_local) Button _final;
    @BindView(R.id.password_text_local) TextView _passwordText;

    private Activity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.activity_recover_password_local, container, false);
        ButterKnife.bind(this, view);
        _final.setOnClickListener(this);
        SharedPreferences pref = Utils.getLocalSharedPref(mActivity);
        _question1Text.setText(pref.getString(Const.Local.SECURITY_QUESTION_1_LABEL, "Could not find question"));
        _question2Text.setText(pref.getString(Const.Local.SECURITY_QUESTION_2_LABEL, "Could not find question"));
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
            case R.id.recover_password_final_local:
                decryptPassword();
                break;
        }
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
            SharedPreferences pref = Utils.getLocalSharedPref(mActivity);
            byte[] salt = Utils.JSONToByteArray(pref.getString(Const.Local.PASSWORD_RECOVERY_SALT_LABEL, "[]"));
            byte[] enc = Utils.JSONToByteArray(pref.getString(Const.Local.PASSWORD_ENC_LABEL, "[]"));
            byte[] key = CryptoPrimitives.keyGenSetM(question1Answer + question2Answer, salt, 100, 128);
            byte[] passwordBytes = CryptoPrimitives.decryptAES_CTR_String(enc, key);
            _passwordText.setText(new String(passwordBytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
