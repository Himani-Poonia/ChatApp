package com.example.chatapp.profile;

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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.Login.LoginActivity;
import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.password.ChangePasswordActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText enterEmail,enterName;
    private String email,name;

    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;
    private StorageReference firebaseStorage;

    private ImageView profileImage;
    private Uri localFileUri, serverFileUri;
    private FirebaseAuth firebaseAuth;
    private View prograssbarLayout;
    String photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        enterEmail = findViewById(R.id.enterEmail);
        enterName = findViewById(R.id.enterName);
        profileImage = findViewById(R.id.profileiv);
        prograssbarLayout = findViewById(R.id.progressBarLayout);


        firebaseStorage = FirebaseStorage.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();

        firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            photo=firebaseUser.getUid()+".jpg";
            enterName.setText(firebaseUser.getDisplayName());
            enterEmail.setText(firebaseUser.getEmail());
            serverFileUri = firebaseUser.getPhotoUrl();

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            DatabaseReference databaseReference = rootRef.child(NodeNames.USERS).child(firebaseUser.getUid());

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String uri = snapshot.child(NodeNames.PHOTO).getValue()!=null?
                            snapshot.child(NodeNames.PHOTO).getValue().toString():"";

                    if(!uri.equals("")) {
                        firebaseStorage.child(Constants.IMAGES_FOLDER).child(photo).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(ProfileActivity.this)
                                        .load(uri)
                                        .placeholder(R.drawable.default_profile)
                                        .error(R.drawable.default_profile)
                                        .into(profileImage);
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

    }

    public void logoutClick(View v) {

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference databaseReference = rootRef.child(NodeNames.TOKEN).child(currentUser.getUid());

        databaseReference.setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    firebaseAuth.signOut();
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                    finish();
                }
                else {
                    Toast.makeText(ProfileActivity.this, getString(R.string.something_went_wrong, task.getException()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void saveProfileClick(View v) {
        if(enterName.getText().equals("")) {
            enterName.setError(getString(R.string.enter_name));
        }
        else
        {
            prograssbarLayout.setVisibility(View.VISIBLE);
            if(localFileUri != null){
                updateNameAndPhoto();
            } else {
                updateNameOnly();
            }
        }
    }

    public void changeImage(View v){

        if(serverFileUri == null){
            pickImage();
        } else{
            PopupMenu popupMenu = new PopupMenu(this,v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_picture, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    if(item.getItemId() == R.id.changePicture) {
                        pickImage();
                    }
                    else if(item.getItemId() == R.id.removePicture) {
                        removePhoto();
                    }

                    return false;
                }
            });

            popupMenu.show();
        }
    }

    private void removePhoto(){

        prograssbarLayout.setVisibility(View.VISIBLE);

        UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName(enterName.getText().toString().trim())
                .setPhotoUri(null)
                .build();

        firebaseUser.updateProfile(userProfileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                prograssbarLayout.setVisibility(View.GONE);

                if(task.isSuccessful()){

                    String userID = firebaseUser.getUid();
                    databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

                    databaseReference.child(userID).child(NodeNames.PHOTO).setValue("").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Glide.with(ProfileActivity.this)
                                    .load(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .placeholder(R.drawable.default_profile)
                                    .into(profileImage);

                            Toast.makeText(ProfileActivity.this, R.string.photo_removed_successfully, Toast.LENGTH_SHORT).show();
                        }
                    });

                } else{
                    Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void pickImage(){
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
                                    .setPhotoUri(serverFileUri)
                                    .build();

                            firebaseUser.updateProfile(userProfileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){

                                        String userID = firebaseUser.getUid();
                                        databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

                                        Map hashMap = new HashMap<>();

                                        hashMap.put(NodeNames.EMAIL, enterEmail.getText().toString().trim());
                                        hashMap.put(NodeNames.NAME, enterName.getText().toString().trim());
                                        hashMap.put(NodeNames.ONLINE, true);
                                        hashMap.put(NodeNames.PHOTO, photo);

                                        databaseReference.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                prograssbarLayout.setVisibility(View.GONE);
                                                finish();
                                            }
                                        });

                                    } else{
                                        Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
                } else {
                    Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_SHORT).show();
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

                    String name = enterName.getText().toString().trim();

                    databaseReference.child(userID).child(NodeNames.NAME).setValue(name).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            prograssbarLayout.setVisibility(View.GONE);
                            finish();
                        }
                    });

                } else{
                    Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void changePasswordClick(View v) {
        startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
    }
}