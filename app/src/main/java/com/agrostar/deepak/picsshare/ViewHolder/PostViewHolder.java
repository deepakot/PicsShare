package com.agrostar.deepak.picsshare.ViewHolder;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.agrostar.deepak.picsshare.R;
import com.agrostar.deepak.picsshare.UserDetailActivity;
import com.agrostar.deepak.picsshare.Utils.GlideUtility;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Sharmaji on 8/6/2018.
 */

public class PostViewHolder extends RecyclerView.ViewHolder {

    private final View mView;
    private PostClickListener mListener;
    public DatabaseReference mPostRef;
    public ValueEventListener mPostListener;

    public enum LikeStatus { LIKED, NOT_LIKED }
    private static final int POST_TEXT_MAX_LINES = 6;

    @BindView(R.id.post_photo) ImageView mPhotoView;
    @BindView(R.id.post_author_icon) ImageView mIconView;
    @BindView(R.id.post_author_name) TextView mAuthorView;
    @BindView(R.id.post_text) TextView mPostTextView;
    @BindView(R.id.post_timestamp) TextView mTimestampView;
    @BindView(R.id.post_num_likes) TextView mNumLikesView;
    @BindView(R.id.post_like_icon) ImageView mLikeIcon;

    public String mPostKey;
    public ValueEventListener mLikeListener;


    public PostViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        mView = itemView;

        itemView.findViewById(R.id.post_comment_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.showComments();
            }
        });
        mLikeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.toggleLike();
            }
        });
    }

    public void setPhoto(String url) {
        GlideUtility.loadImage(url, mPhotoView);
    }

    public void setIcon(String url, final String authorId) {
        GlideUtility.loadProfileIcon(url, mIconView);
        mIconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUserDetail(authorId);
            }
        });
    }

    public void setAuthor(String author, final String authorId) {
        if (author == null || author.isEmpty()) {
            author = mView.getResources().getString(R.string.user_info_no_name);
        }
        mAuthorView.setText(author);
        mAuthorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUserDetail(authorId);
            }
        });
    }

    private void showUserDetail(String authorId) {
        Context context = mView.getContext();
        Intent userDetailIntent = new Intent(context, UserDetailActivity.class);
        userDetailIntent.putExtra(UserDetailActivity.USER_ID_EXTRA_NAME, authorId);
        context.startActivity(userDetailIntent);
    }


    public void setText(final String text) {
        if (text == null || text.isEmpty()) {
            mPostTextView.setVisibility(View.GONE);
            return;
        } else {
            mPostTextView.setVisibility(View.VISIBLE);
            mPostTextView.setText(text);
            mPostTextView.setMaxLines(POST_TEXT_MAX_LINES);
            mPostTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mPostTextView.getMaxLines() == POST_TEXT_MAX_LINES) {
                        mPostTextView.setMaxLines(Integer.MAX_VALUE);
                    } else {
                        mPostTextView.setMaxLines(POST_TEXT_MAX_LINES);
                    }
                }
            });
        }
    }

    public void setTimestamp(String timestamp) {
        mTimestampView.setText(timestamp);
    }

    public void setNumLikes(long numLikes) {
        String suffix = numLikes == 1 ? " like" : " likes";
        mNumLikesView.setText(numLikes + suffix);
    }

    public void setPostClickListener(PostClickListener listener) {
        mListener = listener;
    }

    public void setLikeStatus(LikeStatus status, Context context) {
        mLikeIcon.setImageDrawable(ContextCompat.getDrawable(context,
                status == LikeStatus.LIKED ? R.drawable.heart_full : R.drawable.heart_empty));
    }


    public interface PostClickListener {
        void showComments();
        void toggleLike();
    }

}
