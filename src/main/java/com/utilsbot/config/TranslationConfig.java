package com.utilsbot.config;

import net.suuft.libretranslate.Translator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TranslationConfig {

    public TranslationConfig(AppProperties appProperties) {
        String translationApiUrl = appProperties.getTranslationApiUrl();
        if (StringUtils.isNoneBlank(translationApiUrl)) {
            Translator.setUrlApi(translationApiUrl);
        }
    }
}
