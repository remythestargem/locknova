package com.remythestargem.locknova;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(246,247,253), ink = Color.rgb(35,42,75), muted = Color.rgb(119,126,153);
    private int accent;
    private LinearLayout root, body;
    private String current = "Home";
    private android.content.SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("locknova", MODE_PRIVATE);
        accent = prefs.getInt("accent", Color.rgb(139,116,245));
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        show("Home");
    }
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, 1); t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private void pad(View v, int l, int t, int r, int b) { v.setPadding(dp(l),dp(t),dp(r),dp(b)); }
    private LinearLayout col() { LinearLayout l=new LinearLayout(this); l.setOrientation(1); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setOrientation(0); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void show(String page) {
        current=page; root=col(); root.setBackgroundColor(bg); setContentView(root);
        LinearLayout top=row(); pad(top,20,12,20,8);
        if (!page.equals("Home")) { TextView back=label("‹",34,ink,false); top.addView(back,new LinearLayout.LayoutParams(dp(36),dp(42))); back.setOnClickListener(v->show("Home")); }
        LinearLayout titles=col(); titles.addView(label(page.equals("Home")?"LockNova":page,22,ink,true));
        if(page.equals("Home")) titles.addView(label("Your phone, your rules.",12,muted,false));
        top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView dots=label("⚙",23,ink,false); dots.setGravity(Gravity.CENTER); top.addView(dots,new LinearLayout.LayoutParams(dp(36),dp(42)));
        dots.setOnClickListener(v->show("More")); root.addView(top);
        ScrollView scroll=new ScrollView(this); body=col(); pad(body,16,10,16,18); scroll.setFillViewport(true); scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(page.equals("Home")) home(); else if(page.equals("Status Bar")) status(); else if(page.equals("Quick Panel")) quick(); else if(page.equals("Lock Screen")) lock(); else if(page.equals("Home Screen")) homescreen(); else if(page.equals("Gestures")) gestures(); else if(page.equals("System UI")) systemUi(); else if(page.equals("Appearance")) appearance(); else if(page.equals("Explore")) explore(); else if(page.equals("Backups")) backups(); else more();
        LinearLayout nav=row(); nav.setBackgroundColor(Color.WHITE); pad(nav,5,8,5,8);
        String[] tabs={"Home","Explore","Backups","More"}; String[] icons={"⌂","◉","♧","•••"};
        for(int i=0;i<tabs.length;i++){ final String tab=tabs[i]; LinearLayout item=col(); item.setGravity(Gravity.CENTER); int c=(page.equals(tab)||(page.equals("Home")&&tab.equals("Home")))?accent:muted; item.addView(label(icons[i],20,c,true)); TextView tx=label(tab,10,c,true); tx.setGravity(Gravity.CENTER); item.addView(tx); item.setOnClickListener(v->show(tab)); nav.addView(item,new LinearLayout.LayoutParams(0,dp(48),1)); }
        root.addView(nav);
    }
    private void section(String s) { TextView t=label(s,15,ink,true); pad(t,2,14,2,8); body.addView(t); }
    private void note(String title,String desc) { LinearLayout c=col(); c.setBackground(shape(Color.WHITE,18,0xFFE4E7F8)); pad(c,15,13,15,13); c.addView(label(title,14,ink,true)); TextView d=label(desc,12,muted,false); d.setGravity(Gravity.TOP); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(5); c.addView(d,p); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=dp(10); body.addView(c,cp); }
    private void card(String icon,String title,String desc,String page) {
        LinearLayout r=row(); r.setBackground(shape(Color.WHITE,18,0xFFE5E8F7)); pad(r,12,12,12,12);
        TextView badge=label(icon,22,accent,true); badge.setGravity(Gravity.CENTER); badge.setBackground(shape(0xFFF0EDFF,14,0)); r.addView(badge,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout text=col(); pad(text,12,0,0,0); text.addView(label(title,14,ink,true)); TextView sub=label(desc,11,muted,false); sub.setMaxLines(2); text.addView(sub);
        r.addView(text,new LinearLayout.LayoutParams(0,-2,1)); r.addView(label("›",24,muted,false));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(9); body.addView(r,p); r.setOnClickListener(v->show(page));
    }
    private void home() {
        LinearLayout hero=col(); hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFFDDE8FF,0xFFF4E6FF,0xFFD8E9FF})); ((GradientDrawable)hero.getBackground()).setCornerRadius(dp(22)); pad(hero,20,20,20,20);
        hero.addView(label("✦  LOCKNOVA STUDIO",11,accent,true)); TextView h=label("Your phone,\nyour style.",28,ink,true); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.topMargin=dp(10); hero.addView(h,hp);
        hero.addView(label("A softer, more personal ColorOS experience.",13,muted,false)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(16); body.addView(hero,p);
        section("Customisation studio");
        card("◉","Status Bar","Style preview and system status settings","Status Bar");
        card("☷","Quick Panel","Tile layout ideas, colors and shortcuts","Quick Panel");
        card("▣","Lock Screen","Clock styles, widgets and wallpaper","Lock Screen");
        card("⌂","Home Screen","Icon styles, grid and launcher settings","Home Screen");
        card("☝","Gestures","Find navigation and gesture controls","Gestures");
        card("✳","System UI","Notification and media preferences","System UI");
        note("Made for your phone","LockNova's lavender UI and previews work in the app. Protected ColorOS system surfaces may still need OEM support.");
    }
    private void status() {
        section("Style"); note("Compact Dynamic Island · Preview","A visual concept only. A normal app cannot replace the real Oppo status bar.");
        LinearLayout preview=row(); preview.setBackground(shape(0xFFE9EDFA,14,0)); pad(preview,12,14,12,14); preview.addView(label("19:42",12,ink,true)); View spacer=new View(this); preview.addView(spacer,new LinearLayout.LayoutParams(0,1,1)); TextView island=label("  ●  ",12,Color.WHITE,true); island.setBackground(shape(Color.BLACK,20,0)); preview.addView(island); preview.addView(label("  ▴  92%",11,ink,true)); body.addView(preview);
        section("Appearance"); choice("Island position","Left · Center · Right"); choice("Size","Small · Medium · Large"); choice("Animation style","Smooth · Sharp · None");
        section("Show icons"); note("Clock and battery","These are preview controls. Real system icon visibility is controlled by ColorOS.");
        card("⚙","Open status bar settings","Use the phone's supported settings","System UI");
    }
    private void quick() {
        section("Quick panel preview"); LinearLayout panel=col(); panel.setBackground(shape(Color.WHITE,22,0xFFE3E6F7)); pad(panel,14,14,14,14);
        LinearLayout r=row(); r.addView(tile("◉","Wi-Fi","On",true),new LinearLayout.LayoutParams(0,dp(72),1)); View gap=new View(this); r.addView(gap,new LinearLayout.LayoutParams(dp(8),1)); r.addView(tile("ᛒ","Bluetooth","Off",false),new LinearLayout.LayoutParams(0,dp(72),1)); panel.addView(r);
        LinearLayout r2=row(); String[] a={"◖","◉","♧","✣","⌖"}; for(String s:a){TextView t=label(s,18,accent,true);t.setGravity(Gravity.CENTER);t.setBackground(shape(0xFFF0EDFF,30,0));LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,dp(44),1);q.topMargin=dp(12);q.rightMargin=dp(4);r2.addView(t,q);} panel.addView(r2);
        SeekBar sb=new SeekBar(this); sb.setMax(100); sb.setProgress(68); sb.setProgressTintList(android.content.res.ColorStateList.valueOf(accent)); panel.addView(sb); body.addView(panel);
        section("Layout"); choice("Grid style","3 × 4"); choice("Tile size","Medium"); card("▣","Edit tiles in ColorOS","Open the system panel settings where available","System UI");
        section("Style"); choice("Background","Blur · Solid · Transparent"); note("Preview only","LockNova cannot rewrite Oppo's actual Quick Settings panel without privileged system access.");
    }
    private LinearLayout tile(String icon,String title,String state,boolean selected) { LinearLayout t=col(); t.setGravity(Gravity.CENTER); t.setBackground(shape(selected?accent:0xFFF0F2FB,14,0)); t.addView(label(icon,18,selected?Color.WHITE:ink,true)); TextView tx=label(title,11,selected?Color.WHITE:ink,true);tx.setGravity(Gravity.CENTER);t.addView(tx);TextView st=label(state,10,selected?Color.WHITE:muted,false);st.setGravity(Gravity.CENTER);t.addView(st);return t; }
    private void choice(String title,String options) { LinearLayout r=row(); r.setBackground(shape(Color.WHITE,14,0xFFE5E8F7)); pad(r,14,13,12,13); LinearLayout c=col(); c.addView(label(title,13,ink,true)); c.addView(label(options,11,muted,false)); r.addView(c,new LinearLayout.LayoutParams(0,-2,1)); r.addView(label("›",21,accent,false)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);body.addView(r,p); }
    private void lock() { section("Lock screen preview"); note("19:42   ·   Tuesday","Wallpaper and clock concept preview; actual lock-screen layout remains ColorOS-controlled."); section("Widgets"); choice("Music","On · Off"); choice("Weather","On · Off"); choice("Calendar","On · Off"); choice("Steps","On · Off"); card("▧","Apply lavender wallpaper","Sets a generated pastel gradient wallpaper","Appearance"); card("⚙","Display and timeout","Open display settings","System UI"); }
    private void homescreen() { section("Layout"); choice("Grid","Default · 4 × 6 · 5 × 6"); choice("App icon style","Default · Rounded · Circular"); section("Sizing"); TextView t=label("Icon size",13,ink,true);body.addView(t); SeekBar b=new SeekBar(this);b.setMax(100);b.setProgress(45);b.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));body.addView(b); choice("App labels","Show · Hide (launcher dependent)"); choice("Grid spacing","Compact · Normal · Wide"); card("⌂","Open home settings","Change launcher and home-screen options","System UI"); note("For deeper home customisation","A dedicated launcher can provide more control over icon grids, folders and widgets than a regular settings app.");
    }
    private void gestures() { section("Swipe gestures"); choice("Swipe up from bottom","Home (system setting)"); choice("Swipe up and hold","Recent apps"); choice("Swipe from left edge","Back"); choice("Swipe from right edge","Back"); section("Shortcuts"); choice("Double tap","Device dependent"); card("☝","Open navigation settings","Configure available system navigation options","System UI"); note("No fake toggles","These options explain common gesture patterns; they do not silently change protected system gestures.");
    }
    private void systemUi() { section("System shortcuts"); card("◉","Wi-Fi settings","Open Android Wi-Fi settings","__wifi"); card("☼","Display settings","Brightness and screen timeout","__display"); card("♧","Notification settings","Manage app notifications","__notifications"); card("♙","Security settings","Lock screen and device security","__security"); card("☝","Accessibility settings","Accessibility controls and services","__accessibility"); note("System UI limits","Oppo controls the actual status bar, Quick Panel and lock screen. Android's public wallpaper API can change wallpaper, but it does not grant permission to redesign those system surfaces.");
    }
    private void appearance() { section("Choose your accent"); addColor("Lavender",Color.rgb(139,116,245)); addColor("Rose",Color.rgb(220,119,157)); addColor("Sky",Color.rgb(77,145,217)); addColor("Mint",Color.rgb(57,158,132)); addColor("Peach",Color.rgb(217,133,91)); section("Wallpaper studio"); card("✦","Apply pastel wallpaper","Generate and set a soft gradient wallpaper","__wallpaper"); }
    private void addColor(String name,int color) { LinearLayout r=row();r.setBackground(shape(Color.WHITE,15,0xFFE4E7F8));pad(r,14,12,14,12);View sw=new View(this);sw.setBackground(shape(color,24,0));r.addView(sw,new LinearLayout.LayoutParams(dp(30),dp(30)));TextView tx=label(name,14,ink,true);pad(tx,12,0,0,0);r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));r.addView(label(accent==color?"✓":"›",20,accent,false));r.setOnClickListener(v->{accent=color;prefs.edit().putInt("accent",color).apply();show("Appearance");});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);body.addView(r,p); }
    private void explore() { section("Explore modules"); card("◉","Status Bar","Preview and open supported settings","Status Bar");card("☷","Quick Panel","Tile layout and appearance concepts","Quick Panel");card("▣","Lock Screen","Clock and wallpaper tools","Lock Screen");card("⌂","Home Screen","Launcher and grid options","Home Screen");card("☝","Gestures","Navigation and accessibility settings","Gestures"); }
    private void backups() { section("Local preferences"); note("Saved on this phone","LockNova stores your accent colour locally. It does not back up every Android or ColorOS system setting."); card("↻","Reset accent","Restore the default lavender accent","__reset"); }
    private void more() { section("LockNova");card("✦","Appearance","Accent colors and wallpaper","Appearance");card("↻","Backups & reset","Local app preferences","Backups");card("ⓘ","About LockNova","What works and what needs system access","About");note("Built for Android 8.0+","Native Android app. No root required for the features currently implemented; protected system modifications are not advertised as working."); }
    private void applyWallpaper() { try { int w=720,h=1280;Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint(3);p.setShader(new LinearGradient(0,0,w,h,new int[]{0xFFE8E9FF,0xFFF7E5F5,0xFFD9E9FF},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);WallpaperManager.getInstance(this).setBitmap(b);b.recycle();Toast.makeText(this,"Wallpaper applied",Toast.LENGTH_SHORT).show(); } catch(Exception e){Toast.makeText(this,"Could not apply wallpaper on this device.",Toast.LENGTH_LONG).show();} }
    private void open(String action) { try { if(action.equals("__wallpaper")){applyWallpaper();return;} if(action.equals("__reset")){prefs.edit().clear().apply();accent=Color.rgb(139,116,245);show("Backups");return;} if(action.equals("About")){show("More");return;} String a=action; if(action.equals("__wifi"))a=Settings.ACTION_WIFI_SETTINGS;else if(action.equals("__display"))a=Settings.ACTION_DISPLAY_SETTINGS;else if(action.equals("__notifications"))a=Settings.ACTION_APP_NOTIFICATION_SETTINGS;else if(action.equals("__security"))a=Settings.ACTION_SECURITY_SETTINGS;else if(action.equals("__accessibility"))a=Settings.ACTION_ACCESSIBILITY_SETTINGS; startActivity(new Intent(a)); }catch(Exception e){Toast.makeText(this,"This setting isn't available on this device.",Toast.LENGTH_LONG).show();} }
}
