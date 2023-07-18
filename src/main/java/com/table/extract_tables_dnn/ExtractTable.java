package com.table.extract_tables_dnn;

import ai.onnxruntime.OrtException;
import com.utils.common.PropertiesReader;
import com.utils.cv.OpenCVUtils;
import org.apache.commons.codec.binary.StringUtils;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

/**
 * @author sy
 * @date 2023/4/28 20:49
 */
public class ExtractTable {
    static ModelDet modelDet;
    static {
        String modelPath = PropertiesReader.get("table_det_model_path");
        String labelPath = PropertiesReader.get("table_det_labels_path");
        try {
            modelDet = new ModelDet(modelPath, labelPath);
        } catch (OrtException e) {
            e.printStackTrace();
        }
    }

    /**
     * extract tables from image
     * @param mat
     * @return
     */
    public static List<org.bytedeco.opencv.opencv_core.Mat> extractTables(Mat mat) {
        List<org.bytedeco.opencv.opencv_core.Mat> resultList = null;
        try {
            List<Detection> detectionList = modelDet.detectObjects(mat);
            detectionList = detectionList.stream().filter(detection -> StringUtils.equals(detection.getLabel(), "Table")).collect(Collectors.toList());
            resultList = detectionList.stream().map(detection -> {
                float[] bbox = detection.getBbox();
                Rect rect = new Rect(new Point(bbox[0], bbox[1]),
                        new Point(bbox[2], bbox[3]));
                Mat tableImg = mat.submat(rect);
                BufferedImage bufferedImage = OpenCVUtils.matToBufferedImage(tableImg);
                return bufferedImageToMat(bufferedImage);
            }).collect(Collectors.toList());
        } catch (OrtException ortException) {
            ortException.printStackTrace();
        }
        return resultList;
    }
    public static void main(String...args) {
        String imgPath = "D:\\project\\idea_workspace\\table_ocr_java\\img_test\\simple.png";
        Mat img = Imgcodecs.imread(imgPath);
        List<org.bytedeco.opencv.opencv_core.Mat> resultList = extractTables(img);
        if(Optional.ofNullable(resultList).isPresent()) {
            int tableNum=0;
            for(org.bytedeco.opencv.opencv_core.Mat table: resultList) {
                imwrite("D:\\project\\idea_workspace\\table_ocr_java\\img_test\\table_" + tableNum + ".jpg", table);
                tableNum++;
            }
        }
        }

    public static org.bytedeco.opencv.opencv_core.Mat bufferedImageToMat(BufferedImage bi) {
        OpenCVFrameConverter.ToMat cv = new OpenCVFrameConverter.ToMat();
        return cv.convertToMat(new Java2DFrameConverter().convert(bi));
    }

}
