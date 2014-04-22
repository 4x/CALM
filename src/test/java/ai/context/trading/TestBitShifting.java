package ai.context.trading;

import java.io.IOException;

public class TestBitShifting {
    static int y = 10;
    {System.out.println("TEST");}
    public static void main(String[] args){
        int x = -1;
        x = x >>> 0;
        System.out.println(x);

        if(false)
            System.out.println("A");
            if(true)
                System.out.println("B");
            else;


        new TestBitShifting();

        int y = 5;
        Boolean.parseBoolean("true");
        new Character('C');

        Sup s = new Sub();
        System.out.println(s.i);

        if(1 == 1);
        System.out.println("4" + 2 + 3);

        try{
            throw new IllegalArgumentException();
        }
        finally {
            System.out.println("BLABLA");
        }
    }

    enum El{
        H,
        B
    }


}

class Sup{
    int i = 0;
    Sup(){

    }

    Sup(int a){

    }
}

class Sub extends Sup{

    int i = 1;
    Sub(){

    }

    Sub(int a) {
        super(a);
    }
}



interface Intf{
    public void test() throws IOException;
}

class Impl implements Intf{

    @Override
    public void test(){

    }
}
