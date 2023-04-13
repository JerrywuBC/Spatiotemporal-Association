package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.ais.api.ShipAnalysis;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.core.utils.AreaUtil;
import org.lmars.geodata.searchX.ISearchXFunction;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.ais.bean.ShipInfo;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
//所有船舶时空查询
public class ComputeAllShipInRegions implements ISearchXFunction {

    @Override
    public String getName() {

        return "computeAllShipInRegions";
    }

    @Override
    public Object handle(QueryXEngine engine, QueryX query, MultiMap params, Buffer postinfo) throws Exception {
        ShipAnalysis shipAnalysis = StartAis.getShipAnalysis();
        String geometry = "null".equals(params.get("regioninfo")) ? null : AreaUtil.geometryString(params.get("regioninfo"));
        Integer beginTime = Integer.parseInt(params.get("beginTime"));
        Integer endTime = Integer.parseInt(params.get("endTime"));
        Long mssiPrefix = "null".equals(params.get("mssiPrefix")) ? null : Long.parseLong(params.get("mssiPrefix"));
        Integer maxN = Integer.parseInt(params.get("maxN"));
        Boolean onlyValidateMSSI = "null".equals(params.get("onlyValidateMSSI")) ? true : Boolean.parseBoolean(params.get("onlyValidateMSSI"));
        List<Long> longs = shipAnalysis.computeAllShipInRegions
                (geometry, beginTime, endTime, mssiPrefix, maxN, onlyValidateMSSI);
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
            ja.add(jo);
        }
        Gson gson = new Gson();
        return gson.toJson(ja);
    }
}
