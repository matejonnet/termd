package io.termd.core.http;

import io.termd.core.io.BinaryDecoder;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by <a href="mailto:matejonnet@gmail.com">Matej Lazar</a> on 2015-04-23.
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
}
