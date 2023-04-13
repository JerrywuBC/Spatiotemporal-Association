package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.apache.commons.math3.util.MultidimensionalCounter;
import org.lmars.geodata.ais.utils.GsonTools;
import org.lmars.geodata.ais.utils.regexTools;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class getMatchShipByImage implements ISearchXFunction {

    @Override
    public String getName() {
        return "getMatchShipByImage";
    }

    @Override
    public Object handle(QueryXEngine engine, QueryX query, MultiMap params, Buffer postinfo) throws Exception {
        String imageName = params.get("imageName");
        imageName = regexTools.getRootNameFromImageName(imageName);
        String outcome = getMatchResultByImage(engine,imageName);
        return outcome;
    }


    public static String getMatchResultByImage(QueryXEngine engine, String imageName) throws Exception {
        String result = null;
        List list = new ArrayList<JsonObject>();
        StringBuffer QuerySB = new StringBuffer();
        QuerySB.append("select st_x(centerpoint) as lon, st_y(centerpoint) as lat,akeys(mmsi) as keys,avals(mmsi) as vals,uuid from ship where mmsi notnull and imagename like ?");
        SQLHelper.executeSearch(engine.getCacheDB(),QuerySB.toString(),pstat -> {
            pstat.setString(1,"%"+imageName+"%");
        },(rs) -> {
            while (rs.next()) {
                JsonObject res = new JsonObject();
                res.addProperty("lon",rs.getDouble(1));
                res.addProperty("lat",rs.getDouble(2));
                res.addProperty("mmsi",rs.getString(3));
                res.addProperty("value",rs.getString(4));
                res.addProperty("uuid",rs.getString(5));
                list.add(res);
            }
        });

        result = generateOutCome(list);
        return result;
    }

    public static String generateOutCome(List<JsonObject> list) {
        JsonArray result = new JsonArray();
        Iterator it = list.iterator();
        while (it.hasNext()){
            JsonObject jo = (JsonObject) it.next();
            JsonObject outJson = new JsonObject();
            JsonArray outJA = new JsonArray();

            outJson.addProperty("sliceUUID",jo.get("uuid").getAsString());
            outJson.addProperty("lon",jo.get("lon").getAsDouble());
            outJson.addProperty("lat",jo.get("lat").getAsDouble());
            String mmsi = jo.get("mmsi").getAsString();
            String value = jo.get("value").getAsString();
            String[] mmsiList = mmsi.replace("{", "").replace("}", "").split(",");
            String[] valueList = value.replace("{", "").replace("}", "").replace("\"","").split(",");
            for(int i =0;i<mmsiList.length;i++){
                JsonObject out = new JsonObject();
                double mmsiValue = Double.parseDouble(valueList[i]);
                if(mmsiValue>0.01) {
                    out.addProperty("mmsi", mmsiList[i]);
                    out.addProperty("value", mmsiValue);
                    outJA.add(out);
                }
            }
            if(outJA.size()==0)
                continue;
            outJA = GsonTools.sortByAttri(outJA,"value","desc");
            outJson.add("result",outJA);
            result.add(outJson);
        }
        return result.toString();
    }

    public static void main(String[] args) {
        String imageName = "GF2_PMS2_E139.7_N35.2_20211126_L1A0006081639";
        List list = new ArrayList<JsonObject>();
        String result = null;
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://192.168.106.146:5432/postgres";
            String user = "postgres";
            String passWord = "postgres";
            String SQL = "select st_x(centerpoint) as lon, st_y(centerpoint) as lat,akeys(mmsi) as keys,avals(mmsi) as vals,uuid from ship where mmsi notnull and imagename like ?";
            try(Connection conn = DriverManager.getConnection(url, user, passWord);
                PreparedStatement pre =conn.prepareStatement(SQL)){
                pre.setString(1,"%"+imageName+"%");
                ResultSet rs = pre.executeQuery();
                while (rs.next()){
                    JsonObject res = new JsonObject();
                    res.addProperty("lon",rs.getDouble(1));
                    res.addProperty("lat",rs.getDouble(2));
                    res.addProperty("mmsi",rs.getString(3));
                    res.addProperty("value",rs.getString(4));
                    res.addProperty("uuid",rs.getString(5));
                    list.add(res);
                }
                result= generateOutCome(list);
            }catch (Exception e){
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(result);
    }
}
