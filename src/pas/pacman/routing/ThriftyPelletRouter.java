package src.pas.pacman.routing;

import java.util.*;

import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.game.Tile;

public class ThriftyPelletRouter extends PelletRouter {

    public static class PelletExtraParams extends ExtraParams {
    }

    private GameView currentGame;

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

    @Override
    public float getEdgeWeight(final PelletVertex src,
                               final PelletVertex dst,
                               final ExtraParams params) {

        Coordinate from = src.getPacmanCoordinate();
        Coordinate to   = dst.getPacmanCoordinate();

        return shortestPathDistance(from, to);
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game,
                              final ExtraParams params) {

        if (src.getRemainingPelletCoordinates().isEmpty()) {
            return 0f;
        }

        Coordinate pac = src.getPacmanCoordinate();
        float minDist = Float.MAX_VALUE;

        for (Coordinate pellet : src.getRemainingPelletCoordinates()) {
            float d = shortestPathDistance(pac, pellet);
            minDist = Math.min(minDist, d);
        }

        return minDist;
    }

    private Collection<Coordinate> getNeighbors(Coordinate c) {

        List<Coordinate> neighbors = new ArrayList<>();

        int[][] dirs = { {1,0}, {-1,0}, {0,1}, {0,-1} };

        for (int[] d : dirs) {
            Coordinate next = new Coordinate(c.x() + d[0], c.y() + d[1]);

            if (next.x() >= 0 &&
                next.y() >= 0 &&
                next.x() < currentGame.getXBoardDimension() &&
                next.y() < currentGame.getYBoardDimension() &&
                currentGame.getTile(next).getState() != Tile.State.WALL) {

                neighbors.add(next);
            }
        }

        return neighbors;
    }

    private int shortestPathDistance(Coordinate start,
                                     Coordinate goal) {

        if (start.equals(goal)) {
            return 0;
        }

        Queue<Coordinate> queue = new LinkedList<>();
        Map<Coordinate, Integer> dist = new HashMap<>();

        queue.add(start);
        dist.put(start, 0);

        while (!queue.isEmpty()) {

            Coordinate cur = queue.poll();
            int curDist = dist.get(cur);

            for (Coordinate neighbor : getNeighbors(cur)) {

                if (!dist.containsKey(neighbor)) {

                    if (neighbor.equals(goal)) {
                        return curDist + 1;
                    }

                    dist.put(neighbor, curDist + 1);
                    queue.add(neighbor);
                }
            }
        }

        return Integer.MAX_VALUE;
    }

    @Override
    public Path<PelletVertex> graphSearch(final GameView game) {

        this.currentGame = game;

        PelletVertex start = new PelletVertex(game);

        PriorityQueue<Path<PelletVertex>> frontier =
            new PriorityQueue<>(
                Comparator.comparingDouble(p ->
                    p.getTrueCost() +
                    getHeuristic(p.getDestination(), game, null)
                )
            );

        Map<PelletVertex, Float> bestCost = new HashMap<>();

        frontier.add(new Path<>(start));
        bestCost.put(start, 0f);

        while (!frontier.isEmpty()) {

            Path<PelletVertex> currentPath = frontier.poll();
            PelletVertex current = currentPath.getDestination();
            float currentCost = currentPath.getTrueCost();

            if (current.getRemainingPelletCoordinates().isEmpty()) {
                return currentPath;
            }

            if (bestCost.containsKey(current) &&
                currentCost > bestCost.get(current)) {
                continue;
            }

            for (PelletVertex neighbor :
                 getOutgoingNeighbors(current, game, null)) {

                float edgeCost =
                    shortestPathDistance(
                        current.getPacmanCoordinate(),
                        neighbor.getPacmanCoordinate()
                    );

                float newCost = currentCost + edgeCost;

                if (!bestCost.containsKey(neighbor) ||
                    newCost < bestCost.get(neighbor)) {

                    bestCost.put(neighbor, newCost);

                    Path<PelletVertex> newPath =
                        new Path<>(neighbor, edgeCost, currentPath);

                    frontier.add(newPath);
                }
            }
        }

        return null;
    }
}