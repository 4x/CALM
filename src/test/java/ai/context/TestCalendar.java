package ai.context;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestCalendar {

    public static void main(String[] args){

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        try {
            InputStream input = new URL("http://www.fxstreet.com/economic-calendar/calendar.ics").openStream();
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(input);

            for(int i = 0; i < calendar.getComponents().size(); i++){
                Component c = (Component) calendar.getComponents().get(i);

                Property dStr = c.getProperties().getProperty("DTSTAMP");

                //System.out.println(c);
                if(dStr != null){
                    long t = format.parse(dStr.getValue()).getTime();
                    String location = c.getProperties().getProperty("LOCATION").getValue();
                    String type = c.getProperties().getProperty("SUMMARY").getValue().substring(4).split(" \\[")[0];
                    System.out.println(new Date(t) + "," + location + "," + type);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
