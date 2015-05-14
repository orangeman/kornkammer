package org.baobab.pos;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LegitimateActivity extends ActionBarActivity {

    private MediaPlayer win;
    private MediaPlayer fail;

    private static final String TAG = "POS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getSupportActionBar().setHomeButtonEnabled(true);
        win = MediaPlayer.create(this, R.raw.tada);
        fail = MediaPlayer.create(this, R.raw.trombone);
        if (getIntent().hasExtra("SCAN")) {
            Intent intent = new Intent(
                    "com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, 0);
        }
        setContentView(R.layout.activity_legitimate);
        ((EditText) findViewById(R.id.pin)).setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView view, int id, KeyEvent e) {
                        findViewById(R.id.result).setVisibility(View.INVISIBLE);
                        if (id == EditorInfo.IME_ACTION_SEARCH ||
                                id == EditorInfo.IME_ACTION_DONE ||
                                e.getAction() == KeyEvent.ACTION_DOWN &&
                                        e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            String hash = Crypt.hash(((EditText) view)
                                    .getText().toString(), LegitimateActivity.this);
                            legitimate(hash);
                        }
                        return false;
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String qr = data.getStringExtra("SCAN_RESULT");
            legitimate(Crypt.hash(qr, this));
        }
    }

    private boolean legitimate(String pin) {
        final Cursor auth = getContentResolver().query(Uri.parse(
                        "content://org.baobab.pos/legitimate?pin=" + pin),
                null, null, null, null);
        if (auth.getCount() != 1) {
            fail.start();
            findViewById(R.id.result).setVisibility(View.VISIBLE);
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(1400);
            return false;
        }
        auth.moveToFirst();
        if (auth.getString(1).equals("archive")) {
            fail.start();
            findViewById(R.id.result).setVisibility(View.VISIBLE);
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(1400);
            return false;
        } else if (auth.getString(1).equals("locked")) {
            findViewById(R.id.result).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.result)).setText("Jemand wollte den selben (evtl zu leichten?) Pin benutzen.\n Daher Konto vorläufig gesperrt bis ein neuer Pin gesetzt ist");
            findViewById(R.id.result).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Intent.ACTION_EDIT,
                            Uri.parse("content://org.baobab.pos/accounts/" + auth.getLong(0))));
                }
            });

            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(1400);
            Toast.makeText(LegitimateActivity.this, "Konto gesperrt!", Toast.LENGTH_LONG).show();
            return false;
        }
        if (win != null) {
            win.start();
        }
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(70);
        Cursor sum = getContentResolver().query(getIntent().getData().buildUpon()
                .appendEncodedPath("sum").build(), null, null, null, null);
        sum.moveToFirst();
        Log.d(TAG, "sum " + sum.getFloat(2));
        ContentValues b = new ContentValues();
        b.put("account_guid", auth.getString(3));
        b.put("quantity", sum.getFloat(2));
        getContentResolver().insert(
                getIntent().getData().buildUpon()
                        .appendEncodedPath("products/18")
                        .build(), b);
//        startActivity(new Intent(LegitimateActivity.this,
//                PosActivity.class).setData(getIntent().getData())
//                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
        return true;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT < 16) {
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
