package ai.context.util.common;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.MessageDigest;
import java.util.ArrayList;

public class FileUtils {
    public static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    public static String extractText(String html) throws IOException {
        final ArrayList<String> list = new ArrayList<String>();

        ParserDelegator parserDelegator = new ParserDelegator();
        HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback() {
            public void handleText(final char[] data, final int pos) {
                list.add(new String(data));
            }

            public void handleStartTag(HTML.Tag tag, MutableAttributeSet attribute, int pos) {
            }

            public void handleEndTag(HTML.Tag t, final int pos) {
            }

            public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, final int pos) {
            }

            public void handleComment(final char[] data, final int pos) {
            }

            public void handleError(final java.lang.String errMsg, final int pos) {
            }
        };
        parserDelegator.parse(new StringReader(html), parserCallback, true);

        String text = "";

        for (String s : list) {
            text += " " + s;
        }

        return text;
    }
}
