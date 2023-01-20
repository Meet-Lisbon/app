package com.example.meetlisbon;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputLayout;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RouteAdd extends AppCompatActivity {
    RequestQueue queue;
    protected SharedPreferences prefs;
    ListView placeListView;
    Button createRoute;
    TextInputLayout routeName;
    TextInputLayout routeData;
    ArrayList<String> routePlaceIds = new ArrayList();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_add);
        prefs=this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        queue = Volley.newRequestQueue(this);
        placeListView = findViewById(R.id.placeListView);
        placeListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        createRoute = findViewById(R.id.createRoute);
        createRoute.setOnClickListener(v -> addRoute());
        routeName = findViewById(R.id.routeName);
        routeData = findViewById(R.id.routeDescription);
        populate();
    }

    private void addRoute() {
        String url = "https://api.meetlisbon.pt/api/routes";
        StringRequest req = new StringRequest(Request.Method.POST, url, resp -> {
            try {
                resp = new String(resp.getBytes("ISO-8859-1"), "UTF-8");
                Log.i("resp", resp);
                JSONObject jsonObject = new JSONObject(resp);
                String routeId = jsonObject.getString("id");
                String url2 = "https://api.meetlisbon.pt/api/routes/" + routeId + "/";
                for (String placeId : routePlaceIds) {
                    StringRequest req2 = new StringRequest(Request.Method.POST, url2 + placeId, resp2 -> {
                        DynamicToast.makeSuccess(this, "Done").show();
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
                    queue.add(req2);
                }

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

            @Override
            public byte[] getBody() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("routeName", routeName.getEditText().getText());
                    jsonObject.put("routeData", routeData.getEditText().getText());
                    return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        if(getToken() != null) queue.add(req);
    }

    @Override
    public void onRestart() {
        prefs=this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        this.populate();
        super.onRestart();

    }

    protected void populate() {
        String url = "https://api.meetlisbon.pt/api/places";

        HashMap<String, String> placeNameIds = new HashMap<>();
        StringRequest req = new StringRequest(Request.Method.GET, url, resp -> {
            Log.i("PRE", "Success!");

            try {
                resp = new String(resp.getBytes("ISO-8859-1"), "UTF-8");

                JSONArray jsonArray = new JSONArray(resp);
                for (int i = 0; i < jsonArray.length(); i++) {
                    Log.i("ON", "Success!");
                    placeNameIds.put(jsonArray.getJSONObject(i).getString("placeName"), jsonArray.getJSONObject(i).getString("id"));
                }

                ArrayAdapter<String> placeNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, new ArrayList<>(placeNameIds.keySet()));
                placeListView.setAdapter(placeNamesAdapter);
                placeListView.setOnItemClickListener((parent, view, position, id) -> {
                    Log.i("List", "onItemClick: " + position);
                    CheckedTextView v = (CheckedTextView) view;
                    boolean currentCheck = v.isChecked();
                    Log.i("checked", String.valueOf(currentCheck));
                    String placeName = placeListView.getItemAtPosition(position).toString();
                    if(!currentCheck) routePlaceIds.remove(placeNameIds.get(placeName));
                    else routePlaceIds.add(placeNameIds.get(placeName));
                });
            }
            catch (UnsupportedEncodingException | JSONException e) {
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