package com.example.meetlisbon;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {
    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        queue = Volley.newRequestQueue(this);
//        toasts = new Toasts();
    }

    public void signUp(View view) {
        EditText firstNameText = findViewById(R.id.firstName);
        EditText lastNameText = findViewById(R.id.lastName);
        EditText userText = findViewById(R.id.usernameCreate);
        EditText emailText = findViewById(R.id.emailInsert);
        EditText passText = findViewById(R.id.passwordCreate);
        String url = "https://api.meetlisbon.pt/api/user/register";
        StringRequest req = new StringRequest(Request.Method.POST, url,
                resp -> {

                    DynamicToast.makeSuccess(this, "User created!").show();
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                },
                error -> DynamicToast.makeError(this, "User not created!").show()) {
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
                    jsonObject.put("usrEmail", emailText.getText());
                    jsonObject.put("firstName", firstNameText.getText());
                    jsonObject.put("lastName", lastNameText.getText());

                    return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
                } catch (JSONException e) {

                    e.printStackTrace();
                    return null;
                }
            }
        };
        queue.add(req);
    }

    public void toast_error(Context context, String info) {
        LayoutInflater li = getLayoutInflater();
        View layout = li.inflate(R.layout.toast_error, findViewById(R.id.custom_toast_layout));
        Toast toast = Toast.makeText(context, info, Toast.LENGTH_LONG);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL, -50, 150);
        TextView toastText = layout.findViewById(R.id.custom_toast_message);
        toastText.setText(info);
        toast.setView(layout);

        toast.show();
    }

}