import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by mohsen on 5/15/15.
 */
public class Solution {
    public static void main(String[] args) throws IOException {

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String s;

        StringBuilder sb = null;
        while ((s = in.readLine()) != null && s.length() != 0) {
            if(s.equals("5")) continue;
            try {
                sb = new StringBuilder();

                Long.valueOf(s);
                sb.insert(0, "* long");

                Integer.valueOf(s);
                if (sb.length() > 0)
                    sb.insert(0, "\n");
                sb.insert(0, "* int");

                Short.valueOf(s);
                if (sb.length() > 0)
                    sb.insert(0, "\n");
                sb.insert(0, "* short");

                Byte.valueOf(s);
                if (sb.length() > 0)
                    sb.insert(0, "\n");
                sb.insert(0, "* byte");

            } catch (java.lang.NumberFormatException ex) {

            }

            System.out.println(sb.length() > 0 ? (s + " can be fitted in:\n" + sb.toString()) : (s + " can't be fitted anywhere."));
        }

    }
}
