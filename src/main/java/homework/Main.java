package homework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Main {
    final static String FILE = "/Yvar14.txt";

    public static void main(String[] args) {
        InputStream inputStream = Main.class.getResourceAsStream(FILE);
        if (inputStream == null) {
            System.out.printf("Failed to load file (%s)%n", FILE);
            return;
        }

        InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        try {
            for (String line; (line = reader.readLine()) != null;) {
                System.out.println(line);
            }
        } catch (IOException exception) {
            System.out.printf("%s: failed to read line%n", exception.getMessage());
        }
    }
}