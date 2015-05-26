package io.termd.core.http;

import io.termd.core.io.BinaryDecoder;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class IoUtils {

  public static void writeToDecoder(BinaryDecoder decoder, String msg) {
    JsonObject obj = new JsonObject(msg.toString());
    switch (obj.getString("action")) {
      case "read":
        String data = obj.getString("data");
        decoder.write(data.getBytes());
        break;
    }
  }

  /**
   * @param name Resource name to use.
   * @param classLoader Classloader to use
   * @return The configuration string
   * @throws IOException Thrown if data couldn't be loaded
   */
  public static String readResource(String name, ClassLoader classLoader) throws IOException {
    String configString;
    InputStream is = classLoader.getResourceAsStream(name);
    if (is == null) {
      System.out.println("Cannot read resource:" + name);//TODO log
      throw new IOException("Cannot read resource:" + name);
    }
    try {
      configString = new Scanner(is, Charset.defaultCharset().name()).useDelimiter("\\A").next();
    } finally {
      if (is != null) {
        is.close();
      }
    }
    return configString;
  }

  /**
   * Reads whole content of file to String
   *
   * @param fileName Existing file
   * @return File content
   * @throws IOException Thrown if data couldn't be loaded
   */
  public static String readFileAsString(File fileName) throws IOException {
    try (Scanner sc = new Scanner(fileName, Charset.defaultCharset().name())) {
      sc.useDelimiter("\\A");
      return sc.next();
    }
  }

  /**
   * Reads whole content of input stream to String
   *
   * @param stream Opened stream
   * @return Stream content
   * @throws IOException Thrown if data couldn't be loaded
   */
  public static String readStreamAsString(InputStream stream) throws IOException {
    try (Scanner sc = new Scanner(stream, Charset.defaultCharset().name())) {
      sc.useDelimiter("\\A");
      return sc.next();
    }
  }

  /**
   * <p>readFileOrResource.</p>
   *
   * @param systemPropertyName Name of system property to override default file name
   * @param defaultFileName Filename to use if system property doesn't exist.
   * @param classLoader Classloader to use
   * @return The configuration string
   * @throws IOException Thrown if data couldn't be loaded
   */
  public static String readFileOrResource(String systemPropertyName, String defaultFileName,
                                          ClassLoader classLoader) throws IOException {

    String templateFileName = System.getProperty(systemPropertyName);

    if (templateFileName == null) {
      templateFileName = defaultFileName;
    }

    File file = new File(templateFileName); // try full path

    String configString;
    if (file.exists()) {
      try {
        byte[] encoded;
        encoded = Files.readAllBytes(Paths.get(file.getPath()));
        configString = new String(encoded, Charset.defaultCharset());
      } catch (IOException e) {
        throw new IOException("Cannot load " + templateFileName + ".", e);
      }
    } else {
      try {
        configString = IoUtils.readResource(templateFileName, classLoader);
      } catch (IOException e) {
        throw new IOException("Cannot load " + templateFileName + ".", e);
      }
    }

    if (configString == null) {
      throw new IOException("Cannot load " + templateFileName + ".");
    }

    return configString;
  }

}
