package src.pas.pacman.agents;

// SYSTEM IMPORTS
import edu.bu.pas.pacman.agents.Agent;
import edu.bu.pas.pacman.agents.SearchAgent;
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Tile;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;

import java.util.Random;
import java.util.Set;

// JAVA PROJECT IMPORTS
import src.pas.pacman.routing.ThriftyBoardRouter; // responsible for how to get somewhere
import src.pas.pacman.routing.ThriftyPelletRouter; // responsible for pellet order

public class PacmanAgentBoard
        extends SearchAgent {

    private final Random random;
    private BoardRouter boardRouter;
    private PelletRouter pelletRouter;

    public PacmanAgentBoard(int myUnitId,
            int pacmanId,
            int ghostChaseRadius) {
        super(myUnitId, pacmanId, ghostChaseRadius);
        this.random = new Random();

        this.boardRouter = new ThriftyBoardRouter(myUnitId, pacmanId, ghostChaseRadius);
        this.pelletRouter = new ThriftyPelletRouter(myUnitId, pacmanId, ghostChaseRadius);
    }

    public final Random getRandom() {
        return this.random;
    }

    public final BoardRouter getBoardRouter() {
        return this.boardRouter;
    }

    public final PelletRouter getPelletRouter() {
        return this.pelletRouter;
    }

    @Override
    public void makePlan(final GameView game) {

        Coordinate src = game.getEntity(this.getMyEntityId()).getCurrentCoordinate();

        Coordinate closestPellet = null;
        float bestDistance = Float.POSITIVE_INFINITY;

        // Scan board for pellets
        for (int x = 0; x < game.getXBoardDimension(); x++) {
            for (int y = 0; y < game.getYBoardDimension(); y++) {

                Coordinate c = new Coordinate(x, y);

                if (game.getTile(c).getState() == Tile.State.PELLET) {

                    // Manhattan distance for target selection
                    float dist = Math.abs(src.x() - x)
                            + Math.abs(src.y() - y);

                    if (dist < bestDistance) {
                        bestDistance = dist;
                        closestPellet = c;
                    }
                }
            }
        }

        // No pellets remaining
        if (closestPellet == null) {
            this.setPlanToGetToTarget(null);
            this.setTargetCoordinate(null);
            return;
        }

        this.setTargetCoordinate(closestPellet);

        Path<Coordinate> path = this.getBoardRouter().graphSearch(src, closestPellet, game);

        if (path == null) {
            this.setPlanToGetToTarget(null);
            return;
        }

        java.util.Stack<Coordinate> stack = new java.util.Stack<>();

        // Convert reversed Path into forward stack
        Path<Coordinate> current = path;

        while (current.getParentPath() != null) {
            stack.push(current.getDestination());
            current = current.getParentPath();
        }

        this.setPlanToGetToTarget(stack);
    }

    @Override
    public Action makeMove(final GameView game) {

        // If no plan exists or plan is finished → create one
        if (this.getPlanToGetToTarget() == null
                || this.getPlanToGetToTarget().isEmpty()) {

            this.makePlan(game);
        }

        // If still no plan, do nothing safe
        if (this.getPlanToGetToTarget() == null
                || this.getPlanToGetToTarget().isEmpty()) {

            return Action.UP; // fallback safe move
        }

        Coordinate current = game.getEntity(this.getMyEntityId()).getCurrentCoordinate();

        Coordinate next = this.getPlanToGetToTarget().pop();

        try {
            return Action.inferFromCoordinates(current, next);
        } catch (Exception e) {
            e.printStackTrace();
            return Action.UP;
        }
    }

    @Override
    public void afterGameEnds(final GameView game) {
        // if you want to log stuff after a game ends implement me!
    }
}
