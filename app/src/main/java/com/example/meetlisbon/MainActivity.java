package com.example.meetlisbon;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    Animation topAnim, botAnim;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animator);
        botAnim = AnimationUtils.loadAnimation(this, R.anim.bot_animator);

        ImageView logoTop = findViewById(R.id.logoTop);
        ImageView logoBot = findViewById(R.id.logoBot);
        logoTop.setAnimation(topAnim);
        logoBot.setAnimation(botAnim);

        new Handler().postDelayed((Runnable) () -> {
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
            finish();
        }, 2000);
    }
}