package com.example.chatapp.requests;

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
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class RequestsFragment extends Fragment {

    private RecyclerView requestRecyclerView;
    private List<RequestModel> requestModelList;
    private RequestsAdapter requestsAdapter;
    private TextView emptyTextView;

    private DatabaseReference databaseReferenceRequests, databaseReferenceUsers;
    private FirebaseUser currentUser;
    private View progressBar;

    public RequestsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestRecyclerView = view.findViewById(R.id.requestsRecyclerView);
        emptyTextView = view.findViewById(R.id.emptyRequestsListTextView);
        progressBar = view.findViewById(R.id.progressBar);

        requestRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        requestModelList = new ArrayList<>();
        requestsAdapter = new RequestsAdapter(getActivity(),requestModelList);
        requestRecyclerView.setAdapter(requestsAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

        databaseReferenceRequests = FirebaseDatabase.getInstance().getReference()
                            .child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());

        emptyTextView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        databaseReferenceRequests.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                requestModelList.clear();

                for(DataSnapshot snaps : snapshot.getChildren()) {
                    if(snaps.exists()){

                        String requestType = snaps.child(NodeNames.REQUEST_TYPE).getValue().toString();

                        if(requestType.equals(Constants.REQUEST_DATA_RECEIVED)) {
                            String userId = snaps.getKey();

                            databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String userName = snapshot.child(NodeNames.NAME).getValue().toString();
                                    String photoName = "";

                                    if(snapshot.child(NodeNames.PHOTO).getValue()!=null)
                                        photoName=snapshot.child(NodeNames.PHOTO).getValue().toString();

                                    RequestModel requestModel = new RequestModel(userId, userName, photoName);
                                    requestModelList.add(requestModel);
                                    requestsAdapter.notifyDataSetChanged();
                                    emptyTextView.setVisibility(View.GONE);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getActivity(), getActivity().getString(R.string.failed_to_fetch_friend_requests,
                                            error.getMessage()), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getActivity(), getActivity().getString(R.string.failed_to_fetch_friend_requests,
                        error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }
}