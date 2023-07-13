package com.table.ocr_image;

import ai.djl.modality.cv.Image;
import ai.djl.translate.TranslateException;
import com.utils.common.CollectionUtil;
import com.utils.common.PropertiesReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author sy
 * @date 2022/12/30 22:25
 */
public class OcrImage {

    static OcrApp ocrApp;
    static {
        // init model
        String detModelFile = PropertiesReader.get("det_model_file");
        String recModelFile = PropertiesReader.get("rec_model_file");
        Path detectionModelPath = Paths.get(detModelFile);
        Path recognitionModelPath = Paths.get(recModelFile);
        ocrApp = new OcrApp(detectionModelPath, recognitionModelPath);
        ocrApp.init();
    }

    /**
     * table cell ocr
     * @param cellImages
     * @return
     * @throws TranslateException
     */
    public static List<List<TextListBox>> ocrImage(List<List<Image>> cellImages) throws TranslateException {
        List<List<TextListBox>> resultList = CollectionUtil.newArrayList();
        for(List<Image> rowList : cellImages) {
            List<TextListBox> rowResult = CollectionUtil.newArrayList();
            for(Image cell : rowList) {
                List<TextListBox> textListBoxResult = ocrApp.ocrImage(cell);
                TextListBox textListBox = null;
                if(textListBoxResult.size() > 1) {
                    String text = "";
                    List<Float> box = CollectionUtil.newArrayList();
                    float minX = Float.MAX_VALUE;
                    float minY = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE;
                    float maxY = Float.MIN_VALUE;
                    for(TextListBox textResult: textListBoxResult) {
                        text += textResult.getText() + " ";
                        List<Float> positionList = textResult.getBox();
                        if(positionList.get(0) < minX) {
                            minX = positionList.get(0);
                        }
                        if(positionList.get(1) < minY) {
                            minY = positionList.get(1);
                        }
                        if(positionList.get(4) > maxX) {
                            maxX = positionList.get(4);
                        }
                        if(positionList.get(5) > maxY) {
                            maxY = positionList.get(5);
                        }
                    }
                    box.add(minX);
                    box.add(minY);
                    box.add(maxX);
                    box.add(minY);
                    box.add(maxX);
                    box.add(maxY);
                    box.add(minX);
                    box.add(maxY);
                    textListBox = new TextListBox(box, text.strip());

                } else if(textListBoxResult.size() == 1){
                    textListBox = textListBoxResult.get(0);
                }
                rowResult.add(textListBox);
            }
            resultList.add(rowResult);
        }
        ocrApp.closeAllModel();
        return resultList;
    }
}

