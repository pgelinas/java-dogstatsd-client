package com.timgroup.statsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;

import java.net.SocketException;
import java.util.Arrays;
import java.util.Locale;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class NonBlockingStatsDClientTest {

    private static final int STATSD_SERVER_PORT = 17254;

    private static DummyStatsDServer server;
    public static final String LOCALHOST = "localhost";
    public static final String PREFIX = "my.prefix";

    private final static StatsDClientBuilder builder;
    private final static StatsDClient nonBlockingClient;
    private final static StatsDClient blockingClient;
    private final static StatsDClient concurrentClient;

    static {
        builder = new StatsDClientBuilder().prefix(PREFIX)
            .hostname(LOCALHOST)
            .port(STATSD_SERVER_PORT)
            .udpProtocol();
        blockingClient = builder.buildBlocking();
        nonBlockingClient = builder.buildNonBlocking();
        concurrentClient = builder.buildConcurrent(10);
        empty_prefix_client = builder.prefix("").buildBlocking();
        null_prefix_client = builder.prefix(null).buildBlocking();
        constant_tags_client = builder.prefix(PREFIX).constantTags("instance:foo", "app:bar")
            .buildBlocking();
    }

    private static StatsDClient empty_prefix_client;
    private static StatsDClient null_prefix_client;
    private static StatsDClient constant_tags_client;

    @Parameters(name="{0}")
    public static Iterable<? extends StatsDClient> createClient() {
        return Arrays.asList(nonBlockingClient, blockingClient, concurrentClient);
    }

    @Parameter
    public StatsDClient client;

    @BeforeClass
    public static void start() throws SocketException {
        server = new DummyStatsDServer(STATSD_SERVER_PORT);
    }

    @AfterClass
    public static void stop() throws Exception {
        nonBlockingClient.stop();
        blockingClient.stop();
        concurrentClient.stop();
        server.close();
    }

    @After
    public void clear() {
        server.clear();
    }

    @Test(timeout=5000L) public void
    sends_counter_value_to_statsd() throws Exception {


        client.count("mycount", 24);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mycount:24|c"));
    }

    @Test(timeout=5000L) public void
    sends_counter_value_with_sample_rate_to_statsd() throws Exception {

    	client.count("mycount", 24, 1);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mycount:24|c|@1.000000"));
    }

    @Test(timeout=5000L) public void
    sends_counter_value_to_statsd_with_null_tags() throws Exception {


        client.count("mycount", 24, (java.lang.String[]) null);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mycount:24|c"));
    }

    @Test(timeout=5000L) public void
    sends_counter_value_to_statsd_with_empty_tags() throws Exception {


        client.count("mycount", 24);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mycount:24|c"));
    }

    @Test(timeout=5000L) public void
    sends_counter_value_to_statsd_with_tags() throws Exception {


        client.count("mycount", 24, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mycount:24|c|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_counter_value_with_sample_rate_to_statsd_with_tags() throws Exception {


        client.count("mycount", 24, 1, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mycount:24|c|@1.000000|#baz,foo:bar"));
    }


    @Test(timeout=5000L) public void
    sends_counter_increment_to_statsd() throws Exception {


        client.incrementCounter("myinc");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myinc:1|c"));
    }

    @Test(timeout=5000L) public void
    sends_counter_increment_to_statsd_with_tags() throws Exception {


        client.incrementCounter("myinc", "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myinc:1|c|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_counter_increment_with_sample_rate_to_statsd_with_tags() throws Exception {


        client.incrementCounter("myinc", 1, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myinc:1|c|@1.000000|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_counter_decrement_to_statsd() throws Exception {


        client.decrementCounter("mydec");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mydec:-1|c"));
    }

    @Test(timeout=5000L) public void
    sends_counter_decrement_to_statsd_with_tags() throws Exception {


        client.decrementCounter("mydec", "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mydec:-1|c|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_counter_decrement_with_sample_rate_to_statsd_with_tags() throws Exception {


        client.decrementCounter("mydec", 1, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mydec:-1|c|@1.000000|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_gauge_to_statsd() throws Exception {


        client.recordGaugeValue("mygauge", 423);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mygauge:423|g"));
    }

    @Test(timeout=5000L) public void
    sends_gauge_with_sample_rate_to_statsd() throws Exception {


        client.recordGaugeValue("mygauge", 423, 1);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mygauge:423|g|@1.000000"));
    }

    @Test(timeout=5000L) public void
    sends_large_double_gauge_to_statsd() throws Exception {


        client.recordGaugeValue("mygauge", 123456789012345.67890);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mygauge:123456789012345.67|g"));
    }

    @Test(timeout=5000L) public void
    sends_exact_double_gauge_to_statsd() throws Exception {


        client.recordGaugeValue("mygauge", 123.45678901234567890);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mygauge:123.456789|g"));
    }

    @Test(timeout=5000L) public void
    sends_double_gauge_to_statsd() throws Exception {


        client.recordGaugeValue("mygauge", 0.423);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mygauge:0.423|g"));
    }

    @Test(timeout=5000L) public void
    sends_gauge_to_statsd_with_tags() throws Exception {


        client.recordGaugeValue("mygauge", 423, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mygauge:423|g|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_gauge_with_sample_rate_to_statsd_with_tags() throws Exception {


        client.recordGaugeValue("mygauge", 423, 1, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mygauge:423|g|@1.000000|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_double_gauge_to_statsd_with_tags() throws Exception {


        client.recordGaugeValue("mygauge", 0.423, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mygauge:0.423|g|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_histogram_to_statsd() throws Exception {

        client.recordHistogramValue("myhistogram", 423);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myhistogram:423|h"));
    }

    @Test(timeout=5000L) public void
    sends_double_histogram_to_statsd() throws Exception {


        client.recordHistogramValue("myhistogram", 0.423);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myhistogram:0.423|h"));
    }

    @Test(timeout=5000L) public void
    sends_histogram_to_statsd_with_tags() throws Exception {


        client.recordHistogramValue("myhistogram", 423, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myhistogram:423|h|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_histogram_with_sample_rate_to_statsd_with_tags() throws Exception {


        client.recordHistogramValue("myhistogram", 423, 1, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myhistogram:423|h|@1.000000|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_double_histogram_to_statsd_with_tags() throws Exception {


        client.recordHistogramValue("myhistogram", 0.423, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myhistogram:0.423|h|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_double_histogram_with_sample_rate_to_statsd_with_tags() throws Exception {


        client.recordHistogramValue("myhistogram", 0.423, 1, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myhistogram:0.423|h|@1.000000|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_distribtuion_to_statsd() throws Exception {

        client.recordDistributionValue("mydistribution", 423);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mydistribution:423|d"));
    }

    @Test(timeout=5000L) public void
    sends_double_distribution_to_statsd() throws Exception {


        client.recordDistributionValue("mydistribution", 0.423);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mydistribution:0.423|d"));
    }

    @Test(timeout=5000L) public void
    sends_distribution_to_statsd_with_tags() throws Exception {


        client.recordDistributionValue("mydistribution", 423, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mydistribution:423|d|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_distribution_with_sample_rate_to_statsd_with_tags() throws Exception {


        client.recordDistributionValue("mydistribution", 423, 1, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mydistribution:423|d|@1.000000|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_double_distribution_to_statsd_with_tags() throws Exception {


        client.recordDistributionValue("mydistribution", 0.423, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mydistribution:0.423|d|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_double_distribution_with_sample_rate_to_statsd_with_tags() throws Exception {


        client.recordDistributionValue("mydistribution", 0.423, 1, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mydistribution:0.423|d|@1.000000|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_timer_to_statsd() throws Exception {


        client.recordExecutionTime("mytime", 123);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mytime:123|ms"));
    }

    /**
     * A regression test for <a href="https://github.com/indeedeng/java-dogstatsd-client/issues/3">this i18n number formatting bug</a>
     * @throws Exception
     */
    @Test public void
    sends_timer_to_statsd_from_locale_with_unamerican_number_formatting() throws Exception {

        Locale originalDefaultLocale = Locale.getDefault();

        // change the default Locale to one that uses something other than a '.' as the decimal separator (Germany uses a comma)
        Locale.setDefault(Locale.GERMANY);

        try {


            client.recordExecutionTime("mytime", 123, "foo:bar", "baz");
            server.waitForMessage();

            assertThat(server.messagesReceived(), contains("my.prefix.mytime:123|ms|#baz,foo:bar"));
        } finally {
            // reset the default Locale in case changing it has side-effects
            Locale.setDefault(originalDefaultLocale);
        }
    }


    @Test(timeout=5000L) public void
    sends_timer_to_statsd_with_tags() throws Exception {


        client.recordExecutionTime("mytime", 123, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mytime:123|ms|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_timer_with_sample_rate_to_statsd_with_tags() throws Exception {


        client.recordExecutionTime("mytime", 123, 1, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mytime:123|ms|@1.000000|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_event() throws Exception {

        final Event event = Event.builder()
                .withTitle("title1")
                .withText("text1\nline2")
                .withDate(1234567000)
                .withHostname("host1")
                .withPriority(Event.Priority.LOW)
                .withAggregationKey("key1")
                .withAlertType(Event.AlertType.ERROR)
                .build();
        client.recordEvent(event);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("_e{16,12}:my.prefix.title1|text1\\nline2|d:1234567|h:host1|k:key1|p:low|t:error"));
    }

    @Test(timeout=5000L) public void
    sends_partial_event() throws Exception {

        final Event event = Event.builder()
                .withTitle("title1")
                .withText("text1")
                .withDate(1234567000)
                .build();
        client.recordEvent(event);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("_e{16,5}:my.prefix.title1|text1|d:1234567"));
    }

    @Test(timeout=5000L) public void
    sends_event_with_tags() throws Exception {

        final Event event = Event.builder()
                .withTitle("title1")
                .withText("text1")
                .withDate(1234567000)
                .withHostname("host1")
                .withPriority(Event.Priority.LOW)
                .withAggregationKey("key1")
                .withAlertType(Event.AlertType.ERROR)
                .build();
        client.recordEvent(event, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("_e{16,5}:my.prefix.title1|text1|d:1234567|h:host1|k:key1|p:low|t:error|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_partial_event_with_tags() throws Exception {

        final Event event = Event.builder()
                .withTitle("title1")
                .withText("text1")
                .withDate(1234567000)
                .build();
        client.recordEvent(event, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("_e{16,5}:my.prefix.title1|text1|d:1234567|#baz,foo:bar"));
    }

    @Test(timeout=5000L) public void
    sends_service_check() throws Exception {
        final String inputMessage = "\u266c \u2020\u00f8U \n\u2020\u00f8U \u00a5\u00bau|m: T0\u00b5 \u266a"; // "♬ †øU \n†øU ¥ºu|m: T0µ ♪"
        final String outputMessage = "\u266c \u2020\u00f8U \\n\u2020\u00f8U \u00a5\u00bau|m\\: T0\u00b5 \u266a"; // note the escaped colon
        final String[] tags = {"key1:val1", "key2:val2"};
        final ServiceCheck sc = ServiceCheck.builder()
                .withName("my_check.name")
                .withStatus(ServiceCheck.Status.WARNING)
                .withMessage(inputMessage)
                .withHostname("i-abcd1234")
                .withTags(tags)
                .withTimestamp(1420740000)
                .build();

        assertEquals(outputMessage, sc.getEscapedMessage());

        client.serviceCheck(sc);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains(String.format("_sc|my_check.name|1|d:1420740000|h:i-abcd1234|#key2:val2,key1:val1|m:%s",
                outputMessage)));
    }

    @Test(timeout=5000L) public void
    sends_nan_gauge_to_statsd() throws Exception {
        client.recordGaugeValue("mygauge", Double.NaN);

        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.mygauge:NaN|g"));
    }

    @Test(timeout=5000L) public void
    sends_set_to_statsd() throws Exception {
        client.recordSetValue("myset", "myuserid");

        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myset:myuserid|s"));

    }

    @Test(timeout=5000L) public void
    sends_set_to_statsd_with_tags() throws Exception {
        client.recordSetValue("myset", "myuserid", "foo:bar", "baz");

        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("my.prefix.myset:myuserid|s|#baz,foo:bar"));

    }

    // Non-parameterized test to check specific features of DefaultStatsDClient.
    @Test(timeout = 5000L)
    public void
    sends_gauge_mixed_tags() throws Exception {
        constant_tags_client.gauge("value", 423, "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(),
            contains("my.prefix.value:423|g|#app:bar,instance:foo,baz"));
    }

    @Test(timeout = 5000L)
    public void
    sends_gauge_mixed_tags_with_sample_rate() throws Exception {
        constant_tags_client.gauge("value", 423, 1, "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(),
            contains("my.prefix.value:423|g|@1.000000|#app:bar,instance:foo,baz"));
    }

    @Test(timeout = 5000L)
    public void
    sends_gauge_constant_tags_only() throws Exception {
        constant_tags_client.gauge("value", 423);
        server.waitForMessage();

        assertThat(server.messagesReceived(),
            contains("my.prefix.value:423|g|#app:bar,instance:foo"));
    }

    @Test(timeout = 5000L)
    public void
    sends_gauge_empty_prefix() throws Exception {
        empty_prefix_client.gauge("top.level.value", 423);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("top.level.value:423|g"));
    }

    @Test(timeout = 5000L)
    public void
    sends_gauge_null_prefix() throws Exception {
        null_prefix_client.gauge("top.level.value", 423);
        server.waitForMessage();

        assertThat(server.messagesReceived(), contains("top.level.value:423|g"));
    }

    @Test(timeout = 5000L)
    public void
    sends_event_empty_prefix() throws Exception {

        final Event event = Event.builder()
            .withTitle("title1")
            .withText("text1")
            .withDate(1234567000)
            .withHostname("host1")
            .withPriority(Event.Priority.LOW)
            .withAggregationKey("key1")
            .withAlertType(Event.AlertType.ERROR)
            .build();
        empty_prefix_client.recordEvent(event, "foo:bar", "baz");
        server.waitForMessage();

        assertThat(server.messagesReceived(),
            contains("_e{6,5}:title1|text1|d:1234567|h:host1|k:key1|p:low|t:error|#baz,foo:bar"));
    }
}
