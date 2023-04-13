package org.lmars.geodata.ais.bean;

public class Result {
    private double[][] data;
    private String func;
    private double preData;

    public Result() {
        this.data=null;
        this.func=null;
        this.preData=-1;
    }

    public Result(double[][] data, String func,double preData) {
        this.data = data;
        this.func = func;
        this.preData =preData;
    }

    public double[][] getData(){
        return this.data;
    }

    public String getFunc(){
        return this.func;
    }

    public double getPreData(){
        return this.preData;
    }
}
