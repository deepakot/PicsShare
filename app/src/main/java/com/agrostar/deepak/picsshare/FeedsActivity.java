package com.agrostar.deepak.picsshare;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.agrostar.deepak.picsshare.Models.Author;
import com.agrostar.deepak.picsshare.Models.Post;
import com.agrostar.deepak.picsshare.Utils.FirebaseUtility;
import com.agrostar.deepak.picsshare.ViewHolder.PostViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Sharmaji on 8/6/2018.
 */

public class FeedsActivity extends BaseActivity {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.my_recycler_view) RecyclerView recyclerView;

    public static final String TAG = "FeedsActivity";
    private static final String KEY_LAYOUT_POSITION = "layoutPosition";
    private int recyclerViewPosition = 0;
    private RecyclerView.Adapter<PostViewHolder> adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feeds);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        Query allPostsQuery = FirebaseUtility.getPostsRef();
        adapter = getFirebaseRecyclerAdapter(allPostsQuery);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                // TODO: Refresh feed view.
            }
        });
        recyclerView.setAdapter(adapter);
        if (savedInstanceState != null) {
            recyclerViewPosition = (int) savedInstanceState.getSerializable(KEY_LAYOUT_POSITION);
            recyclerView.scrollToPosition(recyclerViewPosition);
        }

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            recyclerViewPosition = (int) savedInstanceState.getSerializable(KEY_LAYOUT_POSITION);
            Log.d(TAG, "Restoring recycler view position (all): " + recyclerViewPosition);
            recyclerView.scrollToPosition(recyclerViewPosition);
        }
    }

    @OnClick(R.id.fab) void onClickFab() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            Toast.makeText(FeedsActivity.this, "You must sign-in to post.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent newPostIntent = new Intent(FeedsActivity.this, NewPostActivity.class);
        startActivity(newPostIntent);
    }

    private FirebaseRecyclerAdapter<Post, PostViewHolder> getFirebaseRecyclerAdapter(Query query) {
        FirebaseRecyclerOptions<Post> options = new FirebaseRecyclerOptions.Builder<Post>()
                .setLifecycleOwner(this)
                .setQuery(query, Post.class)
                .build();

        return new FirebaseRecyclerAdapter<Post, PostViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull PostViewHolder postViewHolder,
                                            int position,
                                            @NonNull Post post) {
                setupPost(postViewHolder, post, position, null);
            }

            @Override
            public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.post_item, parent, false);

                return new PostViewHolder(view);
            }
        };
    }

    private void setupPost(final PostViewHolder postViewHolder, final Post post, final int position, final String inPostKey) {
        postViewHolder.setPhoto(post.getFull_url());
        postViewHolder.setText(post.getText());
        postViewHolder.setTimestamp(DateUtils.getRelativeTimeSpanString(
                (long) post.getTimestamp()).toString());
        final String postKey;
        if (adapter instanceof FirebaseRecyclerAdapter) {
            postKey = ((FirebaseRecyclerAdapter) adapter).getRef(position).getKey();
        } else {
            postKey = inPostKey;
        }

        Author author = post.getAuthor();
        postViewHolder.setAuthor(author.getFull_name(), author.getUid());
        postViewHolder.setIcon(author.getProfile_picture(), author.getUid());

        ValueEventListener likeListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                postViewHolder.setNumLikes(dataSnapshot.getChildrenCount());
                if (dataSnapshot.hasChild(FirebaseUtility.getCurrentUserId())) {
                    postViewHolder.setLikeStatus(PostViewHolder.LikeStatus.LIKED, FeedsActivity.this);
                } else {
                    postViewHolder.setLikeStatus(PostViewHolder.LikeStatus.NOT_LIKED, FeedsActivity.this);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        FirebaseUtility.getLikesRef().child(postKey).addValueEventListener(likeListener);
        postViewHolder.mLikeListener = likeListener;

        postViewHolder.setPostClickListener(new PostViewHolder.PostClickListener() {
            @Override
            public void showComments() {
                Log.d(TAG, "Comment position: " + position);
                onPostComment(postKey);
            }

            @Override
            public void toggleLike() {
                Log.d(TAG, "Like position: " + position);
                onPostLike(postKey);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        int recyclerViewScrollPosition = getRecyclerViewScrollPosition();
        Log.d(TAG, "Recycler view scroll position: " + recyclerViewScrollPosition);
        savedInstanceState.putSerializable(KEY_LAYOUT_POSITION, recyclerViewScrollPosition);
        super.onSaveInstanceState(savedInstanceState);
    }

    private int getRecyclerViewScrollPosition() {
        int scrollPosition = 0;
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) recyclerView.getLayoutManager())
                    .findFirstVisibleItemPosition();
        }
        return scrollPosition;
    }

    public void onPostComment(String postKey) {
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra(CommentsActivity.POST_KEY_EXTRA, postKey);
        startActivity(intent);
    }

    public void onPostLike(final String postKey) {
        final String userKey = FirebaseUtility.getCurrentUserId();
        final DatabaseReference postLikesRef = FirebaseUtility.getLikesRef();
        postLikesRef.child(postKey).child(userKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User already liked this post, so we toggle like off.
                    postLikesRef.child(postKey).child(userKey).removeValue();
                } else {
                    postLikesRef.child(postKey).child(userKey).setValue(ServerValue.TIMESTAMP);
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {

            }
        });
    }


}
