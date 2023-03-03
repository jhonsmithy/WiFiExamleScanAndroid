package dev.iotml.ru.wifiexamlescan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.PatternMatcher;
import android.provider.Settings;
import android.util.Log;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

//@RequiresApi(api = Build.VERSION_CODES.Q)
public class WiFiScanner extends Activity {
    private Context context;//доступ к контексту
    Activity activity;      //доступ к активити
    private static final int MY_LOCATION_PERMISSION_REQUEST = 45;
    private static final String TAG = "WiFI";

    //конструктор класса для получения контекста
    public WiFiScanner(Context context) {//конструктор класса для передачи контекста
        this.context = context;
    }

    private WifiManager wifimanager;//менеджер вифи сетей
    private WifiConfiguration wifiConfig;//настройки вифи
    private boolean status_wifi_connect=false;

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
        Log.i(TAG, "Request permission: "+permission_string + " is granted: "+result);
        return result;
    }

    private boolean checkSystemWritePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(Settings.System.canWrite(context))
                return true;
            else
                openAndroidPermissionsMenu(context);
                return Settings.System.canWrite(context);
        }
        return false;
    }

    private void openAndroidPermissionsMenu(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
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
            //scanSuccess(success);
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

            //if (results.get(i).SSID.contains("RSHB_WI-FI") ){
                //connectivityManager.requestNetwork(request, networkCallback);
                if (!status_wifi_connect) connectToWifi(results.get(i).SSID);
            }
        }
    }

    public void connectToWifi(String ssid){
        Log.i(TAG, "Wireles connect to  "+ssid);
        try {
            context.unregisterReceiver(wifiScanReceiver);
        }
        catch (final Exception exception) {
            // The receiver was not registered.
            // There is nothing to do in that case.
            // Everything is fine.
            Log.i(TAG,exception.toString());
        }

        if (!checkPermission(Manifest.permission.CHANGE_NETWORK_STATE)){//проверим разрешения
            Log.i(TAG, "No permissions CHANGE_NETWORK_STATE");
            return;}

        try {
            if (checkSystemWritePermission(context)) {
                Log.i(TAG, "Permissions SYSTEM_WRITE is True");
            }else {
                Log.i(TAG, "Permissions SYSTEM_WRITE is false");
            }
        } catch (Exception e) {
            Log.i(TAG,e.toString());
        }

        NetworkSpecifier specifier = null;
        NetworkRequest request = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    //.setSsidPattern(new PatternMatcher(ssid, PatternMatcher.PATTERN_PREFIX))
                    //.setBssidPattern(MacAddress.fromString("10:03:23:00:00:00"), MacAddress.fromString("ff:ff:ff:00:00:00"))
                    .build();
            request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();

        }

        final ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "onAvailable: "+network);
                connectivityManager.bindProcessToNetwork(network);
            }
        };
        connectivityManager.requestNetwork(request, networkCallback);

        /*
        final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {


            public void onAvailable() {
                // do success processing here..
                Log.i(TAG, "Connect to Network: "+ssid +" is true");
                status_wifi_connect=true;
            }

            @Override
            public void onUnavailable() {
                // do failure processing here..
                Log.i(TAG, "Connect to Network: "+ssid +" is false");
                status_wifi_connect=false;
            }

        };
        connectivityManager.requestNetwork(request, networkCallback);

// Release the request when done.
        connectivityManager.unregisterNetworkCallback(networkCallback);
        */
    }

    public void connectToWifi2(String ssid) {
        Log.i(TAG, "Wireles connect to  " + ssid);
        if (!checkPermission(Manifest.permission.CHANGE_NETWORK_STATE)) {//проверим разрешения
            Log.i(TAG, "No permissions CHANGE_NETWORK_STATE");
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {


            final WifiNetworkSuggestion suggestion1 =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid)
                            .setIsAppInteractionRequired(true) // Optional (Needs location permission)
                            .build();



            final List<WifiNetworkSuggestion> suggestionsList =
                    new ArrayList<WifiNetworkSuggestion>();
            suggestionsList.add(suggestion1);


            final WifiManager wifiManager =wifimanager;
                    //(WifiManager) context.getSystemService(Context.WIFI_SERVICE);

            final int status = wifiManager.addNetworkSuggestions(suggestionsList);
            if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
// do error handling here…
            }

// Optional (Wait for post connection broadcast to one of your suggestions)
            final IntentFilter intentFilter =
                    new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

            final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!intent.getAction().equals(
                            WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                        return;
                    }
                    // do post connect processing here...
                }
            };
            context.registerReceiver(broadcastReceiver, intentFilter);
        }
    }


    protected void onCreate(){


    }

    @Override
    protected void onResume(){
        super.onResume();
        //context.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(wifiScanReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiScanReceiver);
    }
}
