package com.foobnix.pdf.info.wrapper;

import android.os.Environment;
import com.foobnix.android.utils.LOG;

import java.io.File;
import java.io.FileWriter;

/**
 * Writes reading progress and TTS scrobble events to shared storage
 * so Termux scripts can sync them to Obsidian.
 *
 * Output files (created automatically):
 *   /sdcard/obsidian-sync/progress.json      — latest reading position per book
 *   /sdcard/obsidian-sync/tts-scrobble.jsonl — append-only TTS chapter log
 */
public class ObsidianSync {

    private static final String TAG = "ObsidianSync";
    private static final File SYNC_DIR = new File(
            Environment.getExternalStorageDirectory(), "obsidian-sync");

    /** Called on every page save — writes current position for this book. */
    public static void syncProgress(String bookPath, int page, int totalPages) {
        if (bookPath == null || totalPages <= 0) return;
        try {
            SYNC_DIR.mkdirs();
            String bookName = new File(bookPath).getName();
            float pct = (float) page / totalPages * 100f;
            String json = String.format(
                    "{\"book\":\"%s\",\"page\":%d,\"pages\":%d,\"percent\":%.1f,\"ts\":%d}\n",
                    bookName.replace("\"", "\\\""), page, totalPages, pct, System.currentTimeMillis());
            // Overwrite per-book file so we always have the latest position
            File f = new File(SYNC_DIR, sanitize(bookName) + ".json");
            try (FileWriter fw = new FileWriter(f, false)) {
                fw.write(json);
            }
        } catch (Exception e) {
            LOG.e(TAG, e);
        }
    }

    /** Called when TTS advances to a new page/chapter. */
    public static void scrobbleChapter(String bookPath, String bookTitle, int page) {
        if (bookPath == null) return;
        try {
            SYNC_DIR.mkdirs();
            String bookName = bookTitle != null ? bookTitle : new File(bookPath).getName();
            String line = String.format(
                    "{\"event\":\"tts_page\",\"book\":\"%s\",\"page\":%d,\"ts\":%d}\n",
                    bookName.replace("\"", "\\\""), page, System.currentTimeMillis());
            File f = new File(SYNC_DIR, "tts-scrobble.jsonl");
            try (FileWriter fw = new FileWriter(f, true)) {
                fw.write(line);
            }
        } catch (Exception e) {
            LOG.e(TAG, e);
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
