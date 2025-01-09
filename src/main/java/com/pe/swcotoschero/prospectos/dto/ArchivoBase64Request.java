package com.pe.swcotoschero.prospectos.dto;

public class ArchivoBase64Request {

    private String filename;
    private String fileContent;

    // Getters y Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }
}
