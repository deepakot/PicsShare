package com.agrostar.deepak.picsshare;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.agrostar.deepak.picsshare.Models.Author;
import com.agrostar.deepak.picsshare.Models.Comment;
import com.agrostar.deepak.picsshare.Utils.FirebaseUtility;
import com.agrostar.deepak.picsshare.Utils.GlideUtility;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Sharmaji on 8/6/2018.
 */

public class CommentsActivity extends BaseActivity {

    public static final String POST_KEY_EXTRA = "post_key";
    public static final String TAG = "CommentsActivity";
    private static final int DEFAULT_MSG_LENGTH_LIMIT = 256;
    private FirebaseRecyclerAdapter<Comment, CommentViewHolder> mAdapter;

    @BindView(R.id.comment_list) RecyclerView recyclerView;
    @BindView(R.id.editText) EditText editText;
    @BindView(R.id.send_comment) Button sendButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);
        ButterKnife.bind(this);

        String postKey = getIntent().getStringExtra(POST_KEY_EXTRA);
        if (postKey == null) {
            finish();
        }

        final DatabaseReference commentsRef = FirebaseUtility.getCommentsRef().child(postKey);

        FirebaseRecyclerOptions<Comment> options = new FirebaseRecyclerOptions.Builder<Comment>()
                .setQuery(commentsRef, Comment.class)
                .build();

        mAdapter = new FirebaseRecyclerAdapter<Comment, CommentViewHolder>(options) {
            @Override
            public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.comment_item, parent, false);

                return new CommentViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull CommentViewHolder viewHolder,
                                            int position,
                                            @NonNull Comment comment) {
                Author author = comment.getAuthor();
                viewHolder.commentAuthor.setText(author.getFull_name());
                GlideUtility.loadProfileIcon(author.getProfile_picture(), viewHolder.commentPhoto);

                viewHolder.authorRef = author.getUid();
                viewHolder.commentTime
                        .setText(DateUtils.getRelativeTimeSpanString((long) comment.getTimestamp
                                ()));
                viewHolder.commentText.setText(comment.getText());
            }
        };

        mAdapter.startListening();

        sendButton.setEnabled(false);
        editText.setHint(R.string.new_comment_hint);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter
                (DEFAULT_MSG_LENGTH_LIMIT)});
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0) {
                    sendButton.setEnabled(true);
                } else {
                    sendButton.setEnabled(false);
                }
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear input box and hide keyboard.
                final Editable commentText = editText.getText();
                editText.setText("");
                InputMethodManager inputManager =
                        (InputMethodManager)
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(
                        editText.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(CommentsActivity.this, R.string.user_logged_out_error,
                            Toast.LENGTH_SHORT).show();
                }

                Author author = new Author(user.getDisplayName(),
                        user.getPhotoUrl().toString(), user.getUid());

                Comment comment = new Comment(author, commentText.toString(),
                        ServerValue.TIMESTAMP);
                commentsRef.push().setValue(comment, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError error, DatabaseReference firebase) {
                        if (error != null) {
                            Log.w(TAG, "Error posting comment: " + error.getMessage());
                            Toast.makeText(CommentsActivity.this, "Error posting comment.", Toast
                                    .LENGTH_SHORT).show();
                            editText.setText(commentText);
                        }
                    }
                });
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.stopListening();
        }
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.comment_author_icon) ImageView commentPhoto;
        @BindView(R.id.comment_text) TextView commentText;
        @BindView(R.id.comment_name) TextView commentAuthor;
        @BindView(R.id.comment_time) TextView commentTime;
        public String authorRef;

        public CommentViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (authorRef != null) {
                        Context context = v.getContext();
                        Intent userDetailIntent = new Intent(context, UserDetailActivity.class);
                        userDetailIntent.putExtra(UserDetailActivity.USER_ID_EXTRA_NAME,
                                authorRef);
                        context.startActivity(userDetailIntent);
                    }
                }
            });
        }
    }

}
