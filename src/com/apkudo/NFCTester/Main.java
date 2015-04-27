package com.apkudo.NFCTester;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.*;
import android.nfc.tech.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class Main extends Activity {
    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] mFilters;
    private String[][] mTechLists;
    private String TAG = "SHIBBY";
    TextView textView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main);

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Setup an intent filter for all MIME based dispatches
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        IntentFilter td = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        mFilters = new IntentFilter[] {
                ndef, td
        };

        // Setup a tech list for all NfcF tags
        mTechLists = new String[][] { new String[] {
                NfcV.class.getName(),
                NfcF.class.getName(),
                NfcA.class.getName(),
                NfcB.class.getName()
        } };
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mAdapter.enableForegroundDispatch(this, pendingIntent, mFilters, mTechLists);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onNewIntent(Intent intent){
        // fetch the tag from the intent
        Tag t = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        new NFCDataReader().execute(t);
    }

    public class NFCDataReader extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                Log.w(TAG, "NOT SUPPORTED !!!!");
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                try {
                    return readText(ndefRecord);
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Unsupported Encoding", e);
                }
            }
            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            String result = "Mime Type: ";
            result += record.toMimeType();
            result += " String: ";
            result += record.toString();
            result += " Payload: ";
            result += new String(payload, textEncoding);

            // Get the Text
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                String str = "\r\n" + result;
                Log.w(TAG, str);
                textView = (TextView) findViewById(R.id.textView);
                textView.append(str);
            }
        }
    }
}
