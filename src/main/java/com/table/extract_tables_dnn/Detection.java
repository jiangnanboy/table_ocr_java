package com.table.extract_tables_dnn;

/**
 * @author sy
 * @date 2023/4/28 19:44
 */
public class Detection {
    private String label;
    private int labelIndex;
    private float[] bbox;
    private float confidence;

    public String getLabel() {
        return label;
    }

    public int getLabelIndex() {
        return labelIndex;
    }
    public float[] getBbox() {
        return bbox;
    }

    public float getConfidence() {
        return confidence;
    }

    public Detection(String label, int labelIndex, float[] bbox, float confidence) {
        this.label = label;
        this.labelIndex = labelIndex;
        this.bbox = bbox;
        this.confidence = confidence;
    }

}
