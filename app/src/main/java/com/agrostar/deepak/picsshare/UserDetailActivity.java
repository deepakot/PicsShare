package com.agrostar.deepak.picsshare;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.agrostar.deepak.picsshare.Models.Person;
import com.agrostar.deepak.picsshare.Models.Post;
import com.agrostar.deepak.picsshare.Utils.FirebaseUtility;
import com.agrostar.deepak.picsshare.Utils.GlideUtility;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Sharmaji on 8/6/2018.
 */

public class UserDetailActivity extends BaseActivity {

    private final String TAG = "UserDetailActivity";
    public static final String USER_ID_EXTRA_NAME = "user_name";
    private RecyclerView recyclerView;
    private GridAdapter gridAdapter;
    private ValueEventListener followingListener;
    private ValueEventListener personInfoListener;
    private String userId;
    private DatabaseReference peopleRef;
    private DatabaseReference personRef;
    private static final int GRID_NUM_COLUMNS = 2;
    private DatabaseReference followersRef;
    private ValueEventListener followersListener;
    private String currentUserId = null;

    @BindView(R.id.follow_user_fab) FloatingActionButton followUserFab;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);
        ButterKnife.bind(this);
        Intent intent = getIntent();
        userId = intent.getStringExtra(USER_ID_EXTRA_NAME);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        peopleRef = FirebaseUtility.getPeopleRef();
        currentUserId = FirebaseUtility.getCurrentUserId();

        followingListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    followUserFab.setImageDrawable(ContextCompat.getDrawable(
                            UserDetailActivity.this, R.drawable.ic_done_24dp));
                } else {
                    followUserFab.setImageDrawable(ContextCompat.getDrawable(
                            UserDetailActivity.this, R.drawable.ic_person_add_24dp));
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {

            }
        };
        if (currentUserId != null) {
            peopleRef.child(currentUserId).child("following").child(userId)
                    .addValueEventListener(followingListener);
        }
        recyclerView = (RecyclerView) findViewById(R.id.user_posts_grid);
        gridAdapter = new GridAdapter();
        recyclerView.setAdapter(gridAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, GRID_NUM_COLUMNS));

        personRef = FirebaseUtility.getPeopleRef().child(userId);
        personInfoListener = personRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Person person = dataSnapshot.getValue(Person.class);
                Log.w(TAG, "mPersonRef:" + personRef.getKey());
                CircleImageView userPhoto = (CircleImageView) findViewById(R.id.user_detail_photo);
                GlideUtility.loadProfileIcon(person.getPhotoUrl(), userPhoto);
                String name = person.getDisplayName();
                if (name == null) {
                    name = getString(R.string.user_info_no_name);
                }
                collapsingToolbar.setTitle(name);
                if (person.getFollowing() != null) {
                    int numFollowing = person.getFollowing().size();
                    ((TextView) findViewById(R.id.user_num_following))
                            .setText(numFollowing + " following");
                }
                List<String> paths = new ArrayList<String>(person.getPosts().keySet());
                gridAdapter.addPaths(paths);
                String firstPostKey = paths.get(0);

                FirebaseUtility.getPostsRef().child(firstPostKey).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Post post = dataSnapshot.getValue(Post.class);

                        ImageView imageView = (ImageView) findViewById(R.id.backdrop);
                        GlideUtility.loadImage(post.getFull_url(), imageView);
                    }

                    @Override
                    public void onCancelled(DatabaseError firebaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {

            }
        });
        followersRef = FirebaseUtility.getFollowersRef().child(userId);
        followersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long numFollowers = dataSnapshot.getChildrenCount();
                ((TextView) findViewById(R.id.user_num_followers))
                        .setText(numFollowers + " follower" + (numFollowers == 1 ? "" : "s"));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        followersRef.addValueEventListener(followersListener);
    }
    
    

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (FirebaseUtility.getCurrentUserId() != null) {
            peopleRef.child(FirebaseUtility.getCurrentUserId()).child("following").child(userId)
                    .removeEventListener(followingListener);
        }

        personRef.child(userId).removeEventListener(personInfoListener);
        followersRef.removeEventListener(followersListener);
    }

    @OnClick(R.id.follow_user_fab) void onClickFab() {
        if (currentUserId == null) {
            Toast.makeText(UserDetailActivity.this, "You need to sign in to follow someone.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        else if(currentUserId == userId) {
            Toast.makeText(UserDetailActivity.this, "You cannot follow yourself.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        peopleRef.child(currentUserId).child("following").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> updatedUserData = new HashMap<>();
                if (dataSnapshot.exists()) {
                    // Already following, need to unfollow
                    updatedUserData.put("people/" + currentUserId + "/following/" + userId, null);
                    updatedUserData.put("followers/" + userId + "/" + currentUserId, null);
                } else {
                    updatedUserData.put("people/" + currentUserId + "/following/" + userId, true);
                    updatedUserData.put("followers/" + userId + "/" + currentUserId, true);
                }
                FirebaseUtility.getBaseRef().updateChildren(updatedUserData, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError firebaseError, DatabaseReference firebase) {
                        if (firebaseError != null) {
                            Toast.makeText(UserDetailActivity.this, R.string
                                    .follow_user_error, Toast.LENGTH_LONG).show();
                            Log.d(TAG, getString(R.string.follow_user_error) + "\n" +
                                    firebaseError.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {

            }
        });
    }

    class GridAdapter extends RecyclerView.Adapter<GridImageHolder> {
        private List<String> mPostPaths;

        public GridAdapter() {
            mPostPaths = new ArrayList<String>();
        }

        @Override
        public GridImageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(UserDetailActivity.this);
            int tileDimPx = getPixelsFromDps(100);
            imageView.setLayoutParams(new GridView.LayoutParams(tileDimPx, tileDimPx));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);

            return new GridImageHolder(imageView);
        }

        @Override
        public void onBindViewHolder(final GridImageHolder holder, int position) {
            DatabaseReference ref = FirebaseUtility.getPostsRef().child(mPostPaths.get(position));
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Post post = dataSnapshot.getValue(Post.class);
                    GlideUtility.loadImage(post.getFull_url(), holder.imageView);
                }

                @Override
                public void onCancelled(DatabaseError firebaseError) {
                    Log.e(TAG, "Unable to load grid image: " + firebaseError.getMessage());
                }
            });
        }

        public void addPaths(List<String> paths) {
            int startIndex = mPostPaths.size();
            for(String path: paths) {
                if(!mPostPaths.contains(path)) {
                    mPostPaths.add(path);
                }
            }
            notifyItemRangeInserted(startIndex, mPostPaths.size());
        }

        @Override
        public int getItemCount() {
            return mPostPaths.size();
        }

        private int getPixelsFromDps(int dps) {
            final float scale = UserDetailActivity.this.getResources().getDisplayMetrics().density;
            return (int) (dps * scale + 0.5f);
        }
    }

    private class GridImageHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;

        public GridImageHolder(ImageView itemView) {
            super(itemView);
            imageView = itemView;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

}
