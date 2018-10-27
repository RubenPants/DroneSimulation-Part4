package entities;
	
public class PackageKey {

    private final int fromA;
    private final int fromG;

    public PackageKey(int x, int y) {
        this.fromA = x;
        this.fromG = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PackageKey)) return false;
        PackageKey key = (PackageKey) o;
        return fromA == key.fromA && fromG == key.fromG;
    }

    @Override
    public int hashCode() {
        int result = fromA;
        result = 31 * result + fromG;
        return result;
    }

}