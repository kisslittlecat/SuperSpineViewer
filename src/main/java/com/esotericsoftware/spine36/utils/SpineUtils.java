package com.esotericsoftware.spine36.utils;

public class SpineUtils {
    static public final float PI = 3.1415927f;
    static public final float PI2 = PI * 2;
    static public final float radiansToDegrees = 180f / PI;
    static public final float radDeg = radiansToDegrees;
    static public final float degreesToRadians = PI / 180;
    static public final float degRad = degreesToRadians;

    public static float cosDeg(float angle) {
        return (float) Math.cos(angle * degRad);
    }

    public static float sinDeg(float angle) {
        return (float) Math.sin(angle * degRad);
    }

    public static float cos(float angle) {
        return (float) Math.cos(angle);
    }

    public static float sin(float angle) {
        return (float) Math.sin(angle);
    }

    public static float atan2(float y, float x) {
        return (float) Math.atan2(y, x);
    }
}