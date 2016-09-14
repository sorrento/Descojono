package com.stupidpeople.descojono;

import com.parse.ParseClassName;
import com.parse.ParseObject;

/**
 * Created by halatm on 14/09/2016.
 */
@ParseClassName("libros")
public class Chiste extends ParseObject {
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
        String te = getText();
        te = te.replaceAll(" —", ", ");
        te = te.replaceAll("—", "");
        te = te.replaceAll("-", "");//TODo cambiar por expresion regular que comprueba que viene una palabra
        te = te.replaceAll("–¿", "¿");
        te = te.replaceAll("–¡", "¡");
        te = te.replaceAll("No\\.", "No . ");
        te = te.replaceAll("pie", "píe");
        te = te.replaceAll("local", "lokal");
        te = te.replaceAll("normal", " noormal");
        te = te.replaceAll("<<", "");
        te = te.replaceAll(">>", "");
        te = te.replaceAll("\\.\\.\\.\\.", "...");
        te = te.replaceAll("\\.\\.", ".");
        te = te.replaceAll(":\\.", ".");

        return te;
    }

    public int getChapterId() {
        return getInt("n");
    }
}
