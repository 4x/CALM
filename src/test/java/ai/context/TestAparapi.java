package ai.context;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.Range;

/**
 * An example Aparapi application which computes and displays squares of a set of 512 input values.
 * While executing on GPU using Aparpi framework, each square value is computed in a separate kernel invocation and
 * can thus maximize performance by optimally utilizing all GPU computing units
 *
 * @author gfrost
 *
 */

public class TestAparapi {

    static final int size = 8192 * 8192;

    public static void main(String[] _args) {
        runOnGPU();
        runNormally();
    }

    public static void runOnGPU(){

        long t = System.nanoTime();

        /** Input float array for which square values need to be computed. */
        final float[] values = new float[size];

        /** Initialize input array. */
        for (int i = 0; i < size; i++) {
            values[i] = i;
        }

        /** Output array which will be populated with square values of corresponding input array elements. */
        final float[] squares = new float[size];

        /** Aparapi Kernel which computes squares of input array elements and populates them in corresponding elements of
         * output array.
         **/
        Kernel kernel = new Kernel(){
            @Override public void run() {
                int gid = getGlobalId();
                squares[gid] = values[gid] * values[gid];
            }
        };

        // Execute Kernel.

        kernel.execute(Range.create(512));

        // Report target execution mode: GPU or JTP (Java Thread Pool).
        //System.out.println("Execution mode=" + kernel.getExecutionMode());
        kernel.dispose();

        System.out.println("Time: " + (System.nanoTime() - t));
    }

    public static void runNormally(){
        long t = System.nanoTime();

        final float[] values = new float[size];
        for (int i = 0; i < size; i++) {
            values[i] = i;
        }

        final float[] squares = new float[size];

        for(int i = 0; i < size; i++){
            squares[i] = values[i] * values[i];
        }

        System.out.println("Time: " + (System.nanoTime() - t));
    }
}
