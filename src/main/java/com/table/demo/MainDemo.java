package com.table.demo;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.translate.TranslateException;
import com.table.ocr_image.TextListBox;
import com.utils.common.CollectionUtil;
import org.bytedeco.opencv.opencv_core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.table.extract_cells.ExtractCell.extractCells;
import static com.table.ocr_image.OcrImage.ocrImage;
import static com.table.ocr_to_csv.OcrToCsv.ocrToCsv;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;

/**
 * @author sy
 * @date 2023/7/12 20:08
 */
public class MainDemo {

    public static void main(String...args) throws IOException, TranslateException {
        String imagePath = "D:/project/idea_workspace/table_ocr_java/img_test/simple.png";
        // 1.extract tables
        boolean useDnn = false;
        List<Mat> tableList = null;
        if(useDnn) {
            tableList = extractTableDnn(imagePath);
        } else {
            tableList = extractTable(imagePath);
        }
        String imageName = Paths.get(imagePath).getFileName().toString().split("\\.")[0];
        String imageParent = Paths.get(imagePath).getParent().toString();
        String imageDirectory  = imageParent + "/" + imageName;
        System.out.println("imageName: " + imageName);
        System.out.println("imageParent: " + imageParent);
        System.out.println("imageDirectory: " + imageDirectory);
        createImageDirectory(imageDirectory);
        int tableNum=0;
        for(Mat tableMat : tableList) {
            String tableMatNum = "table-"+tableNum;
            imwrite(imageDirectory + "/" + tableMatNum + ".png", tableMat);
            createImageDirectory(imageDirectory+"/"+tableMatNum);
            // 2.extract cells
            List<List<Mat>> cellMatList = extractCells(tableMat);
            System.out.println("cellmatList size: " + cellMatList.size());
            List<List<Image>> cellImages = CollectionUtil.newArrayList();
            int row=0;
            for(List<Mat> rowMat : cellMatList) {
                List<Image> rowList = CollectionUtil.newArrayList();
                int col=0;
                for(Mat colMat : rowMat) {
                    imwrite(imageDirectory + "/" + tableMatNum + "/" + row + "-" + col + ".png", colMat);
                    var cellFile = Paths.get(imageDirectory + "/" + tableMatNum + "/" + row + "-" + col + ".png");
                    var cellImage = ImageFactory.getInstance().fromFile(cellFile);
                    rowList.add(cellImage);
                    col++;
                }
                cellImages.add(rowList);
                row++;
            }
            tableNum++;
            // 3.cell ocr
            List<List<TextListBox>> resultCell = ocrImage(cellImages);
            if(Optional.ofNullable(resultCell).isPresent()) {
                // 4.to csv
                ocrToCsv(resultCell, imageDirectory + "/" + tableMatNum + ".csv");
            }
        }
    }

    /**
     * extract tables from image by opencv
     * @param imgPath
     * @return
     */
    public static List<Mat> extractTable(String imgPath) {
        Mat imageMat = imread(imgPath);
        List<Mat> tableList = com.table.extract_tables.ExtractTable.extractTables(imageMat);
        return tableList;
    }

    /**
     * extract tables from image by dnn
     * @param imgPath
     * @return
     */
    public static List<Mat> extractTableDnn(String imgPath) {
        org.opencv.core.Mat img = Imgcodecs.imread(imgPath);
        List<Mat> tableList = com.table.extract_tables_dnn.ExtractTable.extractTables(img);
        return tableList;
    }

    public static void createImageDirectory(String imageDirectory) throws IOException {
        if(Files.notExists(Paths.get(imageDirectory))) {
            Files.createDirectory(Paths.get(imageDirectory));
        }
    }

}

