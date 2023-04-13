package org.lmars.geodata.searchX.fun.ais;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;
// 档案查询
public class ShipMilitary implements ISearchXFunction {
    @Override
    public String getName() {
        return "ShipMilitary";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        String sql = "select smmsi from military_archive_info";
        JsonArray array = new JsonArray();
        SQLHelper.executeSearch(queryXEngine.getCacheDB(), sql, pst -> {
        }, rs -> {
            while (rs.next()) {
                array.add(rs.getLong(1));
            }
        });
        return array.toString();
    }
}
