package za.co.entelect.challenge;

import javafx.geometry.Pos;
import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;
    private int currentStrat;


    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
        if (gameState.currentRound <= 10){
            this.currentStrat = 2; // All worms goes to the middle of the map in early rounds.
        }
        else {
            this.currentStrat = 2; // Hunt enemies
        }
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {

        Worm enemyWorm = getFirstWormInRange();

        //Combat
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            if (currentWorm.id == 3 && currentWorm.snowballs.count > 0 && enemyWorm.roundsUntilUnfrozen == 0) {
                currentWorm.snowballs.count -= 1;
                return new SnowballCommand(enemyWorm.position.x, enemyWorm.position.y);
            }
            else if (currentWorm.id == 2 && currentWorm.bananaBombs.count > 0){
                currentWorm.bananaBombs.count -= 1;
                return new BananaBombCommand(enemyWorm.position.x, enemyWorm.position.y);
            }
            return new ShootCommand(direction);
        }

        //Movement
        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);


        if (getTargetPos(currentStrat).x != -999) {
            Cell block = ClosestCellIdxtoTarget(currentStrat, surroundingBlocks, getTargetPos(currentStrat));
            if (block.type == CellType.AIR) {
                return new MoveCommand(block.x, block.y);
            } else if (block.type == CellType.DIRT) {
                return new DigCommand(block.x, block.y);
            }
        }
        else {
            int cellIdx = random.nextInt(surroundingBlocks.size());
            Cell block = surroundingBlocks.get(cellIdx);

            if (block.type == CellType.AIR) {
                return new MoveCommand(block.x, block.y);
            } else if (block.type == CellType.DIRT) {
                return new DigCommand(block.x, block.y);
            }
        }

        return new DoNothingCommand();

    }

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition)) {
                return enemyWorm;
            }
        }

        return null;
    }

    private Position getTargetPos(int strat) {
        Position Middle = new Position();
        Middle.x = 16;
        Middle.y = 16;
        if (strat == 1) {// Go To Mid,
//            Position Middle = new Position();
//            Middle.x = 16;
//            Middle.y = 16;
            return Middle;
        } else if (strat == 2) { // HUNT

            if (currentWorm.id == 1) { //Agent
                int AllyCommPow = currentWorm.health;

                if (AllyCommPow >= opponent.worms[0].health && opponent.worms[0].health > 0) {
                    return opponent.worms[0].position;
                } else if (AllyCommPow >= opponent.worms[1].health && opponent.worms[1].health > 0) {
                    return opponent.worms[1].position;
                } else if (AllyCommPow >= opponent.worms[2].health && opponent.worms[2].health > 0) {
                    return opponent.worms[2].position;
                }
            }

            else if (currentWorm.id == 2) { //Agent
                int AllyAgentPow = currentWorm.bananaBombs.count * 20 + currentWorm.health;

                if (AllyAgentPow >= opponent.worms[0].health && opponent.worms[0].health > 0) {
                    return opponent.worms[0].position;
                } else if (AllyAgentPow >= opponent.worms[1].health && opponent.worms[1].health > 0) {
                    return opponent.worms[1].position;
                } else if (AllyAgentPow >= opponent.worms[2].health && opponent.worms[2].health > 0) {
                    return opponent.worms[2].position;
                }
            }

            else if (currentWorm.id == 3 && opponent.worms[2].health > 0){ // Technical
                int AllyTechPow = currentWorm.snowballs.count * 25 + currentWorm.health;

                if (AllyTechPow >= opponent.worms[0].health && opponent.worms[0].health > 0) {
                    return opponent.worms[0].position;
                } else if (AllyTechPow >= opponent.worms[1].health && opponent.worms[1].health > 0) {
                    return opponent.worms[1].position;
                } else if (AllyTechPow >= opponent.worms[2].health && opponent.worms[2].health > 0) {
                    return opponent.worms[2].position;
                }
            }
        }
        Position blank = new Position();
        blank.x = -999;
        blank.y = -999;
        return blank;
    }

    private Cell ClosestCellIdxtoTarget(int strat, List<Cell> surrBlocks, Position targetPos){
        int closestIdx = 0;
        int currRange;
        int closestRange;

        for (int i = 1; i < surrBlocks.size(); i++){
            Cell Block = surrBlocks.get(i);
            Cell closestBlock = surrBlocks.get(closestIdx);
            if (strat == 1) {
                closestRange = euclideanDistance(closestBlock.x, closestBlock.y, targetPos.x, targetPos.y);
                currRange = euclideanDistance(Block.x, Block.y, targetPos.x, targetPos.y);
                if (currRange < closestRange) {
                    closestIdx = i;
                }
            }
        }
        return surrBlocks.get(closestIdx);
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }
}
