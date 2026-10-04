package fr.tropimon.catchpreview.release;

final class ReleaseRules {
    static final int PC_RADIUS = 5;
    private ReleaseRules() {}
    static boolean canReleaseParty(int count) { return count > 1; }
    static boolean withinPcRange(int dx, int dy, int dz) {
        return Math.abs((long) dx) <= PC_RADIUS && Math.abs((long) dy) <= 3 && Math.abs((long) dz) <= PC_RADIUS;
    }
}
