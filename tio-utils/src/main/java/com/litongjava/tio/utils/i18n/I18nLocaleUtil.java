package com.litongjava.tio.utils.i18n;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class I18nLocaleUtil {

  private static final Map<String, String> CODE_TO_NAME;

  static {
    Map<String, String> map = new HashMap<>();

    map.put("en_US", "English (United States)");
    map.put("zh_CN", "Chinese (China)");
    map.put("zh_TW", "Chinese (Taiwan)");
    map.put("ja_JP", "Japanese (Japan)");
    map.put("ko_KR", "Korean (South Korea)");
    map.put("fr_FR", "French (France)");
    map.put("de_DE", "German (Germany)");
    map.put("es_ES", "Spanish (Spain)");
    map.put("it_IT", "Italian (Italy)");
    map.put("pt_BR", "Portuguese (Brazil)");
    map.put("ru_RU", "Russian (Russia)");
    map.put("ar_SA", "Arabic (Saudi Arabia)");
    map.put("hi_IN", "Hindi (India)");
    map.put("bn_BD", "Bengali (Bangladesh)");
    map.put("nl_NL", "Dutch (Netherlands)");
    map.put("sv_SE", "Swedish (Sweden)");
    map.put("tr_TR", "Turkish (Turkey)");
    map.put("pl_PL", "Polish (Poland)");
    map.put("el_GR", "Greek (Greece)");
    map.put("th_TH", "Thai (Thailand)");
    map.put("vi_VN", "Vietnamese (Vietnam)");
    map.put("id_ID", "Indonesian (Indonesia)");
    map.put("ms_MY", "Malay (Malaysia)");
    map.put("cs_CZ", "Czech (Czech Republic)");
    map.put("sk_SK", "Slovak (Slovakia)");
    map.put("uk_UA", "Ukrainian (Ukraine)");
    map.put("da_DK", "Danish (Denmark)");
    map.put("fi_FI", "Finnish (Finland)");
    map.put("no_NO", "Norwegian (Norway)");
    map.put("sv_FI", "Swedish (Finland)");
    map.put("ro_RO", "Romanian (Romania)");
    map.put("hu_HU", "Hungarian (Hungary)");
    map.put("he_IL", "Hebrew (Israel)");
    map.put("ar_EG", "Arabic (Egypt)");
    map.put("fa_IR", "Persian (Iran)");
    map.put("ur_PK", "Urdu (Pakistan)");
    map.put("ta_IN", "Tamil (India)");
    map.put("te_IN", "Telugu (India)");
    map.put("ml_IN", "Malayalam (India)");
    map.put("bg_BG", "Bulgarian (Bulgaria)");
    map.put("sr_RS", "Serbian (Serbia)");
    map.put("hr_HR", "Croatian (Croatia)");
    map.put("sl_SI", "Slovenian (Slovenia)");
    map.put("lt_LT", "Lithuanian (Lithuania)");
    map.put("lv_LV", "Latvian (Latvia)");
    map.put("et_EE", "Estonian (Estonia)");
    map.put("is_IS", "Icelandic (Iceland)");
    map.put("ga_IE", "Irish (Ireland)");
    map.put("af_ZA", "Afrikaans (South Africa)");
    map.put("sw_KE", "Swahili (Kenya)");
    map.put("am_ET", "Amharic (Ethiopia)");
    map.put("km_KH", "Khmer (Cambodia)");
    map.put("lo_LA", "Lao (Laos)");
    map.put("my_MM", "Burmese (Myanmar)");
    map.put("ne_NP", "Nepali (Nepal)");
    map.put("fo_FO", "Faroese (Faroe Islands)");
    map.put("sq_AL", "Albanian (Albania)");
    map.put("hy_AM", "Armenian (Armenia)");
    map.put("ka_GE", "Georgian (Georgia)");
    map.put("kk_KZ", "Kazakh (Kazakhstan)");
    map.put("uz_UZ", "Uzbek (Uzbekistan)");
    map.put("mn_MN", "Mongolian (Mongolia)");
    map.put("bo_CN", "Tibetan (China)");
    map.put("ug_CN", "Uyghur (China)");
    map.put("sa_IN", "Sanskrit (India)");
    map.put("eo_XX", "Esperanto");

    CODE_TO_NAME = Collections.unmodifiableMap(map);
  }

  private I18nLocaleUtil() {
  }

  /**
   * Returns language and region name in English.
   *
   * @param code locale code, e.g. en_US
   * @return English name, e.g. English (United States)
   */
  public static String getLanguageRegion(String code) {
    if (code == null) {
      return "Unknown";
    }
    return CODE_TO_NAME.getOrDefault(code, "Unknown (" + code + ")");
  }
}
