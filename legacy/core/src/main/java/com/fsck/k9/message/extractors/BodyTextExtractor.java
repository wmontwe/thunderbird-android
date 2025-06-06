package com.fsck.k9.message.extractors;


import androidx.annotation.NonNull;
import net.thunderbird.core.logging.legacy.Log;

import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.message.html.HtmlConverter;


//TODO: Get rid of this class and use MessageViewInfoExtractor instead
public class BodyTextExtractor {
    /** Fetch the body text from a messagePart in the desired messagePart format. This method handles
     * conversions between formats (html to text and vice versa) if necessary.
     */
    @NonNull
    public static String getBodyTextFromMessage(Part messagePart, SimpleMessageFormat format) {
        Part part;
        if (format == SimpleMessageFormat.HTML) {
            // HTML takes precedence, then text.
            part = MimeUtility.findFirstPartByMimeType(messagePart, "text/html");
            if (part != null) {
                Log.d("getBodyTextFromMessage: HTML requested, HTML found.");
                return getTextFromPartOrEmpty(part);
            }

            part = MimeUtility.findFirstPartByMimeType(messagePart, "text/plain");
            if (part != null) {
                Log.d("getBodyTextFromMessage: HTML requested, text found.");
                String text = getTextFromPartOrEmpty(part);
                return HtmlConverter.textToHtml(text);
            }
        } else if (format == SimpleMessageFormat.TEXT) {
            // Text takes precedence, then html.
            part = MimeUtility.findFirstPartByMimeType(messagePart, "text/plain");
            if (part != null) {
                Log.d("getBodyTextFromMessage: Text requested, text found.");
                return getTextFromPartOrEmpty(part);
            }

            part = MimeUtility.findFirstPartByMimeType(messagePart, "text/html");
            if (part != null) {
                Log.d("getBodyTextFromMessage: Text requested, HTML found.");
                String text = getTextFromPartOrEmpty(part);
                return HtmlConverter.htmlToText(text);
            }
        }

        // If we had nothing interesting, return an empty string.
        return "";
    }

    @NonNull
    private static String getTextFromPartOrEmpty(Part part) {
        String text = MessageExtractor.getTextFromPart(part);
        return text != null ? text : "";
    }
}
