package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
public class Command {
    public static String exeCmd(String commandStr) {
        BufferedReader br = null;
        String result = "";
        try {
            Process p = Runtime.getRuntime().exec(commandStr);
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            result = sb.toString();
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally
        {
            if (br != null)
            {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
    /*
    public static void main(String[] args) {
        String commandStr = "ping www.baidu.com";
        //String commandStr = "ipconfig";
        Command.exeCmd(commandStr);
    }*/
}
