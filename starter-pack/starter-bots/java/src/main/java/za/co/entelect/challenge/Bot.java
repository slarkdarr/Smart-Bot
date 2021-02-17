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
        if (gameState.currentRound <= 40){
            this.currentStrat = 1; // All worms goes to the middle of the map in early rounds.
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
        Worm closeEnemyWorm = getFirstObstructedWormInRange(); // Tanpa peduli DIRT sebagai penghalang
        //Combat
        if (closeEnemyWorm != null){
            if (currentWorm.id == 3 && currentWorm.snowballs.count > 0) {
                List<List<Worm>> ListTwoTeamsMembersInVicinity = getTeammatesInRadius(closeEnemyWorm.position, currentWorm.snowballs.freezeRadius);
                if (currentWorm.snowballs.count > 0 && ListTwoTeamsMembersInVicinity.get(0).size() == 0) {
                    if ((currentWorm.health <= 50 && closeEnemyWorm.roundsUntilUnfrozen == 0) || ListTwoTeamsMembersInVicinity.get(1).size() > 1 ) {
                        currentWorm.snowballs.count -= 1;
                        return new SnowballCommand(closeEnemyWorm.position.x, closeEnemyWorm.position.y);
                    }
                }
            }

            else if (currentWorm.id == 2 && currentWorm.bananaBombs.count > 0) {
                List<List<Worm>> ListTwoTeamsMembersInVicinity = getTeammatesInRadius(closeEnemyWorm.position, currentWorm.bananaBombs.damageRadius);
                if (currentWorm.bananaBombs.count > 0 && ListTwoTeamsMembersInVicinity.get(0).size() == 0) {
                    if (currentWorm.health <= 50 || ListTwoTeamsMembersInVicinity.get(1).size() > 1 ) {
                        currentWorm.bananaBombs.count -= 1;
                        return new BananaBombCommand(closeEnemyWorm.position.x, closeEnemyWorm.position.y);
                    }
                }
            }
        }
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }

        //Movement
        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);


        if (getTargetPos(currentStrat).x != -999) {
            Cell block = ClosestCelltoTarget(surroundingBlocks, getTargetPos(currentStrat));
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
            if (cells.contains(enemyPosition) && enemyWorm.health > 0) {
                return enemyWorm;
            }
        }

        return null;
    }
    private Worm getFirstObstructedWormInRange() {

        Set<String> cells = constructObstructedDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health > 0) {
                return enemyWorm;
            }
        }

        return null;
    }

    private List<List<Worm>> getTeammatesInRadius (Position centerArea, int radius) {
        List<Worm> ListAlliesInRadius = new ArrayList<Worm>();
        List<Worm> ListEnemiesInRadius = new ArrayList<Worm>();
        List<List<Worm>> ListTeamsMembersInRadius = new ArrayList<List<Worm>>();
        for (int i = 0; i < 3; i ++) {
            Worm Ally = gameState.myPlayer.worms[i];
            if (Ally.health > 0 && euclideanDistance(Ally.position.x, Ally.position.y, centerArea.x, centerArea.y) < radius) {
                ListAlliesInRadius.add(Ally);
            }
            Worm Enemy = opponent.worms[i];
            if (Enemy.health > 0 && euclideanDistance(Enemy.position.x, Enemy.position.y, centerArea.x, centerArea.y) < radius) {
                if (Enemy.roundsUntilUnfrozen == 0) {
                    ListEnemiesInRadius.add(Enemy);
                }
                // Tambahkan kalkulasi damage/dirt untuk banana bomb Agent
                else if (currentWorm.id == 2) {
                    ListEnemiesInRadius.add(Enemy);
                }
            }

        }
        ListTeamsMembersInRadius.add(ListAlliesInRadius);
        ListTeamsMembersInRadius.add(ListEnemiesInRadius);
        return  ListTeamsMembersInRadius;
    }



    private Position getTargetPos(int strat) {
        Position Middle = new Position();
        Middle.x = 16;
        Middle.y = 16;

        Worm EnemyCommando = new Worm();
        EnemyCommando.position = opponent.worms[0].position;
        EnemyCommando.health = opponent.worms[0].health;
        Worm EnemyAgent = new Worm();
        EnemyAgent.position = opponent.worms[1].position;
        EnemyAgent.health = opponent.worms[1].health;
        Worm EnemyTechnical = new Worm();
        EnemyTechnical.position = opponent.worms[2].position;
        EnemyTechnical.health = opponent.worms[2].health;

        if (strat == 1) {// Go To Mid,
//            Position Middle = new Position();
//            Middle.x = 16;
//            Middle.y = 16;
            return Middle;

        } else if (strat == 2) { // HUNT
            int x;
            int y;
            if (currentWorm.id == 1) { //Agent
                int AllyCommPow = currentWorm.health;

                if (AllyCommPow >= EnemyCommando.health && EnemyCommando.health > 0) {
                    return EnemyCommando.position;
                } else if (AllyCommPow >= EnemyAgent.health && EnemyAgent.health > 0) {
                    return EnemyAgent.position;
                } else if (AllyCommPow >= EnemyTechnical.health && EnemyTechnical.health > 0) {
                    return EnemyTechnical.position;
                }
            }

            else if (currentWorm.id == 2) { //Agent
                int AllyAgentPow = currentWorm.bananaBombs.count * 20 + currentWorm.health;

                if (AllyAgentPow >= EnemyCommando.health && EnemyCommando.health > 0) {
                    return EnemyCommando.position;
                } else if (AllyAgentPow >= EnemyAgent.health && EnemyAgent.health > 0) {
                    return EnemyAgent.position;
                } else if (AllyAgentPow >= EnemyTechnical.health && EnemyTechnical.health > 0) {
                    return EnemyTechnical.position;
                }
            }

            else if (currentWorm.id == 3 && EnemyTechnical.health > 0){ // Technical
                int AllyTechPow = currentWorm.snowballs.count * 25 + currentWorm.health;

                if (AllyTechPow >= EnemyCommando.health && EnemyCommando.health > 0) {
                    return EnemyCommando.position;
                } else if (AllyTechPow >= EnemyAgent.health && EnemyAgent.health > 0) {
                    return EnemyAgent.position;
                } else if (AllyTechPow >= EnemyTechnical.health && EnemyTechnical.health > 0) {
                    return EnemyTechnical.position;
                }
            }
        }
        Position blank = new Position();
        blank.x = -999;
        blank.y = -999;
        return blank;
    }

    private Cell ClosestCelltoTarget(List<Cell> surrBlocks, Position targetPos){
        int closestIdx = 0;
        int currRange;
        int closestRange;
        for (int i = 1; i < surrBlocks.size(); i++){
            Cell Block = surrBlocks.get(i);
            closestRange = euclideanDistance(surrBlocks.get(closestIdx).x, surrBlocks.get(closestIdx).y, targetPos.x, targetPos.y);
            currRange = euclideanDistance(surrBlocks.get(i).x, surrBlocks.get(i).y, targetPos.x, targetPos.y);
            if (currRange < closestRange) {
                closestIdx = i;
            }
        }
        return surrBlocks.get(closestIdx);
    }

    private List<List<Cell>> constructObstructedDirectionLines(int range) {
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
                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
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
