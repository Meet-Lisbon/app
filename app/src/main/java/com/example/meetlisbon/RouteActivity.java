package com.example.meetlisbon;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RouteActivity extends AppCompatActivity {

    String routeId;
    TextView nameText;
    TextView dataText;
    ListView placeList;

    RequestQueue queue;
    protected SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);
        queue = Volley.newRequestQueue(getApplicationContext());

        prefs=this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String routeName = prefs.getString("routeName", "");
        String routeData = prefs.getString("routeData", "");
        routeId = prefs.getString("routeId", "");

        nameText = findViewById(R.id.nameText);
        dataText = findViewById(R.id.dataText);
        placeList = findViewById(R.id.placesList);

        nameText.setText(routeName);
        dataText.setText(routeData);

        populate();
    }

    private void populate() {
        String url = "https://api.meetlisbon.pt/api/routes/places?routeId=" + routeId;

        ArrayList<String> placeNames = new ArrayList();
        StringRequest req = new StringRequest(Request.Method.GET, url, resp -> {
            Log.i("PRE", "Success!");
            try {
                resp = new String(resp.getBytes("ISO-8859-1"), "UTF-8");
                JSONArray jsonArray = new JSONArray(resp);
                for(int i=0; i < jsonArray.length(); i++) {
                    Log.i("ON", "Success!");
                    placeNames.add(jsonArray.getJSONObject(i).getString("placeName"));
                }
                ArrayAdapter<String> placeNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, placeNames);
                placeList.setAdapter(placeNamesAdapter);
                this.placeList.setOnItemClickListener((adapterView, view, i, l) -> {
                    SharedPreferences.Editor edit;
                    edit=prefs.edit();
                    edit.putString("placeDetailedView", placeList.getItemAtPosition(i).toString());
                    edit.commit();
                    Intent intent = new Intent(RouteActivity.this, PlaceDetailedView.class);
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