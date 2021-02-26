package com.example.blogapp;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {

    //public final static String TAG = BlogRecyclerAdapter.class.getSimpleName();

    public List<BlogPost> blog_list;
    public List<User> userList;
    public Context context;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public BlogRecyclerAdapter(List<BlogPost> blog_list, List<User>userList)
    {
       // this.context=context;
        this.blog_list=blog_list;
        this.userList=userList;
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item, parent, false);
        context = parent.getContext();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //BlogPost blogPost = blog_list.get(position);
        // User user = userList.get(position);

        holder.setIsRecyclable(false);

        String blogPostId = blog_list.get(position).BlogPostId;
        String currentUserId = mAuth.getCurrentUser().getUid();

        String descData = blog_list.get(position).getDesc();
        holder.setDescText(descData);

        String imageUrl = blog_list.get(position).getImage_url();
        String thumbUri = blog_list.get(position).getImage_thumb();
        holder.setBlogImage(imageUrl, thumbUri);




        String user_id = blog_list.get(position).getUser_id();
        if (user_id.equals(currentUserId)){
            holder.blogDeleteBtn.setEnabled(true);
            holder.blogDeleteBtn.setVisibility(View.VISIBLE);
        }

        db.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()){
                    String userName = task.getResult().getString("name");
                    String userImage =task.getResult().getString("image");
                    holder.setUserData(userName, userImage);
                }
                else
                {

                }
            }
        });

        long milliseconds = blog_list.get(position).getTimestamp().getTime();
        String dateString = DateFormat.format("dd/MM/yyyy", new Date(milliseconds)).toString();
        holder.setTime(dateString);

        db.collection("Posts/" + blogPostId + "/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (!value.isEmpty()) {
                    int count = value.size();
                    holder.updateLikesCount(count);
                } else {
                    holder.updateLikesCount(0);
                }
            }
        });

        db.collection("Posts/" + blogPostId + "/Comments").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (!value.isEmpty()) {
                    int count = value.size();
                    holder.updateCommentCount(count);
                }
            }
        });

        db.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {

                if (value.exists()) {
                    holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.ic_like));
                } else {
                    holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.drawable.ic_like));
                }

            }
        });

        holder.blogLikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                        if (!task.getResult().exists()) {
                            Map<String, Object> likesMap = new HashMap<>();
                            likesMap.put("timestamp", FieldValue.serverTimestamp());
                            db.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).set(likesMap);
                        } else {
                            db.collection("Posts/" + blogPostId + "/Likes").document(currentUserId).delete();
                        }
                    }
                });


            }
        });


           holder.blogCommentBtn.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   Intent i = new Intent(context, CommentsActivity.class);
                   i.putExtra("blog_post_id", blogPostId);
                   context.startActivity(i);
               }
           });

           holder.blogDeleteBtn.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   db.collection("Posts").document(blogPostId).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                       @Override
                       public void onSuccess(Void aVoid) {
                           blog_list.remove(position);

                       }
                   });
               }
           });
    }
    @Override
    public int getItemCount() {
        return blog_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

       private View mView;
        private TextView descView, blogDate, blogUserName, blogLikeCount,usernameView,blogCommentCount;

        private ImageView blogImageView, blogLikeBtn, blogCommentBtn;
        private CircleImageView blogUserImage,profileImage;

        private Button blogDeleteBtn;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mView=itemView;
            blogLikeBtn = mView.findViewById(R.id.blog_like_btn);
            blogCommentBtn = mView.findViewById(R.id.blog_comment_icon);
            blogDeleteBtn = mView.findViewById(R.id.blog_delete_btn);
        }
        public void setDescText(String descText){
            descView = mView.findViewById(R.id.blog_desc);
            descView.setText(descText);
        }

       public void setBlogImage(String downloadUri,String thumbUri){
            blogImageView = mView.findViewById(R.id.blog_image);
           RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.crop_image_menu_flip);
            Glide.with(context).applyDefaultRequestOptions(requestOptions).load(downloadUri).thumbnail(Glide.with(context).load(thumbUri))
                    .into(blogImageView);
        }

       public void setTime(String date){
            blogDate = mView.findViewById(R.id.blog_date);
            blogDate.setText(date);
        }

        public void setUserData(String name, String image){
            blogUserImage = mView.findViewById(R.id.blog_user_image);
            blogUserName = mView.findViewById(R.id.blog_username);

            blogUserName.setText(name);

            RequestOptions placeholderOption = new RequestOptions();
            placeholderOption.placeholder(R.mipmap.profpic);

            Glide.with(context).applyDefaultRequestOptions(placeholderOption).load(image).into(blogUserImage);
        }

        public void updateLikesCount(int count){
            blogLikeCount = mView.findViewById(R.id.blog_like_count);
            blogLikeCount.setText(count + " Likes");
        }

        public void updateCommentCount(int count)
        {
           blogCommentCount=mView.findViewById(R.id.blog_comment_count);
           blogCommentCount.setText(count+" Comments");
        }
    }
}
