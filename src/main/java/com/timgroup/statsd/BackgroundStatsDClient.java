package com.timgroup.statsd;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public abstract class BackgroundStatsDClient extends DefaultStatsDClient {

  protected final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
    final ThreadFactory delegate = Executors.defaultThreadFactory();

    @Override
    public Thread newThread(final Runnable r) {
      final Thread result1 = delegate.newThread(r);
      result1.setName("StatsD-" + result1.getName());
      result1.setDaemon(true);
      return result1;
    }
  });

  public BackgroundStatsDClient(String prefix, String[] constantTags,
      StatsDClientErrorHandler errorHandler) {
    super(prefix, constantTags, errorHandler);
  }

  /**
   * Cleanly shut down this StatsD client. This method may throw an exception if
   * the socket cannot be closed.
   */
  @Override
  public void stop() {
      try {
          executor.shutdown();
          executor.awaitTermination(30, TimeUnit.SECONDS);
      }
      catch (final Exception e) {
          handler.handle(e);
      }
      finally {
          super.stop();
      }
  }
}