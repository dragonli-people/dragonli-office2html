package org.dragonli.service.app.office2html.utils.tohtml;

import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class PdfConverter {

    public String convert(InputStream in) throws Exception{

        StringBuffer base64 = new StringBuffer();;
        PDDocument document = PDDocument.load(in, (String) null);;
        int size = document.getNumberOfPages();;
//        Long randStr = 0l;
//
//        randStr = System.currentTimeMillis();
//        document = new PDDocument();

        BufferedImage image;
        Long start = System.currentTimeMillis(), end = null;
        base64.append("<div>");
        PDFRenderer reader = new PDFRenderer(document);
        for (int i = 0; i < size; i++) {
            //image = newPDFRenderer(document).renderImageWithDPI(i,130,ImageType.RGB);

            base64.append("<img src=\"data:image/jpg;base64,");
            image = reader.renderImage(i, 1.5f);
            //生成图片,保存位置
            //输出流
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", stream);
            base64.append(Base64.encodeBase64String(stream.toByteArray()));
            base64.append("\"/><br/>");
        }

        base64.append("</div>");
        document.close();

        end = System.currentTimeMillis() - start;
        System.out.println("===> Reading pdf times: " + (end / 1000));
        return base64.toString();


//        PDDocument pdDocument = null;
//
//        BufferedInputStream is = new BufferedInputStream(file);
//        PDFParser parser = new PDFParser(is);
//        parser.parse();
//        pdDocument = parser.getPDDocument();
//        List<PDPage> pages = pdDocument.getDocumentCatalog().getAllPages();
//        for (int i = 0; i < pages.size(); i++) {
//            String saveFileName = savePath+"\\"+fileName+i+"."+imgType;
//            PDPage page =  pages.get(i);
//            pdfPage2Img(page,saveFileName,imgType);
//        }
//
//
//        pdDocument.close();


//        return "111";
    }
}
