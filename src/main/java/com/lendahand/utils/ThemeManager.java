package com.lendahand.utils;

/**
 * Simple singleton that tracks dark/light mode.
 * Toggle with ThemeManager.toggle() — SceneManager picks it up on next scene load.
 */
public class ThemeManager {

    private static boolean dark = false;

    public static boolean isDark()  { return dark; }
    public static void    toggle()  { dark = !dark; }
    public static void    setDark(boolean d) { dark = d; }
}