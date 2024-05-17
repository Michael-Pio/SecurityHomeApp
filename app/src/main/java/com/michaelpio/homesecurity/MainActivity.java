package com.michaelpio.homesecurity;

import android.app.Service;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    DeviceState deviceState;
    private ConnectionStatusUI connectionStatusUI;
    private TemperatureHumidityUI temperatureHumidityUI;
    private IntruderUI intruderUI;
    private ArmDisarmUI armDisarmUI;
    private AuthenticateUI authenticateUI;
    private boolean isAuthenticated;
    ProgressBar progressBar;

    /*Component 1*/
    private class ConnectionStatusUI {
        TextView TVDeviceConnection , TVMobileConnection;
        Button BTNConnect;


        void init_UIElements(){
            TVDeviceConnection = findViewById(R.id.TVDeviceConnection);
            TVMobileConnection = findViewById(R.id.TVMobileConnection);
            BTNConnect = findViewById(R.id.BTNConnect);
            TVDeviceConnection.setText("Device\nFetching ...");
            TVMobileConnection.setText("Mobile\nFetching ...");
        }

        void set_Listeners(){
            BTNConnect.setOnClickListener(View -> {
                deviceState.setIsMobileConnected(mqttClientManager.reconnect());
                mqttClientManager.setMessageHandler(getMessageHandler());
            });
        }

        public void update_MobileConnection(Boolean isMobileConnected) {
            if (isMobileConnected) {
                TVMobileConnection.setText("Mobile\nconnected");
                TVMobileConnection.setBackgroundColor(getColor(R.color.green));
            } else {
                TVMobileConnection.setText("Mobile\nDisconnected");
                TVMobileConnection.setBackgroundColor(getColor(R.color.red));

            }
        }
        public void update_DeviceConnection(Boolean isDeviceConnected){
            if(isDeviceConnected){
                TVDeviceConnection.setText("Device\nConnected");
                TVDeviceConnection.setBackgroundColor(getColor(R.color.green));
            }else{
                TVDeviceConnection.setText("Device\ndisconnected");
                TVDeviceConnection.setBackgroundColor(getColor(R.color.red));
            }
        }
    };

    /*Component 2*/
    private class TemperatureHumidityUI{
        ImageView IMTemperature , IMHumidity;
        TextView tvTemperature , tvHumidity;

        void init_UIElements(){
            IMTemperature = findViewById(R.id.IMTemperature);
            IMHumidity = findViewById(R.id.IMHumidity);
            tvTemperature = findViewById(R.id.tvTemperature);
            tvHumidity = findViewById(R.id.tvHumidity);
            setIMHumidity(deviceState.getHumidity().getValue());
            setIMTemperature(deviceState.getTemperature().getValue());
        }

        void set_Listeners(){


        }

        public void setIMTemperature(int temperature){
            int tintColor = getResources().getColor(R.color.Temperature_NORMAL);
            if(temperature<10){     //Cold
                tintColor = getResources().getColor(R.color.Temperature_COLD);
            } else if (temperature<30) {        //Normal
                tintColor = getResources().getColor(R.color.Temperature_NORMAL);
            }else{      //Hot
                tintColor = getResources().getColor(R.color.Temperature_HOT);
            }
            tvTemperature.setTextColor(tintColor);
            IMTemperature.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
            String myText = temperature+"°C";
            tvTemperature.setText(myText);
        }

        public void setIMHumidity(int humidityPercentage){
            int tintColor = getResources().getColor(R.color.Temperature_NORMAL);
            if(humidityPercentage<20){     //Low
                tintColor = getResources().getColor(R.color.Temperature_COLD);
            } else if (humidityPercentage<70) {        //Normal
                tintColor = getResources().getColor(R.color.Temperature_NORMAL);
            }else{      //HIGH
                tintColor = getResources().getColor(R.color.Temperature_HOT);
            }
            tvHumidity.setTextColor(tintColor);
            IMHumidity.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
            String myText = humidityPercentage+"%";
            tvHumidity.setText(myText);
        }
    };

    /*Component 3*/
    private class IntruderUI{
        ImageView IM_Intruder;

        void init_UIElements(){
            IM_Intruder = findViewById(R.id.IM_Intruder);
        }

        void set_Listeners(){

        }

        void update_InteruderUI(boolean isDetected){
            if(isDetected){
                IM_Intruder.setImageResource(R.drawable.intruder_alert);
            }
            else{
                IM_Intruder.setImageResource(R.drawable.intruder_safe);
            }
        }
    };

    /*Component 4*/
    private class ArmDisarmUI {
        CardView cardARM;
        ImageView IMArmDisarm;
        Button BTNarm, BTNdisarm;



        void init_UIElements(){
            cardARM = (CardView) findViewById(R.id.cardARM);
            IMArmDisarm = (ImageView) findViewById(R.id.IMArmDisarm);
            BTNarm = (Button) findViewById(R.id.BTNarm);
            BTNdisarm = (Button) findViewById(R.id.BTNdisarm);
        }

        void set_Listeners(){
            BTNarm.setOnClickListener(view -> {
                if(isAuthenticated){
                    deviceState.setIsArmed(true);
                    deviceState.setHumidity(deviceState.getHumidity().getValue() + 3);
                    deviceState.setTemperature(deviceState.getTemperature().getValue() + 1);
                    mqttClientManager.publishMessage("MainGate001/App/isARM", "1", false);
                }else{
                    Toast.makeText(MainActivity.this, "Please Authenticate First", Toast.LENGTH_SHORT).show();
                }

           });
            BTNdisarm.setOnClickListener(view -> {
                if(isAuthenticated) {
                    deviceState.setHumidity(deviceState.getHumidity().getValue() - 2);
                    deviceState.setTemperature(deviceState.getTemperature().getValue() - 1);
                    deviceState.setIsArmed(false);
                    mqttClientManager.publishMessage("MainGate001/App/isARM", "0", false);
                }else{
                    Toast.makeText(MainActivity.this, "Please Authenticate First", Toast.LENGTH_SHORT).show();
                }
           });
        }

        void UpdateArmStatus(Boolean isArmed){
            int tintColor = getColor(R.color.lightGreen);
            if(isArmed){
                tintColor = getColor(R.color.lightGreen);
                IMArmDisarm.setImageResource(R.drawable.arm);
            }else{
                tintColor = getColor(R.color.lightRed);
                IMArmDisarm.setImageResource(R.drawable.disarm);
            }
            cardARM.setCardBackgroundColor(tintColor);

        }

    };

    private class AuthenticateUI{
        EditText ETNpassword;
        Button BTNauthenticate;
        TextView TV_LockPassword;
        String AuthKey = "12345";


        void init_UIElements(){
            isAuthenticated = false;
            ETNpassword = findViewById(R.id.ETNpassword);
            BTNauthenticate = findViewById(R.id.BTNauthenticate);
            TV_LockPassword = findViewById(R.id.TV_LockPassword);
        }

        void set_Listeners(){
            BTNauthenticate.setOnClickListener(view ->{
                if(isAuthenticated){
                    unlockDoor();
                }else{
                    checkAuth();
                }
            });
        }

        private void checkAuth() {
            String userPassword = ETNpassword.getText().toString().trim();
            if(userPassword.equals(AuthKey)){
                BTNauthenticate.setText("Unlock");
                TV_LockPassword.setVisibility(View.GONE);
                ETNpassword.setVisibility(View.GONE);
                isAuthenticated = true;
            }else{
                Toast.makeText(MainActivity.this,"Wrong Password "+AuthKey,Toast.LENGTH_SHORT).show();
                ETNpassword.setFocusable(true);
            }
        }

        private void unlockDoor() {
            boolean temp_prev_isArmed = deviceState.getIsArmed().getValue();

            // Publish the initial message to disarm
            mqttClientManager.publishMessage("MainGate001/App/isARM", "0",false);
            deviceState.setIsArmed(false);
            if(temp_prev_isArmed){
                handler.postDelayed(()->{
                    // Publish the Final message to armBack
                    mqttClientManager.publishMessage("MainGate001/App/isARM", "1",false);
                    deviceState.setIsArmed(true);
                },10000);
            }
        }
    };
    private static final long TIMEOUT_DURATION = 10000; // 15 seconds timeout
    Handler handler = new Handler();
    private long lastAliveMessageReceivedTime = 0;
    HiveMQTTCloudClientManager mqttClientManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceState = new DeviceState();


        mqttClientManager = new HiveMQTTCloudClientManager();
        deviceState.setIsMobileConnected(mqttClientManager.setupClient());
        mqttClientManager.subscribeToTopic("MainGate001/Device");
        mqttClientManager.subscribeToTopic("MainGate001/Device/State");
        mqttClientManager.subscribeToTopic("MainGate001/Device/Intruder");
        mqttClientManager.setMessageHandler(getMessageHandler());

        //Enable Background Service
        Intent serviceIntent = new Intent(this, MQTTBackgroundService.class);
        startService(serviceIntent);

        InitUIComponents();
        SetupListeners();
    }

    @NonNull
    private MessageHandler getMessageHandler() {
        return new MessageHandler() {
            @Override
            public void handleMessage(String message, String topic) {
                System.out.println("TOPIC IN : " + topic + "\nMESSAGE IN : " + message);
                Handler mainLoophandler = new Handler(Looper.getMainLooper());//To Update UI elements in MainActivity

                switch (topic) {
                    case "MainGate001/Device":
                        DeviceMessageHandler(message, mainLoophandler);
                        break;
                    case "MainGate001/Device/State":
                        DeviceStateMessageHandler(message, mainLoophandler);
                        break;
                    case "MainGate001/Device/Intruder":
                        DeviceIntruderMessageHandler(message, mainLoophandler);
                        break;
                }
            }
            private void DeviceIntruderMessageHandler(String message,Handler mainLoophandler) {
                mainLoophandler.post(() -> {
                        if(message.equals("Threat Detected")){
                            Toast.makeText(MainActivity.this, "IntruderDetected", Toast.LENGTH_SHORT).show();
                            deviceState.setIsIntruderDetected(true);
                        }else{
                            deviceState.setIsIntruderDetected(false);
                        }
                });
            }

            private void DeviceStateMessageHandler(String message ,Handler mainLoophandler ) {
                String[] elements = message.split(",");
                mainLoophandler.post(()->{
                if(elements.length >= 4){
                    deviceState.setTemperature(Integer.parseInt(elements[0]));
                    deviceState.setHumidity(Integer.parseInt(elements[1]));
                    deviceState.setIsArmed(Integer.parseInt(elements[2]) != 0);
                    deviceState.setIsIntruderDetected(Integer.parseInt(elements[3]) != 0);


                }else if (message.equals("Alive")){
                    deviceState.setIsDeviceConnected(true);
                    lastAliveMessageReceivedTime = System.currentTimeMillis();
                    mqttClientManager.publishMessage("MainGate001/App","Fetch Request",false);
                }
                });
                handler.postDelayed(() -> {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastAliveMessageReceivedTime > TIMEOUT_DURATION) {
                        // Timeout occurred, update device connection status
                        deviceState.setIsDeviceConnected(false);
                        System.out.println("Time limit exceeded!");
                        // Perform any other actions as needed
                    }
                    System.out.println("excuse me "+((currentTime - lastAliveMessageReceivedTime)/1000));
                }, TIMEOUT_DURATION+5000);

            }

            private void DeviceMessageHandler(String message , Handler mainLoophandler) {
                mainLoophandler.post(()->{
                    if(message.equals("Alive")){
                        deviceState.setIsDeviceConnected(true);
                        lastAliveMessageReceivedTime = System.currentTimeMillis();
                        mqttClientManager.publishMessage("MainGate001/App","Fetch Request",false);
                    }
                });

                // Schedule a new task to check for timeout
                handler.postDelayed(() -> {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastAliveMessageReceivedTime > TIMEOUT_DURATION) {
                        // Timeout occurred, update device connection status
                        deviceState.setIsDeviceConnected(false);
                        System.out.println("Time limit exceeded!");
                        // Perform any other actions as needed
                    }
                    System.out.println("excuse me "+((currentTime - lastAliveMessageReceivedTime)/1000));

                }, TIMEOUT_DURATION+5000);
            }
        };
    }

    void InitUIComponents(){
        connectionStatusUI = new ConnectionStatusUI();
        temperatureHumidityUI = new TemperatureHumidityUI();
        intruderUI = new IntruderUI();
        armDisarmUI = new ArmDisarmUI();
        authenticateUI = new AuthenticateUI();


        connectionStatusUI.init_UIElements();
        temperatureHumidityUI.init_UIElements();
        intruderUI.init_UIElements();
        armDisarmUI.init_UIElements();
        authenticateUI.init_UIElements();
    }

    void SetupListeners(){
        connectionStatusUI.set_Listeners();
        intruderUI.set_Listeners();
        armDisarmUI.set_Listeners();
        authenticateUI.set_Listeners();

        deviceState.getTemperature().observe(this, temperature -> {
            // Update UI with new temperature value
            temperatureHumidityUI.setIMTemperature(temperature);
        });

        deviceState.getHumidity().observe(this, humidity -> {
            // Update UI with new humidity value
            temperatureHumidityUI.setIMHumidity(humidity);
        });

        deviceState.getIsDeviceConnected().observe(this,isDeviceConnected ->{
            // Update UI with new isDeviceConnected value
            connectionStatusUI.update_DeviceConnection(isDeviceConnected);
            Toast.makeText(MainActivity.this, "Device is "+(isDeviceConnected?"Online":"Offline"), Toast.LENGTH_SHORT).show();
        });

        deviceState.getIsMobileConnected().observe(this,isMobileConnected ->{
            connectionStatusUI.update_MobileConnection(isMobileConnected);
            Toast.makeText(MainActivity.this, "Mobile "+(isMobileConnected?"Online":"Offline"), Toast.LENGTH_SHORT).show();
        });


        deviceState.getIsIntruderDetected().observe(this,isIntruderDetected ->{
            intruderUI.update_InteruderUI(isIntruderDetected);
            Toast.makeText(MainActivity.this, (isIntruderDetected?"Intruder Detected":"Safe Home"), Toast.LENGTH_SHORT).show();
        });

        deviceState.getIsArmed().observe(this,isArmed ->{
            armDisarmUI.UpdateArmStatus(isArmed);
            Toast.makeText(this, (isArmed?"Device Armed":"Device DisArmed"), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttClientManager.disconnect();
    }


}


