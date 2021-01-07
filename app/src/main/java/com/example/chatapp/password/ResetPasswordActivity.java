package com.example.chatapp.password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.chatapp.Login.LoginActivity;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.R;
import com.example.chatapp.common.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextView messageTextView;
    private LinearLayout messageLinearLayout, resetPasswordLinearLayout;
    private Button retryButton;
    private View prograssbarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        emailEditText = findViewById(R.id.enterEmail);
        messageTextView = findViewById(R.id.messageTextView);
        messageLinearLayout = findViewById(R.id.messageLinearLayout);
        resetPasswordLinearLayout = findViewById(R.id.resetPasswordLinearLayout);
        retryButton = findViewById(R.id.retryButton);
        prograssbarLayout = findViewById(R.id.progressBarLayout);
    }

    public void resetPasswordCllick(View v) {
        String email = emailEditText.getText().toString().trim();

        if(email.equals("")){
            emailEditText.setError(getString(R.string.enter_email));
        } else {

            prograssbarLayout.setVisibility(View.VISIBLE);

            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

            firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    prograssbarLayout.setVisibility(View.GONE);

                    resetPasswordLinearLayout.setVisibility(View.GONE);
                    messageLinearLayout.setVisibility(View.VISIBLE);

                    if(task.isSuccessful()) {
                        messageTextView.setText(getString(R.string.reset_password_instructions, email));

                        new CountDownTimer(60000,1000){

                            @Override
                            public void onTick(long millisUntilFinished) {

                                retryButton.setText(getString(R.string.resend_timer, String.valueOf(millisUntilFinished/1000)));
                                retryButton.setOnClickListener(null);
                            }

                            @Override
                            public void onFinish() {
                                retryButton.setText(R.string.retry);

                                retryButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        resetPasswordLinearLayout.setVisibility(View.VISIBLE);
                                        messageLinearLayout.setVisibility(View.GONE);
                                    }
                                });
                            }
                        }.start();

                    } else {
                        messageTextView.setText(getString(R.string.email_sent_failed, task.getException()));

                        retryButton.setText(R.string.retry);

                        retryButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                resetPasswordLinearLayout.setVisibility(View.VISIBLE);
                                messageLinearLayout.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            });
        }
    }

    public void closeButtonClick(View v) {
        finish();
    }
}