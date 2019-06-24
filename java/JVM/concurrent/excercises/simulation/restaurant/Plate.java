package com.jr.test.tkij.conc.simulation.restaurant;

/**
 * Created by PengXianglong on 2018/8/8.
 */
public class Plate {

    private final Order order;
    private final Food food;

    public Plate(Order order, Food food) {
        this.order = order;
        this.food = food;
    }

    public Order getOrder() {
        return order;
    }

    public Food getFood() {
        return food;
    }

    @Override
    public String toString() {
        return food.toString();
    }
}
