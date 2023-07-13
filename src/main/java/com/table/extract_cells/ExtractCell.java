package com.table.extract_cells;

import com.utils.common.CollectionUtil;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sy
 * @date 2023/7/12 21:15
 */
public class ExtractCell {

    /**
     * extract cells from table
     * @param mat
     * @return
     */
    public static List<List<Mat>> extractCells(Mat mat) {
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
        opencv_imgproc.findContours(vhMat, contours, opencv_imgproc.RETR_TREE, opencv_imgproc.CHAIN_APPROX_SIMPLE);

        List<Mat> contourList = CollectionUtil.newArrayList();
        for(int index=0; index<contours.size(); index++) {
            Mat rectMat = contours.get(index);
            contourList.add(rectMat);
        }
        List<Double> perimeterLengths = contourList.stream().map(m -> opencv_imgproc.arcLength(m, true)).collect(Collectors.toList());
        List<Double> epsilonList = perimeterLengths.stream().map(d -> 0.05*d).collect(Collectors.toList());
        List<Mat> approxPolyList = CollectionUtil.newArrayList();
        for(int i=0;i<contourList.size();i++) {
            Mat approxMat = new Mat();
            opencv_imgproc.approxPolyDP(contourList.get(i), approxMat, epsilonList.get(i), true);
            approxPolyList.add(approxMat);
        }
//        filter out contours that aren't rectangular. Those that aren't rectangular are probably noise.
//        List<Mat> approxRectList = approxPolyList.stream().filter(m -> m.total()==4).collect(Collectors.toList());
        List<Rect> boundingRectList = approxPolyList.stream().map(approxPoly -> opencv_imgproc.boundingRect(approxPoly)).collect(Collectors.toList());

//        Filter out rectangles that are too narrow or too short.
        boundingRectList = boundingRectList.stream().filter(rect -> (rect.height() >= 10) && (rect.width() >= 40)).collect(Collectors.toList());
        /**
         * The largest bounding rectangle is assumed to be the entire table.
         * Remove it from the list. We don't want to accidentally try to OCR
         * the entire table.
         */
        int largestRect = boundingRectList.stream().mapToInt(rect -> rect.height() * rect.width()).max().getAsInt();
        boundingRectList = boundingRectList.stream().filter(rect -> rect.height() * rect.width() != largestRect).collect(Collectors.toList());
        List<List<Rect>> rowList = CollectionUtil.newArrayList();
        List<Rect> cellList = boundingRectList.stream().collect(Collectors.toList());
        while(cellList.size() != 0) {
            Rect firstRect = cellList.get(0);
            List<Rect> restList = cellList.subList(1, cellList.size());
            List<Rect> cellInSameRowList = restList.stream().filter(rect -> cellInSameRow(rect, firstRect)).collect(Collectors.toList());
            cellInSameRowList = cellInSameRowList.stream().sorted(Comparator.comparing(Rect::x)).collect(Collectors.toList());
            cellInSameRowList.add(0, firstRect);
            List<Rect> rowCells = cellInSameRowList.stream().sorted(Comparator.comparing(Rect::x)).collect(Collectors.toList());
            rowList.add(rowCells);
            cellList = cellList.stream().filter(rect -> !cellInSameRow(rect, firstRect)).collect(Collectors.toList());
        }
        rowList = rowList.stream().sorted((row1, row2) -> {
            float avg = avgHeightOfCenter(row1) - avgHeightOfCenter(row2);
            if(avg > 0) {
                return 1;
            } else if(avg == 0){
                return 0;
            } else {
                return -1;
            }
        }).collect(Collectors.toList());
        List<List<Mat>> cellRowMat = CollectionUtil.newArrayList();
        for(List<Rect> rects : rowList) {
            List<Mat> matList = rects.stream().map(rect -> mat.apply(rect)).collect(Collectors.toList());
            cellRowMat.add(matList);
        }
        return cellRowMat;
    }

    /**
     * @param restRect
     * @param firstRect
     * @return
     */
    public static boolean cellInSameRow(Rect restRect, Rect firstRect) {
        var restX = restRect.x();
        var restY = restRect.y();
        var restWidth = restRect.width();
        var restHeight = restRect.height();

        var firstX = firstRect.x();
        var firstY = firstRect.y();
        var firstWidth = firstRect.width();
        var firstHeight = firstRect.height();

        var restRectCenter = restY + restHeight - (float)restHeight / 2;
        var firstBottom = firstY + firstHeight;
        return (firstY < restRectCenter) && (restRectCenter < firstBottom);
    }

    /**
     * sort rows by average height of their center.
     * @param row
     * @return
     */
    public static float avgHeightOfCenter(List<Rect> row) {
        List<Float> centerList = row.stream().map(rect -> {
            int y = rect.y();
            int h = rect.height();
            return y + h - (float)h / 2;
        }).collect(Collectors.toList());
        float sum = centerList.stream().reduce(0f, (a,b) -> a+b);
        return sum / centerList.size();
    }

}


