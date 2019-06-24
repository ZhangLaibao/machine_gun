package com.jr.test.tkij.conc.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by PengXianglong on 2018/8/26.
 */
public class FillingContainers {

    public static void main(String[] args) {
        List<String> copies = new ArrayList<>(
                Collections.nCopies(8, new String("nCopies")));
        System.out.println(copies);
        Collections.fill(copies, new String("fill"));
        System.out.println(copies);
    }

}
