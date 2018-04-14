package com.example.wipaint.wipaint;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.SerializablePermission;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collection;
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
        View.OnTouchListener i = new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                startSending();
                return false;
            }
        };
        paintView.setOnTouchListener(i);
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
    protected void onEndpointConnected(Endpoint endpoint) throws IOException
    {
        Toast.makeText(this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_LONG).show();
        Log.d("Draw", "onEndpoint connected sending ");
        startSending();
    }
    protected void onEndpointDisconnected(Endpoint endpoint)
    {
        Toast.makeText(this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_LONG).show();
    }
    protected void onReceive(Endpoint endpoint, Payload payload) // should complete in next time
    {
        ArrayList<FingerPath> paths = new ArrayList<>();

            ByteArrayInputStream in = new ByteArrayInputStream(payload.asBytes());
            try
            {
                ObjectInputStream is = new ObjectInputStream(in);
                Object o = is.readObject();
                if(o instanceof ArrayList<?>)
                {
                    if(!((ArrayList<?>)o).isEmpty()  & ((ArrayList<?>)o).get(0) instanceof FingerPath )
                    {
                        Log.v("Draw", " received msg is nstance of ArrayList<Fingerpath>");
                        paths.addAll((ArrayList<FingerPath>)o);
                    }
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            } catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            if(paths != null)
            {
                paintView.addPaths(paths);
            }

        /*try
        {
            Log.d("Draw", "receiving data");
            ByteArrayInputStream bis = new ByteArrayInputStream(payload.asBytes());
            ObjectInput in = new ObjectInputStream(bis);
            paths = (ArrayList<FingerPath>)in.readObject();
        } catch (IOException e )
        {
            e.printStackTrace();
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        paintView.getPaths().addAll(paths);*/
    }
    protected void startSending()
    {
        try
        {
            Log.d("Draw", "startSending");
            ParcelFileDescriptor[] payloadPipe = ParcelFileDescriptor.createPipe();



            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(paintView.getPaths());
            send(Payload.fromBytes(out.toByteArray()));

            /*send(Payload.fromStream(payloadPipe[0]));
            sendd(payloadPipe[1]);*/
            Log.d("Draw", "finishSending");
        } catch (IOException e)
        {
            Log.e("Draw", "startSending failed!");
        }
    }
    private void sendd(ParcelFileDescriptor file)
    {
        final OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(file);
        Thread thread;
        thread = new Thread()
        {
            @Override
            public void run()
            {

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try
                {
                    ObjectOutputStream os = new ObjectOutputStream(out);
                    os.writeObject(paintView.getPaths());
                    outputStream.write(out.toByteArray());
                    outputStream.flush();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }

            }
        };
        thread.start();
    }
    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return name;
    }
}
