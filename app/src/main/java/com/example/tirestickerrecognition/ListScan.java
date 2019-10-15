package com.example.tirestickerrecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrInterface;

import java.util.ArrayList;
import java.util.List;

public class ListScan extends AppCompatActivity {
    private SlidrInterface slidr;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    Drawable backgroundImage;
    private RecyclerView.LayoutManager layoutManager;
    private List<Tire> tireDataset;
    Toolbar toolbar;
    private DatabaseReference mDatabaseRead;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_scan);
        tireDataset = new ArrayList<>();

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Scan History");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Reading data from database
        mDatabaseRead = FirebaseDatabase.getInstance().getReference().child("tires");

        mDatabaseRead.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Tire tire = snapshot.getValue(Tire.class);
                    tireDataset.add(tire);
                }
             Log.w("DATASET", ""+tireDataset);
                recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
                layoutManager = new LinearLayoutManager(getApplicationContext());
                recyclerView.setLayoutManager(layoutManager);

                mAdapter = new ListScanAdapter(tireDataset);
                recyclerView.setAdapter(mAdapter);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        slidr = Slidr.attach(this);


    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
