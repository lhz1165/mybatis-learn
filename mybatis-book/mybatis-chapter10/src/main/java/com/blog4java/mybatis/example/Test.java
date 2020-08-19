package com.blog4java.mybatis.example;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author: lhz
 * @date: 2020/8/18
 **/
public class Test {
    public static void main(String[] args) {

        List<Integer> list = new ArrayList<>(Arrays.asList(1,2,3,4,5));
        Integer sum = list.stream()
                .collect(Collectors.reducing(0, a -> a, (a1, a2) -> a1 + a2));
        System.out.println(sum);

        Integer max = list.stream().collect(Collectors.reducing((a, b) -> a > b ? a : b)).get();

        Map<String, Long> collect = list.stream()
                .collect(Collectors.groupingBy(a -> {
                    if (a < 5) {
                        return "小于5";
                    } else {
                        return "大于5";
                    }
                }, Collectors.counting()));

        System.out.println(collect);

        Map<String, Optional<Integer>> collect1 = list.stream()
                .collect(Collectors.groupingBy(a -> {
                    if (a < 5) {
                        return "小于5";
                    } else {
                        return "大于5";
                    }
                }, Collectors.maxBy(Integer::compareTo)));


        System.out.println(collect);
        // System.out.println(collect);

        System.out.println(max);


        Test t = new Test();
        System.out.println(t.part(10));


    }

    public static boolean isPrime(int c) {
        return IntStream.range(2, c)
                .noneMatch(i -> c % i == 0);
    }

    public Map<Boolean, List<Integer>> part(int n) {
        Map<Boolean, List<Integer>> collect = IntStream.rangeClosed(2, 10)
                .boxed()
                .collect(Collectors.partitioningBy(i -> isPrime(i)));
        return collect;
    }

}
