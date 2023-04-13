package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.ais.api.STTrack;
import org.lmars.ais.api.ShipAnalysis;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.searchX.ISearchXFunction;
import java.util.List;
//历史轨迹查询
public class searchHistoryTrack implements ISearchXFunction {

    @Override
    public String getName() {

        return "searchHistoryTrack";
    }

    @Override
    public Object handle(QueryXEngine engine, QueryX query, MultiMap params, Buffer postinfo) throws Exception {
        ShipAnalysis shipAnalysis = StartAis.getShipAnalysis();
        long mssi = Long.parseLong(params.get("mssi"));
        int beginTime = Integer.parseInt(params.get("beginTime"));
        int endTime = Integer.parseInt(params.get("endTime"));
        int segTimeT = Integer.parseInt(params.get("segTimeT"));
        int maxPointN = Integer.parseInt(params.get("maxPointN"));
        List<STTrack> stTracks = shipAnalysis.searchHistoryTrack(mssi,
                beginTime, endTime, segTimeT, maxPointN);
        Gson gson = new Gson();
        return gson.toJson(stTracks);
    }
}
