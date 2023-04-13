package org.lmars.geodata.aisproject.main;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import lmars.clickhouse.driver.Driver;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.lmars.ais.api.ShipAnalysis;
import org.lmars.ais.api.ShipPort;
import org.lmars.geodata.ais.bean.ShipInfo;
import org.lmars.geodata.ais.utils.PGDriver;
import org.lmars.geodata.core.utils.ConfigureFileParser;
import org.lmars.geodata.core.utils.SQLHelper;

import java.beans.PropertyVetoException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StartAis {

    private static ComboPooledDataSource cpds;

    private static ShipAnalysis shipAnalysis;

    private static ConcurrentHashMap<String, String> areainfo = null;

    public static ConcurrentHashMap<Long, ShipInfo> shipinfo = new ConcurrentHashMap<>();

    public static ShipAnalysis getShipAnalysis() {
        return shipAnalysis;
    }

    public static ConcurrentHashMap<String, String> getareainfo() {
        return areainfo;
    }


    public static String jpgphoto_server;
    public static String jpegphoto_server;
    public static Map<String, String> TYPE_MAPS = new ConcurrentHashMap<>();
    public static final String NOT_ABLE = "Not Available";
    public static final String Fishing = "Fishing";
    public static final String TugTow = "Tug Tow";
    public static final String Military = "Military";
    public static final String LawEV = "Law-Enforcement Vessel";
    public static final String Sailing = "Pleasure Craft/Sailing";
    public static final String Passenger = "Passenger";
    public static final String Cargo = "Cargo";
    public static final String Tanker = "Tanker";
    public static final String Other = "Other";

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("f", true, "configure file path");
        try {
            TYPE_MAPS.put(NOT_ABLE, "其它");
            TYPE_MAPS.put(Fishing, "捕捞");
            TYPE_MAPS.put(TugTow, "拖轮");
            TYPE_MAPS.put(Military, "军事");
            TYPE_MAPS.put(LawEV, "执法");
            TYPE_MAPS.put(Sailing, "游艇");
            TYPE_MAPS.put(Passenger, "客船");
            TYPE_MAPS.put(Cargo, "货船");
            TYPE_MAPS.put(Tanker, "油轮");
            TYPE_MAPS.put(Other, "其它");

            CommandLineParser parser = new BasicParser();
            CommandLine commandLine = parser.parse(options, args);
            String configureFilePath = commandLine.getOptionValue("f");

            configureFilePath = "/home/root123/lkr/aisdir/aisprojectconf/aislkr.cfg";

            Map<String, String> conf = ConfigureFileParser.parseConfigurationWithDirectory(configureFilePath);
            jpegphoto_server = conf.get("ais.jpegphoto_server");
            jpgphoto_server = conf.get("ais.jpgphoto_server");
            creatpool(conf);
            PGDriver.getInstance().init(conf);
            Driver.init(conf.get("DBDriver.cldriver"));
            initshipAnalysis(conf);
//            initShipInfo();
//            initareainfo();
            WareHouseServer server = new WareHouseServer();
            server.startServer(configureFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initShipInfo() {
        shipinfo.clear();
        String sql = "select smmsi,cn_country,ssupertype from ship_archival_info sai";
        try {
            SQLHelper.executeSearch(cpds, sql, (rs) -> {
                while (rs.next()) {
                    ShipInfo si = new ShipInfo();
                    si.smmi = rs.getLong(1);
                    si.cn_country = rs.getString(2).split("\\(")[0];
                    si.ssupertype = rs.getString(3);
                    String cntype = TYPE_MAPS.get(si.ssupertype);
                    if (cntype != null) {
                        si.ssupertype = cntype;
                    } else {
                        si.ssupertype = "未知";
                    }
                    shipinfo.put(si.smmi, si);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void creatpool(Map<String, String> conf) {
        ComboPooledDataSource cpdsource = new ComboPooledDataSource();
        try {
            cpdsource.setDriverClass("org.postgresql.Driver");
            cpdsource.setJdbcUrl("jdbc:postgresql://" + conf.get("ais.databaseurl"));
            cpdsource.setUser(conf.get("ais.databaseuser"));
            cpdsource.setPassword(conf.get("ais.databasepassword"));
            cpdsource.setMinPoolSize(2);
            cpdsource.setAcquireIncrement(5);
            cpdsource.setMaxPoolSize(5);
            cpdsource.setMaxStatements(300);
            cpds = cpdsource;
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }

    }

    private static void initshipAnalysis(Map<String, String> conf) throws Exception {
        ShipAnalysis ship = new ShipAnalysis();
        List<String> dbips = Arrays.asList((conf.get("ais.dbips")).split("\\|"));
        String[] portsstring = (conf.get("ais.ports")).split("\\|");
        List<Integer> ports = new ArrayList();

        for (int i = 0; i < portsstring.length; ++i) {
            ports.add(Integer.parseInt(portsstring[i]));
        }

        Map<String, ShipPort> shipPorts = new HashMap();
        String shipPortssql = "select portid,portname,latitude ,longitude from ship_port";
        SQLHelper.executeSearch(cpds, shipPortssql, (rs) -> {
            while (rs.next()) {
                ShipPort shipPort = new ShipPort();
                shipPort.oid = (long) rs.getInt(1);
                shipPort.name = rs.getString(2);
                shipPort.latitude = rs.getDouble(3);
                shipPort.longitude = rs.getDouble(4);
                shipPorts.put(shipPort.name, shipPort);
            }

        });
        Set<Long> mssis = new HashSet();
        String querymssissql = "select distinct smmsi from military_archive_info";
//        SQLHelper.executeSearch(cpds, querymssissql, (rs) -> {
//            while (rs.next()) {
//                mssis.add(rs.getLong(1));
//            }
//
//        });
        ship.init(2, dbips, ports, shipPorts, null, Integer.parseInt(conf.get("ais.timeoutSecond")), Integer.parseInt(conf.get("ais.cacheExpireTimeSecond")));
        shipAnalysis = ship;
    }

    public static void initareainfo() {
        ConcurrentHashMap<String, String> areainfos = new ConcurrentHashMap<>();
        String queryareasql = "select areaname,st_asewkt(geometry) as geometry  from area_info";
        try {
            SQLHelper.executeSearch(cpds, queryareasql, (rs) -> {
                while (rs.next()) {
                    areainfos.put(rs.getString(1), rs.getString(2));
                }
            });
            areainfo = areainfos;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateMajorMssi() {
        Set<Long> mssis = new HashSet<Long>();
        String querymssissql = "select distinct smmsi from military_archive_info";
        try {
            SQLHelper.executeSearch(cpds, querymssissql, (rs) -> {
                while (rs.next()) {
                    mssis.add(rs.getLong(1));
                }
            });
            shipAnalysis.updateMajorMssi(mssis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
