package ai.context.util.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class GetFirstLine {

    public static String fromFile(String filePath){
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filePath));

            return br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }
}
