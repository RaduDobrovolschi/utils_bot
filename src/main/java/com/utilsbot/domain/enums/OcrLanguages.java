package com.utilsbot.domain.enums;

public enum OcrLanguages {
    ARA, BUL, CHS, HRV, CZE, DAN, DUT, ENG, FIN, FRE, GER, GRE, HUN, KOR, ITA, JPN, POL, POR, RUS, SLV, SPA, SWE, TUR,
    HIN, KAN, PER, TEL, TAM, TAI, VIE; // engine 3

    public static OcrLanguages of(String langCode) {
        return switch (langCode) {
            case "ar", "arabic", "ara" -> ARA;
            case "bg", "bulgarian", "bul" -> BUL;
            case "zh", "chinese", "chs" -> CHS;
            case "hr", "croatian", "hrv" -> HRV;
            case "cs", "czech", "cze" -> CZE;
            case "da", "danish", "dan" -> DAN;
            case "nl", "dutch", "dut" -> DUT;
            case "fi", "finnish", "fin" -> FIN;
            case "fr", "french", "fre" -> FRE;
            case "de", "german", "ger" -> GER;
            case "el", "greek", "gre" -> GRE;
            case "hu", "hungarian", "hun" -> HUN;
            case "ko", "korean", "kor" -> KOR;
            case "it", "italian", "ita" -> ITA;
            case "ja", "japanese", "jpn" -> JPN;
            case "pl", "polish", "pol" -> POL;
            case "pt", "portuguese", "por" -> POR;
            case "ru", "russian", "rus" -> RUS;
            case "sl", "slovenian", "slv" -> SLV;
            case "es", "spanish", "spa" -> SPA;
            case "sv", "swedish", "swe" -> SWE;
            case "tr", "turkish", "tur" -> TUR;

            case "hi", "hindi", "hin" -> HIN;
            case "kn", "kannada", "kan" -> KAN;
            case "fa", "persian", "per" -> PER;
            case "te", "telugu", "tel" -> TEL;
            case "ta", "tamil", "tam" -> TAM;
            case "th", "thai", "tai" -> TAI;
            case "vi", "vietnamese", "vie" -> VIE;

            default -> ENG;
        };
    }

    public static boolean isE3(OcrLanguages ocrLanguages) {
        if (ocrLanguages.equals(HIN) ||
            ocrLanguages.equals(KAN) ||
            ocrLanguages.equals(PER) ||
            ocrLanguages.equals(TEL) ||
            ocrLanguages.equals(TAM) ||
            ocrLanguages.equals(TAI) ||
            ocrLanguages.equals(VIE))
            return true;
        return false;
    }
}
