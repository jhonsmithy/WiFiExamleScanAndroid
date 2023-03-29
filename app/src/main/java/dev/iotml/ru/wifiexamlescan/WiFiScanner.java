package dev.iotml.ru.wifiexamlescan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
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
import android.os.Build;
import android.os.PatternMatcher;
import android.provider.Settings;
import android.util.Log;
import android.Manifest;
import android.view.Gravity;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import java.util.List;
import java.util.Objects;

public class WiFiScanner extends Activity {

    private Context context;//доступ к контексту
    Activity activity;      //доступ к активити
    private static final int MY_LOCATION_PERMISSION_REQUEST = 45;//число рандом
    private static final String TAG = "WiFI";
    private WifiManager wifimanager;            //менеджер вифи сетей
    private WifiConfiguration wifiConfig;       //настройки вифи
    public boolean status_wifi_connect=false;  //статус подключения к вифи
    public String ssid_wifi_connect="IoTmanager";//имя сети вифи для подключения
    private ConnectivityManager connectivityManager=null;
    private ConnectivityManager.NetworkCallback networkCallback = null;
    private LocationManager locationManager;
    public static boolean geolocationEnabled = false;//статус включения сервисов месторасположения
    public static boolean wifiStateEnabled=false; //статус включения сервисов вифи
    public List<ScanResult> wifiListResult; //список сканированных сетей

    // создаем колбек и его метод
    interface NetworkFindCallback{
        void enableConnectWifi();
    }

    NetworkFindCallback callback;

    public void registerCallBack(NetworkFindCallback callback){
        this.callback = callback;
    }

    //конструктор класса для получения контекста
    public WiFiScanner(Context context) {
        this.context = context;//получим контекст
        this.activity = (Activity) context;//получим активность
    }

    //сервис слушателя системных сообщений,что список вифи сетей обновлен
    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver()
    {//запуск слушателя сканера сетей
        @Override
        public void onReceive(Context c, Intent intent) {
            boolean success = false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
            }
            scanSuccess(success);//вызов обработки статуса запроса
        }
    };

    //проверить и запросить у пользователя разрешения
    private  boolean checkPermission(String permission_string){
        boolean result=true;
        //проверяем необходимо разрешение
        if (ActivityCompat.checkSelfPermission(context, permission_string) != PackageManager.PERMISSION_GRANTED) {
            //если отрицательно ир  запросим это разрешение
            ActivityCompat.requestPermissions(activity,
                        new String[]{permission_string},
                        MY_LOCATION_PERMISSION_REQUEST);
            //отловить результат выполенияя запроса разрешения или проверить еще раз
            if (ActivityCompat.checkSelfPermission(context, permission_string) != PackageManager.PERMISSION_GRANTED) {
                result=false;
            }
        }
        Log.i(TAG, "Request permission: "+permission_string + " is granted: "+result);
        return result;
    }

    //проверки или запрос разрешения на изменение системных настроек
    private boolean checkSystemWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(Settings.System.canWrite(context))
                return true;
            else
                openPermissionsMenu(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                return Settings.System.canWrite(context);
        }
        return false;
    }

    //открыть страницу разрешения изменения ннастроек
    private void openPermissionsMenu(String settings_action) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(settings_action);
            if (Objects.equals(settings_action, Settings.ACTION_MANAGE_WRITE_SETTINGS))
                intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    //Проверяет включены ли соответствующие провайдеры локации
    private boolean checkLocationServiceEnabled() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        geolocationEnabled=false;
        try {
            geolocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Log.i(TAG, "Check location service error: "+e.toString());
        }
        if (!geolocationEnabled){
            explainMessage(context.getResources().getString(R.string.msg_switch_gps),
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        }
        return geolocationEnabled;
    }

    //Проверяет включены ли вифи сервисы
    private boolean checkWiFiServiceEnabled(){
        wifiStateEnabled=false;
        wifimanager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {//проверяем включен ли сервис вифи
            wifiStateEnabled = wifimanager.isWifiEnabled();
        } catch (Exception e){
            Log.i(TAG, "Check WiFi service error: "+e.toString());
        }

        if (!wifiStateEnabled)    //если вифи выключен
            if (Build.VERSION.SDK_INT<Build.VERSION_CODES.Q)
                wifimanager.setWifiEnabled(true);//для старых версий включить
            else
            {//для новых версий запросить вклюяение
                //заапустить слушатель на вкл вифи
                IntentFilter intentFilter = new IntentFilter();//создадим фильтр
                intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
                context.registerReceiver(wifiStateReceiver, intentFilter);//включим слушатель на этот фильтр
                //высветим сообщение о необходимости включения
                explainMessage(
                        context.getResources().getString(R.string.msg_switch_wifi),
                        android.provider.Settings.Panel.ACTION_WIFI);
            }
        return wifiStateEnabled;
    }

    //сервис слушателя системных сообщений,что  вифи включен
    BroadcastReceiver wifiStateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context c, Intent intent) {
            //final String action = intent.getAction();
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                showToast("WIFI on");
                wifiStateEnabled=true;
                context.unregisterReceiver(wifiStateReceiver);
            }else if(wifiState == WifiManager.WIFI_STATE_DISABLED) {
                showToast("WIFI OFF");
                wifiStateEnabled=false;
            }
        }
    };

    //Показываем диалог и переводим пользователя к настройкам
    private void explainMessage(String msg_show_text, String action_settings) {
        //String msg = !network_enabled ? msg_show_text : null;
        if (msg_show_text != null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(false)
                    .setMessage(msg_show_text)
                    .setPositiveButton("Включить", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            //context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            openPermissionsMenu(action_settings);
                        }
                    })
                    .setNegativeButton("Отмена",new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {

                                    }
            });
            final AlertDialog alert = builder.create();
            alert.show();
        }
    }

    //создадим сканер сетей
    public void RunScanWifi(String find_ssid)
    {
        ssid_wifi_connect=find_ssid;
        if ((!checkWiFiServiceEnabled())||(!checkLocationServiceEnabled())) {
            Log.i(TAG, "Check WiFi toogle or GPS toogle: false");
            return;
        }

        if (!checkPermission(Manifest.permission.CHANGE_WIFI_STATE)){//проверим разрешения
            showToast("Нет доступа к WiFi");
            return;}

        if (this.context!=null) {//еслти контекст не нулевой
            IntentFilter intentFilter = new IntentFilter();//создадим фильтр
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            context.registerReceiver(wifiScanReceiver, intentFilter);//включим слушатель на этот фильтр
            // создаем новый объект для подключения к конкретной точке
            wifiConfig = new WifiConfiguration();
            //запуск сканирования
            Log.i(TAG, "Scanner WiFi is created: "+wifimanager.startScan());
        }
        else Log.i(TAG, "Context application is null");
    }

    //обработка результатов сканирования
    private void scanSuccess(boolean success) {//обработка результатов сканирования
        if (!success) {
            Log.i(TAG, "Network list is old!");
        }
        else
        {//сканирование вифи успешно
            Log.i(TAG, "Network list is new!");
        }

        if (!checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)){//проверим разрешения
            showToast("Отсутствует разрешение местоположения");
            return;}

        if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)){//проверим разрешения
            showToast("Отсутствует разрешение местоположения");
            return;}

        wifiListResult = wifimanager.getScanResults();//получим результаты сканирования сетей
        Log.i(TAG, "Find wireless networks: " + String.valueOf(wifiListResult.size()));
        for (int i =0;i<wifiListResult.size();i++)
        {
            Log.i(TAG, "Wireless "+String.valueOf(i)+": "+String.valueOf(wifiListResult.get(i).SSID));
            if (wifiListResult.get(i).SSID.contains(ssid_wifi_connect) ){
                //если нашли эту сеть то подключаемся к ней
                //if (!status_wifi_connect) connectToWifi(wifiListResult.get(i).SSID);
                // вызываем метод обратного вызова
                callback.enableConnectWifi();
            }
        }
    }

    //создаём и отображаем текстовое уведомление
    private void showToast(String str_text) {
        Toast toast = Toast.makeText(context,
                str_text,
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }

    //процедура подключения к определенной сети
    public void connectToWifi(String ssid){
        Log.i(TAG, "Wireles connect to  "+ssid);
        try {
            //остановим сканирование сетей
            context.unregisterReceiver(wifiScanReceiver);
        }
        catch (final Exception exception) {
            Log.i(TAG,"Receiver is empty? error "+exception.toString());
        }

        if (!checkPermission(Manifest.permission.CHANGE_NETWORK_STATE)){//проверим разрешения
            showToast("Отсутствует доступ к Wi-Fi");
            return;}

        try {//проверим разрешение изменениея настроек
            if (checkSystemWritePermission()) {
                Log.i(TAG, "Permissions SYSTEM_WRITE is True");
            }else {
                Log.i(TAG, "Permissions SYSTEM_WRITE is false");
                showToast("Отсутствует доступ к системным настройкам");
                return;
            }
        } catch (Exception e) {
            Log.i(TAG,e.toString());
        }
        //создаем подключение
        NetworkSpecifier specifier = null;
        NetworkRequest request = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            specifier = new WifiNetworkSpecifier.Builder()
                    //.setSsid(ssid)
                    .setSsidPattern(new PatternMatcher(ssid, PatternMatcher.PATTERN_PREFIX))
                    //.setBssidPattern(MacAddress.fromString("10:03:23:00:00:00"), MacAddress.fromString("ff:ff:ff:00:00:00"))
                    .build();
            request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();

        }

        connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.i(TAG, "Connect to Network: "+ssid +" is true");
                connectivityManager.bindProcessToNetwork(network);
                //Если успешно подключились, то сменим статус
                status_wifi_connect=true;
                /*
                try {//остановим сканирование сетей
                    context.unregisterReceiver(wifiStateReceiver);
                } catch (final Exception exception) {
                    Log.i(TAG,"wifiStateReceiver is empty? error "+exception.toString());}
                */
            }

            @Override
            public void onUnavailable() {
                //нет успешного подключения
                Log.i(TAG, "Connect to Network: "+ssid +" is false");
                status_wifi_connect=false;
            }

        };
        connectivityManager.requestNetwork(request, networkCallback);
    }

    //разорвать соединение
    public void unConnectWiFi()
    {
        connectivityManager.unregisterNetworkCallback(networkCallback);
        Log.i(TAG, "Connection to WiFi is break");
    }
/*
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
*/
    public void onCreate() {

    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.i(TAG, "Resume");
        //context.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "Start");
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "Stop");
        unregisterReceiver(wifiScanReceiver);
        unregisterReceiver(wifiStateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroy");
        unregisterReceiver(wifiScanReceiver);
        unregisterReceiver(wifiStateReceiver);
    }
}
