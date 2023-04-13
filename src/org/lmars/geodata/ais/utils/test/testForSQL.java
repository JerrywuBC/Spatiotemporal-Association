package org.lmars.geodata.ais.utils.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.FileUtils;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.QueryXEngine;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class testForSQL {

    public static void main(String[] args) throws IOException {
        File inputFile = new File("D:\\VM15Disk\\workspaceShare\\workspace\\shanghai2\\aisproject\\File\\jo.json");
        String content = FileUtils.readFileToString(inputFile, "UTF-8");
        long sliceUUID = Long.parseLong("16480446362450002") ;
        JsonObject jo = JsonParser.parseString(content).getAsJsonObject();
        System.out.println(getFeaturesVectorSimilarity(sliceUUID,jo));
    }

    public static void test1() {
        ArrayList<String> mmsi = new ArrayList<String>();
        ArrayList<Double> sim = new ArrayList<Double>();
        mmsi.add("431018152");
        sim.add(0.9611592173269203);
        mmsi.add("431008542");
        sim.add(0.006019775455131732);
        mmsi.add("431000841");
        sim.add(0.0038417632622723854);
        StringBuffer mmsiSB = new StringBuffer();
        for(int i=0;i<mmsi.size();i++){
            mmsiSB.append("\"");
            mmsiSB.append(mmsi.get(i));
            mmsiSB.append("\"");
            mmsiSB.append("=>");
            mmsiSB.append("\"");
            mmsiSB.append(sim.get(i));
            mmsiSB.append("\",");
        }
        mmsiSB.deleteCharAt(mmsiSB.length()-1);
        StringBuffer updateSQL = new StringBuffer();
        updateSQL.append("update ship set mmsi =?::hstore where uuid=?");
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://192.168.106.146:5432/postgres";
            String user = "postgres";
            String passWord = "postgres";
            try(Connection conn = DriverManager.getConnection(url, user, passWord);
                PreparedStatement pre =conn.prepareStatement(updateSQL.toString())){
                conn.setAutoCommit(false);
                pre.setString(1,mmsiSB.toString());
                pre.setString(2,"6a6e0984-e0d1-4319-b166-67c608d4597f");
                pre.executeUpdate();
                conn.commit();
            }catch (Exception e){
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double getFeaturesVectorSimilarity(long sliceUUID, JsonObject jo)  {
        double outcome = -1;
        String mmsi = jo.get("smmi").getAsString();
        JsonArray ja = new JsonArray();
        JsonObject joTemp = new JsonObject();
//      根据候选船舶的mmsi号，从船舶特征表中获取其影像切片的sliceUUID
        StringBuffer QuerySB = new StringBuffer();
        QuerySB.append("select akeys(shapevector) as vectorUUID,avals(shapevector) as vectorSim from ship_archival_info sai where smmsi = ?");
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://192.168.1.98:5432/postgres";
            String user = "postgres";
            String passWord = "postgres";
            try(Connection conn = DriverManager.getConnection(url, user, passWord);
                PreparedStatement pre =conn.prepareStatement(QuerySB.toString())){
                pre.setInt(1,Integer.parseInt(mmsi));
                ResultSet rs = pre.executeQuery();
                while (rs.next()){
                    joTemp.addProperty("vectorUUID", rs.getInt("vectorUUID"));
                    joTemp.addProperty("vectorSim", rs.getDouble("vectorSim"));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


//      该候选船舶确实存在影像切片
        if(ja.size()>0){
            double featuresVectorSimilarity = 0;

            for(int i =0;i<ja.size();i++)
            {
                JsonObject joTemp2 = ja.get(i).getAsJsonObject();
                int vectorUUID = joTemp2.get("vectorUUID").getAsInt();
                double vectorSim = joTemp2.get("vectorSim").getAsDouble();

                HttpClient httpClient = new HttpClient();
                //设置Http连接超时为10秒
                httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(10*000);

                //生成GetMethod对象并设置参数
                StringBuilder sb = new StringBuilder();

                sb.append("http://192.168.106.146:8888/compare?");
                sb.append("baseVectorUUID=");
                sb.append(sliceUUID).append("&");
                sb.append("candidateVectorUUID=");
                sb.append(vectorUUID);

                GetMethod getMethod = new GetMethod(sb.toString());
                //设置get请求超时为10秒
                getMethod.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, 10*000);
                //设置请求重试处理，用的是默认的重试处理：请求三次
                getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());

                String response = null;

                //             执行HTTP GET 请求
                try {
                    int statusCode = httpClient.executeMethod(getMethod);
                    if (statusCode != HttpStatus.SC_OK){
                        System.err.println("请求出错：" + getMethod.getStatusLine());
                    }
                    //                 处理HTTP响应内容
                    Header[] headers = getMethod.getResponseHeaders();
                    for (Header h: headers){
                        System.out.println(h.getName() + "---------------" + h.getValue());
                    }
                    //读取HTTP响应内容，这里简单打印网页内容,读取为字节数组
                    byte[] responseBody = getMethod.getResponseBody();
                    response = new String(responseBody, "UTF-8");
                    System.out.println("-----------response:" + response);
                    //读取为InputStream，在网页内容数据量大时候推荐使用
                    //InputStream response = getMethod.getResponseBodyAsStream();
                    JsonObject joHttp = JsonParser.parseString(response).getAsJsonObject();
                    double sim = joHttp.get("similarity").getAsDouble();
                    featuresVectorSimilarity = featuresVectorSimilarity+sim*vectorSim;
                } catch (HttpException e) {
                    //发生致命的异常，可能是协议不对或者返回的内容有问题
                    System.out.println("请检查输入的URL!");
                    e.printStackTrace();
                } catch (IOException e){
                    //发生网络异常
                    System.out.println("发生网络异常!");
                }finally {
                    //                 释放连接
                    getMethod.releaseConnection();
                }
            }
            outcome =featuresVectorSimilarity/ja.size();
        }

        return outcome;
    }
}
