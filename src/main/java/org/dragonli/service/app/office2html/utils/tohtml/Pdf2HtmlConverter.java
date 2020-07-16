package org.dragonli.service.app.office2html.utils.tohtml;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.pdfdom.PDFDomTree;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Pdf2HtmlConverter {
    public Map<String,Object> convert(InputStream in) throws Exception {
        StringBuilderWriter writer = new StringBuilderWriter();
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PDDocument document = PDDocument.load(in, (String) null);
        PDFDomTree pdfDomTree = new PDFDomTree();
//        int size = document.getNumberOfPages();
        pdfDomTree.writeText(document, writer);

        Map<String,Object> result = new HashMap<>();
        result.put("value",writer.toString());
        return result;
    }
}
