package com.yeeun.pics;
import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FileUploadUtils {
    public static String res;

    public static void sendToServer(File file) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), RequestBody.create(MultipartBody.FORM, file))
                    .build();
            Request request = new Request.Builder()
                    .url("http://54.180.104.24:5000/fileandcroll")
                    .post(requestBody)
                    .build();

            //Sync
            Response response = client.newCall(request).execute();

            String message = response.body().string();
            res= message;

//Async
//            client.newCall(request).enqueue(new Callback() {
//                    @Override
//                    public void onFailure(Call call, IOException e) {
//                        e.printStackTrace();
//                        res = call.toString();
//                    }
//
//                    @Override
//                    public void onResponse(Call call, Response response) throws IOException {
//                        Log.d("TEST: ", response.body().string());
//                        res = response.body().string();
//                    }
//                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            //       return res;
        }
}

