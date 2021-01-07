package com.example.chatapp.signup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.chatapp.Login.LoginActivity;
import com.example.chatapp.R;
import com.example.chatapp.common.NodeNames;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText enterEmail,enterPassword,enterName,enterConfirmPassword;
    private String email,password,name,confirmPassword, photo;

    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;
    private StorageReference firebaseStorage;

    private ImageView profileImage;
    private Uri localFileUri, serverFileUri;
    private View prograssbarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        enterEmail = findViewById(R.id.enterEmail);
        enterPassword = findViewById(R.id.enterPassword);
        enterName = findViewById(R.id.enterName);
        enterConfirmPassword = findViewById(R.id.enterConfirmPassword);
        profileImage = findViewById(R.id.profileiv);
        prograssbarLayout = findViewById(R.id.progressBarLayout);

        firebaseStorage = FirebaseStorage.getInstance().getReference();
    }

    public void pickImage(View v){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //pick image from gallery
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 101);
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 102) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 101);
            }
            else {
                Toast.makeText(this, R.string.permission_required , Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 101){
            if(resultCode == RESULT_OK){  //if user selected any image
                localFileUri = data.getData();
                profileImage.setImageURI(localFileUri);
            }
        }
    }

    private void updateNameAndPhoto(){

        prograssbarLayout.setVisibility(View.VISIBLE);

        String fileNameString = firebaseUser.getUid() + ".jpg";   //filename when uploaded on server

        final StorageReference storageReference = firebaseStorage.child("images/" + fileNameString);
        storageReference.putFile(localFileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                prograssbarLayout.setVisibility(View.GONE);

                if(task.isSuccessful()){

                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                serverFileUri = uri;

                                UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(enterName.getText().toString().trim())
                                        .setPhotoUri(Uri.parse(serverFileUri.getPath()))
                                        .build();

                                prograssbarLayout.setVisibility(View.VISIBLE);

                                firebaseUser.updateProfile(userProfileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        prograssbarLayout.setVisibility(View.GONE);

                                        if(task.isSuccessful()){

                                            String userID = firebaseUser.getUid();
                                            databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

                                            Map hashMap = new HashMap<>();

                                            hashMap.put(NodeNames.NAME, enterName.getText().toString().trim());
                                            hashMap.put(NodeNames.EMAIL, enterEmail.getText().toString().trim());
                                            hashMap.put(NodeNames.ONLINE, true);
                                            hashMap.put(NodeNames.PHOTO, photo);

                                            databaseReference.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    Toast.makeText(SignupActivity.this, R.string.user_created_successfully, Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                                }
                                            });

                                        } else{
                                            Toast.makeText(SignupActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        });
                } else {
                    Toast.makeText(SignupActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateNameOnly(){

        prograssbarLayout.setVisibility(View.VISIBLE);

        UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName(enterName.getText().toString().trim())
                .build();

        firebaseUser.updateProfile(userProfileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                prograssbarLayout.setVisibility(View.GONE);

                if(task.isSuccessful()){

                    String userID = firebaseUser.getUid();
                    databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

                    Map hashMap = new HashMap<>();

                    hashMap.put(NodeNames.NAME, enterName.getText().toString().trim());
                    hashMap.put(NodeNames.EMAIL, enterEmail.getText().toString().trim());
                    hashMap.put(NodeNames.ONLINE, true);
                    hashMap.put(NodeNames.PHOTO, "");

                    prograssbarLayout.setVisibility(View.VISIBLE);

                    databaseReference.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            prograssbarLayout.setVisibility(View.GONE);

                            Toast.makeText(SignupActivity.this, R.string.user_created_successfully, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        }
                    });

                } else{
                    Toast.makeText(SignupActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void signupClick(View view){

        email = enterEmail.getText().toString().trim(); //trim will remove spaces
        password = enterPassword.getText().toString().trim();
        name = enterName.getText().toString().trim();
        confirmPassword = enterConfirmPassword.getText().toString().trim();

        if(email.equals("")){
            enterEmail.setError(getString(R.string.enter_email));
        }
        else if(password.equals("")){
            enterPassword.setError(getString(R.string.enter_password));
        }
        else if(name.equals("")){
            enterName.setError(getString(R.string.enter_name));
        }
        else if(confirmPassword.equals("")){
            enterConfirmPassword.setError(getString(R.string.confirm_password));
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            enterEmail.setError(getString(R.string.enter_correct_email));
        }
        else if(!password.equals(confirmPassword)){
            enterPassword.setError(getString(R.string.password_mismatch));
        }
        else{

            prograssbarLayout.setVisibility(View.VISIBLE);

            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

            firebaseAuth.createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    prograssbarLayout.setVisibility(View.GONE);

                    if(task.isSuccessful()){

                        firebaseUser = firebaseAuth.getCurrentUser();
                        photo = firebaseUser.getUid() + ".jpg";
                        if(localFileUri == null)
                            updateNameOnly();
                        else
                            updateNameAndPhoto();

                    } else {
                        Toast.makeText(SignupActivity.this, getString(R.string.signup_failed, task.getException()), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
   }
}