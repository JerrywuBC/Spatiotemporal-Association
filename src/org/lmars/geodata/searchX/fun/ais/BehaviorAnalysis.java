package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.ais.api.BehaviorType;
import org.lmars.ais.api.ShipAnalysis;
import org.lmars.ais.api.ShipBehavior;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.searchX.ISearchXFunction;
//船舶行为分析
public class BehaviorAnalysis implements ISearchXFunction {

    @Override
    public String getName() {

        return "behaviorAnalysis";
    }

    @Override
    public Object handle(QueryXEngine engine, QueryX query, MultiMap params, Buffer postinfo) throws Exception {
        ShipAnalysis shipAnalysis = StartAis.getShipAnalysis();
        Long userid= Long.parseLong(params.get("userid"));
        Integer beginTime= Integer.parseInt(params.get("beginTime"));
        Integer endTime = Integer.parseInt(params.get("endTime"));
        Integer connectTimeSecond = Integer.parseInt(params.get("connectTimeSecond"));
        Double shipPortDistanceT=Double.parseDouble(params.get("shipPortDistanceT"));
        BehaviorType type = BehaviorType.All;
        ShipBehavior shipBehavior = shipAnalysis.behaviorAnalysis(userid,
                beginTime, endTime, connectTimeSecond, shipPortDistanceT, type);
        Gson gson = new Gson();
        return gson.toJson(shipBehavior);
    }

}
