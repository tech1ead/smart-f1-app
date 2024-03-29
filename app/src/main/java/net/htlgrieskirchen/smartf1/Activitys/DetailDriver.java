package net.htlgrieskirchen.smartf1.Activitys;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import net.htlgrieskirchen.smartf1.Beans.Constructor;
import net.htlgrieskirchen.smartf1.Beans.Driver;
import net.htlgrieskirchen.smartf1.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

public class DetailDriver extends AppCompatActivity {

    private ArrayList<Driver> arrayList = new ArrayList<>();
    private String driver;
    private String constructor;
    private ImageView imageView;
    private String url;
    private TextView tvName;
    private TextView tvBiography;
    private TextView tvFacts;
    private TextView tvSport;
    private Bitmap bitmap;
    private String picName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_driver);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        driver = intent.getStringExtra("driver");
        constructor = intent.getStringExtra("constructor");
        splitNadd();

        picName=arrayList.get(0).getGivenName()+"_"+arrayList.get(0).getFamilyName()+".jpg";
        initializeViews();
        if (!fileExist(picName)) {
            if (Connection()) {
                    ServerTask st = new ServerTask(arrayList.get(0).getUrl().substring(29));
                    st.execute();
            }else{
                if (fileExist(picName)) {
                    loadIMG();
                } else {
                    Toast.makeText(this, "Stellen Sie eine Internetverbindung her um das Fahrerbild zu sehen/downloaden!", Toast.LENGTH_LONG).show();
                }
            }
        }else{
            loadIMG();
        }
    }

    public boolean fileExist(String fileName) {
        String path = "/data/data/net.htlgrieskirchen.smartf1/app_drivers/"+picName;
        File file = new File(path);
        return file.exists();
    }
    private String calcAge(){
        int age = 0;
        try {
            SimpleDateFormat ourFormat = new SimpleDateFormat("dd.MM.yyyy");
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd");
            String reformattedStr = null;
            try {
                reformattedStr = ourFormat.format(apiFormat.parse(arrayList.get(0).getDateOfBirth()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Date birth = ourFormat.parse(reformattedStr);
            Date d = new Date();

            LocalDate birthday = birth.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate now = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            age = Period.between(birthday, now).getYears();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return String.valueOf(age);

    }
    private String formatDate(){
        SimpleDateFormat ourFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd");
        String reformattedStr = null;
        try {
            reformattedStr = ourFormat.format(apiFormat.parse(arrayList.get(0).getDateOfBirth()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return reformattedStr;
    }
    private void splitNadd(){
        String [] split = driver.split(",");
        Constructor cons = new Constructor("", "", constructor, "");
        Constructor[] constructors = {cons};
        arrayList.add(new Driver(split[0], split[1], split[2], split[3], split[4], split[5], split[6], split[7], constructors, split[9], split[10]));
    }
    public class ServerTask extends AsyncTask<String, Integer, String> {
        private final String baseURL = "https://en.wikipedia.org/w/api.php?action=query&titles=";
        private final String endURL = "&prop=pageimages&pithumbsize=300&format=json";
        private String title;

        public ServerTask(String title) {
            this.title = title;
        }
        @Override
        protected String doInBackground(String... strings) {
            String sJsonResponse = "";
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(baseURL + title + endURL).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }
                    String jsonResponse = stringBuilder.toString();
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    JSONObject query = jsonObject.getJSONObject("query");
                    JSONObject pages = query.getJSONObject("pages");
                    String id = pages.keys().next();
                    JSONObject pagesWithId = pages.getJSONObject(String.valueOf(id));
                    JSONObject thumbnail = pagesWithId.getJSONObject("thumbnail");
                    url = thumbnail.getString("source");
                    HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                    bitmap = BitmapFactory.decodeStream(con.getInputStream());
                    return jsonResponse;
                } else {
                    return "ErrorCodeFromAPI";

                }
            } catch (IOException | JSONException e) {
            }
            return sJsonResponse;
        }

        @Override
        protected void onPostExecute(String s) {
            if (url != null) {
                Picasso.with(DetailDriver.this).load(url).into(imageView);
                saveToInternalStorage(bitmap);
            }
        }
    }
        private String saveToInternalStorage(Bitmap bitmapImage) {
            ContextWrapper cw = new ContextWrapper(getApplicationContext());
            File directory = cw.getDir("drivers", Context.MODE_PRIVATE);
            File mypath = new File(directory, arrayList.get(0).getGivenName()+"_"+arrayList.get(0).getFamilyName()+".jpg");

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mypath);
                bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return directory.getAbsolutePath();
        }
    private boolean Connection() {
        boolean Wifi = false;
        boolean Mobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo NI : netInfo) {
            if (NI.getTypeName().equalsIgnoreCase("WIFI")) {
                if (NI.isConnected()) {
                    Wifi = true;
                }
            }
            if (NI.getTypeName().equalsIgnoreCase("MOBILE"))
                if (NI.isConnected()) {
                    Mobile = true;
                }
        }
        return Wifi || Mobile;
    }
    public boolean onOptionsItemSelected(MenuItem item){
        Intent myIntent = new Intent(this, MainActivity.class);
        startActivity(myIntent);
        return true;
    }
    private void initializeViews(){
         tvName = findViewById(R.id.driver);
         tvBiography = findViewById(R.id.biography);
         tvFacts = findViewById(R.id.facts);
         tvSport = findViewById(R.id.sport);
         imageView = findViewById(R.id.imageView);
         tvName.setText(arrayList.get(0).getGivenName()+" "+arrayList.get(0).getFamilyName().toUpperCase());
         tvBiography.setText("Geburtsdatum: "+formatDate()+"\n"+"Alter: "+calcAge()+"\nNationalität: "+arrayList.get(0).getNationality());
        tvSport.setText("Siege: "+arrayList.get(0).getSeasonWins()+"\nPunkte: "+arrayList.get(0).getSeasonPoints());
        String permNumber = arrayList.get(0).getPermanentNumber();
        String code = arrayList.get(0).getCode();
         if (code.equals("null")){
             if (permNumber.equals("null")){
                 tvFacts.setText("Konstrukteur: "+constructor+"\nCode: k.A.\nNummer: k.A.");
             }else{
                 tvFacts.setText("Konstrukteur: "+constructor+"\nCode: k.A.\nNummer: "+permNumber);
             }
         }else if(permNumber.equals("null")){
             tvFacts.setText("Konstrukteur: "+constructor+"\nCode: "+code+"\nNummer: k.A.");
         }else if (!permNumber.equals("null") && !code.equals("null")){
             tvFacts.setText("Konstrukteur: "+constructor+"\nCode: "+code+"\nNummer: "+permNumber);
         }
    }
    private void loadIMG(){
         Bitmap bitmap = BitmapFactory.decodeFile("/data/data/net.htlgrieskirchen.smartf1/app_drivers/"+picName);
         imageView.setImageBitmap(bitmap);
    }

}


