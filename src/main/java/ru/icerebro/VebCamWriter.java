package ru.icerebro;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VebCamWriter {


    private Scanner scanner;

    private int codec;

    private int WIDTH = 640, HEIGHT = 480;
    private int fps = 5;
    private int thrashHold = 1000/fps;
    private int secondsToWrite = 60;
    private long storageSize_In_Gb = 1073741824L;


    private boolean detected;
    private Mat frame_previous;

    private String recDir;

    private boolean color;

    private VideoCapture camera;



    public VebCamWriter() {
        scanner = new Scanner(System.in);

        camera = new VideoCapture(0);

        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, WIDTH);
        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, HEIGHT);

        codec = VideoWriter.fourcc('H', '2', '6', '4');

        color = false;
        detected = false;
    }

    public boolean write(){
        VideoWriter writer = null;

        this.scanInput();


        if(!camera.isOpened()){
            System.out.println("Error");
        }
        else {
            int index = 0;
            int counter = 0;

            Mat frame = new Mat();
            Mat grayFrame = new Mat();

            int framesToWrite = fps * secondsToWrite;

            while(true){
                if (Main.exit){
                    if (writer != null) {
                        writer.release();
                        System.out.println("Released");
                    }
                    break;
                }

                if (camera.read(frame)){

                    LocalDateTime ldt = LocalDateTime.now();
                    DateTimeFormatter formmatDate = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ENGLISH);
                    DateTimeFormatter formmatTime = DateTimeFormatter.ofPattern("HH.mm", Locale.ENGLISH);
                    DateTimeFormatter formmatSec = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);

                    String date = formmatDate.format(ldt);
                    String time = formmatTime.format(ldt);
                    String sec = formmatSec.format(ldt);

                    if (this.detectMotion(frame)){
                        counter = 0;
                        if (!detected){
                            detected = true;


                            String dir = recDir + "/" + date;
                            this.checkDir(dir);

                            writer = new VideoWriter(dir+"/"+time+".mp4", codec, fps, new Size(WIDTH, HEIGHT), color);

                        }
                    }

                    if (writer != null && counter < framesToWrite){
                        Imgproc.putText(
                                frame,
                                date + " - " + sec,
                                new Point(50, 50),
                                0,
                                1,
                                new Scalar(255, 255, 255),
                                2 );

                        if (!color){
                            Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_RGB2GRAY);
                            writer.write(grayFrame);
                        }else {
                            writer.write(frame);
                        }


                        try {
                            Thread.sleep(thrashHold);
                        } catch (InterruptedException e) {
                            frame.release();
                            writer.release();
                            camera.release();
                            return false;
                        }

                    }else {
                        detected = false;
                        if (writer != null) {
                            writer.release();
                            System.out.println("Released");
                        }
                        writer = null;
                    }
                    index++;
                    counter++;
                }

                frame.release();
            }
        }
        camera.release();
        return true;
    }

    private void scanInput(){
        Runnable runnable = () -> {
            if (scanner.next().toLowerCase().equals("exit")){
                Main.exit = true;
            }
        };
        new Thread(runnable).start();
    }




    private boolean detectMotion(Mat frame){
        //-------------------------
        boolean result = false;
        int sensivity = 30;
        double maxArea = 30;

        Mat frame_current = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
        Mat frame_result = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
        Size size = new Size(3, 3);
        Mat v = new Mat();
//        Scalar scalar1 = new Scalar(0, 0, 255);
//        Scalar scalar2 = new Scalar(0, 255, 0);
        //---------------------------

        //-----------------------------------------------------------------------------
        frame.copyTo(frame_current);

        Imgproc.GaussianBlur(frame_current, frame_current, size, 0);

        if (this.frame_previous == null) {
            this.frame_previous = frame_current;
            return false;
        }else {

            Core.subtract(this.frame_previous, frame_current, frame_result);

            Imgproc.cvtColor(frame_result, frame_result, Imgproc.COLOR_RGB2GRAY);

            Imgproc.threshold(frame_result, frame_result, sensivity, 255, Imgproc.THRESH_BINARY);

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(frame_result, contours, v, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            v.release();


            boolean found = false;
            for (int idx = 0; idx < contours.size(); idx++) {
                Mat contour = contours.get(idx);
                double contourarea = Imgproc.contourArea(contour);
                if (contourarea > maxArea) {
                    found = true;

                    Rect r = Imgproc.boundingRect(contours.get(idx));
                    //Imgproc.drawContours(frame, contours, idx, scalar1);
                    //Imgproc.rectangle(frame, r.br(), r.tl(), scalar2, 1);
                }
                contour.release();
            }

            if (found) {
                //System.out.println("Moved");
                result = true;
            }
        }
        frame_current.copyTo(this.frame_previous);
        return result;
    }

    private void checkDir(String path){
        File theDir = new File(path);

        if (!theDir.exists()) {
            theDir.mkdirs();
        }


        File mainDir = new File(recDir);
        long size = FileUtils.sizeOfDirectory(mainDir);


        if (storageSize_In_Gb < size){
            this.deleteOldDir();
        }
    }

    public void setFps(int fps) {
        this.fps = fps;
        this.thrashHold = 1000/ fps;
    }

    public void setSecondsToWrite(int secondsToWrite) {
        this.secondsToWrite = secondsToWrite;
    }

    public void setColor(boolean color) {
        this.color = color;
    }



    private void deleteOldDir(){
        File file;


        ArrayList<File> files = new ArrayList<>(FileUtils.listFilesAndDirs(
                new File(recDir),
                new NotFileFilter(TrueFileFilter.INSTANCE),
                DirectoryFileFilter.DIRECTORY
        ));

        files.remove(0);

        files.sort(Comparator.comparingLong(File::lastModified));

        if (files.size() > 1){
            file = files.get(0);
            try {
                FileUtils.deleteDirectory(file);
                System.out.println("deleted: "+ file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setStorageSize_In_Gb(long storageSize_In_Gb) {
//        this.storageSize_In_Gb = 1024*1024*6;
        this.storageSize_In_Gb = storageSize_In_Gb * this.storageSize_In_Gb;
    }

    public void setRecDir(String recDir) {
        this.recDir = recDir;
        this.checkDir(recDir);
    }
}
