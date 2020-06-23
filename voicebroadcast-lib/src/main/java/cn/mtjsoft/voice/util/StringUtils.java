package cn.mtjsoft.voice.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @describe 字符相关的工具类
 * @ideas
 */

public class StringUtils {

    /**
     * 提取字符串中的 数字 带小数点 ，没有就返回""
     *
     * @param money
     * @return
     */
    public static String getMoney(String money) {
        Pattern pattern = Pattern.compile("(\\d+\\.\\d+)");
        Matcher m = pattern.matcher(money);
        if (m.find()) {
            money = m.group(1) == null ? "" : m.group(1);
        } else {
            pattern = Pattern.compile("(\\d+)");
            m = pattern.matcher(money);
            if (m.find()) {
                money = m.group(1) == null ? "" : m.group(1);
            } else {
                money = "";
            }
        }

        return money;
    }
}
