import java.io.IOException;
import java.io.InputStreamReader;

public class H12 {
    // ((ab+ba)*baa)*

    static state[] states = {
            new state("{1}", 1, 2),
            new state("{2}", 6, 0),
            new state("{3,4}", 3, 6),
            new state("{1,5}", 4, 2),
            new state("{1,2}", 1, 5),
            new state("{2,3,4}", 3, 0),
            new state("E", 6, 6)
    };

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();

        int currentState = 0;
        InputStreamReader br = new InputStreamReader(System.in);
        char ch = ' ';
        loop: while (true) {
            ch = (char) br.read();
            if (ch == 'a') {
                currentState = states[currentState].indexA;
            } else if (ch == 'b') {
                currentState = states[currentState].indexB;
            } else if (ch == '\n') {
                System.out.println("\nWord accepted completely");
            } else if (ch == -1 || ch == 65535) {
                break;
            } else {
                System.err.println("Invalid character " + (int) ch + ": " + ch);
            }
            if(currentState!=6)
                System.out.print(ch);

            if (currentState == 6) {
                // System.out.print("         |          ");
                currentState = 0;
                while (true) {
                    ch = (char) br.read();
                    if (ch == '\n') {
                        System.out.print("\n");
                        break;
                    }
                    if (ch == -1) {
                        break loop;
                    }
                }
            }

        }
        System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private record state(String name, int indexA, int indexB) {
    };
}