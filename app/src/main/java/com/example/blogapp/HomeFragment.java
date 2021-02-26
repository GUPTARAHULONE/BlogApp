package com.example.blogapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.List;




public class HomeFragment extends Fragment {

    private RecyclerView blog_list_View;


    private List<BlogPost> blog_list;
    private List<User> userList;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth mAuth;
   private BlogRecyclerAdapter blogRecyclerAdapter;

    private DocumentSnapshot lastVisible;
    private Boolean isFirstPageFirstLoad = true;

    public HomeFragment() {
        // Required empty public constructor
    }





    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view=inflater.inflate(R.layout.fragment_home, container, false);

        blog_list = new ArrayList<>();
        userList = new ArrayList<>();
        blog_list_View = view.findViewById(R.id.blog_list_view);
        firebaseFirestore=FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();



        blogRecyclerAdapter = new BlogRecyclerAdapter( blog_list,userList);
        blog_list_View.setLayoutManager(new LinearLayoutManager(container.getContext()));
        blog_list_View.setAdapter(blogRecyclerAdapter);
        //blog_list_View.setHasFixedSize(true);

       if (mAuth.getCurrentUser()!=null) {
           firebaseFirestore = FirebaseFirestore.getInstance();

           blog_list_View.addOnScrollListener(new RecyclerView.OnScrollListener() {
               @Override
               public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                   super.onScrolled(recyclerView, dx, dy);

                   Boolean reachedBottom = !recyclerView.canScrollVertically(1);
                   if (reachedBottom){

                       loadMorePost();
                   }
               }
           });

           Query firstQuery=firebaseFirestore.collection("Posts").orderBy("timestamp",Query.Direction.DESCENDING).limit(3);

           firstQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
               @Override
               public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                   if (!value.isEmpty())
                   {
                       if (isFirstPageFirstLoad){
                       lastVisible = value.getDocuments().get(value.size() - 1);}
                       for (DocumentChange doc : value.getDocumentChanges()) {
                           if (doc.getType() == DocumentChange.Type.ADDED) {


                               String blogPostId=doc.getDocument().getId();
                               BlogPost blogPost = doc.getDocument().toObject(BlogPost.class).withId(blogPostId);

                              if (isFirstPageFirstLoad){
                                  blog_list.add(blogPost);
                              }
                              else{
                                  blog_list.add(0,blogPost);
                              }
                              blogRecyclerAdapter.notifyDataSetChanged();
                           }
                       }
                       isFirstPageFirstLoad=true;
                   }
               }
           });

       }


       return view;
    }

    public void loadMorePost()
    {
        Query nextQuery=firebaseFirestore.collection("Posts")
                .orderBy("timestamp",Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(3);

        nextQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                if (!value.isEmpty())
                {
                    if (isFirstPageFirstLoad){
                        lastVisible = value.getDocuments().get(value.size() - 1);}
                    for (DocumentChange doc : value.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED) {


                            String blogPostId=doc.getDocument().getId();
                            BlogPost blogPost = doc.getDocument().toObject(BlogPost.class).withId(blogPostId);

                            if (isFirstPageFirstLoad){
                                blog_list.add(blogPost);
                            }
                            else{
                                blog_list.add(0,blogPost);
                            }
                            blogRecyclerAdapter.notifyDataSetChanged();
                        }
                    }
                    isFirstPageFirstLoad=true;
                }
            }
        });
    }
}