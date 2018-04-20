package com.example.wipaint.wipaint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Erfan on 20/04/2018.
 */

public class PaintListAdapter extends RecyclerView.Adapter<PaintListAdapter.PaintViewHolder>
{
    private final ListItemClickListener mOnClickListener;
    private ArrayList<String> fileList;
    private int numberOfPaints ;
    private static int viewHolderCount;

    public PaintListAdapter(ArrayList<String> paths,ListItemClickListener listItemClickListener)
    {
        fileList = paths;
        numberOfPaints = paths.size();
        mOnClickListener = listItemClickListener;
        viewHolderCount = 0;
    }



    // call when new iem is created
    @Override
    public PaintViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.paint_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachToParentImmediately);
        PaintViewHolder viewHolder = new PaintViewHolder(view);

        String path = Environment.getExternalStorageDirectory() +"/wipaint/";
        Bitmap bitmap = BitmapFactory.decodeFile(path + fileList.get(viewHolderCount));
        viewHolder.paintView.setImageBitmap(bitmap);
        viewHolder.name.setText(fileList.get(viewHolderCount));
        String s = fileList.get(viewHolderCount);
        char[] x = new char[10];
        x[0] = s.charAt(0);
        x[1] = s.charAt(1);
        x[2] = s.charAt(2);
        x[3] = s.charAt(3);
        x[4] = '-';
        x[5] = s.charAt(5);
        x[6] = s.charAt(6);
        x[7] = '-';
        x[8] = s.charAt(8);
        x[9] = s.charAt(9);
        viewHolder.date.setText(String.valueOf(x));
        ExifInterface exif = null;
        try
        {
            exif = new ExifInterface(path+fileList.get(viewHolderCount));
            viewHolder.gps.setText(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)+','+ exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));

        } catch (IOException e)
        {
            e.printStackTrace();
        }
        //viewHolder.gps.setText(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)+","+exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
        if(numberOfPaints <= ++viewHolderCount)
            viewHolderCount = 0;
        return viewHolder;
    }


    /*
    * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the correct
     * indices in the list for this particular position, using the "position" argument that is conveniently
     * passed into us.*/
    @Override
    public void onBindViewHolder(PaintViewHolder holder, int position)
    {
        holder.bind(position);
    }

    @Override
    public int getItemCount()
    {
        return numberOfPaints;
    }

    public interface ListItemClickListener
    {
        void onListItemClick(int clickItemIndex);
    }


    class PaintViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        ImageView paintView;
        TextView name, date, gps;
        public PaintViewHolder(View itemView)
        {
            super(itemView);
            paintView = (ImageView)itemView.findViewById(R.id.small_paint);
            name = (TextView)itemView.findViewById(R.id.paint_name);
            date = (TextView)itemView.findViewById(R.id.paint_date);
            gps = (TextView)itemView.findViewById(R.id.gps_location);
            itemView.setOnClickListener(this);
        }

        void bind(int listIndex)
        {

        }

        @Override
        public void onClick(View view)
        {
            int clickedPosition = getAdapterPosition();
            mOnClickListener.onListItemClick(clickedPosition);
        }
    }

}
