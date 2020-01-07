package ru.mark99.music.plugin.muzofond;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

class MuzofondParser
{
    MuzofondParser() {}

    JSONArray doAnonymousSearch(String request)
    {
        // TODO: input checks

        try
        {
            // TODO: request+parsing html or service api request

            JSONArray jsonArray = new JSONArray();

            JSONObject jsonObjectTrack = new JSONObject();
            jsonObjectTrack.put("name", "(пример) Любэ - По мосту");
            jsonObjectTrack.put("time", "281");
            jsonObjectTrack.put("link", "https://dl3ca1.muzofond.fm/aHR0cDovL2YubXAzcG9pc2submV0L21wMy8wMDMvNzExLzU5Ni8zNzExNTk2Lm1wMw==");

            jsonArray.put(jsonObjectTrack.toString());

            return jsonArray;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
