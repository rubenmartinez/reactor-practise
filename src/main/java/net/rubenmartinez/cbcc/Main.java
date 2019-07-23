package net.rubenmartinez.cbcc;

import com.beust.jcommander.JCommander;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        CommandLineArgs commandLineArgs = new CommandLineArgs();
        JCommander.newBuilder()
                .addObject(commandLineArgs)
                .build()
                .parse(args);


        System.out.println("endDateTime: " + commandLineArgs.getEndDatetime());

        try {
            Stream<String> lines = Files.lines(Path.of("Instructions/input-file-10000.txt"));



        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
