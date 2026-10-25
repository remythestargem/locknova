package app.locknova.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EventLog {
    private static final String FILE = "locknova_activity";
    private static final String KEY_EVENTS = "events";

    public static final class Event {
        public final long timestamp;
        public final String category;
        public final String title;
        public final String detail;
        Event(long timestamp, String category, String title, String detail) {
            this.timestamp = timestamp;
            this.category = category;
            this.title = title;
            this.detail = detail;
        }
    }

    private EventLog() { }
    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static synchronized void add(Context context, String category, String title, String detail) {
        JSONArray old = readArray(context);
        JSONArray next = new JSONArray();
        try {
            next.put(new JSONObject().put("time", System.currentTimeMillis()).put("category", category).put("title", title).put("detail", detail));
            for (int i = 0; i < old.length() && i < 199; i++) next.put(old.getJSONObject(i));
        } catch (JSONException ignored) { }
        prefs(context).edit().putString(KEY_EVENTS, next.toString()).apply();
    }

    public static synchronized List<Event> list(Context context) {
        JSONArray array = readArray(context);
        ArrayList<Event> events = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object != null) events.add(new Event(object.optLong("time"), object.optString("category", "info"), object.optString("title", "Activity"), object.optString("detail", "")));
        }
        return Collections.unmodifiableList(events);
    }

    public static synchronized void clear(Context context) { prefs(context).edit().remove(KEY_EVENTS).apply(); }

    private static JSONArray readArray(Context context) {
        try { return new JSONArray(prefs(context).getString(KEY_EVENTS, "[]")); }
        catch (JSONException ignored) { return new JSONArray(); }
    }
}
