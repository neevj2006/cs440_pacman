package src.pas.pacman.routing;

import java.util.*;

import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;

public class ThriftyPelletRouter extends PelletRouter {

    public static class PelletExtraParams extends ExtraParams {
    }

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

        // Manhattan distance
        return Math.abs(from.x() - to.x())
             + Math.abs(from.y() - to.y());
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
            float d = Math.abs(pac.x() - pellet.x())
                    + Math.abs(pac.y() - pellet.y());
            minDist = Math.min(minDist, d);
        }

        return minDist;
    }
    
    @Override
    public Path<PelletVertex> graphSearch(final GameView game) {

        PelletVertex start = new PelletVertex(game);

        PriorityQueue<Path<PelletVertex>> frontier =
            new PriorityQueue<>(
                Comparator.comparing(p ->
                    p.getTrueCost() +
                    getHeuristic(p.getDestination(), game, null)
                )
            );

        Set<PelletVertex> visited = new HashSet<>();

        frontier.add(new Path<>(start));

        while (!frontier.isEmpty()) {

            Path<PelletVertex> currentPath = frontier.poll();
            PelletVertex current = currentPath.getDestination();

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);

            // Goal = no pellets left
            if (current.getRemainingPelletCoordinates().isEmpty()) {
                return currentPath;
            }

            for (PelletVertex neighbor :
                 getOutgoingNeighbors(current, game, null)) {

                if (!visited.contains(neighbor)) {

                    float cost =
                        getEdgeWeight(current, neighbor, null);

                    Path<PelletVertex> newPath =
                        new Path<>(neighbor, cost, currentPath);

                    frontier.add(newPath);
                }
            }
        }

        return null;
    }
}