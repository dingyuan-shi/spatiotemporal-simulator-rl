package Solvers.GMAPF.Graph;

public class Location {

    private int row;
    private int col;

    public Location(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public Location(int row, int col, boolean isConvert) {
        this.row = isConvert?col:row;
        this.col = isConvert?row:col;
    }

    public int getRow() {
        return row;
    }
    public void setRow(int row) {
        this.row = row;
    }
    public int getCol() {
        return col;
    }
    public void setCol(int col) {
        this.col = col;
    }

    public int getCol(boolean isConvert) {
        return isConvert?this.row:this.col;
    }

    public int getRow(boolean isConvert) {
        return isConvert?this.col:this.row;
    }

    public static long crossMultiplication(Location a, Location b) {
        return a.row * b.col - a.col * b.row;
    }

    public static Location sub(Location a, Location b) {
        return new Location(a.row - b.row, a.col - b.col);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return row == location.getRow() && col == location.getCol();
    }

    public int manhattanDistance(Location location) {
        return Math.abs(this.col - location.getCol()) + Math.abs(this.row - location.getRow());
    }
}
