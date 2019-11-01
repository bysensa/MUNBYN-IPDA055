package com.android.bluetoothprinter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by Administrator on 2018/8/2.
 */

public class MainActivity extends AppCompatActivity {

    private static final int TIME1 = Menu.FIRST;
    private static final int TIME2 = Menu.FIRST+1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,TIME1,0,"58mm");
        menu.add(0,TIME2,0,"80mm");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case TIME1:
                break;
            case TIME2:
                break;
        }
        return true;
    }
}
