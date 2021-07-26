package aleksey.vasiliev;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;

import java.io.EOFException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.abs;
import static java.lang.System.currentTimeMillis;

public class Sniffer {
    private static Sniffer instance = null;
    private static final String READ_TIMEOUT_KEY = Sniffer.class.getName() + ".readTimeout";
    private static final int READ_TIMEOUT = Integer.getInteger(READ_TIMEOUT_KEY, 10);
    private static final String SNAPLEN_KEY = Sniffer.class.getName() + ".snaplen";
    private static final int SNAPLEN = Integer.getInteger(SNAPLEN_KEY, 65536);
    private static final AtomicInteger minLimit = new AtomicInteger(0);
    private static final AtomicInteger maxLimit = new AtomicInteger(0);
    private static final AtomicBoolean minAlertSent = new AtomicBoolean(false);
    private static final AtomicBoolean maxAlertSent = new AtomicBoolean(false);
    private static final AtomicInteger currentTrafficUsage = new AtomicInteger(0);
    private static final ConcurrentHashMap<String, AtomicBoolean> alertSent = new ConcurrentHashMap<>();
    private static final AtomicLong currentTime = new AtomicLong(0);
    private static final AtomicInteger currentFiveMinute = new AtomicInteger(0);
    private final Properties props = new Properties();

    private final DataBase db;
    private final String ip;
    private List<PcapNetworkInterface> PcapNetworkInterfaces = null;

    private Sniffer(String ip, DataBase db) {
        this.db = db;
        this.ip = ip;
        currentTime.set(currentTimeMillis());
        try {
            this.PcapNetworkInterfaces = Pcaps.findAllDevs();
        } catch (PcapNativeException e) {
            System.out.println("Either native library isn't available or internet adapters are not connected.");
        }
        assert PcapNetworkInterfaces != null;
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    }

    public static synchronized Sniffer getInstance(String ip, DataBase db) {
        if (instance == null) {
            minLimit.set(db.getLimit(DataBase.LimitType.MIN));
            maxLimit.set(db.getLimit(DataBase.LimitType.MAX));
            alertSent.put("min", new AtomicBoolean(false));
            alertSent.put("max", new AtomicBoolean(false));
            instance = new Sniffer(ip, db);
        }
        return instance;
    }

    private void writeToTopic(String limit, int trafficUsed, int trafficAvailable) {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date date = new Date();
        String message = String.format("%s limit exceeded, %d/%d. %s", limit, trafficUsed, trafficAvailable, df.format(date));
        Producer<String, String> producer = new KafkaProducer<>(props);
        producer.send(new ProducerRecord<>("alerts", "Limit exceeded!", message));
        System.out.printf("\"%s\" with key \"Limit exceeded!\" was sent to topic alerts.%n", message);
        producer.close();
    }

    public void sniff() {
        for (PcapNetworkInterface nif : PcapNetworkInterfaces) {
            if (nif.getName().equals("bluetooth-monitor") ||
                    nif.getName().equals("nflog") ||
                    nif.getName().equals("nfqueue") ||
                    nif.getName().equals("sit0")) {
                continue;
            }
            Thread thread = new Thread(() -> {
                PcapHandle handle;
                try {
                    handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
                    handle.setFilter(ip, BpfProgram.BpfCompileMode.OPTIMIZE);
                    while (true) {
                        if (abs((currentTimeMillis() - currentTime.get()) / (60 * 1000)) % 60 > currentFiveMinute.get()) {
                            if (currentTrafficUsage.get() > minLimit.get() && !minAlertSent.get()) {
                                writeToTopic("Min", currentTrafficUsage.get(), minLimit.get());
                                minAlertSent.set(true);
                            }
                            if (currentTrafficUsage.get() > maxLimit.get() && !maxAlertSent.get()) {
                                writeToTopic("Max", currentTrafficUsage.get(), maxLimit.get());
                                maxAlertSent.set(true);
                            }
                            if (currentFiveMinute.get() == 12) {
                                updateNewHour();
                            }
                            if (currentFiveMinute.get() % 4 == 0) {
                                updateTrafficLimits();
                            }
                            currentFiveMinute.getAndAdd(1);
                        }
                        try {
                            Packet packet = handle.getNextPacketEx();
                            currentTrafficUsage.getAndAdd(packet.length());
                        } catch (TimeoutException ignored) {
                        } catch (EOFException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (PcapNativeException | NotOpenException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }
    }

    private static void updateNewHour() {
        currentTime.getAndAdd(60 * 60 * 1000);
        currentTrafficUsage.set(0);
        currentFiveMinute.set(0);
        minAlertSent.set(false);
        maxAlertSent.set(false);
    }

    private void updateTrafficLimits() {
        minLimit.set(db.getLimit(DataBase.LimitType.MIN));
        maxLimit.set(db.getLimit(DataBase.LimitType.MAX));
    }
}