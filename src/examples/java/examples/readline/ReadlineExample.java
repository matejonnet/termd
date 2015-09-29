package examples.readline;

import io.termd.core.readline.Function;
import io.termd.core.readline.Keymap;
import io.termd.core.readline.Readline;
import io.termd.core.tty.TtyConnection;

/**
 * Shows how to use async Readline.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineExample {

  public static void handle(TtyConnection conn) {
    readline(
        new Readline(Keymap.getDefault()).addFunctions(Function.loadDefaults()),
        conn);
  }

  private static void readline(Readline readline, TtyConnection conn) {
    readline.readline(conn, "% ", line -> {
      if (line == null) {
        conn.write("Logout").close();
      } else {
        conn.write("User entered " + line + "\n");
        readline(readline, conn);
      }
    });
  }
}
