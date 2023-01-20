package com.example.meetlisbon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    String routeId;
    TextView nameText;
    TextView dataText;
    ListView placeList;
    protected SupportMapFragment supportMapFragment;
    protected FusedLocationProviderClient client;
    protected LocationManager locationManager;
    RequestQueue queue;
    protected SharedPreferences prefs;
    GoogleMap map;
    Marker oldMarker;
    boolean start = false;
    boolean first = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);
        queue = Volley.newRequestQueue(getApplicationContext());
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this::onMapReady);
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


    private void placeMarkers() {
        String url = "https://api.meetlisbon.pt/api/routes/places?routeId=" + routeId;
        StringRequest req = new StringRequest(Request.Method.GET, url, resp -> {
            Log.i("Marker from db:", "Success!");
            try {
                resp = new String(resp.getBytes("ISO-8859-1"), "UTF-8");
                JSONArray jsonArray = new JSONArray(resp);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    showMarker(
                            Float.parseFloat(jsonObject.getString("placeLatitude")),
                            Float.parseFloat(jsonObject.getString("placeLongitude")),
                            jsonObject.getString("placeName")
                    );
                }
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
        if (getToken() != null) queue.add(req);
    }



    private void tensDeFuncionar(String origin, String destination, String waypoints) {

        String url = String.format("https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&waypoints=%s&sensor=false&mode=walking&key=AIzaSyAeUyaj3TMqmNMCU2g_15HJHcjJ-c9SZ4k", origin, destination, waypoints);
        StringRequest req = new StringRequest(Request.Method.GET, url, resp -> {
            Log.i("PRE", "Success!");
            try {
                resp = new String(resp.getBytes("ISO-8859-1"), "UTF-8");
                JSONObject jsonArray = new JSONObject(resp);
                JSONArray routesArray = jsonArray.getJSONArray("routes");
                JSONObject routesObj = routesArray.getJSONObject(0);
                JSONObject overviewObj = routesObj.getJSONObject("overview_polyline");
                Log.i("ovcerview", overviewObj.toString());
                String polyline = overviewObj.getString("points");
                Log.i("poly", polyline);
                List<LatLng> ll = PolyUtil.decode(polyline);
                PolylineOptions poly = new PolylineOptions();
                poly.addAll(ll);
                map.addPolyline(poly);
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
        queue.add(req);
    }
        //String url = String.format("https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&sensor=false&mode=driving&key=AIzaSyAeUyaj3TMqmNMCU2g_15HJHcjJ-c9SZ4k", origin, destination);

    @Override
    public void onLocationChanged(@NonNull Location location) {

        LatLng latLng = new LatLng(location.getLatitude()
                , location.getLongitude());
        MarkerOptions options = new MarkerOptions().position(latLng)
                .title("Estou aqui!").icon(BitmapFromVector(getApplicationContext(), R.drawable.ic_blue_man));
        if (oldMarker != null) {
            oldMarker.remove();
        }
        oldMarker = map.addMarker(options);
        if(first) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
            first = false;
        } else if(start) map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
    }

    private void showMarker(double latitude, double longitude, String name) {
        Log.i("Placing Marker", String.format("Lat: %s Long: %s", latitude, longitude));
        LatLng latLng = new LatLng(latitude
                , longitude);
        MarkerOptions options = new MarkerOptions().position(latLng)
                .title(name).icon(BitmapFromVector(getApplicationContext(), R.drawable.ic_baseline_flag_24));
//        if(first) {
//            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
//            first = false;
//        }
        map.addMarker(options);
    }

    private BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 56) {
            Log.i("Here", "doing");
            run();
            Log.i("Here", "after super");
        }

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Button button = findViewById(R.id.startButton);
        button.setOnClickListener(view -> {
          start = !start;
          if(start) button.setText("STOP");
          else button.setText("START");
        });
        this.map = googleMap;
        client = LocationServices.getFusedLocationProviderClient(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        run();

    }

    private void run() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Hello", "darkness my old friend");
            ActivityCompat.requestPermissions
                    (this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 56);
            return;
        }
        client.getLastLocation().addOnSuccessListener(location -> {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            placeMarkers();
            PolylineOptions poly = new PolylineOptions();
            String url1 = "https://api.meetlisbon.pt/api/routes/places?routeId=" + routeId;
            //String url = String.format("https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&waypoints=%s&sensor=false&mode=walking&key=AIzaSyAeUyaj3TMqmNMCU2g_15HJHcjJ-c9SZ4k", origin, destination, waypoints);
            RequestFuture<String> future = RequestFuture.newFuture();
            StringRequest req = new StringRequest(Request.Method.GET, url1, resp -> {
                Log.i("PRE", "Success!");

                try {
                    JSONArray placesArray = new JSONArray(resp);
                    JSONObject placeObj1 = placesArray.getJSONObject(0);
                    JSONObject placeObj2 = placesArray.getJSONObject(placesArray.length() - 1);
                    String origin = placeObj1.getString("placeLatitude") + "," + placeObj1.getString("placeLongitude");
                    String destination = placeObj2.getString("placeLatitude") + "," + placeObj2.getString("placeLongitude");
                    String latlongs = "";
                    latlongs += (String.format("%f,%f|", location.getLatitude(), location.getLongitude()));
                    for(int i=1; i<placesArray.length() - 1; i++) {
                        JSONObject placeObj = placesArray.getJSONObject(i);
                        String lat = placeObj.getString("placeLatitude");
                        String longitude = placeObj.getString("placeLongitude");
                        latlongs += (String.format("%s,%s|", lat, longitude));
                    }
                    latlongs = latlongs.substring(0, latlongs.length() - 1);
                    tensDeFuncionar(origin, destination, latlongs);
                } catch (JSONException e) {
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
        });

    }
}