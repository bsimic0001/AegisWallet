package com.aegiswallet.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aegiswallet.R;
import com.aegiswallet.listeners.SimpleGestureFilter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;
import java.util.List;

public class MyActivity extends Activity implements SimpleGestureFilter.SimpleGestureListener {

    private SimpleGestureFilter detector;
    private TextView mTextView;
    private TextView balanceView;

    private static final int SPEECH_REQUEST_CODE = 0;
    private QRCodeWriter qrCodeWriter;
    private SharedPreferences prefs;
    private String address;
    private ImageView addressImageView;
    private Context context = this;

    private int STATE_QR = 100;
    private int STATE_BAL = 101;
    private int CURRENT_STATE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        detector = new SimpleGestureFilter(this, this);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                addressImageView = (ImageView) stub.findViewById(R.id.address_image);
                balanceView = (TextView) stub.findViewById(R.id.balance_text);

                prefs = PreferenceManager.getDefaultSharedPreferences(context);
                address = prefs.getString("ADDRESS", null);

                Log.d("MAINACTIVITY", "ADDRESS IS: " + address);
                Log.d("MAINACTIVITY", "Image View IS: " + addressImageView);

                if (address != null && addressImageView != null) {
                    Log.d("MAINACTIVITY", "address image view is not null");

                    qrCodeWriter = new QRCodeWriter();
                    Bitmap addressBitmap = encodeAsBitmap(address, BarcodeFormat.QR_CODE, 200);
                    addressImageView.setImageBitmap(addressBitmap);

                    addressImageView.setVisibility(View.VISIBLE);
                    CURRENT_STATE = STATE_QR;

                } else {
                    Log.d("MAINACTIVITY", "address image view must be null");
                }
            }
        });


        //displaySpeechRecognizer();
    }

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            Log.d("Main", "spoken text: " + spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int dimension) {

        Bitmap bitmap = null;

        try {
            final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            final BitMatrix result = qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, dimension, dimension, hints);

            final int width = result.getWidth();
            final int height = result.getHeight();
            final int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                final int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }

            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;

        } catch (WriterException e) {
            Log.e("Basic Utils", "cannot write to bitmap " + e.getMessage());
        }
        return bitmap;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent me) {
        // Call onTouchEvent of SimpleGestureFilter class
        this.detector.onTouchEvent(me);
        return super.dispatchTouchEvent(me);
    }

    @Override
    public void onSwipe(int direction) {
        String str = "";

        switch (direction) {

            case SimpleGestureFilter.SWIPE_RIGHT:
                str = "Swipe Right";
                break;
            case SimpleGestureFilter.SWIPE_LEFT:
                str = "Swipe Left";
                break;
            case SimpleGestureFilter.SWIPE_DOWN:
                str = "Swipe Down";
                handleSwipe(SimpleGestureFilter.SWIPE_DOWN);
                break;
            case SimpleGestureFilter.SWIPE_UP:
                str = "Swipe Up";
                handleSwipe(SimpleGestureFilter.SWIPE_UP);
                break;

        }
        //Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDoubleTap() {
       Log.d("MyActivity", "Double Tap");
    }

    private void handleSwipe(int swipeType){

        if(CURRENT_STATE == STATE_QR){
            addressImageView.setVisibility(View.GONE);
            balanceView.setVisibility(View.VISIBLE);
            CURRENT_STATE = STATE_BAL;

            balanceView.setText(prefs.getString("BALANCE", "Balance not Synced"));
        }
        else if(CURRENT_STATE == STATE_BAL){
            addressImageView.setVisibility(View.VISIBLE);
            balanceView.setVisibility(View.GONE);
            CURRENT_STATE = STATE_QR;
        }


    }

    public static class ToTheMoonActivity extends Activity {

        private TextView mTextView;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_my);
            final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
            stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                @Override
                public void onLayoutInflated(WatchViewStub stub) {
                    mTextView = (TextView) stub.findViewById(R.id.text);
                    Log.d("TAG", "TextView: " + mTextView.getText() + " view=" + mTextView);
                }
            });
        }
    }
}