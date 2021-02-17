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
        MoveSet moveset = new MoveSet();
        return moveset.getCommand();
    }

    /* Contains all possible moves */
    /* Responsible for choosing current best action */
    private class MoveSet {
        private Command command;

        /*
        * POSSIBLE MOVES    Weight                              Description
        * MoveTo*           5                                   Towards center or powerup (nearest)
        * MoveToPowerUp     25                                  Towards powerup
        * Evade             2 * 8 - 1 (+40 if lethal)           Always away from firing line
        * Shoot             2 * 8 (+1 per enemy health delta)   Always targeted at closest enemy
        * Dig               7                                   Always when MoveTo is obstructed
        * Banana Bomb*      14 * n                              Always towards enemy, can invoke select, ignores line of sight
        * Snowball*         17                                  Always towards enemy, can invoke select, needs line of sight
        * DoNothing         0                                   Chill
        *
        * MoveTo will try to maintain a distance of 3-2 from center point.
        * n is number of enemies, 14 is average expected damage
        * Snowball is given a LOS restriction so that at least one worm is guaranteed to be able to follow up with a shot
        * invalid moves have weight < 0
        * */

        public MoveSet()
        {
            ActionPlanner strategy = new ActionPlanner();
            selectCommand(strategy);
        }

        public Command getCommand()
        {
            return command;
        }

        private void selectCommand(ActionPlanner strategy)
        {
            int commandWeights[] = strategy.getWeights();
            int commandID = indexOfMax(commandWeights);

            if (commandWeights[commandID] < 0)
            {
                command = new DoNothingCommand();
                return;
            }

            Position target = strategy.getCommandParams(commandID);
            Direction aim = strategy.getShootParams();
            switch(commandID)
            {
                case 0: // MoveTo
                case 1: // MoveToPowerUp
                case 2: // Evade
                    command = new MoveCommand(target.x, target.y);
                    break;
                case 3: // Shoot
                    command = new ShootCommand(aim);
                    break;
                case 4: // Dig
                    command = new DigCommand(target.x, target.y);
                    break;
                case 5: // BananaBomb
                    if (currentWorm.id != 2)
                    {
                        command = new SelectCommand(2, 5,target.x, target.y);
                    } else {
                        command = new BananaBombCommand(target.x, target.y);
                    }
                    break;
                case 6: // Snowball Command
                    if (currentWorm.id != 2)
                    {
                        command = new SelectCommand(2, 6, target.x, target.y);
                    } else {
                        command = new SnowballCommand(target.x, target.y);
                    }
                    break;
                default: // Logical error somewhere, fix later
                    command = new DoNothingCommand();
                    break;
            }
            return;
        }

    }

    /* Contains all possible actions and their values */
    /* Responsible for construction commands */
    class ActionPlanner {

        /*ACTION            ID
        * MoveTo            0
        * MoveToPowerUp     1
        * Evade             2
        * Shoot             3
        * Dig               4
        * BananaBomb        5
        * SnowBall          6
        * */

        // Weights of actions are stored in weights attribute
        private int weights[];

        // Action parameters are stored in commandParams and shootParams
        private Position commandParams[];
        private Direction shootParams;


        // On construct, construct all action data
        public ActionPlanner()
        {
            this.weights = new int[7];
            this.commandParams = new Position[7];
            constructMoveTo();
            constructMoveToPowerUp();
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

        public Position getCommandParams(int id)
        {
            return commandParams[id];
        }

        public Direction getShootParams()
        {
            return shootParams;
        }


        // construct action data
        public Position constructMoveTo()
        {

        }

        public Position constructMoveToPowerUp()
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

    private int indexOfMax(int[] arr)
    {
        int max  = 0;
        for (int i = 0; i < arr.length; i++)
        {
            if (arr[i] > arr[max])
            {
                max = i;
            }
        }

        return max;
    }
}


