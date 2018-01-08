package com.spare.cointrade.model;

/**
 * exponentially weighted moving average
 */
public class Ewma {

    private Double thelta;

    private Double value;

    public Ewma() {
        this.thelta = 0.9;
        this.value = 0.0;
    }

    public void setValue(Double newVal) {
        this.value = (1 - thelta) * newVal + thelta * this.value;
    }

    public Double getValue() {
        return this.value;
    }
}
