package com.example.meetlisbon;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImageRecognition extends AppCompatActivity {
    SharedPreferences prefs;
    SharedPreferences.Editor edit;
    RequestQueue requestQueue;

    public Bitmap StringToBitMap(String encodedString){
        try {
            byte [] encodeByte= Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_recognition);
        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);
        edit=prefs.edit();
        ImageView landmarkImage = findViewById(R.id.landmarkImage);
        TextView landmarkText = findViewById(R.id.landmarkText);

        Bitmap image = StringToBitMap(prefs.getString("imageRecognized", ""));
        String classification = prefs.getString("imageClassification", "");
        landmarkImage.setImageBitmap(image);
        landmarkText.setText(classification);
        setDescription(classification);
    }

    private void setDescription(String placeName) {

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
                String description = jsonObject.getString("placeDescription");
                TextView descriptionView = findViewById(R.id.descriptionText);
                descriptionView.setText(Html.fromHtml(description));

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
        if(getToken() != null) requestQueue.add(req);
    }

    private String getToken() {
        String token = prefs.getString("token","");
        return token;
    }

}