package cz.fim.uhk.thesis.libraryforofflinemode.api;

import java.util.List;

import cz.fim.uhk.thesis.libraryforofflinemode.model.User;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Komunikační rozhraní knihovny pro offline režim
 * Definice požadavků na centrální server pro knihovnu Retrofit 2
 * Pro synchronizaci seznamu klientů se serverem při využití knihovny pro realizaci role P2P serveru
 * v metodě exit() - při ukončení běhu knihovny
 */
public interface CentralServerApi {
    @Headers("Content-Type: application/json")
    @POST("users/send")
    Call<ResponseBody> sendUsers(@Body List<User> users);
}
