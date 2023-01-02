package org.brianbruce.bio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by kezhenxu94 in 2018/8/28 21:58
 *
 * @author kezhenxu94 (kezhenxu94 at 163 dot com)
 */
public class EchoClient {
  private static final Logger LOGGER = LogManager.getLogger(EchoClient.class);

  private static final String POISON_PILL = "BYE";

  private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

  public void start() throws IOException, InterruptedException {
    final var socket = new Socket();
    socket.connect(new InetSocketAddress(8080));

    final Thread readerThread = new Thread(new ReaderTask());
    readerThread.setDaemon(true);
    readerThread.start();

    final var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    final var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    final char[] buff = new char[1024];

    for (var msg = queue.take(); !Thread.interrupted(); msg = queue.take()) {
      LOGGER.info("===> " + msg);

      writer.write(msg);
      // writer.newLine();
      writer.flush();

      // final var response = reader.readLine();


      final var charsRead = reader.read(buff);
      String response;
      if (charsRead != -1) {
        response = new String(buff, 0, charsRead);
      } else {
        response = "";
      }

      LOGGER.info("<=== " + response);

      if (response.equals(POISON_PILL)) {
        break;
      }
    }
    socket.close();
    LOGGER.info("Socket closed");
  }

  private class ReaderTask implements Runnable {
    @Override
    public void run() {
      LOGGER.info("ReaderTask starting...");
      try (final var userReader = new BufferedReader(new InputStreamReader(System.in))) {
        for (var line = userReader.readLine(); line != null; line = userReader.readLine()) {
          queue.put(line);
          LOGGER.info("Put a line...");
        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    new EchoClient().start();
  }
}
