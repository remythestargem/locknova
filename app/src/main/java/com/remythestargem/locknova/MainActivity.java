package com.remythestargem.locknova;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(247, 246, 252);
    private final int ink = Color.rgb(38, 34, 54);
    private final int muted = Color.rgb(119, 112, 139);
    private int accent;
    private LinearLayout content;
    private String page = "Home";
    private android.content.SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("locknova", MODE_PRIVATE);
        accent = prefs.getInt("accent", Color.rgb(139, 116, 235));
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        showPage("Home");
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void pad(View v, int l, int t, int r, int b) { v.setPadding(dp(l), dp(t), dp(r), dp(b)); }

    private void showPage(String target) {
        page = target;
        LinearLayout root = column(); root.setBackgroundColor(bg); setContentView(root);
        LinearLayout header = column(); pad(header,22,22,22,12);
        header.addView(text("✦  LOCKNOVA",13,accent,true));
        TextView title = text(target.equals("Home") ? "Make your phone yours." : target,27,ink,true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1,-2); tp.topMargin=dp(9); header.addView(title,tp);
        TextView sub = text(target.equals("Home") ? "A soft, personal control centre for ColorOS." : "Personalisation tools for your Android device.",13,muted,false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1,-2); sp.topMargin=dp(5); header.addView(sub,sp); root.addView(header);
        ScrollView scroll = new ScrollView(this); content=column(); pad(content,18,8,18,20); scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if (target.equals("Home")) home(); else if (target.equals("Explore")) explore(); else if (target.equals("Backups")) settingsPage(); else settingsPage();
        LinearLayout nav=row(); nav.setBackgroundColor(Color.WHITE); pad(nav,6,8,6,8);
        String[] tabs={"Home","Explore","Backups","More"}; String[] symbols={"⌂","✧","▣","•••"};
        for(int i=0;i<tabs.length;i++){
            final String tab=tabs[i]; LinearLayout item=column(); item.setGravity(Gravity.CENTER);
            TextView ico=text(symbols[i],20,page.equals(tab)?accent:muted,true); ico.setGravity(Gravity.CENTER); item.addView(ico);
            TextView label=text(tab,10,page.equals(tab)?accent:muted,page.equals(tab)); label.setGravity(Gravity.CENTER); item.addView(label);
            item.setOnClickListener(v->showPage(tab)); nav.addView(item,new LinearLayout.LayoutParams(0,dp(48),1));
        }
        root.addView(nav);
    }
    private void section(String title,String description){
        TextView t=text(title,16,ink,true); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(17); p.bottomMargin=dp(7); content.addView(t,p);
        if(description!=null){TextView d=text(description,12,muted,false); LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,-2); q.bottomMargin=dp(10); content.addView(d,q);}
    }
    private void card(String icon,String title,String desc,Runnable action){
        LinearLayout c=row(); c.setBackground(shape(Color.WHITE,20)); pad(c,15,15,15,15);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=dp(10); content.addView(c,cp);
        TextView badge=text(icon,22,accent,true); badge.setGravity(Gravity.CENTER); badge.setBackground(shape(tint(accent,0.88f),15)); c.addView(badge,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout details=column(); pad(details,13,0,4,0); details.addView(text(title,15,ink,true));
        TextView d=text(desc,12,muted,false); LinearLayout.LayoutParams dp2=new LinearLayout.LayoutParams(-1,-2); dp2.topMargin=dp(4); details.addView(d,dp2);
        c.addView(details,new LinearLayout.LayoutParams(0,-2,1)); c.addView(text("›",25,muted,false)); c.setOnClickListener(v->action.run());
    }
    private int tint(int color,float amount){return Color.rgb((int)(Color.red(color)*amount+255*(1-amount)),(int)(Color.green(color)*amount+255*(1-amount)),(int)(Color.blue(color)*amount+255*(1-amount)));}
    private Button button(String label){Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(13); b.setTextColor(Color.WHITE); b.setBackground(shape(accent,14)); return b;}
    private void note(String title,String body){
        LinearLayout n=column(); n.setBackground(shape(Color.WHITE,18)); pad(n,15,14,15,14); n.addView(text(title,14,ink,true));
        TextView b=text(body,12,muted,false); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(6); n.addView(b,p);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2); np.bottomMargin=dp(10); content.addView(n,np);
    }
    private void home(){
        LinearLayout hero=column(); hero.setBackground(shape(tint(accent,0.20f),24)); pad(hero,20,20,20,20);
        hero.addView(text("YOUR SPACE, YOUR RULES",11,accent,true));
        TextView h=text("Small changes. A more personal phone.",21,ink,true); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.topMargin=dp(8); hero.addView(h,hp);
        TextView body=text("LockNova brings your everyday Android customisation shortcuts together in one place.",13,muted,false); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2); bp.topMargin=dp(8); hero.addView(body,bp);
        Button choose=button("Choose accent colour"); LinearLayout.LayoutParams chp=new LinearLayout.LayoutParams(-1,dp(44)); chp.topMargin=dp(15); hero.addView(choose,chp); choose.setOnClickListener(v->showPage("Appearance")); content.addView(hero);
        section("Customisation studio","Jump straight to a part of your phone.");
        card("◈","Status bar & quick settings","Open system controls and learn what can be changed",()->showPage("Status bar"));
        card("▤","Lock screen","Explore lock-screen settings and wallpaper tools",()->showPage("Lock screen"));
        card("⌂","Home screen","Home screen, launcher and layout shortcuts",()->showPage("Home screen"));
        card("↗","Gestures & navigation","Find available navigation and accessibility controls",()->showPage("Gestures"));
        card("◉","Appearance & wallpaper","Accent colour and apply a soft system wallpaper",()->showPage("Appearance"));
        section("Honest compatibility",null);
        note("ColorOS limits","Samsung Good Lock can use Samsung-specific system hooks. LockNova uses supported Android APIs and settings shortcuts; it does not pretend to rewrite protected ColorOS system UI.");
    }
    private void explore(){
        section("Explore modules","Each card is a real Android shortcut or a clearly labelled guide.");
        card("◈","Status bar & quick settings","Open Wi-Fi, display and system settings",()->showPage("Status bar"));
        card("▤","Lock screen","Wallpaper and screen timeout options",()->showPage("Lock screen"));
        card("⌂","Home screen","Home settings and launcher options",()->showPage("Home screen"));
        card("↗","Gestures & navigation","Navigation, accessibility and gesture settings",()->showPage("Gestures"));
        card("◉","Appearance & wallpaper","Apply a soft wallpaper or choose an accent",()->showPage("Appearance"));
        card("ⓘ","What LockNova can change","Read the platform limits",()->showPage("About"));
    }
    private void settingsPage(){
        switch(page){
            case "Status bar":
                section("System controls","Android does not let ordinary apps redesign ColorOS's global status bar without privileged OEM access.");
                card("⌁","Wi-Fi settings","Open the system Wi-Fi panel",()->launch(Settings.ACTION_WIFI_SETTINGS));
                card("☼","Display settings","Brightness, screen timeout and display options",()->launch(Settings.ACTION_DISPLAY_SETTINGS));
                card("▦","Notification settings","Manage app notifications in Android settings",()->launch(Settings.ACTION_APP_NOTIFICATION_SETTINGS));
                card("⌘","Developer options","Open developer settings if enabled",()->launch(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                note("Prototype boundary","Light status/navigation styling only applies while LockNova itself is open. Global icon hiding and Quick Settings redesign require ColorOS/OEM support."); break;
            case "Lock screen":
                section("Lock-screen tools","Shortcuts to settings that ColorOS exposes.");
                card("▤","Wallpaper","Apply LockNova's soft lavender wallpaper",()->applyWallpaper());
                card("◷","Display & timeout","Find screen timeout and display settings",()->launch(Settings.ACTION_DISPLAY_SETTINGS));
                card("⌑","Security & lock screen","Open device security settings",()->launch(Settings.ACTION_SECURITY_SETTINGS));
                note("Lock-screen layouts","Moving system clock widgets, replacing the lock screen, and editing always-on display need OEM APIs or a supported system extension."); break;
            case "Home screen":
                section("Home screen tools","Launchers can change the home experience, but not every ColorOS surface.");
                card("⌂","Home settings","Open default home app settings",()->launchHomeSettings());
                card("▦","Installed apps","Review apps and launchers",()->launch(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS));
                card("✧","Wallpaper","Apply a soft wallpaper",()->applyWallpaper());
                note("Deeper changes","A future launcher module can provide custom icons, grids, folders and widgets. System-wide recent-apps changes depend on ColorOS."); break;
            case "Gestures":
                section("Navigation & gestures","Open supported Android settings instead of showing fake switches.");
                card("↔","System navigation","Look for System navigation or Gestures",()->launch(Settings.ACTION_DISPLAY_SETTINGS));
                card("♿","Accessibility","Review accessibility services and shortcuts",()->launch(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                card("⌁","System controls","Open Android settings",()->launch(Settings.ACTION_SETTINGS));
                note("Back-tap and edge gestures","These are not exposed as a general-purpose API on every Oppo phone. LockNova won't claim they are active unless the device supports them."); break;
            case "Appearance":
                section("Choose your accent","Your choice is saved on this phone and updates LockNova's interface.");
                addColor("Lavender",Color.rgb(139,116,235)); addColor("Rose",Color.rgb(220,119,157)); addColor("Sky",Color.rgb(77,145,217)); addColor("Mint",Color.rgb(57,158,132)); addColor("Peach",Color.rgb(217,133,91));
                card("▧","Apply soft wallpaper","Set a lavender gradient as your device wallpaper",()->applyWallpaper()); break;
            case "Backups":
                section("Local preferences","Only LockNova's own preferences are saved; Android system settings are not backed up.");
                card("↻","Reset LockNova preferences","Restore the default lavender accent",()->{prefs.edit().clear().apply();accent=Color.rgb(139,116,235);showPage("Backups");Toast.makeText(this,"LockNova preferences reset",Toast.LENGTH_SHORT).show();});
                note("Why not restore system settings?","Android restricts third-party apps from silently changing many protected settings. This is not a full-device backup."); break;
            case "About":
                section("Built for Android","LockNova is a native Android application, not a website wrapper.");
                note("What works today","Native mobile interface, saved accent preference, system settings shortcuts, and wallpaper action using Android's WallpaperManager API.");
                note("What needs OEM access","Replacing the system status bar, editing ColorOS Quick Settings layout, moving the lock-screen clock, changing recent-apps UI, and deep system gestures.");
                note("Compatibility","Targets Android 8.0+ and is intended for Android 11 / ColorOS. Exact settings pages vary by manufacturer."); break;
            default:
                section("More","LockNova information and local preferences.");
                card("◉","Appearance","Accent colour and wallpaper",()->showPage("Appearance"));
                card("▣","Backups & reset","Reset local app preferences",()->showPage("Backups"));
                card("ⓘ","About LockNova","Capabilities and limitations",()->showPage("About"));
        }
    }
    private void addColor(String name,int color){
        LinearLayout r=row(); r.setBackground(shape(Color.WHITE,16)); pad(r,14,12,14,12);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(8); content.addView(r,p);
        View swatch=new View(this); swatch.setBackground(shape(color,50)); r.addView(swatch,new LinearLayout.LayoutParams(dp(28),dp(28)));
        TextView label=text(name,14,ink,true); pad(label,12,0,0,0); r.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        r.addView(text(color==accent?"✓ Selected":"Use",12,color,true));
        r.setOnClickListener(v->{accent=color;prefs.edit().putInt("accent",accent).apply();showPage("Appearance");});
    }
    private void launch(String action){try{startActivity(new Intent(action));}catch(Exception e){Toast.makeText(this,"This settings page isn't available on this device.",Toast.LENGTH_LONG).show();}}
    private void launchHomeSettings(){try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}catch(Exception e){launch(Settings.ACTION_SETTINGS);}}
    private void applyWallpaper(){
        try{
            int w=720,h=1280; Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); Canvas canvas=new Canvas(bitmap); Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new android.graphics.LinearGradient(0,0,w,h,new int[]{Color.rgb(245,239,255),Color.rgb(219,211,255),Color.rgb(255,237,246)},null,android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,w,h,paint); WallpaperManager.getInstance(this).setBitmap(bitmap); bitmap.recycle();
            Toast.makeText(this,"Wallpaper applied",Toast.LENGTH_SHORT).show();
        }catch(IOException|SecurityException e){Toast.makeText(this,"Could not apply wallpaper on this device.",Toast.LENGTH_LONG).show();}
    }
}
