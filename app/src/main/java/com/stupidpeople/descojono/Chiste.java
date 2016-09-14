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
        t = t.replaceAll(" —", ", ");
        t = t.replaceAll("—", "");
        t = t.replaceAll("-", "");//TODo cambiar por expresion regular que comprueba que viene una palabra
        t = t.replaceAll("–¿", "¿");
        t = t.replaceAll("–¡", "¡");
        t = t.replaceAll("No\\.", "No . ");
        t = t.replaceAll("pie", "píe");
        t = t.replaceAll("local", "lokal");
        t = t.replaceAll("normal", " noormal");
        t = t.replaceAll("<<", "");
        t = t.replaceAll(">>", "");
        t = t.replaceAll("\\.\\.\\.\\.", "...");
        t = t.replaceAll("\\.\\.", ".");
        t = t.replaceAll(":\\.", ".");
        t = t.replaceAll("’", "");
        myLog.add("ree", "before:" + t);
        t = t.replaceAll("- |-\\w+", "$1");
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
}
