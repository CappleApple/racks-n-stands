package com.cappleapple.racksnstands.display.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Shared translucent repair bar; labels use a single pass with no text shadow. */
public final class RepairBar {
    private RepairBar() {}
    public static void draw(GuiGraphics graphics,float x,float y,float width,float height,float progress,Component label,float opacity,int fill,int text) {
        int left=Math.round(x),top=Math.round(y),right=Math.round(x+width),bottom=Math.round(y+height);
        graphics.fill(left,top,right,bottom,alpha(0x40344150,opacity));
        graphics.fill(left,top,Math.round(x+width*Math.clamp(progress,0,1)),bottom,alpha(fill,opacity));
        if(label!=null) {
            var font=Minecraft.getInstance().font;
            graphics.drawString(font,label,Math.round(x+(width-font.width(label))/2),Math.round(y+(height-font.lineHeight)/2),alpha(text,opacity),false);
        }
    }
    private static int alpha(int color,float opacity) {
        return color&0x00ffffff|Math.round((color>>>24)*Math.clamp(opacity,0,1))<<24;
    }
}
