package za.co.entelect.challenge;

import javafx.geometry.Pos;
import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;
import java.util.stream.Collectors;

public class Bot2 {

    private Random random;
    private GameState gameState;
    private Opponent opponents[];
    private MyWorm currentWorm;

    public Bot2(Random random, GameState gameState)
    {
        this.random = random;
        this.gameState = gameState;
        this.opponents = gameState.opponents;
        this.currentWorm = getCurrentWorm(gameState);
    }

    public Command run()
    {

    }

    /* Contains all possible moves */
    /* Responsible for choosing current best action */
    class MoveSet {
        /*
        * POSSIBLE MOVES    Weight                              Description
        * MoveTo            5                                   Towards center or powerup (nearest)
        * MoveToPowerUp     25                                  Towards powerup
        * Evade             2 * 8 (+40 if lethal)               Always away from firing line
        * Shoot             2 * 8 (+1 per enemy health delta)   Always targeted at closest enemy
        * Dig               7                                   Always when MoveTo is obstructed
        * Banana Bomb       17                                  Always towards enemy, can invoke select
        * Snowball          17                                  Always towards enemy, can invoke select
        * DoNothing         0                                   Selam
        * */

        public MoveSet()
        {

        }

    }

    /* Contains all possible actions and their values */
    /* Responsible for construction commands */
    class ActionPlanner {

        // Weights of actions are stored in weights attribute
        private int weights[];

        // Action parameters are stored in <command>Strat attribute
        public Position moveStrat;
        public Position evadeStrat;
        public Direction shootStrat;
        public Position digStrat;
        public Position bananaBombStrat;
        public Position snowBallStrat;

        // On construct, construct all action data
        public ActionPlanner()
        {
            constructMoveTo();
            constructEvade();
            constructShoot();
            constructDig();
            constructBananaBomb();
            constructSnowBall();
        }

        public int[] getWeights()
        {
            return this.weights;
        }

        // construct action data
        public Position constructMoveTo()
        {

        }

        public Position constructEvade()
        {

        }

        public Direction constructShoot()
        {

        }

        public Position constructDig()
        {

        }

        public Position constructBananaBomb()
        {

        }

        public Position constructSnowBall()
        {

        }

    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

}


