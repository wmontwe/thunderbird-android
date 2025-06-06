package com.fsck.k9.activity.loader;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.Context;
import androidx.loader.content.AsyncTaskLoader;

import com.fsck.k9.activity.misc.Attachment;
import com.fsck.k9.message.Attachment.LoadingState;
import de.cketti.safecontentresolver.SafeContentResolver;
import org.apache.commons.io.IOUtils;
import net.thunderbird.core.logging.legacy.Log;

/**
 * Loader to fetch the content of an attachment.
 *
 * This will copy the data to a temporary file in our app's cache directory.
 */
public class AttachmentContentLoader extends AsyncTaskLoader<Attachment> {
    private static final String FILENAME_PREFIX = "attachment";


    private final Attachment sourceAttachment;
    private Attachment cachedResultAttachment;


    public AttachmentContentLoader(Context context, Attachment attachment) {
        super(context);
        if (attachment.state != LoadingState.METADATA) {
            throw new IllegalArgumentException("Attachment provided to content loader must be in METADATA state");
        }

        sourceAttachment = attachment;
    }

    @Override
    protected void onStartLoading() {
        if (cachedResultAttachment != null) {
            deliverResult(sourceAttachment);
        }

        if (takeContentChanged() || cachedResultAttachment == null) {
            forceLoad();
        }
    }

    @Override
    public Attachment loadInBackground() {
        Context context = getContext();

        try {
            File file = File.createTempFile(FILENAME_PREFIX, null, context.getCacheDir());
            file.deleteOnExit();

            Log.v("Saving attachment to %s", file.getAbsolutePath());

            InputStream in;

            if (sourceAttachment.internalAttachment) {
                ContentResolver unsafeContentResolver = context.getContentResolver();
                in = unsafeContentResolver.openInputStream(sourceAttachment.uri);
            } else {
                SafeContentResolver safeContentResolver = SafeContentResolver.newInstance(context);
                in = safeContentResolver.openInputStream(sourceAttachment.uri);
            }
            if (in == null) {
                Log.w("Error opening attachment for reading: %s", sourceAttachment.uri);

                cachedResultAttachment = sourceAttachment.deriveWithLoadCancelled();
                return cachedResultAttachment;
            }

            try {
                FileOutputStream out = new FileOutputStream(file);
                try {
                    IOUtils.copy(in, out);
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }

            cachedResultAttachment = sourceAttachment.deriveWithLoadComplete(file.getAbsolutePath());
            return cachedResultAttachment;
        } catch (Exception e) {
            Log.e(e, "Error saving attachment!");
        }

        cachedResultAttachment = sourceAttachment.deriveWithLoadCancelled();
        return cachedResultAttachment;
    }
}
