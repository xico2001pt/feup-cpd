package pt.up.fe.cpd.utils;

public class HashUtils {
    public static int compare(byte[] a, byte[] b){
        for (int i = 0; i < a.length; ++i) {
            if (a[i] < b[i]){
                return -1;  // A is less then B
            } else if (a[i] > b[i]){
                return 1;  // A is greater than B
            }
        }
        return 0;  // They're equal
    }

    public static byte[] keyStringToByte(String key){
        byte[] result = new byte[32];
        for (int i = 0; i < key.length(); i += 2) {
            result[i/2] = (byte) ((Character.digit(key.charAt(i), 16) << 4)
                                + Character.digit(key.charAt(i+1), 16));
        }
        return result;
    }

    public static String keyByteToString(byte[] key) {
        StringBuilder result = new StringBuilder();
        for (byte b : key) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
