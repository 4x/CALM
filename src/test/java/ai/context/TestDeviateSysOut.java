package ai.context;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class TestDeviateSysOut {

    @Test
    public void testDeviation() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // IMPORTANT: Save the old System.out!
        PrintStream old = System.out;
        // Tell Java to use your special stream
        System.setOut(ps);
        // Print some output: goes to your special stream
        System.out.println("Foofoofoo!");
        // Put things back
        ps.flush();
        System.setOut(old);
        // Show what happened
        System.out.println("Here: " + baos.toString());
        baos.reset();

        System.setOut(ps);
        System.out.println("XXXXX!");
        ps.flush();
        System.setOut(old);
        System.out.println("Here: " + baos.toString());
    }

}
