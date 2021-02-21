package za.co.entelect.challenge;

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
    private int remainingSelect;
    private List<Position> powerUpsLocs;
    private int currentStrat;
    private int snowballRadius;
    private int snowballRange;
    private int snowballCount;
    private int bananaBombRadius;
    private int bananaBombRange;
    private int bananaBombCount;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
        this.remainingSelect = gameState.myPlayer.remainingWormsSelection;
        this.powerUpsLocs = getPowerUpLocations(gameState);
        this.snowballRadius = getSnowBallRadius(gameState);
        this.snowballRange = getSnowballRange(gameState);
        this.snowballCount = getSnowballCount(gameState);
        this.bananaBombRadius = getBananaBombRadius(gameState);
        this.bananaBombRange = getBananaBombRange(gameState);
        this.bananaBombCount = getBananaBombCount(gameState);
        if (gameState.currentRound <= 10){
            this.currentStrat = 1; // All worms goes to the middle of the map in early rounds.
        }
        else {
            this.currentStrat = 2; // Hunt enemies
        }
    }

    // Getters
    private List<Position> getPowerUpLocations(GameState gameState){
        List<Position> powUpsLocs = new ArrayList<>();
        for (int i = 0; i < gameState.mapSize; i++){
            for (int j = 0; j < gameState.mapSize; j++){
                if (gameState.map[j][i].powerUp != null){
                    Position powUpLoc = new Position();
                    powUpLoc.x = i;
                    powUpLoc.y = j;
                    powUpsLocs.add(powUpLoc);
                }
            }
        }
        return powUpsLocs;
    }

    private int getShootRange(GameState gameState, int WormID){
        return gameState.myPlayer.worms[WormID-1].weapon.range;
    }
    private int getSnowballRange(GameState gameState){
        return gameState.myPlayer.worms[2].snowballs.range;
    }
    private int getSnowBallRadius(GameState gameState){
        return gameState.myPlayer.worms[2].snowballs.freezeRadius;
    }

    private int getSnowballCount(GameState gameState){
        return gameState.myPlayer.worms[2].snowballs.count;
    }

    private int getBananaBombRange(GameState gameState){
        return gameState.myPlayer.worms[1].bananaBombs.range;
    }

    private int getBananaBombRadius(GameState gameState){
        return gameState.myPlayer.worms[1].bananaBombs.damageRadius;
    }

    private int getBananaBombCount(GameState gameState) {
        return gameState.myPlayer.worms[1].bananaBombs.count;
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {

        Worm enemyWorm = getFirstWormInRange();


        //Check close Power Ups
        int ClosestWormIDtoPowerUp = IDofClosestAllyWormtoPowerUp();
        if (ClosestWormIDtoPowerUp != -1){
            if (getTargetPos(3).x != -999) {
                MyWorm closestWormToPowerUp = gameState.myPlayer.worms[ClosestWormIDtoPowerUp-1];
                List<Cell> surroundingBlocks = getSurroundingCells(closestWormToPowerUp.position.x, closestWormToPowerUp.position.y);
                Position TargetPos = getTargetPos(3);
                Cell block = ClosestCelltoTarget(surroundingBlocks, TargetPos);
                if (block.type == CellType.AIR && currentWorm == closestWormToPowerUp ) {
                    return new MoveCommand(block.x, block.y);
                } else if (block.type == CellType.DIRT && currentWorm == closestWormToPowerUp) {
                    return new DigCommand(block.x, block.y);
                }
            }
        }


        //Check snowball
        MyWorm AllyTech = gameState.myPlayer.worms[2];
        if (AllyTech.health > 0 && snowballCount > 0) {
            Position SBTarget = IdealThrowTarget(3);
            if (SnowballScore(SBTarget) > 0) {
                if (currentWorm.id == 3) {
                    return new SnowballCommand(SBTarget.x, SBTarget.y);

                }
                else if (gameState.myPlayer.remainingWormsSelection > 0){
                    return new SelectCommand(3, 6, SBTarget.x, SBTarget.y);
                }
            }
        }
        //Combat
        MyWorm AllyAgent = gameState.myPlayer.worms[1];
        if (AllyAgent.health > 0 && bananaBombCount > 0) {
            Position SBTarget = IdealThrowTarget(2);
            if (BananaBombScore(SBTarget) > 0) {
                if (currentWorm.id == 2) {
                    return new BananaBombCommand(SBTarget.x, SBTarget.y);
                } else if (gameState.myPlayer.remainingWormsSelection > 0){
                    return new SelectCommand(2, 5, SBTarget.x, SBTarget.y);
                }
            }
        }
        //PEW PEW!

        if (enemyWorm != null && isNoDirtAndAllyInBetweenCardinalAandB(currentWorm.position, enemyWorm.position)) {
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

    // getFirstObstructedWormInRange() digunakan untuk mencari Worm musuh pertama tanpa mempertimbangkan DIRT
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

    // Mengambil semua worm radius dengan titik pusat centerArea. Mengembalikan anggota dari tiap tim yang ada di radius.
    private List<List<Worm>> getTeammatesInRadius (Position centerArea, int radius, int ID) { // ID == 2 atau 3, Agent atau Tech

        //Array anggota worm MyPlayer di area
        List<Worm> ListAlliesInRadius = new ArrayList<Worm>();
        //Array anggota worm Opponent di area
        List<Worm> ListEnemiesInRadius = new ArrayList<Worm>();
        //Array of array worm, untuk menampung array anggota team setiap regu.
        List<List<Worm>> ListTeamsMembersInRadius = new ArrayList<List<Worm>>();
        for (int i = 0; i < 3; i ++) {
            Worm Ally = gameState.myPlayer.worms[i];
            if (Ally.health > 0 && euclideanDistance(Ally.position.x, Ally.position.y, centerArea.x, centerArea.y) <= radius) {
                ListAlliesInRadius.add(Ally);
            }
            Worm Enemy = opponent.worms[i];
            if (Enemy.health > 0 && euclideanDistance(Enemy.position.x, Enemy.position.y, centerArea.x, centerArea.y) <= radius) {
                // Kalau tidak frozen, worm musuh tersebut dipertimbangkan
                if (ID == 3 && Enemy.roundsUntilUnfrozen == 0 && AllyInShootLine(Enemy.position)) {
                    ListEnemiesInRadius.add(Enemy);
                }
                // Agent throw
                else if (ID == 2) {
                    ListEnemiesInRadius.add(Enemy);
                }
            }

        }
        // Masukkan
        ListTeamsMembersInRadius.add(ListAlliesInRadius); // indeks 0
        ListTeamsMembersInRadius.add(ListEnemiesInRadius); // indeks 1
        return  ListTeamsMembersInRadius;
    }


    // Cari posisi target berdasarkan strategi.
    private Position getTargetPos(int strat) {
        if (strat == 3){
            for (int i = 0; i < 3; i++){
                MyWorm allyWorm = gameState.myPlayer.worms[i];
                Position allyWormPos = allyWorm.position;
                Position closestPowUp = ClosestPowerUp(powerUpsLocs, allyWormPos);
                int distancetoPowUp= euclideanDistance(allyWormPos.x, allyWormPos.y, closestPowUp.x, closestPowUp.y);
                if (distancetoPowUp < 8){
                    return closestPowUp;
                }
            }
        }

        else if (strat == 1) {// Go To Mid,
            Position Middle = new Position();
            Middle.x = 16;
            Middle.y = 16;
            return Middle;

        } else if (strat == 2) { // HUNT
            Worm EnemyCommando = new Worm();
            EnemyCommando.position = opponent.worms[0].position;
            EnemyCommando.health = opponent.worms[0].health;
            Worm EnemyAgent = new Worm();
            EnemyAgent.position = opponent.worms[1].position;
            EnemyAgent.health = opponent.worms[1].health;
            Worm EnemyTechnical = new Worm();
            EnemyTechnical.position = opponent.worms[2].position;
            EnemyTechnical.health = opponent.worms[2].health;
            int x;
            int y;

            if (currentWorm.id == 1) { //Agent
                int AllyCommPow = currentWorm.health;

                if (AllyCommPow >= EnemyAgent.health && EnemyAgent.health > 0) {
                    return EnemyAgent.position;
                } else if (AllyCommPow >= EnemyTechnical.health && EnemyTechnical.health > 0) {
                    return EnemyTechnical.position;
                } else if (AllyCommPow >= EnemyCommando.health && EnemyCommando.health > 0) {
                    return EnemyCommando.position;
                }
            }

            else if (currentWorm.id == 2) { //Agent
                int AllyAgentPow = 20 + currentWorm.health;
                if (AllyAgentPow >= EnemyAgent.health && EnemyAgent.health > 0) {
                    return EnemyAgent.position;
                } else if (AllyAgentPow >= EnemyTechnical.health && EnemyTechnical.health > 0) {
                    return EnemyTechnical.position;
                } else if (AllyAgentPow >= EnemyCommando.health && EnemyCommando.health > 0) {
                    return EnemyCommando.position;
                }
            }
            else if (currentWorm.id == 3){ // Technical
                int AllyTechPow = currentWorm.snowballs.count * 10 + currentWorm.health;
                if (AllyTechPow >= EnemyAgent.health && EnemyAgent.health > 0) {
                    return EnemyAgent.position;
                } else if (AllyTechPow >= EnemyTechnical.health && EnemyTechnical.health > 0) {
                    return EnemyTechnical.position;
                }
                else if (AllyTechPow >= EnemyCommando.health && EnemyCommando.health > 0) {
                    return EnemyCommando.position;
                }
            }

        }

        // Strategi spesifik worm
        Position blank = new Position();
        blank.x = -999;
        blank.y = -999;
        return blank;
    }

    // Mencari posisi target snowball yang paling efektif.
    private Position IdealThrowTarget(int WormID){
        //ASUMSI WormID selalu 2 atau 3
        MyWorm AllyTech = gameState.myPlayer.worms[WormID-1];
        Position AllyTechPos = AllyTech.position;
        Position BestScorePos = new Position();

        int BestScore = -999;
        int range;
        int currentScore;

        if (WormID == 2){
            range = bananaBombRange;

        }
        else { // Worm ID == 3
            range = snowballRange;
        }

        for (int i = -range; i <= range; i++ ){
            for (int j = -range; j <= range; j++ ) {
                if (i != 0 || j != 0) {
                    Position currentCellPos = new Position();
                    currentCellPos.x = AllyTechPos.x + i;
                    currentCellPos.y = AllyTechPos.y + j;

                    if (euclideanDistance(AllyTechPos.x, AllyTechPos.y, currentCellPos.x, currentCellPos.y) <= range) {
                        if (WormID == 2) {
                            currentScore = BananaBombScore(currentCellPos);
                        }
                        else {
                            currentScore = SnowballScore(currentCellPos);
                        }
                        if (BestScore < currentScore) {
                            BestScore = currentScore;
                            BestScorePos.x = currentCellPos.x;
                            BestScorePos.y = currentCellPos.y;
                        }
                    }
                }
            }
        }
        return BestScorePos;
    }
    private int DirtInRadius(Position centralArea, int radius){
        int DirtCounter = 0;
        for (int i = -radius; i <= radius; i++ ) {
            for (int j = -radius; j <= radius; j++) {
                if (euclideanDistance(centralArea.x, centralArea.y, centralArea.x+1, centralArea.y+1) <= radius) {
                    if (gameState.map[centralArea.y+j][centralArea.x+i].type == CellType.DIRT) {
                        DirtCounter++;
                    }
                }
            }
        }
        return DirtCounter;
    }
    // Pengukur score yang didapat dari melempar banana bomb.
    private int BananaBombScore(Position centralArea){
        List<List<Worm>> ListTeamWorms = getTeammatesInRadius(centralArea, bananaBombRadius, 2);
        int nAllies = ListTeamWorms.get(0).size();
        int nEnemies = ListTeamWorms.get(1).size();
        if (nEnemies > 0) {
            float totalDmg = 0;
            for (int i = 0; i < ListTeamWorms.get(1).size(); i++) {
                Position enemyPos = ListTeamWorms.get(1).get(i).position;
                int getDistFromCenter = euclideanDistance(centralArea.x,centralArea.y,enemyPos.x, enemyPos.y);
                int bananaFullDmg = gameState.myPlayer.worms[1].bananaBombs.damage;
                //Damage dealt
                totalDmg += (bananaFullDmg*(bananaBombRadius+1-getDistFromCenter))/(bananaBombRadius+1);
            }
            if (totalDmg >= 20) { //20 = Damage min
                return 14 * nEnemies - 14 * nAllies + DirtInRadius(centralArea, bananaBombRadius) * 7;
            }
        }
        return -999;
    }

    // Pengukur score yang didapat dari melempar snowball.
    private int SnowballScore(Position centralArea) {
        List<List<Worm>> ListTeamWorms = getTeammatesInRadius(centralArea, snowballRadius, 3);
        int nAllies = ListTeamWorms.get(0).size();
        int nEnemies = ListTeamWorms.get(1).size();
        if (nEnemies > 0) {
            for (int i = 0; i < ListTeamWorms.get(1).size(); i++) {
                if (AllyInShootLine(ListTeamWorms.get(1).get(i).position)) {
                    return 17 * nEnemies - 17 * nAllies;
                }
            }
        }
        return 0;
    }

    // Pencari cell tujuan move worm untuk mencapai targetPos
    private Cell ClosestCelltoTarget(List<Cell> surrBlocks, Position targetPos){
        int closestIdx = 0;
        int currRange;
        int closestRange;
        for (int i = 0; i < surrBlocks.size(); i++){
            closestRange = euclideanDistance(surrBlocks.get(closestIdx).x, surrBlocks.get(closestIdx).y, targetPos.x, targetPos.y);
            currRange = euclideanDistance(surrBlocks.get(i).x, surrBlocks.get(i).y, targetPos.x, targetPos.y);
            if (currRange < closestRange) {
                closestIdx = i;
            }
            if (surrBlocks.get(i).x == targetPos.x && surrBlocks.get(i).y == targetPos.y) {
                return surrBlocks.get(i);
            }
        }
        return surrBlocks.get(closestIdx);
    }

    //Seperti constructFireDirectionLines, dengan perbedaan menghiraukan DIRT
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


    private Position ClosestPowerUp(List<Position> powerUpsLocs, Position wormPos){
        Position closestPos = new Position();
        closestPos.x = 999;
        closestPos.y = 999;
        int closestRange;
        int currentRange;
        for (int i = 0; i < powerUpsLocs.size(); i++){
            closestRange = euclideanDistance(wormPos.x, wormPos.y, closestPos.x, closestPos.y);
            currentRange = euclideanDistance(wormPos.x, wormPos.y, powerUpsLocs.get(i).x, powerUpsLocs.get(i).y);
            if (closestRange > currentRange){
                closestPos.x = powerUpsLocs.get(i).x;
                closestPos.y = powerUpsLocs.get(i).y;
            }
        }
        return closestPos;
    }
    private int IDofClosestAllyWormtoPowerUp(){
        int currentClosestWormId = -1;
        int currentRange = 100;
        for (int i = 0; i < 3; i++){
            MyWorm AllyWorm = gameState.myPlayer.worms[i];
            if (AllyWorm.health>0) {
                Position AllyWormPos = AllyWorm.position;
                Position closestPowerUptoAlly = ClosestPowerUp(powerUpsLocs, AllyWormPos); // Out of Map Coordinate if none left
                int closestPoweruptoAllyDist = euclideanDistance(AllyWormPos.x, AllyWormPos.y, closestPowerUptoAlly.x, closestPowerUptoAlly.y);
                if (closestPoweruptoAllyDist < currentRange && closestPoweruptoAllyDist < 12) { // Never trigger if none left
                    currentRange = closestPoweruptoAllyDist;
                    currentClosestWormId = i + 1;
                }
            }
        }
        return currentClosestWormId;
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
                if ((i != x || j != y) && isValidCoordinate(i, j)) {
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

    //Kondisi tambahan snowball agar setidaknya ada 1 ally yang siap menyerang target snowball
    private boolean AllyInShootLine(Position enemyWormPos){
        for (int i = 0; i<3; i++){
            Worm allyWorm = gameState.myPlayer.worms[i];
            if(allyWorm.health > 0) {
                Position allyWormPos = allyWorm.position;
                int allyWormShootRange = getShootRange(gameState, i + 1);
                if (allyWormShootRange >= euclideanDistance(allyWormPos.x, allyWormPos.y, enemyWormPos.x, enemyWormPos.y)) {
                    if (isOnCardinalLineAtoB(allyWormPos, enemyWormPos)) {
                        if (isNoDirtAndAllyInBetweenCardinalAandB(allyWormPos, enemyWormPos)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    private float getGradientAtoB(int x1, int y1, int x2, int y2)
    {
        if (x2 - x1 == 0) {
            if (y2 > y1) {
                return 9999;
            }
            else if (y2 < y1){
                return -9999;
            }
        }
        return (y2 - y1)/(x2 - x1);
    }
    private boolean isOnCardinalLineAtoB(Position a, Position b){
        float grad = getGradientAtoB(a.x,a.y,b.x,b.y);
        if (grad == 1.0 || grad == -1.0 || grad == -9999.0 || grad == 9999.0 || grad == 0.0){
            return true;
        }
        else {return false;}

    }

    private boolean isNoDirtAndAllyInBetweenCardinalAandB(Position a, Position b){
        //ASUMSI POS A DAN POS BERADA DALAM CARDINAL DIRECTION SATU SAMA LAIN

        float[] gradComp = gradLineComponent(a,b);

        if (isOnCardinalLineAtoB(a,b)){
            if (gradComp[0] == 0){
                if (gradComp[1] > 0) {
                    for (int j = 1; j <= Math.abs(gradComp[0]); j++) {
                        Cell currentCell = gameState.map[a.y + j][a.x];
                        if (currentCell.type == CellType.DIRT || isAllyOnCell(currentCell)) {
                            return false;
                        }
                    }
                }
                else if (gradComp[1] < 0) {
                    for (int j = 1; j <= Math.abs(gradComp[0]); j++) {
                        Cell currentCell = gameState.map[a.y - j][a.x];
                        if (currentCell.type == CellType.DIRT || isAllyOnCell(currentCell)) {
                            return false;
                        }
                    }
                }
            }
            else if (gradComp[0] > 0){
                if (gradComp[1] > 0) {
                    for (int i = 1; i <= Math.abs(gradComp[0]); i++) {
                        Cell currentCell = gameState.map[a.y + i][a.x + i];
                        if (currentCell.type == CellType.DIRT || isAllyOnCell(currentCell)) {
                            return false;
                        }
                    }
                }
                else if (gradComp[1] < 0) {
                    for (int i = 1; i <= Math.abs(gradComp[0]); i++) {
                        Cell currentCell = gameState.map[a.y - i][a.x + i];
                        if (currentCell.type == CellType.DIRT || isAllyOnCell(currentCell)) {
                            return false;
                        }
                    }
                }
                else {
                    for (int i = 1; i <= Math.abs(gradComp[0]); i++) {
                        Cell currentCell = gameState.map[a.y][a.x + i];
                        if (currentCell.type == CellType.DIRT || isAllyOnCell(currentCell)) {
                            return false;
                        }
                    }
                }
            }
            else if (gradComp[0] < 0){
                if (gradComp[1] > 0) {
                    for (int i = 1; i <= Math.abs(gradComp[0]); i++) {
                        Cell currentCell = gameState.map[a.y + i][a.x + i];
                        if (currentCell.type == CellType.DIRT || isAllyOnCell(currentCell)) {
                            return false;
                        }
                    }
                }
                else if (gradComp[1] < 0) {
                    for (int i = 1; i <= Math.abs(gradComp[0]); i++) {
                        Cell currentCell = gameState.map[a.y - i][a.x - i];
                        if (currentCell.type == CellType.DIRT || isAllyOnCell(currentCell)) {
                            return false;
                        }
                    }
                }
                else {
                    for (int i = 1; i <= Math.abs(gradComp[0]); i++) {
                        Cell currentCell = gameState.map[a.y][a.x - i];
                        if (currentCell.type == CellType.DIRT || isAllyOnCell(currentCell)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    private boolean isAllyOnCell(Cell cell){
        for (int i = 0; i < 3; i++){
            MyWorm allyWorm = gameState.myPlayer.worms[i];
            if (allyWorm.health > 0 && allyWorm.position.x == cell.x && allyWorm.position.y == cell.y){
                return true;
            }
        }
        return false;
    }
    private float[] gradLineComponent(Position a, Position b){
        float xComp = b.x-a.x;
        float yComp = b.y-a.y;
        float[] gradComp = {xComp,yComp} ;
        return gradComp;
    }

}
