package cz.fim.uhk.thesis.libraryforofflinemode;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import cz.fim.uhk.thesis.libraryforofflinemode.api.CentralServerApi;
import cz.fim.uhk.thesis.libraryforofflinemode.helper.DatabaseHelper;
import cz.fim.uhk.thesis.libraryforofflinemode.model.User;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Knihovna realizující roli klienta v offline režimu
 * Především persistentní ukládání seznamu klientů do DB a manipulace s daty v DB
 * hlavní (řídící) třída knihovny
 */
public class MainClass implements LibraryLoaderInterface {

    private static final String TAG = "LibraryOfflineMain";
    private static final String PATH_TO_DESC = "/LibraryForOfflineMode/descriptor.txt";
    private DatabaseHelper myDb;
    private List<User> users;
    private String libraryPathInApp;
    private Context appContext;
    private CentralServerApi centralServerApi;

    @Override
    public int start(String path, Context context) {
        Log.d(TAG, "Knihovna startuje");
        try {
            this.libraryPathInApp = path;
            this.appContext = context;
            // init komunikace se serverem pro synchronizaci
            // nastavení gson converteru (JSON -> Java) pro Date formát
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm")
                    .create();
            // init a nastavení retrofit objektu pro připojení k serveru
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:8080/") // localhost alias pro AVD
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            // naplnění těl metod prostřednictvím retrofit objektu
            centralServerApi = retrofit.create(CentralServerApi.class);

            // init db a seznam klientů
            myDb = new DatabaseHelper(context);
            users = new ArrayList<>();

            Log.d(TAG, "Knihovna spuštěna");
            return 0;
        } catch (Exception ex) {
            Log.e(TAG, "knihovnu se nepodařilo spustit: " + ex.getMessage());
            return 1;
        }
    }

    @Override
    public int stop() {
        // uvolnění prostředků knihovny
        // vzhledem k činnosti knihovny v rámci funkcí IS zde není nutné nic implementovat
        // smazání DB jakožto jedniného prostředku, který knihovna využívá by nedávalo smysl
        try {
            // pro ukázku
            users = null;
            return 0;
        } catch (Exception ex) {
            Log.e(TAG, "knihovnu se nepodařilo zastavit: " + ex.getMessage());
            return 1;
        }
    }

    @Override
    public int resume(String path, Context context) {
        // opětovné spuštění činnosti knihovny
        // implementace jako v metodě start() při prvotním zavedení
        Log.d(TAG, "Knihovna znovu startuje");
        try {
            this.libraryPathInApp = path;
            this.appContext = context;
            // init komunikace se serverem pro synchronizaci
            // nastavení gson converteru (JSON -> Java) pro Date formát
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm")
                    .create();
            // init a nastavení retrofit objektu pro připojení k serveru
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:8080/") // localhost alias pro AVD
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            // naplnění těl metod prostřednictvím retrofit objektu
            centralServerApi = retrofit.create(CentralServerApi.class);

            // init db a seznam klientů
            myDb = new DatabaseHelper(context);
            users = new ArrayList<>();

            Log.d(TAG, "Knihovna znovu spuštěna");
            return 0;
        } catch (Exception ex) {
            Log.e(TAG, "knihovnu se nepodařilo znovu spustit: " + ex.getMessage());
            return 1;
        }
    }

    @Override
    public int exit() {
        try {
            // uvolnění prostředků knihovny - smazání db
            appContext.deleteDatabase(myDb.getDatabaseName());
            // synchronizace se serverem - poslání dat na server
            // request na server
            Call<ResponseBody> call = centralServerApi.sendUsers(users);
            // zpracování response ze serveru
            // metoda enqueue zajistí, aby zpracovaní proběhlo na nově vytvořeném background vlákně
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // kontrola zda response je neúspěšný
                if (!response.isSuccessful()) {
                    // zobrazíme chybový HTTP kód a message a návrat z metody
                    Log.e(TAG, "Synchronizace se serverem se nepodařila: ");
                    Log.e(TAG, "HTTP kód: " + response.code() + "Message: " + response.message());
                    return;
                }
                // vše proběhlo jak mělo
                Log.d(TAG, "Synchronizace se serverem proběhla úspěšně");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Synchronizace se serverem se nepodařila. Chybová hláška: " + t.getMessage());
            }
        });
        } catch (Exception ex) {
            Log.e(TAG, "Metoda exit() selhala: " + ex.getMessage());
            return 1;
        }
        return 0;
    }

    @Override
    public String getDescription() {
        // pro získání popisu knihovny z jejího deskriptoru umístěném v uložišti klienta
        List<String> data = new ArrayList<>();
        // získání dat z txt
        String pathToFile = libraryPathInApp + PATH_TO_DESC;
        Log.d(TAG, "Cesta k descriptor.txt: " + pathToFile);
        Scanner myReader = null;
        try {
            myReader = new Scanner(new File(pathToFile));
        } catch (FileNotFoundException e) {
            Log.d(TAG, "Nepodařilo se získat popis z descriptor.txt, chyba: " + e.getMessage());
        }
        String line;
        while (myReader.hasNextLine()) {
            line = myReader.nextLine();
            String[] split = line.split(":");
            data.add(split[1]);
        }
        return data.get(4);
    }

    // metoda pro předání dat z db knihovny do aplikace
    public List<User> getUsers() {
        // získání dat z databáze
        Cursor result = myDb.getAllData();
        // naplnění seznamu klientů pro předání do aplikace
        if (result.getCount() != 0) {
            Log.d(TAG, "SSID klientů v knihovně: ");
            while (result.moveToNext()) {
                try {
                    String id = result.getString(0);
                    double lat = result.getDouble(1);
                    double lng = result.getDouble(2);
                    int isOnlineInt = result.getInt(3);
                    boolean isOnline = isOnlineInt == 1;
                    String actState = result.getString(4);
                    String futState = result.getString(5);
                    String firstConnStr = result.getString(6);
                    Date firstConn = null;
                    if(firstConnStr != null) {
                        firstConn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm",
                                Locale.getDefault()).parse(firstConnStr);
                    }
                    String lastConnStr = result.getString(7);
                    Date lastConn = null;
                    if(lastConnStr != null) {
                        lastConn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm",
                                Locale.getDefault()).parse(lastConnStr);
                    }
                    double temp = result.getDouble(8);
                    double pres = result.getDouble(9);
                    User user = new User(id, lat, lng, isOnline, actState, futState, firstConn, lastConn,
                            temp, pres);
                    users.add(user);
                } catch (Exception e) {
                    Log.e(TAG, "Nepodařilo se uložit klienta do seznamu klientů: ");
                    e.printStackTrace();
                }
            }
            // výpis klientů pro kontrolu
            for(User user : users) {
                Log.d(TAG, "USER ID: " + user.getSsid());
            }
        } else {
            Log.d(TAG, "V DB offline knihovny není žádný klient");
        }
        return users;
    }

    // metoda pro získání dat z aplikace (seznam klientů ze serveru)
    public int addUser(String id, Double latitude, Double longitude, Boolean isOnline, String actualState,
                        String futureState, Date firstConnectionToServer, Date lastConnectionToServer,
                        Double temperature, Double pressure) {
        User user = new User(id, latitude, longitude, isOnline, actualState, futureState, firstConnectionToServer,
                lastConnectionToServer, temperature, pressure);
        // kontrola zda klient již není v DB dle jeho SSID
       if(userExists(user.getSsid())) {
           // již je v DB -> update záznamu - mohlo dojít k aktualizaci stavu klienta
           if (myDb.updateUser(user)) {
               Log.d(TAG, "update data!");
               return 0;
           }
           else return 1;
       } else {
           // uložení klienta ze serveru do DB
           if (myDb.insertData(user)) {
               Log.d(TAG, "insert data!");
               return 0;
           }
           else return 1;
       }
    }

    // metoda pro kontrolu zda je klient už v uložišti preš seznam klientů v db
    private boolean userExists(String id) {
        // získání dat z databáze
        Cursor result = myDb.getAllData();
        if (result.getCount() == 0) {
            return false;
        }
        // získání ssid z DB
        List<String> userIdes = new ArrayList<>();
        while (result.moveToNext()) {
            userIdes.add(result.getString(0));
        }
        // kontrola zda daný klient je již v seznamu, tudíž v uložišti
        for (String userId : userIdes) {
            if (userId.equals(id)) {
                return true;
            }
        }
        return false;
    }
}
