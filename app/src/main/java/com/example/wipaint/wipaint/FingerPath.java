package com.example.wipaint.wipaint;

import android.graphics.Path;

import java.io.Serializable;

/**
 * Created by Erfan on 21/03/2018.
 */

public class FingerPath implements Serializable
{
    public int color;
    public int strokeWidth;
    public transient Path path;

    public FingerPath(int color, int strokeWidth, Path path)
    {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
    }
}
