package com.example.meetlisbon;

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
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class InApp extends AppCompatActivity implements LocationListener {
    RequestQueue queue;
    protected SupportMapFragment supportMapFragment;
    protected FusedLocationProviderClient client;
    protected LocationManager locationManager;
    protected Location myLocation;
    protected Marker oldMarker;
    protected SharedPreferences prefs;
    protected SharedPreferences.Editor edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app);

        queue = Volley.newRequestQueue(this);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);

        client = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(InApp.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 44);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        getCurrentLocation();
        placeMarkers();
        final Button button = findViewById(R.id.locateMeButton);
        final Button logOutButton = findViewById(R.id.logOut);
        logOutButton.setOnClickListener(view -> {
            prefs=this.getSharedPreferences("settings", Context.MODE_PRIVATE);
            edit=prefs.edit();
            edit.clear();
            edit.commit();
            DynamicToast.makeWarning(this, "Logged out").show();
            Intent intent = new Intent(InApp.this, MainActivity.class);
            startActivity(intent);
        });
        button.setOnClickListener(v -> getCurrentLocation());
    }

    @Override
    public void onLocationChanged(Location location) {
        myLocation = location;
        Log.i("Changed:", location.toString());

    }


    private void getCurrentLocation() {
        if (myLocation != null){
            supportMapFragment.getMapAsync(googleMap -> {
                LatLng latLng = new LatLng(myLocation.getLatitude()
                        ,myLocation.getLongitude());
                MarkerOptions options = new MarkerOptions().position(latLng)
                        .title("Estou aqui!").icon(BitmapFromVector(getApplicationContext(), R.drawable.ic_blue_man));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 30));
                if(oldMarker != null) {
                    oldMarker.remove();
                }
                oldMarker = googleMap.addMarker(options);
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }

    private void placeMarkers() {
        String url = "https://api.meetlisbon.pt/api/place/all";
        StringRequest req = new StringRequest(Request.Method.GET, url, resp -> {
            Log.i("Marker from db:", "Success!");
            try {
                resp = new String(resp.getBytes("ISO-8859-1"), "UTF-8");
                JSONArray jsonArray = new JSONArray(resp);
                for(int i=0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    showMarker (
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
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                Log.i("Token", getToken());
                params.put("Authorization", "Bearer " + getToken());
                return params;
            }

        };
        if(getToken() != null) queue.add(req);
    }

    private void showMarker(double latitude, double longitude, String name) {
        Log.i("Placing Marker", String.format("Lat: %s Long: %s", latitude, longitude));
        supportMapFragment.getMapAsync(googleMap -> {
            LatLng latLng = new LatLng(latitude
                    ,longitude);
            MarkerOptions options = new MarkerOptions().position(latLng)
                    .title(name).icon(BitmapFromVector(getApplicationContext(), R.drawable.ic_baseline_flag_24));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
            googleMap.addMarker(options);
        });
    }

    private BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private String getToken() {
        prefs=this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String token = prefs.getString("token","");
        return token;
    }
}