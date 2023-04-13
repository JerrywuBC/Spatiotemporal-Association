package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.ais.api.*;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.searchX.ISearchXFunction;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
//计算异常船只的活跃度
public class FindAbnormalActiveShips implements ISearchXFunction {

    @Override
    public String getName() {

        return "findAbnormalActiveShips";
    }

    @Override
    public Object handle(QueryXEngine engine, QueryX query, MultiMap params, Buffer postinfo) throws Exception {
        ShipAnalysis shipAnalysis = StartAis.getShipAnalysis();
        ConcurrentHashMap<String,String> getareainfo = StartAis.getareainfo();
        String regionname = params.get("regionname");
        String regionWKT = getareainfo.get(regionname);
        Integer beginTime = Integer.parseInt(params.get("beginTime"));
        Integer endTime = Integer.parseInt(params.get("endTime"));
        Integer segTimeT = Integer.parseInt(params.get("segTimeT"));
        Integer sogT = Integer.parseInt(params.get("sogT"));
        List<ShipActiveN> abnormalActiveShips = shipAnalysis.findAbnormalActiveShips
                (regionWKT, beginTime, endTime, segTimeT, sogT);
        Gson gson = new Gson();
        return gson.toJson(abnormalActiveShips);
    }
}
