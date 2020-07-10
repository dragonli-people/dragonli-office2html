package org.dragonli.service.app.docx2html.utils.tohtml;

import org.apache.poi.hwpf.HWPFDocumentCore;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.converter.WordToHtmlUtils;
import org.apache.poi.hwpf.usermodel.Picture;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;

// https://blog.kuoruan.com/43.html/3

public class DocConverter {

    public String convert(InputStream file) throws Exception{
        Document newDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        InlineImageWordToHtmlConverter wordToHtmlConverter = new InlineImageWordToHtmlConverter(newDocument);

        return wordToHtmlConverter.convert(file);
    }

    class InlineImageWordToHtmlConverter extends WordToHtmlConverter {
        public InlineImageWordToHtmlConverter(Document document) {
            super(document);
        }

        /**
         * 重写方法
         *
         * @param currentBlock
         * @param inlined
         * @param picture
         */
        @Override
        protected void processImageWithoutPicturesManager(Element currentBlock, boolean inlined, Picture picture) {
            Element imgNode = currentBlock.getOwnerDocument().createElement("img");// 创建img标签
            StringBuilder sb = new StringBuilder();
            // Base64处理
            sb.append(Base64.getMimeEncoder().encodeToString(picture.getRawContent()));
            sb.insert(0, "data:" + picture.getMimeType() + ";base64,");
            imgNode.setAttribute("src", sb.toString());
            currentBlock.appendChild(imgNode);
        }

        /**
         * Doc转换为Html
         *
         * @param file Doc文件
         */
        public String convert(InputStream file) {
            ByteArrayOutputStream out = null;
            try {
                HWPFDocumentCore wordDocument = WordToHtmlUtils.loadDoc(file);


                this.processDocument(wordDocument);
                Document htmlDocument = this.getDocument();
                out = new ByteArrayOutputStream();
                DOMSource domSource = new DOMSource(htmlDocument);
                StreamResult streamResult = new StreamResult(out);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer serializer = tf.newTransformer();
                serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                serializer.setOutputProperty(OutputKeys.INDENT, "yes");
                serializer.setOutputProperty(OutputKeys.METHOD, "html");
                serializer.transform(domSource, streamResult);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            String result = new String(out.toByteArray());
            return ConverterUtil.formatResult(result);

        }
    }
}