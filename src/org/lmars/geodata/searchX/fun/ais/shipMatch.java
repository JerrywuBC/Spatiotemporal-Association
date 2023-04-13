package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.apache.lucene.search.suggest.analyzing.BlendedInfixSuggester;
import org.lmars.geodata.ais.utils.regexTools;
import org.lmars.geodata.core.utils.AreaUtil;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lmars.geodata.ais.utils.geoTools.getSearchSquare;

/**
 * 根据影像名称，更新此影像的匹配结果
 * */

public class shipMatch implements ISearchXFunction {
    @Override
    public String getName() {
        return "shipMatch";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap params, Buffer postinfo) throws Exception {
        String imageName = params.get("imagename");
        imageName = regexTools.getRootNameFromImageName(imageName);
        long startTime = System.currentTimeMillis();
        int num = updatable(queryXEngine,imageName);
        long endTime = System.currentTimeMillis();
        long time = (endTime-startTime)/1000;
        System.out.println("finished, comple  te "+num+" items, time:"+time+"s");
        JsonObject jo = new JsonObject();
        jo.addProperty("items",num);
        jo.addProperty("time",time);
        return jo.toString();
    }

    public static int updatable(QueryXEngine queryXEngine, String imageName) throws Exception {
        ArrayList list = new ArrayList();
        StringBuffer QuerySB = new StringBuffer();
        QuerySB.append("select uuid,score,height,width,st_x(centerpoint),st_y(centerpoint),axisangle,sourcepath from ship where imagename like ?");

        SQLHelper.executeSearch(queryXEngine.getCacheDB(),QuerySB.toString(),pstat -> {
            pstat.setString(1,"%"+imageName+"%");
        },(rs) -> {
            while (rs.next()) {
                JsonObject res = new JsonObject();
                res.addProperty("uuid",rs.getString(1));
                res.addProperty("score",rs.getDouble(2));
                res.addProperty("height",rs.getDouble(3));
                res.addProperty("width",rs.getDouble(4));
                res.addProperty("longitude",rs.getString(5));
                res.addProperty("latitude",rs.getString(6));
                res.addProperty("axisangle",rs.getDouble(7));
                String sourcepath = rs.getString(8);
                res.addProperty("imageTime",getImageTime(queryXEngine,sourcepath));
                list.add(res);
            }
        });


//        遍历未进行处理的船舶
        System.out.println("共"+list.size()+"条数据");
        Iterator ite = list.iterator();
        int updateNum = 0;
        while (ite.hasNext()){
            JsonObject json = (JsonObject) ite.next();
            long uuid = json.get("uuid").getAsLong();
            double longitude = json.get("longitude").getAsDouble();
            double latitude = json.get("latitude").getAsDouble();
            double length = json.get("height").getAsDouble();
            double width = json.get("width").getAsDouble();
            double axisAngle = json.get("axisangle").getAsDouble();
            double score = json.get("score").getAsDouble();
            Integer imageTime = json.get("imageTime").getAsInt();
            String strGeo = getSearchSquare(longitude, latitude, 1000);
            String geometry = AreaUtil.geometryString(strGeo);
            String outcome = getAroundShip.calculateAroundShip(queryXEngine,longitude,latitude,imageTime,600,geometry,
                    10,6,length,width,axisAngle,score,uuid);
            JsonArray ja = JsonParser.parseString(outcome).getAsJsonArray();

            JsonObject js = new JsonObject();
            for (int i=0;i<ja.size();i++){
                JsonObject jo = ja.get(i).getAsJsonObject();
                js.addProperty(jo.get("smmi").getAsString(),jo.get("comprehensiveSimilarity").getAsDouble());
            }
            boolean update = updateMMsi(queryXEngine,uuid,js);
            if(update)
                updateNum++;
        }
        return updateNum;
    }

    public static boolean updateMMsi(QueryXEngine queryXEngine,long uuid,JsonObject js) throws Exception {
        StringBuffer updateSQL = new StringBuffer();
        updateSQL.append("update ship set mmsi =?::hstore where uuid=?");
        StringBuffer mmsiSB = new StringBuffer();
        Iterator sIterator = js.keySet().iterator();
        while (sIterator.hasNext()){
            String mmsi = sIterator.next().toString();
            mmsiSB.append("\"");
            mmsiSB.append(mmsi);
            mmsiSB.append("\"");
            mmsiSB.append("=>");
            mmsiSB.append("\"");
            mmsiSB.append(js.get(mmsi));
            mmsiSB.append("\",");
        }
        if(mmsiSB.length()>0) {
            mmsiSB.deleteCharAt(mmsiSB.length() - 1);
        }else {
            return false;
        }
        System.out.println(mmsiSB.toString());

        SQLHelper.executeUpdate(queryXEngine.getCacheDB(),updateSQL.toString(),(pstat) -> {
            pstat.setString(1,mmsiSB.toString());
            pstat.setString(2,Long.toString(uuid));
        });
        return true;
    }

    public static long getImageTime(QueryXEngine queryXEngine, String sourcepath) throws Exception {
        List<Long> outcome = new ArrayList();
        String imageName = regexTools.getImageNameFromImagePath(sourcepath);
        StringBuffer QuerySB = new StringBuffer();
        QuerySB.append("select imagetime from data_remote_metainfo drm where title like ?");
        SQLHelper.executeSearch(queryXEngine.getCacheDB(),QuerySB.toString(),pstat -> {
            pstat.setString(1,"%"+imageName+"%");
                }
        ,(rs) -> {
            while (rs.next()) {
                outcome.add(getUnixTimeByTimestamp(rs.getString("imageTime")));
            }
        });
        return outcome.size()>0?outcome.get(0):0;
    }


//    从格式时间获得时间戳（东八区时间）
    public static long getUnixTimeByTimestamp(String timeStamp) throws ParseException {
        long unixTime = -1;
        timeStamp = timeStamp.replaceAll(".000","");
        SimpleDateFormat dateFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormate.setTimeZone(TimeZone.getTimeZone("GMT+08:00"));
        Date date = dateFormate.parse(timeStamp);
        unixTime = date.getTime()/1000;
        return unixTime;
    }

    public static void main(String[] args) throws ParseException {
        String str ="2021-11-26 09:22:28";
        SimpleDateFormat dateFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormate.setTimeZone(TimeZone.getTimeZone("GMT+08:00"));
        Date date = dateFormate.parse(str);
        System.out.println(date.getTime()/1000);
    }
}
