package ru.mark99.music.plugin.muzofond;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class MuzofondParser
{
    private final String TAG = "MuzofondParser";
    private OkHttpClient client;

    MuzofondParser() {}

    private SharedPreferences sharedPreferences;
    private String cookieUserId = null;
    private String lastError = "";

    void init(Context context)
    {
        CookieHandler cookieHandler = new CookieManager(
                new CookieStore() {
                    @Override
                    public void add(URI uri, HttpCookie cookie) {
                        Log.d(TAG, "CookieStore add " + cookie.getName() + " = " + cookie.getValue());
                        if (cookie.getName().equals("user"))
                            setUserId(cookie.getValue());
                    }

                    @Override
                    public List<HttpCookie> get(URI uri) {
                        if (getUserId() != null)
                        {
                            Log.d(TAG, "CookieStore get cookie user = " + getUserId());
                            HttpCookie cookie = new HttpCookie("user", getUserId());
                            ArrayList<HttpCookie> httpCookies = new ArrayList<>();
                            httpCookies.add(cookie);
                            return httpCookies;
                        }
                        else
                        {
                            Log.d(TAG, "CookieStore get cookie null");
                        }
                        return new ArrayList<>();
                    }

                    @Override
                    public List<HttpCookie> getCookies() {
                        if (getUserId() != null)
                        {
                            Log.d(TAG, "CookieStore get cookie user = " + getUserId());
                            HttpCookie cookie = new HttpCookie("user", getUserId());
                            ArrayList<HttpCookie> httpCookies = new ArrayList<>();
                            httpCookies.add(cookie);
                            return httpCookies;
                        }
                        else
                        {
                            Log.d(TAG, "CookieStore get cookie null");
                        }
                        return new ArrayList<>();
                    }

                    @Override
                    public List<URI> getURIs() {
                        return null;
                    }

                    @Override
                    public boolean remove(URI uri, HttpCookie cookie) {
                        return true;
                    }

                    @Override
                    public boolean removeAll() {
                        return true;
                    }
                }, CookiePolicy.ACCEPT_ALL);
        client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieHandler))
                .build();
        sharedPreferences = context.getSharedPreferences("user-data", Context.MODE_PRIVATE);
    }

    // Main functions

    JSONArray doAnonymousSearch(String requestString)
    {
        // TODO: input checks

        try
        {
            // building request with user-agent from original google chrome 79
            Request request = new Request.Builder()
                    .url("https://muzofond.fm/search/" + requestString)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36")
                    .build();

            Response httpResponse;
            try {
                httpResponse = client.newCall(request).execute();
            }
            catch (Exception e)
            {
                Log.e(TAG, "client.newCall().execute(): " + e.getMessage());
                lastError = "client.newCall().execute(): " + e.getMessage();
                return null;
            }

            ResponseBody responseBody = httpResponse.body();
            if (responseBody == null) {
                Log.e(TAG, "responseBody == null");
                lastError = "responseBody == null";
                return null;
            }

            Document m2 = Jsoup.parse(responseBody.string());

            // get main container with tracks
            Elements container = m2.select("ul[data-type=\"tracks\"]");
            if (container.size() == 0)
            {
                Log.e(TAG, "container.size() == 0");
                lastError = "container.size() == 0";
                return null;
            }

            // get tracks elements from container
            Elements listTracks = container.select("li[class=\"item\"]");
            Log.d(TAG,"len: " + listTracks.size());

            JSONArray jsonArray = new JSONArray();

            try
            {
                for(Element element : listTracks)
                {
                    // parsing track
                    String duration = element.attr("data-duration");
                    String link = element.select("li[class=\"play\"]").attr("data-url");
                    String artist = element.select("span[class=\"artist\"]").text();
                    String track = element.select("span[class=\"track\"]").text();

                    // building object track
                    JSONObject jsonObjectTrack = new JSONObject();
                    jsonObjectTrack.put("name", artist + " - " + track);
                    jsonObjectTrack.put("time", Integer.valueOf(duration));
                    jsonObjectTrack.put("link", link);
                    jsonArray.put(jsonObjectTrack.toString());

                    //Log.d(TAG,"[" + duration + "] " + artist + " - " + track + "\t\t" + link + "\n");
                }
            }
            catch (Exception e)
            {
                Log.d(TAG,"Parsing tracks error: " + e.getMessage());
                lastError = "Parsing tracks error: " + e.getMessage();
                return null;
            }

            lastError = "";
            return jsonArray;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            lastError = "Unknown: " + e.getMessage();
            return null;
        }
    }

    // ===================================

    private String getUserId()
    {
        if (cookieUserId == null && sharedPreferences != null)
            cookieUserId = sharedPreferences.getString("cookie-user", null);
        return cookieUserId;
    }

    @SuppressLint("ApplySharedPref")
    private void setUserId(String userId)
    {
        cookieUserId = userId;

        if (sharedPreferences != null)
        {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("cookie-user", cookieUserId);
            if (!editor.commit())
                Log.w(TAG, "Error writing new cookie user id in sharedPreferences");
        }
    }

    String getLastError()
    {
        String returnedError = lastError;
        lastError = "";
        return returnedError;
    }
}
