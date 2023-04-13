package org.lmars.geodata.searchX.fun.ais;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;

// 删除区域信息
public class RemoveAreaInfo implements ISearchXFunction {
    @Override
    public String getName() {
        return "RemoveAreaInfo";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        String name = multiMap.get("name");
        String sql = "delete from area_info where areaname = \'" + name + "\'";
        SQLHelper.executeUpdate(queryXEngine.getCacheDB(), sql, (pstat) -> {
        });
        StartAis.initareainfo();
        return "{status:success}";
    }
}
