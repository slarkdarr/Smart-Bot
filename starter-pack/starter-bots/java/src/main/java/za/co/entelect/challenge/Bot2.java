package za.co.entelect.challenge;

import javafx.geometry.Pos;
import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;
import za.co.entelect.challenge.enums.PowerUpType;

import java.util.*;
import java.util.stream.Collectors;

public class Bot2 {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;
    private MyPlayer player;

    public Bot2(Random random, GameState gameState)
    {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.player = gameState.myPlayer;
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

            Position inversedTarget = strategy.getCommandParams(commandID);
            Direction aim = strategy.getShootParams();
            Position target = new Position();
            target.x = inversedTarget.x;
            target.y = inversedTarget.y;

            System.out.println(String.format("Map cell [16][15] has position x=%d y=%d", gameState.map[16][15].x, gameState.map[16][15].y));
            switch(commandID)
            {
                case 0: // MoveTo
                case 1: // MoveToPowerUp
                case 2: // Evade
                    System.out.println(String.format("Doing command %d on worm %d position %d %d with target %d %d", commandID, currentWorm.id, currentWorm.position.x, currentWorm.position.y, target.x, target.y));
                    command = new MoveCommand(target.x, target.y);
                    break;
                case 3: // Shoot
                    command = new ShootCommand(aim);
                    System.out.println(String.format("Doing command %d on worm %d position %d %d with target %d %d", commandID, currentWorm.id, currentWorm.position.x, currentWorm.position.y, aim.x, aim.y));
                    break;
                case 4: // Dig
                    System.out.println(String.format("Doing command %d on worm %d position %d %d with target %d %d", commandID, currentWorm.id, currentWorm.position.x, currentWorm.position.y, target.x, target.y));
                    command = new DigCommand(target.x, target.y);
                    break;
                case 5: // BananaBomb
                    if (currentWorm.id != 2)
                    {
                        command = new SelectCommand(2, 5,target.x, target.y);
                    } else {
                        command = new BananaBombCommand(target.x, target.y);
                    }
                    System.out.println(String.format("Doing command %d on worm %d position %d %d with target %d %d", commandID, currentWorm.id, currentWorm.position.x, currentWorm.position.y, target.x, target.y));
                    break;
                case 6: // Snowball Command
                    if (currentWorm.id != 3)
                    {
                        command = new SelectCommand(3, 6, target.x, target.y);
                    } else {
                        command = new SnowballCommand(target.x, target.y);
                    }
                    System.out.println(String.format("Doing command %d on worm %d position %d %d with target %d %d", commandID, currentWorm.id, currentWorm.position.x, currentWorm.position.y, target.x, target.y));
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

            Position def = new Position();
            def.x = 16;
            def.y = 16;

            for(int i = 0; i < 7; i++)
            {
                this.weights[i] = -1;
                this.commandParams[i] = def;
            }

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
            } // May hang around in center for a bit

            Direction d = getDirectionAToB(position.x, position.y, dest.x, dest.y);
//            Cell movetoCell = gameState.map[position.x+d.x][position.y+d.y];
            Cell movetoCell = gameState.map[position.y+d.y][position.x+d.x];

            // Rotate until unoccupied cell is found
            if (movetoCell.occupier != null)
            {
                d = rotateCCW(d);
//                movetoCell = gameState.map[position.x+d.x][position.y+d.y];
                movetoCell = gameState.map[position.y+d.y][position.x+d.x];
            }

            System.out.println(String.format("Moving to direction %d %d", d.x, d.y));

//            while (movetoCell.occupier.playerId == gameState.myPlayer.id) // If occupied by enemy, evade or shoot will be prioritized
//            {
//                d = rotateCCW(d);
//                movetoCell = gameState.map[position.x+d.x][position.y+d.y];
//            }

            if (movetoCell.type == CellType.AIR)
            {
                System.out.println(String.format("Found air cell %d %d near position %d %d at direction %d %d", movetoCell.x, movetoCell.y, position.x, position.y, d.x, d.y));
                commandParams[0].x = movetoCell.x;
                commandParams[0].y = movetoCell.y;
                weights[0] = 5;
                weights[4] = -1;
            } else if (movetoCell.type == CellType.DIRT)
            {
                System.out.println(String.format("Found dirt cell %d %d near position %d %d at direction %d %d", movetoCell.x, movetoCell.y, position.x, position.y, d.x, d.y));
                commandParams[0].x = movetoCell.x;
                commandParams[0].y = movetoCell.y;
                weights[0] = -1;
                weights[4] = 7;
            } else if (target == 0)
            {
                weights[0] = -1;
            }
        }

        private void constructMoveToPowerUp()
        {
//            Cell movetoCell = gameState.map[commandParams[0].x][commandParams[0].y];
            Cell movetoCell = gameState.map[commandParams[0].y][commandParams[0].x];

            if (movetoCell.powerUp != null)
            {
                if (movetoCell.powerUp.type == PowerUpType.HEALTH_PACK)
                {
                    commandParams[1].x = commandParams[0].x;
                    commandParams[1].y = commandParams[0].y;
                    weights[1] = 25;
                } else {
                    weights[1] = -1;
                }
            }
        }

        private void constructEvade()
        {
            List<Direction> threats = new ArrayList<Direction>();
            for (Worm i: opponent.worms)
            {
                if (euclideanDistance(position.x, position.y, i.position.x, i.position.y) <= 4)
                {
                    float g = getGradientAToB(position.x, position.y, i.position.x, i.position.y);
                    Direction danger = getDirectionAToB(position.x, position.y, i.position.x, i.position.y);
                    if (position.x != i.position.x && position.y != i.position.y)
                    {
                        if ((g == 1 || g == -1) && LineOfSight(position.x, position.y, danger, 4))
                        {
                            threats.add(danger);
                        }
                    } else {
                        if (LineOfSight(position.x, position.y, danger, 4))
                        {
                            threats.add(danger);
                        }
                    }
                }
            }

            int[] axis = new int[] { 1, 1, 1, 1 }; // N, NE, E, SE

            for (Direction d: threats)
            {
                if ((d == Direction.SE || d == Direction.NW) && axis[2] == 1)
                { axis[3] = 0; }

                if ((d == Direction.E || d == Direction.W) && axis[2] == 1)
                { axis[2] = 0; }

                if ((d == Direction.NE || d == Direction.SW) && axis[1] == 1)
                { axis[1] = 0; }

                if ((d == Direction.N || d == Direction.S) && axis[0] == 1)
                { axis[0] = 0; }
            }

            Direction escape = Direction.N;
            Position dangerPos;
            Cell targetEscape;
            boolean foundEscape = false;
            int a = 0;

            while (a < 4 && !foundEscape)
            {
                // Test original axis
                dangerPos = position;
                dangerPos.x += escape.x;
                dangerPos.y += escape.y;
                targetEscape = gameState.map[dangerPos.x][dangerPos.y];
                if (targetEscape.type == CellType.AIR)
                {
                    foundEscape = true;
                    commandParams[2].x = dangerPos.x;
                    commandParams[2].y = dangerPos.y;
                } else {
                    // Test inverse of original axis (too lazy to do fancy logic)
                    dangerPos = position;
                    escape = Rotate180(escape);
                    dangerPos.x += escape.x;
                    dangerPos.y += escape.y;
                    targetEscape = gameState.map[dangerPos.x][dangerPos.y];
                    if (targetEscape.type == CellType.AIR)
                    {
                        foundEscape = true;
                        commandParams[2].x = dangerPos.x;
                        commandParams[2].y = dangerPos.y;
                    } else {
                        a++;
                        escape = Rotate180(escape);
                        escape = rotateCW(escape);
                    }
                }
            }

            if (threats.size() == 0)
            {
                weights[2] = -1;
            } else if (currentWorm.health < 10)
            {
                weights[2] = 8 + 40;
                // If cornered and pinned by lava
                while (!foundEscape)
                {
                    dangerPos = position;
                    dangerPos.x += escape.x;
                    dangerPos.y += escape.y;
                    targetEscape = gameState.map[dangerPos.x][dangerPos.y];
                    if (targetEscape.type == CellType.LAVA || targetEscape.type == CellType.AIR)
                    {
                        foundEscape = true;
                        commandParams[2].x = dangerPos.x;
                        commandParams[2].y = dangerPos.y;
                    }
                    escape = rotateCW(escape);
                }
            } else {
                weights[2] = 8;
            }

        }

        private void constructShoot()
        {
            List<Worm> targets = new ArrayList<Worm>();
            int lowestHealth = 200;
            int lowestHealthIndex = 4;
            Direction lowestHealthDirection = Direction.N;

            for(int i = 0; i < 3; i++)
            {
                int distance = euclideanDistance(position.x, position.y, opponent.worms[i].position.x, opponent.worms[i].position.y);
                Direction aim = getDirectionAToB(position.x, position.y, opponent.worms[i].position.x, opponent.worms[i].position.y);

                if (distance <= 4 && LineOfSight(position, aim, opponent.worms[i].position))
                {
                    targets.add(opponent.worms[i]);
                    if (opponent.worms[i].health < lowestHealth)
                    {
                        lowestHealth = opponent.worms[i].health;
                        lowestHealthDirection = aim;
                        lowestHealthIndex = i;
                    }
                }
            }

            if (targets.size() != 0)
            {
                shootParams = lowestHealthDirection;
                if (lowestHealthIndex != 4)
                {
                    weights[3] = (opponent.worms[lowestHealthIndex].health <= 8) ? 2 * 8 + 40 : 2 * 8;
                }
            } else {
                weights[3] = -1;
            }
        }

        private void constructDig()
        {
            if (weights[4] != -1)
            {
                commandParams[4].x = commandParams[0].x;
                commandParams[4].y = commandParams[0].y;
            } else if (weights[0] == -1){
                Direction nearby = Direction.N;
//                Cell digThis = gameState.map[position.x + nearby.x][position.y + nearby.y];
                Cell digThis = gameState.map[position.y + nearby.y][position.x + nearby.x];


                while (digThis.type != CellType.DIRT)
                {
                    nearby = rotateCCW(nearby);
//                    digThis = gameState.map[position.x + nearby.x][position.y + nearby.y];
                    digThis = gameState.map[position.y + nearby.y][position.x + nearby.x];
                }

                if (digThis.type == CellType.DIRT)
                {
                    System.out.println(String.format("Found dirt cell %d %d near position %d %d at direction  ", digThis.x, digThis.y, position.x, position.y, nearby.x, nearby.y));
                    commandParams[4].x = digThis.x;
                    commandParams[4].y = digThis.y;
                    weights[4] = 7;
                }
            } else {
                weights[4] = -1;
            }
        }

        private void constructBananaBomb()
        {
            List<Worm> targets = new ArrayList<Worm>();
            int highestWeightIndex = 4;
            int highestWeight = -1;
            int tempWeight = -1;

            if (player.worms[1].bananaBombs.count <= 0 || player.worms[1].health <= 0)
            {
                weights[6] = -1;
                return;
            } else if (gameState.currentWormId != 1 && player.remainingWormsSelection <= 0)  // REMINDER: ADD CHECK FOR SELECTION TOKEN COUNT
            {
                weights[6] = -1;
                return;
            }

            for(int i = 0; i < 3; i++)
            {
                int distance = euclideanDistance(position.x, position.y, opponent.worms[i].position.x, opponent.worms[i].position.y);
                if (distance <= 5)
                {
                    targets.add(opponent.worms[i]);

                    tempWeight = 0;
                    for(Worm j: opponent.worms)
                    {
                        int temp = (3 - euclideanDistance(opponent.worms[i].position.x, opponent.worms[i].position.y, j.position.x, j.position.y)) * 7;
                        if (temp > 0) { tempWeight += temp; }
                    }

                    if (tempWeight > highestWeight && !friendInRadius(opponent.worms[i].position, 2, 1))
                    {
                        highestWeight = tempWeight;
                        highestWeightIndex = i;
                    }
                }
            }

            if (highestWeightIndex != 4)
            {
                commandParams[5].x = opponent.worms[highestWeightIndex].position.x;
                commandParams[5].y = opponent.worms[highestWeightIndex].position.y;
                weights[5] = highestWeight;
            }
        }

        private void constructSnowBall()
        {
            if (player.worms[2].snowballs.count <= 0 || player.worms[2].health <= 0)
            {
                weights[6] = -1;
                return;
            } else if (gameState.currentWormId != 2 && player.remainingWormsSelection <= 0)  // REMINDER: ADD CHECK FOR SELECTION TOKEN COUNT
            {
                weights[6] = -1;
                return;
            }

            List<Worm> targets = new ArrayList<Worm>();
            int highestWeightIndex = 4;
            int highestWeight = -1;
            int tempWeight;

            for(int i = 0; i < 3; i++)
            {
                int distance = euclideanDistance(position.x, position.y, opponent.worms[i].position.x, opponent.worms[i].position.y);
                Direction aim = getDirectionAToB(position.x, position.y, opponent.worms[i].position.x, opponent.worms[i].position.y);

                if (distance <= 5 && LineOfSight(position, aim, opponent.worms[i].position))
                {
                    targets.add(opponent.worms[i]);

                    tempWeight = 17;
                    for(Worm j: opponent.worms)
                    {
                        int temp = euclideanDistance(opponent.worms[i].position.x, opponent.worms[i].position.y, j.position.x, j.position.y);
                        if (temp <= 1) { tempWeight += temp; }
                    }

                    tempWeight = 17 + tempWeight * 8;

                    if (tempWeight > highestWeight && !friendInRadius(opponent.worms[i].position, 2, 1))
                    {
                        highestWeight = tempWeight;
                        highestWeightIndex = i;
                    }
                }
            }

            if (highestWeightIndex != 4)
            {
                commandParams[6].x = opponent.worms[highestWeightIndex].position.x;
                commandParams[6].y = opponent.worms[highestWeightIndex].position.y;
                weights[6] = highestWeight;
            }
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
        if (y1 == y2)
        {
            return 0.0f;
        } else if (x1 == x2)
        {
            return 999.0f;
        }
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
            if (arr[i] <= arr[max])
            {
                max = i;
            }
        }

        return max;
    }

    private boolean friendInRadius(Position origin, int radius, int wormExceptionID)
    {
        boolean isFriendlyFire = false;
        int c = 0;
        while(!isFriendlyFire && c < 4)
        {
            if (euclideanDistance(origin.x, origin.y , player.worms[c].position.x, player.worms[c].position.y) <= radius
                    && c != wormExceptionID)
            {
                isFriendlyFire = true;
            }
            c++;
        }

        return  isFriendlyFire;
    }


    // Range > 0
    private boolean LineOfSight(int x, int y, Direction aim, int range)
    {
        for (int i = 0; i < range; i++)
        {
            x += aim.x;
            y += aim.y;

//            if (gameState.map[x][y].type == CellType.DIRT || gameState.map[x][y].type == CellType.DEEP_SPACE)
//            {
//                return false;
//            }
            if (gameState.map[y][x].type == CellType.DIRT || gameState.map[y][x].type == CellType.DEEP_SPACE)
            {
                return false;
            }
        }

        return true;
    }

    private boolean LineOfSight(Position shooter, Direction aim, Position target)
    {
        int x = shooter.x;
        int y = shooter.y;

        while (x != target.x && y != target.y)
        {
            x += aim.x;
            y += aim.y;

            if (gameState.map[x][y].type == CellType.DIRT || gameState.map[x][y].type == CellType.DEEP_SPACE)
            {
                return false;
            }
        }

        return true;
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
        return Direction.S;
    }

    private Direction rotateCW(Direction input)
    {
        switch(input){
            case N:
                return Direction.NE;
            case NW:
                return Direction.N;
            case W:
                return Direction.NW;
            case SW:
                return Direction.W;
            case S:
                return Direction.SW;
            case SE:
                return Direction.S;
            case E:
                return Direction.SE;
            case NE:
                return Direction.E;
        }
        return Direction.N;
    }

    private Direction Rotate180(Direction input)
    {
        switch(input){
            case N:
                return Direction.S;
            case NW:
                return Direction.SE;
            case W:
                return Direction.E;
            case SW:
                return Direction.NE;
            case S:
                return Direction.N;
            case SE:
                return Direction.NW;
            case E:
                return Direction.W;
            case NE:
                return Direction.SW;
        }
        return Direction.N;
    }

}
