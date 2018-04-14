package com.example.wipaint.wipaint;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Created by Erfan on 23/03/2018.
 */

public abstract class ConnectionsActivity extends AppCompatActivity
{
    private static final String TAG = "ConnectionsActivity";
    private ConnectionsClient mConnectionsClient;
    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();
    private boolean mIsConnectiong, mIsDiscovering, mIsAdvertising;
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback()
    {
        @Override
        public void onConnectionInitiated(String s, ConnectionInfo connectionInfo)
        {
            Log.d(TAG, String.format("onConnectionInitiated(endpointId=%s, endpointName=%s" ,s, connectionInfo.getEndpointName()));
            Endpoint endpoint = new Endpoint(s, connectionInfo.getEndpointName());
            mPendingConnections.put(s, endpoint);
            ConnectionsActivity.this.onConnectionInitiated(endpoint, connectionInfo);
        }

        @Override
        public void onConnectionResult(String s, ConnectionResolution connectionResolution)
        {
            Log.d(TAG, String.format("onConnectionResult(endpointId=%s, result=%s", s, connectionResolution));
            mIsConnectiong = false;
            if(!connectionResolution.getStatus().isSuccess())
            {
                Log.w(TAG, String.format("Connection Failed. Receiving status %s", ConnectionsActivity.toString(connectionResolution.getStatus())));
                onConnectionFailed(mPendingConnections.remove(s));
                return;
            }
            try
            {
                connectedToEndpoint(mPendingConnections.remove(s));
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(String s)
        {
            if(!mEstablishedConnections.containsKey(s))
            {
                Log.w(TAG, String.format("Unexpected disconnection from endpoint %s", s));
                return;
            }
            disconnectedFromEndpoint(mEstablishedConnections.remove(s));
        }
    };
    private final PayloadCallback mPayloadCallback = new PayloadCallback()
    {
        @Override
        public void onPayloadReceived(String s, Payload payload)
        {
            Log.d(TAG, String.format("onPayloadReceived(endpointId=%s, payload=%s", s, payload));
            onReceive(mEstablishedConnections.get(s), payload);
        }

        @Override
        public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate)
        {
            Log.d(TAG, String.format("onPayloadTransferUpdate(endpointId=%s, update=%s", s, payloadTransferUpdate));
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mConnectionsClient = Nearby.getConnectionsClient(this);
    }

    protected void startAdvertising()
    {
        mIsAdvertising = true;
        final String localEndpointName = getName();
        mConnectionsClient
            .startAdvertising(
                localEndpointName,
                getServiceId(),
                mConnectionLifecycleCallback,
                new AdvertisingOptions(getStrategy()))
            .addOnSuccessListener(new OnSuccessListener<Void>()
            {
                @Override
                public void onSuccess(Void aVoid)
                {
                    Log.v(TAG, String.format("Now Advertising endpoint " + localEndpointName));
                    onAdvertisingStarted();
                }
            })
            .addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    mIsAdvertising = false;
                    Log.w(TAG, String.format("Advertising Failed ! " + e));
                    onAdvertisingFailed();
                }
            });

    }
    protected void stopAdvertisiong()
    {
        mIsAdvertising = false;
        mConnectionsClient.stopAdvertising();
    }
    protected void acceptConnection(final Endpoint endpoint) {
        mConnectionsClient
                .acceptConnection(endpoint.getId(), mPayloadCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, String.format("acceptConnection() failed.", e));
                            }
                        });
    }
    protected void rejectConnection(Endpoint endpoint) {
        mConnectionsClient
                .rejectConnection(endpoint.getId())
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, String.format("rejectConnection() failed.", e));
                            }
                        });
    }
    protected void startDiscovering()
    {
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        mConnectionsClient
            .startDiscovery(
                    getServiceId(),
                    new EndpointDiscoveryCallback()
                    {
                        @Override
                        public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo)
                        {
                            Log.d(TAG, String.format("onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s",
                                    s, discoveredEndpointInfo.getServiceId(), discoveredEndpointInfo.getEndpointName()));
                            if(getServiceId().equals(discoveredEndpointInfo.getServiceId()))
                            {
                                Endpoint endpoint = new Endpoint(s, discoveredEndpointInfo.getEndpointName());
                                mDiscoveredEndpoints.put(s, endpoint);
                                onEndpointDiscovered(endpoint);
                            }
                        }

                        @Override
                        public void onEndpointLost(String s)
                        {
                            Log.d(TAG, String.format("onEndpointLost(endpointid=%s)", s));
                        }
                    },
                    new DiscoveryOptions(getStrategy()))
            .addOnSuccessListener(new OnSuccessListener<Void>()
            {
                @Override
                public void onSuccess(Void aVoid)
                {
                    onDiscoveryStarted();
                }
            })
            .addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    Log.w(TAG, String.format("startDiscovery failed !" + e));
                    mIsDiscovering = false;
                    onDiscoveryFailed();
                }
            });
    }
    protected void stopDiscovering()
    {
        mIsDiscovering = false;
        mConnectionsClient.stopDiscovery();
    }
    protected void disconnect(Endpoint endpoint)
    {
        mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        mEstablishedConnections.remove(endpoint.getId());
    }
    protected void disconnectFromAllEndpoints()
    {
        for(Endpoint endpoint : mEstablishedConnections.values())
        {
            mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        }
        mEstablishedConnections.clear();
    }
    protected void stopAllEndpoints()
    {
        mConnectionsClient.stopAllEndpoints();
        mIsDiscovering = false;
        mIsAdvertising = false;
        mIsConnectiong = false;
        mEstablishedConnections.clear();
        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
    }
    protected void connectToEndpoint(final Endpoint endpoint)
    {
        Log.v(TAG, String.format("Sending a connection request to endpoint : " + endpoint));
        mIsConnectiong = true;
        mConnectionsClient
                .requestConnection(getName(), endpoint.getId(), mConnectionLifecycleCallback)
                .addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        mIsConnectiong = false;
                        Log.w(TAG, String.format("request connection failed!" + e));
                        onConnectionFailed(endpoint);
                    }
                });
    }



    protected void onDiscoveryFailed(){}
    protected void onDiscoveryStarted(){}
    protected void onEndpointDiscovered(Endpoint endpoint) {}
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {}
    protected void onAdvertisingFailed(){}
    protected void onAdvertisingStarted(){}
    protected void onReceive(Endpoint endpoint, Payload payload){}
    protected void onEndpointDisconnected(Endpoint endpoint){}
    private void disconnectedFromEndpoint(Endpoint endpoint) {
        Log.d(TAG, String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
    }
    protected void onEndpointConnected(Endpoint endpoint) throws IOException
    {}
    private void connectedToEndpoint(Endpoint endpoint) throws IOException
    {
        Log.d(TAG, String.format("connectedToEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
    }
    protected void onConnectionFailed(Endpoint endpoint) {}
    private static String toString(Status status) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                        ? status.getStatusMessage()
                        : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }


    protected void send(Payload payload)
    {
        Log.d(TAG, "Sending payloads ");
        send(payload, mEstablishedConnections.keySet());
    }
    protected void send(Payload payload, Set<String> endpoints)
    {
        mConnectionsClient.sendPayload(new ArrayList<>(endpoints) , payload)
                .addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        Log.w(TAG, String.format("send payload failed!" + e));
                    }
                });
    }

    protected void sendData()
    {

    }

    protected static class Endpoint {
        @NonNull
        private final String id;
        @NonNull
        private final String name;
        private Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        public String getId()
        {
            return id;
        }

        @NonNull
        public String getName()
        {
            return name;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj != null && obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return id.hashCode();
        }

        @Override
        public String toString()
        {
            return String.format("Endpoint{id=%s, name=%s}", id, name);
        }
    }


    protected abstract String getName();
    protected abstract String getServiceId();
    protected abstract Strategy getStrategy();
}
