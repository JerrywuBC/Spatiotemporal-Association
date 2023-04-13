package org.lmars.geodata.searchX.fun.ais;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.core.utils.AreaUtil;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;

// 修改区域信息
public class UpdateAreaInfo implements ISearchXFunction {
    @Override
    public String getName() {
        return "UpdateAreaInfo";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        String name = multiMap.get("name");
        String info = multiMap.get("info");
        if (name == null || info == null) {
            return null;
        }
        String geometry = AreaUtil.geometryString(info);
        String portlist = AreaUtil.portsString(geometry);
        String insql = "update area_info set info=?, geometry=ST_GeometryFromText(\'" + geometry + "\'), portlist=? where areaname = \'" + name + "\'";
        SQLHelper.executeUpdate(queryXEngine.getCacheDB(), insql, (pstat) -> {
            pstat.setString(1, info);
            pstat.setString(2, portlist);
        });
        StartAis.initareainfo();
        return "{status:success}";
    }
}
