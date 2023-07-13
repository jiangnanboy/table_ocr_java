package com.table.ocr_to_csv;

import com.table.ocr_image.TextListBox;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author sy
 * @date 2023/7/12 22:31
 */
public class OcrToCsv {
    public static void ocrToCsv(List<List<TextListBox>> textListBox, String csvPath) {
        try(BufferedWriter br = Files.newBufferedWriter(Paths.get(csvPath), StandardCharsets.UTF_8)) {
            for(List<TextListBox> rowBoxes : textListBox) {
                String rowLine = String.join(",", rowBoxes.stream().map(row -> {
                    if(Optional.ofNullable(row).isPresent()) {
                        return row.getText();
                    } else {
                        return "";
                    }
                }).collect(Collectors.toList()));
                br.write(rowLine);
                br.newLine();
                br.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
