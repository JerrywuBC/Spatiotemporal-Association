package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;
// 查询区域信息
public class SearchAreaInfo implements ISearchXFunction {
    @Override
    public String getName() {
        return "SearchAreaInfo";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        System.out.println("start searchAreafun");
        String name = multiMap.get("name");
        StringBuffer sql = new StringBuffer();
        sql.append("select areaname, info from area_info ");
        if (!name.equalsIgnoreCase("all")) {
            sql.append(" where areaname = \'").append(name).append("\'");
        }
        JsonArray array = new JsonArray();
        SQLHelper.executeSearch(queryXEngine.getCacheDB(), sql.toString(), (pstat) -> {
        }, (rs) -> {
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", rs.getString(1));
                obj.addProperty("info", rs.getString(2));
                array.add(obj);
            }
        });
        return array.toString();
    }
}
