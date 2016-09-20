package com.stupidpeople.descojono;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.SaveCallback;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private static final String PREFS_FIRST_TIME = "first time";
    private static final String PREFS_CURRENT_CENTENA = "centena";
    private static final String LIKE = "like";

    private static final String PLAYPAUSE = "playpause";
    private static final String NEXT = "next";
    private static final String STOP = "stop";
    private static final String SHARETEXT = "shareText";
    private static final String SHAREAUDIO = "shareAudio";
    private static final String UTTRISA = "risa";
    final private String samsungEngine = "com.samsung.SMT";
    TextToSpeech t1;
    private SharedPreferences settings;
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
    private boolean isShowingTextNotification = false;
    private String destFileName;
    private String uttsavingFile = "savingFile";
    private Intent mIntentWhatsApp;
    private int centena;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myLog.initialize("LOG_DESCO");

        destFileName = (Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/Download") + "/" + "tts_file.wav";//TODO remove audio file

        // Settings
        settings = getPreferences(MODE_PRIVATE);
        centena = settings.getInt(PREFS_CURRENT_CENTENA, 0);

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

                                if (isFirstTime()) importaYCuenta();
                                else cuentaDesdeLocal();
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

    private boolean isFirstTime() {
        boolean first = settings.getBoolean(PREFS_FIRST_TIME, true);
        myLog.add(tag, "***es first time:" + first);

        return first;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(eventsReceiver);
        settings.edit().putBoolean(PREFS_FIRST_TIME, false).apply();

        removeTextNotification();
        removeMediaNotification();
        t1.shutdown();

    }


    // Chistes Management

    private void importaYCuenta() {
        parseHelper.importChistes(centena, this, new TaskCallback() {
            @Override
            public void onDone() {
                myLog.add("IMP", "se han traido y guardado 100 chistes de centena " + centena);
                centena++;
                settings.edit().putInt(PREFS_CURRENT_CENTENA, centena + 1).commit();

                cuentaDesdeLocal();

            }

            @Override
            public void onError(ParseException e, String msg) {
                myLog.error("Imposible importar:" + msg, e);
            }
        });
    }

    /**
     * Lee desde local 10 chistes que no haya leido, y los pasa a buffer para contarlos.
     */
    private void cuentaDesdeLocal() {

        parseHelper.load20chistesrandomDesdeLocal(new FindCallback<Chiste>() {
            @Override
            public void done(List<Chiste> chistes, ParseException e) {
                if (e == null) {
                    if (chistes != null && chistes.size() > 0) {
                        myLog.add(tag, "traidos los 20 chistes");
                        chistesPreLoaded = chistes;
                        setCurrentChiste(chistes.get(0));
                        iBuffer = 0;
                        playCurrentChiste();

                    } else {
                        myLog.add(tag, "****No he podido cargar 20 nuevos, así que importo 100");
                        importaYCuenta();
                    }
                } else {
                    myLog.error("trayendo 20 desde local", e);
                }
            }
        });
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

    private void playCurrentChiste() {

        if (currentChiste == null) {
            myLog.add(tag, "..Playnext porque play current pero no hay ninguno");
            playNext();

        } else {
            myLog.add(tag, "----> MANDADO: " + currentChiste.firstLine());

            final String risa = utils.getRisa();


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

    public void setCurrentChiste(final Chiste currentChiste) {
        this.currentChiste = currentChiste;

//        this.runOnUiThread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        txtText.setText(currentChapter.getText());
//                        txtDesc.setText(currentChapter.toString());
//                    }
//                });
    }

    // Buttons

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

            playCurrentChiste();
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

    private void playNext() {
        myLog.add(tag, "en playNext, ibffer = " + iBuffer);

        if (chistesPreLoaded == null || iBuffer + 1 == chistesPreLoaded.size()) { //Traer los siguientes de local
            cuentaDesdeLocal();

        } else {

            iBuffer++;
            final Chiste chiste = chistesPreLoaded.get(iBuffer);
            myLog.add(tag2, "Ahora ibffer=" + iBuffer + " y el chaper es" + chiste.firstLine());

            setCurrentChiste(chiste);
            playCurrentChiste();
        }

    }


    // Notifications

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

        final String title = currentChiste.NotifTitle();
        final String content = currentChiste.NotifSubTitle();
        final String subtext = currentChiste.NotifSubText();

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
                .setSubText(subtext)
//                .setLargeIcon(currentBook.getImageBitmap())
                .setTicker(subtext)

                .build();
        // mId allows you to update the notification later on.
        mNotificationManager.notify(1, notification);
    }

    private void showTextNotification() {

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

    private void removeTextNotification() {
        mNotificationManager.cancel(2);
    }

    private void removeMediaNotification() {
        mNotificationManager.cancel(1);
    }

    private void hideNotifications() {
        MainActivity.this.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }


    // Whatsapp

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        myLog.add(tagW, "********onnew intent");

        if (intent.getBooleanExtra("fromNotification", false)) {

            hideNotifications();

            // Enviar a Whatsapp
            try {
                MainActivity.this.startActivity(mIntentWhatsApp);
            } catch (ActivityNotFoundException ex) {
                //todo dibujos para botones de notif
                Toast.makeText(MainActivity.this, "Whatsapp have not been installed.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                myLog.add(tagW, "error lanzando la actividad wasap: " + e.getLocalizedMessage());
            }


            //Quitamos el lock, requiere que la ventana sea la primera
//            final Window w = this.getWindow();
//            this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    w.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
//                    w.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
//
//                    // Enviar a Whatsapp
//                    try {
//                        MainActivity.this.startActivity(mIntentWhatsApp);
//                    } catch (ActivityNotFoundException ex) {
//                        //todo dibujos para botones de notif
//                        Toast.makeText(MainActivity.this, "Whatsapp have not been installed.", Toast.LENGTH_SHORT).show();
//                    } catch (Exception e) {
//                        myLog.add(tagW, "error lanzando la actividad wasap: " + e.getLocalizedMessage());
//                    }
//
//                }
//            });
        }
    }

    private void sendWhatsappIntent(Intent whatsappIntent) {
        myLog.add(tagW, "sending wasap intent");


        mIntentWhatsApp = whatsappIntent;

        // ponemos la actividad en el frente
        Intent intentHome = new Intent(getApplicationContext(), MainActivity.class);
        intentHome.putExtra("fromNotification", true);
        intentHome.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intentHome);

//        https://blog.akquinet.de/2010/04/15/android-activites-and-tasks-series-intent-flags/
    }

    public void shareLyricWhatsapp(final boolean audio) {

        if (audio) {

            interrupted = true;
            t1.stop();

            String text = currentChiste.getTextForAudio() + utils.getFraseDive() + utils.getPiblicity();

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
                    "_____\n" + "\nEnviado por *Descojono*"; //TODO poner enlace a la app

            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.setType("text/plain");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, text);

            sendWhatsappIntent(whatsappIntent);
        }

    }

    private void audioawhatsapp() {
        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setPackage("com.whatsapp");

        whatsappIntent.setType("audio/*");
        Uri uri = Uri.parse(destFileName);
        whatsappIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendWhatsappIntent(whatsappIntent);
    }

    // Handlers

    class uListener extends UtteranceProgressListener {

        private static final String UTTSILENCE = "silence";

        @Override
        public void onStart(final String utteranceId) {
            myLog.add(tag, "----> START SPEAKING: " + utteranceId);


            //Si es el chiste
            if (!(utteranceId.equals(uttsavingFile) || utteranceId.equals(UTTRISA)
                    || utteranceId.equals(UTTSILENCE) || utteranceId.equals(utils.UTTEHM))) {

                myLog.add("CONT", currentChiste.getChisteId() + " | " + utteranceId);

                // Marcar como contado
                currentChiste.setContado(true);
                currentChiste.pinInBackground(parseHelper.PINCENTENA, new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            myLog.add(tag, "marcado como contado: " + utteranceId);
                        } else myLog.error("marcondo como leido", e);
                    }
                });

                showMediaNotification();
                showTextNotification();
                isShowingTextNotification = true;
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

            if (utteranceId.equals(UTTSILENCE) || utteranceId.equals(utils.UTTEHM)) {

            } else if (utteranceId.equals(uttsavingFile)) {
                myLog.add(tagW, "terminado de guardar el archivo, vamos a mandar el intent");

                // Ponemos un delay a ver si lo manda entero el archivo
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        audioawhatsapp();
                    }
                }, 3000);


            } else if (utteranceId.equals(UTTRISA)) {
                myLog.add(tag, "Ha finalizado, " + utteranceId + " por lo que ponemos el siguiente");
                if (isShowingTextNotification) {
                    removeTextNotification();
                }

                playNext();

            } else {

                if (interrupted) {
                    myLog.add(tag, "se ha interrumpido, no ponemos otro");
                    showMediaNotification(); //Para cambiar el boton
                    interrupted = false;

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
