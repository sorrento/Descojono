package com.stupidpeople.descojono;

import com.parse.ParseClassName;
import com.parse.ParseObject;

import java.util.Arrays;

/**
 * Created by halatm on 14/09/2016.
 */
@ParseClassName("chistes")
public class Chiste extends ParseObject {
    static String processForReading(String t) {
        myLog.add("ree", "before:" + t);

        t = t.replaceAll("^(-|—|–) ?(¿|\\w+¡|)", "$2"); //al inicio con guion
        t = t.replaceAll("\\n ?(-|—|–) ?(¿|\\w+¡|)", "\n$2"); //principio de linea con guion
        t = t.replaceAll("¡", "");
        t = t.replaceAll("(\\w+)\\n", "$1.\\n"); //punto al final de la línea

        t = t.replaceAll("No\\.", "No . ");
        t = t.replaceAll("no\\.", "No . ");
        t = t.replaceAll("pie", "píe");
        t = t.replaceAll("local", "lokal");
        t = t.replaceAll("normal", " noormal");

        t = t.replaceAll("<<", "");
        t = t.replaceAll(">>", "");
        t = t.replaceAll("\\.\\.\\.\\.", "...");
        t = t.replaceAll("\\.\\.", ".");
        t = t.replaceAll(":\\.", ".");
        t = t.replaceAll("’", "");

        t = t.replaceAll("Patxi", "Páchi");



        myLog.add("ree", "   after:" + t);

        return t;
    }

    public static String joinVersos(String[] versos, String sep) {

        StringBuilder builder = new StringBuilder();

        for (String s : Arrays.asList(versos)) {
            myLog.add("div", "agregando: " + s);
            builder.append(s).append(sep);
        }

        final String s2 = builder.toString();
        myLog.add("div", "afeter pegar de nuevo, queda:_" + s2);
        return s2;

    }

    public String getType() {
        return getString("tipo");
    }

    public String getText() {
        return getString("texto");
    }

    public String firstLine() {
        String[] parts = getText().split("\n");
        return parts[0];
    }

    /**
     * Text prepared to be read, for instance, removing "guiones"
     *
     * @return
     */
    public String getProcessedText() {
        return processForReading(getTextForAudio());
    }

    String getTextForAudio() {
        final String sonido = getString("sonido");
        String r;

        if (getType().toLowerCase().startsWith("nivel") && !sonido.equals("ok")) {
            r = sonido;
        } else {
            r = getText();
        }
        return r;
    }

    public int getChisteId() {
        return getInt("n");
    }

    public String getFormatedText() {
        String[] versos = getText().split("\n");

        return "_" + Chiste.joinVersos(versos, "_\n_");
    }

    public String NotifTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(getChisteId() + "/");

        final String title = getTitle();

        if (title != null && title != "") {
            sb.append(title);

        } else {
            final String type = getType();

            if (type != null && type != "") {
                sb.append(type);
            } else {
                sb.append(firstLine());
            }
        }

        return sb.toString();
    }

    private String getTitle() {
        return getString("titulo");
    }

    public String NotifSubTitle() {
        return getType();
    }

    public String NotifSubText() {
        return firstLine();
    }

    public Boolean getContado() {
        return getBoolean("contado");
    }

    public void setContado(boolean b) {
        put("contado", b);
    }
}
