package org.brianbruce.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by kezhenxu94 in 2018/8/28 21:53
 *
 * @author kezhenxu94 (kezhenxu94 at 163 dot com)
 */
public class EchoServer {
  private static final Logger LOGGER = LogManager.getLogger(EchoServer.class);

  private static final String POISON_PILL = "BYE";

  public void start() throws Exception {
    try (final var serverSocketChannel = ServerSocketChannel.open()) {
      final var selector = Selector.open();

      serverSocketChannel.bind(new InetSocketAddress(8080));
      serverSocketChannel.configureBlocking(false);
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

      while (!Thread.interrupted()) {
        if (selector.select(1000L) == 0) {
          continue;
        }
        for (final var it = selector.selectedKeys().iterator(); it.hasNext(); it.remove()) {
          final var key = it.next();
          if (key.isAcceptable()) {
            LOGGER.info("Accept");
            final var server = (ServerSocketChannel) key.channel();
            final var client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                ByteBuffer.allocate(1024));
            LOGGER.info("client connected: " + client);
          }
          if (key.isReadable()) {
            readData(key);
          }
          if (key.isWritable()) {
            writeData(key);
          }
        }
      }
    }
  }

  private void writeData(final SelectionKey key) throws Exception {
    final var channel = (SocketChannel) key.channel();
    final var buffer = (ByteBuffer) key.attachment();

    try {
      // LOGGER.info("Writing");
      buffer.flip();
      if (!buffer.hasRemaining()) {
        // LOGGER.info("Nothing to write");
        return;
      }

      final var s = new String(buffer.array(), buffer.arrayOffset(), buffer.remaining()).trim();
      LOGGER.info("===> " + s);

      channel.write(buffer);

      if (s.equals(POISON_PILL)) {
        channel.close();
        LOGGER.info(channel + " closed");
      }
    } finally {
      buffer.clear();
    }
  }

  private void readData(final SelectionKey key) throws Exception {
    final var buffer = ((ByteBuffer) key.attachment());
    final var channel = (SocketChannel) key.channel();
    final var read = channel.read(buffer);
    LOGGER.info("Reading");
    if (read <= 0) {
      LOGGER.info("Nothing to read");
      return;
    }
    final var s = new String(buffer.array(), 0, read).trim();
    LOGGER.info("<=== " + s);
  }

  public static void main(String[] args) throws Exception {
    new EchoServer().start();
  }
}
