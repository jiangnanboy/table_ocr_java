package com.table.extract_tables;

import com.utils.common.CollectionUtil;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sy
 * @date 2023/7/12 21:11
 */
public class ExtractTable {

    /**
     * extract tables from image
     * @param mat
     * @return
     */
    public static List<Mat> extractTables(Mat mat) {
        opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.COLOR_BGR2GRAY);
        Mat blurMat = new Mat();
        opencv_imgproc.GaussianBlur(mat, blurMat, new Size(17, 17), 0);
        opencv_core.bitwise_not(blurMat, blurMat);
        Mat binaryMat = new Mat();
        opencv_imgproc.adaptiveThreshold(blurMat, binaryMat, 255, opencv_imgproc.ADAPTIVE_THRESH_MEAN_C,
                opencv_imgproc.THRESH_BINARY, 15, -2);
        int imageWidth = binaryMat.size().width();
        int imageHeight = binaryMat.size().height();
        Mat horKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(imageWidth/5, 1));
        Mat horOpen = new Mat();
        opencv_imgproc.morphologyEx(binaryMat, horOpen, opencv_imgproc.MORPH_OPEN, horKernel);

        Mat verKernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(1, imageHeight/5));
        Mat verOpen = new Mat();
        opencv_imgproc.morphologyEx(binaryMat, verOpen, opencv_imgproc.MORPH_OPEN, verKernel);

        Mat horDilate = new Mat();
        opencv_imgproc.dilate(horOpen, horDilate, opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(40, 1)));
        Mat verDilate = new Mat();
        opencv_imgproc.dilate(verOpen, verDilate, opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(1, 60)));

        Mat vhMat = new Mat();
        opencv_core.addWeighted(horDilate, 0.5, verDilate, 0.5, 0, vhMat);

        MatVector contours = new MatVector();
        opencv_imgproc.findContours(vhMat, contours, opencv_imgproc.RETR_EXTERNAL, opencv_imgproc.CHAIN_APPROX_SIMPLE);

        List<Mat> contourList = CollectionUtil.newArrayList();
        for(int index=0; index<contours.size(); index++) {
            Mat rectMat = contours.get(index);
            double area = opencv_imgproc.contourArea(rectMat);
           if(area > 1e5) {
               contourList.add(rectMat);
           }
        }
        List<Double> perimeterLengths = contourList.stream().map(m -> opencv_imgproc.arcLength(m, true)).collect(Collectors.toList());
        List<Double> epsilonList = perimeterLengths.stream().map(d -> 0.1*d).collect(Collectors.toList());
        List<Mat> approxPolyList = CollectionUtil.newArrayList();
        for(int i=0;i<contourList.size();i++) {
            Mat approxMat = new Mat();
            opencv_imgproc.approxPolyDP(contourList.get(i), approxMat, epsilonList.get(i), true);
            approxPolyList.add(approxMat);
        }
        List<Rect> rectList = approxPolyList.stream().map(approxPoly -> opencv_imgproc.boundingRect(approxPoly)).collect(Collectors.toList());
        List<Mat> resultList = rectList.stream().map(rect -> {
            Mat resultMat = mat.apply(rect);
            return resultMat;
        }).collect(Collectors.toList());
        return resultList;
    }

}

