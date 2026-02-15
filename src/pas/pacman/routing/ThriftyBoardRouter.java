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

import java.util.ArrayList;
import java.util.HashSet;       // will need for bfs
import java.util.Queue;         // will need for bfs
import java.util.LinkedList;    // will need for bfs
import java.util.Set;

// This class is responsible for calculating routes between two Coordinates on the Map.
// Use this in your PacmanAgent to calculate routes that (if followed) will lead
// Pacman from some Coordinate to some other Coordinate on the map.
public class ThriftyBoardRouter
    extends BoardRouter
{

    // If you want to encode other information you think is useful for Coordinate routing
    // besides Coordinates and data available in GameView you can do so here.
    public static class BoardExtraParams
        extends ExtraParams
    {

    }

    // feel free to add other fields here!

    public ThriftyBoardRouter(int myUnitId,
                              int pacmanId,
                              int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);

        // if you add fields don't forget to initialize them here!
    }


    @Override
    public Collection<Coordinate> getOutgoingNeighbors(final Coordinate src,
                                                       final GameView game,
                                                       final ExtraParams params)
    {
        final Collection<Coordinate> neighbors = new ArrayList<>(4);
        for (final Action a : Action.values()) {
            if (game.isLegalPacmanMove(src,a)) {
                neighbors.add(a.apply(src));
            }
        }
        return neighbors;
    }

    @Override
    public Path<Coordinate> graphSearch(final Coordinate src,
                                        final Coordinate tgt,
                                        final GameView game)
    {
        if (src.equals(tgt)) {
            return new Path<>(src);
        }

        final Queue<Path<Coordinate>> q = new LinkedList<>();
        final Set<Coordinate> visited = new HashSet<>();

        q.add(new Path<>(src));
        visited.add(src);

        while (!q.isEmpty()) {
            final Path<Coordinate> curPath = q.remove();
            final Coordinate cur = curPath.getDestination();

            if (cur.equals(tgt)) {
                return curPath;
            }

            for (final Coordinate nbr : getOutgoingNeighbors(cur, game, new BoardExtraParams())) {
                if (visited.add(nbr)) {
                    q.add(new Path<>(nbr, 1.0f, curPath));
                }
            }
        }
        return null;
    }

}

