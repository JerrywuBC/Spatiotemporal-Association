package org.lmars.geodata.searchX.fun.ais;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import org.lmars.geodata.core.utils.SQLHelper;
import org.lmars.geodata.searchX.ISearchXFunction;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.searchX.QueryXEngine;
//用户登录
public class Userlogin implements ISearchXFunction {
    @Override
    public String getName() {
        return "Userlogin";
    }

    @Override
    public Object handle(QueryXEngine queryXEngine, QueryX queryX, MultiMap multiMap, Buffer postinfo) throws Exception {
        String username = multiMap.get("username");
        String password = multiMap.get("password");
        String sql = "select username,ismanager from userinfo u where username =? and password =?";
        JsonObject jo = new JsonObject();
        SQLHelper.executeSearch(queryXEngine.getCacheDB(), sql, (pstat) -> {
            pstat.setString(1, username);
            pstat.setString(2, password);
        }, (rs) -> {
            while (rs.next()) {
                jo.addProperty("username",rs.getString(1));
                jo.addProperty("ismanager",rs.getString(2));
            }
        });
        if (jo.size()==0)
            return "{status:failed}";
        Gson gson = new Gson();
        return gson.toJson(jo);
    }
}
