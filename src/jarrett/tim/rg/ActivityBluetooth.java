package jarrett.tim.rg;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

public class ActivityBluetooth extends Activity
{
    private BluetoothServer bts;

    private Button buttonServer;
    private Button buttonDiscovery;
    private Button buttonShowDevices;
    private ListView deviceList;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth);
        
        //This will hook up all the bluetooth stuff
        bts = BluetoothServer.getInstance();
        
        //Got get all the layout references we need
        buttonServer = (Button)findViewById(R.id.bluetooth_server_button);
        buttonDiscovery = (Button)findViewById(R.id.bluetooth_discoverable_button);
        buttonShowDevices = (Button)findViewById(R.id.bluetooth_connect_to_server_button);
        deviceList = (ListView)findViewById(R.id.bluetooth_device_list);
        
        //Wire up the start server button
        buttonServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if ( bts.isServerRunning() ) {
                    bts.stopServer();
                    
                } else {                    
                    bts.startServer();
                    
                }
                
                updateServerButton((Button)v);
                
            }
            
        });
        updateServerButton(buttonServer);
        
        //Wire up the Start Discovery mode button
        buttonDiscovery.setOnClickListener(new View.OnClickListener()
        {   
            @Override
            public void onClick(View v)
            {
                bts.startDiscoverabilityMode();
                
            }
            
        });
        
        //Wire p the deviceList
        deviceList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> list, View view, int position, long id)
            {
                bts.setSelectedBluetoothDevice(position);
                
                getIntent().putExtra("device", bts.getSelectedBluetoothDevice());
                setResult(RESULT_OK, getIntent());
                
                
            }
            
        });
        
        deviceList.setAdapter(bts.getBluetoothDeviceListAdapter());
        
        //Wire up the Show Devices button
        buttonShowDevices.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if ( buttonShowDevices.getText().equals("Start Discovering Devices") ) {
                    bts.startDeviceDiscovery();
                    buttonShowDevices.setText("Stop Discovering Devices");
                    
                } else {
                    bts.stopDeviceDiscovery();
                    buttonShowDevices.setText("Start Discovering Devices");
                    
                }
                
                
            }
        });
        
    }//end onCreate
    
    @Override protected void onPause()
    {
        bts.stopDeviceDiscovery();
        super.onPause();
        
    }

    private void updateServerButton(Button b)
    {
        if ( bts.isServerRunning() ) {
            b.setText("Stop Server");
            //buttonDiscovery.setEnabled(true);
            //buttonShowDevices.setEnabled(false);
            
        } else {
            b.setText("Start Server");
            //buttonDiscovery.setEnabled(false);
            //buttonShowDevices.setEnabled(true);
            
        }
        
    }

}
