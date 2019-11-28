package com.atguigu.gmall.cart.controller;

import java.math.BigDecimal;

public class TestBigDecimal {

    public static void main(String[] args) {
        // 初始化，尽量用字符串初始化
        BigDecimal b1 = new BigDecimal(0.01f);
        BigDecimal b2 = new BigDecimal(0.01d);
        BigDecimal b3 = new BigDecimal("0.01");
        System.out.println(b1); //0.00999999977648258209228515625
        System.out.println(b2); //0.01000000000000000020816681711721685132943093776702880859375
        System.out.println(b3); //0.01

        // 比较
        int i = b1.compareTo(b2);// 1 0 -1
        System.out.println(i); //-1

        // 运算
        BigDecimal add = b1.add(b2);
        System.out.println(add); //0.01999999977648258230045197336721685132943093776702880859375

        BigDecimal subtract = b2.subtract(b1);
        System.out.println(subtract);// 2.2351741811588166086721685132943093776702880859375E-10

        BigDecimal b4 = new BigDecimal("6");
        BigDecimal b5 = new BigDecimal("7");
        BigDecimal multiply = b4.multiply(b5);
        System.out.println(multiply); //42

        BigDecimal divide = b4.divide(b5,3,BigDecimal.ROUND_HALF_DOWN);//保留3位小数，四舍五入
        System.out.println(divide); //0.857

        // 约数
        BigDecimal subtract1 = b2.add(b1);
        BigDecimal bigDecimal = subtract1.setScale(3, BigDecimal.ROUND_HALF_DOWN);//保留3位小数，四舍五入
        System.out.println(bigDecimal); //0.020

    }
}
