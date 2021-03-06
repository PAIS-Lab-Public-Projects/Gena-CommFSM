/*
 *  MIT License
 *  Copyright (c) 2017 PAIS-Lab-Public-Projects
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package cfsm.dist;

import cfsm.domain.CFSMConfiguration;
import cfsm.engine.CmdOptions;
import cfsm.engine.Mining;
import cfsm.parser.Parser;
import cfsm.syntax.SyntaxChecker;
import io.vertx.core.json.JsonObject;
import org.apache.commons.cli.*;
import scala.Option;
import scala.Tuple2;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

    public static final Long DefaultMaxEvents = 10000L;
    public static final Long DefaultCases = 1L;

    public static void main(String[] args) {
        init(args);
    }

    private static void init(String[] args) {
        try {

            // options specification
            Options options = new Options();

            options.addOption("f", "file", true, "Path to file with description of model");
            options.addOption("h", "help", false, "Print help message");
            options.addOption("d", "destination", true, "Path where generated logs will be stored");
            options.addOption("csv", false, "Format of logs is csv. Work only with '-d' flag");
            options.addOption("ss", "show-states", false, "Print states in csv log." +
                    " Works only with '-d' and '-csv' flag");
            options.addOption("sc", "show-conditions", false, "Print conditions in csv log." +
                    " Works only with '-d' and '-csv' flag");
            options.addOption("elim", "max-events", true, "maximum amount of events inside one generation session");
            options.addOption("c", "cases", true, "amount of cases to generate");
            options.addOption("nv", "no-validation", false, "should config file be validated");

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            // if launched with -help
            if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("cfsm", options);
                return;
            }

            String file = cmd.getOptionValue("file");
            String dest = cmd.getOptionValue("destination");
            Boolean csv = cmd.hasOption("csv");
            Long maxEvents = cmd.hasOption("elim") ? Long.parseLong(cmd.getOptionValue("elim")) : DefaultMaxEvents;
            Long cases = cmd.hasOption("c") ? Long.parseLong(cmd.getOptionValue("c")) : DefaultCases;

            CmdOptions cmdOptions = new CmdOptions(
                    cmd.hasOption("show-states"),
                    cmd.hasOption("show-conditions"),
                    maxEvents,
                    cases
            );

            // check for -file option
            if (file == null) {
                System.out.println("-file options is not specified");
                return;
            }

            System.out.println("Specified path to file is: " + file);

            // check file for existence
            if (!new File(file).exists()) {
                System.out.println(String.format("File \"%s\" is not exist", file));
                return;
            }

            System.out.println("Parsing configuration file......");

            Tuple2<Option<JsonObject>, String> res = SyntaxChecker.rawParse(new File(file).getAbsolutePath());

            // print result of parsing
            System.out.println("Valid JSON: " + res._2);
            String check = SyntaxChecker.check(res._1.get());
            System.out.println("Valid syntax: " + check);
            if (res._1.isEmpty() || !check.equals(SyntaxChecker.OK())) {
                return;
            }

            CFSMConfiguration configuration = Parser.parse(res._1.get());
            if (!cmd.hasOption("nv")) {
                String validate = Parser.validate(configuration);
                System.out.println("Valid config objects: " + validate);
                if (!validate.equals(SyntaxChecker.OK())) {
                    return;
                }
            }

            System.out.println(String.format("%d machines found", configuration.machines.size()));

            AtomicLong caseId = new AtomicLong(0);
            AtomicLong eventId = new AtomicLong(0);

            // begin mining
            Mining.begin(configuration, dest, csv, caseId, eventId, cmdOptions);
        } catch (ParseException e) {
            System.out.println("Not able to parse given options. Please, use help for details");
            e.printStackTrace();
        }
    }

}