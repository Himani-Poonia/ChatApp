package com.example.chatapp.selectfriend;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.chats.ChatActivity;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.NodeNames;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class SelectFriendAdapter extends RecyclerView.Adapter<SelectFriendAdapter.SelectFriendViewHolder> {

    private Context context;
    private List<SelectFriendModel> selectFriendModelList;

    public SelectFriendAdapter(Context context, List<SelectFriendModel> selectFriendModelList) {
        this.context = context;
        this.selectFriendModelList = selectFriendModelList;
    }

    @NonNull
    @Override
    public SelectFriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.select_friend_list_item, parent,false);
        return new SelectFriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectFriendViewHolder holder, int position) {
        SelectFriendModel selectFriendModel = selectFriendModelList.get(position);
        holder.userNameTextView.setText(selectFriendModel.getUserName());

        //set profile image
        DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference databaseReference = rootReference.child(NodeNames.USERS).child(selectFriendModel.getUserId());

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.child(NodeNames.PHOTO).getValue()!=null?
                        snapshot.child(NodeNames.PHOTO).getValue().toString():"";

                if(!url.equals("")) {
                    StorageReference firebaseStorage = FirebaseStorage.getInstance().getReference();
                    firebaseStorage.child(Constants.IMAGES_FOLDER).child(url).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Glide.with(context)
                                    .load(uri)
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(holder.ivProfile);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        holder.selectFriendLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(context instanceof SelectFriendActivity) {
                    ((SelectFriendActivity) context).returnSelectedFriend(selectFriendModel.getUserId(), selectFriendModel.getUserName(),
                            selectFriendModel.getUserId() + "jpg");
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectFriendModelList.size();
    }

    public class SelectFriendViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout selectFriendLinearLayout;
        private ImageView ivProfile;
        private TextView userNameTextView;

        public SelectFriendViewHolder(@NonNull View itemView) {
            super(itemView);

            selectFriendLinearLayout = itemView.findViewById(R.id.selectFriendLinearLayout);
            ivProfile = itemView.findViewById(R.id.profileImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
        }
    }
}
