
package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.core.utils.ConstantParams;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.ais.utils.ShipInfoUtil;

// 查询重点船舶信息
public class SearchArchiveInfo implements ISearchXFunction {
    @Override
    public String getName() {
        return "SearchArchiveInfo";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        System.out.println("start searchArchivefun");
        long mmsi = Long.parseLong(multiMap.get("mmsi"));
        return getShipArchive(mmsi,queryXEngine);
    }

    public static String getShipArchive(long mmsi, QueryXEngine queryXEngine) throws Exception {
        JsonObject result = new JsonObject();
        ShipInfoUtil.initMilitarySet();
        boolean isMiliatry = true;
        StringBuffer sqlbuf = new StringBuffer();
        sqlbuf.append("select smmsi, sname, stype, sflag, shomeport, sbuildyear, spicturepath, slength, sbreadth, sgrosstonnage, sdwt ");
        if (ShipInfoUtil.MilitaryMap.containsKey(mmsi)) {
            sqlbuf.append(" from military_archive_info ");
        } else {
            isMiliatry = false;
            sqlbuf.append(", cn_country, ssupertype ");
            sqlbuf.append(" from ship_archival_info ");
        }
        boolean isMilitary = isMiliatry;
        sqlbuf.append(" where smmsi = ? ");
        SQLHelper.executeSearch(queryXEngine.getCacheDB(), sqlbuf.toString(), (pstat) -> {
            pstat.setLong(1, mmsi);
        }, (rs) -> {
            while (rs.next()) {
                result.addProperty("mmsi", mmsi);
                result.addProperty("name", rs.getString(2));
                if (isMilitary) {
                    result.addProperty("type", rs.getString(3));
                    result.addProperty("flag", rs.getString(4));
                } else {
                    String supertype = rs.getString(13);
                    result.addProperty("type", ConstantParams.TYPE_MAPS.get(supertype));
                    result.addProperty("flag", rs.getString(12));
                }
                result.addProperty("homeport", rs.getString(5));
                result.addProperty("buildyear", rs.getString(6));
                String spicturepath = rs.getString(7);
                JsonArray array = getPicInfo(spicturepath);
                result.add("pictures", array);
                result.addProperty("length", rs.getFloat(8));
                result.addProperty("breadth", rs.getFloat(9));
                result.addProperty("tonnage", rs.getFloat(10));
                result.addProperty("dwt", rs.getFloat(11));
            }
        });
        return result.toString();
    }
    public static JsonArray getPicInfo(String spicturepath) {
        JsonArray array = new JsonArray();
        if (spicturepath != null) {
            if (spicturepath.contains("jpg") || spicturepath.contains("jpeg")) {
                String paths[] = spicturepath.split(",");
                for (String path : paths) {
                    path = path.replace("\\", "/");
                    if (spicturepath.contains("jpg")){
                        array.add(StartAis.jpgphoto_server + path);
                    }else{
                        array.add(StartAis.jpegphoto_server + path);
                    }
                }
            }
        }
        return array;
    }
}
