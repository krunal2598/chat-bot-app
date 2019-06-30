package com.example.chatbot;

//import android.content.DialogInterface;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private TextToSpeech textToSpeech;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    ImageButton btnSpeak;
    TextView txtSpeechInput, outputText;


    //ON CREATE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        outputText = (TextView) findViewById(R.id.outputTex);
        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });


        //For TTS initialization
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int ttsLang = textToSpeech.setLanguage(Locale.US);

                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The Language is not supported!");
                    } else {
                        Log.i("TTS", "Language Supported.");
                    }
                    Log.i("TTS", "Initialization success.");
                } else {
                    Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    //ON DESTROY
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }


    //Showing google speech input dialog
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Say Something");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Sorry! Your device doesn't support speech input",
                    Toast.LENGTH_SHORT).show();
        }
    }


    //Receiving speech input
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String userQuery = result.get(0);
                    txtSpeechInput.setText(userQuery);
                    RetrieveFeedTask task = new RetrieveFeedTask();
                    task.execute(userQuery);
                }
                break;
            }

        }
    }

    // Create GetText Metod
    public String GetText(String query) throws UnsupportedEncodingException {
        String text = "";
        BufferedReader reader = null;

        // Send data
        try {
            // Defined URL  where to send data
            URL url = new URL("https://api.api.ai/v1/query?v=20150910");

            // Send POST data request

            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);

            conn.setRequestProperty("Authorization", "Bearer f430fa5f65fa4961a8870cdd36459050 ");
            conn.setRequestProperty("Content-Type", "application/json");

            //Create JSONObject here
            JSONObject jsonParam = new JSONObject();
            JSONArray queryArray = new JSONArray();
            queryArray.put(query);
            jsonParam.put("query", queryArray);
            jsonParam.put("lang", "en");
            jsonParam.put("sessionId", "1234567890");


            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            Log.d("karma", "after conversion is " + jsonParam.toString());
            wr.write(jsonParam.toString());
            wr.flush();
            Log.d("karma", "json is " + jsonParam);

            // Get the server response
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;


            // Read Server Response
            while ((line = reader.readLine()) != null) {
                // Append server response in string
                sb.append(line + "\n");
            }


            text = sb.toString();


            JSONObject object1 = new JSONObject(text);
            JSONObject object = object1.getJSONObject("result");
            Log.d("JSON", object.toString());
            JSONObject parameters = object.getJSONObject("parameters"), fulfillment = null;

            String speech = null;
//            if (object.has("fulfillment")) {
            fulfillment = object.getJSONObject("fulfillment");
//                if (fulfillment.has("speech")) {
            speech = fulfillment.optString("speech");
//                }
//            }

            if (object.has("contexts")) {
                //Getting the context
                String strContext = object.getJSONArray("contexts").getJSONObject(0).getString("name");

                if (strContext.equals("open")) {
                    //For Opening apps
                    if (!openApp(parameters.getString("App_names"))) {
                        speak("Sorry, the app was not found");
                    }
                }
            }


            Log.d("karma ", "response is " + text);
            return speech;
        } catch (Exception ex) {
            Log.d("karma", "exception at last " + ex);
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
            }
        }
        return null;
    }


    class RetrieveFeedTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... voids) {
            String s = null;
            try {
                s = GetText(voids[0]);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.d("karma", "Exception occurred " + e);
            }
            return s;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            outputText.setText(s);

            //Speak text
            speak(s);
        }
    }


    //For Opening apps
    boolean openApp(String appName) {
        boolean blappFound = false;

        final PackageManager pm = getApplicationContext().getPackageManager();
        List<ApplicationInfo> lstInstalledApps = pm.getInstalledApplications(0);

        for (int i = 0; i < lstInstalledApps.size() - 1; i++) {
            ApplicationInfo ai = lstInstalledApps.get(i);

            final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "unknown");
            Log.d("", applicationName);
            if (applicationName != "unknown" && applicationName.toLowerCase().trim().equals(appName)) {
                //The actual code for opening apps
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(ai.packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);//null pointer check in case package name was not found
                }
                blappFound = true;
                break;
            }
        }

        if (blappFound == false) {
            for (int i = 0; i < lstInstalledApps.size() - 1; i++) {
                ApplicationInfo ai = lstInstalledApps.get(i);
                final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "unknown");
                if (applicationName != "unknown" && applicationName.toLowerCase().trim().contains(appName)) {
                    //The actual code for opening apps
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(ai.packageName);
                    if (launchIntent != null) {
                        startActivity(launchIntent);//null pointer check in case package name was not found
                    }
                    blappFound = true;
                    break;
                }
            }
        }
        return blappFound;
    }


    //For speaking any text
    int speak(String toSpeak) {
        int speechStatus = 0;
        try {
            speechStatus = textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            if (speechStatus == TextToSpeech.ERROR) {
                Log.e("TTS", "Error in converting Text to Speech!");
//                Toast.makeText(getApplicationContext(), "Error speaking test: " + TextToSpeech.ERROR, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.d("error", e.toString());
        }
        return speechStatus;
    }

}

