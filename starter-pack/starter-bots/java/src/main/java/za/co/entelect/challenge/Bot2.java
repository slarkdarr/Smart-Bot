package za.co.entelect.challenge;

import javafx.geometry.Pos;
import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;
import za.co.entelect.challenge.enums.PowerUpType;

import java.util.*;
import java.util.stream.Collectors;
import java.lang.system;

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
        private ActionPlanner strategy;

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
            strategy = new ActionPlanner();
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
                    if (currentWorm.id != 3)
                    {
                        command = new SelectCommand(3, 6, target.x, target.y);
                    } else {
                        command = new SnowballCommand(target.x, target.y);
                    }
                    break;
                default: // Logical error somewhere, fix later
                    command = new DoNothingCommand();
                    System.out.console.log("Logical error in class: MoveSet");
                    break;
            }
            return;
        }

    }

    /* Contains all possible actions and their values */
    /* Responsible for construction commands */
    class ActionPlanner {

        /*POI               ID
        * Center            0
        * PowerUp 1         1
        * PowerUp 2         2
        * */
        private int distancesToPOI[] = new int[3];

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

        private Position position;

        // On construct, construct all action data
        public ActionPlanner()
        {
            this.weights = new int[7];
            this.commandParams = new Position[7];

            this.position = currentWorm.position;

            this.distancesToPOI[0] = euclideanDistance(position.x, position.y, 16, 16);
            this.distancesToPOI[1] = euclideanDistance(position.x, position.y, 14, 15);
            this.distancesToPOI[2] = euclideanDistance(position.x, position.y, 15, 14);

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
        private void constructMoveTo()
        {
            int target = indexOfMin(distancesToPOI);
            Position dest = new Position();
            switch(target)
            {
                case 0:
                    dest.x = 16; dest.y = 16;
                    break;
                case 1:
                    dest.x = 14; dest.y = 15;
                    break;
                case 2:
                    dest.x = 15; dest.y = 14;
                    break;
            }

            Direction d = getDirectionAToB(dest.x, dest.y, position.x, position.y);
            Cell movetoCell = gameState.map[position.x+d.x][position.y+d.y];

            // Rotate until unoccupied cell is found
            while (movetoCell.occupier.playerId == gameState.myPlayer.id) // If occupied by enemy, evade or shoot will be prioritized
            {
                d = rotateCCW(d);
                movetoCell = gameState.map[position.x+d.x][position.y+d.y];
            }

            if (movetoCell.type == CellType.AIR)
            {
                commandParams[0].x = movetoCell.x;
                commandParams[0].y = movetoCell.y;
                weights[0] = 5;
                weights[4] = -1;
            } else if (movetoCell.type == CellType.DIRT)
            {
                weights[4] = 7;
            }
        }

        private void constructMoveToPowerUp()
        {
            Cell movetoCell = gameState.map[commandParams[0].x][commandParams[0].y];

            if (movetoCell.powerUp.type == PowerUpType.HEALTH_PACK)
            {
                commandParams[1].x = commandParams[0].x;
                commandParams[1].y = commandParams[0].y;
                weights[1] = 25;
            } else {
                weights[1] = -1;
            }
        }


        private void constructEvade()
        {

        }

        private void constructShoot()
        {

        }

        private void constructDig()
        {

        }

        private void constructBananaBomb()
        {

        }

        private void constructSnowBall()
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

    private Direction getDirectionAToB(int xA, int yA, int xB, int yB)
    {
        if (xA == xB && yA != yB)
        {
            if (yA < yB)    { return Direction.N; }
            else            { return Direction.S; }
        } else
        if (xA != xB && yA == yB)
        {
            if (xA < xB)    { return Direction.W; }
            else            { return Direction.E; }
        } else {
            float gDir = getGradientAToB(xA, yA, xB, yB);
            Direction result;

            if (gDir > 0) { result = (xA < xB) ? Direction.SE : Direction.NW; } else
            if (gDir < 0) { result = (xA < xB) ? Direction.NE : Direction.SW; }
        }

        return Direction.S;
    }

    private float getGradientAToB(int x1, int y1, int x2, int y2)
    {
        return (y2 - y1) / (x2 - x1);
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

    private int indexOfMin(int[] arr)
    {
        int max  = 0;
        for (int i = 0; i < arr.length; i++)
        {
            if (arr[i] < arr[max])
            {
                max = i;
            }
        }

        return max;
    }

    private Direction rotateCCW(Direction input)
    {
        switch(input){
            case N:
                return Direction.NW;
            case NW:
                return Direction.W;
            case W:
                return Direction.SW;
            case SW:
                return Direction.S;
            case S:
                return Direction.SE;
            case SE:
                return Direction.E;
            case E:
                return Direction.NE;
            case NE:
                return Direction.N;
        }
    }

}


