import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Test {
    public static void main(String[] args) throws IOException {
        String[] bloques32 = {"08B1EDB36AE8747CE96E9E7AEB971938", "D1FD523475E449252A8150D881FA234F", "5A957FE805A2E0B43C9D0A1FE0EA4BB3", "F7D57E0D5581C8189C478E2B475C13C1"};
        String[] iv =        {"50c0acfb539f120c905ffc2ba2a26c4f", "94be2250449e707249e306edb69b5122", "3bc01e8734e1b2d970fc534c93a619d9", "9de233383fe9a155f172882d415a15c7"};
        for(int i=0; i<bloques32.length; i++){
            System.out.print(new String(ClienteOracle.hexStringToBytes(ClienteOracle.xor(bloques32[i], iv[i]))));
        }
    }


}
