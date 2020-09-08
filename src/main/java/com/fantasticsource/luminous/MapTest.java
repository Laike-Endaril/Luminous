package com.fantasticsource.luminous;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapTest
{
    public static void main(String[] args)
    {
        int iterations = 1000000;

        long time1, time2, time3;
        System.nanoTime(); //Just to make sure any possible overhead is done...though there shouldn't really be any

        int sequential[] = new int[iterations]; //Counting up from 0
        int random[] = new int[iterations]; //Same set of values, but randomized (no duplicates)
        HashSet<Integer> addedRandoms = new HashSet<>();
        for (int i = 0; i < iterations; i++)
        {
            sequential[i] = i;

            int randomVal = random(iterations);
            while (addedRandoms.contains(randomVal)) randomVal = random(iterations); //Get another random instead of sequentially finding another unused value, to prevent clumping
            addedRandoms.add(randomVal);
            random[i] = random(iterations);
        }

        HashMap<Integer, Integer> hashMap = new HashMap<>();
        LinkedHashMap<Integer, Integer> linkedHashMap = new LinkedHashMap<>();


        int gcRuns = 0, prevGCRuns;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) gcRuns += gcBean.getCollectionCount();
        prevGCRuns = gcRuns;


        //Test
        time1 = System.nanoTime();
        for (int i : sequential) hashMap.put(i, 0);
        time2 = System.nanoTime();
        for (int i : sequential) linkedHashMap.put(i, 0);
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("Put: sequential key (from 0 to " + (iterations - 1) + "), no overwrites", time1, time2, time3, prevGCRuns);


        //Test
        time1 = System.nanoTime();
        for (int i : random) hashMap.put(i, 0);
        time2 = System.nanoTime();
        for (int i : random) linkedHashMap.put(i, 0);
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("Put: random key (between 0 and " + (iterations - 1) + " inclusive), all overwrites (exactly one per entry, random order)", time1, time2, time3, prevGCRuns);


        //Attempt GC
        System.gc();
        prevGCRuns = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) prevGCRuns += gcBean.getCollectionCount();


        //Test
        time1 = System.nanoTime();
        for (int i : sequential) hashMap.put(i, 0);
        time2 = System.nanoTime();
        for (int i : sequential) linkedHashMap.put(i, 0);
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("Put: sequential key (from 0 to " + (iterations - 1) + "), all overwrites (exactly one per entry, sequential order)", time1, time2, time3, prevGCRuns);


        //Empty maps
        hashMap = new HashMap<>();
        linkedHashMap = new LinkedHashMap<>();


        //Attempt GC
        System.gc();
        prevGCRuns = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) prevGCRuns += gcBean.getCollectionCount();


        //Test
        time1 = System.nanoTime();
        for (int i : random) hashMap.put(i, 0);
        time2 = System.nanoTime();
        for (int i : random) linkedHashMap.put(i, 0);
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("Put: random key (between 0 and " + (iterations - 1) + " inclusive), no overwrites", time1, time2, time3, prevGCRuns);


        //Test
        time1 = System.nanoTime();
        for (int i : sequential) hashMap.get(i);
        time2 = System.nanoTime();
        for (int i : sequential) linkedHashMap.get(i);
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("Sequential get, randomized internal keys", time1, time2, time3, prevGCRuns);


        //Test
        time1 = System.nanoTime();
        for (int i : random) hashMap.get(i);
        time2 = System.nanoTime();
        for (int i : random) linkedHashMap.get(i);
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("Random get, randomized internal keys", time1, time2, time3, prevGCRuns);


        //Set sequential keys
        hashMap = new HashMap<>();
        linkedHashMap = new LinkedHashMap<>();
        for (int i : sequential)
        {
            hashMap.put(i, 0);
            linkedHashMap.put(i, 0);
        }


        //Attempt GC
        System.gc();
        prevGCRuns = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) prevGCRuns += gcBean.getCollectionCount();


        //Test
        time1 = System.nanoTime();
        for (int i : sequential) hashMap.get(i);
        time2 = System.nanoTime();
        for (int i : sequential) linkedHashMap.get(i);
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("Sequential get, sequential internal keys", time1, time2, time3, prevGCRuns);


        //Test
        time1 = System.nanoTime();
        for (int i : random) hashMap.get(i);
        time2 = System.nanoTime();
        for (int i : random) linkedHashMap.get(i);
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("Random get, sequential internal keys", time1, time2, time3, prevGCRuns);


        //Test
        time1 = System.nanoTime();
        for (int i : hashMap.values()) ;
        time2 = System.nanoTime();
        for (int i : linkedHashMap.values()) ;
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("values() iteration, sequential internal keys", time1, time2, time3, prevGCRuns);


        //Set random keys
        hashMap = new HashMap<>();
        linkedHashMap = new LinkedHashMap<>();
        for (int i : random)
        {
            hashMap.put(i, 0);
            linkedHashMap.put(i, 0);
        }


        //Attempt GC
        System.gc();
        prevGCRuns = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) prevGCRuns += gcBean.getCollectionCount();


        //Test
        time1 = System.nanoTime();
        for (int i : hashMap.values()) ;
        time2 = System.nanoTime();
        for (int i : linkedHashMap.values()) ;
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("values() iteration, randomized internal keys", time1, time2, time3, prevGCRuns);


        //Test
        time1 = System.nanoTime();
        for (int i : hashMap.keySet()) ;
        time2 = System.nanoTime();
        for (int i : linkedHashMap.keySet()) ;
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("keySet() iteration, randomized internal keys", time1, time2, time3, prevGCRuns);


        //Set sequential keys
        hashMap = new HashMap<>();
        linkedHashMap = new LinkedHashMap<>();
        for (int i : sequential)
        {
            hashMap.put(i, 0);
            linkedHashMap.put(i, 0);
        }


        //Attempt GC
        System.gc();
        prevGCRuns = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) prevGCRuns += gcBean.getCollectionCount();


        //Test
        time1 = System.nanoTime();
        for (int i : hashMap.keySet()) ;
        time2 = System.nanoTime();
        for (int i : linkedHashMap.keySet()) ;
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("keySet() iteration, sequential internal keys", time1, time2, time3, prevGCRuns);


        //Test
        time1 = System.nanoTime();
        for (Map.Entry<Integer, Integer> entry : hashMap.entrySet()) ;
        time2 = System.nanoTime();
        for (Map.Entry<Integer, Integer> entry : linkedHashMap.entrySet()) ;
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("entrySet() iteration, sequential internal keys", time1, time2, time3, prevGCRuns);


        //Set random keys
        hashMap = new HashMap<>();
        linkedHashMap = new LinkedHashMap<>();
        for (int i : random)
        {
            hashMap.put(i, 0);
            linkedHashMap.put(i, 0);
        }


        //Attempt GC
        System.gc();
        prevGCRuns = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) prevGCRuns += gcBean.getCollectionCount();


        //Test
        time1 = System.nanoTime();
        for (Map.Entry<Integer, Integer> entry : hashMap.entrySet()) ;
        time2 = System.nanoTime();
        for (Map.Entry<Integer, Integer> entry : linkedHashMap.entrySet()) ;
        time3 = System.nanoTime();

        prevGCRuns = printAndReset("entrySet() iteration, randomized internal keys", time1, time2, time3, prevGCRuns);


        //Test
        time1 = System.nanoTime();
        for (int i : random) time3 = sequential[i];
        time2 = System.nanoTime();
        System.out.println("Array get: " + (time2 - time1) + " nanos");
    }


    protected static int printAndReset(String description, long time1, long time2, long time3, int prevGCRuns)
    {
        System.out.println(description);
        System.out.println("HashMap: " + (time2 - time1) + " nanos");
        System.out.println("LinkedHashMap: " + (time3 - time2) + " nanos");
        int gcRuns = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) gcRuns += gcBean.getCollectionCount();
        System.out.println("GC during test: " + (gcRuns != prevGCRuns));
        System.out.println();

        return gcRuns;
    }


    public static int random(int maxvalue)
    {
        return (int) ((double) maxvalue * Math.random());
    }
}
