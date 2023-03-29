package dev.iotml.ru.wifiexamlescan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    //WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button but = (Button) findViewById(R.id.button); //запуск вифи модуля
        Button but2 = (Button) findViewById(R.id.button2); //разрешения. удалить
        WiFiScanner wifi=new WiFiScanner(MainActivity.this);
        CallbackFromWifiScanner callbackwifi = new CallbackFromWifiScanner();

        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("wifi", "wifi start");
                //если нажали на кнопку то создадим сканер вифи

                //инициализируем колбек, передавая методу registerCallBack экземпляр, реализующий интерфейс колбек
                wifi.registerCallBack(callbackwifi);

                wifi.RunScanWifi("IoTmanager");

                but2.setEnabled(false);
            }
        });

        but2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("wifi", "Connect to wifi");
                //если нажали на кнопку то создадим сканер вифи
                wifi.connectToWifi("IoTmanager");

            }
        });

    }

    class CallbackFromWifiScanner implements WiFiScanner.NetworkFindCallback {

        @Override
        public void enableConnectWifi() {
            Button but2 = (Button) findViewById(R.id.button2); //разрешения. удалить
            Log.i("wifi", "WiFi Network is founded");
            but2.setEnabled(true);
        }
    }

}

