package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.ais.api.ShipAnalysis;
import org.lmars.ais.api.ShipRegionInfo;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.ais.bean.ShipInfo;
import org.lmars.geodata.ais.bean.ShipRegionInfos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
//计算重点船舶集合在某个区域内的活动情况
public class ComputeShipInRegions implements ISearchXFunction {

    @Override
    public String getName() {

        return "computeShipInRegions";
    }

    @Override
    public Object handle(QueryXEngine engine, QueryX query, MultiMap params, Buffer postinfo) throws Exception {
        ShipAnalysis shipAnalysis = StartAis.getShipAnalysis();
        ConcurrentHashMap<String, String> getareainfo = StartAis.getareainfo();
        String username = params.get("username");
        String regionname = params.get("regionname");
        String regionWKT = getareainfo.get(regionname);
        Integer beginTime = Integer.parseInt(params.get("beginTime"));
        Integer endTime = Integer.parseInt(params.get("endTime"));
        Integer segTimeT = Integer.parseInt(params.get("segTimeT"));
        Integer sogT = "null".equals(params.get("sogT")) ? null : Integer.parseInt(params.get("sogT"));
        List<ShipRegionInfo> shipRegionInfos = shipAnalysis.computeShipInRegions(regionWKT, beginTime, endTime, segTimeT, sogT);

        Map<String,List<ShipRegionInfos>> result=new HashMap<>();
        List<ShipRegionInfos> shrislist = new ArrayList<>();
        List<ShipRegionInfos> careshrislist = new ArrayList<>();
        ConcurrentHashMap<Long, ShipInfo> getshipinfo = StartAis.shipinfo;
        ArrayList<Long> smmsis= new ArrayList<>();
        if (username!=null||"".equals(username)){
            String sql="select distinct smmsi from military_archive_info mai where username =?";
            SQLHelper.executeSearch(engine.getCacheDB(), sql, (pstat) -> {
                pstat.setString(1, username);
            }, (rs) -> {
                while (rs.next()) {
                    smmsis.add(rs.getLong(1));
                }
            });
        }
        for (ShipRegionInfo sri : shipRegionInfos) {
            ShipInfo shipInfo = getshipinfo.get(sri.mssi);
            ShipRegionInfos sris = new ShipRegionInfos();
            sris.mssi = sri.mssi;
            sris.timeInfos = sri.timeInfos;
            sris.totalEnterTime = sri.totalEnterTime;
            if (null != shipInfo) {
                sris.cn_country = shipInfo.cn_country;
                sris.ssupertype = shipInfo.ssupertype;
            } else {
                sris.cn_country = "未知";
                sris.ssupertype = "未知";
            }
            if (smmsis.size()>0&&smmsis.contains(sri.mssi)){
                careshrislist.add(sris);
            }
            shrislist.add(sris);
        }
        result.put("all",shrislist);
        result.put("care",careshrislist);
        Gson gson = new Gson();
        return gson.toJson(result);
    }
}
