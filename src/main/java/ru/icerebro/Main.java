package ru.icerebro;


import org.opencv.core.Core;

public class Main {
    public static boolean exit = false;

    private static String recordsFolder = "d:/records";
    private static String color = "false";
    private static String fps = "5";
    private static String maxStorage = "200";
    private static String secToWrite  = "60";

    public static void main (String args[]) throws InterruptedException{
        for (int i = 0; i < args.length; i++) {
            switch (args[i]){
                case "-r":
                    recordsFolder = args[++i];
                    break;
                case "-c":
                    color = args[++i];
                    break;
                case "-fps":
                    fps = args[++i];
                    break;
                case "-s":
                    maxStorage = args[++i];
                    break;
                case "-t":
                    secToWrite = args[++i];
                    break;
            }
        }


        int bitness = Integer.parseInt(System.getProperty("sun.arch.data.model"));
        if (bitness == 32) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME+"_x32");
        } else if (bitness == 64) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME+"_x64");
        } else {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME+"_x32");
        }


        VebCamWriter vebCamWriter = new VebCamWriter();

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("TYPE: \"EXIT\" to close program");
        System.out.println("-------------------------------------------------------------------------");
        while (true){
            vebCamWriter.setRecDir(recordsFolder);

            vebCamWriter.setColor(Boolean.valueOf(color));

            vebCamWriter.setFps(Integer.valueOf(fps));


            vebCamWriter.setSecondsToWrite(Integer.valueOf(secToWrite));

            vebCamWriter.setStorageSize_In_Gb(Integer.valueOf(maxStorage));

            try {
                vebCamWriter.write();
            }catch (Exception e){
                e.printStackTrace();
            }

            if (exit){
                break;
            }
        }
    }
}
