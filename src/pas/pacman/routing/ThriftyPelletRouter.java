package src.pas.pacman.routing;

import java.util.*;

import edu.bu.pas.pacman.game.Game;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.game.Tile;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.game.Board;
import edu.bu.pas.pacman.game.Tile;
import edu.bu.pas.pacman.game.Action;

public class ThriftyPelletRouter extends PelletRouter {

    private Map<Coordinate, Map<Coordinate, Integer>> distCache = new HashMap<>();
    private GameView cachedGame;

    public ThriftyPelletRouter(int myUnitId,
            int pacmanId,
            int ghostChaseRadius) {
        super(myUnitId, pacmanId, ghostChaseRadius);
    }

    @Override
    public Collection<PelletVertex> getOutgoingNeighbors(
            final PelletVertex src,
            final GameView game,
            final ExtraParams params) {

        Collection<PelletVertex> neighbors = new ArrayList<>();

        for (Coordinate pellet : src.getRemainingPelletCoordinates()) {
            neighbors.add(src.removePellet(pellet));
        }

        return neighbors;
    }

    private void buildDistanceCache(GameView game, PelletVertex start) {

        distCache = new HashMap<>();

        Set<Coordinate> important = new HashSet<>();
        important.add(start.getPacmanCoordinate());
        important.addAll(start.getRemainingPelletCoordinates());

        for (Coordinate src : important) {
            distCache.put(src, bfsFrom(src, game));
        }
    }

    private Map<Coordinate, Integer> bfsFrom(
            Coordinate start,
            GameView game) {

        Map<Coordinate, Integer> dist = new HashMap<>();
        Queue<Coordinate> q = new ArrayDeque<>();

        dist.put(start, 0);
        q.add(start);

        while (!q.isEmpty()) {

            Coordinate current = q.poll();
            int currentDist = dist.get(current);

            for (Action action : Action.values()) {

                if (game.isLegalPacmanMove(current, action)) {

                    Coordinate neighbor = action.apply(current);

                    if (!dist.containsKey(neighbor)) {
                        dist.put(neighbor, currentDist + 1);
                        q.add(neighbor);
                    }
                }
            }
        }

        return dist;
    }

    private void ensureBfs(Coordinate src, GameView game) {
        if (src == null) return;
        if (distCache == null) distCache = new HashMap<>();
        if (!distCache.containsKey(src)) {
            distCache.put(src, bfsFrom(src, game));
        }
    }

    private Map<Coordinate, Integer> bfsCached(Coordinate src, GameView game) {
        return distCache.computeIfAbsent(src, k -> bfsFrom(k, game));
    }

    private int dist(Coordinate from, Coordinate to, GameView game) {
        Integer d = bfsCached(from, game).get(to);
        return (d == null) ? Integer.MAX_VALUE / 4 : d;
    }

    @Override
    public float getEdgeWeight(final PelletVertex src,
            final PelletVertex dst,
            final ExtraParams params) {

        Coordinate from = src.getPacmanCoordinate();
        Coordinate to = dst.getPacmanCoordinate();

        if (cachedGame == null) return Float.MAX_VALUE;
        ensureBfs(from, cachedGame);

        Integer d = distCache.get(from).get(to);
        return (d == null) ? Float.MAX_VALUE : d;
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                            final GameView game,
                            final ExtraParams params) {

        Collection<Coordinate> rem = src.getRemainingPelletCoordinates();
        if (rem.isEmpty()) return 0f;

        Coordinate pac = src.getPacmanCoordinate();
        Map<Coordinate, Integer> pacMap = bfsCached(pac, game);

        int best = Integer.MAX_VALUE;
        for (Coordinate pellet : rem) {
            Integer d = pacMap.get(pellet);
            if (d != null && d < best) best = d;
        }
        return (best == Integer.MAX_VALUE) ? 0f : best;
    }

    private static class Node {
        final Path<PelletVertex> path;
        final float f;
        Node(Path<PelletVertex> path, float f) {
            this.path = path;
            this.f = f;
        }
    }

    @Override
    public Path<PelletVertex> graphSearch(final GameView game) {

        distCache.clear(); // important: new search/game tick

        PelletVertex start = new PelletVertex(game);

        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<PelletVertex, Float> bestG = new HashMap<>();
        Map<PelletVertex, Float> hMemo = new HashMap<>(); // optional but helps a lot

        float h0 = hMemo.computeIfAbsent(start, v -> getHeuristic(v, game, null));
        frontier.add(new Node(new Path<>(start), h0));
        bestG.put(start, 0f);

        while (!frontier.isEmpty()) {

            Node curNode = frontier.poll();
            Path<PelletVertex> curPath = curNode.path;
            PelletVertex cur = curPath.getDestination();
            float g = curPath.getTrueCost();

            if (cur.getRemainingPelletCoordinates().isEmpty()) return curPath;
            if (g > bestG.getOrDefault(cur, Float.MAX_VALUE)) continue;

            for (PelletVertex nb : getOutgoingNeighbors(cur, game, null)) {

                float edge = dist(cur.getPacmanCoordinate(), nb.getPacmanCoordinate(), game);
                float g2 = g + edge;

                if (g2 < bestG.getOrDefault(nb, Float.MAX_VALUE)) {
                    bestG.put(nb, g2);

                    Path<PelletVertex> nbPath = new Path<>(nb, edge, curPath);
                    float h = hMemo.computeIfAbsent(nb, v -> getHeuristic(v, game, null));
                    frontier.add(new Node(nbPath, g2 + h));
                }
            }
        }
        return null;
    }
}