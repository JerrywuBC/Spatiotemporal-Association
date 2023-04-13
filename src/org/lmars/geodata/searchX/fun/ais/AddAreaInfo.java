package org.lmars.geodata.searchX.fun.ais;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.core.utils.AreaUtil;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;

// 新增区域信息
public class AddAreaInfo implements ISearchXFunction {
    @Override
    public String getName() {
        return "AddAreaInfo";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        String name = multiMap.get("name");
        String info = multiMap.get("info");
        if (name == null || info == null) {
            return null;
        }
        // 多边形
        String geometry = AreaUtil.geometryString(info);
        // 多边形包含港口
        String portsStr = AreaUtil.portsString(geometry);
        int id = (int) (System.currentTimeMillis() / 1000);
        String insql = "insert into area_info(areaid, areaname, info, geometry, portlist) values(?,?,?,ST_GeometryFromText(\'" + geometry + "\'),?) on conflict(areaname) do update set areaid=excluded.areaid";
        SQLHelper.executeUpdate(queryXEngine.getCacheDB(), insql, (pstat) -> {
            pstat.setInt(1, id);
            pstat.setString(2, name);
            pstat.setString(3, info);
            pstat.setString(4, portsStr);
        });
        StartAis.initareainfo();
        return "{status:success}";
    }
}
