package com.example.blogapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import id.zelory.compressor.Compressor;

public class NewPostActivity extends AppCompatActivity {

    private Toolbar newPostToolbar;

    private ImageView newPostImage;
    private EditText newPostDesc;
    private Button newPostBtn;
    private ProgressBar newPostProgress;

    private Uri postImageURI = null;
    private String current_user_id;
    private Bitmap compressorImageFile;

    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth mAuth;
    private String downloadthumbUri;
    private String getDownloadUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        current_user_id = mAuth.getCurrentUser().getUid();

        newPostToolbar = findViewById(R.id.new_post_toolbar1);
        setSupportActionBar(newPostToolbar);
        getSupportActionBar().setTitle("Add New Post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        newPostImage = findViewById(R.id.new_post_image);
        newPostDesc = findViewById(R.id.new_post_desc);
        newPostProgress = findViewById(R.id.new_post_progress);
        newPostBtn = findViewById(R.id.post_btn);

        newPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(256, 256)
                        .start(NewPostActivity.this);
            }
        });

        newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String desc = newPostDesc.getText().toString();

                if (!TextUtils.isEmpty(desc) && postImageURI!=null)
                {
                    newPostProgress.setVisibility(View.VISIBLE);

                    final String randomString = UUID.randomUUID().toString();
                    //photo upload
                    File newImageFile = new File(postImageURI.getPath());
                    try {

                        compressorImageFile = new Compressor(NewPostActivity.this)
                                .setMaxHeight(100)
                                .setMaxWidth(100)
                                .setQuality(1)
                                .compressToBitmap(newImageFile);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressorImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] imageData = baos.toByteArray();

                   final StorageReference filepath=storageReference.child("Posts").child(randomString+".jpg");
                    UploadTask uploadTask = storageReference.child("Posts/thumbs")
                            .child(randomString + ".jpg").putBytes(imageData);
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            downloadthumbUri = taskSnapshot.getMetadata().getReference().getDownloadUrl().toString();
                            filepath.putFile(postImageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                                    if(task.isSuccessful())
                                    {
                                        filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                getDownloadUri = uri.toString();
                                                HashMap<String, Object> usermap = new HashMap<>();
                                                usermap.put("image_url", getDownloadUri);
                                                usermap.put("image_thumb", downloadthumbUri);
                                                usermap.put("desc", desc);
                                                usermap.put("user_id", current_user_id);
                                                usermap.put("timestamp", FieldValue.serverTimestamp());
                                                firebaseFirestore.collection("Posts").add(usermap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentReference> task) {
                                                        if (task.isSuccessful()) {
                                                            Toast.makeText(NewPostActivity.this, "Post Added Successful", Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(NewPostActivity.this, MainActivity.class);
                                                            startActivity(intent);
                                                            finish();
                                                        } else {
                                                            String error = task.getException().getMessage();
                                                            Toast.makeText(NewPostActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                            }
                                        });
                                    }

                                    else {
                                        String error = task.getException().getMessage();
                                        Toast.makeText(NewPostActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            String error = e.getMessage();
                            Toast.makeText(NewPostActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }


                else
                {

                    Toast.makeText(NewPostActivity.this, "Fill the contents first", Toast.LENGTH_SHORT).show();
                }

            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){
                postImageURI = result.getUri();
                newPostImage.setImageURI(postImageURI);
            }else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception e = result.getError();
            }
        }

    }
}

/*
 final String desc = newPostDesc.getText().toString();

                if (!TextUtils.isEmpty(desc) && postImageURI != null){
                    newPostProgress.setVisibility(View.VISIBLE);

                    final String randomString = UUID.randomUUID().toString();
                    //photo upload
                    File newImageFile = new File(postImageURI.getPath());
                    try {

                        compressorImageFile = new Compressor(NewPostActivity.this)
                                .setMaxHeight(720)
                                .setMaxWidth(720)
                                .setQuality(50)
                                .compressToBitmap(newImageFile);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressorImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] imageData = baos.toByteArray();
                    //photo upload
                    UploadTask filePath = storageReference.child("post_images").child(randomString + ".jpg").putBytes(imageData);
                    filePath.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            task.getResult().getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    File newThumbFile=new File(postImageURI.getPath());
                                    try {
                                        compressorImageFile = new Compressor(NewPostActivity.this)
                                                .setMaxHeight(100)
                                                .setMaxWidth(100)
                                                .setQuality(2)
                                                .compressToBitmap(newThumbFile);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    compressorImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                    byte[] thumbData = baos.toByteArray();

                                    UploadTask uploadTask = storageReference.child("post_images/thumbs")
                                            .child(randomString + ".jpg").putBytes(thumbData);
                                    uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                            task.getResult().getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri1) {

                                                    Map<String, Object> postMap = new HashMap<>();
                                                    postMap.put("image_url", uri);
                                                    postMap.put("image_thumb", uri1);
                                                    postMap.put("desc", desc);
                                                    postMap.put("user_id", current_user_id);
                                                    postMap.put("timestamp", FieldValue.serverTimestamp());
                                                    firebaseFirestore.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<DocumentReference> task) {
                                                            if (task.isSuccessful()){
                                                                Toast.makeText(NewPostActivity.this, "Post Added", Toast.LENGTH_SHORT).show();
                                                                Intent i=new Intent(NewPostActivity.this,MainActivity.class);
                                                                startActivity(i);
                                                                finish();

                                                            }else{

                                                            }
                                                            newPostProgress.setVisibility(View.INVISIBLE);
                                                        }
                                                    });
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {

                                                }
                                            });



                                        }
                                    });
                                }
                            });



                        }
                    });
                }

                else{
                    toastMessage("Fill the content first");
                    newPostProgress.setVisibility(View.INVISIBLE);
                }



 */

/*
 uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                            task.getResult().getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri1) {

                                                    Map<String, Object> postMap = new HashMap<>();
                                                    postMap.put("image_url", uri);
                                                    postMap.put("image_thumb", uri1);
                                                    postMap.put("desc", desc);
                                                    postMap.put("user_id", current_user_id);
                                                    postMap.put("timestamp", FieldValue.serverTimestamp());
                                                    firebaseFirestore.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<DocumentReference> task) {
                                                            if (task.isSuccessful()){
                                                                Toast.makeText(NewPostActivity.this, "Post Added", Toast.LENGTH_SHORT).show();
                                                                Intent i=new Intent(NewPostActivity.this,MainActivity.class);
                                                                startActivity(i);
                                                                finish();

                                                            }else{

                                                                Toast.makeText(NewPostActivity.this, "NOT UPLOADED", Toast.LENGTH_SHORT).show();
                                                            }
                                                            newPostProgress.setVisibility(View.INVISIBLE);
                                                        }
                                                    });
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {

                                                    Toast.makeText(NewPostActivity.this, "Failed???", Toast.LENGTH_SHORT).show();
                                                }
                                            });
 */