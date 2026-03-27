package com.example.myapplication;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;

public class TibetanFontHelper {
    
    private static Typeface tibetanTypeface;
    
    public static Typeface getTibetanTypeface(Context context) {
        if (tibetanTypeface == null) {
            tibetanTypeface = ResourcesCompat.getFont(context, R.font.sunshine_uchen);
        }
        return tibetanTypeface;
    }
    
    public static void applyTibetanFont(Context context, TextView textView) {
        if (textView != null) {
            textView.setTypeface(getTibetanTypeface(context));
        }
    }
    
    public static void applyTibetanFontToViewGroup(Context context, ViewGroup viewGroup) {
        if (viewGroup == null) {
            return;
        }
        
        Typeface typeface = getTibetanTypeface(context);
        applyFontToViewGroup(viewGroup, typeface);
    }
    
    private static void applyFontToViewGroup(ViewGroup viewGroup, Typeface typeface) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            
            if (child instanceof TextView) {
                ((TextView) child).setTypeface(typeface);
            } else if (child instanceof ViewGroup) {
                applyFontToViewGroup((ViewGroup) child, typeface);
            }
        }
    }
    
    public static void applyTibetanFontToView(Context context, View view) {
        if (view == null) {
            return;
        }
        
        Typeface typeface = getTibetanTypeface(context);
        
        if (view instanceof TextView) {
            ((TextView) view).setTypeface(typeface);
        } else if (view instanceof ViewGroup) {
            applyFontToViewGroup((ViewGroup) view, typeface);
        }
    }
}
