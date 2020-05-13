import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Juan Palma Borda
 * @author Isaac
 */
public class ClienteOracle {


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    final protected static String url_base = "http://research.adabyron.uma.es:9500/";

    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] xor(byte[] x, byte[] y){
        int min = Math.min(x.length, y.length);
        byte[] z = new byte[min];
        for(int i=0; i<min; i++){
            z[i] = (byte) (x[i] ^ y[i]);
        }
        return z;
    }

    public static String xor(String x, String y){
        return bytesToHexString(xor(hexStringToBytes(x),hexStringToBytes(y)));
    }

    public static boolean paddingOracle(String u) throws IOException {
        URL url = new URL(url_base+"error/"+u);
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        return (http.getResponseCode()==404);
    }

    public static boolean login(String name) throws IOException {
        URL url = new URL(url_base+"start/"+name);
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        return (http.getResponseCode()==200);
    }

    public static long timingOracle(String u) throws IOException {
        URL url = new URL(url_base+"time/"+u);
        long inicio = System.currentTimeMillis();
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.getResponseCode();
        long fin = System.currentTimeMillis();
        return  fin-inicio;
    }

    public static void pintarArray(String[] a){
        for (String s: a) {
            System.out.println(s);
        }
    }

    public static String dameSiguiente(String x){
        return Integer.toHexString(Integer.parseInt(x, 16)+1);
    }

    public static void printSolucion(List<List<String>> s){
        for(List<String> t: s){
            System.out.print(getBloqueCompleto(t));
            System.out.print(" ");
        }
   }
    public static String getBloqueCompleto(List<String> t){
        StringBuilder sb = new StringBuilder();
        for(String st: t){
            sb.append(st);
        }
        return sb.toString();
    }

    /**
     * Metodo que te devuelve la String resultante del proceso de descifrado llevado a cabo
     * por el oraculo de padding gracias al disinto tipo de error que provoca el padding
     * @param e Cadena hexadecimal a la que romper la seguridad
     * @throws IOException
     */
   public static void getError(String e) throws IOException {
       //Se consigue el IV original
       String iv_ori =e.substring(0, 32);
       System.out.println(iv_ori);
       String[] bloques32 = new String[4];
       //Se Separa la cadena en los bloques
       for (int c=32;c<e.length();c+=32)
       {
           bloques32[(c/32)-1] = e.substring(c, c+32);
       }
       //Se muestran los distintos bloques
       pintarArray(bloques32);
       List<List<String>> solucion_completa = new ArrayList<>();
       // Se realiza el algoritmo para cada bloque, para los que se buscan las soluciones a los disitnas parejas hexadecimales
       for(String bloque: bloques32){
           String iv = "00000000000000000000000000000000";
           List<String> sol = new ArrayList<>();
           String pad = "01";
           // Se requieren 16 soluciones para cada bloque, ya que son 16 parejas a resolver
           while(sol.size()<16){
               //Se parte el IV en 3, lo que falta, la pareja que se esta resolviendo y lo que se tiene resuelto y al que se le aplica el padding correspondiente
               String sol_hex= "00";
               String iv_sol = cacularIVNuevo(sol, pad);
               iv = iv.substring(0, iv.length()-2);
               while(!paddingOracle(iv+sol_hex+iv_sol+bloque)){
                   if(sol_hex.equals("ff")||sol_hex.equals("FF")){
                       throw new RuntimeException("Problema encontrado");
                   }
                   sol_hex = dameSiguiente(sol_hex);
                   if(sol_hex.length()<2){
                       sol_hex = "0"+sol_hex;
                   }
               }
               //Se imprime la solucion de la pareja por motivos de control y se añade a las soluciones
               System.out.println(sol_hex);
               sol.add(0, xor(pad, sol_hex));
               pad = dameSiguiente(pad);
               if(pad.length()<2){
                   pad = "0"+pad;
               }
           }
           // Se muestra la solucion del bloque por motivos de control y se añade a las solucion completa
           System.out.println(sol);
           solucion_completa.add(sol);
       }
       // Se muestran todas las soluciones y se procede a aplicar el xor final
       printSolucion(solucion_completa);
       String[] iv_com = {iv_ori, bloques32[0], bloques32[1], bloques32[2]};
       System.out.println("\n-----------------------------------------------");
       System.out.println("SOLUCION");
       System.out.println("-----------------------------------------------");
       for(int i=0; i<solucion_completa.size(); i++){
           System.out.print(new String(ClienteOracle.hexStringToBytes((ClienteOracle.xor(getBloqueCompleto(solucion_completa.get(i)), iv_com[i])))));
       }
       System.out.println("\n-----------------------------------------------");
   }
    /**
     * Metodo que te devuelve la String resultante del proceso de descifrado llevado a cabo
     * por el oraculo de padding gracias al tiempo estimatorio que tarda al responderte la pagian con distintos tipos de padding
     * @param e Cadena hexadecimal a la que romper la seguridad
     * @throws IOException
     */
    public static void getTime(String e) throws IOException {
        //Se consigue el IV original
        String iv_ori =e.substring(0, 32);
        System.out.println(iv_ori);
        String[] bloques32 = new String[4];
        for (int c=32;c<e.length();c+=32)
        {
            bloques32[(c/32)-1] = e.substring(c, c+32);
        }
        //Se muestran los distintos bloques
        pintarArray(bloques32);
        List<List<String>> solucion_completa = new ArrayList<>();
        // Se realiza el algoritmo para cada bloque, para los que se buscan las soluciones a los disitnas parejas hexadecimales
        for(String bloque: bloques32){
            String iv = "00000000000000000000000000000000";
            List<String> sol = new ArrayList<>();
            String pad = "01";
            // Se requieren 16 soluciones para cada bloque, ya que son 16 parejas a resolver
            while(sol.size()<16){
                //Se parte el IV en 3, lo que falta, la pareja que se esta resolviendo y lo que se tiene resuelto y al que se le aplica el padding correspondiente
                String sol_hex= "00";
                String iv_sol = cacularIVNuevo(sol, pad);
                iv = iv.substring(0, iv.length()-2);
                // La estimacion esta puesta para mas de 200, ya que los valores incorrectos no superan los 100 y los valores corrector suelen rondar los 400 y pico
                while(timingOracle(iv+sol_hex+iv_sol+bloque)<200){
                    if(sol_hex.equals("ff")||sol_hex.equals("FF")){
                        throw new RuntimeException("Problema encontrado");
                    }
                    sol_hex = dameSiguiente(sol_hex);
                    if(sol_hex.length()<2){
                        sol_hex = "0"+sol_hex;
                    }
                }
                //Se imprime la solucion de la pareja por motivos de control y se añade a las soluciones
                System.out.println(sol_hex);
                sol.add(0, xor(pad, sol_hex));
                pad = dameSiguiente(pad);
                if(pad.length()<2){
                    pad = "0"+pad;
                }
            }
            // Se muestra la solucion del bloque por motivos de control y se añade a las solucion completa
            System.out.println(sol);
            solucion_completa.add(sol);
        }
        // Se muestran todas las soluciones y se procede a aplicar el xor final
        printSolucion(solucion_completa);
        String[] iv_com = {iv_ori, bloques32[0], bloques32[1], bloques32[2]};
        System.out.println("\n-----------------------------------------------");
        System.out.println("SOLUCION");
        System.out.println("-----------------------------------------------");
        for(int i=0; i<solucion_completa.size(); i++){
            System.out.print(new String(ClienteOracle.hexStringToBytes((ClienteOracle.xor(getBloqueCompleto(solucion_completa.get(i)), iv_com[i])))));
        }
        System.out.println("\n-----------------------------------------------");
    }

    /**
     * Metodo que te devuelve la String resultante del proceso de descifrado llevado a cabo
     * por el oraculo de padding gracias al tiempo que tarda al responderte la pagian con distintos tipos de padding,
     * esta version difiere de la anterior en que en esta se consigue el maximo tiempo.
     * @param e Cadena hexadecimal a la que romper la seguridad
     * @throws IOException
     */
    public static void getTimeExacto(String e) throws IOException {
        //Se consigue el IV original
        String iv_ori = e.substring(0, 32);
        System.out.println(iv_ori);
        String[] bloques32 = new String[4];
        for (int c=32;c<e.length();c+=32)
        {
            bloques32[(c/32)-1] = e.substring(c, c+32);
        }
        //Se muestran los distintos bloques
        pintarArray(bloques32);
        List<List<String>> solucion_completa = new ArrayList<>();
        for(String bloque: bloques32){
            String iv = "00000000000000000000000000000000";
            List<String> sol = new ArrayList<>();
            String pad = "01";
            // Se requieren 16 soluciones para cada bloque, ya que son 16 parejas a resolver
            while(sol.size()<16){
                //Se parte el IV en 3, lo que falta, la pareja que se esta resolviendo y lo que se tiene resuelto y al que se le aplica el padding correspondiente
                String sol_hex= "00";
                String hex_sol = "00";
                String iv_sol = cacularIVNuevo(sol, pad);
                iv = iv.substring(0, iv.length()-2);
                timingOracle(iv+sol_hex+iv_sol+bloque);
                long time =  timingOracle(iv+sol_hex+iv_sol+bloque);
                // No se realiza estimacion puesto que se consiguen todos los resultados y se asigna como correcto al mas alto
                while(!sol_hex.equals("ff")){
                    sol_hex = dameSiguiente(sol_hex);
                    if(sol_hex.length()<2){
                        sol_hex = "0"+sol_hex;
                    }
                    long timeHex = timingOracle(iv+sol_hex+iv_sol+bloque);
                    if(timeHex>time){
                        hex_sol = sol_hex;
                        time = timeHex;
                    }
                }
                //Se imprime la solucion de la pareja y el tiempo que ha tardado por motivos de control, si ha tardado mucho (mas de 550) vigilar porque probablemente haya habido algun problema en la redd y se añade a las soluciones
                System.out.println(hex_sol+": "+ time);
                sol.add(0, xor(pad, hex_sol));
                pad = dameSiguiente(pad);
                if(pad.length()<2){
                    pad = "0"+pad;
                }
            }
            // Se muestra la solucion del bloque por motivos de control y se añade a las solucion completa
            System.out.println(sol);
            solucion_completa.add(sol);
        }
        // Se muestran todas las soluciones y se procede a aplicar el xor final
        printSolucion(solucion_completa);
        String[] iv_com = {iv_ori, bloques32[0], bloques32[1], bloques32[2]};
        System.out.println("\n-----------------------------------------------");
        System.out.println("SOLUCION");
        System.out.println("-----------------------------------------------");
        for(int i=0; i<solucion_completa.size(); i++){
            System.out.print(new String(ClienteOracle.hexStringToBytes((ClienteOracle.xor(getBloqueCompleto(solucion_completa.get(i)), iv_com[i])))));
        }
        System.out.println("\n-----------------------------------------------");
    }

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        //Hace que se guarden las cookies para los siguientes accesos usando HttpURLConnection
        CookieHandler.setDefault( new CookieManager( null, CookiePolicy.ACCEPT_ALL ) );
        //Llama la url del Login con tu Nombre, cambiar el string por tu nombre completo
        //Lo ideal es llamar primero a start con curl y copiar los retos, luego al hacer login
        //simplemente cargará la cookie necesaria para las demás llamadas
        login("JuanPalmaBorda");
        //Es la cadena que queremos descifrar usando el oraculo de padding.
        String e = "50c0acfb539f120c905ffc2ba2a26c4f94be2250449e707249e306edb69b51223bc01e8734e1b2d970fc534c93a619d99de233383fe9a155f172882d415a15c792238d1a231a716b2eabfe7efd1e3539";
        String t = "32d5454a21ba5850cb19d6cb5f5b89c477d4892b1443271e601e5d9264f33ce89e7f46ad7ea842d3c2fd477aef53a20b5509aa655a55d2d778fcaf07c88f3bf1bb00ff438108dcd3414c76be47ded71b";
        getTime(t);

    }

    private static String cacularIVNuevo(List<String> sol, String pad) {
        StringBuilder sb = new StringBuilder();
        for(String s: sol){
            sb.append(xor(s, pad));
        }
        return sb.toString();
    }
}
