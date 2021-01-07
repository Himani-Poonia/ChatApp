package com.example.chatapp.chats;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.common.NodeNames;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {
    private RecyclerView chatRecyclerView;
    private ChatListAdapter chatListAdapter;
    private View progressBar;
    private TextView emptyTextView;
    private List<ChatListModel> chatListModelList;
    private List<String> userIds;

    private DatabaseReference databaseReferenceChats, databaseReferenceUsers;
    private FirebaseUser currentUser;

    private ChildEventListener childEventListener;
    private Query query;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyTextView = view.findViewById(R.id.emptyChatListTextView);
        chatListModelList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(getActivity(), chatListModelList);

        userIds = new ArrayList<>();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(linearLayoutManager);
        chatRecyclerView.setAdapter(chatListAdapter);

        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS).child(currentUser.getUid());

        query = databaseReferenceChats.orderByChild(NodeNames.TIME_STAMP);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                updateList(snapshot, true, snapshot.getKey());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                updateList(snapshot, false, snapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };

        query.addChildEventListener(childEventListener);

        progressBar.setVisibility(View.VISIBLE);

        emptyTextView.setVisibility(View.VISIBLE);
    }

    private void updateList(DataSnapshot dataSnapshot, boolean isNew, String userId) {

        progressBar.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.GONE);

        final String lastMessage, lastMessageTime, unreadCount;

        lastMessage = dataSnapshot.child(NodeNames.LAST_MESSAGE).getValue()==null?
                "":dataSnapshot.child(NodeNames.LAST_MESSAGE).getValue().toString();

        lastMessageTime = dataSnapshot.child(NodeNames.LAST_MESSAGE_TIME).getValue()==null?
                "":dataSnapshot.child(NodeNames.LAST_MESSAGE_TIME).getValue().toString();

        unreadCount = dataSnapshot.child(NodeNames.UNREAD_COUNT).getValue()==null?
                        "0":dataSnapshot.child(NodeNames.UNREAD_COUNT).getValue().toString();

        databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String userName = snapshot.child(NodeNames.NAME).getValue()!=null?
                        snapshot.child(NodeNames.NAME).getValue().toString():"";

                String photoName = snapshot.child(NodeNames.PHOTO).getValue()!=null?
                        snapshot.child(NodeNames.PHOTO).getValue().toString():"";

                ChatListModel chatListModel = new ChatListModel(userId, userName, photoName,unreadCount, lastMessage, lastMessageTime);

                if(isNew) {
                    chatListModelList.add(chatListModel);
                    userIds.add(userId);
                }
                else {  //it means already existing child is changed
                    int indexOfClickedUser = userIds.indexOf(userId);
                    chatListModelList.set(indexOfClickedUser, chatListModel);
                }
                chatListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.failed_to_fetch_chat_list, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        query.removeEventListener(childEventListener);
    }
}