package org.dragonli.service.app.docx2html.utils.tohtml;

public class ConverterUtil {
    public static String formatResult(String result){
        result = result.replaceAll("(?i)<html", "<div").replaceAll("(?i)</html", "</div").replaceAll("(?i)<body",
                "<div").replaceAll("(?i)</body", "</div").replaceAll("(?i)<style", "<style scoped");
        result = result.replaceAll("(?i)<meta\\s+[^<>]*>", "").replaceAll("(?i)<head\\s*[^<>]*>", "").replaceAll(
                "(?i)<\\/head\\s*[^<>]*>", "");
        return result;
    }
}
