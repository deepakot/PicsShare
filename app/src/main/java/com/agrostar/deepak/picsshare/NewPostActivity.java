package com.agrostar.deepak.picsshare;

import android.*;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.agrostar.deepak.picsshare.Models.Author;
import com.agrostar.deepak.picsshare.Models.Post;
import com.agrostar.deepak.picsshare.Utils.FirebaseUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by Sharmaji on 8/6/2018.
 */

public class NewPostActivity extends BaseActivity implements EasyPermissions.PermissionCallbacks {

    public static final String TAG = "NewPostActivity";
    private static final int FULL_SIZE_MAX_DIMENSION = 1280;

    @BindView(R.id.new_post_picture) ImageView imageView;
    @BindView(R.id.new_post_submit) Button submitButton;
    @BindView(R.id.new_post_text) EditText descriptionText;

    private Uri fileUri;
    private Bitmap resizedBitmap;

    private static final int TC_PICK_IMAGE = 101;
    private static final int RC_CAMERA_PERMISSIONS = 102;

    private static final String[] cameraPerms = new String[]{
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);
        ButterKnife.bind(this);
    }

    public void onPostUploaded(final String error) {
        NewPostActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                submitButton.setEnabled(true);
                hideProgress();
                if (error == null) {
                    Toast.makeText(NewPostActivity.this, "Post created!", Toast.LENGTH_SHORT).show();
                    onBackPressed();
                } else {
                    Toast.makeText(NewPostActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @OnClick(R.id.new_post_picture) void onClickImage() {
        showImagePicker();
    }
    @OnClick(R.id.new_post_submit) void onClickSubmit() {

        if (resizedBitmap == null) {
            Toast.makeText(NewPostActivity.this, "Select an image first.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String postText = descriptionText.getText().toString();
        if (TextUtils.isEmpty(postText)) {
            descriptionText.setError(getString(R.string.error_required_field));
            return;
        }
        showProgress(R.string.post_upload_progress_message);
        submitButton.setEnabled(false);

        Long timestamp = System.currentTimeMillis();

        String bitmapPath = "/" + FirebaseUtility.getCurrentUserId() + "/full/" + timestamp.toString() + "/";
        uploadPost(resizedBitmap, bitmapPath, fileUri.getLastPathSegment(),
                postText);

    }

    @AfterPermissionGranted(RC_CAMERA_PERMISSIONS)
    private void showImagePicker() {
        // Check for camera permissions
        if (!EasyPermissions.hasPermissions(this, cameraPerms)) {
            EasyPermissions.requestPermissions(this,
                    "This sample will upload a picture from your Camera",
                    RC_CAMERA_PERMISSIONS, cameraPerms);
            return;
        }

        // Choose file storage location
        File file = new File(getExternalCacheDir(), UUID.randomUUID().toString());
        fileUri = Uri.fromFile(file);

        // Camera
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam){
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            cameraIntents.add(intent);
        }

        // Image Picker
        Intent pickerIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        Intent chooserIntent = Intent.createChooser(pickerIntent,
                getString(R.string.picture_chooser_title));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new
                Parcelable[cameraIntents.size()]));
        startActivityForResult(chooserIntent, TC_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TC_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                final boolean isCamera;
                if (data == null || data.getData() == null) {
                    isCamera = true;
                } else {
                    isCamera = MediaStore.ACTION_IMAGE_CAPTURE.equals(data.getAction());
                }
                if (!isCamera) {
                    fileUri = data.getData();
                }
                Log.d(TAG, "Received file uri: " + fileUri.getPath());

                resizeBitmap(fileUri, FULL_SIZE_MAX_DIMENSION);
            }
        }
    }

    public void onBitmapResized(Bitmap resizedBitmap, int mMaxDimension) {
        if (resizedBitmap == null) {
            Log.e(TAG, "Couldn't resize bitmap in background task.");
            Toast.makeText(getApplicationContext(), "Couldn't resize bitmap.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (mMaxDimension == FULL_SIZE_MAX_DIMENSION) {
            this.resizedBitmap = resizedBitmap;
            imageView.setImageBitmap(resizedBitmap);
        }

        if (resizedBitmap != null) {
            submitButton.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {}

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    public void resizeBitmap(Uri uri, int maxDimension) {
        LoadResizedBitmapTask task = new LoadResizedBitmapTask(maxDimension);
        task.execute(uri);
    }

    public void uploadPost(Bitmap bitmap, String inBitmapPath,
                           String inFileName, String inPostText) {
        UploadPostTask uploadTask = new UploadPostTask(bitmap, inBitmapPath, inFileName, inPostText);
        uploadTask.execute();
    }

    class UploadPostTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<Bitmap> bitmapReference;
        private String postText;
        private String fileName;
        private String bitmapPath;

        public UploadPostTask(Bitmap bitmap, String inBitmapPath,
                              String inFileName, String inPostText) {
            bitmapReference = new WeakReference<Bitmap>(bitmap);
            postText = inPostText;
            fileName = inFileName;
            bitmapPath = inBitmapPath;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            Bitmap fullSize = bitmapReference.get();
            if (fullSize == null) {
                return null;
            }
            FirebaseStorage storageRef = FirebaseStorage.getInstance();
            StorageReference photoRef = storageRef.getReferenceFromUrl("gs://" + getString(R.string.google_storage_bucket));


            Long timestamp = System.currentTimeMillis();
            final StorageReference fullSizeRef = photoRef.child(FirebaseUtility.getCurrentUserId()).child("full").child(timestamp.toString()).child(fileName + ".jpg");
            Log.d(TAG, fullSizeRef.toString());

            ByteArrayOutputStream fullSizeStream = new ByteArrayOutputStream();
            fullSize.compress(Bitmap.CompressFormat.JPEG, 90, fullSizeStream);
            byte[] bytes = fullSizeStream.toByteArray();
            fullSizeRef.putBytes(bytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    final Uri fullSizeUrl = taskSnapshot.getDownloadUrl();

                    ByteArrayOutputStream thumbnailStream = new ByteArrayOutputStream();

                    final DatabaseReference ref = FirebaseUtility.getBaseRef();
                    DatabaseReference postsRef = FirebaseUtility.getPostsRef();
                    final String newPostKey = postsRef.push().getKey();
                    final Uri thumbnailUrl = taskSnapshot.getDownloadUrl();

                    Author author = FirebaseUtility.getAuthor();
                    if (author == null) {
                        FirebaseCrash.logcat(Log.ERROR, TAG, "Couldn't upload post: Couldn't get signed in user.");
                        onPostUploaded(getString(R.string.error_user_not_signed_in));
                        return;
                    }
                    Post newPost = new Post(author, fullSizeUrl.toString(), fullSizeRef.toString(),
                             postText, ServerValue.TIMESTAMP);

                    Map<String, Object> updatedUserData = new HashMap<>();
                    updatedUserData.put(FirebaseUtility.getPeoplePath() + author.getUid() + "/posts/"
                            + newPostKey, true);
                    updatedUserData.put(FirebaseUtility.getPostsPath() + newPostKey,
                            new ObjectMapper().convertValue(newPost, Map.class));
                    ref.updateChildren(updatedUserData, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError firebaseError, DatabaseReference databaseReference) {
                            if (firebaseError == null) {
                                onPostUploaded(null);
                            } else {
                                Log.e(TAG, "Unable to create new post: " + firebaseError.getMessage());
                                FirebaseCrash.report(firebaseError.toException());
                                onPostUploaded(getString(R.string.error_upload_task_create));
                            }
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    FirebaseCrash.logcat(Log.ERROR, TAG, "Failed to upload post to database.");
                    FirebaseCrash.report(e);
                    onPostUploaded(getString(R.string.error_upload_task_create));
                }
            });
            return null;
        }
    }

    class LoadResizedBitmapTask extends AsyncTask<Uri, Void, Bitmap> {
        private int mMaxDimension;

        public LoadResizedBitmapTask(int maxDimension) {
            mMaxDimension = maxDimension;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Uri... params) {
            Uri uri = params[0];
            if (uri != null) {
                // TODO: Currently making these very small to investigate modulefood bug.
                // Implement thumbnail + fullsize later.
                Bitmap bitmap = null;
                try {
                    bitmap = decodeSampledBitmapFromUri(uri, mMaxDimension, mMaxDimension);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Can't find file to resize: " + e.getMessage());
                    FirebaseCrash.report(e);
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred during resize: " + e.getMessage());
                    FirebaseCrash.report(e);
                }
                return bitmap;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            onBitmapResized(bitmap, mMaxDimension);
        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private InputStream streamFromUri(Uri fileUri) throws FileNotFoundException {
        return new BufferedInputStream(getContentResolver().openInputStream(fileUri));
    }

    public Bitmap decodeSampledBitmapFromUri(Uri fileUri, int reqWidth, int reqHeight)
            throws IOException {

        // First decode with inJustDecodeBounds=true to check dimensions
        InputStream stream = streamFromUri(fileUri);
        stream.mark(stream.available());
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, options);
        stream.reset();

        // Decode bitmap with inSampleSize set
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        BitmapFactory.decodeStream(stream, null, options);
        stream.close();

        InputStream freshStream = streamFromUri(fileUri);
        return BitmapFactory.decodeStream(freshStream, null, options);
    }

}
