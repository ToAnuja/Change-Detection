/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package videochangedetection;

import java.awt.image.BufferedImage;
import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.opencv.core.Core;
import static org.opencv.core.Core.FONT_HERSHEY_SIMPLEX;
import static org.opencv.core.Core.absdiff;
import static org.opencv.core.Core.getTickCount;
import static org.opencv.core.Core.getTickFrequency;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_NONE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.threshold;
import org.opencv.video.BackgroundSubtractorMOG2;
import static org.opencv.video.Video.createBackgroundSubtractorMOG2;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 *
 * @author anuja
 */
public class VideoChangeDetection {

    /**
     * @param args the command line arguments
     */
    static ConcurrentHashMap<String, ChangeObjectSpec> objectMap = new ConcurrentHashMap();

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        ShowImgJFrame imgshow = new ShowImgJFrame();

        Mat frame = new Mat(); //to get primary frame
        Mat back = new Mat(); //to ; //to get background exclusively 
        Mat backz = new Mat(); //to ; //to get timed instances of background
        Mat fore = new Mat(); //to ; //to get foreground contours
        Mat sub = new Mat();
        VideoCapture cap1 = new VideoCapture("TEST.avi");


        /*to capture from saved video*/
        //Background Subtraction Part
        BackgroundSubtractorMOG2 bg = createBackgroundSubtractorMOG2(0, 0, false);

        int flag = 0;

        int fps = (int) cap1.get(Videoio.CAP_PROP_FPS);
        System.out.println("fps value=" + fps);
        cap1.read(frame);
        float area = (float) (frame.rows() * frame.cols() * 0.20);
        double t = 0;
        while (true) {
            cap1.read(frame);

            if (frame.size().height == 0) {
                System.out.println("Video has been completed");
                cap1.release();
                break;
            }

            if (!imgshow.frameCap) {
                flag = 0;
                imgshow.addImageOnLabel(frame);
                try {
                    Thread.sleep(fps);
                } catch (InterruptedException ex) {

                }
                continue;
            }

            bg.apply(frame, fore, 0.01);
            bg.getBackgroundImage(back);
            if (flag == 0) {
                t = getTickCount() / getTickFrequency();
                BufferedImage SubImgage = imgshow.Mat2BufferedImage(back);
                try {
                    ImageIO.write(SubImgage, "jpg", new File("back.jpg"));
                } catch (IOException ex) {

                }
                backz = back.clone();
                flag = 10;
                objectMap.clear();

                imgshow.addImageOnLabel(frame);
                try {
                    Thread.sleep(fps);
                } catch (InterruptedException ex) {

                }
                continue;
            } else if (flag == 10)//this loop runs once
            {
                if ((getTickCount() / getTickFrequency() - t) >= 3) {
                    backz = back.clone(); //extra loop run to ensure stable initial background                
                    flag = 20;
                }
                imgshow.addImageOnLabel(frame);
                try {
                    Thread.sleep(fps);
                } catch (InterruptedException ex) {
                }
                continue;

            }

            absdiff(back, backz, sub);
            threshold(sub, sub, 35, 255, THRESH_BINARY);
            Mat diff = new Mat();
            Imgproc.cvtColor(sub, diff, COLOR_BGR2GRAY);

            List<MatOfPoint> contours = new ArrayList();
            Imgproc.findContours(diff, contours, new Mat(), RETR_EXTERNAL, CHAIN_APPROX_NONE);
            MatOfPoint2f approxCurve = new MatOfPoint2f();

            if (!contours.isEmpty()) {

                int a = 0;
                ArrayList<Rect> list = new ArrayList();
                for (int i = 0; i < contours.size(); i++) {
                    //Convert contours(i) from MatOfPoint to MatOfPoint2f
                    MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
                    //Processing on mMOP2f1 which is in type MatOfPoint2f
                    double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                    Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
                    //Convert back to MatOfPoint
                    MatOfPoint point = new MatOfPoint(approxCurve.toArray());
                    // Get bounding rect of contour
                    Rect rect = Imgproc.boundingRect(point);

                    a = a + ((rect.width) * (rect.height));
                    if (a >= area) {
                        //Camera Tampering declared
                        flag = 0;
                        list.clear();
                        break;
                    }
                    list.add(rect);
                }

                if (!list.isEmpty()) {
                    for (int i = 0; i < list.size(); i++) {

                        Rect rect = list.get(i);

                        if ((rect.width >= 10 && rect.height >= 25) || (rect.width >= 10 && rect.height >= 10)) {
                            int diffT = 0;
                            Mat cropMat = new Mat(frame, rect);
                            if (extractObjectFromMat(cropMat) != 0) {

                                ChangeObjectSpec obj = getHistoryObject(rect.x, rect.y);
                                if (obj.drawnCnt == 0) {
                                    ++obj.drawnCnt;
                                    obj.t0 = getTickCount() / getTickFrequency();
                                    // System.out.println("Object Time t0:"+obj.t0+" Key:"+obj.key);
                                } else {
                                    ++obj.drawnCnt;
                                }

                                if (obj.drawnCnt > 100) {
                                    double t2 = getTickCount() / getTickFrequency();
                                    diffT = (int) (t2 - obj.t0);
                                    if (diffT > 5) {
                                        obj.abandantFlg = true;
                                    }
                                }

                                int w = rect.width;
                                int h = rect.height;
                                if (w < 15) {
                                    rect.x = rect.x - w;
                                    rect.width = rect.width * 2;
                                }

                                if (h < 15) {
                                    rect.y = rect.y - h;
                                    rect.height = rect.height * 2;
                                }

                                if (obj.abandantFlg) {
                                    rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255, 255), 3);
                                    Imgproc.putText(frame, "Warn::" + diffT + " Sec", new Point(rect.x, rect.y), FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0), 2);
                                } else {
                                    rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0, 255), 3);
                                }

                                objectMap.remove(obj.key);
                                objectMap.put(obj.key, obj);
                            }
                        }
                    }
                }
            } else {
                clearOldRectFromMap();
            }
            imgshow.addImageOnLabel(frame);
        }//end of infinite loop  
    }

    private static void clearOldRectFromMap() {
        double t = getTickCount() / getTickFrequency();
        Iterator itr = objectMap.keySet().iterator();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            ChangeObjectSpec obj = objectMap.get(key);
            if (obj != null) {
                int diffT = (int) (obj.t0 - t);
                if (diffT > 60) {
                    objectMap.remove(obj.key);
                }
            }
        }
    }

    private static ChangeObjectSpec getHistoryObject(int x, int y) {
        String key = new StringBuilder("").append(x).append("_").append(y).toString();
        ChangeObjectSpec obj = objectMap.get(key);
        if (obj == null) {
            int X1 = x - 1;
            key = new StringBuilder("").append(X1).append("_").append(y).toString();
            obj = objectMap.get(key);
            if (obj == null) {
                int Y1 = y - 1;
                key = new StringBuilder("").append(x).append("_").append(Y1).toString();
                obj = objectMap.get(key);
                if (obj == null) {
                    key = new StringBuilder("").append(X1).append("_").append(Y1).toString();
                    obj = objectMap.get(key);
                    if (obj == null) {
                        obj = new ChangeObjectSpec();
                        obj.key = key;
                        obj.abandantFlg = false;
                    }
                }
            }
        }

        return obj;
    }

    private static int extractObjectFromMat(Mat image) {

        Mat edged = new Mat();
        Imgproc.Canny(image, edged, 5, image.height());

        //applying closing function 
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Mat closed = new Mat();
        Imgproc.morphologyEx(edged, closed, Imgproc.MORPH_CLOSE, kernel);

        //finding_contours 
        List<MatOfPoint> contours = new ArrayList();
        Mat points = new Mat();
        Imgproc.findContours(closed, contours, points, RETR_EXTERNAL, CHAIN_APPROX_NONE);

        erode(closed, closed, new Mat());
        erode(closed, closed, new Mat());
        erode(closed, closed, new Mat());
        dilate(closed, closed, new Mat());
        dilate(closed, closed, new Mat());
        dilate(closed, closed, new Mat());

        MatOfPoint2f approxCurve = new MatOfPoint2f();
        int cnt = 0;
        for (int i = 0; i < contours.size(); i++) {
            //Convert contours(i) from MatOfPoint to MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
            //Convert back to MatOfPoint
            MatOfPoint point = new MatOfPoint(approxCurve.toArray());
            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(point);

            if ((rect.width >= 10 && rect.height >= 25) || (rect.width >= 25 && rect.height >= 10)) {
                cnt++;
            }
        }
        return cnt;
    }
}

class ChangeObjectSpec {

    double t0;
    double t1;
    int drawnCnt;
    boolean abandantFlg;
    String key;
}
