package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.ais.api.ShipAnalysis;
import org.lmars.ais.api.ShipRegionInfo;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.searchX.ISearchXFunction;
import java.util.List;
//港口船舶信息
public class computeShipsInShipPort implements ISearchXFunction {

    @Override
    public String getName() {

        return "computeShipsInShipPort";
    }

    @Override
    public Object handle(QueryXEngine engine, QueryX query, MultiMap params, Buffer postinfo) throws Exception {
        ShipAnalysis shipAnalysis = StartAis.getShipAnalysis();
        String portKey=params.get("portKey");
        Integer beginTime = Integer.parseInt(params.get("beginTime"));
        Integer endTime = Integer.parseInt(params.get("endTime"));
        Double distanceT=Double.parseDouble(params.get("distanceT"));
        Integer segTimeT=Integer.parseInt(params.get("segTimeT"));
        List<ShipRegionInfo> shipRegionInfos = shipAnalysis.computeShipsInShipPort
                (portKey, beginTime, endTime, distanceT, segTimeT);
        Gson gson = new Gson();
        return gson.toJson(shipRegionInfos);
    }
}
