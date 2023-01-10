package com.example.meetlisbon;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Wishlist extends AppCompatActivity {
    RequestQueue queue;
    protected SharedPreferences prefs;
    ListView wishlistView;
    boolean isWishlistActive;
    boolean isRoutesActive;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);
        queue = Volley.newRequestQueue(this);
        setPreferences();
    }

    @Override
    public void onRestart() {
        setPreferences();
        super.onRestart();

    }

    protected void setPreferences() {
        prefs=this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        TextView textTitle = findViewById(R.id.textTitle);
        isWishlistActive = prefs.getBoolean("isWishlistActive", false);
        if(isWishlistActive) textTitle.setText("WISHLIST");
        else textTitle.setText("PLACES");
        wishlistView = findViewById(R.id.wishlistView);
        this.populate(wishlistView, isWishlistActive);
    }
    protected void populate(ListView wishlistView, boolean isWishlist) {
        String url;
        if(isWishlist) url = "https://api.meetlisbon.pt/api/wishlist";
        else url = "https://api.meetlisbon.pt/api/places";

        ArrayList<String> placeNames = new ArrayList();
        StringRequest req = new StringRequest(Request.Method.GET, url, resp -> {
            try {
                resp = new String(resp.getBytes("ISO-8859-1"), "UTF-8");
                JSONArray jsonArray = new JSONArray(resp);
                for(int i=0; i < jsonArray.length(); i++) {
                    Log.i("ON", "Success!");
                    placeNames.add(jsonArray.getJSONObject(i).getString("placeName"));
                }
                ArrayAdapter<String> placeNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, placeNames);
                wishlistView.setAdapter(placeNamesAdapter);
                this.wishlistView.setOnItemClickListener((adapterView, view, i, l) -> {
                    SharedPreferences.Editor edit;
                    edit=prefs.edit();
                    edit.putString("placeDetailedView", wishlistView.getItemAtPosition(i).toString());
                    edit.commit();
                    Intent intent = new Intent(Wishlist.this, PlaceDetailedView.class);
                    startActivity(intent);
                });
                Log.i("OFF", "Success!");
            } catch (JSONException | UnsupportedEncodingException e) {
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
        if(getToken() != null) queue.add(req);
    }

    private String getToken() {
        String token = prefs.getString("token","");
        return token;
    }

}