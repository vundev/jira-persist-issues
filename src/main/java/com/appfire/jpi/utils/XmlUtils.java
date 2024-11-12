package com.appfire.jpi.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class XmlUtils {
    private static final String XML_VERSION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String XML_ROOT_START = "<root>";
    private static final String XML_ROOT_END = "</root>";
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public static void writeXmlVersion(OutputStream out) throws IOException {
        writeNewLine(XML_VERSION, out);
    }

    public static void writeRootStart(OutputStream out) throws IOException {
        writeNewLine(XML_ROOT_START, out);
    }

    public static void writeRootEnd(OutputStream out) throws IOException {
        writeNewLine(XML_ROOT_END, out);
    }

    private static void writeNewLine(String data, OutputStream out) throws IOException {
        out.write(String.format("%s\n", data).getBytes(UTF8_CHARSET));
    }
}
