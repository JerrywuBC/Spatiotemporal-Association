package org.lmars.geodata.searchX.fun.ais;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;

// 删除档案信息
public class RemoveArchiveInfo implements ISearchXFunction {
    @Override
    public String getName() {
        return "RemoveArchiveInfo";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        Long mmsi = Long.parseLong(multiMap.get("mmsi"));
        String sql = "delete from military_archive_info where sMMSI = ?";
        SQLHelper.executeUpdate(queryXEngine.getCacheDB(), sql, (pstat) -> {
            pstat.setLong(1, mmsi);
        });
        StartAis.updateMajorMssi();
        return "{status:success}";
    }
}
