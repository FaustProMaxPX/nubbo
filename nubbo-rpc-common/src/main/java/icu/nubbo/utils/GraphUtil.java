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
        // 在当前子图看到的节点
        Set<T> seen = new HashSet<>();
        // 访问过的所有节点
        Set<T> visit = new HashSet<>();
        for (T key : graph.keySet()) {
            // 如果当前节点已经被访问过，跳过
            if (visit.contains(key)) continue;
            boolean res = dfsCheckDependency(graph, key, seen, visit);
            if (!res) {
                return true;
            }
//            seens.add(seen);
        }
        return false;
    }

    private static <T> boolean checkVisit(Set<T> seens, T key) {
        return seens.contains(key);
    }

    // 深搜检查是否有循环依赖
    // 如果没有返回true，否则返回false
    private static <T> boolean dfsCheckDependency(Map<T, List<T>> graph, T start, Set<T> seen, Set<T> visit) {
        // 如果来到了已经访问过的节点，代表有环
        if (seen.contains(start)) {
            return false;
        }
        seen.add(start);
        visit.add(start);
        List<T> paths = graph.get(start);
        if (paths == null || paths.isEmpty()) {
            seen.remove(start);
            return true;
        }
        boolean res = true;
        for (T path : paths) {
            res = dfsCheckDependency(graph, path, seen, visit);
            // 如果检测到环，直接返回false
            if (!res) return false;
        }
        seen.remove(start);
        return res;
    }
}
