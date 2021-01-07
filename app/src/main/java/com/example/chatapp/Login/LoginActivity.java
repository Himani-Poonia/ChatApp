package com.example.chatapp.Login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.MainActivity;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.R;
import com.example.chatapp.common.Util;
import com.example.chatapp.password.ResetPasswordActivity;
import com.example.chatapp.signup.SignupActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdReceiver;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.installations.InstallationTokenResult;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText enterEmail,enterPassword;
    private String email,password;
    private View prograssbarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        enterEmail = findViewById(R.id.enterEmail);
        enterPassword = findViewById(R.id.enterPassword);
        prograssbarLayout = findViewById(R.id.progressBarLayout);
    }

    public void signupActivityClick(View view){
        startActivity(new Intent(LoginActivity.this, SignupActivity.class));
    }

    public void loginClick(View view){

        email = enterEmail.getText().toString().trim(); //trim will remove spaces
        password = enterPassword.getText().toString().trim();

        if(email.equals("")){
            enterEmail.setError(getString(R.string.enter_email));
        }
        else if(password.equals("")){
            enterPassword.setError(getString(R.string.enter_password));
        }
        else {

            if(Util.connectionAvailable(LoginActivity.this)) {
                prograssbarLayout.setVisibility(View.VISIBLE);

                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        prograssbarLayout.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            //generate token when the user login for the first time
                            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                                @Override
                                public void onComplete(@NonNull Task<String> task) {
                                    String token = task.getResult();
                                    Util.updateDeviceToken(LoginActivity.this, token);
                                }
                            });

                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed: " +
                                    task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            else {
                startActivity(new Intent(LoginActivity.this, MessageActivity.class));
            }
        }
    }

    public void resetPasswordClick(View v) {
        startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if(firebaseUser != null) {

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    String token = task.getResult();
                    Util.updateDeviceToken(LoginActivity.this, token);
                }
            });

            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }
}