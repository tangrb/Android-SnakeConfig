package com.trb.android.snakeconfig;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.IOException;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void load_ins_charset_onClick(View view) {
        try {
            System.out.println("start time: " + System.currentTimeMillis());
            boolean success = SnakeConfig.getDefault().load(getAssets().open("demo.properties"), Charset.forName("UTF-8"));
            System.out.println("end time: " + System.currentTimeMillis());
            System.out.println("success ? " + success);
            SnakeConfig.getDefault().printConfigItemList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setProperty_onClick(View view) {
        SnakeConfig.getDefault().setProperty("name", "Roby");
        SnakeConfig.getDefault().setProperty("height", 173);
        SnakeConfig.getDefault().setProperty("weight", 63.5);
        SnakeConfig.getDefault().setProperty("sex", "men");
        SnakeConfig.getDefault().setProperty("age", (byte) 30);
        SnakeConfig.getDefault().setProperty("enable", true);
        SnakeConfig.getDefault().printConfigItemList();
    }

    public void getX_onClick(View view) {
        System.out.println("name=" + SnakeConfig.getDefault().getString("name", null));
        System.out.println("height=" + SnakeConfig.getDefault().getInt("height", 0));
        System.out.println("weight=" + SnakeConfig.getDefault().getReal("weight", 0));
        System.out.println("sex=" + SnakeConfig.getDefault().getString("sex", "women"));
        System.out.println("age=" + SnakeConfig.getDefault().getByte("age", (byte) 0));
        System.out.println("xxx=" + SnakeConfig.getDefault().getShort("xxx", (short) -1));
        System.out.println("yyy=" + SnakeConfig.getDefault().getLong("yyy", 11111111));
        System.out.println("enable=" + SnakeConfig.getDefault().getBoolean("enable", false));
    }

    public void commit_onClick(View view) {
        SnakeConfig.getDefault().setStorage(true, getFilesDir().getAbsolutePath() + "/demo.properties", Charset.forName("UTF-8"));
        SnakeConfig.getDefault().commitSync();
        SnakeConfig.getDefault().setProperty("commitType", "async");
        SnakeConfig.getDefault().commitAsync();

        try {
            SnakeConfig.getInstance("demo_write").load(openFileInput("demo.properties"), Charset.forName("UTF-8"));
            SnakeConfig.getInstance("demo_write").printConfigItemList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
