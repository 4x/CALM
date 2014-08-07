package ai.context.util.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CSVConverter {
    public static void main(String[] args){

        SimpleDateFormat inFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        SimpleDateFormat outFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        BufferedReader br = null;
        Date start = null;
        try {
            start = outFormat.parse("2014.07.23 19:00:00");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {

            String sCurrentLine;

            br = new BufferedReader(new FileReader("/opt/dev/data/feeds/USDJPY_UTC_30 Mins_Bid_2014.07.23_2014.08.02.csv"));
            br.readLine();
            while ((sCurrentLine = br.readLine()) != null) {
                String[] parts = sCurrentLine.split(",");

                Date time = inFormat.parse(parts[0]);
                if(time.after(start)){
                    String out = outFormat.format(time);

                    for(int i = 1; i < parts.length; i++){
                        out += "," + parts[i];
                    }
                    System.out.println(out);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
