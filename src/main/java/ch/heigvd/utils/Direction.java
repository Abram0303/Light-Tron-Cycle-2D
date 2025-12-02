package ch.heigvd.utils;

public enum Direction {
    UP, DOWN, LEFT, RIGHT;

    public static Direction fromString(String s) {
        // Retourne la direction correspondante à la chaîne de caractère
        return Direction.valueOf(s.toUpperCase());
    }

    @Override
    public String toString() {
        return name();
    }
}