package com.example.tushar.servodoorlock;

import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private final String DEVICE_ADDRESS = "00:21:13:01:F8:54"; //MAC Address of Bluetooth Module
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothDevice device;
    private BluetoothSocket socket;

    private OutputStream outputStream;
    private InputStream inputStream;

    boolean connected = false;
    String command;

    Button lock_state_btn, unlock_state_btn, get_speech_btn, bluetooth_connect_btn;

    Map<String, String> map;
    ImageView lock_state_img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lock_state_btn = (Button) findViewById(R.id.lock_state_btn);
        unlock_state_btn = (Button) findViewById(R.id.unlock_state_btn);
        get_speech_btn = (Button) findViewById(R.id.btn_speak);
        bluetooth_connect_btn = (Button) findViewById(R.id.bluetooth_connect_btn);


        lock_state_img = (ImageView) findViewById(R.id.lock_state_img);

        map = new HashMap<String, String>();
        map.put("lock", "2");
        map.put("unlock", "1");
        map.put("open", "1");
        map.put("close", "2");



        bluetooth_connect_btn.setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v){

               if(BTinit())
               {
                   BTconnect();

                   // The code below sends the number 3 to the Arduino asking it to send the current state of the door lock so the lock state icon can be updated accordingly

                   command = "2";

                   try
                   {
                       outputStream.write(command.getBytes());
                   }
                   catch (IOException e)
                   {
                       e.printStackTrace();
                   }

               }
           }
        });

        ///*
        lock_state_btn.setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v){
                sendData("2");

           }
        });
        //*/

        unlock_state_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                sendData("1");
            }
        });

        ///*
        get_speech_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getSpeechInput(view);
            }
        });
        //*/

    }

    public void sendData(String message) {
        if(!connected)
        {
            Toast.makeText(getApplicationContext(), "Please establish a connection with the bluetooth servo door lock first", Toast.LENGTH_SHORT).show();
        }
        else
        {
            command = message;

            try
            {
                outputStream.write(command.getBytes()); // Sends the number 1 to the Arduino. For a detailed look at how the resulting command is handled, please see the Arduino Source Code
                Log.d("messange", "write successful");
                if(message.equals("1")) {
                    lock_state_img.setImageResource(R.drawable.unlocked_icon);
                } else {
                    lock_state_img.setImageResource(R.drawable.locked_icon);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void getSpeechInput(View view) {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, 10);
        } else {
            Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    commandFromText(result.get(0).toLowerCase());
                }
                break;
        }
    }

    void commandFromText(String data) {
        for(String word : data.split(" ")) {
            Log.d("Message is :", word);
            if(map.containsKey(word)) {
                Log.d("map value", map.get(word));
                sendData(map.get(word));
                break;
            }
        }
    }




    //Initializes bluetooth module
    public boolean BTinit()
    {
        boolean found = false;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null) //Checks if the device supports bluetooth
        {
            Toast.makeText(getApplicationContext(), "Device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
        }

        if(!bluetoothAdapter.isEnabled()) //Checks if bluetooth is enabled. If not, the program will ask permission from the user to enable it
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter,0);

            try
            {
                Thread.sleep(1000);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if(bondedDevices.isEmpty()) //Checks for paired bluetooth devices
        {
            Toast.makeText(getApplicationContext(), "Please pair the device first", Toast.LENGTH_SHORT).show();
        }
        else
        {
            for(BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device = iterator;
                    found = true;
                    break;
                }
            }
        }

        return found;
    }

    public boolean BTconnect()
    {

        try
        {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID); //Creates a socket to handle the outgoing connection
            socket.connect();

            Toast.makeText(getApplicationContext(),
                    "Connection to bluetooth device successful", Toast.LENGTH_LONG).show();
            connected = true;
        }
        catch(IOException e)
        {
            e.printStackTrace();
            connected = false;
        }

        if(connected)
        {
            try
            {
                outputStream = socket.getOutputStream(); //gets the output stream of the socket
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            //get the input stream if you want to get the data from arduino
            try
            {
                inputStream = socket.getInputStream(); //gets the input stream of the socket
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return connected;
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }
}


