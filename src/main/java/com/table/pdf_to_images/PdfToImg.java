package com.table.pdf_to_images;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author sy
 * @date 2023/7/13 22:24
 */
public class PdfToImg {

    public static void main(String[] args) throws IOException {
        pdfToImg("D:/project/idea_workspace/table_ocr_java/img_test/sci_data.pdf","png");
    }

    /**
     * @param pdfFile
     * @param type
     */
    public static void pdfToImg(String pdfFile, String type) throws IOException {
        String pdfName = Paths.get(pdfFile).getFileName().toString().split("\\.")[0];
        String pdfParent = Paths.get(pdfFile).getParent().toString();
        String pdfDirectory  = pdfParent + "/" + pdfName;
        createImageDirectory(pdfDirectory);
        try {
            PDDocument doc = PDDocument.load(Paths.get(pdfFile).toFile());
            PDFRenderer renderer = new PDFRenderer(doc);
            int pageCount = doc.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 144);
                ImageIO.write(image, type, new File(pdfDirectory + "/" + pdfName + "_" + (i + 1) + "." + type));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createImageDirectory(String imageDirectory) throws IOException {
        if(Files.notExists(Paths.get(imageDirectory))) {
            Files.createDirectory(Paths.get(imageDirectory));
        }
    }

}

