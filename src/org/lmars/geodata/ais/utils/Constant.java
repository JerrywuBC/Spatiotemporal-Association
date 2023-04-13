package org.lmars.geodata.ais.utils;

import com.alibaba.fastjson.JSONObject;

public class Constant {

     public static JSONObject monthconstant(){
         JSONObject monthJson = new JSONObject();
         monthJson.put("1",31);
         monthJson.put("2",28);
         monthJson.put("3",31);
         monthJson.put("4",30);
         monthJson.put("5",31);
         monthJson.put("6",30);
         monthJson.put("7",31);
         monthJson.put("8",31);
         monthJson.put("9",30);
         monthJson.put("10",31);
         monthJson.put("11",30);
         monthJson.put("12",31);
         return monthJson;
     }

}
