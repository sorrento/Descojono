package com.stupidpeople.descojono;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Milenko on 19/09/2016.
 */
public class utils {
    static final String UTTEHM = "ayayaiiii";

    /**
     * Checks if we have internet connection     *
     *
     * @return
     */
    public static boolean isOnline(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        boolean b = netInfo != null && netInfo.isConnectedOrConnecting();
        myLog.add("varios", "Checking connectivity: " + b);

        return b;
    }

    /**
     * Entrega TRUE conuna probabilidad de v
     *
     * @param v
     * @return
     */
    static boolean getProbability(double v) {
        final int i = new Random().nextInt(100 + 1);

        return i <= 100 * v;
    }

    static String getRisa() {
        return getProbability(0.33) ? getFraseDive() : UTTEHM;
    }

    static String getFraseDive() {

        final ArrayList<String> risas = new ArrayList<>();

        risas.add("Ahhh, Es que me parto.");
        risas.add("Ja ja ja, qué bueno.");
        risas.add("oj oj oj parto pecho.");
        risas.add("jaaaa jaaa me meo.");
        risas.add("En realidad no tiene gracia.");
        risas.add("Je je, es muy malo.");
        risas.add("ay, no tengo gracia, pero más que tú, dubidubidú.");
        risas.add("ja ja ¡Pero quién coño los inventa!");
        risas.add("Qué risa, Felisa.");
        risas.add("jaaa jaaa jaaa, me descojono.");
        risas.add("jojojó, me rompo.");
        risas.add("ayyy, qué salá qué soy.");
        risas.add("No tengo gracia, pero es que soy un robot.");

        final int i = new Random().nextInt(risas.size());

        return risas.get(i);
    }

    public static String getPiblicity() {
        String res;
        if (getProbability(0.4)) {
            final ArrayList<String> publi = new ArrayList<>();

            publi.add("Descarga Descojónap y serás guai como yo.");
            publi.add("Si descargas Descojónap oirás los chistes buenos, no como esta bazofia.");
            publi.add("Bájate Descojónap antes de que lo compre guguel.");
            publi.add("Descojónap gasta mucha menos batería que el poquémoh ése.");
            publi.add("Descojónap, una de las más cutres del market.");
            publi.add("Descojónap, developt bai stupidpípol. Se nota, no?");
            publi.add("Bájate Descojónap, para que se agote este despropósito.");
            publi.add("Llama ya y tendrás 10% de descuento. Ah, no, que es gratis.");
            publi.add("Si descargas Descojónap me arranco con una rumba.");
            publi.add("Bájate Descojónap, o te va a salir un virus en el móvil que hasta se te van a caer los dedos.");
            publi.add("Bájate Descojónap, antes que salga pal ífon y se acabe la fiesta.");
            publi.add("Descarga Descojónap mi niña y vamoh a echá una risa loh doh como arma que la lleva er diablo");
            publi.add("Descarga Descojónap y te cuento unos chistes al oído, guapetón...");
            publi.add("Descarga Descojónap ya, mecagoendié...");


            final int i = new Random().nextInt(publi.size());
            res = publi.get(i);
        } else {
            res = "";
        }
        return res;
    }
}
