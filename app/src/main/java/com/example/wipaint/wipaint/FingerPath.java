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
    public boolean blur, emboss;

    public FingerPath(int color, int strokeWidth, Path path, boolean blur, boolean emboss)
    {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
        this.blur = blur;
        this.emboss = emboss;
    }
}
