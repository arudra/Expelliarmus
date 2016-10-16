package com.example.main;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Connections;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Connections.ConnectionRequestListener,
        Connections.MessageListener,
        Connections.EndpointDiscoveryListener,
        View.OnClickListener {
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1;
    private GoogleApiClient mGoogleApiClient;

    private Spinner mTypeSpinner;
    private TextView mStatusText;
    private Button mConnectionButton;
    private Button mSendButton;
    private ListView mListView;
    private ViewGroup mSendTextContainer;
    private EditText mSendEditText;

    private ArrayAdapter<String> mMessageAdapter;

    private boolean mIsHost;
    private boolean mIsConnected;

    private String mRemoteHostEndpoint;
    private List<String> mRemotePeerEndpoints = new ArrayList<String>();

    private static final long CONNECTION_TIME_OUT = 10000L;

    //Wifi or Ethernet for TVs
    private static int[] NETWORK_TYPES = {ConnectivityManager.TYPE_WIFI,
            ConnectivityManager.TYPE_ETHERNET };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //https://code.tutsplus.com/tutorials/google-play-services-using-the-nearby-connections-api--cms-24534
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //checkVoiceRecognition();
        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect(); // this is where megs program starts
    }

    @Override
    protected void onStop() {
        super.onStop();
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            Nearby.Connections.stopAdvertising(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
    }
    public String convToSpell (String phrase){
        String lcPhrase = phrase.toLowerCase();
        String spell = "";
        String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        String payload = "";
        if( (lcPhrase.matches("l.*mos")) || (lcPhrase.equals("loomis")) || (lcPhrase.matches(".*most"))  ) {
            spell="lumos";
        }
        else if ((lcPhrase.matches("expel.*")) || (lcPhrase.matches("spell.*"))){
            spell="expelliarmus";
        }
        else if ((lcPhrase.matches("stupid.*")) || (lcPhrase.matches("super.*"))){
            spell="stupefy";
        }
        else if ((lcPhrase.matches("a.*c.*ra")) || (lcPhrase.matches("of.*c.*ra"))){
            spell="avada kedavra";
        }
        else if ((lcPhrase.matches("conf.*go")) || (lcPhrase.matches("confirm.*go"))){
            spell="confringo";
        }
        else if ((lcPhrase.matches("p.*t.*o")) || (lcPhrase.matches(".*ro.*t.*go"))){
            spell="protego";
        }
        else if ((lcPhrase.matches("chris.*o")) || (lcPhrase.matches("chris.*y.*")) || (lcPhrase.matches("crew.*o"))){
            spell="crucio";
        }
        else if (lcPhrase.matches(".*an.*send.*you")) {
            spell="incendio";
        }
        else{
            spell = lcPhrase;
        }
        payload = spell + "," + time;
        return payload;
    };
    /*public void checkVoiceRecognition() {
        // Check if voice recognition is present
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            mbtSpeak.setEnabled(false);
            mbtSpeak.setText("Voice recognizer not present");
            Toast.makeText(this, "Voice recognizer not present",
                    Toast.LENGTH_SHORT).show();
        }
    }*/
    public void speak() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // Specify the calling package to identify your application
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
                .getPackage().getName());

        // Display an hint to the user about what he should say.
        /*intent.putExtra(RecognizerIntent.EXTRA_PROMPT, metTextHint.getText()
                .toString());*/

        // Given an hint to the recognizer about what the user is going to say
        //There are two form of language model available
        //1.LANGUAGE_MODEL_WEB_SEARCH : For short phrases
        //2.LANGUAGE_MODEL_FREE_FORM  : If not sure about the words or phrases and its domain.
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);

        // If number of Matches is not selected then return show toast message
        /*if (msTextMatches.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
            Toast.makeText(this, "Please select No. of Matches from spinner",
                    Toast.LENGTH_SHORT).show();
            return;
        }*/

        int noOfMatches = 2;
        /*Integer.parseInt(msTextMatches.getSelectedItem()
                .toString());*/
        // Specify how many results you want to receive. The results will be
        // sorted where the first result is the one with higher confidence.
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, noOfMatches);
        //Start the Voice recognizer activity for the result.
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE)

            //If Voice recognition is successful then it returns RESULT_OK
            if(resultCode == RESULT_OK) {

                ArrayList<String> textMatchList = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                if (!textMatchList.isEmpty()) {
                    // convert phrase to spells
                    textMatchList.set(0, convToSpell (textMatchList.get(0)));
                    // If first Match contains the 'search' word
                    // Then start web search.
                    /*if (textMatchList.get(0).contains("search")) {

                        String searchQuery = textMatchList.get(0);
                        searchQuery = searchQuery.replace("search","");
                        Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
                        search.putExtra(SearchManager.QUERY, searchQuery);
                        startActivity(search);
                    } else {
                        // populate the Matches
                        mlvTextMatches
                                .setAdapter(new ArrayAdapter<String>(this,
                                        android.R.layout.simple_list_item_1,
                                        textMatchList));
                    }*/
                    mSendEditText.setText(textMatchList.get(0));
                }
                //Result code for various error.
            }else if(resultCode == RecognizerIntent.RESULT_AUDIO_ERROR){
                showToastMessage("Audio Error");
            }else if(resultCode == RecognizerIntent.RESULT_CLIENT_ERROR){
                showToastMessage("Client Error");
            }else if(resultCode == RecognizerIntent.RESULT_NETWORK_ERROR){
                showToastMessage("Network Error");
            }else if(resultCode == RecognizerIntent.RESULT_NO_MATCH){
                showToastMessage("No Match");
            }else if(resultCode == RecognizerIntent.RESULT_SERVER_ERROR){
                showToastMessage("Server Error");
            }
        super.onActivityResult(requestCode, resultCode, data);
    }
    void showToastMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void initViews() {
        mStatusText = (TextView) findViewById( R.id.text_status );
        mConnectionButton = (Button) findViewById( R.id.button_connection );
        mSendButton = (Button) findViewById( R.id.button_send );
        mListView = (ListView) findViewById( R.id.list );
        mSendTextContainer = (ViewGroup) findViewById( R.id.send_text_container );
        mSendEditText = (EditText) findViewById( R.id.edit_text_send );
        mTypeSpinner = (Spinner) findViewById( R.id.spinner_type );

        setupButtons();
        setupConnectionTypeSpinner();
        setupMessageList();

        mGoogleApiClient = new GoogleApiClient.Builder( this )
                .addConnectionCallbacks( this )
                .addOnConnectionFailedListener( this )
                .addApi( Nearby.CONNECTIONS_API )
                .build();
    }

    private void setupButtons() {
        mConnectionButton.setOnClickListener(this);
        mSendButton.setOnClickListener(this);
    }

    private void setupConnectionTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.connection_types,
                android.R.layout.simple_spinner_item );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mTypeSpinner.setAdapter(adapter);
    }

    private void setupMessageList() {
        mMessageAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1 );
        mListView.setAdapter( mMessageAdapter );
    }

    private boolean isConnectedToNetwork() {
        ConnectivityManager connManager =
                (ConnectivityManager) getSystemService( Context.CONNECTIVITY_SERVICE );
        for( int networkType : NETWORK_TYPES ) {
            NetworkInfo info = connManager.getNetworkInfo( networkType );
            if( info != null && info.isConnectedOrConnecting() ) {
                return true;
            }
        }
        return false;
    }

    private void disconnect() {
        if( !isConnectedToNetwork() )
            return;

        if( mIsHost ) {
            sendMessage( "Shutting down host" );
            Nearby.Connections.stopAdvertising( mGoogleApiClient );
            Nearby.Connections.stopAllEndpoints( mGoogleApiClient );
            mIsHost = false;
            mStatusText.setText( "Not connected" );
            mRemotePeerEndpoints.clear();
        } else {
            if( !mIsConnected || TextUtils.isEmpty( mRemoteHostEndpoint ) ) {
                Nearby.Connections.stopDiscovery( mGoogleApiClient, getString( R.string.service_id ) );
                return;
            }

            sendMessage( "Disconnecting" );
            Nearby.Connections.disconnectFromEndpoint( mGoogleApiClient, mRemoteHostEndpoint );
            mRemoteHostEndpoint = null;
            mStatusText.setText( "Disconnected" );
        }

        mIsConnected = false;
    }

    private void advertise() {
        if( !isConnectedToNetwork() )
            return;

        String name = "Nearby Advertising";

        Nearby.Connections.startAdvertising( mGoogleApiClient, name, null, CONNECTION_TIME_OUT, this ).setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
            @Override
            public void onResult(Connections.StartAdvertisingResult result) {
                if (result.getStatus().isSuccess()) {
                    mStatusText.setText("Advertising");
                }
            }
        });
    }

    private void discover() {
        if( !isConnectedToNetwork() )
            return;

        String serviceId = getString( R.string.service_id );
        Nearby.Connections.startDiscovery(mGoogleApiClient, serviceId, 10000L, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    mStatusText.setText( "Discovering" );
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionRequest(final String remoteEndpointId, final String remoteDeviceId, final String remoteEndpointName, byte[] payload) {
        if( mIsHost ) {
            Nearby.Connections.acceptConnectionRequest( mGoogleApiClient, remoteEndpointId, payload, this ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if( status.isSuccess() ) {
                        if( !mRemotePeerEndpoints.contains( remoteEndpointId ) ) {
                            mRemotePeerEndpoints.add( remoteEndpointId );
                        }

                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        mMessageAdapter.notifyDataSetChanged();
                        sendMessage(remoteDeviceId + " connected!");

                        mSendTextContainer.setVisibility( View.VISIBLE );
                    }
                }
            });
        } else {
            Nearby.Connections.rejectConnectionRequest(mGoogleApiClient, remoteEndpointId );
        }
    }

    private void sendMessage( String message ) {
        if( mIsHost ) {
            Nearby.Connections.sendReliableMessage(mGoogleApiClient, mRemotePeerEndpoints, message.getBytes());
            mMessageAdapter.add(message);
            mMessageAdapter.notifyDataSetChanged();
        } else {
            Nearby.Connections.sendReliableMessage( mGoogleApiClient, mRemoteHostEndpoint, ( Nearby.Connections.getLocalDeviceId( mGoogleApiClient ) + " says: " + message ).getBytes() );
        }
    }

    @Override
    public void onEndpointFound(String endpointId, String deviceId, final String serviceId, String endpointName) {
        byte[] payload = null;

        Nearby.Connections.sendConnectionRequest( mGoogleApiClient, deviceId, endpointId, payload, new Connections.ConnectionResponseCallback() {

            @Override
            public void onConnectionResponse(String s, Status status, byte[] bytes) {
                if( status.isSuccess() ) {
                    mStatusText.setText( "Connected to: " + s );
                    Nearby.Connections.stopDiscovery(mGoogleApiClient, serviceId);
                    mRemoteHostEndpoint = s;
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    mSendTextContainer.setVisibility(View.VISIBLE);

                    if( !mIsHost ) {
                        mIsConnected = true;
                    }
                } else {
                    mStatusText.setText( "Connection to " + s + " failed" );
                    if( !mIsHost ) {
                        mIsConnected = false;
                    }
                }
            }
        }, this );
    }

    @Override
    public void onEndpointLost(String s) {
        if( !mIsHost ) {
            mIsConnected = false;
        }
    }

    @Override
    public void onMessageReceived(String endpointId, byte[] payload, boolean isReliable) {
        mMessageAdapter.add( new String( payload ) );
        mMessageAdapter.notifyDataSetChanged();

        if( mIsHost ) {
            sendMessage( new String( payload ) );
        }
    }

    @Override
    public void onDisconnected(String s) {
        if( !mIsHost ) {
            mIsConnected = false;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if( !mIsHost ) {
            mIsConnected = false;
        }
    }

    @Override
    public void onClick(View v) {// this is where megs prog starts
        //https://code.tutsplus.com/tutorials/google-play-services-using-the-nearby-connections-api--cms-24534
        switch( v.getId() ) {
            case R.id.button_connection: {
                if( mIsConnected ) {
                    disconnect();
                    mStatusText.setText("Disconnected");
                }  else if( getString( R.string.connection_type_host ).equalsIgnoreCase( mTypeSpinner.getSelectedItem().toString() ) ) {
                    mIsHost = true;
                    advertise();
                }  else {
                    mIsHost = false;
                    discover();
                }
                break;
            }
            case R.id.button_send: {
                if( !TextUtils.isEmpty( mSendEditText.getText() ) && mIsConnected || ( mRemotePeerEndpoints != null && !mRemotePeerEndpoints.isEmpty() ) ) {
                    speak();
                    sendMessage( mSendEditText.getText().toString() );
                    mSendEditText.setText( "" );
                }
                break;
            }
        }
    }
}