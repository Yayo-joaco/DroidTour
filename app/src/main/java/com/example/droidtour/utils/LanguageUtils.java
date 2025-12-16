package com.example.droidtour.utils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageUtils {

    private static final Map<String, String> LANGUAGE_MAP = new HashMap<>();
    private static final Map<String, String> FLAG_MAP = new HashMap<>();

    static {
        LANGUAGE_MAP.put("en", "Inglés");
        LANGUAGE_MAP.put("pt", "Portugués");
        LANGUAGE_MAP.put("fr", "Francés");
        LANGUAGE_MAP.put("es", "Español");
        LANGUAGE_MAP.put("qu", "Quechua");
        LANGUAGE_MAP.put("de", "Alemán");
        LANGUAGE_MAP.put("it", "Italiano");
        LANGUAGE_MAP.put("zh", "Chino");
        LANGUAGE_MAP.put("ja", "Japonés");
        LANGUAGE_MAP.put("ko", "Coreano");
        LANGUAGE_MAP.put("ru", "Ruso");
        LANGUAGE_MAP.put("ar", "Árabe");
        LANGUAGE_MAP.put("nl", "Neerlandés");
        LANGUAGE_MAP.put("sv", "Sueco");
        LANGUAGE_MAP.put("no", "Noruego");
        LANGUAGE_MAP.put("da", "Danés");
        LANGUAGE_MAP.put("fi", "Finlandés");
        LANGUAGE_MAP.put("pl", "Polaco");
        LANGUAGE_MAP.put("tr", "Turco");
        LANGUAGE_MAP.put("el", "Griego");

        FLAG_MAP.put("en", "🇺🇸");
        FLAG_MAP.put("pt", "🇧🇷");
        FLAG_MAP.put("fr", "🇫🇷");
        FLAG_MAP.put("es", "🇪🇸");
        FLAG_MAP.put("qu", "🇵🇪");
        FLAG_MAP.put("de", "🇩🇪");
        FLAG_MAP.put("it", "🇮🇹");
        FLAG_MAP.put("zh", "🇨🇳");
        FLAG_MAP.put("ja", "🇯🇵");
        FLAG_MAP.put("ko", "🇰🇷");
        FLAG_MAP.put("ru", "🇷🇺");
        FLAG_MAP.put("ar", "🇸🇦");
        FLAG_MAP.put("nl", "🇳🇱");
        FLAG_MAP.put("sv", "🇸🇪");
        FLAG_MAP.put("no", "🇳🇴");
        FLAG_MAP.put("da", "🇩🇰");
        FLAG_MAP.put("fi", "🇫🇮");
        FLAG_MAP.put("pl", "🇵🇱");
        FLAG_MAP.put("tr", "🇹🇷");
        FLAG_MAP.put("el", "🇬🇷");
    }

    public static String toNames(List<String> codes) {
        if (codes == null || codes.isEmpty()) return "--";

        List<String> names = new ArrayList<>();
        for (String code : codes) {
            names.add(LANGUAGE_MAP.getOrDefault(code, code));
        }
        return TextUtils.join(" · ", names);
    }

    public static String toFlags(List<String> codes) {
        if (codes == null || codes.isEmpty()) return "--";

        StringBuilder sb = new StringBuilder();
        for (String code : codes) {
            sb.append(FLAG_MAP.getOrDefault(code, "")).append(" ");
        }
        return sb.toString().trim();
    }
}
