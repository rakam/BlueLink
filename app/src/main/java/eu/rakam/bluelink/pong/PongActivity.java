package eu.rakam.bluelink.pong;

import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import eu.rakam.bluelink.R;
import eu.rakam.bluelinklib.BlueLinkClient;
import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.BlueLinkOutputStream;
import eu.rakam.bluelinklib.BlueLinkServer;
import eu.rakam.bluelinklib.Client;
import eu.rakam.bluelinklib.Server;
import eu.rakam.bluelinklib.callbacks.OnConnectToServerCallback;
import eu.rakam.bluelinklib.callbacks.OnOpenServerCallback;
import eu.rakam.bluelinklib.callbacks.OnSearchForServerCallback;


public class PongActivity extends ActionBarActivity {

    public static final String TAG = "BlueLink Pong";
    private static final String BASENAME = "Pong";

    private BlueLinkClient blueLinkClient;
    private BlueLinkServer blueLinkServer;
    private List<Server> servers = new LinkedList<>();
    private ArrayAdapter<Server> serverAdapter;
    private View UILayout;
    private PongSurfaceView surfaceView;
    private Model model = new Model();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pong);

        UILayout = findViewById(R.id.UILayout);
        surfaceView = (PongSurfaceView) findViewById(R.id.surfaceView);
        final Button searchForServersButton = (Button) findViewById(R.id.searchForServersButton);
        final Button startServerButton = (Button) findViewById(R.id.startServerButton);
        ListView serverListView = (ListView) findViewById(R.id.serverListView);

        serverAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, servers);
        serverListView.setAdapter(serverAdapter);
        serverListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Server server = servers.get(position);
                BlueLinkOutputStream out = new BlueLinkOutputStream();
                out.writeString(Build.MODEL);
                blueLinkClient.connectToServer(server, out, new OnConnectToServerCallback() {
                    @Override
                    public void onConnect(IOException e) {
                        if (e != null) {
                            Log.d(TAG, "Connection error : " + e);
                        } else {
                            Log.d(TAG, "Connected to " + server.getName());
                            startTestClient();
                        }
                    }
                });
            }
        });

        searchForServersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServerButton.setEnabled(false);
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                blueLinkClient = new BlueLinkClient(PongActivity.this, new Handler(), BASENAME,
                        "234eda5e-048e-4e75-8acc-b56b6e6cc9aa", new PongFactory(model, size.y, size.x), null);
                blueLinkClient.searchForServer(new OnSearchForServerCallback() {
                    @Override
                    public void onSearchStarted() {
                        Log.d(TAG, "Search Started");
                    }

                    @Override
                    public void onNewServer(Server server) {
                        Log.d(TAG, "On New Server : " + server.getName());
                        servers.add(server);
                        serverAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onSearchFinished(List<Server> servers) {
                        Log.d(TAG, "Search Finished");
                        startServerButton.setEnabled(true);
                    }
                });
            }
        });

        startServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchForServersButton.setEnabled(false);
                blueLinkServer = new BlueLinkServer(PongActivity.this, new Handler(), BASENAME,
                        "Super Server!", "234eda5e-048e-4e75-8acc-b56b6e6cc9aa", new MessageProcessor(model));
                blueLinkServer.openServer(new OnOpenServerCallback() {
                    @Override
                    public void onOpen(Exception e) {
                        if (e == null)
                            Log.d(TAG, "Server ON");
                        else
                            Log.d(TAG, "Error during server initialisation : " + e);
                    }

                    @Override
                    public void onNewClient(Client client, BlueLinkInputStream in) {
                        String clientName = in.readString();
                        Log.d(TAG, "New client : " + clientName);
                        startTestServer();
                    }
                });
            }
        });
    }

    private void startTestServer() {
        UILayout.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
        PongEngine engine = new PongEngine(this, blueLinkServer, model);
        surfaceView.setModel(model);
        new PongThread(this, surfaceView, engine).start();
    }

    private void startTestClient() {
        UILayout.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
        surfaceView.setModel(model);
        surfaceView.setBlueLinkClient(blueLinkClient);
        new PongThread(this, surfaceView, null).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (blueLinkClient != null)
            blueLinkClient.onActivityResult(requestCode, resultCode, data);
        if (blueLinkServer != null)
            blueLinkServer.onActivityResult(requestCode, resultCode, data);
    }

}
