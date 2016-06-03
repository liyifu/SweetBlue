package com.idevicesinc.sweetblue;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.idevicesinc.sweetblue.listeners.DeviceStateListener;
import com.idevicesinc.sweetblue.listeners.DiscoveryListener;
import com.idevicesinc.sweetblue.listeners.ManagerStateListener;
import com.idevicesinc.sweetblue.listeners.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.Interval;
import java.util.ArrayList;
import java.util.List;
import com.idevicesinc.sweetblue.tester.R;
import com.idevicesinc.sweetblue.utils.Uuids;


public class MainActivity extends AppCompatActivity
{

    BleManager mgr;
    private ListView mListView;
    private Button mStartScan;
    private Button mStopScan;
    private ScanAdaptor mAdaptor;
    //private ArrayList<BleDevice> mDevices;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BleManagerConfig config = new BleManagerConfig();
        config.loggingEnabled = true;
        config.scanApi = BleScanAPI.POST_LOLLIPOP;
        config.bondBeforeConnecting = true;
        config.postCallbacksToUIThread = true;
        mgr = BleManager.get(this, config);

        mListView = (ListView) findViewById(R.id.listView);
        //mDevices = new ArrayList<>(0);
        mAdaptor = new ScanAdaptor(this, mgr.getDeviceList());
        mListView.setAdapter(mAdaptor);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                final BleDevice device = mgr.getDeviceList().get(position);
                device.setStateListener(new DeviceStateListener()
                {
                    @Override public void onEvent(StateEvent event)
                    {
                        if (event.didEnter(BleDeviceState.SERVICES_DISCOVERED))
                        {
                            device.read(Uuids.BATTERY_LEVEL, new ReadWriteListener()
                            {
                                @Override public void onEvent(final ReadWriteEvent e)
                                {
                                    if (e.wasSuccess())
                                    {
                                        new Handler(Looper.getMainLooper()).post(new Runnable()
                                        {
                                            @Override public void run()
                                            {
                                                Toast.makeText(MainActivity.this, "Battery level: " + e.data_byte(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                });
                device.connect();
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                BleDevice device = mgr.getDeviceList().get(position);
                if (device.is(BleDeviceState.CONNECTED))
                {
                    device.disconnect();
                    return true;
                }
                return false;
            }
        });

        mStartScan = (Button) findViewById(R.id.startScan);
        mStartScan.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick(View v)
            {
                //mgr.startPeriodicScan(Interval.FIVE_SECS, Interval.FIVE_SECS);
                mgr.startScan();
            }
        });
        mStopScan = (Button) findViewById(R.id.stopScan);
        mStopScan.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick(View v)
            {
                mgr.stopScan();
            }
        });


        mgr.setManagerStateListener(new ManagerStateListener()
        {
            @Override public void onEvent(StateEvent event)
            {
                if (event.didEnter(BleManagerState.ON))
                {
                    mStartScan.setEnabled(true);
                }
                else if (event.didEnter(BleManagerState.SCANNING))
                {
                    mStartScan.setEnabled(false);
                    mStopScan.setEnabled(true);
                }
                else if (event.didExit(BleManagerState.SCANNING) && !event.didEnter(BleManagerState.SCAN_PAUSED))
                {
                    mStartScan.setEnabled(true);
                    mStopScan.setEnabled(false);
                }
            }
        });
        mgr.setDiscoveryListener(new DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    //mDevices.add(e.device());
                    mAdaptor.notifyDataSetChanged();
                }
                else if (e.was(LifeCycle.REDISCOVERED))
                {

                }
            }
        });
        if (mgr.is(BleManagerState.OFF))
        {
            mStartScan.setEnabled(false);
            mgr.turnOn();
        }
        else
        {
            mStartScan.setEnabled(true);
        }
    }

    private class ScanAdaptor extends ArrayAdapter<BleDevice>
    {

        private List<BleDevice> mDevices;


        public ScanAdaptor(Context context, List<BleDevice> objects)
        {
            super(context, R.layout.scan_listitem_layout, objects);
            mDevices = objects;
        }

        @Override public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder v;
            if (convertView == null)
            {
                convertView = View.inflate(getContext(), R.layout.scan_listitem_layout, null);
                v = new ViewHolder();
                v.name = (TextView) convertView.findViewById(R.id.name);
                v.rssi = (TextView) convertView.findViewById(R.id.rssi);
                convertView.setTag(v);
            }
            else
            {
                v = (ViewHolder) convertView.getTag();
            }
            v.name.setText(mDevices.get(position).toString());
            //v.rssi.setText(String.valueOf(mDevices.get(position).getRssi()));
            return convertView;
        }

    }

    private static class ViewHolder
    {
        private TextView name;
        private TextView rssi;
    }
}
