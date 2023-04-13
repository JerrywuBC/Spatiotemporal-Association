package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.ais.api.ShipAnalysis;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.searchX.ISearchXFunction;
import java.util.List;
//输入提示服务
public class FindSimlarMSSI implements ISearchXFunction {

    @Override
    public String getName() {
        return "findSimlarMSSI";
    }

    @Override
    public Object handle(QueryXEngine engine, QueryX query, MultiMap params, Buffer postinfo) throws Exception {
        ShipAnalysis shipAnalysis = StartAis.getShipAnalysis();
        Long mssi = Long.parseLong(params.get("mssi"));
        Integer limitN = Integer.parseInt(params.get("limitN"));
        List<String> simlarMSSI = shipAnalysis.findSimlarMSSI(mssi, limitN);
        Gson gson = new Gson();
        return gson.toJson(simlarMSSI);
    }

}
