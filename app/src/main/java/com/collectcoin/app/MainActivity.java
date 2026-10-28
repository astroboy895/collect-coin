package com.collectcoin.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private final ArrayList<JSONObject> coins = new ArrayList<>();
    private LinearLayout listContainer;
    private TextView countView;
    private static final String PREFS = "collect_coin_data";
    private static final String KEY = "coins";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadCoins();
        buildScreen();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density); }

    private TextView label(String text, int size) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextSize(size); v.setTextColor(Color.rgb(30, 42, 58));
        v.setPadding(dp(4), dp(7), dp(4), dp(7));
        return v;
    }

    private Button button(String text) {
        Button b = new Button(this); b.setText(text); return b;
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(8));
        root.setBackgroundColor(Color.rgb(245, 247, 250));

        TextView title = label("🪙 Collect Coin", 27);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.rgb(25, 48, 78));
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = label("Catálogo personal de monedas", 14);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button add = button("+ Agregar moneda");
        add.setOnClickListener(v -> showAddDialog());
        Button report = button("Imprimir catálogo");
        report.setOnClickListener(v -> printCatalog());
        actions.addView(add, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(report, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(actions);

        countView = label("", 15);
        root.addView(countView);

        ScrollView scroll = new ScrollView(this);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView foot = label("Los datos se guardan en este teléfono. Use «Imprimir ficha» para crear una ficha individual en PDF o enviarla a una impresora.", 12);
        root.addView(foot);
        setContentView(root);
        refreshList();
    }

    private void showAddDialog() {
        ScrollView sc = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setPadding(dp(18), dp(8), dp(18), dp(8));
        form.setOrientation(LinearLayout.VERTICAL);
        sc.addView(form);

        EditText country = field(form, "País (ej. Argentina)");
        EditText denomination = field(form, "Denominación (ej. 1 peso)");
        EditText year = field(form, "Año de emisión");
        year.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText material = field(form, "Material / composición");
        EditText weight = field(form, "Peso en gramos");
        EditText diameter = field(form, "Diámetro en milímetros");
        EditText condition = field(form, "Conservación (ej. MBC, EBC, SC)");
        EditText value = field(form, "Valor estimado y moneda");
        EditText catalog = field(form, "Número de catálogo");
        EditText notes = field(form, "Observaciones");

        new AlertDialog.Builder(this).setTitle("Nueva moneda").setView(sc)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", (d, w) -> {
                try {
                    JSONObject c = new JSONObject();
                    c.put("country", country.getText().toString().trim());
                    c.put("denomination", denomination.getText().toString().trim());
                    c.put("year", year.getText().toString().trim());
                    c.put("material", material.getText().toString().trim());
                    c.put("weight", weight.getText().toString().trim());
                    c.put("diameter", diameter.getText().toString().trim());
                    c.put("condition", condition.getText().toString().trim());
                    c.put("value", value.getText().toString().trim());
                    c.put("catalog", catalog.getText().toString().trim());
                    c.put("notes", notes.getText().toString().trim());
                    if (c.getString("country").isEmpty() || c.getString("denomination").isEmpty()) {
                        Toast.makeText(this, "Completa al menos país y denominación.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    coins.add(c); saveCoins(); refreshList();
                    Toast.makeText(this, "Moneda guardada.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "No se pudo guardar la moneda.", Toast.LENGTH_SHORT).show();
                }
            }).show();
    }

    private EditText field(LinearLayout parent, String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true); e.setHint(hint);
        parent.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    private void refreshList() {
        if (listContainer == null) return;
        listContainer.removeAllViews();
        countView.setText("Monedas registradas: " + coins.size());
        if (coins.isEmpty()) {
            TextView empty = label("Todavía no hay monedas. Pulsa «Agregar moneda» para comenzar.", 16);
            listContainer.addView(empty);
            return;
        }
        for (int i = coins.size() - 1; i >= 0; i--) {
            final JSONObject coin = coins.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(7), dp(12), dp(7));
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.setMargins(0, dp(4), 0, dp(4));
            TextView name = label(coin.optString("denomination") + " · " + coin.optString("country") + " · " + coin.optString("year"), 17);
            card.addView(name);
            LinearLayout row = new LinearLayout(this);
            Button details = button("Ver ficha");
            details.setOnClickListener(v -> showDetails(coin));
            Button print = button("Imprimir ficha");
            print.setOnClickListener(v -> printCoin(coin));
            Button delete = button("Eliminar");
            delete.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("¿Eliminar esta moneda?")
                .setNegativeButton("Cancelar", null).setPositiveButton("Eliminar", (dd, ww) -> {
                    coins.remove(coin); saveCoins(); refreshList();
                }).show());
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(print, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(delete, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(row);
            listContainer.addView(card, cp);
        }
    }

    private String htmlFor(JSONObject c, boolean all) {
        StringBuilder s = new StringBuilder("<html><head><meta charset='utf-8'><style>body{font-family:Arial,sans-serif;color:#1e2a3a;padding:22px}h1{color:#18395f;border-bottom:2px solid #b99548;padding-bottom:8px}table{border-collapse:collapse;width:100%}td{border:1px solid #ccd2da;padding:9px;vertical-align:top}td:first-child{font-weight:bold;width:34%;background:#f1f4f7}.coin{page-break-after:always}</style></head><body>");
        if (all) {
            s.append("<h1>Collect Coin — Catálogo de monedas</h1><p>Total de monedas: ").append(coins.size()).append("</p>");
            for (JSONObject item : coins) appendCoin(s, item);
        } else appendCoin(s, c);
        return s.append("</body></html>").toString();
    }

    private void appendCoin(StringBuilder s, JSONObject c) {
        s.append("<section class='coin'><h1>Collect Coin — Ficha numismática</h1><table>");
        addRow(s,"País",c.optString("country")); addRow(s,"Denominación",c.optString("denomination"));
        addRow(s,"Año de emisión",c.optString("year")); addRow(s,"Material / composición",c.optString("material"));
        addRow(s,"Peso",c.optString("weight").isEmpty()?"":" "+c.optString("weight")+" g");
        addRow(s,"Diámetro",c.optString("diameter").isEmpty()?"":" "+c.optString("diameter")+" mm");
        addRow(s,"Conservación",c.optString("condition")); addRow(s,"Valor estimado",c.optString("value"));
        addRow(s,"Número de catálogo",c.optString("catalog")); addRow(s,"Observaciones",c.optString("notes"));
        s.append("</table><p style='font-size:10px;color:#666'>Ficha generada por Collect Coin. Los valores y datos son los ingresados por el coleccionista.</p></section>");
    }
    private void addRow(StringBuilder s, String k, String v) {
        s.append("<tr><td>").append(escape(k)).append("</td><td>").append(escape(v)).append("</td></tr>");
    }
    private String escape(String x) {
        return x.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    private void showDetails(JSONObject c) {
        StringBuilder msg = new StringBuilder();
        String[] keys = {"country","denomination","year","material","weight","diameter","condition","value","catalog","notes"};
        String[] labels = {"País","Denominación","Año","Material","Peso (g)","Diámetro (mm)","Conservación","Valor estimado","N.º de catálogo","Observaciones"};
        for (int i=0;i<keys.length;i++) msg.append(labels[i]).append(": ").append(c.optString(keys[i])).append("\n");
        new AlertDialog.Builder(this).setTitle("Ficha de la moneda").setMessage(msg.toString())
            .setPositiveButton("Cerrar", null).setNeutralButton("Imprimir ficha", (d,w)->printCoin(c)).show();
    }

    private void printCoin(JSONObject c) { startPrint("CollectCoin_Ficha", htmlFor(c, false)); }
    private void printCatalog() {
        if (coins.isEmpty()) {
            Toast.makeText(this, "Agrega al menos una moneda antes de imprimir.", Toast.LENGTH_SHORT).show();
            return;
        }
        startPrint("CollectCoin_Catalogo", htmlFor(null, true));
    }
    private void startPrint(String name, String html) {
        WebView web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                PrintManager pm = (PrintManager)getSystemService(Context.PRINT_SERVICE);
                pm.print(name, view.createPrintDocumentAdapter(name), new PrintAttributes.Builder().build());
            }
        });
        web.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);
    }

    private void saveCoins() {
        JSONArray a = new JSONArray();
        for (JSONObject c : coins) a.put(c);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
    }
    private void loadCoins() {
        try {
            JSONArray a = new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, "[]"));
            for (int i=0;i<a.length();i++) coins.add(a.getJSONObject(i));
        } catch (Exception ignored) { }
    }
}
