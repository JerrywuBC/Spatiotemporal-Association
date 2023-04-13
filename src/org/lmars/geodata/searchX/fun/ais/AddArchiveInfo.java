package org.lmars.geodata.searchX.fun.ais;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;

// 新增档案信息
public class AddArchiveInfo implements ISearchXFunction {
    @Override
    public String getName() {
        return "AddArchiveInfo";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        long mmsi = Long.parseLong(multiMap.get("mmsi"));
        String name = multiMap.get("name");
        String type = multiMap.get("type");
        String flag = multiMap.get("flag");
        float length = Float.parseFloat(multiMap.get("length"));
        float breadth = Float.parseFloat(multiMap.get("breadth"));
        float grossTG = Float.parseFloat(multiMap.get("grossTonnage"));
        String homePort = multiMap.get("homePort");
        String buildYear = multiMap.get("buildYear");
        float dwt = Float.parseFloat(multiMap.get("dwt"));
        String isPulic = multiMap.get("ispublic");
        String userName = multiMap.get("username");
        StringBuffer buffer = new StringBuffer();
        buffer.append("insert into military_archive_info(sMMSI,sName,sType,sLength,sBreadth,sGrossTonnage,sHomePort,sBuildYear,sDWT,sFlag,ispublic,username) values(?,?,?,?,?,?,?,?,?,?,?,?)");
        SQLHelper.executeUpdate(queryXEngine.getCacheDB(), buffer.toString(), (pstat) -> {
            pstat.setLong(1, mmsi);
            pstat.setString(2, name);
            pstat.setString(3, type);
            pstat.setFloat(4, length);
            pstat.setFloat(5, breadth);
            pstat.setFloat(6, grossTG);
            pstat.setString(7, homePort);
            pstat.setString(8, buildYear);
            pstat.setFloat(9, dwt);
            pstat.setString(10, flag);
            pstat.setString(11,isPulic);
            pstat.setString(12,userName);
        });
        StartAis.updateMajorMssi();
        return "{status:success}";
    }
}
