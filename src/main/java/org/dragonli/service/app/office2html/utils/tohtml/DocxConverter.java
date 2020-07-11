package org.dragonli.service.app.office2html.utils.tohtml;


import fr.opensagres.poi.xwpf.converter.xhtml.Base64EmbedImgManager;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class DocxConverter {

    public String convert(InputStream file) throws Exception{
        XWPFDocument docxDocument = new XWPFDocument(file);
        XHTMLOptions options = XHTMLOptions.create();

        //图片转base64
        options.setImageManager(new Base64EmbedImgManager());
        // 转换htm11
        ByteArrayOutputStream htmlStream = new ByteArrayOutputStream();
        XHTMLConverter.getInstance().convert(docxDocument, htmlStream, options);
        String htmlStr = htmlStream.toString();
        return ConverterUtil.formatResult(htmlStr);

    }
}
