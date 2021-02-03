package cz.fim.uhk.thesis.libraryforofflinemode.api;

import java.util.List;

import cz.fim.uhk.thesis.libraryforofflinemode.model.User;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface CentralServerApi {
    @Headers("Content-Type: application/json")
    @POST("users/send")
    Call<ResponseBody> sendUsers(@Body List<User> users);
}
