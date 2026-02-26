package src.pas.pacman.agents;


// SYSTEM IMPORTS
import edu.bu.pas.pacman.agents.Agent;
import edu.bu.pas.pacman.agents.SearchAgent;
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;

import java.util.Random;
import java.util.Set;
import java.util.Stack;
import edu.bu.pas.pacman.game.entity.Entity;
import edu.bu.pas.pacman.game.Tile;


// JAVA PROJECT IMPORTS
import src.pas.pacman.routing.ThriftyBoardRouter;  // responsible for how to get somewhere
import src.pas.pacman.routing.ThriftyPelletRouter; // responsible for pellet order


public class PacmanAgent
    extends SearchAgent
{

    private final Random random;
    private BoardRouter  boardRouter;
    private PelletRouter pelletRouter;

    public PacmanAgent(int myUnitId,
                       int pacmanId,
                       int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);
        this.random = new Random();

        this.boardRouter = new ThriftyBoardRouter(myUnitId, pacmanId, ghostChaseRadius);
        this.pelletRouter = new ThriftyPelletRouter(myUnitId, pacmanId, ghostChaseRadius);
    }

    public final Random getRandom() { return this.random; }
    public final BoardRouter getBoardRouter() { return this.boardRouter; }
    public final PelletRouter getPelletRouter() { return this.pelletRouter; }

    @Override
    public void makePlan(final GameView game)
    {
        final Coordinate tgt = this.getTargetCoordinate();
        if (tgt == null) {
            this.setPlanToGetToTarget(null);
            return;
        }

        final Entity pac = game.getEntity(this.getMyEntityId());
        final Coordinate src = pac.getCurrentCoordinate();

        final Path<Coordinate> path =
            this.getBoardRouter().graphSearch(src, tgt, game);

        if (path == null) {
            this.setPlanToGetToTarget(null);
            return;
        }

        final Stack<Coordinate> plan = new Stack<>();
        for (Path<Coordinate> p = path; p != null; p = p.getParentPath()) {
            plan.push(p.getDestination());
        }

        if (!plan.isEmpty() && plan.peek().equals(src)) {
            plan.pop();
        }

        this.setPlanToGetToTarget(plan);
    }

    @Override
    public Action makeMove(final GameView game)
    {
        final Entity pac = game.getEntity(this.getMyEntityId());
        final Coordinate cur = pac.getCurrentCoordinate();

        Stack<Coordinate> plan = this.getPlanToGetToTarget();

        if (this.getTargetCoordinate() == null || plan == null || plan.isEmpty())
        {
            Coordinate bestPellet = null;
            int bestDist = Integer.MAX_VALUE;

            for (int x = 0; x < game.getXBoardDimension(); x++) {
                for (int y = 0; y < game.getYBoardDimension(); y++) {
                    final Coordinate c = new Coordinate(x, y);
                    if (game.getTile(c).getState() == Tile.State.PELLET) {
                        final int d = Math.abs(x - cur.x()) + Math.abs(y - cur.y());
                        if (d < bestDist) {
                            bestDist = d;
                            bestPellet = c;
                        }
                    }
                }
            }

            if (bestPellet == null) {
                for (Action a : Action.values()) {
                    if (game.isLegalPacmanMove(cur, a)) return a;
                }
                return Action.UP;
            }

            this.setTargetCoordinate(bestPellet);
            this.makePlan(game);
            plan = this.getPlanToGetToTarget();

            if (plan == null || plan.isEmpty()) {
                for (Action a : Action.values()) {
                    if (game.isLegalPacmanMove(cur, a)) return a;
                }
                return Action.UP;
            }
        }

        final Coordinate next = plan.pop();
        this.setPlanToGetToTarget(plan);

        try {
            final Action a = Action.inferFromCoordinates(cur, next);
            if (game.isLegalPacmanMove(cur, a)) return a;
        } catch (Exception ignored) { }

        for (Action a : Action.values()) {
            if (game.isLegalPacmanMove(cur, a)) return a;
        }
        return Action.UP;

    }

    @Override
    public void afterGameEnds(final GameView game)
    {
        // if you want to log stuff after a game ends implement me!
    }
}
