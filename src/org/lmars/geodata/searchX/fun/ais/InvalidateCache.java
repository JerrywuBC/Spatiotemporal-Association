package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.ais.api.ShipAnalysis;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.searchX.ISearchXFunction;

//清理缓存
public class InvalidateCache implements ISearchXFunction {

    @Override
    public String getName() {

        return "invalidateCache";
    }

    @Override
    public Object handle(QueryXEngine engine, QueryX query, MultiMap params, Buffer postinfo) throws Exception {
        ShipAnalysis shipAnalysis = StartAis.getShipAnalysis();
        shipAnalysis.invalidateCache();
        StartAis.initShipInfo();
        Gson gson = new Gson();
        return gson.toJson("success");
    }

}
