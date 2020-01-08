package ru.mark99.music.plugin.muzofond;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;

import java.util.ArrayList;

public class MuzofondPlugin extends Service
{
    final String TAG = "MuzofondPlugin";
    final String managerPackageName = "ru.mark99.music.plugin.manager";
    ArrayList<String> myMethods;

    MuzofondParser muzofondParser;

    public MuzofondPlugin() {}

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        muzofondParser = new MuzofondParser();
        muzofondParser.init(this);

        // Put support methods
        myMethods = new ArrayList<>();
        myMethods.add("getSummaryPluginInfo");
        myMethods.add("doAnonymousSearch");

        registerReceiver(receiver, new IntentFilter(getPackageName()));
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        unregisterReceiver(receiver);
        super.onDestroy();
    }


    void onReceive(String msg, Intent request)
    {
        switch (msg)
        {
            case "getSummaryPluginInfo":
            {
                Intent answer = new Intent()
                        .putExtra("name", "Muzofond")
                        .putExtra("desc", "Plugin for integration MusicApp with Muzofond")
                        .putExtra("methods", myMethods )
                        .putExtra("version", getAppVersion());

                sendToManager(msg, answer, request);
                break;
            }

            case "doAnonymousSearch":
            {
                String requestString = request.getStringExtra("requestString");
                JSONArray resultSearch = muzofondParser.doAnonymousSearch(requestString);

                Intent answer = new Intent();

                if (resultSearch != null)
                {
                    answer.putExtra("status", "successfully");
                    answer.putExtra("tracks", resultSearch.toString());
                }
                else
                {
                    answer.putExtra("status", "error");
                    answer.putExtra("errorString", muzofondParser.getLastError());
                }

                sendToManager(msg, answer, request);
                break;
            }
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent)
        {
            String from = intent.getStringExtra("_from_");
            if (from == null || !from.equals(managerPackageName))
            {
                Log.w(TAG, "onReceive incorrect _from_: " + from);
                return;
            }

            String msg = intent.getStringExtra("msg");
            if (msg == null)
            {
                Log.w(TAG, "onReceive msg == null");
                return;
            }

            Log.d(TAG, "onReceive from: " + from + "\tmsg: " + msg +
                    "\tid: " + intent.getIntExtra("_id_", -1));

            new Thread(() -> MuzofondPlugin.this.onReceive(msg, intent)).start();

        }
    };

    void sendToManager(String msg, Intent answer, Intent request)
    {
        Log.d(TAG, "Sending response to manager: " + msg +
                "\tid: " + request.getIntExtra("_id_", -1) +
                "\tstatus: " + answer.getStringExtra("status"));

        int id = request.getIntExtra("_id_", -1);

        answer.setAction(managerPackageName);
        answer.putExtra("_from_", getPackageName());
        if (id != -1)
            answer.putExtra("_id_", id);
        answer.putExtra("msg", msg);
        this.sendBroadcast(answer);
    }

    String getAppVersion()
    {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        }
        catch (Exception ignored) {
            return "0.0";
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return new Binder(); }
}
