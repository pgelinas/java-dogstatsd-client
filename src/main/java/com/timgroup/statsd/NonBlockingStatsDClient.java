package com.timgroup.statsd;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;



/**
 * A simple StatsD client implementation facilitating metrics recording.
 *
 * <p>Upon instantiation, this client will establish a socket connection to a StatsD instance
 * running on the specified host and port. Metrics are then sent over this connection as they are
 * received by the client.
 * </p>
 *
 * <p>Three key methods are provided for the submission of data-points for the application under
 * scrutiny:
 * <ul>
 *   <li>{@link #incrementCounter} - adds one to the value of the specified named counter</li>
 *   <li>{@link #recordGaugeValue} - records the latest fixed value for the specified named gauge</li>
 *   <li>{@link #recordExecutionTime} - records an execution time in milliseconds for the specified named operation</li>
 *   <li>{@link #recordHistogramValue} - records a value, to be tracked with average, maximum, and percentiles</li>
 *   <li>{@link #recordEvent} - records an event</li>
 *   <li>{@link #recordSetValue} - records a value in a set</li>
 * </ul>
 * From the perspective of the application, these methods are non-blocking, with the resulting
 * IO operations being carried out in a separate thread. Furthermore, these methods are guaranteed
 * not to throw an exception which may disrupt application execution.
 *
 * <p>As part of a clean system shutdown, the {@link #stop()} method should be invoked
 * on any StatsD clients.</p>
 *
 * @author Tom Denley
 *
 */
public final class NonBlockingStatsDClient extends BackgroundStatsDClient {

    private final BlockingQueue<String> queue;

    /**
     * Create a new StatsD client communicating with a StatsD instance on the
     * specified host and port. All messages send via this client will have
     * their keys prefixed with the specified string. The new client will
     * attempt to open a connection to the StatsD server immediately upon
     * instantiation, and may throw an exception if that a connection cannot
     * be established. Once a client has been instantiated in this way, all
     * exceptions thrown during subsequent usage are consumed, guaranteeing
     * that failures in metrics will not affect normal code execution. <p>The client will use the
     * UDP protcol to communicate with the StatsD instance.</p>
     *
     * @param prefix
     *     the prefix to apply to keys sent via this client
     * @param hostname
     *     the host name of the targeted StatsD server
     * @param port
     *     the port of the targeted StatsD server
     * @throws StatsDClientException
     *     if the client could not be started
     */
    public NonBlockingStatsDClient(final String prefix, final String hostname, final int port) throws StatsDClientException {
        this(prefix, hostname, port, Integer.MAX_VALUE);
    }

    /**
     * Create a new StatsD client communicating with a StatsD instance on the
     * specified host and port. All messages send via this client will have
     * their keys prefixed with the specified string. The new client will
     * attempt to open a connection to the StatsD server immediately upon
     * instantiation, and may throw an exception if that a connection cannot
     * be established. Once a client has been instantiated in this way, all
     * exceptions thrown during subsequent usage are consumed, guaranteeing
     * that failures in metrics will not affect normal code execution. <p>The client will use the
     * UDP protcol to communicate with the StatsD instance.</p>
     *
     * @param prefix
     *     the prefix to apply to keys sent via this client
     * @param hostname
     *     the host name of the targeted StatsD server
     * @param port
     *     the port of the targeted StatsD server
     * @param queueSize
     *     the maximum amount of unprocessed messages in the BlockingQueue.
     * @throws StatsDClientException
     *     if the client could not be started
     */
    public NonBlockingStatsDClient(final String prefix, final String hostname, final int port, final int queueSize) throws StatsDClientException {
        this(prefix, hostname, port, queueSize, null, null);
    }

    /**
     * Create a new StatsD client communicating with a StatsD instance on the
     * specified host and port. All messages send via this client will have
     * their keys prefixed with the specified string. The new client will
     * attempt to open a connection to the StatsD server immediately upon
     * instantiation, and may throw an exception if that a connection cannot
     * be established. Once a client has been instantiated in this way, all
     * exceptions thrown during subsequent usage are consumed, guaranteeing
     * that failures in metrics will not affect normal code execution. <p>The client will use the
     * UDP protcol to communicate with the StatsD instance.</p>
     *
     * @param prefix
     *     the prefix to apply to keys sent via this client
     * @param hostname
     *     the host name of the targeted StatsD server
     * @param port
     *     the port of the targeted StatsD server
     * @param constantTags
     *     tags to be added to all content sent
     * @throws StatsDClientException
     *     if the client could not be started
     */
    public NonBlockingStatsDClient(final String prefix, final String hostname, final int port, final String... constantTags) throws StatsDClientException {
        this(prefix, hostname, port, Integer.MAX_VALUE, constantTags, null);
    }

    /**
     * Create a new StatsD client communicating with a StatsD instance on the
     * specified host and port. All messages send via this client will have
     * their keys prefixed with the specified string. The new client will
     * attempt to open a connection to the StatsD server immediately upon
     * instantiation, and may throw an exception if that a connection cannot
     * be established. Once a client has been instantiated in this way, all
     * exceptions thrown during subsequent usage are consumed, guaranteeing
     * that failures in metrics will not affect normal code execution. <p>The client will use the
     * UDP protcol to communicate with the StatsD instance.</p>
     *
     * @param prefix
     *     the prefix to apply to keys sent via this client
     * @param hostname
     *     the host name of the targeted StatsD server
     * @param port
     *     the port of the targeted StatsD server
     * @param constantTags
     *     tags to be added to all content sent
     * @param queueSize
     *     the maximum amount of unprocessed messages in the BlockingQueue.
     * @throws StatsDClientException
     *     if the client could not be started
     */
    public NonBlockingStatsDClient(final String prefix, final String hostname, final int port, final int queueSize, final String... constantTags) throws StatsDClientException {
        this(prefix, hostname, port, queueSize, constantTags, null);
    }

    /**
     * Create a new StatsD client communicating with a StatsD instance on the
     * specified host and port. All messages send via this client will have
     * their keys prefixed with the specified string. The new client will
     * attempt to open a connection to the StatsD server immediately upon
     * instantiation, and may throw an exception if that a connection cannot
     * be established. Once a client has been instantiated in this way, all
     * exceptions thrown during subsequent usage are passed to the specified
     * handler and then consumed, guaranteeing that failures in metrics will
     * not affect normal code execution. <p>The client will use the UDP protcol to communicate
     * with the StatsD instance.</p>
     *
     * @param prefix
     *     the prefix to apply to keys sent via this client
     * @param hostname
     *     the host name of the targeted StatsD server
     * @param port
     *     the port of the targeted StatsD server
     * @param constantTags
     *     tags to be added to all content sent
     * @param errorHandler
     *     handler to use when an exception occurs during usage, may be null to indicate noop
     * @throws StatsDClientException
     *     if the client could not be started
     */
    public NonBlockingStatsDClient(final String prefix, final String hostname, final int port,
                                   final String[] constantTags, final StatsDClientErrorHandler errorHandler) throws StatsDClientException {
        this(prefix, Integer.MAX_VALUE, constantTags, errorHandler, staticStatsDAddressResolution(hostname, port));
    }

    /**
     * Create a new StatsD client communicating with a StatsD instance on the
     * specified host and port. All messages send via this client will have
     * their keys prefixed with the specified string. The new client will
     * attempt to open a connection to the StatsD server immediately upon
     * instantiation, and may throw an exception if that a connection cannot
     * be established. Once a client has been instantiated in this way, all
     * exceptions thrown during subsequent usage are passed to the specified
     * handler and then consumed, guaranteeing that failures in metrics will
     * not affect normal code execution. <p>The client will use the UDP protcol to communicate
     * with the StatsD instance.</p>
     *
     * @param prefix
     *     the prefix to apply to keys sent via this client
     * @param hostname
     *     the host name of the targeted StatsD server
     * @param port
     *     the port of the targeted StatsD server
     * @param constantTags
     *     tags to be added to all content sent
     * @param errorHandler
     *     handler to use when an exception occurs during usage, may be null to indicate noop
     * @param queueSize
     *     the maximum amount of unprocessed messages in the BlockingQueue.
     * @throws StatsDClientException
     *     if the client could not be started
     */
    public NonBlockingStatsDClient(final String prefix, final String hostname, final int port, final int queueSize,
                                   final String[] constantTags, final StatsDClientErrorHandler errorHandler) throws StatsDClientException {
        this(prefix, queueSize, constantTags, errorHandler, staticStatsDAddressResolution(hostname, port));
    }

    /**
     * Create a new StatsD client communicating with a StatsD instance on the
     * specified host and port. All messages send via this client will have
     * their keys prefixed with the specified string. The new client will
     * attempt to open a connection to the StatsD server immediately upon
     * instantiation, and may throw an exception if that a connection cannot
     * be established. Once a client has been instantiated in this way, all
     * exceptions thrown during subsequent usage are passed to the specified
     * handler and then consumed, guaranteeing that failures in metrics will
     * not affect normal code execution. <p>The client will use the UDP protcol to communicate
     * with the StatsD instance.</p>
     *
     *
     * @param prefix
     *     the prefix to apply to keys sent via this client
     * @param constantTags
     *     tags to be added to all content sent
     * @param errorHandler
     *     handler to use when an exception occurs during usage, may be null to indicate noop
     * @param addressLookup
     *     yields the IP address and socket of the StatsD server
     * @param queueSize
     *     the maximum amount of unprocessed messages in the BlockingQueue.
     * @throws StatsDClientException
     *     if the client could not be started
     */
    public NonBlockingStatsDClient(String prefix, int queueSize, String[] constantTags,
        StatsDClientErrorHandler errorHandler, Callable<InetSocketAddress> addressLookup) throws
        StatsDClientException {
        this(prefix, queueSize, constantTags, errorHandler, createStatsDProtocol(addressLookup,
            errorHandler
        ));
    }

    /**
     * Create a new StatsD client communicating with a StatsD instance using the specified protocol.
     * All messages send via this client will have their keys prefixed with the specified string.
     * Once a client has been instantiated in this way, all exceptions thrown during subsequent
     * usage are passed to the specified handler and then consumed, guaranteeing that failures in
     * metrics will not affect normal code execution. <p>Prefer using the {@link
     * StatsDClientBuilder} over this constructor.</p>
     *
     * @param prefix
     *     the prefix to apply to keys sent via this client
     * @param queueSize
     *     the maximum amount of unprocessed messages in the BlockingQueue.
     * @param constantTags
     *     tags to be added to all content sent
     * @param errorHandler
     *     handler to use when an exception occurs during usage, may be null to indicate noop
     * @param protocol
     *     the underlying protocol to use for communication.
     * @throws StatsDClientException
     *     if the client could not be started
     */
    public NonBlockingStatsDClient(final String prefix, final int queueSize, String[] constantTags,
        final StatsDClientErrorHandler errorHandler, Protocol protocol)
        throws StatsDClientException {
        super(prefix, constantTags, errorHandler);

        queue = new LinkedBlockingQueue<>(queueSize);
        executor.submit(new QueueConsumer(protocol));
    }

    @Override
    protected void send(final String message) {
        queue.offer(message);
    }

    private class QueueConsumer implements Runnable {

        private final Protocol protocol;

        QueueConsumer(final Protocol protocol) {
            this.protocol = protocol;
        }

        @Override
        public void run() {
            // Ensure that even if the executor/client is stopped, we send all accumulated metric
            // before stopping the background IO Thread.
            while (!executor.isShutdown() || !queue.isEmpty()) {
                try {
                    final String message = queue.poll(1, TimeUnit.SECONDS);
                    if (null != message) {
                        protocol.send(message);
                        if (null == queue.peek()) {
                            protocol.flush();
                        }
                    }
                } catch (final Exception e) {
                    handler.handle(e);
                }
            }
        }
    }

}
