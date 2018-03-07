package com.example.tomcat.lollipop_ble;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


//  https://www.jianshu.com/p/be0f3cc505ec
//  http://www.davidgyoungtech.com/2017/08/07/beacon-detection-with-android-8

@TargetApi(21)
//public class MainActivity extends AppCompatActivity
public class MainActivity extends ActionBarActivity
{
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;

    TextView    tvInfoText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //set Discover device data structure
        filters = new ArrayList<>();
        ScanFilter.Builder  fb = new ScanFilter.Builder();
        fb.setServiceUuid(new ParcelUuid(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")));
        filters.add(fb.build());

        ScanSettings.Builder    sb = new ScanSettings.Builder();
        ScanSettings            settings = sb.build();

        //set UI component
        tvInfoText = (TextView)findViewById(R.id.textInfo);
        tvInfoText.setText("");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            if (Build.VERSION.SDK_INT >= 21)
            {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        //.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                        .build();
                //filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }

        tvInfoText.append("=> ");
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
        {
            scanLeDevice(false);
        }

        tvInfoText.setText("");
    }

    @Override
    protected void onDestroy()
    {
        if (mGatt == null)
        {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == Activity.RESULT_CANCELED)
            {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    //User define member function
    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    if (Build.VERSION.SDK_INT < 21)
                    {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                    else
                    {
                        mLEScanner.stopScan(mScanCallback);
                    }
                }
            }, SCAN_PERIOD);

            if (Build.VERSION.SDK_INT < 21)
            {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
            else
            {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        }
        else
        {
            if (Build.VERSION.SDK_INT < 21)
            {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
            else
            {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            //BluetoothDevice btDevice = result.getDevice();
            //connectToDevice(btDevice);
            tvInfoText.setText("" + result.getDevice().getName() + ": "
                    + result.getDevice().getAddress());
            connectToDevice(result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            for (ScanResult sr : results)
            {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode)
        {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    //Log.i("onLeScan", device.toString());
                    //connectToDevice(device);
                }
            });
        }
    };

    public void connectToDevice(BluetoothDevice device)
    {
        if (mGatt == null)
        {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState)
            {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;

                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            //gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
            switch (status)
            {
                case BluetoothGatt.GATT_SUCCESS:
                    break;

                case BluetoothGatt.GATT_FAILURE:
                    break;

                //case BluetoothGatt.STATE_DISCONNECTED
                //    break;

                default:
                    break;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };
}
