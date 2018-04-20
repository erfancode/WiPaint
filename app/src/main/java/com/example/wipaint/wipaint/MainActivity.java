package com.example.wipaint.wipaint;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements PaintListAdapter.ListItemClickListener
{
    private static final String TAG = "MainActivity";
    private static final String[] permissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private FloatingActionButton mStartButton;
    private RecyclerView paintList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        paintList = findViewById(R.id.paint_list);
        mStartButton = (FloatingActionButton) findViewById(R.id.add_new_paint);
        mStartButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent goToDrawing = new Intent(MainActivity.this, Draw.class);
                startActivity(goToDrawing);
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        paintList.setLayoutManager(layoutManager);
        paintList.setHasFixedSize(true);
        PaintListAdapter adapter = new PaintListAdapter(FetchImages(), this);
        paintList.setAdapter(adapter);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStart()
    {
        super.onStart();
        if(!hasPermission(this, permissions))
        {
            requestPermissions(permissions, 1);
        }
    }
    private static boolean hasPermission(Context context, String...permissions)
    {
        for(String permission : permissions)
        {
            if(ContextCompat.checkSelfPermission(context,permission) != PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onListItemClick(int clickItemIndex)
    {

    }
    private ArrayList<String> FetchImages() {

        ArrayList<String> filenames = new ArrayList<String>();
        String path = Environment.getExternalStorageDirectory().toString() + "/wipaint/";

        File directory = new File(path);
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++)
        {

            String file_name = files[i].getName();
            // you can store name to arraylist and use it later
            filenames.add(file_name);
        }
        return filenames;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        PaintListAdapter adapter = new PaintListAdapter(FetchImages(), this);
        paintList.setAdapter(adapter);

    }
}
