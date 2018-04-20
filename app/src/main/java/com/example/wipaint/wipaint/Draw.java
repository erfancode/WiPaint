package com.example.wipaint.wipaint;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.location.Location;
import android.media.Image;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnSuccessListener;

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
import java.util.List;
import java.util.Random;

import me.priyesh.chroma.ChromaDialog;
import me.priyesh.chroma.ColorMode;
import me.priyesh.chroma.ColorSelectListener;

public class Draw extends ConnectionsActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String SERVICE_ID = "com.example.wipaint.wipaint.SERVICE_ID";
    Context context;
    private ConnectionsClient connectionsClient;
    private PaintView paintView;
    private String mName;
    private ImageButton selectColor, emboss, blur, brushSize, eraser, clear, save;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        selectColor = findViewById(R.id.select_color_btn);
        emboss = findViewById(R.id.emboss_btn);
        blur = findViewById(R.id.blur_btn);
        brushSize = findViewById(R.id.brush_size_btn);
        eraser = findViewById(R.id.eraser_btn);
        clear = findViewById(R.id.clear_btn);
        save = findViewById(R.id.save_btn);
        selectColor.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(!paintView.eraser)
                {
                    new ChromaDialog.Builder()
                            .initialColor(paintView.getCurrentColor())
                            .colorMode(ColorMode.RGB) // There's also ARGB and HSV
                            .onColorSelected(new ColorSelectListener()
                            {
                                @Override
                                public void onColorSelected(int i)
                                {
                                    paintView.setCurrentColor(i);
                                    Log.v("Draw", "selected color = " + String.valueOf(i));
                                }
                            })
                            .create()
                            .show(getSupportFragmentManager(), "ChromaDialog");
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "you can't chose color \nyou are in eraser mode", Toast.LENGTH_LONG).show();
                }
            }
        });

        brushSize.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                showChangeBrushSizeDialog();
            }
        });
        blur.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(paintView.blur)
                {
                    paintView.normal();
                    blur.setImageResource(R.drawable.blur_off_btn);
                    Toast.makeText(getApplicationContext(), R.string.blur_off, Toast.LENGTH_LONG).show();
                }
                else
                {
                    paintView.blur();
                    blur.setImageResource(R.drawable.blur_on_btn);
                    emboss.setImageResource(R.drawable.emboss_off_btn);
                    Toast.makeText(getApplicationContext(), R.string.blur_on,  Toast.LENGTH_LONG).show();
                }
            }
        });

        emboss.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(paintView.emboss)
                {
                    paintView.normal();
                    emboss.setImageResource(R.drawable.emboss_off_btn);
                    Toast.makeText(getApplicationContext(), R.string.emboss_off, Toast.LENGTH_LONG).show();
                }
                else
                {
                    paintView.emboss();
                    emboss.setImageResource(R.drawable.emboss_btn);
                    blur.setImageResource(R.drawable.blur_off_btn);
                    Toast.makeText(getApplicationContext(), R.string.emboss_on, Toast.LENGTH_LONG).show();
                }
            }
        });
        clear.setOnClickListener(new View.OnClickListener()
             {
                 @Override
                 public void onClick(View view)
                 {
                     paintView.clear();
                     Toast.makeText(getApplicationContext(), R.string.paint_cleared, Toast.LENGTH_LONG).show();
                 }
             }
        );
        eraser.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(!paintView.eraser)
                {
                    paintView.setCurrentColor(paintView.DEFAULT_BG_COLOR);
                    eraser.setImageResource(R.drawable.eraser_btn);
                    paintView.eraser = true;
                }
                else
                {
                    paintView.setCurrentColor(Color.RED);
                    eraser.setImageResource(R.drawable.eraser_off_btn);
                    paintView.eraser = false;
                }
            }
        });
        save.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                paintView.savePaint();
            }
        });
        mName = generateRandomName();
        disconnectFromAllEndpoints();
        startDiscovering();
        startAdvertising();
        context = getApplicationContext();
        paintView = (PaintView)findViewById(R.id.paint_view);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        paintView.init(metrics);
       /* View.OnTouchListener i = new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                startSending();
                return false;
            }
        };
        paintView.setOnTouchListener(i);*/
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
    @Override
    protected void onReceive(Endpoint endpoint, Payload payload)
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            Log.d("Draw", "startSending");
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(paintView.getPaths());
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(out.toByteArray()));
            Log.d("Draw", "finishSending");
        } catch (IOException e)
        {
            Log.e("Draw", "startSending failed!");
        }
    }

    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return name;
    }

    void showChangeBrushSizeDialog()
    {
        final int[] currentSize = {1};
        final AlertDialog.Builder brushSize = new AlertDialog.Builder(this);
        final SeekBar size = new SeekBar(this);
        size.setMax(50);
        brushSize.setTitle(R.string.brush_size_dialog_msg);
        brushSize.setView(size);
        size.setProgress(paintView.getStrokeWidth());
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                currentSize[0] = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });
        brushSize.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                paintView.setStrokeWidth(currentSize[0]);
                size.setProgress(i);
                dialogInterface.dismiss();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                dialogInterface.dismiss();
            }
        }).create().show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {

    }

    @Override
    public void onConnectionSuspended(int i)
    {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {

    }

}
