package com.stupidpeople.descojono;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.SaveCallback;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String PREFS_FIRST_TIME = "first time";
    private static final String PREFS_CURRENT_CENTENA = "centena";

    private static final String PREV = "prev";
    private static final String AGAIN = "again";
    private static final String LOVE = "love";
    private static final String STOP = "stop";

    private static final String SHARETEXT = "shareText";
    private static final String SHAREAUDIO = "shareAudio";
    private static final String UTTRISA = "risa";
    private static final int REQUEST_WRITE_PERMISSION = 789;
    private static final String tagError = "err";
    final int[] animalImages = {R.drawable.ic_01_foca, R.drawable.ic_02_burro, R.drawable.ic_03_panda,
            R.drawable.ic_04_chimp, R.drawable.ic_05_orangutanes, R.drawable.ic_06_orangutan_blnaco,
            R.drawable.ic_07_leon, R.drawable.ic_08_caballo, R.drawable.ic_09_tiburon, R.drawable.ic_10_perro,
            R.drawable.ic_11_buho, R.drawable.ic_12_perro2, R.drawable.ic_13_gato, R.drawable.ic_14_cabra,
            R.drawable.ic_15_cheeta, R.drawable.ic_16_morsa, R.drawable.ic_17_chimp_hand, R.drawable.ic_18_dog_serious,
            R.drawable.ic_19_nutria, R.drawable.ic_20_elephant, R.drawable.ic_21_rana, R.drawable.ic_22_orangutan2,
            R.drawable.ic_23_morsa2, R.drawable.ic_24_mono_riendo};
    final private String samsungEngine = "com.samsung.SMT";
    TextToSpeech t1;
    private SharedPreferences settings;
    //Tags
    private String tag = "mhp";
    private String tagW = "WAS";
    private String tag2 = "ACT";

    //others
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
    private String textToSintetize;

    // UI
    private TextView txtChiste;
    private ImageButton btnPrev;
    private ImageButton btnStopPlay;
    private ImageButton btnNext;
    private ImageButton btnLike;

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

        // UI
        txtChiste = (TextView) findViewById(R.id.txtchiste);
        btnPrev = (ImageButton) findViewById(R.id.btnPrev);
        btnStopPlay = (ImageButton) findViewById(R.id.btnStopPlay);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        btnLike = (ImageButton) findViewById(R.id.btnLove);

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
        IntentFilter filter = new IntentFilter(PREV);
        filter.addAction(LOVE);
        filter.addAction(AGAIN);
        filter.addAction(STOP);
        filter.addAction(SHARETEXT);
        filter.addAction(SHAREAUDIO);
        this.registerReceiver(eventsReceiver, filter);

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

    private boolean isFirstTime() {
        boolean first = settings.getBoolean(PREFS_FIRST_TIME, true);
        myLog.add(tag, "***es first time:" + first);

        return first;
    }


    // Chistes Management

    private void importaYCuenta() {
        parseHelper.importChistes(centena, this, new TaskCallback() {
            @Override
            public void onDone(int size) {
                myLog.add("IMP", "se han traido y guardado " + size + " chistes de centena " + centena);

                if (size > 0) {
                    centena++;
                    settings.edit().putInt(PREFS_CURRENT_CENTENA, centena).commit();

                    cuentaDesdeLocal();
                } else {

                    myLog.add(tag, "***Se han contado ya todos, empezamos de nuevo");
                    centena = 0;
                    settings.edit().putInt(PREFS_CURRENT_CENTENA, centena).commit();
                    importaYCuenta();
                }

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
        boolean isgood = false;

        List<TextToSpeech.EngineInfo> engines = t1.getEngines();

        for (TextToSpeech.EngineInfo engineinfo : engines) {
            if (engineinfo.name.equals(samsungEngine)) {
                isgood = true;
                break;
            }
        }

        return isgood ? samsungEngine : t1.getDefaultEngine();
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

    private void callar() {
        interrupted = true;
        t1.stop();
    }

    public void onClickPrev(View view) {
        callar();

        if (iBuffer > 0) {
            playFromBuffer(iBuffer - 1);
        } else {
            playFromBuffer(iBuffer);//todo, arreglar por si es el primero del buffer...
        }
    }

    public void onClickStopPlay(View view) {
        if (t1.isSpeaking()) t1.stop();
        else playCurrentChiste();
    }

    public void onClickNext(View view) {
        callar();
        playNext();
    }

    public void onClickAgain(View view) {
        callar();
        myLog.add("OJO", "onclick again. current chiste " + currentChiste.firstLine());
        playCurrentChiste();
    }

    public void onClickLove(View view) {
        currentChiste.invertFav();

        //para actualizar el corazon
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int imgHeart = currentChiste.isFav() ? R.drawable.ic_favorite_white_24dp
                        : R.drawable.ic_favorite_border_white_24dp;
                btnLike.setImageResource(imgHeart);
            }
        });
        showMediaNotification();

        currentChiste.pinInBackground(parseHelper.PINCENTENA);

    }

    private void playNext() {
        myLog.add(tag, "en playNext, ibffer = " + iBuffer);

        if (chistesPreLoaded == null || iBuffer + 1 == chistesPreLoaded.size()) { //Traer los siguientes de local
            cuentaDesdeLocal();

        } else {
            iBuffer++;
            playFromBuffer(iBuffer);
        }

    }

    private void playFromBuffer(int i) {
        final Chiste chiste = chistesPreLoaded.get(i);
        myLog.add(tag2, "playing ibffer=" + i + " y el chiste es" + chiste.firstLine());

        setCurrentChiste(chiste);
        playCurrentChiste();
    }


    // Notifications

    private void showMediaNotification() {

        Intent prev = new Intent(PREV);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 1, prev, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent again = new Intent(AGAIN);
        PendingIntent againPendingIntent = PendingIntent.getBroadcast(this, 1, again, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent love = new Intent(LOVE);
        PendingIntent lovePendingIntent = PendingIntent.getBroadcast(this, 1, love, PendingIntent.FLAG_UPDATE_CURRENT);

        //On Dismiss
        Intent stop = new Intent(STOP);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 1, stop, PendingIntent.FLAG_UPDATE_CURRENT);

        //On Click
        Intent intent = getIntentHome();
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        final String title = currentChiste.NotifTitle();
        final String content = currentChiste.NotifSubTitle();
        final String subtext = currentChiste.NotifSubText();

        int iconFav = currentChiste.isFav() ? R.drawable.ic_favorite_white_24dp
                : R.drawable.ic_favorite_border_white_24dp;

        Notification notification = new NotificationCompat.Builder(this)
                // Show controls on lock screen even when user hides sensitive content.
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_mono_mini)
                        // Add media control buttons that invoke intents in your media service
                .addAction(R.drawable.ic_skip_previous_white_24dp, "Previous", prevPendingIntent) // #0
                .addAction(R.drawable.ic_replay_white_24dp, "Again", againPendingIntent)  // #1
                .addAction(iconFav, "Love", lovePendingIntent)     // #2
//                        // Apply the media style template
                .setStyle(new NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(1 /* #1: replay*/)

//                        .setMediaSession( mMediaSession.getSessionToken()))
                )
                .setPriority(1)
                .setShowWhen(false)
                .setDeleteIntent(stopPendingIntent)
                .setContentTitle(title)
                .setContentText(content)
                .setSubText(subtext)
                .setLargeIcon(getBitmapAnimal())
                .setTicker(subtext)
                .setContentIntent(contentIntent)
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
                .setSmallIcon(R.drawable.ic_mono_mini)
                        // Add media control buttons that invoke intents in your media service
//                .addAction(android.R.drawable.ic_media_rew, "Previous", likePendingIntent) //todo poner boton paa wasap

                .setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                        .bigText(currentChiste.getText())
                        .setBigContentTitle(currentChiste.getType()))
                .setShowWhen(false)
//                .setDeleteIntent(stopPendingIntent)
                .setContentTitle(currentChiste.getType())
                .setContentText(currentChiste.firstLine())
//                .setSubText(currentChapter.getChapterId() + "/" + currentBook.nChapters())
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                .setTicker(currentBook.getTitle() + "\n" + currentBook.fakeAuthor())
                        // actions
                .addAction(R.drawable.ic_whatsapp, "TEXTO", pendingIntentText)
                .addAction(R.drawable.ic_whatsapp, "AUDIO", pendingIntentAudio)
                .build();
        // mId allows you to update the notification later on.
        mNotificationManager.notify(2, notification);
    }

    private Bitmap getBitmapAnimal() {

        int i = new Random().nextInt(animalImages.length);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), animalImages[i]);

        return bitmap;
    }

    private void removeMediaNotification() {
        mNotificationManager.cancel(1);
    }

    private void removeTextNotification() {
        mNotificationManager.cancel(2);
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

            myLog.add(tagW, "********onnew intent:mINtentswatsapp=" + mIntentWhatsApp.toString());
            hideNotifications();

            // Enviar a Whatsapp
            try {
                MainActivity.this.startActivity(mIntentWhatsApp);

            } catch (ActivityNotFoundException ex) {
                String text = "Whatsapp have not been installed.";
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                myLog.add(tagError, text + ex.getLocalizedMessage());
            } catch (Exception e) {
                myLog.add(tagError, "error lanzando la actividad wasap: " + e.getLocalizedMessage());
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        myLog.add(tagW, "OnRequestPermissionResult: " + permissions[0] + "| granteed? (0 is yes)" + grantResults[0]);

        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sintetizeToFile(textToSintetize);
        }
    }

    private void sendWhatsappIntent(Intent whatsappIntent) {

        mIntentWhatsApp = whatsappIntent;
        myLog.add(tagW, "sending wasap intent:" + mIntentWhatsApp.toString());

        startActivity(getIntentHome());
    }

    @NonNull
    private Intent getIntentHome() {
        //        https://blog.akquinet.de/2010/04/15/android-activites-and-tasks-series-intent-flags/

        Intent intentHome = new Intent(getApplicationContext(), MainActivity.class);
        intentHome.putExtra("fromNotification", true);
        intentHome.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentHome.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intentHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intentHome;
    }

    public void shareChisteWhatsapp(final boolean audio) {

        if (audio) {
            callar();

            textToSintetize = currentChiste.getTextForAudio() + "...\n" + utils.getFraseDive() +
                    "...\n " + utils.getPublicity();

            myLog.add(tagW, "testo para enviar a ws:" + textToSintetize);

            // Write file permission for Androis 6
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                myLog.add(tagW, "MASRSHMELOW gonna request permision");
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);

            } else {
                sintetizeToFile(textToSintetize);
            }

        } else {
            textToWhatsapp();
        }

    }

    private void sintetizeToFile(String text) {
        Toast.makeText(MainActivity.this, "Preparando la voz para enviar...", Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            File fileTTS = new File(destFileName);
            t1.synthesizeToFile(text, null, fileTTS, uttsavingFile);

        } else {
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, uttsavingFile);
            t1.synthesizeToFile(textToSintetize, map, destFileName);
        }
    }

    private void textToWhatsapp() {
        final String text = currentChiste.getFormatedText() +
                "_____\n" + "\nEnviado por *Descojono*"; //TODO poner enlace a la app

        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setPackage("com.whatsapp");
        whatsappIntent.setType("text/plain");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, text);

        sendWhatsappIntent(whatsappIntent);
    }

    private void audioToWhatsapp() {
        // esto es necesario para que se pueda compartir con Gmail por ejemplo.  usando uri.parse
        // fallaba "empty file"
        File file = new File(destFileName);
        Uri uri = Uri.fromFile(file);

        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setPackage("com.whatsapp");
        whatsappIntent.setType("audio/*");
        whatsappIntent.putExtra(Intent.EXTRA_STREAM, uri);
        whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        whatsappIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

//        startActivity(Intent.createChooser(whatsappIntent, "Share Sound File"));
        sendWhatsappIntent(whatsappIntent);
    }

    public void onClickWsText(View view) {
        shareChisteWhatsapp(false);
    }

    public void onClickWsAudio(View view) {
        shareChisteWhatsapp(true);
    }

    // Handlers

    class uListener extends UtteranceProgressListener {

        private static final String UTTSILENCE = "silence";

        @Override
        public void onStart(final String utteranceId) {
            myLog.add(tag, "----> START SPEAKING: " + utteranceId);

//            Uri uri = Uri.parse(destFileName);

            //Si es el chiste
            if (!(utteranceId.equals(uttsavingFile) || utteranceId.equals(UTTRISA)
                    || utteranceId.equals(UTTSILENCE) || utteranceId.equals(utils.UTTEHM))) {

                myLog.add("CONT", currentChiste.getChisteId() + " | " + utteranceId);

                //show chiste in app
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtChiste.setText(currentChiste.getText());
                        btnStopPlay.setImageResource(R.drawable.ic_stop_white_24dp);
                        int imgHeart = currentChiste.isFav() ? R.drawable.ic_favorite_white_24dp
                                : R.drawable.ic_favorite_border_white_24dp;
                        btnLike.setImageResource(imgHeart);

                    }
                });

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
            if (interrupted) {
                myLog.add(tag, "se ha interrumpido, no ponemos otro");
                interrupted = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnStopPlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                    }
                });
                return;
            }


            if (utteranceId.equals(UTTSILENCE) || utteranceId.equals(utils.UTTEHM)) {


                // Saving file
            } else if (utteranceId.equals(uttsavingFile)) {
                myLog.add(tagW, "terminado de guardar el archivo, vamos a mandar el intent");

                Uri uri = Uri.parse(destFileName);
                File file = new File(uri.toString());
                myLog.add("OJO", "[on done]size of file:" + file.length());


                audioToWhatsapp();
                // Ponemos un delay a ver si lo manda entero el archivo
//                Handler handler = new Handler(Looper.getMainLooper());
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        audioToWhatsapp();
//                    }
//                }, 3000);


            } else if (utteranceId.equals(UTTRISA)) {
                myLog.add(tag, "Ha finalizado, " + utteranceId + " por lo que ponemos el siguiente");
                if (isShowingTextNotification) {
                    removeTextNotification();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnStopPlay.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                    }
                });
                playNext();

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
                    case PREV:
                        onClickPrev(null);
                        break;
                    case AGAIN:
                        onClickAgain(null);
                        break;
                    case LOVE:
                        onClickLove(null);
                        break;
                    case STOP:
                        t1.stop();
                        finish();
                        break;
                    case SHARETEXT:
                        //TODO select the text to share
                        shareChisteWhatsapp(false);
                        break;
                    case SHAREAUDIO:
                        shareChisteWhatsapp(true);
                        break;
                }
            } catch (Exception e) {
                myLog.error("on receive broadcast", e);
            }
        }

    }

}
