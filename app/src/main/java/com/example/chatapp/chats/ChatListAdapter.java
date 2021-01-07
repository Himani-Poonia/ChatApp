package com.example.chatapp.chats;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
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
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.Extras;
import com.example.chatapp.common.Util;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    private final Context context;
    private final List<ChatListModel> chatListModelList;

    public ChatListAdapter(Context context, List<ChatListModel> chatListModelList) {
        this.context = context;
        this.chatListModelList = chatListModelList;
    }

    @NonNull
    @Override
    public ChatListAdapter.ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_list_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatListAdapter.ChatViewHolder holder, int position) {
        final ChatListModel chatListModel = chatListModelList.get(position);

        holder.usernameTextView.setText(chatListModel.getUserName());

        StorageReference fileReference = FirebaseStorage.getInstance().getReference().child(Constants.IMAGES_FOLDER+"/"+chatListModel.getPhotoName());
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

        //set last message UI
        String lastMessage = chatListModel.getLastMessage();
        lastMessage = lastMessage.length() > 30? lastMessage.substring(0,30) : lastMessage;
        holder.lastMessageTextView.setText(lastMessage);

        String lastMessageTime = chatListModel.getLastMessageTime()==null?"":
                chatListModel.getLastMessageTime();

        if(!lastMessageTime.equals(""))
            holder.tvLastMessageTime.setText(Util.getTimeAgo(Long.parseLong(lastMessageTime)));

        if(!lastMessage.equals("")) {
            holder.lastMessageTextView.setVisibility(View.VISIBLE);
            holder.lastMessageTextView.setText(lastMessage);

            holder.tvLastMessageTime.setVisibility(View.VISIBLE);
            holder.tvLastMessageTime.setText(Util.getTimeAgo(Long.parseLong(lastMessageTime)));
        }
        else {
            holder.lastMessageTextView.setVisibility(View.GONE);
            holder.tvLastMessageTime.setVisibility(View.GONE);
        }

        //set unread_count UI
        if(!chatListModel.getUnreadCount().equals("0")) {
            holder.unreadCountTextView.setVisibility(View.VISIBLE);
            holder.unreadCountTextView.setText(chatListModel.getUnreadCount());
        }
        else {
            holder.unreadCountTextView.setVisibility(View.GONE);
        }

        holder.chatListLinearLayout.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra(Extras.USER_KEY, chatListModel.getUserId());
            intent.putExtra(Extras.USER_NAME, chatListModel.getUserName());
            intent.putExtra(Extras.PHOTO_NAME, chatListModel.getPhotoName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatListModelList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout chatListLinearLayout;
        private final TextView usernameTextView;
        private final TextView lastMessageTextView;
        private final TextView unreadCountTextView;
        private final TextView tvLastMessageTime;
        private final ImageView profileImageView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            chatListLinearLayout = itemView.findViewById(R.id.chatListLinearLayout);
            usernameTextView = itemView.findViewById(R.id.userNameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMessageTime);
            unreadCountTextView = itemView.findViewById(R.id.unreadCountTextView);
            profileImageView = itemView.findViewById(R.id.profileImageView);
        }
    }
}
