package com.eun.carpet.pearlcannon;

import java.util.*;

public class PearlCannonCalculator {

    /**
     * 使用贪心算法将整数 value 转换为权重数组对应的二进制串。
     * @param value 要编码的整数
     * @param weights 权重数组，长度必须等于 bits
     * @param bits 位数
     * @return 二进制字符串，长度等于 bits
     * @throws IllegalArgumentException 如果 value 无法用给定权重表示
     */
    public static String encodeWithWeights(int value, int[] weights, int bits) throws IllegalArgumentException {
        if (value < 0) throw new IllegalArgumentException("值不能为负数");
        if (weights.length != bits) throw new IllegalArgumentException("权重数组长度必须等于位数");

        // 按权重降序排列，同时记录原始索引
        List<Map.Entry<Integer, Integer>> indexed = new ArrayList<>();
        for (int i = 0; i < weights.length; i++) {
            indexed.add(new AbstractMap.SimpleEntry<>(weights[i], i));
        }
        indexed.sort((a, b) -> b.getKey().compareTo(a.getKey()));

        char[] bitsArray = new char[bits];
        Arrays.fill(bitsArray, '0');
        int remaining = value;

        for (Map.Entry<Integer, Integer> entry : indexed) {
            int weight = entry.getKey();
            int idx = entry.getValue();
            if (remaining >= weight) {
                bitsArray[idx] = '1';
                remaining -= weight;
            }
        }

        if (remaining != 0) {
            throw new IllegalArgumentException("无法用给定权重表示数值 " + value + "，剩余 " + remaining);
        }

        return new String(bitsArray);
    }

    /**
     * 将二进制串按指定组大小格式化（每 groupSize 位加一个空格）
     */
    public static String formatBinaryGrouped(String binary, int groupSize) {
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < binary.length(); i += groupSize) {
            if (i > 0) grouped.append(' ');
            grouped.append(binary, i, Math.min(i + groupSize, binary.length()));
        }
        return grouped.toString();
    }

    /**
     * 验证二进制串对应的总和是否正确（用于调试）
     */
    public static int verifySum(String binary, int[] weights) {
        int sum = 0;
        for (int i = 0; i < binary.length(); i++) {
            if (binary.charAt(i) == '1') {
                sum += weights[i];
            }
        }
        return sum;
    }
}