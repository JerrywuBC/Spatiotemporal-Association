package org.lmars.geodata.ais.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.lmars.geodata.core.utils.ConstantParams;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.QueryXEngine;

import java.util.*;

public class ShipInfoUtil {

    public static Map<Long, String> MilitaryMap = new HashMap<>();

    public static void initMilitarySet() {
        if (MilitaryMap.size() == 0) {
            String sql = "select smmsi, stype from military_archive_info";
            try {
                SQLHelper.executeSearch(PGDriver.getInstance().getPGDB(), sql, (pstat) -> {
                }, (rs) -> {
                    while (rs.next()) {
                        long mmsi = rs.getLong(1);
                        String type = rs.getString(2);
                        MilitaryMap.put(mmsi, type);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static JsonArray appendBrefInfo(QueryXEngine engine, Map<Long, BriefObject> tempMap, String mmsi_values) {
        String info = "select smmsi, sname, stype, sflag from ship_archival_info where smmsi in (" + mmsi_values + ")";
        try {
            SQLHelper.executeSearch(engine.getCacheDB(), info, (pstat) -> {
            }, (rs) -> {
                while (rs.next()) {
                    long mmsi = rs.getLong(1);
                    if (tempMap.containsKey(mmsi)) {
                        BriefObject obj = tempMap.get(mmsi);
                        obj.mmsi = mmsi;
                        obj.name = rs.getString(2);
                        obj.type = rs.getString(3);
                        obj.flag = rs.getString(4);
                        tempMap.put(mmsi, obj);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonArray array = new JsonArray();
        for (Map.Entry<Long, BriefObject> entry : tempMap.entrySet()) {
            JsonObject obj = new JsonObject();
            BriefObject temp = entry.getValue();
            obj.addProperty("mmsi", temp.mmsi);
            obj.addProperty("name", temp.name);
            obj.addProperty("type", temp.type);
            obj.addProperty("flag", temp.flag);
            array.add(obj);
        }
        return array;
    }

    public static JsonArray getPicInfo(String spicturepath) {
        JsonArray array = new JsonArray();
        if (spicturepath != null) {
            if (spicturepath.contains("jpg") || spicturepath.contains("jpeg")) {
                String paths[] = spicturepath.split(",");
                for (String path : paths) {
                    path = path.replace("\\", "/");
                    array.add(ConstantParams.PHOTO_SERVER + path);
                }
            }
        }
        return array;
    }
}
