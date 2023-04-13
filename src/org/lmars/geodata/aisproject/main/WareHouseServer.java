package org.lmars.geodata.aisproject.main;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.lmars.geodata.ImageMetainfo.Utils.ConfigureUtils;
import org.lmars.geodata.ImageMetainfo.Utils.MappingUtils;
import org.lmars.geodata.ImageMetainfo.Utils.TargetFileUtils;
import org.lmars.geodata.core.utils.*;
import org.lmars.geodata.searchX.QueryX;
import org.lmars.geodata.user.behaviors.UserVisitRecorder;
import org.lmars.geodata.warehouse.DBDescParser;
import org.lmars.geodata.warehouse.DBTableDesc;
import org.lmars.geodata.warehouse.WareHouseEngine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class WareHouseServer extends VWebService {
	
	private WareHouseEngine engine = new WareHouseEngine();
	

	
	@http(url = "/warehouse/table/drop")
	public void dropTable(RoutingContext context) {
		
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			String tableName = req.getParam("name");
			this.engine.dropTable(tableName);
			this.onSuccess(res);
		} catch (Exception ignore) {
			this.onFailed(res, ignore.getMessage());
		}
		
	}
	
	@http(url = "/warehouse/table/list")
	public void getTables(RoutingContext context) {
		
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			List<String> names = this.engine.getAllTables();
			JsonArray json = new JsonArray();
			for (String name : names) {
				json.add(name);
			}
			this.onSuccess(res, json);
		} catch (Exception ignore) {
			this.onFailed(res, ignore.getMessage());
		}
		
	}
	
	@http(url = "/warehouse/table/json")
	public void getTableJson(RoutingContext context) {
		
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			String tableName = req.getParam("name");
			DBTableDesc table_desc = this.engine.getTable(tableName);
			JsonObject json = table_desc.toJson();
			this.onSuccess(res, json);
		} catch (Exception ignore) {
			this.onFailed(res, ignore.getMessage());
		}
		
	}
	
	@http(url = "/warehouse/table/desc")
	public void getTableDesc(RoutingContext context) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			String tableName = req.getParam("name");
			DBTableDesc table_desc = this.engine.getTable(tableName);
			String desc = DBDescParser.saveDescFile(table_desc);
			res.setStatusCode(200);
			res.putHeader("content-type", "text/plain");
			res.putHeader("charset", "UTF-8");
			res.end(desc);
		} catch (Exception ignore) {
			this.onFailed(res, ignore.getMessage());
		}
	}
	
	@http(url = "/warehouse/search/mkv", method = "post")
	public void searchByMultiKV(RoutingContext context, Buffer buffer) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			JsonArray paramArray = new JsonArray(buffer.toString());
			List<Map<String, String>> mkvs = new ArrayList<Map<String, String>>();
			Iterator<Object> iterator = paramArray.iterator();
			while (iterator.hasNext()) {
				JsonObject paramObj = (JsonObject) iterator.next();
				HashMap<String, String> kvs = new HashMap<>();
				paramObj.forEach(e -> {
					kvs.put(e.getKey(), String.valueOf(e.getValue()));
				});
				mkvs.add(kvs);
			}
			String jsonstr = this.engine.searchByKV(mkvs);
			this.onSuccess(res, jsonstr);
		} catch (Exception ignore) {
			ignore.printStackTrace();
			this.onFailed(res, ignore.getMessage());
		}
	}
	
	@http(url = "/warehouse/search/kv")
	public void searchByKV(RoutingContext context) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			MultiMap params = req.params();
			Map<String, String> kvs = new HashMap<String, String>();
			params.forEach(e -> {
				kvs.put(e.getKey(), e.getValue());
			});
			String jsonstr = this.engine.searchByKV(kvs);
			this.onSuccess(res, jsonstr);
			
		} catch (Exception ignore) {
			ignore.printStackTrace();
			this.onFailed(res, ignore.getMessage());
		}
	}
	
	@http(url = "/warehouse/ceshi/kv")
	public static void searchByKVceshi(RoutingContext context) {
		System.out.println("yes");
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		System.out.println(req.uri());
	}
	
	
//	@http(url = "/warehouse/queryx/search")
//	public void searchQueryXSearch(RoutingContext context) {
//		HttpServerRequest req = context.request();
//		HttpServerResponse res = context.response();
//
//		try {
//			MultiMap params = req.params();
//			String queryName = params.get("query");
//			String timeoutStr = params.get("timeout");
//			int timeout = -1;
//			if (timeoutStr != null) {
//				timeout = Integer.parseInt(timeoutStr);
//			}
//			String jsonstr = this.engine.getQueryXEngine().search(queryName, timeout, params);
//			this.onSuccess(res, jsonstr);
//		} catch (Exception ignore) {
//			ignore.printStackTrace();
//			this.onFailed(res, ignore.getMessage());
//		}
//	}
	
	@http(url = "/warehouse/queryx/cache")
	public void searchQueryXCacheInstance(RoutingContext context) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			MultiMap params = req.params();
			String queryName = params.get("query");
			String instanceName = params.get("instance");
			String cacheuuid = params.get("cache");
			String jsonstr = this.engine.getQueryXEngine().searchCache(queryName, instanceName, cacheuuid);
			this.onSuccess(res, jsonstr);
		} catch (Exception ignore) {
			ignore.printStackTrace();
			this.onFailed(res, ignore.getMessage());
		}
	}

/*	@http(url = "/warehouse/export/create", method = "post")
	public void createExportTask(RoutingContext context, Buffer buffer) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();

		String userId = req.getParam("userId");
		List<String> tableNameList = new ArrayList<String>();
		try {
			JsonArray paramArray = new JsonArray(buffer.toString());
			List<Map<String, String>> mkvs = new ArrayList<Map<String, String>>();
			Iterator<Object> iterator = paramArray.iterator();
			while (iterator.hasNext()) {
				JsonObject paramObj = (JsonObject) iterator.next();
				HashMap<String, String> kvs = new HashMap<>();
				paramObj.forEach(e -> {
					if (e.getKey() == "table") {
						tableNameList.add(String.valueOf(e.getValue()));
					}
					kvs.put(e.getKey(), String.valueOf(e.getValue()));
				});
				mkvs.add(kvs);
			}

			String uuid = engine.createExportTask(mkvs);
			for (int i = 0; i < tableNameList.size(); i++) {
				engine.insertDownLog(tableNameList.get(i), userId);
			}
			JsonObject obj = new JsonObject();
			obj.put("task_id", uuid);
			onSuccess(res, obj);

		} catch (Exception ignore) {
			ignore.printStackTrace();
			onFailed(res, ignore.getMessage());
		}
	}*/
	
	@http(url = "/warehouse/export/create", method = "post")
	public void createExportTask(RoutingContext context, Buffer buffer) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		String userId = req.getParam("userId");
		List<String> tableNameList = new ArrayList<>();
		try {
			JsonObject jsonObject = new JsonObject(buffer.toString());
			JsonArray paramArray = jsonObject.getJsonArray("tableinfo");
			List<Map<String, String>> mkvs = new ArrayList<>();
			for (Object param : paramArray) {
				JsonObject paramObj = (JsonObject) param;
				HashMap<String, String> kvs = new HashMap<>();
				paramObj.forEach(e -> {
					if ("table".equals(e.getKey())) {
						tableNameList.add(String.valueOf(e.getValue()));
					}
					kvs.put(e.getKey(), String.valueOf(e.getValue()));
				});
				mkvs.add(kvs);
			}
			
			String uuid = this.engine.createExportTask(mkvs);
			String taskname = jsonObject.getString("taskname");
			String username = jsonObject.getString("username");
			for (String s : tableNameList) {
				this.engine.insertDownLog(s, userId, taskname, uuid, username);
			}
			JsonObject obj = new JsonObject();
			obj.put("task_id", uuid);
			this.onSuccess(res, obj);
			
		} catch (Exception e) {
			e.printStackTrace();
			this.onFailed(res, e.getMessage());
		}
	}
	
	@http(url = "/warehouse/export/status")
	public void getExportStatus(RoutingContext context) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			String taskID = req.getParam("uuid");
			this.onSuccess(res, this.engine.getExportTaskStatus(taskID));
		} catch (Exception ignore) {
			ignore.printStackTrace();
			this.onFailed(res, ignore.getMessage());
		}
	}
	
	@http(url = "/warehouse/driver/status")
	public void getDriverStatus(RoutingContext context) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			this.onSuccess(res, this.engine.getDriverStatus());
		} catch (Exception ignore) {
			ignore.printStackTrace();
			this.onFailed(res, ignore.getMessage());
		}
	}
	
	@http(url = "/warehouse/edit/kv", method = "post")
	public void editTableDate(RoutingContext context, Buffer buffer) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			JsonObject result = new JsonObject();
			String json = buffer.toString();
			boolean resultFlag = this.engine.upsertKVs(json);
			if (resultFlag) {
				result.put("status", "success");
				this.onSuccess(res, result);
			} else {
				this.onFailed(res, "failed");
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.onFailed(res, e.getMessage());
		}
	}
	
	@http(url = "/warehouse/add/kv", method = "post")
	public void addTableDate(RoutingContext context, Buffer buffer) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		
		try {
			JsonObject result = new JsonObject();
			String json = buffer.toString();
			boolean resultFlag = this.engine.insertKVs(json);
			if (resultFlag) {
				result.put("status", "success");
				this.onSuccess(res, result);
			} else {
				this.onFailed(res, "failed");
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.onFailed(res, e.getMessage());
		}
	}
	
	private String pgConnUrl;
	private String pgUserName;
	private String pgPassword;
	
	/**
	 * 数据下载服务
	 *
	 * @param context
	 * @param buffer
	 */
	@http(url = "/warehouse/excel/export", method = "post")
	public void excelExport(RoutingContext context, Buffer buffer) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		try {
			JsonObject result = new JsonObject();
			JsonObject postData = new JsonObject(buffer.toString());
			DataExportToExcel dataExportToExcel = new DataExportToExcel();
			String exportResult = dataExportToExcel.getDataFromTable(buffer.toString(), this.pgConnUrl, this.pgUserName, this.pgPassword);
			if (exportResult == null) {
				result.put("status", "failed");
			} else {
				result.put("status", "success");
				result.put("exportUrl", exportResult);
			}
			this.onSuccess(res, result);
		} catch (Exception e) {
			e.printStackTrace();
			this.onFailed(res, e.getMessage());
		}
	}
	
	@http(url = "/warehouse/d3tile/dealtag", method = "post")
	public void d3tileAddTag(RoutingContext context, Buffer buffer) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();
		try {
			JsonObject postData = new JsonObject(buffer.toString());
			D3tileTagDealUtils d3tileTagDealUtils = new D3tileTagDealUtils();
			JsonObject entries = d3tileTagDealUtils.addD3tileTag(postData, this.engine.getDataSource());
			this.onSuccess(res, entries);
		} catch (Exception e) {
			e.printStackTrace();
			this.onFailed(res, e.getMessage());
		}
	}

	@http(url = "/warehouse/upload/csv", method = "post")
	public void CsvUpload(RoutingContext context, Buffer buffer) {
		HttpServerRequest req = context.request();
		HttpServerResponse res = context.response();

		try{
			String username = req.params().get("username");
			BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer.getBytes())));
			String boundary = br.readLine();
			List<Integer> mssi = new ArrayList<Integer>();
			boolean flag = false;
			String line = br.readLine();
			while(line != null){
				if(line.equals("")){
					flag = true;
				}else if(line.equals(boundary + "--")){
					break;
				}else{
					if(flag){
						Integer ms = Integer.valueOf(line);
						mssi.add(ms);
					}
				}
				line = br.readLine();
			}
			String mssis = mssi.toString();
			String finalMssis =mssis.substring(1,mssis.length()-1);
			String querysql="select distinct smmsi from military_archive_info mai where smmsi in ("+finalMssis+")";
			List<Integer> existsmmis= new ArrayList<Integer>();
			SQLHelper.executeSearch(this.engine.getDataSource(), querysql,(resultSet -> {
				while (resultSet.next()) {
					existsmmis.add(resultSet.getInt(1));
				}
			}));
			if (existsmmis.size()>0){
				String substring = existsmmis.toString().substring(1, existsmmis.toString().length() - 1);
				mssi.removeAll(existsmmis);
				String insertsql = new StringBuilder().append("insert into military_archive_info(sbuildyear, stype, slength, spicturepath, smmsi, shomeport, sdwt, sbreadth, sgrosstonnage, sname, sflag, ispublic, username) ")
						.append("select sbuildyear, stype, slength, spicturepath, smmsi, shomeport, sdwt, sbreadth, sgrosstonnage, sname, sflag, '0', ? from military_archive_info where smmsi in ("+ substring +")").toString();
				SQLHelper.executeUpdate(this.engine.getDataSource(), insertsql, (pstat) -> {
					pstat.setString(1, username);
				});
			}

			String insertnotexistsql="insert into military_archive_info(smmsi,ispublic, username) values (?,?,?)";
			SQLHelper.executeBatchUpdate(this.engine.getDataSource(), insertnotexistsql, (pstat) -> {
				for (Integer mmsi :mssi) {
					pstat.setInt(1,mmsi);
					pstat.setString(2, "0");
					pstat.setString(3, username);
					pstat.addBatch();
				}
			});
			StartAis.updateMajorMssi();
			JsonObject result = new JsonObject();
			result.put("status", "success");
			this.onSuccess(res, result);
		}catch(Exception e){
			e.printStackTrace();
			this.onFailed(res, e.getMessage());
		}
	}

	public void startServer(String configureFile) throws Exception {
		
		// 加载配置文件
		Map<String, String> conf = ConfigureFileParser.parseConfigurationWithDirectory(configureFile);
		
		boolean isuse = Boolean.parseBoolean(conf.get("imagemetainfo.isuse"));
		if (isuse) {
			ConfigureUtils.pgConn = conf.get("imagemetainfo.pgconn");
			ConfigureUtils.conn = conf.get("imagemetainfo.conn");
			ConfigureUtils.initConfigure();
			ConfigureUtils.unzipPath = conf.get("imagemetainfo.unzippath");
			ConfigureUtils.tifType = conf.get("imagemetainfo.tiftype");
			ConfigureUtils.pngType = conf.get("imagemetainfo.pngtype");
			
			TargetFileUtils.initTifExtensionSet(conf.get("imagemetainfo.tifextension"));
			TargetFileUtils.initZipExtensionSet(conf.get("imagemetainfo.zipextension"));
			
			MappingUtils.initMetaDataHelper(conf.get("imagemetainfo.rsdriver"));
		}
		
		this.pgConnUrl = "jdbc:postgresql://" + conf.get("cachedb.ip") + ":" + conf.get("cachedb.port") + "/" + conf.get("cachedb.name");
		this.pgUserName = conf.get("cachedb.user");
		this.pgPassword = conf.get("cachedb.password");
		
		this.engine.init(conf);
		
		// queryX引擎url挂接
		List<QueryX> querys = this.engine.getQueryXEngine().getAllQuerys();
		for (QueryX query : querys) {
			final String queryName = query.name;
			this.hookDynamicHttpObject(query.url, new DynamicHttpGet() {
				public void handle(RoutingContext context) {
					HttpServerRequest req = context.request();
					HttpServerResponse res = context.response();
					try {
						MultiMap params = req.params();
						String jsonstr = (String)WareHouseServer.this.engine.searchQueryX(queryName, params, context);
						WareHouseServer.this.onSuccess(res, jsonstr);
					} catch (Exception ignore) {
						ignore.printStackTrace();
						System.out.println(queryName + "   err");
						WareHouseServer.this.onFailed(res, ignore.getMessage());
					}
				}
			});
		}
		
		Integer listen_port = OSUtil.parseInt(conf.get("WareHouseServer.port"), 10201);
		Integer work_threads = OSUtil.parseInt(conf.get("WareHouseServer.threads"), 5);
		Long timeout = Long.parseLong(conf.get("WareHouseServer.timeout"));
//		UserVisitRecorder recorder = new UserVisitRecorder(conf);
//		this.addHttpBeforeHandle(recorder);
//		this.addHttpAfterHandle(recorder);
		
		this.start(listen_port, timeout, work_threads, this, null);
	}
}
