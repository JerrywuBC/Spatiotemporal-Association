package org.lmars.geodata.ais.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;

public class GsonTools {
//    将JsonArray按照其中一个属性的值进行排序
    public static JsonArray sortByAttri(JsonArray js,String attri,String mode) {
        ArrayList<JsonObject> array = new ArrayList<>();
        JsonArray out =new JsonArray();
        for (int i = 0; i < js.size(); i++) {
            try {
                array.add(js.get(i).getAsJsonObject());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(mode.equals("desc")) {
            // Sort the Java Array List
            Collections.sort(array, (JsonObject jo1, JsonObject jo2) -> {
                double a = jo1.get(attri).getAsDouble();
                double b = jo2.get(attri).getAsDouble();
                //   升序return a > b，降序为 return a < b;
                return (a < b) ? 1 : ((a == b) ? 0 : -1);
            });
        }else {
            // Sort the Java Array List
            Collections.sort(array, (JsonObject jo1, JsonObject jo2) -> {
                double a = jo1.get(attri).getAsDouble();
                double b = jo2.get(attri).getAsDouble();
                //   升序return a > b，降序为 return a < b;
                return (a > b) ? 1 : ((a == b) ? 0 : -1);
            });
        }

        for (int i = 0; i < array.size(); i++) {
            try {
                out.add(array.get(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return out;
    }
}
