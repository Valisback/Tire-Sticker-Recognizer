package com.example.tirestickerrecognition;

public class Tire {
    private long id;
    private String frontSize;
    private String rearSize;

    public Tire(){

    }

    public Tire(long id, String frontSize, String rearSize){
        this.id = id;
        this.frontSize = frontSize;
        this.rearSize = rearSize;
    }

    public long getId() {
        return id;
    }

    public String getFrontSize() {
        return frontSize;
    }

    public String getRearSize() {
        return rearSize;
    }

    public void setFrontSize(String frontSize) {
        this.frontSize = frontSize;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setRearSize(String rearSize) {
        this.rearSize = rearSize;
    }
}

