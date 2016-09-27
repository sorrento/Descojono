package com.stupidpeople.descojono;

import com.parse.ParseException;

/**
 * Created by Milenko on 19/09/2016.
 */
public interface TaskCallback {
    void onDone(int size);

    void onError(ParseException e, String msg);

}
