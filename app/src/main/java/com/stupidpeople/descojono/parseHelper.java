package com.stupidpeople.descojono;

import android.content.Context;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.List;

import static com.stupidpeople.descojono.utils.isOnline;

/**
 * Created by Milenko on 30/08/2016.
 */
public class parseHelper {

    public static final String PINCENTENA = "pincentena";
    final private static String tag = "PARSE";
    private static String PINBOOK = "pinBook";


    public static void getChistes(int iChiste, int nChistes, boolean local, FindCallback<Chiste> cb) {
        final String fi = "n";

        ParseQuery<Chiste> q = ParseQuery.getQuery(Chiste.class);
        q.whereGreaterThanOrEqualTo(fi, iChiste);
        q.whereLessThanOrEqualTo(fi, iChiste + nChistes);
        q.setLimit(nChistes);
        if (local) {
            q.fromPin(PINBOOK);
//            myLog.add(tag, "leido desde LOCAL");
        } else {
//            myLog.add(tag, "leido desde WEB");
        }
        q.findInBackground(cb);
    }

//    /*private static void getAndPinChapters(final int iBook, int iChapter, int nChapters, final TaskDoneCallback2 taskDoneCallback) {
//
//        FindCallback<Chapter> cb = new FindCallback<Chapter>() {
//            @Override
//            public void done(List<Chapter> books, ParseException e) {
//                if (e == null) {
//                    myLog.add(tag, "--- Importados capitulos:" + books.size());
//
//                    SaveCallback scb = new SaveCallback() {
//                        @Override
//                        public void done(ParseException e) {
//                            if (e == null) {
//                                taskDoneCallback.onDone();
//                            } else {
//                                taskDoneCallback.onError("Pinning lot of chapters", e);
//                            }
//                        }
//                    };
//
//                    // PIN them
//                    ParseObject.pinAllInBackground(PINBOOK, books, scb);
//
//                } else {
//                    taskDoneCallback.onError("getting lot of chapters ", e);
//                }
//            }
//        };
//
//        getChistes(iBook, iChapter, nChapters, false, cb);
//    }
//*/
//    public static void getChapter(int iBook, final int iChapter, final boolean local, final ChapterCB2 chapterCB) {
//
//        ParseQuery<Chapter> q = ParseQuery.getQuery(Chapter.class);
//        q.whereEqualTo("nLibro", iBook);
//        q.whereEqualTo("nCapitulo", iChapter);
//        if (local) q.fromPin("pinBook");
//        q.getFirstInBackground(new GetCallback<Chapter>() {
//            @Override
//            public void done(Chapter chapter, ParseException e) {
//                if (e == null) {
//                    chapterCB.onDone(chapter);
//                } else {
//                    chapterCB.onError("Getting chapter " + iChapter + " from local: " + local, e);
//                }
//
//            }
//        });
//    }

    /**
     * Borramos los chistes en local, traemos 100 desde internet y los guuardamos en local
     *
     * @param centena
     * @param cb
     */
    public static void importChistes(final int centena, Context ctx, final TaskCallback cb) {

        final FindCallback<Chiste> findCallback = new FindCallback<Chiste>() {
            @Override
            public void done(List<Chiste> objects, ParseException e) {
                if (e == null) {

                    //3. Los guardamos en local
                    ParseObject.pinAllInBackground(PINCENTENA, objects, new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                cb.onDone();
                            } else {
                                cb.onError(e, "al borrar del pin los 100");
                            }
                        }
                    });

                } else {
                    cb.onError(e, "al traer los 100 chistes");
                }
            }
        };


        if (isOnline(ctx)) {
            // 1. Quitamos los que están en local
            ParseObject.unpinAllInBackground(PINCENTENA, new DeleteCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        // 2. Traemos los correspondientes 100
                        getChistes(100 * centena + 1, 100, false, findCallback);

                    } else {
                        cb.onError(e, "al borrar del pin los 100");
                    }
                }
            });

        } else {
            Toast.makeText(ctx, "Sin internet sólo me sé chistes malos...", Toast.LENGTH_SHORT).show();
        }


    }

    public static void load20chistesrandomDesdeLocal(FindCallback<Chiste> callback) {
        ParseQuery<Chiste> q = ParseQuery.getQuery(Chiste.class);
        q.whereNotEqualTo("contado", true);
        q.setLimit(20);
        q.orderByAscending("objectId");
        q.fromPin(PINCENTENA);
        q.findInBackground(callback);
    }

    /**
     * Guarda de nuevo en local, pero ponienod la etiqueta de que están leídos
     *
     * @param chistesPreLoaded
     * @param readedCB
     */
    public static void saveTheReaded(List<Chiste> chistesPreLoaded, final TaskCallback readedCB) {
        myLog.add(tag, "vamos a marcar como leidos los chistes");

        if (chistesPreLoaded == null) {
            myLog.add(tag, " no hay para marcar");
            return;
        }

        ParseObject.pinAllInBackground(PINCENTENA, chistesPreLoaded, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    readedCB.onDone();
                } else {
                    readedCB.onError(e, "on pinning as leidos");
                }

            }
        });
    }

    /**
     * trae el chiste anterior desde local, sino el mismo
     *
     * @param chisteId
     * @return
     */
    public static void getPrevChiste(int chisteId, final ChisteCallback cb) {
        ParseQuery<Chiste> q = ParseQuery.getQuery(Chiste.class);
        q.whereEqualTo("n", chisteId - 1);
        q.fromPin(PINCENTENA);
        q.getFirstInBackground(new GetCallback<Chiste>() {
            @Override
            public void done(Chiste chiste, ParseException e) {
                if (e == null) {
                    cb.OnDone(chiste);
                } else {
                    cb.onError(e);
                }

            }
        });
    }
}