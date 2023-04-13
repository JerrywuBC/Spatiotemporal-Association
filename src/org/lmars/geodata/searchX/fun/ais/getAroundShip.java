package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.*;
//import com.sun.org.apache.xpath.internal.objects.XNull;
import com.vividsolutions.jts.geom.Coordinate;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.apache.commons.io.FileUtils;
import org.lmars.ais.api.STPoint;
import org.lmars.ais.api.STTrack;
import org.lmars.ais.api.ShipAnalysis;
import org.lmars.geodata.ais.bean.Result;
import org.lmars.geodata.ais.bean.ShipInfo;
import org.lmars.geodata.ais.utils.GsonTools;
import org.lmars.geodata.ais.utils.geoTools;
import org.lmars.geodata.ais.utils.mathTools;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.core.utils.AreaUtil;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import static org.lmars.geodata.ais.utils.geoTools.*;

public class getAroundShip implements ISearchXFunction {
    @Override
    public String getName() {
        return "getAroundShip";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap params, Buffer postinfo) throws Exception {
        double longitude = Double.parseDouble(params.get("longitude"));
        double latitude = Double.parseDouble(params.get("latitude"));
        Integer imageTime = Integer.parseInt(params.get("imageTime"));
        Integer timeInterval = Integer.parseInt(params.get("timeInterval"));

        String strGeo = getSearchSquare(longitude, latitude, Long.parseLong(params.get("distance")));
        String geometry = AreaUtil.geometryString(strGeo);

        Integer maxN = Integer.parseInt(params.get("maxN"));
        Integer trackPointsNum = Integer.parseInt(params.get("trackPointsNum"));
        double length = Double.parseDouble(params.get("length"));
        double width = Double.parseDouble(params.get("width"));
        double axisAngle = Double.parseDouble(params.get("axisangle"));
        double score = Double.parseDouble(params.get("score"));
        Long sliceUUID = Long.parseLong(params.get("sliceuuid"));
        return calculateAroundShip(queryXEngine,longitude,latitude,imageTime,timeInterval,geometry,
                maxN,trackPointsNum,length,width,axisAngle,score,sliceUUID);
    }

    public static String calculateAroundShip(QueryXEngine queryXEngine, double longitude, double latitude,
                                             Integer imageTime, Integer timeInterval, String geometry, Integer maxN,
                                             Integer trackPointsNum, double length, double width, double axisAngle,
                                             double score,long sliceUUID) throws Exception {
        Integer beginTime = imageTime - timeInterval / 2;
        Integer endTime = imageTime + timeInterval / 2;
        ShipAnalysis shipAnalysis = StartAis.getShipAnalysis();
        List<Long> longs = shipAnalysis.computeAllShipInRegions
                (geometry, beginTime, endTime, null, maxN, true);

        JsonArray ja = new JsonArray();
        ConcurrentHashMap<Long, ShipInfo> shipinfo = StartAis.shipinfo;

        for (Long smmi : longs) {
            JsonObject jo = new JsonObject();
            ShipInfo shipInfo = shipinfo.get(smmi);
            jo.addProperty("smmi", smmi);
            if (null != shipInfo) {
                jo.addProperty("cn_country", shipInfo.cn_country);
                jo.addProperty("ssupertype", shipInfo.ssupertype);
            } else {
                jo.addProperty("cn_country", "未知");
                jo.addProperty("ssupertype", "未知");
            }
            List<STTrack> stTracks = shipAnalysis.searchHistoryTrack(smmi,
                    beginTime, endTime, 3600, 1000);
// 去除严重异常点
// Remove serious outliers
            List<STTrack> stTracksAfterFilter = trackFileter(beginTime, endTime, stTracks);
            if (stTracksAfterFilter == null)
                continue;
// 按照影像时间选取影像最近时间的共计trackPointsNum个过滤后的轨迹点,滤除低速点
// Select a total of trackpointsnum filtered track points of the latest time of the image according to the image time,
// and filter out the low-speed points
            List<STPoint> stTracksFiltered = trajectorySimplifiedByTime(stTracksAfterFilter, imageTime, trackPointsNum);

// 按照影像时间选取影像最近时间的共计trackPointsNum个过滤后的轨迹点，不滤除低速点
//  A total of trackpointsnum filtered track points of the latest time of the image are selected according to the image time,
//  and the low-speed points are not filtered out
            List<STPoint> stTracksComplete = trajectoryDuplicateRemovalByTime(stTracksAfterFilter, imageTime, 4);

            if(stTracksFiltered.size()==0)
                return "[]";

            JsonObject archiveJson = new JsonParser().parse(SearchArchiveInfo.getShipArchive(smmi, queryXEngine)).getAsJsonObject();
            archiveJson.remove("pictures");
            ja.add(merge(jo, archiveJson));
            jo.add("tracks", new Gson().toJsonTree(stTracksFiltered));
//            jo.add("partialTracks", new Gson().toJsonTree(stTracksComplete));
            jo.addProperty("comprehensiveSimilarity", getCompreSimilarity(queryXEngine,imageTime, longitude, latitude, length, width,axisAngle,score,sliceUUID,jo)[0]);
            jo.addProperty("trackSimilarity", getCompreSimilarity(queryXEngine,imageTime, longitude, latitude, length, width, axisAngle,score,sliceUUID,jo)[1]);
            jo.addProperty("timeSimilarity", getCompreSimilarity(queryXEngine,imageTime, longitude, latitude, length, width, axisAngle,score,sliceUUID,jo)[2]);
            jo.addProperty("axisAngleSimilarity", getCompreSimilarity(queryXEngine,imageTime, longitude, latitude, length, width, axisAngle,score,sliceUUID,jo)[3]);

            jo.addProperty("shapeSimilarity", getCompreSimilarity(queryXEngine,imageTime, longitude, latitude, length, width, axisAngle,score,sliceUUID,jo)[4]);
            jo.addProperty("featuresVectorSimilarity", getCompreSimilarity(queryXEngine,imageTime, longitude, latitude, length, width, axisAngle,score,sliceUUID,jo)[5]);

        }

        JsonArray out = GsonTools.sortByAttri(ja, "comprehensiveSimilarity", "desc");
        return out.toString();
    }

    //    轨迹过滤，通过限制时间段内的轨迹点数来去除异常轨迹
    //    Track filtering, which removes abnormal tracks by limiting the number of track points in the time period
    public static List<STTrack> trackFileter(long beginTime, long endTime, List<STTrack> stTracks) {
        int interval_minute = (int) (endTime - beginTime) / 60;
        if (stTracks.size() > interval_minute * 1.5) {
            stTracks = null;
        }
        return stTracks;
    }

//  点与轨迹匹配总相似度的核心函数
//  Core function of total similarity between point and trajectory matching
    public static double[] getCompreSimilarity(QueryXEngine queryXEngine, long imageTime, double longitude, double latitude, double height, double width, double axisAngle, double score, long sliceUUID,JsonObject jo) {
        double compreSimilarity = 0;
//      基础相似度计算
//      Basic similarity calculation
        double trackSimilarity = 0;
        double timeSimilarity = 0;
        double axisAngleSimiliraty=0;
 //     附加相似度计算
//      Additional similarity calculation
        double shapeSimilarity= -1;
        double featuresVectorSimilarity = -1;

        double[] similarityIndex = new double[6];
        double[] weight = {1,1,1,1,1,1};

        trackSimilarity = getTrackSimilarity(longitude, latitude, jo);
        timeSimilarity = getTimeSimilarity(longitude, latitude, imageTime,jo);
        axisAngleSimiliraty = getAxisSimilarity(axisAngle,imageTime,jo);

        shapeSimilarity = getShapeSimilarity(width,height,jo);
//      该点存在船舶影像切片
//      There are ship image slices at this point
        if(sliceUUID!=-1)
            featuresVectorSimilarity = getFeaturesVectorSimilarity(queryXEngine,sliceUUID,jo);
        else
            featuresVectorSimilarity = -1;

        similarityIndex[0] = 0.001;
        similarityIndex[1] = new BigDecimal(trackSimilarity).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        similarityIndex[2] = new BigDecimal(timeSimilarity).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        similarityIndex[3] = new BigDecimal(axisAngleSimiliraty).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        similarityIndex[4] = new BigDecimal(shapeSimilarity).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        similarityIndex[5] = new BigDecimal(featuresVectorSimilarity).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();

        // 没有匹配结果或不存在基准值时,权值置为0不进行相似度计算
        // When there is no matching result or there is no benchmark value,
        // the weight is set to 0 and the similarity calculation is not performed
        weight[1]=trackSimilarity==-1?0:weight[1];
        weight[2]=timeSimilarity==-1?0:weight[2];
        weight[3]=axisAngleSimiliraty==-1?0:weight[3];
        weight[4]=shapeSimilarity==-1?0:score;
        weight[5]=featuresVectorSimilarity==-1?0:score;

        int minIndex = getMinIndex(similarityIndex,weight);
        weight[minIndex] = minIndex==0?0:1/similarityIndex[minIndex];

//        double weightSum = weight[1]+weight[2]+weight[3]+weight[4]+weight[5];
        double weightSum = weight[1]+weight[2];
        if(weightSum>0)
            similarityIndex[0] = (weight[1]*trackSimilarity + weight[2]*timeSimilarity) /weightSum;
//            similarityIndex[0] = (weight[1]*trackSimilarity + weight[2]*timeSimilarity + weight[3]*axisAngleSimiliraty) /weightSum;
//            similarityIndex[0] = (weight[1]*trackSimilarity + weight[2]*timeSimilarity + weight[3]*axisAngleSimiliraty +weight[4]*shapeSimilarity+weight[5]*featuresVectorSimilarity) /weightSum;
        else
            similarityIndex[0] = 0;

        return similarityIndex;
    }

    private static class geoPoint {
        double latitude;
        double longitude;
    }

//  获取相似度最低部分的下标,不考虑权值为0的无效部分
//  Obtain the subscript of the lowest part of similarity,
//  regardless of the invalid part with weight 0
    public static int getMinIndex(double[] similarity,double[] weight){
        int index=0;
        double min =2;
        for(int i=1;i<similarity.length;i++){
            if(similarity[i]<min&&weight[i]!=0){
                index=i;
                min = similarity[i];
            }
        }
        return index;
    }

    //    轨迹相似度计算
    // Trajectory similarity calculation
    public static double getTrackSimilarity(double longitude, double latitude, JsonObject jo) {
        double trackSimilarityIndex = 0.001;
        double distance = 0;
        JsonArray jaTrack = jo.get("tracks").getAsJsonArray();
        Iterator<JsonElement> it = jaTrack.iterator();
        List<double[]> points = new ArrayList<>();
        while (it.hasNext()) {
            JsonObject js = it.next().getAsJsonObject();
            geoPoint point = new geoPoint();
            point.latitude = js.get("lat").getAsDouble();
            point.longitude = js.get("lon").getAsDouble();
            double[] xy = {point.latitude, point.longitude};
            points.add(xy);
        }
        double[][] out = points.stream().toArray(double[][]::new);
        if(out.length<2)
            return -1;
        Result result = mathTools.customizeFuncFit(out, latitude);

        if(result!=null) {
             distance = getDistanceByLL(longitude, latitude, result.getPreData(), latitude);
        }else {
             distance = getDistanceByPoints(longitude,latitude,out);
        }
        trackSimilarityIndex = Math.exp(-Math.pow(distance, 2) / (2 * 180 * 180));
        return trackSimilarityIndex<0.001?0.001:trackSimilarityIndex;
    }

    // 时间段相似度计算
    // Time period similarity calculation
    public static double getTimeSimilarity(double longitude, double latitude, long imageTime, JsonObject jo) {
        Coordinate corTar = new Coordinate(longitude, latitude);
        double timeSimilarity = -1;
        JsonArray ja = jo.getAsJsonArray("tracks");
        if(ja.size()<2)
            return -1;
        if (ja.size() >= 2) {
            for (int i = 0; i < ja.size(); i++) {
                JsonObject job = ja.get(i).getAsJsonObject();
                if (imageTime < job.get("time").getAsLong()) {
//            目标点是第一个点
//            The target point is the first point
                    if (i == 0) {
                        double latFir = ja.get(i).getAsJsonObject().get("lat").getAsDouble();
                        double lonFir = ja.get(i).getAsJsonObject().get("lon").getAsDouble();
                        Coordinate corFir = new Coordinate(lonFir, latFir);
                        double dx1 = geographic2Mercator(corFir).y - geographic2Mercator(corTar).y;
                        double dy1 = geographic2Mercator(corFir).x - geographic2Mercator(corTar).x;

                        double latLas = ja.get(i + 1).getAsJsonObject().get("lat").getAsDouble();
                        double lonLas = ja.get(i + 1).getAsJsonObject().get("lon").getAsDouble();
                        Coordinate corLas = new Coordinate(lonLas, latLas);
                        double dx2 = geographic2Mercator(corLas).y - geographic2Mercator(corFir).y;
                        double dy2 = geographic2Mercator(corLas).x - geographic2Mercator(corFir).x;

                        double cos = (dx1 * dx2 + dy1 * dy2) / ((Math.sqrt(dx1 * dx1 + dy1 * dy1) * Math.sqrt(dx2 * dx2 + dy2 * dy2))); // 余弦值

                        timeSimilarity = (cos + 1) / 2;
                        break;
                    } else {
                        double latFir = ja.get(i - 1).getAsJsonObject().get("lat").getAsDouble();
                        double lonFir = ja.get(i - 1).getAsJsonObject().get("lon").getAsDouble();
                        Coordinate corFir = new Coordinate(lonFir, latFir);
                        double dx1 = geographic2Mercator(corTar).y - geographic2Mercator(corFir).y;
                        double dy1 = geographic2Mercator(corTar).x - geographic2Mercator(corFir).x;


                        double latLas = ja.get(i).getAsJsonObject().get("lat").getAsDouble();
                        double lonLas = ja.get(i).getAsJsonObject().get("lon").getAsDouble();
                        Coordinate corLas = new Coordinate(lonLas, latLas);
                        double dx2 = geographic2Mercator(corLas).y - geographic2Mercator(corTar).y;
                        double dy2 = geographic2Mercator(corLas).x - geographic2Mercator(corTar).x;

                        double cos = (dx1 * dx2 + dy1 * dy2) / ((Math.sqrt(dx1 * dx1 + dy1 * dy1) * Math.sqrt(dx2 * dx2 + dy2 * dy2))); // 余弦值
                        timeSimilarity = (cos + 1) / 2;
                        break;
                    }
                }
                // 目标点是最后一个点
                // The last point is the goal
                if (timeSimilarity == -2) {
                    double latLas = ja.get(ja.size() - 1).getAsJsonObject().get("lat").getAsDouble();
                    double lonLas = ja.get(ja.size() - 1).getAsJsonObject().get("lon").getAsDouble();
                    Coordinate corlas = new Coordinate(lonLas, latLas);
                    double dx1 = geographic2Mercator(corTar).y - geographic2Mercator(corlas).y;
                    double dy1 = geographic2Mercator(corTar).x - geographic2Mercator(corlas).x;

                    double latFir = ja.get(ja.size() - 2).getAsJsonObject().get("lat").getAsDouble();
                    double lonFir = ja.get(ja.size() - 2).getAsJsonObject().get("lon").getAsDouble();
                    Coordinate corFir = new Coordinate(lonFir, latFir);
                    double dx2 = geographic2Mercator(corlas).y - geographic2Mercator(corFir).y;
                    double dy2 = geographic2Mercator(corlas).x - geographic2Mercator(corFir).x;

                    double cos = (dx1 * dx2 + dy1 * dy2) / ((Math.sqrt(dx1 * dx1 + dy1 * dy1) * Math.sqrt(dx2 * dx2 + dy2 * dy2))); // 余弦值
                    timeSimilarity = (cos + 1) / 2;
                }
            }
        }

        return timeSimilarity<0.001?0.001:timeSimilarity;
    }

    public static double getAxisSimilarity(double axisAngle,double imageTime, JsonObject jo) {
        double axisAngleSimilarityIndex = 0;
        double trackAngle = 361;
        JsonArray ja = jo.getAsJsonArray("tracks");

        if(axisAngle==-1)
            return -1;
        if(ja.size()<2)
            return -1;

        if (ja.size() >= 2) {
            for (int i = 0; i < ja.size(); i++) {
                JsonObject job = ja.get(i).getAsJsonObject();
                if (imageTime < job.get("time").getAsLong()) {
                    if (i == 0) {
                        double latFir = ja.get(i).getAsJsonObject().get("lat").getAsDouble();
                        double lonFir = ja.get(i).getAsJsonObject().get("lon").getAsDouble();
                        double latLas = ja.get(i + 1).getAsJsonObject().get("lat").getAsDouble();
                        double lonLas = ja.get(i + 1).getAsJsonObject().get("lon").getAsDouble();
                        trackAngle=geoTools.getAngleByLL(lonFir,latFir,lonLas,latLas);
                    }else {
                        double latFir = ja.get(i - 1).getAsJsonObject().get("lat").getAsDouble();
                        double lonFir = ja.get(i - 1).getAsJsonObject().get("lon").getAsDouble();
                        double latLas = ja.get(i).getAsJsonObject().get("lat").getAsDouble();
                        double lonLas = ja.get(i).getAsJsonObject().get("lon").getAsDouble();
                        trackAngle=geoTools.getAngleByLL(lonFir,latFir,lonLas,latLas);
                    }
                }
                if (trackAngle == 361) {
                    double latLas = ja.get(ja.size() - 1).getAsJsonObject().get("lat").getAsDouble();
                    double lonLas = ja.get(ja.size() - 1).getAsJsonObject().get("lon").getAsDouble();
                    double latFir = ja.get(ja.size() - 2).getAsJsonObject().get("lat").getAsDouble();
                    double lonFir = ja.get(ja.size() - 2).getAsJsonObject().get("lon").getAsDouble();
                    trackAngle=geoTools.getAngleByLL(lonFir,latFir,lonLas,latLas);
                }
            }
        }
        double deltaAngle = Math.abs(axisAngle-trackAngle);
        if(deltaAngle<90)
            axisAngleSimilarityIndex = Math.cos(Math.toRadians(deltaAngle));
        else
            axisAngleSimilarityIndex = Math.cos(Math.toRadians(180-deltaAngle));
        return axisAngleSimilarityIndex<0.001?0.001:axisAngleSimilarityIndex;
    }

    public static double getShapeSimilarity(double width,double height,JsonObject jo) {
        double shapeSimilarityIndex = -1;
        double output = 0;

        double archiveHeight = jo.get("length")==null?-1:jo.get("length").getAsDouble();
        double archiveWidth = jo.get("breadth")==null?-1:jo.get("breadth").getAsDouble();

        if(archiveHeight==-1||archiveWidth==-1)
            return -1;
        if(width==-1||height==-1)
            return -1;

        double heightDev = Math.abs(archiveWidth-width)/width;
        double widthDev = Math.abs(archiveHeight-height)/height;
        shapeSimilarityIndex = 1/Math.exp(heightDev+widthDev);
        output = shapeSimilarityIndex<0.5?0.001:shapeSimilarityIndex;
        return output;
    }

//    获得特征向量相似度,如果船舶档案库中没有基准影像切片则返回-1
// Obtain the similarity of feature vectors.
// If there is no reference image slice in the ship archive, return - 1
    public static double getFeaturesVectorSimilarity(QueryXEngine queryXEngine,long sliceUUID,JsonObject jo)  {
        double outcome = -1;
        String mmsi = jo.get("smmi").getAsString();
        JsonArray ja = new JsonArray();

//      根据候选船舶的mmsi号，从船舶特征表中获取其影像切片的sliceUUID
//      According to the MMSI number of the candidate ship,
//      the sliceuuid of its image slice is obtained from the ship feature table
        StringBuffer QuerySB = new StringBuffer();
        QuerySB.append("select akeys(shapevector) as vectorUUID,avals(shapevector) as vectorSim from ship_archival_info sai where smmsi = ?");
        try {
           SQLHelper.executeSearch(queryXEngine.getCacheDB(), QuerySB.toString(), pstat -> {
                       pstat.setInt(1, Integer.parseInt(mmsi));
                   }
                   , (rs) -> {
                       while (rs.next()) {
                           JsonObject joTemp = new JsonObject();
                           joTemp.addProperty("vectorUUID", rs.getInt("vectorUUID"));
                           joTemp.addProperty("vectorSim", rs.getDouble("vectorSim"));
                           ja.add(joTemp);
                       }
                   });
        }catch (Exception e){
           e.printStackTrace();
        }

        long vU =0;
        if(ja.size()>0) {
            vU = ja.get(0).getAsJsonObject().get("vectorUUID").getAsLong();
        }
//      该候选船舶确实存在影像切片
//      The candidate ship does have image slices
        if(vU!=0){
            double featuresVectorSimilarity = 0;
            double weight = 0.001;
            for(int i =0;i<ja.size();i++)
            {
                JsonObject joTemp2 = ja.get(i).getAsJsonObject();
                int vectorUUID = joTemp2.get("vectorUUID").getAsInt();
                double vectorSim = joTemp2.get("vectorSim").getAsDouble();

                HttpClient httpClient = new HttpClient();

                httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(10*000);


                StringBuilder sb = new StringBuilder();

                sb.append("http://192.168.106.146:8888/compare?");
                sb.append("baseVectorUUID=");
                sb.append(sliceUUID).append("&");
                sb.append("candidateVectorUUID=");
                sb.append(vectorUUID);

                GetMethod getMethod = new GetMethod(sb.toString());

                getMethod.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, 10*000);

                getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());

                String response = null;


                try {
                    int statusCode = httpClient.executeMethod(getMethod);
                    if (statusCode != HttpStatus.SC_OK){
                        System.err.println("请求出错：" + getMethod.getStatusLine());
                    }

                    Header[] headers = getMethod.getResponseHeaders();


                    byte[] responseBody = getMethod.getResponseBody();
                    response = new String(responseBody, "UTF-8");
                    JsonObject joHttp = JsonParser.parseString(response).getAsJsonObject();
                    if(joHttp.get("code").getAsInt()==200) {
                        double sim = joHttp.get("similarity").getAsDouble()==-1?0:joHttp.get("similarity").getAsDouble();
                        featuresVectorSimilarity = featuresVectorSimilarity+sim*vectorSim;
                        weight = weight+vectorSim;
                    }else {
                        featuresVectorSimilarity = -1;
                    }

                } catch (HttpException e) {
                    System.out.println("请检查输入的URL!");
                    e.printStackTrace();
                } catch (IOException e){

                    System.out.println("发生网络异常!");
                }finally {
                    getMethod.releaseConnection();
                }
            }
            outcome =featuresVectorSimilarity/weight;
        }

        return outcome;
    }

    //将轨迹与属性融合为一个json
// Merge tracks and attributes into a JSON
    public static JsonObject merge(JsonObject firstObj, JsonObject secondObj) {
        for (String keyInSecondObj : secondObj.keySet()) {
            if (!firstObj.has(keyInSecondObj)) {
                firstObj.add(keyInSecondObj, secondObj.get(keyInSecondObj));
            }
        }
        return firstObj;
    }

    //    选取给定时间前后共计trackPointsNum个点
//    Select a total of trackpointsnum points before and after a given time
    public static List<STPoint> trajectorySimplifiedByTime(List<STTrack> stTracks, Integer imageTime, int trackPointsNum) {
        List<STPoint> originalList = new ArrayList<STPoint>();
        List<STPoint> list = new ArrayList<STPoint>();
        int indextime = -1;

//        for (int i = 0; i < stTracks.size(); i++) {
//            for (int j = 0; j < stTracks.get(i).pts.size(); j++) {
//                int sog = stTracks.get(i).pts.get(j).sog;
//                if (stTracks.get(i).pts.get(j).time != indextime && sog > 1) {
//                    indextime = stTracks.get(i).pts.get(j).time;
//                    originalList.add(stTracks.get(i).pts.get(j));
//                }
//            }
//        }


        for (int i = 0; i < stTracks.size(); i++) {
            for (int j = 0; j < stTracks.get(i).pts.size(); j++) {
                originalList.add(stTracks.get(i).pts.get(j));
            }
        }
        if(originalList.size()<trackPointsNum)
            return originalList;
//        过滤掉重复点和低速点
// Filter out duplicate points and low speed points
        for (int i = 0; i < originalList.size(); i++) {
                int sog = originalList.get(i).sog;
                if (originalList.get(i).time != indextime && sog > 1) {
                    indextime = originalList.get(i).time;
                    list.add(originalList.get(i));
            }
        }

        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (imageTime < list.get(i).time) {
                index = i;
                break;
            }
        }

        int startIndex = (index - trackPointsNum / 2) < 0 ? 0 : (index - trackPointsNum / 2);
        int endIndex = (index + trackPointsNum / 2) > list.size() ? list.size() - 1 : (index + trackPointsNum / 2 - 1);
        return list.subList(startIndex, endIndex + 1);
    }

    public static List<STPoint> trajectoryDuplicateRemovalByTime(List<STTrack> stTracks, Integer imageTime, int trackPointsNum) {
        List<STPoint> originalList = new ArrayList<STPoint>();
        List<STPoint> list = new ArrayList<STPoint>();
        int indextime = -1;

//        for (int i = 0; i < stTracks.size(); i++) {
//            for (int j = 0; j < stTracks.get(i).pts.size(); j++) {
//                int sog = stTracks.get(i).pts.get(j).sog;
//                if (stTracks.get(i).pts.get(j).time != indextime && sog > 1) {
//                    indextime = stTracks.get(i).pts.get(j).time;
//                    originalList.add(stTracks.get(i).pts.get(j));
//                }
//            }
//        }


        for (int i = 0; i < stTracks.size(); i++) {
            for (int j = 0; j < stTracks.get(i).pts.size(); j++) {
                originalList.add(stTracks.get(i).pts.get(j));
            }
        }

        if(originalList.size()<trackPointsNum)
            return originalList;
//        过滤掉重复点
//        Filter out duplicate points
        for (int i = 0; i < originalList.size(); i++) {
            int sog = originalList.get(i).sog;
            if (originalList.get(i).time != indextime) {
                indextime = originalList.get(i).time;
                list.add(originalList.get(i));
            }
        }

        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (imageTime < list.get(i).time) {
                index = i;
                break;
            }
        }

        int startIndex = (index - trackPointsNum / 2) < 0 ? 0 : (index - trackPointsNum / 2);
        int endIndex = (index + trackPointsNum / 2) > list.size() ? list.size() - 1 : (index + trackPointsNum / 2 - 1);
        return list.subList(startIndex, endIndex + 1);
    }

    public static void main(String[] args) throws IOException {
        test1();
    }

    public static void test1() throws IOException {
        File inputFile = new File("./File/originalTrack.json");
        String content = FileUtils.readFileToString(inputFile, "UTF-8");
        JsonArray ja = JsonParser.parseString(content).getAsJsonArray();
        ArrayList<JsonObject> arr = new ArrayList();
        int time = -1;
        for (int i = 0; i < ja.size(); i++) {
            JsonArray ja2 = ja.get(i).getAsJsonObject().get("pts").getAsJsonArray();
            for (int j = 0; j < ja2.size(); j++) {
                int indextime = ja2.get(j).getAsJsonObject().get("time").getAsInt();
                int sog = ja2.get(j).getAsJsonObject().get("sog").getAsInt();
                if (indextime != time && sog > 1)
                    arr.add(ja2.get(j).getAsJsonObject());
                time = indextime;
            }
        }
        int index = -1;
        for (int i = 0; i < arr.size(); i++) {
            if (1637889600 < arr.get(i).get("time").getAsDouble()) {
                index = i;
                break;
            }
        }
        int trackPointsNum = 6;
        int startIndex = (index - trackPointsNum / 2) < 0 ? 0 : (index - trackPointsNum / 2);
        int endIndex = (index + trackPointsNum / 2) > arr.size() ? arr.size() - 1 : (index + trackPointsNum / 2 - 1);
        List<JsonObject> out = arr.subList(startIndex, endIndex + 1);
        File outFile = new File("./File/out.json");
        FileUtils.writeStringToFile(outFile, out.toString(), "UTF-8");
    }
}
