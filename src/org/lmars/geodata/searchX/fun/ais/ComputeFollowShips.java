package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.ais.api.FollowInfo;
import org.lmars.ais.api.ShipAnalysis;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.searchX.ISearchXFunction;

import java.util.List;
//伴随计算服务接口
public class ComputeFollowShips implements ISearchXFunction {

    @Override
    public String getName() {

        return "computeFollowShips";
    }

    @Override
    public Object handle(QueryXEngine engine, QueryX query, MultiMap params, Buffer postinfo) throws Exception {
        ShipAnalysis shipAnalysis = StartAis.getShipAnalysis();
        Long mssi = Long.parseLong(params.get("mssi"));
        Long mssiB ="null".equals(params.get("mssiB"))?null: Long.parseLong(params.get("mssiB"));
        Integer beginTime = Integer.parseInt(params.get("beginTime"));
        Integer endTime = Integer.parseInt(params.get("endTime"));
        Integer segTime = Integer.parseInt(params.get("segTime"));
        Integer segContinueTime = Integer.parseInt(params.get("segContinueTime"));
        Integer maxShips = Integer.parseInt(params.get("maxShips"));
        Integer sog_t = Integer.parseInt(params.get("sog_t"));
        Integer time_interval = Integer.parseInt(params.get("time_interval"));
        List<FollowInfo> followInfos = shipAnalysis.computeFollowShips(mssi, mssiB,
                beginTime, endTime, segTime, segContinueTime, maxShips, sog_t, time_interval);
        Gson gson = new Gson();
        return gson.toJson(followInfos);
    }
}
