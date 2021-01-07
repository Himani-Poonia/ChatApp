package com.example.chatapp.selectfriend;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.common.Extras;
import com.example.chatapp.common.NodeNames;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SelectFriendActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<SelectFriendModel> selectFriendModelList;
    private SelectFriendAdapter adapter;
    private View progressBar;

    private DatabaseReference databaseReferenceUsers, databaseReferenceChats;

    private FirebaseUser currentUser;
    private ValueEventListener valueEventListener;

    private String selectedMessage, selectedMessageId, selectedMessageType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend);

        setTitle(getString(R.string.forward_to));

        if(getIntent().hasExtra(Extras.MESSAGE)) {
            selectedMessage = getIntent().getStringExtra(Extras.MESSAGE);
            selectedMessageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
            selectedMessageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);
        }

        recyclerView = findViewById(R.id.selectFriendRecyclerView);
        progressBar = findViewById(R.id.progressBar);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        selectFriendModelList = new ArrayList<>();
        adapter = new SelectFriendAdapter(this, selectFriendModelList);
        recyclerView.setAdapter(adapter);

        progressBar.setVisibility(View.VISIBLE);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS).child(currentUser.getUid());
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot: snapshot.getChildren()) {
                    final String userId = dataSnapshot.getKey();
                    databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String userName = snapshot.child(NodeNames.NAME).getValue()!=null?
                                    snapshot.child(NodeNames.NAME).getValue().toString():"";

                            SelectFriendModel selectFriendModel = new SelectFriendModel(userId, userName, userId + "jpg");
                            selectFriendModelList.add(selectFriendModel);
                            adapter.notifyDataSetChanged();

                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(SelectFriendActivity.this,
                                    getString(R.string.failed_to_fetch_friends_list, error.getMessage()), Toast.LENGTH_SHORT).show();

                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SelectFriendActivity.this,
                        getString(R.string.failed_to_fetch_friends_list, error.getMessage()), Toast.LENGTH_SHORT).show();

                progressBar.setVisibility(View.GONE);
            }
        };

        databaseReferenceChats.addValueEventListener(valueEventListener);
    }

    public void returnSelectedFriend(String userId, String userName, String photoName) {
        databaseReferenceChats.removeEventListener(valueEventListener);

        Intent intent = new Intent();

        intent.putExtra(Extras.USER_KEY, userId);
        intent.putExtra(Extras.USER_NAME, userName);
        intent.putExtra(Extras.PHOTO_NAME, photoName);
        intent.putExtra(Extras.MESSAGE, selectedMessage);
        intent.putExtra(Extras.MESSAGE_ID, selectedMessageId);
        intent.putExtra(Extras.MESSAGE_TYPE, selectedMessageType);

        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}