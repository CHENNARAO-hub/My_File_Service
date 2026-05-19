package com.enterprise.fileservice.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfService {

    public String extractText(MultipartFile file) {

        try (PDDocument document =
                 PDDocument.load(file.getInputStream())) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            return stripper.getText(document);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
