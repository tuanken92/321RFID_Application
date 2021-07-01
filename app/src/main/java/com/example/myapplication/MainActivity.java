package com.example.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.rfid.api3.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements  View.OnClickListener{

    String TAG = "atk";
    //GUI
    Button btnAdd;
    Button btnClear;
    ListView listview_data_tag;
    TextView tv_status;
    List<String> list_tag; //show UI
    ArrayAdapter<String> arrayAdapter;  //for display listview

    //RFID
    private static Readers readers;
    private static ArrayList availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private EventHandler eventHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init_UI();
        init_RFID();
        Log.i(TAG, "onCreate done!");

    }


    private void init_RFID()
    {
        Log.i(TAG, "Init RFID");
        // SDK
        if (readers == null) {
            Log.i(TAG, "readers = null");
            readers = new Readers(this, ENUM_TRANSPORT.SERVICE_SERIAL);
            Log.i(TAG, "readers = new Readers");
        }

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    if (readers != null) {
                        Log.i(TAG, "try to GetAvailableRFIDReaderList");
                        if (readers.GetAvailableRFIDReaderList() != null) {
                            Log.i(TAG, "get available RFID Reader");
                            availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                            if (availableRFIDReaderList.size() != 0) {
                                // get first reader from list
                                for(int i = 0; i < availableRFIDReaderList.size(); i++)
                                {
                                    Log.i(TAG, String.format("RFID Reader[%i] = %s", i, availableRFIDReaderList.get(i).toString()));
                                }
                                readerDevice = (ReaderDevice) availableRFIDReaderList.get(0);
                                reader = readerDevice.getRFIDReader();
                                if (!reader.isConnected()) {
                                    Log.i(TAG, "reader connecting");
                                    // Establish connection to the RFID Reader
                                    reader.connect();
                                    Log.i(TAG, "reader connected, configure reader");
                                    ConfigureReader();
                                    return true;
                                }
                                else
                                {
                                    Log.i(TAG, "reader is Connected");
                                }
                            }
                            else
                            {
                                Log.i(TAG, "availableRFIDReaderList = 0");
                            }
                        }
                        else
                        {
                            Log.i(TAG, "null -> GetAvailableRFIDReaderList");
                        }
                    }
                } catch (InvalidUsageException e) {
                    e.printStackTrace();
                    Log.e(TAG, "OperationFailureException " + e.getVendorMessage() + " " + e.getInfo());
                } catch (OperationFailureException e) {
                    e.printStackTrace();
                    Log.e(TAG, "OperationFailureException " + e.getVendorMessage() + " " + e.getResults());
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (aBoolean) {
                    Toast.makeText(getApplicationContext(), "Reader Connected", Toast.LENGTH_LONG).show();
                    tv_status.setText("Reader connected");
                }
            }
        }.execute();
    }
    void init_UI()
    {
        Log.i(TAG, "Init UI");
        btnAdd = (Button) findViewById(R.id.btnAdd);
        btnClear = (Button) findViewById(R.id.btnClearData);
        listview_data_tag = (ListView) findViewById(R.id.list_receive_data);
        tv_status = (TextView) findViewById(R.id.tv_status);


        list_tag = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list_tag);
        listview_data_tag.setAdapter(arrayAdapter);
        btnAdd.setOnClickListener(this);
        btnClear.setOnClickListener(this);
    }

    void addListViewData(String data)
    {
        String currentDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        list_tag.add(0, currentDateTime + ": " + data);
        arrayAdapter.notifyDataSetChanged();
        toask_message("added " + data);
    }

    void clearListViewData()
    {
        list_tag.clear();
        arrayAdapter.notifyDataSetChanged();
        toask_message("Clear data done!");
    }

    public void toask_message(String message)
    {
        Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = 10;
        char tempChar;
        for (int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(128));
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAdd:
                addListViewData(random());
                break;
            case R.id.btnClearData:
                clearListViewData();
                break;
            default:
                break;
        }
    }

    private void ConfigureReader() {

        if (reader.isConnected()) {
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
            try {
                // receive events from reader
                if (eventHandler == null)
                    eventHandler = new EventHandler();
                reader.Events.addEventsListener(eventHandler);
                // HH event
                reader.Events.setHandheldEvent(true);
                // tag event with tag data
                reader.Events.setTagReadEvent(true);
                // application will collect tag using getReadTags API
                reader.Events.setAttachTagDataWithReadEvent(false);
                // set trigger mode as rfid so scanner beam will not come
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                // set start and stop triggers
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
            } catch (InvalidUsageException e) {
                e.printStackTrace();
                Log.e(TAG, "error: " + e.getVendorMessage());
            } catch (OperationFailureException e) {
                e.printStackTrace();
                Log.e(TAG, "error: " + e.getVendorMessage());
            }
        }
        else
        {
            Log.i(TAG, "reader not yet connect");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (reader != null) {
                reader.Events.removeEventsListener(eventHandler);
                reader.disconnect();
                Toast.makeText(getApplicationContext(), "Disconnecting reader", Toast.LENGTH_LONG).show();
                reader = null;
                readers.Dispose();
                readers = null;
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read/Status Notify handler
    // Implement the RfidEventsLister class to receive event notifications
    public class EventHandler implements RfidEventsListener {
        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            Log.e(TAG, "eventReadNotify: " + e.getReadEventData().toString());
            // Recommended to use new method getReadTagsEx for better performance in case of large tag population
            TagData[] myTags = reader.Actions.getReadTags(100);
            if (myTags != null) {
                Log.i(TAG, String.format("Number Tag = %d", myTags.length));
                for (int index = 0; index < myTags.length; index++) {
                    //Log.d(TAG, "Tag ID " + myTags[index].getTagID());
                    Log.i(TAG, String.format("index = %d, Tag ID = %s", index, myTags[index].getTagID()));
                    if (myTags[index].getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                            myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                        if (myTags[index].getMemoryBankData().length() > 0) {
                            //Log.d(TAG, " Mem Bank Data " + myTags[index].getMemoryBankData());
                            Log.i(TAG, String.format("index = %d, Mem Bank Data  = %s", index, myTags[index].getMemoryBankData()));
                        }
                    }
                }
            }
        }

        // Status Event Notification
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                Log.i(TAG, "reader.Actions.Inventory.perform();");
                                reader.Actions.Inventory.perform();
                            } catch (InvalidUsageException e) {
                                e.printStackTrace();
                                Log.e(TAG, String.format("InvalidUsageException = %s, %s", e.getVendorMessage(), e.getInfo()));
                            } catch (OperationFailureException e) {
                                e.printStackTrace();
                                Log.e(TAG, String.format("error = %s", e.getStatusDescription(), e.getResults()));
                            }
                            return null;
                        }
                    }.execute();
                }
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            try {
                                Log.i(TAG, "reader.Actions.Inventory.stop();");
                                reader.Actions.Inventory.stop();
                            } catch (InvalidUsageException e) {
                                e.printStackTrace();
                                Log.e(TAG, String.format("InvalidUsageException = %s, %s", e.getVendorMessage(), e.getInfo()));
                            } catch (OperationFailureException e) {
                                e.printStackTrace();

                                Log.e(TAG, "OperationFailureException " + e.getVendorMessage() + " " + e.getResults());
                            }
                            return null;
                        }
                    }.execute();
                }
            }
        }
    }

}