package src.pas.pacman.routing;

import java.util.*;

import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;

public class ThriftyPelletRouter extends PelletRouter {

    public static class PelletExtraParams extends ExtraParams { }

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

    // Euclidean distance instead of Manhattan
    private float euclidean(Coordinate a, Coordinate b) {
        float dx = a.x() - b.x();
        float dy = a.y() - b.y();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public float getEdgeWeight(final PelletVertex src,
                               final PelletVertex dst,
                               final ExtraParams params) {

        return euclidean(
                src.getPacmanCoordinate(),
                dst.getPacmanCoordinate());
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game,
                              final ExtraParams params) {

        if (src.getRemainingPelletCoordinates().isEmpty()) {
            return 0f;
        }

        Coordinate pac = src.getPacmanCoordinate();
        float min = Float.MAX_VALUE;

        for (Coordinate pellet : src.getRemainingPelletCoordinates()) {
            min = Math.min(min, euclidean(pac, pellet));
        }

        return min;
    }

    @Override
    public Path<PelletVertex> graphSearch(final GameView game) {

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
            float g = currentPath.getTrueCost();

            if (current.getRemainingPelletCoordinates().isEmpty()) {
                return currentPath;
            }

            if (g > bestCost.getOrDefault(current, Float.MAX_VALUE)) {
                continue;
            }

            for (PelletVertex neighbor :
                 getOutgoingNeighbors(current, game, null)) {

                float edge = getEdgeWeight(current, neighbor, null);
                float newCost = g + edge;

                if (newCost <
                    bestCost.getOrDefault(neighbor, Float.MAX_VALUE)) {

                    bestCost.put(neighbor, newCost);
                    frontier.add(new Path<>(neighbor, edge, currentPath));
                }
            }
        }

        return null;
    }
}