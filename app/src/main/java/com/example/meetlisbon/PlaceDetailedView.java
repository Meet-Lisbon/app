package com.example.meetlisbon;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PlaceDetailedView extends AppCompatActivity {
    RequestQueue queue;
    SharedPreferences prefs;
    SharedPreferences.Editor edit;
    String placeName;
    ImageView imageView;
    TextView placeNameView;
    TextView placeAddressView;
    TextView placeLatitudeView;
    TextView placeLongitudeView;
    HashSet<String> wishlistData;
    Button buttonWishlist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detailed_view);
        queue = Volley.newRequestQueue(this);
        prefs = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        placeName = prefs.getString("placeDetailedView", "");
        Log.i("Place:", placeName);
        imageView = findViewById(R.id.imageViewPlace);
        placeNameView = findViewById(R.id.textPlaceName);
        placeAddressView = findViewById(R.id.textAddress);
        placeLatitudeView = findViewById(R.id.placeLatitude);
        placeLongitudeView = findViewById(R.id.placeLongitude);

        wishlistData = (HashSet) prefs.getStringSet("wishlistData", new HashSet<>());
        buttonWishlist = findViewById(R.id.buttonWishlist);
        checkWishlist(placeName);
        download();
    }

    protected boolean isInWishlist(String placeName) {
        Log.i("startName", placeName);
        for (String name : wishlistData) {
            Log.i("compareName", name);
            if (name.equalsIgnoreCase(placeName)) return true;
        }
        return false;
    }

    protected void download() {
        String url = "https://api.meetlisbon.pt/api/places?placeName=";
        String encodedPlaceName = null;
        try {
            encodedPlaceName = URLEncoder.encode(placeName, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        url += encodedPlaceName;
        Log.i("After:", encodedPlaceName);
        StringRequest req = new StringRequest(Request.Method.GET, url, resp -> {
            Log.i("PRE", "Success!");
            try {
                resp = new String(resp.getBytes("ISO-8859-1"), "UTF-8");
                JSONArray jsonArray = new JSONArray(resp);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                edit = prefs.edit();
                edit.putString("placeId", jsonObject.getString("id"));
                edit.commit();
                placeNameView.setText(jsonObject.getString("placeName"));
                placeLatitudeView.setText(jsonObject.getString("placeLatitude"));
                placeLongitudeView.setText(jsonObject.getString("placeLongitude"));
                placeAddressView.setText(jsonObject.getString("placeAddress"));
//                placeNameView.setText(jsonObject.getString("placeDescription"));
//                jsonObject.getString("placeImageUrl");
                ImageRequest req2 = new ImageRequest(
                        jsonObject.getString("placeImageUrl"),
                        (Response.Listener<Bitmap>) response -> {
                            imageView.setImageBitmap(response);
                        },
                        0,
                        0, ImageView.ScaleType.CENTER_CROP, null, null);
                queue.add(req2);

            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }

        }, error -> DynamicToast.makeError(this, "Invalid credentials!").show()) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                Log.i("Token", getToken());
                params.put("Authorization", "Bearer " + getToken());
                return params;
            }
        };
        queue.add(req);
    }

    protected void checkWishlist(String placeName) {
        String url;
        url = "https://api.meetlisbon.pt/api/wishlist";

        ArrayList<String> placeNames = new ArrayList();
        StringRequest req = new StringRequest(Request.Method.GET, url, resp -> {
            Log.i("PRE", "Success!");
            try {
                resp = new String(resp.getBytes("ISO-8859-1"), "UTF-8");
                JSONArray jsonArray = new JSONArray(resp);
                for (int i = 0; i < jsonArray.length(); i++) {
                    Log.i("ON", "Success!");
                    placeNames.add(jsonArray.getJSONObject(i).getString("placeName"));
                }
                edit = prefs.edit();
                if (placeNames.contains(placeName)) {
                    edit.putBoolean("removeWishlist", true);
                    buttonWishlist.setText("Remover da wishlist");
                    buttonWishlist.setOnClickListener(v -> removeFromWishlist());
                } else {
                    edit.putBoolean("removeWishlist", false);
                    buttonWishlist.setText("Adicionar à wishlist");
                    buttonWishlist.setOnClickListener(v -> addToWishlist());
                }
                edit.commit();
            } catch (UnsupportedEncodingException | JSONException e) {
                e.printStackTrace();
            }
        }, error -> DynamicToast.makeError(this, "Invalid credentials!").show()) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                Log.i("Token", getToken());
                params.put("Authorization", "Bearer " + getToken());
                return params;
            }

        };
        if (getToken() != null) queue.add(req);
    }

    private String getToken() {
        String token = prefs.getString("token", "");
        return token;
    }


    protected void removeFromWishlist() {
        String url = "https://api.meetlisbon.pt/api/wishlist/" + prefs.getString("placeId", "");
        StringRequest req = new StringRequest(Request.Method.DELETE, url, resp -> {
            buttonWishlist.setText("Adicionar à wishlist");
            buttonWishlist.setOnClickListener(v -> addToWishlist());
            Log.i("PRE", "Success!");
        }, error -> DynamicToast.makeError(this, "Invalid credentials!").show()) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                Log.i("Token", getToken());
                params.put("Authorization", "Bearer " + getToken());
                return params;
            }
        };
        if(getToken() != null) queue.add(req);
    }

    protected void addToWishlist() {
        String url = "https://api.meetlisbon.pt/api/wishlist/" + prefs.getString("placeId", "");

        StringRequest req = new StringRequest(Request.Method.POST, url, resp -> {
            buttonWishlist.setText("Remover da wishlist");
            buttonWishlist.setOnClickListener(v -> removeFromWishlist());
            Log.i("PRE", "Success!");
        }, error -> DynamicToast.makeError(this, "Invalid credentials!").show()) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                Log.i("Token", getToken());
                params.put("Authorization", "Bearer " + getToken());
                return params;
            }
        };
        if(getToken() != null) queue.add(req);
    }
}