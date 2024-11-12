package com.appfire.jpi.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class CDataTextSerializer extends JsonSerializer<String> {
    /**
     * Regex matching all ascii characters not supported in xml text.
     * 
     * \x00-\x08, \x0E-\x1F - matches null + control chars
     * \x0B - vertical tab
     * \x0C - form field
     */
    private static final String INVALID_CDATA_TEXT_ASCII_CHARS = "[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x0A]";

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String sanitizedValue = value
                // Remove nested cdata.
                .replaceAll("]]>", "")
                // Remove unsupported characters for a xml text.
                .replaceAll(INVALID_CDATA_TEXT_ASCII_CHARS, "")
                // Remove nested cdata.
                .replaceAll("<!\\[CDATA\\[", "");
        gen.writeString(sanitizedValue);
    }

    /**
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    public static String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in)))
            return ""; // vacancy test.
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                    (current == 0xA) ||
                    (current == 0xD) ||
                    ((current >= 0x20) && (current <= 0xD7FF)) ||
                    ((current >= 0xE000) && (current <= 0xFFFD)) ||
                    ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    }
}
