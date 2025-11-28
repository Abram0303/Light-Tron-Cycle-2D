package ch.heigvd.utils;

public enum Direction {
    UP, DOWN, LEFT, RIGHT;

    public static Direction fromString(String s) {
        return Direction.valueOf(s.toUpperCase()); // Retourne la direction correspondante à la chaîne
    }

    @Override
    public String toString() {
        return name();
    }
}