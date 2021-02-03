package cz.fim.uhk.thesis.libraryforofflinemode;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
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

public class MainClass implements LibraryLoaderInterface {

    private static final String TAG = "LibraryOfflineMain";
    private static final String PATH_TO_DESC = "/LibraryForOfflineMode/descriptor.txt";
    private DatabaseHelper myDb;
    private List<User> users;
    private String libraryPathInApp;
    private Context appContext;
    private CentralServerApi centralServerApi;
    private int exitResult;

    @Override
    public int start(String path, Context context, List<?> clients) {
        Log.d(TAG,"Knihovna startuje");
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

            // init db
            myDb = new DatabaseHelper(context);
            List<User> users = (List<User>) clients;
            for(User user : users) {
                myDb.insertData(user);
            }
            Log.d(TAG,"Knihovna spuštěna");
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
            List<User> users = (List<User>) clients;
            for(User user : users) {
                myDb.insertData(user);
            }
            Log.d(TAG,"knihovna opětovně spuštěna");
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
        for(User user : usersToActualize) {
            // kontrola zda klient existuje v db
            if(myDb.getUserById(user.getSsid()) != null) {
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
        return users;
    }
}
