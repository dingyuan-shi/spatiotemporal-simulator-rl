package SimulatorCore;


public class Location {
    private int r, c;

    public Location(int r, int c) {
        this.r = r;
        this.c = c;
    }

    public int getC() {
        return c;
    }

    public int getR() {
        return r;
    }

    public void setC(int c) {
        this.c = c;
    }

    public void setR(int r) {
        this.r = r;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return r == location.r && c == location.c;
    }

    public int manhattanDistance(Location location) {
        return Math.abs(this.c - location.getC()) + Math.abs(this.r - location.getR());
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", r, c);
    }
}
