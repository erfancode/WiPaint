package com.example.wipaint.wipaint;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.Random;

public class Draw extends ConnectionsActivity
{
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String SERVICE_ID = "com.example.wipaint.wipaint.SERVICE_ID";
    Context context ;
    private ConnectionsClient connectionsClient;
    private PaintView paintView;

    private String mName;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        mName = generateRandomName();
        disconnectFromAllEndpoints();
        startDiscovering();
        startAdvertising();
        context = getApplicationContext();
        paintView = (PaintView)findViewById(R.id.paint_view);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        paintView.init(metrics);
    }

    @Override
    protected String getName()
    {
        return mName;
    }

    @Override
    protected String getServiceId()
    {
        return SERVICE_ID;
    }

    @Override
    protected Strategy getStrategy()
    {
        return STRATEGY;
    }


    protected void onEndpointDiscovered(Endpoint endpoint)
    {
        stopDiscovering();
        connectToEndpoint(endpoint);
    }
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo)
    {
        acceptConnection(endpoint);
    }
    protected void onEndpointConnected(Endpoint endpoint)
    {
        Toast.makeText(this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_LONG).show();
    }
    protected void onEndpointDisconnected(Endpoint endpoint)
    {
        Toast.makeText(this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_LONG).show();
    }
    protected void onReceive(Endpoint endpoint, Payload payload) // should complete in next time
    {}
    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return name;
    }
}
