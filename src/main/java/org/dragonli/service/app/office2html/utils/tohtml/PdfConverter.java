package org.dragonli.service.app.office2html.utils.tohtml;

import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PdfConverter {

    public Map<String,Object> convert(InputStream in) throws Exception{

        StringBuffer base64 = new StringBuffer();;
        PDDocument document = PDDocument.load(in, (String) null);;
        int size = document.getNumberOfPages();
        PDDocumentCatalog cata = document.getDocumentCatalog();

//        Long randStr = 0l;
//
//        randStr = System.currentTimeMillis();
//        document = new PDDocument();

        BufferedImage image;
        List<String> allImgs = new LinkedList<>();
        Long start = System.currentTimeMillis(), end = null;
        base64.append("<div>");
        PDFRenderer reader = new PDFRenderer(document);
        for (int i = 0; i < size; i++) {
            //image = newPDFRenderer(document).renderImageWithDPI(i,130,ImageType.RGB);
            PDPage page = cata.getPages().get(i);
            final PDResources res  = page.getResources() ;


            List<BufferedImage> imgs = StreamSupport.stream(res.getXObjectNames().spliterator(),false)
                    .filter(v->res.isImageXObject(v))
                    .map(v->{
                        try{
                            return ((PDImageXObject)res.getXObject(v)).getImage();
                        }catch (Exception e){}
                        return null;
            }).filter(v->v!=null).collect(Collectors.toList());
            List<String> imgCoes = imgs.stream().map(v->{
                try{
                    BASE64Encoder encoder = new BASE64Encoder();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(v, "jpg", baos);
                    return encoder.encodeBuffer( baos.toByteArray() ).trim();
                }catch (Exception e){}
                return null;
            }).filter(v->v!=null).collect(Collectors.toList());
            allImgs.addAll(imgCoes);

            base64.append("<img src=\"data:image/jpg;base64,");
            image = reader.renderImage(i, 1.0f);
            //生成图片,保存位置
            //输出流
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", stream);
            base64.append(Base64.encodeBase64String(stream.toByteArray()));
            base64.append("\"/><br/>");
        }

        base64.append("</div>");
        document.close();

        end = System.currentTimeMillis() - start;
        System.out.println("===> Reading pdf times: " + (end / 1000));

        Map<String,Object> result = new HashMap<>();
        result.put("value",base64.toString());
        result.put("imgs",allImgs);

        return result;


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
