package com.example.dl11.parking;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import junit.framework.Test;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {
    private String TAG = MainActivity.class.getSimpleName();
    private ProgressDialog pDialog;
    private ListView lv;
    private String coord = null;
    private String[] tableau = null;
    // URL DU JASON
    private static String url = "https://opendata.lillemetropole.fr/api/records/1.0/search/?dataset=disponibilite-parkings&facet=libelle&facet=ville&facet=etat";
    ArrayList<HashMap<String, String>> contactList;

    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        contactList = new ArrayList<>();
        this.mHandler = new Handler();
        lv = (ListView) findViewById(R.id.list);
        new GetParking().execute();
        m_Runnable.run();
    }

    // METHOD POUR RAFFRAICHIR
    public final Runnable m_Runnable = new Runnable() {
        @Override
        public void run() {

            MainActivity.this.mHandler.postDelayed(m_Runnable,5000);

        }
    };

    // Clicquer sur le TEXT MAP pour afficher la page googlmap
    public void buttonOnClick (View v){

        // Intent et split des coordones du json pour les récuperer dans le google map

        Intent gomap = new Intent(getApplicationContext(),MapsActivity.class);
        TextView temp = (TextView) v.findViewById(R.id.coordones);
        tableau = temp.getText().toString().split(",");
        tableau[0] = tableau[0].substring(1);
        tableau[1] = tableau[1].subSequence(0, tableau[1].length()-1).toString();
        gomap.putExtra("latitude", tableau[0]);
        gomap.putExtra("longitude", tableau[1]);
        startActivity(gomap);

    }

    /**
     * Async task class to get json by making HTTP call
     */
    // CLASSE ASYNTOPE
    private class GetParking extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {

                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // chercher dans le json dans le node records
                    JSONArray contacts = jsonObj.getJSONArray("records");

                    // boucle pour chercher les parkings dans les jason
                    for (int i = 0; i < contacts.length(); i++) {
                        JSONObject c = contacts.getJSONObject(i);

                        String id = c.getString("datasetid");

                        // NODE FIELDS
                        JSONObject fields = c.getJSONObject("fields");

                        String ville = fields.getString("ville");

                        // Condition la ville de roubaix on met dans la liste
                        if ( fields.getString("ville").equals("Roubaix"))
                        {
                            String adresse = fields.getString("adresse");
                            String libelle = fields.getString("libelle");
                            coord =fields.getString("coordgeo");

                            HashMap<String, String> contact = new HashMap<>();
                            // on insere les donnés JSON
                            contact.put("datasetid", id);
                            contact.put("ville", ville);
                            contact.put("adresse", adresse);
                            contact.put("libelle", libelle);
                            contact.put("coordgeo", coord);
                            contact.put("dispo","indisponible");
                            contact.put("etat", "indisponible");
                            contactList.add(contact);
                        }else
                        {
                            String dispo = fields.getString("dispo");
                            String etat = fields.getString("etat");
                            String adresse = fields.getString("adresse");
                            String libelle = fields.getString("libelle");
                            coord =fields.getString("coordgeo");
                            HashMap<String, String> contact = new HashMap<>();
                            // adding each child node to HashMap key => value
                            contact.put("datasetid", id);
                            contact.put("ville", ville);
                            contact.put("adresse", adresse);
                            contact.put("libelle", libelle);
                            contact.put("coordgeo", coord);
                            contact.put("etat", etat);
                            contact.put("dispo", dispo);
                            contactList.add(contact);
                        }
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });
            }
            //FONCTION POUR TRIER LA LIST DANS L'ORDRE APR 0 L'ETAT OUVERT FERMER
            Collections.sort(contactList, new Comparator<HashMap>() {
                @Override
                public int compare(HashMap o1, HashMap o2) {
                    String etat1 = "";
                    String etat2 = "";
                    if(o1.get("etat").toString() == "indisponible")
                    {
                        etat1 = "FERMER";
                    }
                    else
                    {
                        etat1 = o1.get("etat").toString();
                    }
                    if (o2.get("etat").toString() =="indisponible")
                    {
                        etat1 ="FEMER";
                    }
                    else
                    {
                        etat2 = o2.get("etat").toString();
                    }
                    return etat2.compareTo(etat1 );
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            ListAdapter adapter = new SimpleAdapter(
                    MainActivity.this, contactList,
                    R.layout.list_item, new String[]{"ville", "libelle", "adresse", "etat", "dispo", "coordgeo"}, new int[]{R.id.ville,
                    R.id.libelle, R.id.adresse, R.id.etat, R.id.dispo, R.id.coordones}){

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView text1 = (TextView) view.findViewById(R.id.etat);
            switch (text1.getText().toString()) {
                case "OUVERT":
                    text1.setBackgroundColor(Color.GREEN);
                    break;
                default:
                    text1.setBackgroundColor(Color.RED);
                    break;
            }
            return view;
        }
    };

            lv.setAdapter(adapter);
        }
    }
}