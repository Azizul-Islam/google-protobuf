package io.left.hellomesh;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;

import io.left.rightmesh.android.AndroidMeshManager;
import io.left.rightmesh.android.MeshService;
import io.left.rightmesh.id.MeshID;
import io.left.rightmesh.mesh.MeshManager;
import io.left.rightmesh.mesh.MeshStateListener;
import io.left.rightmesh.util.MeshUtility;
import io.left.rightmesh.util.RightMeshException;
import io.reactivex.functions.Consumer;
import protobuf.HelloMessages;
import protobuf.HelloMessages.HelloMessage;
import protobuf.HelloMessages.MessageType;
import protobuf.HelloMessages.Message;

import static io.left.rightmesh.mesh.MeshManager.DATA_RECEIVED;
import static io.left.rightmesh.mesh.MeshManager.PEER_CHANGED;
import static io.left.rightmesh.mesh.MeshManager.REMOVED;

public class MainActivity extends Activity implements MeshStateListener {
    // Port to bind app to.
    private static final int HELLO_PORT = 9879;

    // MeshManager instance - interface to the mesh network.
    AndroidMeshManager mm = null;

    // Set to keep track of peers connected to the mesh.
    HashSet<MeshID> users = new HashSet<>();
    TextView txtReceived;

    /**
     * Called when app first opens, initializes {@link AndroidMeshManager} reference (which will
     * start the {@link MeshService} if it isn't already running.
     *
     * @param savedInstanceState passed from operating system
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtReceived = (TextView) findViewById(R.id.txtReceived);

        mm = AndroidMeshManager.getInstance(MainActivity.this, MainActivity.this, "HM-AZIZ");
    }

    /**
     * Called when activity is on screen.
     */
    @Override
    protected void onResume() {
        try {
            super.onResume();
            mm.resume();
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the app is being closed (not just navigated away from). Shuts down
     * the {@link AndroidMeshManager} instance.
     */
    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            mm.stop();
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called by the {@link MeshService} when the mesh state changes. Initializes mesh connection
     * on first call.
     *
     * @param uuid  our own user id on first detecting
     * @param state state which indicates SUCCESS or an error code
     */
    @Override
    public void meshStateChanged(MeshID uuid, int state) {
        if (state == MeshStateListener.SUCCESS) {
            try {

                // Binds this app to MESH_PORT.
                // This app will now receive all events generated on that port.
                mm.bind(HELLO_PORT);

                // Subscribes handlers to receive events from the mesh.
                mm.on(DATA_RECEIVED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handleDataReceived((MeshManager.RightMeshEvent) o);
                    }
                });
                mm.on(PEER_CHANGED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handlePeerChanged((MeshManager.RightMeshEvent) o);
                    }
                });

                // If you are using Java 8 or a lambda backport like RetroLambda, you can use
                // a more concise syntax, like the following:
                // mm.on(PEER_CHANGED, this::handlePeerChanged);
                // mm.on(DATA_RECEIVED, this::dataReceived);

                // Enable buttons now that mesh is connected.
                Button btnConfigure = (Button) findViewById(R.id.btnConfigure);
                Button btnSend = (Button) findViewById(R.id.btnHello);
                btnConfigure.setEnabled(true);
                btnSend.setEnabled(true);
            } catch (RightMeshException e) {
                String status = "Error initializing the library" + e.toString();
                Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT).show();
                TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
                txtStatus.setText(status);
                return;
            }
        }

        // Update display on successful calls (i.e. not FAILURE or DISABLED).
        if (state == MeshStateListener.SUCCESS || state == MeshStateListener.RESUME) {
            updateStatus();
        }
    }

    /**
     * Update the {@link TextView} with a list of all peers.
     */
    private void updateStatus() {
        String status = "uuid: " + mm.getUuid().toString() + "\n\n\npeers:\n";
        for (MeshID user : users) {
            status += user.toString() + "\n";
        }
        TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
        txtStatus.setText(status);
    }

    /**
     * Handles incoming data events from the mesh - toasts the contents of the data.
     *
     * @param e event object from mesh
     */
    int i = 0;

    private void handleDataReceived(MeshManager.RightMeshEvent e) {
        final MeshManager.DataReceivedEvent event = (MeshManager.DataReceivedEvent) e;
        try {
            final HelloMessage messageWrapper = HelloMessage.parseFrom(event.data);

            MessageType messageType = messageWrapper.getMessageType();
            if (messageType == MessageType.MESSAGE) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Message message = messageWrapper.getMessage();
                        Toast.makeText(MainActivity.this, message.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (InvalidProtocolBufferException e1) {
            e1.printStackTrace();
        }
    }


    /**
     * Handles peer update events from the mesh - maintains a list of peers and updates the display.
     *
     * @param e event object from mesh
     */
    private void handlePeerChanged(MeshManager.RightMeshEvent e) {
        // Update peer list.
        MeshManager.PeerChangedEvent event = (MeshManager.PeerChangedEvent) e;

        if (!mm.getUuid().equals(event.peerUuid)) {
            if (event.state != REMOVED && !users.contains(event.peerUuid)) {
                Log.e("Peer_state", "Peer Added =" + event.peerUuid);
                users.add(event.peerUuid);
            } else if (event.state == REMOVED) {
                Log.e("Peer_state", "Peer Removed =" + event.peerUuid);
                users.remove(event.peerUuid);
            }

            // Update display.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateStatus();
                }
            });
        } else {
            Log.e("Peer_state", "Self Id =" + event.peerUuid);
        }
    }

    /**
     * Sends "hello" to all known peers.
     *
     * @param v calling view
     */
    public void sendHello(View v) throws RightMeshException {
        String msg = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit.";


        for (MeshID receiver : users) {
            String new_msg = "Hello to: " + receiver + " from" + mm.getUuid() + msg;

            txtReceived.setText("Send Message Length: " + msg.length());

            HelloMessages.Message protoMsg = HelloMessages.Message.newBuilder()
                    .setMessage(new_msg)
                    .setMsgtime(System.currentTimeMillis())
                    .build();

            HelloMessages.HelloMessage message = HelloMessages.HelloMessage.newBuilder()
                    .setMessageType(HelloMessages.MessageType.MESSAGE)
                    .setMessage(protoMsg)
                    .build();

            sendDataReliable(receiver, message.toByteArray());


           /* for (int i = 0; i < 100; i++) {
                String new_msg2 = "Index: " + i + new_msg;
                sendDataReliable(receiver, new_msg2.getBytes());
            }*/
        }
    }

    private void sendDataReliable(MeshID receiver, byte[] msg) {
        try {
            mm.sendDataReliable(receiver, HELLO_PORT, msg);
        } catch (RightMeshException e) {

        }
    }

    public void castData(HashSet<MeshID> meshIDs, byte[] bytes) {
        mm.castData(meshIDs, HELLO_PORT, bytes, AndroidMeshManager.CastType.UNICAST, 0);
    }

    public void sendFile(View v) throws RightMeshException {
        if (InvokPermission.getInstance().requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            openFileChooser();
        }
    }

    private void sendFile2(String filePath) {
        byte[] fileBytes = getFileByteBuffer(filePath);

        if (fileBytes == null || fileBytes.length == 0) return;

        txtReceived.setText("Send File Length: " + fileBytes.length);

        for (MeshID receiver : users) {
            for (int i = 0; i < 10; i++) {
                sendDataReliable(receiver, fileBytes);
            }
        }

        for (int i = 0; i <= 10; i++) {
            //castData(users, fileBytes);
        }
    }


    private void openFileChooser() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("*/*");
        startActivityForResult(photoPickerIntent, 100);
    }

    private Cursor cursor = null;
    private String fileSource;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("Request Code", "Code=" + requestCode);
        if (requestCode == 100 && resultCode == RESULT_OK && null != data) {
            // Get the Image from data

            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {

                fileSource = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
                System.out.println("File Path = " + fileSource);
                //File sourceFile = new File(fileSource);
                sendFile2(fileSource);
            }
        }
    }

    /**
     * Open mesh settings screen.
     *
     * @param v calling view
     */
    public void configure(View v) {
        try {
            mm.showSettingsActivity();
        } catch (RightMeshException ex) {
            MeshUtility.Log(this.getClass().getCanonicalName(), "Service not connected");
        }
    }

    public static byte[] getFileByteBuffer(String fileUrl) {
        try {
            FileChannel fileChannel = new RandomAccessFile(fileUrl, "r").getChannel();
            long fileSize = fileChannel.size();
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
            int readBytes = fileChannel.read(buffer, 0);

            Log.e("Send_B", "Send byte = " + readBytes);
            fileChannel.close();
            return buffer.array();
        } catch (IOException e) {

        }
        return null;
    }
}

