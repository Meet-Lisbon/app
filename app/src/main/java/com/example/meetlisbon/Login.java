package com.example.meetlisbon;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputLayout;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Login extends AppCompatActivity {
    RequestQueue queue;
    SharedPreferences prefs;
    SharedPreferences.Editor edit;
    Button buttonLogin;
    Button buttonRegister;
    Button buttonGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        queue = Volley.newRequestQueue(this);
        setContentView(R.layout.activity_login);
        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        edit=prefs.edit();

        String token = this.getToken();
        if(!token.isEmpty()) {
            // TODO verify token before proceeding
            this.goToApp();
        }

        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonGuest = findViewById(R.id.buttonGuest);

        buttonLogin.setOnClickListener(view -> logIn(view));
        buttonRegister.setOnClickListener(view -> signUp(view));
        buttonGuest.setOnClickListener(view -> continueAsGuest(view));

    }

    protected void goToApp() {
        Intent intent = new Intent(Login.this, InApp.class);
        startActivity(intent);
    }

    public void continueAsGuest(View view) { goToApp(); }

    public void logIn(View view) {
        EditText userText = ((TextInputLayout)findViewById(R.id.username)).getEditText();
        EditText passText = ((TextInputLayout)findViewById(R.id.password)).getEditText();
        String url = "https://api.meetlisbon.pt/api/login";

        StringRequest req = new StringRequest(Request.Method.POST, url, resp -> {
            DynamicToast.makeSuccess(this, "Login Successful!").show();
            if (!resp.isEmpty()) {
                try {
                    JSONObject tokenObj = new JSONObject(resp);
                    edit.putString("token",tokenObj.getString("token"));
                    edit.commit();
                    this.goToApp();
                } catch (JSONException e) {
                    DynamicToast.makeError(this, "Invalid credentials!").show();
                    e.printStackTrace();
                }
            } else {
                DynamicToast.makeError(this, "Invalid credentials!").show();
            }
        }, error -> DynamicToast.makeError(this, "Invalid credentials!").show()) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=UTF-8";
            }

            @Override
            public byte[] getBody() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("usrName", userText.getText());
                    jsonObject.put("usrPassword", passText.getText());
                    jsonObject.put("usrName", userText.getText());

                    return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        queue.add(req);
    }

    public void signUp(View view) {
        Intent intent = new Intent(Login.this, SignUp.class);
        startActivity(intent);
    }

    private String getToken() {
        prefs=this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String token = prefs.getString("token","");
        return token;
    }

    public void toast_error(Context context, String info) {
        LayoutInflater li = getLayoutInflater();
        View layout = li.inflate(R.layout.toast_error, findViewById(R.id.custom_toast_layout));
        Toast toast = Toast.makeText(context, info, Toast.LENGTH_LONG);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL, -50, -50);
        TextView toastText = layout.findViewById(R.id.custom_toast_message);
        toastText.setText(info);
        toast.setView(layout);

        toast.show();
    }

    public void toast_success(Context context, String info) {
        DynamicToast.makeSuccess(getApplicationContext(), "Success toast").show();
    }

}