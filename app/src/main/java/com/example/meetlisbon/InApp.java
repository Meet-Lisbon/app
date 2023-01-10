package com.example.meetlisbon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.meetlisbon.ml.ModelUnquant;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
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
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class InApp extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    RequestQueue queue;
    protected SupportMapFragment supportMapFragment;
    protected FusedLocationProviderClient client;
    protected LocationManager locationManager;
    protected Location myLocation;
    protected Marker oldMarker;
    protected SharedPreferences prefs;
    protected SharedPreferences.Editor edit;
    protected boolean firstTime = true;
    GoogleMap map;
    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app);

        queue = Volley.newRequestQueue(this);

        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);
        final Button button = findViewById(R.id.locateMeButton);
        final Button wishlistButton = findViewById(R.id.wishList);
        final Button placesButton = findViewById(R.id.placesButton);
        final Button routesButton = findViewById(R.id.routeButton);
        final Button logOutButton = findViewById(R.id.logOut);
        logOutButton.setOnClickListener(view -> {
            prefs = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
            edit = prefs.edit();
            edit.clear();
            edit.commit();
            DynamicToast.makeWarning(this, "Logged out").show();
            Intent intent = new Intent(InApp.this, Login.class);
            startActivity(intent);
        });
        button.setOnClickListener(v -> getCurrentLocation(true));
        wishlistButton.setOnClickListener(v -> {
            edit = prefs.edit();
            edit.putBoolean("isWishlistActive", true);
            edit.commit();
            goToWishlist();
        });

        placesButton.setOnClickListener(v -> {
            edit = prefs.edit();
            edit.putBoolean("isWishlistActive", false);
            edit.commit();
            TextView textTitle = findViewById(R.id.textTitle);
            if (textTitle != null) textTitle.setText("Places");
            goToWishlist();
        });

        routesButton.setOnClickListener(v -> {
            edit = prefs.edit();
            edit.putBoolean("isWishlistActive", false);
            edit.commit();
            TextView textTitle = findViewById(R.id.textTitle);
            if (textTitle != null) textTitle.setText("Routes");
            goToRoutes();
        });

        Button buttonImage = findViewById(R.id.buttonRecognition);

        buttonImage.setOnClickListener(v -> cameraStart());
    }

    private void goToRoutes() {
        Intent intent = new Intent(InApp.this, RouteList.class);
        startActivity(intent);
    }

    private void cameraStart() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, 1);

    }

    public String BitMapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String temp = Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap originalImage = (Bitmap) data.getExtras().get("data");
            Bitmap image = cropToSquare(originalImage);
            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            originalImage = Bitmap.createScaledBitmap(originalImage, imageSize * 4, imageSize * 4, false);
            String classification = classifyImage(image);
            prefs = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
            edit = prefs.edit();
            edit.putString("imageRecognized", BitMapToString(originalImage));
            edit.putString("imageClassification", classification);
            edit.commit();

            Intent intent = new Intent(InApp.this, ImageRecognition.class);
            startActivity(intent);

//            SharedPreferences.Editor edit;
//            edit=prefs.edit();
//            edit.putString("placeDetailedView", classification);
//            edit.commit();
//            Intent intent = new Intent(InApp.this, PlaceDetailedView.class);
//            startActivity(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width) ? height - (height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0) ? 0 : cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0) ? 0 : cropH;
        Bitmap cropImg = Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);

        return cropImg;
    }

    private String classifyImage(Bitmap image) {
        try {
            ModelUnquant model = ModelUnquant.newInstance(getApplicationContext());

            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++]; // RGB extraction
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }
            inputFeature0.loadBuffer(byteBuffer);

            ModelUnquant.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();

            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {
                    "Torre de Belém",
                    "Padrão dos Descobrimentos",
                    "Farol de Belém",
                    "Praça Luís de Camões",
                    "Armazéns do Chiado",
                    "Casa dos Bicos / Fundação José Saramago",
                    "Kali"
            };

            String s = "";
            for (int i = 0; i < classes.length; i++) {
                s += String.format("%s: %.1f%%\n", classes[i], confidences[i] * 100);
            }
            Log.i("Confidences:", s);
            Log.i("Verdict:", classes[maxPos]);

            model.close();
            return classes[maxPos];
        } catch (IOException e) {
            // TODO Handle the exception
            throw new RuntimeException();
        }
    }


    private void goToWishlist() {
        Intent intent = new Intent(InApp.this, Wishlist.class);
        startActivity(intent);

    }

    @Override
    public void onLocationChanged(Location location) {
        myLocation = location;
        getCurrentLocation(firstTime);
        firstTime = false;
    }


    private void getCurrentLocation(boolean zoom) {

        if (myLocation != null) {
            LatLng latLng = new LatLng(myLocation.getLatitude()
                    , myLocation.getLongitude());
            MarkerOptions options = new MarkerOptions().position(latLng)
                    .title("Estou aqui!").icon(BitmapFromVector(getApplicationContext(), R.drawable.ic_blue_man));
            if(zoom) map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            if (oldMarker != null) {
                oldMarker.remove();
            }
            oldMarker = map.addMarker(options);
        } else {
            Log.i("Location", "null");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 56) {
            Log.i("Here", "doing");
            mapLocate(true);
            Log.i("Here", "after super");
        }

    }

    private void placeMarkers() {
        String url = "https://api.meetlisbon.pt/api/places";
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

    private void showMarker(double latitude, double longitude, String name) {
        Log.i("Placing Marker", String.format("Lat: %s Long: %s", latitude, longitude));
        supportMapFragment.getMapAsync(googleMap -> {
            LatLng latLng = new LatLng(latitude
                    , longitude);
            MarkerOptions options = new MarkerOptions().position(latLng)
                    .title(name).icon(BitmapFromVector(getApplicationContext(), R.drawable.ic_baseline_flag_24));
//            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
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
        prefs = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");
        return token;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        placeMarkers();
        mapLocate(true);

    }
    private void mapLocate(boolean zoom) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions
                    (this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 56);
            return;
        }
        client = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        myLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), false));
        getCurrentLocation(zoom);
    }
}