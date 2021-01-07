package com.example.chatapp.chats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.Extras;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.common.Util;
import com.example.chatapp.profile.ProfileActivity;
import com.example.chatapp.selectfriend.SelectFriendActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static android.content.Intent.ACTION_PICK;
import static android.content.Intent.EXTRA_STREAM;
import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView sendImageView,attachmentImageView,profileImageView;
    private TextView userNameTextView, userStatusTextView;
    private EditText msgEditText;
    private DatabaseReference rootReference;
    private FirebaseAuth firebaseAuth;
    private String currentUserId, chatUserId;

    private RecyclerView recyclerView;
    private List<MessageModel> messageModelList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MessagesAdapter messagesAdapter;

    private int currentPage = 1;
    private static final int RECORD_PER_PAGE = 30;

    private static final int REQUEST_CODE_PICK_IMAGE=101;
    private static final int REQUEST_CODE_CAPTURE_IMAGE=102;
    private static final int REQUEST_CODE_PICK_VIDEO=103;
    private static final int REQUEST_CODE_FORWARD_MESSAGE = 104;

    private DatabaseReference databaseReferenceMsgs;
    private ChildEventListener childEventListener;

    private BottomSheetDialog bottomSheetDialog;

    private LinearLayout progressLinearLayout;
    private String username,photoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //custom action bar
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setTitle("");
            ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_action_bar, null);

            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true); //back arrow on action bar
            actionBar.setElevation(0);

            actionBar.setCustomView(actionBarLayout);
            actionBar.setDisplayOptions(actionBar.getDisplayOptions()|ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        profileImageView = findViewById(R.id.profileImageView);
        userNameTextView = findViewById(R.id.userNameTextView);
        sendImageView = findViewById(R.id.sendImageView);
        msgEditText = findViewById(R.id.messageEditText);
        attachmentImageView = findViewById(R.id.attachmentImageView);
        userStatusTextView = findViewById(R.id.userStatusTextView);

        progressLinearLayout = findViewById(R.id.progressLinearLayout);
        sendImageView.setOnClickListener(this);
        attachmentImageView.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        rootReference = FirebaseDatabase.getInstance().getReference();
        currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

        if(getIntent().hasExtra(Extras.USER_KEY)){
            chatUserId = getIntent().getStringExtra(Extras.USER_KEY);
            photoName=chatUserId + ".jpg";

            //set profile image
            DatabaseReference databaseReference = rootReference.child(NodeNames.USERS).child(chatUserId);

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String url = snapshot.child(NodeNames.PHOTO).getValue()!=null?
                            snapshot.child(NodeNames.PHOTO).getValue().toString():"";

                    if(!url.equals("")) {
                        StorageReference firebaseStorage = FirebaseStorage.getInstance().getReference();
                        firebaseStorage.child(Constants.IMAGES_FOLDER).child(photoName).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(ChatActivity.this)
                                        .load(uri)
                                        .placeholder(R.drawable.default_profile)
                                        .error(R.drawable.default_profile)
                                        .into(profileImageView);
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        if(getIntent().hasExtra(Extras.USER_NAME)){
            username = getIntent().getStringExtra(Extras.USER_NAME);
        }

        userNameTextView.setText(username);

        recyclerView = findViewById(R.id.messageRecyclerView);
        messageModelList = new ArrayList<>();
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        messagesAdapter = new MessagesAdapter(this, messageModelList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messagesAdapter);

        loadMessages();

        if(messageModelList.size() > 0)
            recyclerView.scrollToPosition(messageModelList.size()-1);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                currentPage++;
                loadMessages();
            }
        });

        //for attachment dialog
        bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.chat_file_options,null);
        view.findViewById(R.id.cameraLinearLayout).setOnClickListener(this);
        view.findViewById(R.id.galleryLinearLayout).setOnClickListener(this);
        view.findViewById(R.id.vcLinearLayout).setOnClickListener(this);
        view.findViewById(R.id.closeImageView).setOnClickListener(this);
        bottomSheetDialog.setContentView(view);

        //it will handle the case of forwarded message
        if(getIntent().hasExtra(Extras.MESSAGE) && getIntent().hasExtra(Extras.MESSAGE_ID) && getIntent().hasExtra(Extras.MESSAGE_TYPE)) {
            String message = getIntent().getStringExtra(Extras.MESSAGE);
            String messageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
            String messageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);

            DatabaseReference messageReference = rootReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
            String newMessageId = messageReference.getKey();

            if(messageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
                sendMessage(message, messageType, newMessageId);
            }
            else {
                StorageReference rootStorageRef = FirebaseStorage.getInstance().getReference();
                String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?
                        Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;

                String oldfileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?
                        messageId + ".mp4" : messageId + ".jpg";

                String newfileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?
                        newMessageId + ".mp4" : newMessageId + ".jpg";

                String localFilePath = getExternalFilesDir(null).getAbsolutePath() + "/" + oldfileName; //download with the same name as on firebase
                File localFile = new File(localFilePath);

                StorageReference newFileRef = rootStorageRef.child(folderName).child(newfileName);

                rootStorageRef.child(folderName).child(oldfileName).getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        UploadTask uploadTask = newFileRef.putFile(Uri.fromFile(localFile));
                        uploadProgress(uploadTask, newFileRef, newMessageId, messageType);
                    }
                });
            }
        }

        //set the unread count to 0 when this activity is created
        rootReference.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue("0");

        //set user status (online/offline)
        DatabaseReference databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS).child(chatUserId);
        databaseReferenceUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = "";

                if(snapshot.child(NodeNames.ONLINE).getValue() != null) {
                    status = snapshot.child(NodeNames.ONLINE).getValue().toString();
                }

                if(status.equals("true")) {
                    userStatusTextView.setText(Constants.STATUS_ONLINE);
                } else {
                    userStatusTextView.setText(Constants.STATUS_OFFLINE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        //set the user status to "typing" when the user is typing
        msgEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                DatabaseReference currentUserRef = rootReference.child(NodeNames.CHATS).child(currentUserId).child(chatUserId);
                if(s.toString().matches(""))
                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STOPPED);
                else
                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STARTED);
            }
        });

        DatabaseReference chatUserRef = rootReference.child(NodeNames.CHATS).child(chatUserId).child(currentUserId);
        chatUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(NodeNames.TYPING).getValue() != null) {
                    String typingStatus = snapshot.child(NodeNames.TYPING).getValue().toString();

                    if(typingStatus.equals(Constants.TYPING_STARTED))
                        userStatusTextView.setText(Constants.STATUS_TYPING);
                    else
                        userStatusTextView.setText(Constants.STATUS_ONLINE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(String msg, String msgType, String pushId) {
        try {
            if(!msg.equals("")) {
                HashMap msgHashmap = new HashMap();
                msgHashmap.put(NodeNames.MESSAGE_ID, pushId);
                msgHashmap.put(NodeNames.MESSAGE, msg);
                msgHashmap.put(NodeNames.MESSAGE_TYPE, msgType);
                msgHashmap.put(NodeNames.MESSAGE_FROM, currentUserId);
                msgHashmap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);

                String currentUserReference = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
                String chatUserReference = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;

                HashMap msgUserHashmap = new HashMap();
                msgUserHashmap.put(currentUserReference + "/" + pushId, msgHashmap);
                msgUserHashmap.put(chatUserReference + "/" + pushId, msgHashmap);

                msgEditText.setText("");

                rootReference.updateChildren(msgUserHashmap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if(error==null) {
                            Toast.makeText(ChatActivity.this, R.string.message_sent_successfully,
                                    Toast.LENGTH_SHORT).show();

                            String title = "";

                            if(msgType.equals(Constants.MESSAGE_TYPE_TEXT))
                                title = "New Message";
                            else if(msgType.equals(Constants.MESSAGE_TYPE_IMAGE))
                                title = "New Image";
                            else if(msgType.equals(Constants.MESSAGE_TYPE_VIDEO))
                                title = "New Video";

                            Util.sendNotification(ChatActivity.this, title, msg, chatUserId);

                            String lastMessage = !title.equals("New Message")?title:msg;
                            Util.updateChatDetails(ChatActivity.this, currentUserId, chatUserId, lastMessage);

                        } else {
                            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send_message, error.getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        } catch (Exception e) {
            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send_message, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMessages() {
        messageModelList.clear();
        databaseReferenceMsgs = rootReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);

        Query messageQuery = databaseReferenceMsgs.limitToLast(currentPage * RECORD_PER_PAGE);

        if(childEventListener!=null) {
            messageQuery.removeEventListener(childEventListener);
        }

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                MessageModel messageModel = snapshot.getValue(MessageModel.class);  //for this, empty constructor will be required

                messageModelList.add(messageModel);
                messagesAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messageModelList.size()-1);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                loadMessages();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                swipeRefreshLayout.setRefreshing(false);
            }
        };

        messageQuery.addChildEventListener(childEventListener);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.sendImageView:
                if(Util.connectionAvailable(this)) {
                    //push method will generate unique id for the child;
                    DatabaseReference userMessagePush = rootReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
                    String pushId = userMessagePush.getKey();
                    sendMessage(msgEditText.getText().toString().trim(), Constants.MESSAGE_TYPE_TEXT, pushId);
                } else {
                    Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.attachmentImageView:
                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                    if(bottomSheetDialog!=null){
                        bottomSheetDialog.show();
                    }
                } else {
                    ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }

                //close keyboard
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if(inputMethodManager!=null){
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(),0);
                }
                break;

            case R.id.cameraLinearLayout:
                bottomSheetDialog.dismiss();

                Intent intentCamera = new Intent(ACTION_IMAGE_CAPTURE);
                startActivityForResult(intentCamera, REQUEST_CODE_CAPTURE_IMAGE);
                break;

            case R.id.galleryLinearLayout:
                bottomSheetDialog.dismiss();

                Intent intentGallery = new Intent(ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentGallery, REQUEST_CODE_PICK_IMAGE);
                break;

            case R.id.vcLinearLayout:
                bottomSheetDialog.dismiss();

                Intent intentVideo = new Intent(ACTION_PICK,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentVideo, REQUEST_CODE_PICK_VIDEO);
                break;

            case R.id.closeImageView:
                bottomSheetDialog.cancel();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAPTURE_IMAGE) { //camera

                Bitmap bitmap = (Bitmap) data.getExtras().get("data");

                //convert to byteArray
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

                uploadByteArray(bytes, Constants.MESSAGE_TYPE_IMAGE);

            } else if (requestCode == REQUEST_CODE_PICK_IMAGE) {  //gallery

                Uri uri = data.getData();
                uploadFile(uri,Constants.MESSAGE_TYPE_IMAGE);

            } else if (requestCode == REQUEST_CODE_PICK_VIDEO) {  //video

                Uri uri = data.getData();
                uploadFile(uri,Constants.MESSAGE_TYPE_VIDEO);
            }
            else if(requestCode == REQUEST_CODE_FORWARD_MESSAGE) {
                Intent intent = new Intent(this, ChatActivity.class);

                intent.putExtra(Extras.USER_KEY, data.getStringExtra(Extras.USER_KEY));
                intent.putExtra(Extras.USER_NAME, data.getStringExtra(Extras.USER_NAME));
                intent.putExtra(Extras.PHOTO_NAME, data.getStringExtra(Extras.PHOTO_NAME));

                intent.putExtra(Extras.MESSAGE, data.getStringExtra(Extras.MESSAGE));
                intent.putExtra(Extras.MESSAGE_ID, data.getStringExtra(Extras.MESSAGE_ID));
                intent.putExtra(Extras.MESSAGE_TYPE, data.getStringExtra(Extras.MESSAGE_TYPE));

                startActivity(intent);
                finish();
            }
        }
    }

    //upload file with given uri
    private void uploadFile(Uri uri, String messageType) {
        DatabaseReference databaseReference = rootReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;
        String fileName =  messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?pushId+".mp4":pushId+".jpg";

        StorageReference fileReference = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileReference.putFile(uri);
        uploadProgress(uploadTask, fileReference, pushId, messageType);
    }

    //upload file with a given bitmap(byte Array)
    private void uploadByteArray(ByteArrayOutputStream bytes, String messageType) {
        DatabaseReference databaseReference = rootReference.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;
        String fileName =  messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?pushId+".mp4":pushId+".jpg";

        StorageReference fileReference = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileReference.putBytes(bytes.toByteArray());
        uploadProgress(uploadTask, fileReference, pushId, messageType);
    }

    //monitor the progress
    private void uploadProgress(final UploadTask task, StorageReference filePath, String pushId, String messageType) {
        View view = getLayoutInflater().inflate(R.layout.file_progress, null);
        ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
        TextView tvProgress = view.findViewById(R.id.tvFileProgress);
        ImageView pauseImageView = view.findViewById(R.id.pauseImageView);
        ImageView playImageView = view.findViewById(R.id.playImageView);
        ImageView cancelImageView = view.findViewById(R.id.cancelImageView);

        pauseImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                task.pause();
                playImageView.setVisibility(View.VISIBLE);
                pauseImageView.setVisibility(View.GONE);
            }
        });

        playImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                task.resume();
                playImageView.setVisibility(View.GONE);
                pauseImageView.setVisibility(View.VISIBLE);
            }
        });

        cancelImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                task.cancel();
            }
        });

        progressLinearLayout.addView(view);
        tvProgress.setText(getString(R.string.upload_progress, messageType, "0"));

        task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                double progress = (100.0 * snapshot.getBytesTransferred())/snapshot.getTotalByteCount();
                pbProgress.setProgress((int)progress);
                tvProgress.setText(getString(R.string.upload_progress, messageType, String.valueOf(pbProgress.getProgress())));
            }
        });

        task.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                progressLinearLayout.removeView(view);

                if(task.isSuccessful()) {
                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String downloadUri = uri.toString();
                            sendMessage(downloadUri, messageType, pushId);
                        }
                    });
                }
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressLinearLayout.removeView(view);
                Toast.makeText(ChatActivity.this, getString(R.string.upload_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==1){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                if(bottomSheetDialog!=null){
                    bottomSheetDialog.show();
                }
            } else{
                Toast.makeText(this, "Permission required to access files", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //to handle back arrow

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:  //id of back arrow set by android
                finish();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void deleteMessage(String messageId, String messageType) {
        DatabaseReference databaseReference = rootReference.child(NodeNames.MESSAGES)
                .child(currentUserId).child(chatUserId).child(messageId);

        databaseReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    DatabaseReference databaseReferenceChatuser = rootReference.child(NodeNames.MESSAGES)
                            .child(chatUserId).child(currentUserId).child(messageId);

                    databaseReferenceChatuser.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                Toast.makeText(ChatActivity.this, getString(R.string.message_deleted_successfully), Toast.LENGTH_SHORT).show();

                                if(!messageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
                                    StorageReference rootRef = FirebaseStorage.getInstance().getReference();
                                    String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)
                                            ?Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;
                                    String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)
                                            ?messageId+".mp4":messageId+".jpg";

                                    StorageReference fileRef = rootRef.child(folderName).child(fileName);
                                    fileRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(!task.isSuccessful()){
                                                Toast.makeText(ChatActivity.this,
                                                        getString(R.string.failed_to_delete_file,task.getException()), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }

                            } else {
                                Toast.makeText(ChatActivity.this, getString(R.string.failed_to_delete_message,task.getException()), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else{
                    Toast.makeText(ChatActivity.this, getString(R.string.failed_to_delete_message,task.getException()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void downloadFile(String messageId, String messageType, boolean isShare) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        else {
            String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;
            String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?messageId + ".mp4" : messageId + ".jpg";

            StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(folderName).child(fileName);
            String localFilePath = getExternalFilesDir(null).getAbsolutePath() + "/" + fileName;

            File localFile = new File(localFilePath);  //create object of file

            try {
                if(localFile.exists() || localFile.createNewFile()) {
                    FileDownloadTask downloadTask = fileRef.getFile(localFile);

                    View view = getLayoutInflater().inflate(R.layout.file_progress, null);
                    ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
                    TextView tvProgress = view.findViewById(R.id.tvFileProgress);
                    ImageView pauseImageView = view.findViewById(R.id.pauseImageView);
                    ImageView playImageView = view.findViewById(R.id.playImageView);
                    ImageView cancelImageView = view.findViewById(R.id.cancelImageView);

                    pauseImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            downloadTask.pause();
                            playImageView.setVisibility(View.VISIBLE);
                            pauseImageView.setVisibility(View.GONE);
                        }
                    });

                    playImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            downloadTask.resume();
                            playImageView.setVisibility(View.GONE);
                            pauseImageView.setVisibility(View.VISIBLE);
                        }
                    });

                    cancelImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            downloadTask.cancel();
                        }
                    });

                    progressLinearLayout.addView(view);
                    tvProgress.setText(getString(R.string.download_progress, messageType, "0"));

                    downloadTask.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull FileDownloadTask.TaskSnapshot snapshot) {
                            double progress = (100.0 * snapshot.getBytesTransferred())/snapshot.getTotalByteCount();
                            pbProgress.setProgress((int)progress);
                            tvProgress.setText(getString(R.string.download_progress, messageType, String.valueOf(pbProgress.getProgress())));
                        }
                    });

                    downloadTask.addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                            progressLinearLayout.removeView(view);

                            if(task.isSuccessful()) {
                                if(isShare) {
                                    Intent intentShare = new Intent();
                                    intentShare.setAction(Intent.ACTION_SEND);
                                    intentShare.putExtra(Intent.EXTRA_STREAM, Uri.parse(localFilePath));

                                    if(messageType.equals(Constants.MESSAGE_TYPE_VIDEO))
                                            intentShare.setType("video/mp4");

                                    if(messageType.equals(Constants.MESSAGE_TYPE_IMAGE))
                                        intentShare.setType("image/jpg");
                                    startActivity(Intent.createChooser(intentShare,getString(R.string.share_with)));
                                }
                                else {
                                    Snackbar snackbar = Snackbar.make(progressLinearLayout, getString(R.string.file_downloaded_successfully)
                                            , Snackbar.LENGTH_INDEFINITE);

                                    snackbar.setAction(R.string.view, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Uri uri = Uri.parse(localFilePath);
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            if (messageType.equals(Constants.MESSAGE_TYPE_VIDEO)) {
                                                intent.setDataAndType(uri, "video/mp4");
                                            } else if (messageType.equals(Constants.MESSAGE_TYPE_IMAGE)) {
                                                intent.setDataAndType(uri, "image/jpg");
                                            }

                                            startActivity(intent);
                                        }
                                    });

                                    snackbar.show();
                                }
                            }
                        }
                    });

                    downloadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressLinearLayout.removeView(view);
                            Toast.makeText(ChatActivity.this, getString(R.string.download_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    Toast.makeText(this, R.string.failed_to_store_file, Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Toast.makeText(ChatActivity.this, getString(R.string.download_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void forwardMessage(String selectedMessageId, String selectedMessage, String selectedMessageType) {
        Intent intent = new Intent(this, SelectFriendActivity.class);
        intent.putExtra(Extras.MESSAGE, selectedMessage);
        intent.putExtra(Extras.MESSAGE_ID, selectedMessageId);
        intent.putExtra(Extras.MESSAGE_TYPE, selectedMessageType);
        startActivityForResult(intent, REQUEST_CODE_FORWARD_MESSAGE);
    }

    @Override
    public void onBackPressed() {
        rootReference.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue("0");
        super.onBackPressed();
    }
}