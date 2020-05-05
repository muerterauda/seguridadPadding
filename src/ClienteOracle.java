/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author isaac
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

   public static void getError(String e) throws IOException {
       String iv_ori =e.substring(0, 32);
       System.out.println(iv_ori);
       String[] bloques32 = new String[4];
       for (int c=32;c<e.length();c+=32)
       {
           bloques32[(c/32)-1] = e.substring(c, c+32);
       }
       pintarArray(bloques32);
       List<List<String>> solucion_completa = new ArrayList<>();
       for(String bloque: bloques32){
           String iv = "00000000000000000000000000000000";
           List<String> sol = new ArrayList<>();
           String pad = "01";
           while(sol.size()<16){
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
               System.out.println(sol_hex);
               sol.add(0, xor(pad, sol_hex));
               pad = dameSiguiente(pad);
               if(pad.length()<2){
                   pad = "0"+pad;
               }
           }
           System.out.println(sol);
           solucion_completa.add(sol);
       }
       printSolucion(solucion_completa);
       String[] iv_com = {iv_ori, bloques32[0], bloques32[1], bloques32[2]};
       System.out.println("\n-----------------------------------------------");
       System.out.println("SOLUCION");
       System.out.println("-----------------------------------------------");
       for(int i=0; i<solucion_completa.size(); i++){
           System.out.print(convertHexToString(ClienteOracle.xor(getBloqueCompleto(solucion_completa.get(i)), iv_com[i])).toLowerCase());
       }
       System.out.println("\n-----------------------------------------------");
   }
    public static void getTime(String e) throws IOException {
        String iv_ori =e.substring(0, 32);
        System.out.println(iv_ori);
        String[] bloques32 = new String[4];
        for (int c=32;c<e.length();c+=32)
        {
            bloques32[(c/32)-1] = e.substring(c, c+32);
        }
        pintarArray(bloques32);
        List<List<String>> solucion_completa = new ArrayList<>();
        for(String bloque: bloques32){
            String iv = "00000000000000000000000000000000";
            List<String> sol = new ArrayList<>();
            String pad = "01";
            while(sol.size()<16){
                String sol_hex= "00";
                String iv_sol = cacularIVNuevo(sol, pad);
                iv = iv.substring(0, iv.length()-2);
                while(timingOracle(iv+sol_hex+iv_sol+bloque)<200){
                    if(sol_hex.equals("ff")||sol_hex.equals("FF")){
                        throw new RuntimeException("Problema encontrado");
                    }
                    sol_hex = dameSiguiente(sol_hex);
                    if(sol_hex.length()<2){
                        sol_hex = "0"+sol_hex;
                    }
                }
                System.out.println(sol_hex);
                sol.add(0, xor(pad, sol_hex));
                pad = dameSiguiente(pad);
                if(pad.length()<2){
                    pad = "0"+pad;
                }
            }
            System.out.println(sol);
            solucion_completa.add(sol);
        }
        printSolucion(solucion_completa);
        String[] iv_com = {iv_ori, bloques32[0], bloques32[1], bloques32[2]};
        System.out.println("\n-----------------------------------------------");
        System.out.println("SOLUCION");
        System.out.println("-----------------------------------------------");
        for(int i=0; i<solucion_completa.size(); i++){
            System.out.print(convertHexToString(ClienteOracle.xor(getBloqueCompleto(solucion_completa.get(i)), iv_com[i])));
        }
        System.out.println("\n-----------------------------------------------");
    }

    public static void getTimeExacto(String e) throws IOException {
        String iv_ori = e.substring(0, 32);
        System.out.println(iv_ori);
        String[] bloques32 = new String[4];
        for (int c=32;c<e.length();c+=32)
        {
            bloques32[(c/32)-1] = e.substring(c, c+32);
        }
        pintarArray(bloques32);
        List<List<String>> solucion_completa = new ArrayList<>();
        for(String bloque: bloques32){
            String iv = "00000000000000000000000000000000";
            List<String> sol = new ArrayList<>();
            String pad = "01";
            while(sol.size()<16){
                String sol_hex= "00";
                String hex_sol = "00";
                String iv_sol = cacularIVNuevo(sol, pad);
                iv = iv.substring(0, iv.length()-2);
                timingOracle(iv+sol_hex+iv_sol+bloque);
                long time =  timingOracle(iv+sol_hex+iv_sol+bloque);
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
                System.out.println(hex_sol+": "+ time);
                sol.add(0, xor(pad, hex_sol));
                pad = dameSiguiente(pad);
                if(pad.length()<2){
                    pad = "0"+pad;
                }
            }
            System.out.println(sol);
            solucion_completa.add(sol);
        }
        printSolucion(solucion_completa);
        String[] iv_com = {iv_ori, bloques32[0], bloques32[1], bloques32[2]};
        System.out.println("\n-----------------------------------------------");
        System.out.println("SOLUCION");
        System.out.println("-----------------------------------------------");
        for(int i=0; i<solucion_completa.size(); i++){
            System.out.print(convertHexToString(ClienteOracle.xor(getBloqueCompleto(solucion_completa.get(i)), iv_com[i])));
        }
        System.out.println("\n-----------------------------------------------");
    }

    public static String convertHexToString(String hex) {

        StringBuilder result = new StringBuilder();

        // split into two chars per loop, hex, 0A, 0B, 0C...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            String tempInHex = hex.substring(i, (i + 2));

            //convert hex to decimal
            int decimal = Integer.parseInt(tempInHex, 16);

            // convert the decimal to char
            result.append((char) decimal);

        }

        return result.toString();

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
        String e = "c0ad3d84be178fbd3475f3b5271d0ee9cbc1d28c9197805a933cc67180703e1826b4ad6fc6376b976f05053679924e2cc6550c6c59686e0e70193fa038b587a5917a1a082251135b2e2a2b49a0120c85";
        String e1 = "50c0acfb539f120c905ffc2ba2a26c4f94be2250449e707249e306edb69b51223bc01e8734e1b2d970fc534c93a619d99de233383fe9a155f172882d415a15c792238d1a231a716b2eabfe7efd1e3539";
        String t = "32d5454a21ba5850cb19d6cb5f5b89c477d4892b1443271e601e5d9264f33ce89e7f46ad7ea842d3c2fd477aef53a20b5509aa655a55d2d778fcaf07c88f3bf1bb00ff438108dcd3414c76be47ded71b";
        getError(e1);

    }

    private static String cacularIVNuevo(List<String> sol, String pad) {
        StringBuilder sb = new StringBuilder();
        for(String s: sol){
            sb.append(xor(s, pad));
        }
        return sb.toString();
    }
}
