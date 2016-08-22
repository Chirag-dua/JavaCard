package com.example.rishabh.bluetoothreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class QRCodeActivity extends AppCompatActivity {

    private static final String TAG = "QRCodeActivity";
    private MutualAuthenticationThread maThread;
    JavaCard javaCard;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy on QRActivity");
        // TODO Put in on Destroy of the new Activity
        javaCard.SDSCDisconnectDev();
        Log.e(TAG, "Destroying card");
        if(maThread != null && maThread.isAlive()) {
            maThread.cancel();
        }
        Log.i(TAG, "QRCodeActivity is destroyed");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        Intent intent = getIntent();
        String address = null;
        if(intent.hasExtra("address")) {
            address = intent.getStringExtra("address");
        }
        else {
            Log.e(TAG, "Bluetooth address not found");
        }
        if(address != null) {
            try {
                Bitmap code = encodeAsBitmap(address);
                ImageView image = (ImageView) findViewById(R.id.qrcode);
                image.setImageBitmap(code);
            } catch (WriterException ex) {
                Log.e(TAG, "Cannot display QR Code: " + ex.getMessage());
            }
        }
        int option = 0;
        if(intent.hasExtra("OPTION")) {
            option = intent.getIntExtra("OPTION", MainActivity.OPTION_BLUETOOTH);
        }
        else {
            Log.e(TAG, "Option is not found");
        }
        javaCard = JavaCard.getInstance(getApplicationContext());
        maThread = new MutualAuthenticationThread(null, option, javaCard);
        maThread.start();
    }

    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, 1000, 1000, null);
            Log.d(TAG, "String Encoded in QR Code");
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            Log.e(TAG, "Format not supported: " + iae.getMessage());
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ?  0xFF000000 : 0xFFFFFFFF;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, 1000, 0, 0, w, h);
        return bitmap;
    }
}
