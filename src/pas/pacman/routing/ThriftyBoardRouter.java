package src.pas.pacman.routing;

// SYSTEM IMPORTS
import java.util.Collection;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pacman.agents.Agent;
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.game.Tile;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.BoardRouter.ExtraParams;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;

// This class is responsible for calculating routes between two Coordinates on the Map.
// Use this in your PacmanAgent to calculate routes that (if followed) will lead
// Pacman from some Coordinate to some other Coordinate on the map.
public class ThriftyBoardRouter
        extends BoardRouter {

    // If you want to encode other information you think is useful for Coordinate
    // routing
    // besides Coordinates and data available in GameView you can do so here.
    public static class BoardExtraParams
            extends ExtraParams {

    }

    // feel free to add other fields here!

    public ThriftyBoardRouter(int myUnitId,
            int pacmanId,
            int ghostChaseRadius) {
        super(myUnitId, pacmanId, ghostChaseRadius);

        // if you add fields don't forget to initialize them here!
    }

    @Override
    public Collection<Coordinate> getOutgoingNeighbors(final Coordinate src,
            final GameView game,
            final ExtraParams params) {

        Collection<Coordinate> neighbors = new java.util.ArrayList<>(4);

        for (Action action : Action.values()) {
            if (game.isLegalPacmanMove(src, action)) {
                neighbors.add(action.apply(src));
            }
        }
        return neighbors;
    }

    @Override
    public Path<Coordinate> graphSearch(final Coordinate src,
            final Coordinate tgt,
            final GameView game) {

        java.util.PriorityQueue<Path<Coordinate>> frontier = new java.util.PriorityQueue<>(
                java.util.Comparator.comparing(Path::getTrueCost));

        java.util.Set<Coordinate> visited = new java.util.HashSet<>();

        Path<Coordinate> startPath = new Path<>(src);

        frontier.add(startPath);

        while (!frontier.isEmpty()) {

            Path<Coordinate> currentPath = frontier.poll();
            Coordinate current = currentPath.getDestination();

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);

            if (current.equals(tgt)) {
                return currentPath;
            }

            for (Coordinate neighbor : getOutgoingNeighbors(current, game, null)) {

                if (!visited.contains(neighbor)) {
                    Path<Coordinate> newPath = new Path<>(neighbor, 1.0f, currentPath);

                    frontier.add(newPath);
                }
            }
        }
        return null;
    }

}
