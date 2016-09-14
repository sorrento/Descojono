package com.stupidpeople.descojono;

import com.parse.FindCallback;
import com.parse.ParseQuery;

/**
 * Created by Milenko on 30/08/2016.
 */
public class parseHelper {

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


}