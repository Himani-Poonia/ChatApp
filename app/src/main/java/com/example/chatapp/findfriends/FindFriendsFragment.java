package com.example.chatapp.findfriends;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.NodeNames;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class FindFriendsFragment extends Fragment {
    private RecyclerView findFriendsRecyclerView;
    private FindFriendsAdapter findFriendsAdapter;
    private TextView emptyListTextView;
    private List<FindFriendsModel> findFriendsModelList;

    private DatabaseReference reference, databaseReferenceFriendRequest;
    private FirebaseUser currentUser;
    private View progressBar;

    public FindFriendsFragment() {
        // Required empty public constructor

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findFriendsRecyclerView = view.findViewById(R.id.findFriendsRecyclerView);
        emptyListTextView = view.findViewById(R.id.emptyFindFriendsListTextView);
        progressBar = view.findViewById(R.id.progressBar);

        findFriendsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        findFriendsModelList = new ArrayList<>();
        findFriendsAdapter = new FindFriendsAdapter(getActivity(), findFriendsModelList);
        findFriendsRecyclerView.setAdapter(findFriendsAdapter);

        reference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        databaseReferenceFriendRequest = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());

        emptyListTextView.setVisibility(View.VISIBLE);

        if(findFriendsModelList.size() > 0)
            progressBar.setVisibility(View.VISIBLE);

        Query query = reference.orderByChild(NodeNames.NAME);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                findFriendsModelList.clear();
                for(DataSnapshot snaps: snapshot.getChildren()) {
                    final String userId = snaps.getKey();

                    if(userId.equals(currentUser.getUid()))
                        continue;

                    if(snaps.child(NodeNames.NAME).getValue() != null) {
                        final String fullName = snaps.child(NodeNames.NAME).getValue().toString();
                        String finalPhotoName = snaps.child(NodeNames.PHOTO).getValue()!=null?
                                snaps.child(NodeNames.PHOTO).getValue().toString():"";

                        databaseReferenceFriendRequest.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(snapshot.exists()){
                                    String requestType = snapshot.child(NodeNames.REQUEST_TYPE).getValue().toString();

                                    if(requestType.equals(Constants.REQUEST_DATA_SENT)){
                                        findFriendsModelList.add(new FindFriendsModel(fullName, finalPhotoName, userId, true));
                                        findFriendsAdapter.notifyDataSetChanged();
                                    }
                                }
                                else {
                                    findFriendsModelList.add(new FindFriendsModel(fullName, finalPhotoName, userId, false));
                                    findFriendsAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                progressBar.setVisibility(View.GONE);
                            }
                        });

                        emptyListTextView.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), R.string.failed_to_fetch_friends
                        , Toast.LENGTH_SHORT).show();
            }
        });
    }
}