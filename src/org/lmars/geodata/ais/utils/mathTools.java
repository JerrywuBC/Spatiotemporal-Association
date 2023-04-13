package org.lmars.geodata.ais.utils;

import com.zeroc.IceInternal.Ex;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.lmars.geodata.ais.bean.Result;

import java.util.ArrayList;
import java.util.List;

//自定义函数拟合模型
public class mathTools {
/*定义拟合函数*/
    static class MyFunction implements ParametricUnivariateFunction {
        public double value(double x, double ... parameters) {
            double a = parameters[0];
            double b = parameters[1];
            double c = parameters[2];
            double d = parameters[3];
//            自定义函数
            return d + ((a - d) / (1 + Math.pow(x / c, b)));
        }

        public double[] gradient(double x, double ... parameters) {
            double a = parameters[0];
            double b = parameters[1];
            double c = parameters[2];
            double d = parameters[3];

            double[] gradients = new double[4];
            double den = 1 + Math.pow(x / c, b);

            gradients[0] = 1 / den; // 对 a 求导

            gradients[1] = -((a - d) * Math.pow(x / c, b) * Math.log(x / c)) / (den * den); // 对 b 求导

            gradients[2] = (b * Math.pow(x / c, b - 1) * (x / (c * c)) * (a - d)) / (den * den); // 对 c 求导

            gradients[3] = 1 - (1 / den); // 对 d 求导

            return gradients;

        }
    }
/*生成随机离散点*/
    public static double[][] customizeFuncScatters() {
        MyFunction function = new MyFunction();
        List<double[]> data = new ArrayList<>();
        for (double x = 7; x <= 10000; x *= 1.5) {
            double y = function.value(x, 1500, 0.95, 65, 35000);
            y += Math.random() * 5000 - 2000; // 随机数
            double[] xy = {x, y};
            data.add(xy);
        }
        return data.stream().toArray(double[][]::new);
    }

    public static Result customizeFuncFit(double[][] scatters, double baseData) {
        try {
            ParametricUnivariateFunction function = new MyFunction();/*多项式函数*/
            double[] guess = {1, 1, 1, 1}; /*猜测值 依次为 a b c d 。必须和 gradient 方法返回数组对应。如果不知道都设置为 1*/

            // 初始化拟合
            SimpleCurveFitter curveFitter = SimpleCurveFitter.create(function, guess);

            // 添加数据点
            WeightedObservedPoints observedPoints = new WeightedObservedPoints();
            for (double[] point : scatters) {
                observedPoints.add(point[0], point[1]);
            }

            /*
             * best 为拟合结果 对应 a b c d
             * 可能会出现无法拟合的情况
             * 需要合理设置初始值
             * */
            double[] best = curveFitter.fit(observedPoints.toList());
            double a = best[0];
            double b = best[1];
            double c = best[2];
            double d = best[3];

            // 根据拟合结果生成拟合曲线散点
            List<double[]> fitData = new ArrayList<>();
            for (double[] datum : scatters) {
                double x = datum[0];
                double y = function.value(x, a, b, c, d);
                double[] xy = {x, y};
                fitData.add(xy);
            }

//      基于基准值进行预测
            double preData = 0;
            preData = function.value(baseData, a, b, c, d);


            // f(x) = d + ((a - d) / (1 + Math.pow(x / c, b)))
            StringBuilder strBFunc = new StringBuilder();
            strBFunc.append("f(x) =");
            strBFunc.append(d > 0 ? " " : " - ");
            strBFunc.append(Math.abs(d));
            strBFunc.append(" ((");
            strBFunc.append(a > 0 ? "" : "-");
            strBFunc.append(Math.abs(a));
            strBFunc.append(d > 0 ? " - " : " + ");
            strBFunc.append(Math.abs(d));
            strBFunc.append(" / (1 + ");
            strBFunc.append("(x / ");
            strBFunc.append(c > 0 ? "" : " - ");
            strBFunc.append(Math.abs(c));
            strBFunc.append(") ^ ");
            strBFunc.append(b > 0 ? " " : " - ");
            strBFunc.append(Math.abs(b));
            return new Result(fitData.stream().toArray(double[][]::new), strBFunc.toString(),preData);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

//    匈牙利算法
    public static class Hungary {
        int[][] graph;  //需要计算的图的邻接矩阵，注意每个顶点和它自己的连接被设置成了0。另外graph需要是n*n的矩阵
        int[] match;    //记录每个顶点的匹配顶点。假如match[0]=1，就是说顶点0和顶点1已经匹配
        int len;        //图的顶点的个数
        boolean[] used; //在从每个顶点搜索其增广路径的循环中，记录每个顶点是否已经被访问过

        public Hungary(int[][] graph) {
            this.graph = graph;
            len = graph.length;
            used = new boolean[len];

            match = new int[len];
            for (int i = 0; i < len; i++) {
                match[i] = -1;
                used[i] = false;
            }
        }

        //寻找顶点x的增广路径。如果能够寻找到则返回true，否则返回false。
        //匈牙利算法一个重要的定理：如果从一个顶点A出发，没有找到增广路径，那么无论再从别的点出发找到多少增广路径来改变现在的匹配，从A出发都永远找不到增广路径
        boolean findAugmentPath(int x) {
            for (int i = 0; i < len; i++) {
                if (graph[x][i] == 1) { //顶点x和顶点i之间有连接。需要注意的一点是我们在输入需要计算的图的邻接矩阵的时候把对角线上的元素设置为0
                    if (!used[i]) {     //如果顶点i还未访问
                        used[i] = true;
                        //如果顶点i还未匹配，或者与顶点i匹配的那个顶点可以换个顶点匹配（也就是说可以把顶点i“让给”当前顶点x），则把顶点x和顶点i为对方的匹配顶点
                        //由于上一步已经将顶点i设置成used，所以findAugmentPath(match[i])不会再考虑顶点i了
                        if (match[i] == -1 || findAugmentPath(match[i])) {
                            match[x] = i;
                            match[i] = x;
                            System.out.println(x + "------>" + i);
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        void search() {
            //对于每个顶点都循环处理
            for (int i = 0; i < len; i++) {
                if (match[i] == -1) {   //如果当前顶点已经有匹配的顶点了，就略过此顶点
                    clearUsed();    //新的一轮搜索，把used数组设置成false
                    System.out.println("开始匹配顶点" + i);
                    findAugmentPath(i);
                }
            }

            System.out.println();
            System.out.println();
            System.out.println();

            for (int i = 0; i < len; i++) {
                System.out.println(i + "------>" + match[i]);
            }
        }

        void clearUsed() {
            for (int i = 0; i < len; i++) {
                used[i] = false;
            }
        }


    }

    public static void main(String[] args) {
//        double[][] scatters = customizeFuncScatters();
//        Result result = customizeFuncFit(scatters, 0);
//        String modle = result.getFunc();
//        System.out.println(modle);

        int[][] graph = {{0, 0, 0, 0, 1, 0, 1, 0},
                {0, 0, 0, 0, 1, 0, 0, 0},
                {0, 0, 0, 0, 1, 1, 0, 0},
                {0, 0, 0, 0, 0, 0, 1, 1},
                {1, 1, 1, 0, 0, 0, 0, 0},
                {0, 0, 1, 0, 0, 0, 0, 0},
                {1, 0, 0, 1, 0, 0, 0, 0},
                {0, 0, 0, 1, 0, 0, 0, 0}};
        new Hungary(graph).search();
    }
}
