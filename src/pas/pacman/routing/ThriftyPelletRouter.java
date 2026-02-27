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

    private Map<Coordinate, Map<Coordinate, Integer>> distCache;

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
        Queue<Coordinate> q = new LinkedList<>();

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

    @Override
    public float getEdgeWeight(final PelletVertex src,
            final PelletVertex dst,
            final ExtraParams params) {

        Coordinate from = src.getPacmanCoordinate();
        Coordinate to = dst.getPacmanCoordinate();

        return distCache.get(from).get(to);
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                          final GameView game,
                          final ExtraParams params) {

        if (src.getRemainingPelletCoordinates().isEmpty()) return 0f;

        if (distCache == null) distCache = new HashMap<>();

        Coordinate pac = src.getPacmanCoordinate();

        if (!distCache.containsKey(pac)) {
            distCache.put(pac, bfsFrom(pac, game));
        }

        Map<Coordinate, Integer> pacDists = distCache.get(pac);

        int best = Integer.MAX_VALUE;
        for (Coordinate pellet : src.getRemainingPelletCoordinates()) {
            Integer d = pacDists.get(pellet);
            if (d != null) best = Math.min(best, d);
        }

        return (best == Integer.MAX_VALUE) ? 0f : best;
    }

    @Override
    public Path<PelletVertex> graphSearch(final GameView game) {

        PelletVertex start = new PelletVertex(game);

        // Build cache ONCE
        buildDistanceCache(game, start);

        PriorityQueue<Path<PelletVertex>> frontier = new PriorityQueue<>(
                Comparator.comparingDouble(p -> p.getTrueCost() +
                        getHeuristic(p.getDestination(), game, null)));

        Map<PelletVertex, Float> bestCost = new HashMap<>();

        frontier.add(new Path<>(start));
        bestCost.put(start, 0f);

        while (!frontier.isEmpty()) {

            Path<PelletVertex> currentPath = frontier.poll();
            PelletVertex current = currentPath.getDestination();
            float g = currentPath.getTrueCost();

            if (current.getRemainingPelletCoordinates().isEmpty()) {
                return currentPath;
            }

            if (g > bestCost.getOrDefault(current, Float.MAX_VALUE)) {
                continue;
            }

            for (PelletVertex neighbor : getOutgoingNeighbors(current, game, null)) {

                float edge = getEdgeWeight(current, neighbor, null);
                float newCost = g + edge;

                if (newCost < bestCost.getOrDefault(neighbor, Float.MAX_VALUE)) {

                    bestCost.put(neighbor, newCost);
                    frontier.add(
                            new Path<>(neighbor, edge, currentPath));
                }
            }
        }

        return null;
    }
}