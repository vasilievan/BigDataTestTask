package aleksey.vasiliev;

import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Main {
    private static final Options summaryOptions = new Options();

    public static void main(String[] args) {
        System.out.println("Server is working.");
        setUp();
        DataBase db = DataBase.getInstance();
        ArrayList<String> parsedArgs = parseArgs(args);
        String filterString = "";
        if (parsedArgs.size() > 0) {
            filterString = String.format(" src net %s", parsedArgs.get(0));
        }
        Sniffer sniffer = Sniffer.getInstance(filterString, db);
        sniffer.sniff();
    }

    private static ArrayList<String> parseArgs(String[] args) {
        ArrayList<String> parsedArguments = new ArrayList<>();
        setUp();
        DefaultParser defaultParser = new DefaultParser();
        try {
            CommandLine parsedCmdLine = defaultParser.parse(summaryOptions, args);
            if (parsedCmdLine.hasOption("a")) {
                String ip = parsedCmdLine.getOptionValues("a")[0];
                if (Pattern.matches("((^\\s*((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|" +
                        "[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))\\s*$)|(^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]" +
                        "{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
                        "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]" +
                        "{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
                        "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}" +
                        "(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
                        "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}" +
                        "(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
                        "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}" +
                        "(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
                        "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}" +
                        "(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)" +
                        "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|" +
                        "((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|" +
                        "2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\\s*$))", ip)) {
                    parsedArguments.add(ip);
                } else {
                    System.out.println("Incorrect IPv4 or IPv6 address.");
                }
            }
            return parsedArguments;
        } catch (ParseException e) {
            System.out.println("Incorrect arguments.");
        }
        return parsedArguments;
    }

    private static void setUp() {
        Option ip = new Option("a", "address", true, "Write ip address to filer.");
        summaryOptions.addOption(ip);
    }
}