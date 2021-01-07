package com.example.chatapp.chats;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.selectfriend.SelectFriendActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessagesViewHolder> {
    private final Context context;
    private final List<MessageModel> messageModelList;
    private FirebaseAuth firebaseAuth;

    private ActionMode actionMode;
    private ConstraintLayout selectedView;

    public MessagesAdapter(Context context, List<MessageModel> messageModelList) {
        this.context = context;
        this.messageModelList = messageModelList;
    }

    @NonNull
    @Override
    public MessagesAdapter.MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.message_list_item, parent, false);
        return new MessagesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessagesAdapter.MessagesViewHolder holder, int position) {
        MessageModel messageModel = messageModelList.get(position);
        firebaseAuth = FirebaseAuth.getInstance();
        String currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        String fromUserId = messageModel.getMessageFrom();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

        String dateTime = dateFormat.format(messageModel.getMessageTime());
        String[] splitString = dateTime.split(" ");
        String messageTime = splitString[1];

        if(fromUserId.equals(currentUserId)){  //this means message is sent

            if(messageModel.getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)){
                holder.sentImageLinearLayout.setVisibility(View.GONE);
                holder.sentLinearLayout.setVisibility(View.VISIBLE);
            }
            else {
                holder.sentImageLinearLayout.setVisibility(View.VISIBLE);
                holder.sentLinearLayout.setVisibility(View.GONE);
            }

            holder.receivedLinearLayout.setVisibility(View.GONE);
            holder.receivedImageLinearLayout.setVisibility(View.GONE);

            holder.sentMsgTextView.setText(messageModel.getMessage());
            holder.sentMsgTimeTextView.setText(messageTime);
            holder.sentImageTimeTextView.setText(messageTime);
            Glide.with(context)
                    .load(messageModel.getMessage())
                    .placeholder(R.drawable.ic_image)
                    .into(holder.sentImageView);

        }
        else {   //this means message is received

            if(messageModel.getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)){
                holder.receivedImageLinearLayout.setVisibility(View.GONE);
                holder.receivedLinearLayout.setVisibility(View.VISIBLE);
            }
            else {
                holder.receivedImageLinearLayout.setVisibility(View.VISIBLE);
                holder.receivedLinearLayout.setVisibility(View.GONE);
            }

            holder.sentLinearLayout.setVisibility(View.GONE);
            holder.sentImageLinearLayout.setVisibility(View.GONE);

            holder.receivedMsgTextView.setText(messageModel.getMessage());
            holder.receivedMsgTimeTextView.setText(messageTime);
            holder.receivedImageTimeTextView.setText(messageTime);
            Glide.with(context)
                    .load(messageModel.getMessage())
                    .placeholder(R.drawable.ic_image)
                    .into(holder.receivedImageView);
        }

        holder.msgConstraintLayout.setTag(R.id.TAG_MESSAGE, messageModel.getMessage());
        holder.msgConstraintLayout.setTag(R.id.TAG_MESSAGE_ID, messageModel.getMessageId());
        holder.msgConstraintLayout.setTag(R.id.TAG_MESSAGE_TYPE, messageModel.getMessageType());
        holder.msgConstraintLayout.setTag(R.id.TAG_MESSAGE_FROM, messageModel.getMessageFrom());

        holder.msgConstraintLayout.setOnClickListener(v -> {
            String messageType = v.getTag(R.id.TAG_MESSAGE_TYPE).toString();
            Uri uri = Uri.parse(v.getTag(R.id.TAG_MESSAGE).toString());

            if(messageType.equals(Constants.MESSAGE_TYPE_VIDEO)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setDataAndType(uri,"video/mp4");
                context.startActivity(intent);
            }
            else if(messageType.equals(Constants.MESSAGE_TYPE_IMAGE)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setDataAndType(uri,"image/jpg");
                context.startActivity(intent);
            }
        });

        holder.msgConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(actionMode!=null)
                    return false;

                selectedView = holder.msgConstraintLayout;
                actionMode = ((AppCompatActivity)context).startSupportActionMode(actionModeCallback);
                holder.msgConstraintLayout.setBackgroundColor(context.getResources().getColor(R.color.colorAccent));
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    public static class MessagesViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout sentLinearLayout;
        private final LinearLayout receivedLinearLayout;
        private final LinearLayout sentImageLinearLayout;
        private final LinearLayout receivedImageLinearLayout;
        private final TextView sentMsgTextView;
        private final TextView sentMsgTimeTextView;
        private final TextView receivedMsgTimeTextView;
        private final TextView receivedMsgTextView;
        private final TextView receivedImageTimeTextView;
        private final TextView sentImageTimeTextView;
        private final ImageView sentImageView;
        private final ImageView receivedImageView;
        private final ConstraintLayout msgConstraintLayout;

        public MessagesViewHolder(@NonNull View itemView) {
            super(itemView);

            sentLinearLayout = itemView.findViewById(R.id.sentLinearLayout);
            receivedLinearLayout = itemView.findViewById(R.id.receivedLinearLayout);
            sentMsgTextView = itemView.findViewById(R.id.sentMsgTextView);
            sentMsgTimeTextView = itemView.findViewById(R.id.sentMsgTimeTextView);
            receivedMsgTextView = itemView.findViewById(R.id.receivedMsgTextView);
            receivedMsgTimeTextView = itemView.findViewById(R.id.receivedMsgTimeTextView);
            msgConstraintLayout = itemView.findViewById(R.id.messageConstraintLayout);
            sentImageLinearLayout = itemView.findViewById(R.id.sentImageLinearLayout);
            receivedImageLinearLayout = itemView.findViewById(R.id.receivedImageLinearLayout);
            receivedImageTimeTextView = itemView.findViewById(R.id.receivedImageTimeTextView);
            sentImageTimeTextView = itemView.findViewById(R.id.sentImageTimeTextView);
            sentImageView = itemView.findViewById(R.id.sentImageView);
            receivedImageView = itemView.findViewById(R.id.receivedImageView);
        }
    }

    public ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.chat_menu_options, menu);

            String selectedMessageType =  String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));
            if(selectedMessageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
                MenuItem itemDownload = menu.findItem(R.id.menuDownload);
                itemDownload.setVisible(false);
            }

            String currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
            if(!selectedView.getTag(R.id.TAG_MESSAGE_FROM).equals(currentUserId)){
                MenuItem itemDelete = menu.findItem(R.id.menuDelete);
                itemDelete.setVisible(false);
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            String selectedMessageId = selectedView.getTag(R.id.TAG_MESSAGE_ID).toString();
            String selectedMessage = selectedView.getTag(R.id.TAG_MESSAGE).toString();
            String selectedMessageType = selectedView.getTag(R.id.TAG_MESSAGE_TYPE).toString();

            int itemId = item.getItemId();

            switch (itemId) {
                case R.id.menuDelete:

                    if(context instanceof ChatActivity) {
                        ((ChatActivity)context).deleteMessage(selectedMessageId, selectedMessageType);
                    }

                    mode.finish();
                    break;

                case R.id.menuDownload:
                    if(context instanceof ChatActivity) {
                        ((ChatActivity)context).downloadFile(selectedMessageId, selectedMessageType,false);
                    }
                    mode.finish();
                    break;

                case R.id.menuShare:
                    if(selectedMessageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
                        Intent intentShare = new Intent();
                        intentShare.setAction(Intent.ACTION_SEND);
                        intentShare.putExtra(Intent.EXTRA_TEXT, selectedMessage);
                        intentShare.setType("text/plain");
                        context.startActivity(intentShare);
                    }
                    else {
                        if(context instanceof ChatActivity) {
                            ((ChatActivity)context).downloadFile(selectedMessageId, selectedMessageType,true);
                        }
                    }
                    mode.finish();
                    break;

                case R.id.menuForward:
                    if(context instanceof ChatActivity) {
                        ((ChatActivity) context).forwardMessage(selectedMessageId, selectedMessage, selectedMessageType);
                    }

                    mode.finish();
                    break;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode=null;
            selectedView.setBackgroundColor(context.getResources().getColor(R.color.chat_background));
        }
    };
}
