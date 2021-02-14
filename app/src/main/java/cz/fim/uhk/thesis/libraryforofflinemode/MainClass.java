package cz.fim.uhk.thesis.libraryforofflinemode;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

public class MainClass implements LibraryLoaderInterface, Serializable {

    private static final String TAG = "LibraryOfflineMain";
    private static final String PATH_TO_DESC = "/LibraryForOfflineMode/descriptor.txt";
    private DatabaseHelper myDb;
    private List<User> users;
    private String libraryPathInApp;
    private Context appContext;
    private CentralServerApi centralServerApi;
    private int exitResult;

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
        // smazání obsahu db
        try {
            myDb.dropTable();
            return 0;
        } catch (Exception ex) {
            Log.e(TAG, "knihovnu se nepodařilo zastavit: " + ex.getMessage());
            return 1;
        }
    }

    @Override
    public int resume(List<?> clients) { // nevim esi je nutny ten parametr - muzu si ukladat do data do tridy tady klidne ze startu
        // opětovné spuštění činnosti knihovny
        // synchronizace db
        try {
            for (User user : users) {
                myDb.insertData(user);
            }
            Log.d(TAG, "knihovna opětovně spuštěna");
            return 0;
        } catch (Exception ex) {
            Log.e(TAG, "knihovnu se nepodařilo opětovně spustit: " + ex.getMessage());
            return 1;
        }
    }

    @Override
    public int exit() {
        exitResult = 1;
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
                    Log.e(TAG, "HTTP kód: " + response.code() + "Message: " + response.message());
                    return;
                }
                // vše proběhlo jak mělo
                Log.d(TAG, "Synchronizace proběhla úspěšně");
                exitResult = 0;
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Synchronizace se nepodařila. Chybová hláška: " + t.getMessage());
            }
        });
        return exitResult;
    }

    @Override
    public String getDescription() {
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

    // metoda pro aktualizaci db knihovny
    public void actualizeDatabase(List<?> clients) {
        List<User> usersToActualize = (List<User>) clients;
        for (User user : usersToActualize) {
            // kontrola zda klient existuje v db
            if (myDb.getUserById(user.getSsid()) != null) {
                // ano -> update v db
                myDb.updateUser(user);
            } else {
                // ne -> vložení nového do db
                myDb.insertData(user);
            }
        }
    }

    // pro předání dat z db knihovny do aplikace
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
                    Date firstConn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(firstConnStr);
                    String lastConnStr = result.getString(7);
                    Date lastConn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(lastConnStr);
                    double temp = result.getDouble(8);
                    double pres = result.getDouble(9);
                    User user = new User(id, lat, lng, isOnline, actState, futState, firstConn, lastConn,
                            temp, pres);
                    users.add(user);
                } catch (Exception e) {
                    Log.e(TAG, "Nepodařilo se uložit klienta do seznamu klientů: ");
                    e.printStackTrace();
                }
                for(User user : users) {
                    Log.d(TAG, "USER ID: " + user.getSsid());
                }
            }
        } else {
            Log.d(TAG, "V DB offline knihovny není žádný klient");
        }
        return users;
    }

    // pro získání dat z aplikace (seznam klientů ze serveru)
    public int addUser(String id, Double latitude, Double longitude, Boolean isOnline, String actualState,
                        String futureState, Date firstConnectionToServer, Date lastConnectionToServer,
                        Double temperature, Double pressure) {
        User user = new User(id, latitude, longitude, isOnline, actualState, futureState, firstConnectionToServer,
                lastConnectionToServer, temperature, pressure);
        // uložení klienta ze serveru do DB
        if (myDb.insertData(user)) return 0;
        else return -1;

    }
}
