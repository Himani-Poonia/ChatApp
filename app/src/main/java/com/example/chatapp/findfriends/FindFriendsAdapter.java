package com.example.chatapp.findfriends;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.List;

public class FindFriendsAdapter extends RecyclerView.Adapter<FindFriendsAdapter.FindFriendsViewHolder> {

    private Context context;
    private List<FindFriendsModel> findFriendsModelList;

    private DatabaseReference friendRequestDatabase;
    private FirebaseUser currentUser;
    private String userId;

    public FindFriendsAdapter(Context context, List<FindFriendsModel> findFriendsModelList) {
        this.context = context;
        this.findFriendsModelList = findFriendsModelList;
    }

    @NonNull
    @Override
    public FindFriendsAdapter.FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.find_friends_list_item,parent,false);
        return new FindFriendsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FindFriendsViewHolder holder, int position) {
        FindFriendsModel findFriendsModel = findFriendsModelList.get(position);

        holder.userNameTextView.setText(findFriendsModel.getUsername());

        StorageReference fileReference = FirebaseStorage.getInstance().getReference().child(Constants.IMAGES_FOLDER +"/"+findFriendsModel.getPhotoName());
        fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(context)
                        .load(uri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(holder.profileImageView);
            }
        });

        friendRequestDatabase = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if(findFriendsModel.isRequestSent()){
            holder.sendRequestButton.setVisibility(View.GONE);
            holder.cancelButton.setVisibility(View.VISIBLE);
        } else {
            holder.sendRequestButton.setVisibility(View.VISIBLE);
            holder.cancelButton.setVisibility(View.GONE);
        }

        holder.sendRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.sendRequestButton.setVisibility(View.GONE);
                holder.progressBar.setVisibility(View.VISIBLE);

                userId = findFriendsModel.getUserId();

                friendRequestDatabase.child(currentUser.getUid()).child(userId).child(NodeNames.REQUEST_TYPE)
                                .setValue(Constants.REQUEST_DATA_SENT).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            friendRequestDatabase.child(userId).child(currentUser.getUid()).child(NodeNames.REQUEST_TYPE)
                                    .setValue(Constants.REQUEST_DATA_RECEIVED).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        Toast.makeText(context, R.string.request_sent_successfully, Toast.LENGTH_SHORT).show();

                                        String title = "New Friend Request";
                                        String message = "Friend request from " + currentUser.getDisplayName();
                                        Util.sendNotification(context, title, message, userId);

                                        holder.progressBar.setVisibility(View.GONE);
                                        holder.sendRequestButton.setVisibility(View.GONE);
                                        holder.cancelButton.setVisibility(View.VISIBLE);
                                    }
                                    else {
                                        Toast.makeText(context, context.getString(R.string.failed_to_send_friend_request,
                                                task.getException()), Toast.LENGTH_SHORT).show();
                                        holder.progressBar.setVisibility(View.GONE);
                                        holder.sendRequestButton.setVisibility(View.VISIBLE);
                                        holder.cancelButton.setVisibility(View.GONE);
                                    }
                                }
                            });
                        }
                        else {
                            Toast.makeText(context, context.getString(R.string.failed_to_send_friend_request,
                                    task.getException()), Toast.LENGTH_SHORT).show();
                            holder.progressBar.setVisibility(View.GONE);
                            holder.sendRequestButton.setVisibility(View.VISIBLE);
                            holder.cancelButton.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        holder.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.cancelButton.setVisibility(View.GONE);
                holder.progressBar.setVisibility(View.VISIBLE);

                userId = findFriendsModel.getUserId();

                friendRequestDatabase.child(currentUser.getUid()).child(userId).child(NodeNames.REQUEST_TYPE)
                        .setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            friendRequestDatabase.child(userId).child(currentUser.getUid()).child(NodeNames.REQUEST_TYPE)
                                    .setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        Toast.makeText(context, R.string.request_cancelled_successfully, Toast.LENGTH_SHORT).show();
                                        holder.progressBar.setVisibility(View.GONE);
                                        holder.sendRequestButton.setVisibility(View.VISIBLE);
                                        holder.cancelButton.setVisibility(View.GONE);
                                    }
                                    else {
                                        Toast.makeText(context, context.getString(R.string.failed_to_cancel_friend_request,
                                                task.getException()), Toast.LENGTH_SHORT).show();
                                        holder.progressBar.setVisibility(View.GONE);
                                        holder.sendRequestButton.setVisibility(View.GONE);
                                        holder.cancelButton.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                        }
                        else {
                            Toast.makeText(context, context.getString(R.string.failed_to_cancel_friend_request,
                                    task.getException()), Toast.LENGTH_SHORT).show();
                            holder.progressBar.setVisibility(View.GONE);
                            holder.sendRequestButton.setVisibility(View.GONE);
                            holder.cancelButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
    }


    @Override
    public int getItemCount() {
        return findFriendsModelList.size();
    }

    public class FindFriendsViewHolder extends RecyclerView.ViewHolder {
        private ImageView profileImageView;
        private TextView userNameTextView;
        private Button sendRequestButton, cancelButton;
        private ProgressBar progressBar;

        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImageView = itemView.findViewById(R.id.profileiv);
            userNameTextView = itemView.findViewById(R.id.nameTextView);
            sendRequestButton = itemView.findViewById(R.id.sendRequestButton);
            cancelButton = itemView.findViewById(R.id.cancelRequestButton);
            progressBar = itemView.findViewById(R.id.requestProgressBar);
        }
    }
}
