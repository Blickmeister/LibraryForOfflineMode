package cz.fim.uhk.thesis.libraryforofflinemode;

import android.content.Context;

import java.util.List;

public interface LibraryLoaderInterface {
    int start(String path, Context context);

    int stop();

    int resume(List<?> clients);

    int exit();

    String getDescription();
}
