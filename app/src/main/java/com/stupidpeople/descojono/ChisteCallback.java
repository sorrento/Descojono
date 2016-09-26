package com.stupidpeople.descojono;

import com.parse.ParseException;

/**
 * Created by Milenko on 20/09/2016.
 */
public interface ChisteCallback {
    void onError(ParseException e);

    void OnDone(Chiste chiste);
}
