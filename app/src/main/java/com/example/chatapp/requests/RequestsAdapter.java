package com.example.chatapp.requests;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.common.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {
    private Context context;
    private List<RequestModel> requestModelList;
    private DatabaseReference databaseReferenceFriendRequest, databaseReferenceChats;
    private FirebaseUser currentUser;

    public RequestsAdapter(Context context, List<RequestModel> requestModelList) {
        this.context = context;
        this.requestModelList = requestModelList;
    }

    @NonNull
    @Override
    public RequestsAdapter.RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_request_list_item, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestsAdapter.RequestViewHolder holder, int position) {
        RequestModel requestModel = requestModelList.get(position);

        holder.usernameTextView.setText(requestModel.getUserName());
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(Constants.IMAGES_FOLDER + "/" + requestModel.getPhotoName());
        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(context)
                        .load(uri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(holder.profileImageView);
            }
        });

        databaseReferenceFriendRequest = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS);
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        holder.acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.denyButton.setVisibility(View.GONE);
                holder.acceptButton.setVisibility(View.GONE);

                String userId = requestModel.getUserId();
                databaseReferenceChats.child(currentUser.getUid()).child(userId).child(NodeNames.TIME_STAMP)
                        .setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {

                            databaseReferenceChats.child(userId).child(currentUser.getUid()).child(NodeNames.TIME_STAMP)
                                    .setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {

                                        databaseReferenceFriendRequest.child(currentUser.getUid()).child(userId).child(NodeNames.REQUEST_TYPE)
                                                .setValue(Constants.REQUEST_DATA_ACCEPTED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()) {

                                                    databaseReferenceFriendRequest.child(userId).child(currentUser.getUid()).child(NodeNames.REQUEST_TYPE)
                                                            .setValue(Constants.REQUEST_DATA_ACCEPTED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if(task.isSuccessful()){

                                                                String title = "Firend Request Accepted";
                                                                String message = "Friend request accepted by  " + currentUser.getDisplayName();
                                                                Util.sendNotification(context, title, message, userId);

                                                                holder.progressBar.setVisibility(View.GONE);
                                                                holder.denyButton.setVisibility(View.VISIBLE);
                                                                holder.acceptButton.setVisibility(View.VISIBLE);

                                                            } else {
                                                                handleException(holder, task.getException());
                                                            }
                                                        }
                                                    });

                                                } else {
                                                    handleException(holder, task.getException());
                                                }
                                            }
                                        });

                                    } else {
                                        handleException(holder, task.getException());
                                    }
                                }
                            });
                        } else {
                            handleException(holder, task.getException());
                        }
                    }
                });
            }
        });

        holder.denyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.denyButton.setVisibility(View.GONE);
                holder.acceptButton.setVisibility(View.GONE);

                databaseReferenceFriendRequest.child(currentUser.getUid()).child(requestModel.getUserId())
                        .setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            databaseReferenceFriendRequest.child(requestModel.getUserId()).child(currentUser.getUid())
                                    .setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){

                                        String title = "Firend Request Denied";
                                        String message = "Friend request denied by  " + currentUser.getDisplayName();
                                        Util.sendNotification(context, title, message, requestModel.getUserId());

                                        holder.progressBar.setVisibility(View.GONE);
                                        holder.denyButton.setVisibility(View.VISIBLE);
                                        holder.acceptButton.setVisibility(View.VISIBLE);

                                    } else {

                                        Toast.makeText(context, context.getString(R.string.failed_to_deny_request, task.getException()), Toast.LENGTH_SHORT).show();
                                        holder.progressBar.setVisibility(View.GONE);
                                        holder.denyButton.setVisibility(View.VISIBLE);
                                        holder.acceptButton.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                        }
                        else {
                            Toast.makeText(context, context.getString(R.string.failed_to_deny_request, task.getException()), Toast.LENGTH_SHORT).show();
                            holder.progressBar.setVisibility(View.GONE);
                            holder.denyButton.setVisibility(View.VISIBLE);
                            holder.acceptButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
    }

    private void handleException(RequestViewHolder holder, Exception exception) {
        Toast.makeText(context, context.getString(R.string.failed_to_accept_request, exception), Toast.LENGTH_SHORT).show();
        holder.progressBar.setVisibility(View.GONE);
        holder.denyButton.setVisibility(View.VISIBLE);
        holder.acceptButton.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return requestModelList.size();
    }

    public class RequestViewHolder extends RecyclerView.ViewHolder {

        private ImageView profileImageView;
        private TextView usernameTextView;
        private Button acceptButton, denyButton;
        private ProgressBar progressBar;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImageView = itemView.findViewById(R.id.profileImageView);
            usernameTextView = itemView.findViewById(R.id.usernameRequestTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            denyButton = itemView.findViewById(R.id.denyButton);
            progressBar = itemView.findViewById(R.id.acceptProgressbar);
        }
    }
}
