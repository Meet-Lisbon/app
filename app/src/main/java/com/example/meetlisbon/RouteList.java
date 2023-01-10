package com.example.meetlisbon;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RouteList extends AppCompatActivity {
    RequestQueue queue;
    protected SharedPreferences prefs;
    ListView routeListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list);
        prefs = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        queue = Volley.newRequestQueue(this);
        routeListView = findViewById(R.id.routeView);
        MaterialButton button = findViewById(R.id.newButton);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), RouteAdd.class);
            startActivity(intent);
        });
        populate();
    }

    protected void populate() {
        String url = "https://api.meetlisbon.pt/api/routes";
        ArrayList<String> routeNames = new ArrayList();
        ArrayList<String> routeIds = new ArrayList();
        ArrayList<String> routeDatas = new ArrayList();
        StringRequest req = new StringRequest(Request.Method.GET, url, resp -> {
            Log.i("PRE", "Success!");
            try {
                resp = new String(resp.getBytes("ISO-8859-1"), "UTF-8");
                JSONArray jsonArray = new JSONArray(resp);
                for(int i=0; i < jsonArray.length(); i++) {
                    Log.i("ON", "Success!");
                    routeNames.add(jsonArray.getJSONObject(i).getString("routeName"));
                    routeIds.add(jsonArray.getJSONObject(i).getString("id"));
                    routeDatas.add(jsonArray.getJSONObject(i).getString("routeData"));
                }
                ArrayAdapter<String> placeNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, routeNames);
                routeListView.setAdapter(placeNamesAdapter);
                this.routeListView.setOnItemClickListener((adapterView, view, i, l) -> {
                    SharedPreferences.Editor edit;
                    edit=prefs.edit();
                    edit.putString("routeId", routeIds.get(i));
                    edit.putString("routeName", routeNames.get(i));
                    edit.putString("routeData", routeDatas.get(i));
                    edit.commit();
                    Intent intent = new Intent(RouteList.this, RouteActivity.class);
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



