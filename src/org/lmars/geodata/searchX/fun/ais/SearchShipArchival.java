package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.geodata.aisproject.main.StartAis;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// 根据mmsi搜索船舶国籍及类型等信息
public class SearchShipArchival implements ISearchXFunction {
    @Override
    public String getName() {
        return "searchShipArchival";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        String mmsis = multiMap.get("mmsis");
        String replace = mmsis.replace("|", ",");
        String sql="select smmsi,cn_country,ssupertype from ship_archival_info sai where smmsi in ("+replace+")";
        ArrayList<Map<String,Object>> result = new ArrayList<>();
        SQLHelper.executeSearch(queryXEngine.getCacheDB(), sql, (rs) -> {
            while(rs.next()) {
                Map<String,Object> m = new HashMap<>();
                m.put("smmsi",rs.getInt(1));
                m.put("cn_country",rs.getString(2));
                m.put("ssupertype",StartAis.TYPE_MAPS.get(rs.getString(3)));
                result.add(m);
            }
        });
        Gson gson = new Gson();
        return gson.toJson(result);
    }
}
