package com;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class HungarianAssignment {

    public static class Result {
        public final java.util.Map<Integer, String> itemToDrone; // itemId -> droneId
        public final double totalScore;
        public Result(java.util.Map<Integer, String> mapping, double score) {
            this.itemToDrone = mapping;
            this.totalScore = score;
        }
    }

    public static Result findBestAssignment(java.util.Map<String, java.util.Map<Integer, Double>> scoreEstimates) {
        // Get ordered lists of drones and items
        List<String> drones = new ArrayList<>(scoreEstimates.keySet());
        Set<Integer> itemSet = new HashSet<>();
        for (java.util.Map<Integer, Double> m : scoreEstimates.values()) {
            itemSet.addAll(m.keySet());
        }
        List<Integer> items = new ArrayList<>(itemSet);

        int n = Math.max(drones.size(), items.size());
        double[][] cost = new double[n][n];

        // Fill the matrix (Hungarian algo finds min cost, so use -score for max)
        for (int i = 0; i < items.size(); i++) {
            for (int j = 0; j < drones.size(); j++) {
                double score = scoreEstimates.getOrDefault(drones.get(j), Collections.emptyMap())
                                             .getOrDefault(items.get(i), 0.0);
                cost[i][j] = -score;
            }
        }

        int[] assignment = hungarian(cost);

        // Build result mapping
        java.util.Map<Integer, String> mapping = new java.util.HashMap<>();
        double totalScore = 0.0;
        for (int i = 0; i < items.size(); i++) {
            int droneIndex = assignment[i];
            if (droneIndex >= 0 && droneIndex < drones.size()) {
                String droneId = drones.get(droneIndex);
                mapping.put(items.get(i), droneId);
                totalScore += scoreEstimates.get(droneId).getOrDefault(items.get(i), 0.0);
            }
        }

        return new Result(mapping, totalScore);
    }

    // Hungarian algorithm (O(n^3)) — works for rectangular matrices
    private static int[] hungarian(double[][] costMatrix) {
        int n = costMatrix.length;
        double[] u = new double[n + 1];
        double[] v = new double[n + 1];
        int[] p = new int[n + 1];
        int[] way = new int[n + 1];

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            double[] minv = new double[n + 1];
            Arrays.fill(minv, Double.POSITIVE_INFINITY);
            boolean[] used = new boolean[n + 1];
            do {
                used[j0] = true;
                int i0 = p[j0];
                double delta = Double.POSITIVE_INFINITY;
                int j1 = 0;
                for (int j = 1; j <= n; j++) {
                    if (!used[j]) {
                        double cur = costMatrix[i0 - 1][j - 1] - u[i0] - v[j];
                        if (cur < minv[j]) {
                            minv[j] = cur;
                            way[j] = j0;
                        }
                        if (minv[j] < delta) {
                            delta = minv[j];
                            j1 = j;
                        }
                    }
                }
                for (int j = 0; j <= n; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }
                j0 = j1;
            } while (p[j0] != 0);
            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] assignment = new int[n];
        for (int j = 1; j <= n; j++) {
            if (p[j] > 0 && p[j] <= n) {
                assignment[p[j] - 1] = j - 1;
            }
        }
        return assignment;
    }

}