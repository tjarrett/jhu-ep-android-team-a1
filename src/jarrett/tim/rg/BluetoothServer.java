package jarrett.tim.rg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

public class BluetoothServer
{
    // dialog constants for reporting bluetooth status
    public static final int DIALOG_NO_BLUETOOTH = 11;
    public static final int DIALOG_WE_HAVE_BLUETOOTH = 12;
    public static final int DIALOG_BLUETOOTH_ENABLED = 13;
    public static final int DIALOG_BLUETOOTH_ALREADY_ENABLED = 14;
    public static final int DIALOG_USER_IS_EVIL = 15;

    // constants used for startActivityForResult calls, so we know which call is
    // responding
    public static final int BLUETOOTH_ENABLED = 21;
    public static final int SELECTING_DEVICE = 42;

    // constant to represent a message being passed from the server thread back
    // to our GUI thread
    public static final int BLUETOOTH_MESSAGE = 98;

    // a UUID that represents this service
    // THIS MUST BE UNIQUE FOR THE SERVICE AND BOTH THE CLIENT AND SERVER NEED
    // TO KNOW ITS VALUE
    public static final UUID uuid = UUID.fromString("bb37d560-09b3-11e1-be50-0800200c9a66");

    /**
     * The actual BluetoothServer instance -- there can be only one
     */
    private static BluetoothServer bts;

    /**
     * The activity that this server is attached to -- we need this so that I
     * can pop-up messages and the such
     */
    private Activity activity;

    /**
     * Whether or not the server is running currently
     */
    private boolean isRunning = false;

    /**
     * Our bluetoothAdapter
     */
    private BluetoothAdapter bluetoothAdapter;
    
    private Handler handler;

    // devices to show in our listview
    private List<BluetoothDeviceWrapper> wrappers = new ArrayList<BluetoothDeviceWrapper>();

    private BluetoothDeviceListAdapter bluetoothDeviceListAdapter = new BluetoothDeviceListAdapter();

    private int selectedBluetoothDevicePosition = -1;
    
    private BluetoothDevice selectedBluetoothServer;
    
    private AcceptThread serverThread;
    
    private ConnectedThread connectedThread;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if ( BluetoothDevice.ACTION_FOUND.equals(intent.getAction()) ) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if ( device != null ) {
                    bluetoothDeviceListAdapter.add(new BluetoothDeviceWrapper(device));

                }

            }

        }// end onReceive

    };

    /**
     * Can only be constructed from the factory() method
     * 
     * @param a
     */
    private BluetoothServer(Activity a, Handler handler)
    {
        activity = a;
        
        this.handler = handler;

    }// end BluetoothServer

    /**
     * Check that the bluetooth adapter is available and that it is active (if
     * not active, tries to activate it)
     */
    public void initBluetooth()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( bluetoothAdapter == null ) {
            activity.showDialog(DIALOG_NO_BLUETOOTH);

        }

        // Make sure bluetooth is enabled
        if ( !bluetoothAdapter.isEnabled() ) {
            activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BLUETOOTH_ENABLED);

        }

    }// end initBlueTooth

    /**
     * Get access to the adapter directly
     * 
     * @return
     */
    public BluetoothAdapter getBluetoothAdapter()
    {
        return bluetoothAdapter;

    }// end getBluetoothAdapter

    public List<BluetoothDeviceWrapper> getBluetoothDeviceWrappers()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setSelectedBluetoothDevice(int position)
    {
        for ( BluetoothDeviceWrapper wrapper : wrappers ) {
            wrapper.setSelected(false);

        }

        wrappers.get(position).setSelected(true);
        
        selectedBluetoothServer = wrappers.get(position).getBluetoothDevice();
        
        Log.d("Tim-Client", "Wire up thread to connected device");
        Thread connectThread = new ConnectThread(getSelectedBluetoothDevice());
        connectThread.start();

        bluetoothDeviceListAdapter.fireChange();

    }

    public BluetoothDevice getSelectedBluetoothDevice()
    {
        if ( selectedBluetoothServer != null ) {
            return selectedBluetoothServer;

        }

        return null;

    }

    /**
     * Returns true if the server is running, false otherwise
     * 
     * @return
     */
    public boolean isServerRunning()
    {
        return isRunning;

    }// end isServerRunning

    /**
     * Start the server (if it's not already running)
     */
    public void startServer()
    {
        if ( serverThread == null ) {
            serverThread = new AcceptThread();
            serverThread.start();
            isRunning = true;
        }

    }// end startServer

    /**
     * Stop the server
     */
    public void stopServer()
    {
        if ( serverThread != null ) {
            serverThread.cancel();
            serverThread = null;
            isRunning = false;
            
        }
        
        if ( connectedThread != null ) {
            connectedThread.cancel();
            connectedThread = null;
            
        }

    }// end stopServer

    /**
     * Kicks off discoverability mode for the default 120 seconds -- this is
     * only needed if we are trying to act as a server so if the server isn't
     * running it does nothing
     */
    public void startDiscoverabilityMode()
    {
        // Only if the server is running
        if ( isServerRunning() ) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            activity.startActivity(intent);

        }

    }// end startDiscoverabilityMode

    /**
     * Start device discovery
     */
    public void startDeviceDiscovery()
    {
        // Clear any devices already in the list
        wrappers.clear();

        // Reset the selected device
        selectedBluetoothDevicePosition = -1;
        selectedBluetoothServer = null;

        // Add in any devices already bonded
        for ( BluetoothDevice device : bluetoothAdapter.getBondedDevices() ) {
            bluetoothDeviceListAdapter.add(new BluetoothDeviceWrapper(device));

        }

        // Actually start the discovery
        bluetoothAdapter.startDiscovery();

    }// end startDeviceDiscovery

    /**
     * Stops device discovery
     */
    public void stopDeviceDiscovery()
    {
        bluetoothAdapter.cancelDiscovery();

    }// end stopDeviceDiscovery

    /**
     * Returns the list adapter for discovery devices
     * 
     * @return
     */
    public BluetoothDeviceListAdapter getBluetoothDeviceListAdapter()
    {
        return bluetoothDeviceListAdapter;

    }// end getBluetoothDeviceListAdapter

    /**
     * Return our pre-defined broadcast receiver
     * 
     * @return
     */
    public BroadcastReceiver getBroadcastReceiver()
    {
        return broadcastReceiver;

    }// end getBroadcastReceiver
    
    public void send(String msg)
    {
        if ( connectedThread != null ) {
            connectedThread.write(msg.getBytes());
        }
    }
    
    private void manageConnectedSocket(BluetoothSocket socket)
    {
        Log.d("Tim-Either", "Managing connected socket");
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        
    }

    /**
     * Build us one of these BluetoothServer objects
     * 
     * @param a
     * @return
     */
    static public BluetoothServer factory(Activity a, Handler handler)
    {
        if ( !isConfigured() ) {
            bts = new BluetoothServer(a, handler);
            return bts;

        } else {
            return getInstance();

        }

    }// end BluetoothServer

    /**
     * Gets the BluetoothServer object that has already been created. Returns
     * null if it hasn't been created yet
     * 
     * @return
     */
    static public BluetoothServer getInstance()
    {
        return bts;

    }// end getInstance

    /**
     * Returns true if we already have a BluetoothServer, false otherwise
     * 
     * @return
     */
    static public boolean isConfigured()
    {
        if ( bts == null ) {
            return false;

        } else {
            return true;

        }

    }// end isConfigured

    /**
     * A wrapper class that holds a device and its selection status for us to
     * display in a list
     */
    public static class BluetoothDeviceWrapper
    {
        // generate a unique id for each device (makes the listview happy)
        private static int nextId = 0;
        private int id = nextId++;
        private BluetoothDevice bluetoothDevice;
        private boolean selected;

        public BluetoothDeviceWrapper(BluetoothDevice bluetoothDevice)
        {
            this.bluetoothDevice = bluetoothDevice;
        }

        public BluetoothDevice getBluetoothDevice()
        {
            return bluetoothDevice;
        }

        public int getId()
        {
            return id;
        }

        public boolean isSelected()
        {
            return selected;
        }

        public void setSelected(boolean selected)
        {
            this.selected = selected;
        }
    }

    /** list adapter that displays the device information */
    private class BluetoothDeviceListAdapter implements ListAdapter
    {
        // list of data set observers to notify when the list data changes
        private List<DataSetObserver> observers = new ArrayList<DataSetObserver>();

        /** how many devices do we have? */
        @Override
        public int getCount()
        {
            return wrappers.size();
        }

        /** return the wrapped bluetooth device for this position */
        @Override
        public Object getItem(int position)
        {
            return wrappers.get(position);
        }

        /** return the id of the wrapped bluetooth device for this position */
        @Override
        public long getItemId(int position)
        {
            return wrappers.get(position).getId();
        }

        /**
         * return which view we want to use to display the device - all list
         * entries use the same id
         */
        @Override
        public int getItemViewType(int position)
        {
            return 0;
        }

        /**
         * get (or fill in an existing) view to display the wrapper at the
         * specified position
         */
        @Override
        public View getView(int position, View view, ViewGroup group)
        {
            if ( view == null )
                view = LayoutInflater.from(activity).inflate(R.layout.device_item, null);
            BluetoothDeviceWrapper wrapper = wrappers.get(position);
            TextView name = (TextView)view.findViewById(R.id.name);
            name.setText(wrapper.getBluetoothDevice().getName());
            TextView mac = (TextView)view.findViewById(R.id.mac);
            mac.setText(wrapper.getBluetoothDevice().getAddress());
            TextView status = (TextView)view.findViewById(R.id.status);
            if ( wrapper.isSelected() ) {
                name.setBackgroundColor(Color.YELLOW);
                mac.setBackgroundColor(Color.YELLOW);
                status.setBackgroundColor(Color.YELLOW);
            } else {
                name.setBackgroundColor(Color.WHITE);
                mac.setBackgroundColor(Color.WHITE);
                status.setBackgroundColor(Color.WHITE);
            }
            switch ( wrapper.getBluetoothDevice().getBondState() ) {
                case BluetoothDevice.BOND_BONDED:
                    status.setText("BONDED");
                    break;
                case BluetoothDevice.BOND_BONDING:
                    status.setText("BONDING");
                    break;
                case BluetoothDevice.BOND_NONE:
                    status.setText("NOT BONDED");
                    break;
            }
            return view;
        }

        /** we use the same view type for all entries in the list */
        @Override
        public int getViewTypeCount()
        {
            return 1;
        }

        /** all entries have consistent ids */
        @Override
        public boolean hasStableIds()
        {
            return true;
        }

        /** tell if the list is empty */
        @Override
        public boolean isEmpty()
        {
            return wrappers.isEmpty();
        }

        /** all items are always enabled */
        @Override
        public boolean areAllItemsEnabled()
        {
            return true;
        }

        /** all items are always enabled */
        @Override
        public boolean isEnabled(int position)
        {
            return true;
        }

        /** report that something in the list has changed so it will be redrawn */
        private void fireChange()
        {
            for ( DataSetObserver observer : observers ) {
                observer.onChanged();
            }
        }

        /** add an observer */
        @Override
        public void registerDataSetObserver(DataSetObserver observer)
        {
            observers.add(observer);
        }

        /** remove an observer */
        @Override
        public void unregisterDataSetObserver(DataSetObserver observer)
        {
            observers.remove(observer);
        }

        /** add a new wrapper to the list */
        public void add(BluetoothDeviceWrapper wrapper)
        {
            wrappers.add(wrapper);
            fireChange();
        }
    }// end BluetoothDeviceListAdapter

    /**
     * The server thread. This thread will listen for incoming requests and then
     * transfer them to the GUI thread for processing.
     */
    private class ServerThread extends Thread
    {
        // the server socket that listens for requests
        private BluetoothServerSocket serverSocket;
        
        private BluetoothSocket socket;

        public ServerThread()
        {
            try {
                // start listening for requests to our UUID for the service
                serverSocket = getBluetoothAdapter().listenUsingRfcommWithServiceRecord("Rube", BluetoothServer.uuid);

            } catch ( IOException e ) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Our listening loop - infinite until an error occurs, including
         * stopping the server by closing its socket. I'm looking into better
         * ways to do this so we can tell the difference between a real error
         * and cancel()
         */
        @Override
        public void run()
        {
            Log.d("Tim-Server", "Running server thread...");
            while ( true ) {
                try {
                    byte[] buffer = new byte[128];
                    int numRead = 0;
                    socket = null;
                    try {                        
                        // accept the incoming client connection
                        socket = serverSocket.accept();
                        // read the data from the client
                        // note that all of our data happens to fit in this
                        // buffer; longer data may require
                        // multiple reads to obtain all the data
                        // you may want to send the length of the data first,
                        // followed by the data
                        numRead = socket.getInputStream().read(buffer);
                        
                        
                        
                        
                        
                        
                        
                        Log.d("Tim-Server", "Reading stuff...");
                        // send an ack to the client so they know it's ok to
                        // close
                        //socket.getOutputStream().flush();
                    } finally {
                        if ( socket != null ) {
                            Log.d("Tim-Server", "Socket closed");
                            socket.close();
                            
                        }
                    }
                    handler.obtainMessage(BLUETOOTH_MESSAGE, numRead, -1, buffer).sendToTarget();
                } catch ( IOException e ) {
                    // EEEEEEWWWW!!!
                }
            }
        }
        
        public void sendResponse(String response)
        {
            try {
                socket.getOutputStream().write(response.getBytes());
                socket.getOutputStream().flush();
                
            } catch ( IOException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            
        }

        public void cancel()
        {
            try {
                serverSocket.close();
                Log.d("Tim-Server", "Closed socket");
            } catch ( IOException e ) {
                // ignore me... ewwwwwwww...
            }
        }
    }// end ServerThread
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private class AcceptThread extends Thread
    {
        private final BluetoothServerSocket serverSocket;
        
        public AcceptThread()
        {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Rube", BluetoothServer.uuid);
                
            } catch ( IOException ioe ) {
                
            }
            
            serverSocket = tmp;
            
        }
        
        public void run()
        {
            BluetoothSocket socket = null;
            
            // Keep listening until exception occurs or a socket is returned
            while ( true ) {
                try {
                    socket = serverSocket.accept();
                    
                } catch ( IOException ioe ) {
                    break;
                    
                }
                
                //If a connection was accepted...
                if ( socket != null ) {
                    //Do work to manage the connection elsewhere
                    manageConnectedSocket(socket);
                    try {
                        serverSocket.close();
                        
                    } catch ( IOException ioe ) {
                        break;
                        
                    }
                    
                    break;
                    
                }
                
            }//end while
            
        }//end run

        public void cancel()
        {
            try {
                serverSocket.close();
                
            } catch ( IOException ioe ) {
                
            }
            
        }//end cancel
        
    }//end AcceptThread
    
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
     
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
     
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
     
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
     
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
     
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    handler.obtainMessage(BLUETOOTH_MESSAGE, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
     
        /* Call this from the main Activity to send data to the remote device */
        public void write(byte[] bytes) 
        {
            String test = new String(bytes);
            Log.d("Tim-Server", "Trying to write out " + test);
            
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
     
        /* Call this from the main Activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { 
                e.printStackTrace();
            }
        }
    }
    
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
     
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
     
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(BluetoothServer.uuid);
            } catch (IOException e) { 
                Log.d("Tim-Client", "Could not open RF comm socket");
                e.printStackTrace();
                
            }
            mmSocket = tmp;
        }
     
        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();
            
            Log.d("Tim-Client", "Running client thread...");
     
            try {
                // Do work to manage the connection (in a separate thread)
                Log.d("Tim-Client", "Managing socket");
                
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { 
                    Log.d("Tim-Client", "Connection closed");
                    
                }
                
                Log.d("Tim-Client", "Returning from ConnectThread");
                return;
            }
            
            manageConnectedSocket(mmSocket);
     
        }
     
        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}// end BluetoothServer
