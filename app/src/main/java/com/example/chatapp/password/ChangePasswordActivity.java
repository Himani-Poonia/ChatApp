package com.example.chatapp.password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.Login.LoginActivity;
import com.example.chatapp.MessageActivity;
import com.example.chatapp.R;
import com.example.chatapp.common.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText passwordEditText,confirmPasswordEditText;
    private View prograssbarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        passwordEditText = findViewById(R.id.enterPassword);
        confirmPasswordEditText = findViewById(R.id.enterConfirmPassword);
        prograssbarLayout = findViewById(R.id.progressBarLayout);
    }

    public void changePasswordButtonClick(View v) {

        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if(password.equals("")){
            passwordEditText.setError(getString(R.string.enter_password));
        }
        else if(confirmPassword.equals("")){
            confirmPasswordEditText.setError(getString(R.string.confirm_password));
        }
        else if(!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.password_mismatch));
        }
        else {

            prograssbarLayout.setVisibility(View.VISIBLE);

            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

            if(firebaseUser != null) {
                firebaseUser.updatePassword(password).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        prograssbarLayout.setVisibility(View.GONE);

                        if(task.isSuccessful()) {
                            Toast.makeText(ChangePasswordActivity.this, R.string.password_changed_successfully , Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(ChangePasswordActivity.this, getString(R.string.something_went_wrong, task.getException()), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }
}