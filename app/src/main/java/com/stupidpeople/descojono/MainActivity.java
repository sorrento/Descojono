package com.stupidpeople.descojono;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
//    private static final String PREFS_LAST_CHAPTER = "last chapter";
//    private static final String PREFS_FIRST_TIME = "first time";

    private static final String COURTESY = "courtesy";
    private static final String LIKE = "like";
    private static final String PLAYPAUSE = "playpause";
    private static final String NEXT = "next";
    private static final String STOP = "stop";
    private static final String SHARETEXT = "shareText";
    private static final String SHAREAUDIO = "shareAudio";
    private static final String UTTEHM = "ayayayyy";
    private static final String UTTRISA = "risa";

    final private String samsungEngine = "com.samsung.SMT";
    TextToSpeech t1;

    private String tag = "mhp";
    private String tagW = "WAS";
    private String tag2 = "ACT";

    private List<Chiste> chistesPreLoaded;

    private boolean interrupted = false;
    private int iBuffer = 0;
    private int nextQueueMode;
    private NotificationManager mNotificationManager;
    private BroadcastReceiver eventsReceiver;

    private Chiste currentChiste;
    private boolean isShowingLyricsNotification = false;
    private String destFileName;
    private String uttsavingFile = "savingFile";


    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(eventsReceiver);
//        settings.edit().putBoolean(PREFS_MODE, entireBookMode).commit();
//        settings.edit().putBoolean(PREFS_MODE_MUSIC, musicMode).commit();

        removeLyricNotification();
        t1.shutdown();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myLog.initialize("LOG_DESCO");

        destFileName = (Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/Download") + "/" + "tts_file.wav";//TODO remove audio file


        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                myLog.add(tag, "status=" + status);

                if (status != TextToSpeech.ERROR) {

                    String engine = getBestEngine();

                    t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            if (status != TextToSpeech.ERROR) {
                                setSpeakLanguage("ES", t1);

                                t1.setOnUtteranceProgressListener(new uListener());

                                getRandomChistesAndPlay(10);

                            }
                        }
                    }, engine);


                }
            }
        });

        //Register receiver
        eventsReceiver = new EventsReceiver();
        IntentFilter filter = new IntentFilter(LIKE);
        filter.addAction(NEXT);
        filter.addAction(PLAYPAUSE);
        filter.addAction(STOP);
        filter.addAction(SHARETEXT);
        filter.addAction(SHAREAUDIO);
        this.registerReceiver(eventsReceiver, filter);

    }

    private String getBestEngine() {
        String engine;
        boolean isgood = false;

        List<TextToSpeech.EngineInfo> engines = t1.getEngines();

        for (TextToSpeech.EngineInfo engineinfo : engines) {
            if (engineinfo.name.equals(samsungEngine)) {
                isgood = true;
                break;
            }
        }

        if (isgood) {
            Toast.makeText(MainActivity.this, "Detectado tts samsung", Toast.LENGTH_SHORT).show();
            engine = samsungEngine;
        } else {
            Toast.makeText(MainActivity.this, "Instale un sintenizador bueno, la calidad no será aceptable. Por ejemplo, tts samsung", Toast.LENGTH_SHORT).show();
//            courtesyMessage("Instale un sintenizador bueno, la calidad no será aceptable. Por ejemplo, el sintenizador de Samsung");
            engine = t1.getDefaultEngine();
        }
        return engine;
    }

    public void onClickPlayStop(View view) {

        final boolean speaking = t1.isSpeaking();


        myLog.add(tag, "*********PRESSED Play/Stop . Speaking?" + speaking);

        //STOP
        if (speaking) {
//            btnPlayStop.setText("PLAY");
            interrupted = true;
            t1.stop();
            myLog.add(tag, "        ***after pressing. Speaking?" + t1.isSpeaking());

            //PLAY
        } else {
//            btnPlayStop.setText("STOP");

            playCurrentChapter();
        }
    }

    private void playCurrentChapter() {

        if (currentChiste == null) {
            myLog.add(tag, "..Playnext porque play current pero no hay ninguno");
            playNext();

        } else {
            myLog.add(tag, "----> MANDADO: " + currentChiste.firstLine());

            final String risa = getRisa();


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                t1.playSilentUtterance(800, TextToSpeech.QUEUE_ADD, uListener.UTTSILENCE);
                t1.speak(currentChiste.getProcessedText(), nextQueueMode, null, currentChiste.firstLine());
                t1.speak(risa, TextToSpeech.QUEUE_ADD, null, UTTRISA);


            } else {
                HashMap<String, String> map = new HashMap<>();


                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, currentChiste.firstLine());
                t1.speak(currentChiste.getProcessedText(), nextQueueMode, map);

                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTRISA);
                t1.speak(risa, TextToSpeech.QUEUE_ADD, map);
            }
        }
    }

    private String getRisa() {

        String risa;

        if (getProbability(0.33)) {

            final ArrayList<String> risas = new ArrayList<>();
            risas.add("Ahhh, Es que me parto.");
            risas.add("Ja ja ja, qué bueno.");
            risas.add("oj oj oj parto pecho");
            risas.add("jaaaa jaaa me meo");
            risas.add("En realidad no tiene gracia.");
            risas.add("Je je, es muy malo.");
            risas.add("ja ja ¡Pero quién coño los inventa!");
            risas.add("Qué risa, Felisa");
            risas.add("jaaa jaaa jaaa, me descojono");
            risas.add("jojojó, me rompo.");
            risas.add("ayyy, qué salá qué soy");

            final int iChiste = new Random().nextInt(risas.size());
            risa = risas.get(iChiste);
        } else {
            risa = UTTEHM;
        }
        myLog.add(tag, "risa es:" + risa);
        return risa;
    }

    /**
     * Entrega TRUE conuna probabilidad de v
     *
     * @param v
     * @return
     */
    private boolean getProbability(double v) {
        final int i = new Random().nextInt(100 + 1);
        myLog.add(tag, "valor random:" + i);

        return i <= 100 * v;
    }

    private void setSpeakLanguage(String lan, TextToSpeech t) {
        myLog.add(tag, "Setting Language:" + lan);
        //todo elegir el motor y las voces una sola vez, al inicio

        switch (lan) {
            case "ES":
                Locale spanish = new Locale("es", "ES");
                t.setLanguage(spanish);
                break;

            case "EN":
                t.setLanguage(Locale.US);

                //Select voice
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    Set<Voice> voices = t.getVoices();
                    for (Voice voice : voices) {
                        // if (voice.getName().equals("en-US-SMTl01")) {
                        if (voice.getQuality() == 400 && voice.getLocale() == Locale.US) {
                            myLog.add(tag, "voice set to: " + voice.toString());
                            t.setVoice(voice);
                            break;
                        }
                    }
                }
                break;
        }
    }

    public void onClickNext(View view) {
        interrupted = true;
        myLog.add(tag, "*********PRESSED NEXT");

        //block button 2 secons
//        btnNext.setEnabled(false);
//        btnNext.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                btnNext.setEnabled(true);
//            }
//        }, 2000);

        myLog.add(tag, "..Playnext porque cluckedNext");

//        if (!btnLike.isEnabled()) btnLike.setEnabled(true);

        //avanzamos en el buffer y luego saltanos
        playNext();
    }

    /**
     * Checks if we have internet connection     *
     *
     * @return
     */
    public boolean isOnline() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        boolean b = netInfo != null && netInfo.isConnectedOrConnecting();
        myLog.add(tag, "Checking connectivity: " + b);

        return b;
    }

    public void setCurrentChapter(final Chiste currentChapter) {
        this.currentChiste = currentChapter;

//        this.runOnUiThread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        txtText.setText(currentChapter.getText());
//                        txtDesc.setText(currentChapter.toString());
//                    }
//                });
    }

    /**
     * Lee de parse (local o no) y guarda en buffer (array) nchpater
     *
     * @param iChapter
     * @param bufferSize
     */
    private void getChapterAndPlay(final int iChapter, final int bufferSize, final boolean local) {
//        currentBook = bookSummary;

        myLog.add(tag, "getChapterAndPlay:local " + local + " desde chiste numero:" + iChapter);
        parseHelper.getChistes(iChapter, bufferSize, local, new FindCallback<Chiste>() {
            @Override
            public void done(List<Chiste> chistes, ParseException e) {
                if (e == null) {
                    myLog.add(tag2, "--- Traidos capitulos:" + chistes.size() + "desde local?" + local);

                    if (chistes.size() > 0) {
                        setCurrentChapter(chistes.get(0));
                        iBuffer = 0;
                        chistesPreLoaded = chistes;
                        playCurrentChapter();

                    } else {
                        //Hemos llegado al final
//                        if (currentBook.nChapters() == currentChapter.getChapterId()) {
//                            courtesyMessage("Fin. Espero que te haya gustado tanto como a mi.");
//                            if (entireBookMode) {
//                                BookContability.setFinishedBook(currentBook);
//                            } else {
//                                courtesyMessage("Ah, pero no lo habías oído desde el principio.");
//                                onClickLike(null);
//                            }
//                        } else {
//                            myLog.add(tag, "como no hemos cargado chaps, ponemos random");
//                            getRandomChaptersAndPlay(10);
//
//                        }

                    }

                } else {
                    myLog.add(tag, "errer---" + e.getLocalizedMessage());
                }
            }
        });


//        parseHelper.getBookSummary(iBook, local, cb);
    }

    private void getRandomChistesAndPlay(final int nChistes) {

        if (isOnline()) {

            final int nTotalChistes = 354;
            final int iChiste = new Random().nextInt(nTotalChistes + 1);

            myLog.add(tag, "+++ el random pra traer es:" + iChiste);
//                    BookContability.incrementJumpedInBook(bookSummary); TODO MARCAR libro por visita

            getChapterAndPlay(iChiste, nChistes, false);

//            getRandomBookSummary(musicMode, cb);


        } else {
//            courtesyMessage("Tengo un problema, no puedo recordar más historias. Si conectas internet, seguro que me refresca la memoria.");
        }
    }

    public void onClickLike(View view) {
//        String[] dividedLyrics = currentChiste.getDividedLyrics();
//
//        // TEST on wheel
//        View outerView = LayoutInflater.from(this).inflate(R.layout.wheel_view, null);
//        WheelView wv = (WheelView) outerView.findViewById(R.id.wheel_view_wv);
//        wv.setOffset(2);
////        wv.setItems(Arrays.asList(PLANETS));
//        wv.setItems(Arrays.asList(dividedLyrics));
//        wv.setSelection(3);
//        wv.setOnWheelViewListener(new WheelView.OnWheelViewListener() {
//            @Override
//            public void onSelected(int selectedIndex, String item) {
//                myLog.add(tag, "[Dialog]selectedIndex: " + selectedIndex + ", item: " + item);
//            }
//        });
//
//        new AlertDialog.Builder(this)
//                .setTitle("Verso inicial")
//                .setView(outerView)
//                .setPositiveButton("OK", null)
//                .show();
//
////        myLog.add(tag, "*********PRESSED LIKE");
////
////        btnLike.setEnabled(false);
////
////
////        if (isOnline()) {
////            final int bookId = currentChiste.getBookId();
////            courtesyMessage("Bueno, ya que te gusta el relato, veamo si recuerdo cómo empezaba...");
////            interrupted = true;
////            getChapterAndPlay(bookId, 1, 10, false);
////
////
////            parseHelper.importWholeBook(bookId, new TaskDoneCallback2() {
////                @Override
////                public void onDone() {
////                    myLog.add(tag, "DONE. book " + bookId + " loaded in internal storage");
////                    entireBookMode = true;
////                }
////
////                @Override
////                public void onError(String text, ParseException e) {
////                    myLog.error("fallo en importar los libros |" + text, e);
////                }
////            });
////
////        } else {
////            courtesyMessage("Te lo contaría desde el principio, pero necesitamos conección a internet para ayudarme a recordar. Seguiré por donde iba...");
////            playCurrentChapter();
////        }
    }

    private void showMediaNotification() {

        Intent like = new Intent(LIKE);
        PendingIntent likePendingIntent = PendingIntent.getBroadcast(this, 1, like, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent play = new Intent(PLAYPAUSE);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 1, play, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent next = new Intent(NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 1, next, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stop = new Intent(STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 1, stop, PendingIntent.FLAG_UPDATE_CURRENT);


        int iconPlayPause = t1.isSpeaking() ? android.
                R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
//        int iconMusicBook = musicMode ? R.drawable.ic_book : R.drawable.ic_music_note_white_24dp;
        int iconMusicBook = android.R.drawable.ic_media_ff;

        String title = "Chiste";
        String content = currentChiste.getType();

        Notification notification = new NotificationCompat.Builder(this)
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(android.R.drawable.ic_media_play)
                        // Add media control buttons that invoke intents in your media service
                .addAction(android.R.drawable.ic_media_rew, "Previous", likePendingIntent) // #0
                .addAction(iconPlayPause, "Pause", playPendingIntent)  // #1
                .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)     // #2
//                        // Apply the media style template
                .setStyle(new NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(1 /* #1: pause button */)
//                        .setMediaSession( mMediaSession.getSessionToken()))
                )
                .setShowWhen(false)
                .setDeleteIntent(stopPendingIntent)
                .setContentTitle(title)
                .setContentText(content)
                .setSubText(currentChiste.firstLine())
//                .setLargeIcon(currentBook.getImageBitmap())
                .setTicker(currentChiste.firstLine())

                .build();
        // mId allows you to update the notification later on.
        mNotificationManager.notify(1, notification);
    }

    private void showLyricsNotification() {

        Intent shareText = new Intent(SHARETEXT);
        PendingIntent pendingIntentText = PendingIntent.getBroadcast(this, 1, shareText, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent shareAudio = new Intent(SHAREAUDIO);
        PendingIntent pendingIntentAudio = PendingIntent.getBroadcast(this, 1, shareAudio, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(android.R.drawable.ic_media_play) //TODO poner icono de musica
                        // Add media control buttons that invoke intents in your media service
//                .addAction(android.R.drawable.ic_media_rew, "Previous", likePendingIntent) //todo poner boton paa wasap

                .setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                        .bigText(currentChiste.getText())
                        .setBigContentTitle(currentChiste.firstLine()))
                .setShowWhen(false)
//                .setDeleteIntent(stopPendingIntent)
                .setContentTitle(currentChiste.getType())
                .setContentText(currentChiste.firstLine())
//                .setSubText(currentChapter.getChapterId() + "/" + currentBook.nChapters())
//                .setLargeIcon(currentBook.getImageBitmap())
//                .setTicker(currentBook.getTitle() + "\n" + currentBook.fakeAuthor())
                        // actions
                .addAction(android.R.drawable.ic_media_next, "TEXT", pendingIntentText)
                .addAction(android.R.drawable.ic_media_previous, "AUDIO", pendingIntentAudio)
                .build();
        // mId allows you to update the notification later on.
        mNotificationManager.notify(2, notification);
    }

    private void removeLyricNotification() {
        mNotificationManager.cancel(2);
    }

    private void sendWhatsappIntent(Intent whatsappIntent) {
        myLog.add(tagW, "sending wasap intent");

        // cerramos las notificaciones
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        MainActivity.this.sendBroadcast(it);


        // ponemos la actividad en el frente
        Intent intentHome = new Intent(getApplicationContext(), MainActivity.class);
        intentHome.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intentHome);
        //Quitamos el lock
        Window w = this.getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
//        w.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        //enviar a Whatsapp
        try {
            this.startActivity(whatsappIntent);
        } catch (ActivityNotFoundException ex) {
            //todo dibujos para botones de notif
            Toast.makeText(this, "Whatsapp have not been installed.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            myLog.add(tagW, "error lanzando la actividad wasap: " + e.getLocalizedMessage());
        }
    }

    public void shareLyricWhatsapp(final boolean audio) {

        if (audio) {

            interrupted = true;
            t1.stop();

            String text = currentChiste.getTextForAudio();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Toast.makeText(MainActivity.this, "Preparando la voz para enviar...", Toast.LENGTH_SHORT).show();

                File fileTTS = new File(destFileName);
                t1.synthesizeToFile(text, null, fileTTS, uttsavingFile);

            } else {
                HashMap<String, String> map = new HashMap<>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, uttsavingFile);
                t1.synthesizeToFile(text, map, destFileName);
            }


        } else { //TEXTO
            final String text = currentChiste.getFormatedText() +
                    "_____\n" + "\nEnviado por *Descojono*";

            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.setType("text/plain");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, text);

            sendWhatsappIntent(whatsappIntent);

        }


    }

    /**
     * Empieza el siguiente del buffer, y se si ha acabado, trae los siguients
     */
    private void playNext() {
        myLog.add(tag, "en playNext, ibffer = " + iBuffer);

        if (chistesPreLoaded == null || iBuffer + 1 == chistesPreLoaded.size()) { //Traer los siguientes
            getRandomChistesAndPlay(10);

        } else {

            iBuffer++;
            final Chiste chiste = chistesPreLoaded.get(iBuffer);
            myLog.add(tag2, "Ahora ibffer=" + iBuffer + " y el chaper es" + chiste.firstLine());

            setCurrentChapter(chiste);
            playCurrentChapter();
        }

    }

    private interface StringCallback {
        void onDone(String[] versos);
    }

    class uListener extends UtteranceProgressListener {

        private static final String UTTSILENCE = "silence";

        @Override
        public void onStart(String utteranceId) {
            myLog.add(tag, "----> START SPEAKING: " + utteranceId);
//                                        Toast.makeText(MainActivity.this, utteranceId, Toast.LENGTH_SHORT).show();

            if (utteranceId.equals(uttsavingFile)) myLog.add(tagW, " onstart uttery");

            showMediaNotification();
            //notificación con la letra
            if (!utteranceId.equals(COURTESY)) {
                showLyricsNotification();
                isShowingLyricsNotification = true;
            }

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    btnPlayStop.setText("STOP");
//                }
//            });
        }

        @Override
        public void onDone(String utteranceId) {
            myLog.add(tag, "----> END SPEAKING: " + utteranceId + " forced?" + interrupted);
            // if (utteranceId.equals(msgs)) return;

            //Quitar la notificaciónde lyrics si es que
            if (isShowingLyricsNotification) {
                removeLyricNotification();
            }


            if (utteranceId.equals(UTTSILENCE) || utteranceId.equals(UTTEHM)) {

            } else if (utteranceId.equals(uttsavingFile)) {
                myLog.add(tagW, "terminado de guardar el archivo, vamos a mandar el intent");
                // enviar el file to whatsapp

                // Todo convert to mp3 before sending
                Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
                whatsappIntent.setPackage("com.whatsapp");

                whatsappIntent.setType("audio/*");
                Uri uri = Uri.parse(destFileName);

                whatsappIntent.putExtra(Intent.EXTRA_STREAM, uri);

                sendWhatsappIntent(whatsappIntent);
            } else if (utteranceId.equals(UTTRISA)) {
                myLog.add(tag, "Ha finalizado, " + utteranceId + " por lo que ponemos el siguiente");
                playNext();

            } else {
                if (interrupted) {
                    myLog.add(tag, "se ha interrumpido, no ponemos otro");
                    showMediaNotification(); //Para cambiar el boton
                    interrupted = false;

                    //termino de contar la el chapter
                }
            }
        }


        @Override
        public void onError(String utteranceId) {
            myLog.add(tag, "***ERRORen utterance: id = " + utteranceId);

        }

    }

    private class EventsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action;
            try {
                action = intent.getAction();
                myLog.add(tag, "action received: " + action);
                switch (action) {
                    case LIKE:
                        onClickLike(null);
                        break;
                    case PLAYPAUSE:
                        onClickPlayStop(null);
                        break;
                    case NEXT:
                        onClickNext(null);
                        break;
                    case STOP:
                        t1.stop();
                        finish();
                        break;
                    case SHARETEXT:
                        //TODO select the text to share
                        shareLyricWhatsapp(false);
                        break;
                    case SHAREAUDIO:
                        shareLyricWhatsapp(true);
                        break;
                }
            } catch (Exception e) {
                myLog.error("on receive broadcast", e);
            }
        }

    }

}
