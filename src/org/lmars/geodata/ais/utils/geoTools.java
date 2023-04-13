package org.lmars.geodata.ais.utils;

import com.vividsolutions.jts.geom.Coordinate;
import org.osgeo.proj4j.BasicCoordinateTransform;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.ProjCoordinate;

import java.math.BigDecimal;

public class geoTools {

    //      赤道半径
    private final static double EarthRadius = 6378137.0;
    //    平均地球半径
    private final static double EarthRadiusAverage = 6371393.0;
    //     地球周长
    private final static double EarthPerimeter = 2 * Math.PI * EarthRadius;
    //    坐标原点
    private final static Coordinate origin = new Coordinate(-EarthPerimeter / 2.0, EarthPerimeter / 2.0);
    private final static BasicCoordinateTransform transform1;
    private final static BasicCoordinateTransform transform2;
    private final static CRSFactory crsFactory = new CRSFactory();
    private final static CoordinateReferenceSystem WGS84CRS = crsFactory.createFromName("EPSG:4326");
    private final static CoordinateReferenceSystem WebMercatorCRS = crsFactory.createFromName("EPSG:3857");

    static {
        transform1 = new BasicCoordinateTransform(WGS84CRS, WebMercatorCRS);
        transform2 = new BasicCoordinateTransform(WebMercatorCRS, WGS84CRS);
    }

    // 计算两个经纬度点之间的距离
    public static double getDistanceByLL(double longitude1, double latitude1, double longitude2, double latitude2) {
        double radLat1 = latitude1 * Math.PI / 180.0;
        double radLat2 = latitude2 * Math.PI / 180.0;
        double radLon1 = longitude1 * Math.PI / 180.0;
        double radLon2 = longitude2 * Math.PI / 180.0;
        double a = radLat1 - radLat2;
        double b = radLon1 - radLon2;
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2))) * EarthRadiusAverage;
        s = Math.round(s * 10000d) / 10000d;
        return s;
    }

    public static double getDistanceByPoints(double longitude, double latitude, double[][] points) {
        double distance = 0;
        for(int i =0;i<points.length;i++){
            double lat = points[i][0];
            double lon = points[i][1];
            distance = distance + getDistanceByLL(longitude,latitude,lon,lat);
        }
        double outcome = distance/points.length;
        return outcome;
    }

//    根据经纬度计算两点方位角（0-180）
    public static double getAngleByLL(double lon1, double lat1, double lon2, double lat2) {
        double out = 0;
        double longitude1 = lon1;
        double longitude2 = lon2;
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff = Math.toRadians(longitude2-longitude1);
        double y = Math.sin(longDiff)*Math.cos(latitude2);
        double x = Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);
        double angle =  (Math.toDegrees(Math.atan2(y, x))+360)%360;
        if(angle>180){
            out = angle-180;
        }else {
            out =angle;
        }
        return out;
    }

    /**
     * 经纬度转墨卡托
     *
     * @param pt 经纬度坐标,x为经度,y为纬度
     * @return 墨卡托坐标
     */
    public static Coordinate geographic2Mercator(Coordinate pt) {
        synchronized (transform1) {
            ProjCoordinate pt1 = new ProjCoordinate(pt.x, pt.y);
            ProjCoordinate pt2 = new ProjCoordinate();
            transform1.transform(pt1, pt2);
            return new Coordinate(pt2.x, pt2.y);
        }
    }

    //    通过中心点经纬度和距离获得经纬度描述的正方形搜索框
    public static String getSearchSquare(double longitude, double latitude, long distance) {

        // 求东西两侧的的范围边界。在haversin公式中令φ1 = φ2(维度相同),传入的距离为米，转换为千米
        double dlng = 2 * Math.asin(Math.sin((distance) / (2 * EarthRadiusAverage))
                / Math.cos(latitude * Math.PI / 180));
        // 弧度转换成角度
        // dlng = Math.toRadians(dlng);
        dlng = Math.toDegrees(dlng);

        // 然后求南北两侧的范围边界，在haversin公式中令 Δλ = 0
        double dlat = distance / EarthRadiusAverage;
        // 弧度转换成角度
        dlat = Math.toDegrees(dlat);

        // 通过计算可以得到上下左右四个点的经纬度，即，两个经度，两个纬度
        double minLon = longitude - new BigDecimal(dlng).setScale(8, BigDecimal.ROUND_HALF_UP).doubleValue();
        double minLat = latitude - new BigDecimal(dlat).setScale(8, BigDecimal.ROUND_HALF_UP).doubleValue();

        double maxLon = longitude + new BigDecimal(dlng).setScale(8, BigDecimal.ROUND_HALF_UP).doubleValue();
        double maxLat = latitude + new BigDecimal(dlat).setScale(8, BigDecimal.ROUND_HALF_UP).doubleValue();

        StringBuilder result = new StringBuilder();
        result.append(minLon).append(" ").append(minLat).append("|")
                .append(minLon).append(" ").append(maxLat).append("|")
                .append(maxLon).append(" ").append(maxLat).append("|")
                .append(maxLon).append(" ").append(minLat).append("|")
                .append(minLon).append(" ").append(minLat);
        return result.toString();

    }

    public static void main(String[] args) {
//        Coordinate pt = new Coordinate(139.73981, 35.30376);
//        Coordinate out = geographic2Mercator(pt);
//        BigDecimal bigX = new BigDecimal(out.x).setScale(3, BigDecimal.ROUND_HALF_UP);
//        BigDecimal bigY = new BigDecimal(out.y).setScale(3, BigDecimal.ROUND_HALF_UP);
//        System.out.println(bigX);
//        System.out.println(bigY);
        double[] lon1={139.661471865};
        double[] lat1={35.29498306};
        double[] lon2={139.664195059};
        double[] lat2={35.293120615};
        double angle = getAngleByLL(lon1[0],lat1[0],lon2[0],lat2[0]);
        System.out.printf(String.valueOf(angle));
    }
}
