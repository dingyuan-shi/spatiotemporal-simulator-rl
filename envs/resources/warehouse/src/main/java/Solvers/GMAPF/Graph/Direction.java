package Solvers.GMAPF.Graph;

public enum Direction {
    ROW, COL;
    public static Direction invertDirection(Direction dir) {
        return dir == Direction.ROW?Direction.COL:Direction.ROW;
    }
}
