package org.lmars.geodata.searchX.fun.ais;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;

// 修改档案信息
public class UpdateArchiveInfo implements ISearchXFunction {
    @Override
    public String getName() {
        return "UpdateArchiveInfo";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        long smmsi = Long.parseLong(multiMap.get("smmsi"));
        long ismanager = Long.parseLong(multiMap.get("ismanager"));
        String username = multiMap.get("username");
        String name = multiMap.get("name");
        String type = multiMap.get("type");
        String flag = multiMap.get("flag");
        float length = Float.parseFloat(multiMap.get("length"));
        float breadth = Float.parseFloat(multiMap.get("breadth"));
        float grossTG = Float.parseFloat(multiMap.get("grossTonnage"));
        String homePort = multiMap.get("homePort");
        String buildYear = multiMap.get("buildYear");
        float dwt = Float.parseFloat(multiMap.get("dwt"));
        StringBuffer buffer = new StringBuffer();
        buffer.append("update military_archive_info set sName =?, sType=?, sLength=?, sBreadth=?, sGrossTonnage=?,sHomePort=?,sBuildYear=?,sDWT=?, sFlag=? where smmsi = ?");
        if (1!=ismanager){
            buffer.append(" and username =?");
        }else{
            buffer.append(" and ispublic = '1'");
        }
        SQLHelper.executeUpdate(queryXEngine.getCacheDB(), buffer.toString(), (pstat) -> {
            pstat.setString(1, name);
            pstat.setString(2, type);
            pstat.setFloat(3, length);
            pstat.setFloat(4, breadth);
            pstat.setFloat(5, grossTG);
            pstat.setString(6, homePort);
            pstat.setString(7, buildYear);
            pstat.setFloat(8, dwt);
            pstat.setString(9, flag);
            pstat.setLong(10, smmsi);
            if (1!=ismanager) {
                pstat.setString(11, username);
            }
        });
        StartAis.updateMajorMssi();
        return "{status:success}";
    }
}
