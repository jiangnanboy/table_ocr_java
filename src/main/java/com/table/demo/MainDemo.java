package com.table.demo;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.translate.TranslateException;
import com.table.ocr_image.TextListBox;
import com.utils.common.CollectionUtil;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.table.extract_cells.ExtractCell.extractCells;
import static com.table.extract_tables.ExtractTable.extractTables;
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
        Mat imageMat = imread(imagePath);
        // 1.extract tables
        List<Mat> resultList = extractTables(imageMat);
        String imageName = Paths.get(imagePath).getFileName().toString().split("\\.")[0];
        String imageParent = Paths.get(imagePath).getParent().toString();
        String imageDirectory  = imageParent + "/" + imageName;
        System.out.println("imageName: " + imageName);
        System.out.println("imageParent: " + imageParent);
        System.out.println("imageDirectory: " + imageDirectory);
        createImageDirectory(imageDirectory);
        int tableNum=0;
        for(Mat tableMat : resultList) {
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

    public static void createImageDirectory(String imageDirectory) throws IOException {
        if(Files.notExists(Paths.get(imageDirectory))) {
            Files.createDirectory(Paths.get(imageDirectory));
        }
    }

}

