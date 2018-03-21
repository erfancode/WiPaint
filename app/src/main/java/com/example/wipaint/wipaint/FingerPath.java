package com.example.wipaint.wipaint;

import android.graphics.Path;

/**
 * Created by Erfan on 21/03/2018.
 */

public class FingerPath
{
    public int color;
    public int strokeWidth;
    public Path path;

    public FingerPath(int color, int strokeWidth, Path path)
    {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
    }
}
