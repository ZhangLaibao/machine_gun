package com.jr.test.jvm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by PengXianglong on 2018/5/31.
 */
public class HeapOOMExample {

    public static void main(String[] args) {

        List<Date> list = new ArrayList<>();
        while(true){
            list.add(new Date());
        }

    }

}
