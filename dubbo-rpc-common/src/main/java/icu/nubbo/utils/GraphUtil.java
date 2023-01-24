package icu.nubbo.utils;

import java.util.*;

public class GraphUtil {

    // 检查图是否有环
    public static <T> boolean isCircle(Map<T, List<T>> graph) {
        if (graph == null || graph.size() < 2) {
            return false;
        }
        // 用于存储依赖图的多个岛屿
//        List<Set<T>> seens = new ArrayList<>();
        Set<T> seen = new HashSet<>();
        for (T key : graph.keySet()) {
//            // 如果要访问的某个节点已经在岛屿中，跳过
//            if(checkSeens(seens, key)) {
//                continue;
//            }
            seen.clear();
            boolean res = dfsCheckDependency(graph, key, seen);
            if (!res) {
                return true;
            }
//            seens.add(seen);
        }
        return false;
    }

//    private static <T> boolean checkSeens(List<Set<T>> seens, T key) {
//        for (Set<T> seen : seens) {
//            if (seen.contains(key)) {
//                return true;
//            }
//        }
//        return false;
//    }

    // 深搜检查是否有循环依赖
    // 如果没有返回true，否则返回false
    private static <T> boolean dfsCheckDependency(Map<T, List<T>> graph, T start, Set<T> seen) {
        seen.add(start);
        List<T> paths = graph.get(start);
        if (paths == null || paths.isEmpty()) {
            return true;
        }
        boolean res = true;
        for (T path : paths) {
            res = dfsCheckDependency(graph, path, seen);
            // 如果检测到环，直接返回false
            if (!res) return false;
        }
        return res;
    }
}
