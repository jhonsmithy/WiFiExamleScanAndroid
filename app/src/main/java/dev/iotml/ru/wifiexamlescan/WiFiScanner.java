package dev.iotml.ru.wifiexamlescan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.PatternMatcher;
import android.util.Log;
import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.List;

//@RequiresApi(api = Build.VERSION_CODES.Q)
public class WiFiScanner {
    private Context context;//доступ к контексту
    Activity activity;//ждоступ к активити
    private static final int MY_LOCATION_PERMISSION_REQUEST = 45;
    private static final String TAG = "WiFI";
    //конструктор класса для получения контекста
    public WiFiScanner(Context context) {//конструктор класса для передачи контекста
        this.context = context;
    }
    private WifiManager wifimanager;//менеджер вифи сетей
    private WifiConfiguration wifiConfig;
/*
    final NetworkSpecifier specifier =
            new WifiNetworkSpecifier.Builder()
                    .setSsidPattern(new PatternMatcher("IoTmanager", PatternMatcher.PATTERN_PREFIX))
                    //.setBssidPattern(MacAddress.fromString("10:03:23:00:00:00"), MacAddress.fromString("ff:ff:ff:00:00:00"))
                    .build();

    final NetworkRequest request =
            new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();

    final ConnectivityManager connectivityManager = (ConnectivityManager)
            context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);


    final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

        //@Override
        public void onAvailable() {
            // do success processing here..
        }

        @Override
        public void onUnavailable() {
            // do failure processing here..
        }

    };
*/
    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override//запуск слушателя сканера сетей
        public void onReceive(Context c, Intent intent) {
            boolean success = false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
            }
            scanSuccess(success);
        }
    };

    private  boolean checkPermission(String permission_string){
        boolean result=true;
        //проверяем разрешение
        if (ActivityCompat.checkSelfPermission(context, permission_string) != PackageManager.PERMISSION_GRANTED) {
            // запросим разрешение к вифи сетям
            ActivityCompat.requestPermissions(activity,
                        new String[]{permission_string},
                        MY_LOCATION_PERMISSION_REQUEST);
            //отловить результат выполенияя запроса разрешения
            //или проверить еще раз
            if (ActivityCompat.checkSelfPermission(context, permission_string) != PackageManager.PERMISSION_GRANTED) {
                result=false;
            }
        }
        Log.i(TAG, "Request permission: "+permission_string + "is granted: "+result);
        return result;
    }

    public void create_scan()   //создадим сканер сетей
    {
        if (!checkPermission(Manifest.permission.CHANGE_WIFI_STATE)){//проверим разрешения
            Log.i(TAG, "No permissions CHANGE_WIFI_STATE");
            return;}
        if (this.context!=null) {//еслти контекст не нулевой
            activity = (Activity) context;//получим активность
            wifimanager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            context.registerReceiver(wifiScanReceiver, intentFilter);
            // создаем новый объект для подключения к конкретной точке
            wifiConfig = new WifiConfiguration();
            //запуск сканирования
            boolean success = wifimanager.startScan();
            scanSuccess(success);
        }
        else Log.i(TAG, "Context application is null");
    }

    private void scanSuccess(boolean success) {
        if (!success) {
            Log.i(TAG, "Network list is old!");
        }
        else
        {//сканирование вифи успешно
            Log.i(TAG, "Network list is new!");
        }
        if (!checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)){//проверим разрешения
            Log.i(TAG, "No permissions ACCESS_COARSE_LOCATION");
            return;}
        if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)){//проверим разрешения
            Log.i(TAG, "No permissions ACCESS_FINE_LOCATION");
            return;}
        List<ScanResult> results = wifimanager.getScanResults();//получим результаты сканирования сетей
        Log.i(TAG, "Find wireless networks: " + String.valueOf(results.size()));
        for (int i =0;i<results.size();i++)
        {
            Log.i(TAG, "Wireles "+String.valueOf(i)+": "+String.valueOf(results.get(i).SSID));
            if (results.get(i).SSID.contains("IoTmanager") ){
                //connectivityManager.requestNetwork(request, networkCallback);
                //connctToWifi(results.get(i).SSID);
            }
        }
    }

    public void connctToWifi(String ssid){
        Log.i(TAG, "Wireles connect to  "+ssid);
        wifiConfig.SSID = ssid;
        wifiConfig.priority = 1;
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wifiConfig.status = WifiConfiguration.Status.ENABLED;

        //получаем ID сети и пытаемся к ней подключиться,
        int netId = wifimanager.addNetwork(wifiConfig);
        wifimanager.saveConfiguration();
        //если вайфай выключен то включаем его
        wifimanager.enableNetwork(netId, true);
        //если же он включен но подключен к другой сети то перегружаем вайфай.
        wifimanager.reconnect();

    }
}
