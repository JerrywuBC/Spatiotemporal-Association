package org.lmars.geodata.ais.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class regexTools {

    //从影像名称中获得根名称
    // eg:  GF2_PMS2_E114.4_N30.4_20200518_L1A0004807781-PAN2.tiff
    public static String getRootNameFromImageName(String str) {
        String outcome = "";
        if(str.contains("-")) {
            String pattern = "(.*?)-";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(str);
            while (m.find()) {
                outcome = m.group();
                outcome = outcome.split("/")[outcome.split("/").length - 1];
                outcome = outcome.replaceAll("-", "");
            }
        }else {
            outcome = str;
        }
        return outcome;
    }

    //从影像路径中获取影像名称
    //eg: /home/root123/fyy/OBB/data/tiff/GF2_PMS2_E139.7_N35.2_20211106_L1A0006020055-MSS2_rpcortho_warp_bil_NND.tif
    public static String getImageNameFromImagePath(String str) {
        String outcome ="";
        String pattern = "/(.*?)-";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        while (m.find()) {
            outcome = m.group();
            outcome = outcome.split("/")[outcome.split("/").length - 1];
            outcome = outcome.replaceAll("-", "");
        }
        return outcome;
    }
}
