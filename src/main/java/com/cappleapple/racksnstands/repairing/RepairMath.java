package com.cappleapple.racksnstands.repairing;

/** Pure durability arithmetic shared by fixtures and third-party Repairing hosts. */
public final class RepairMath {
    private RepairMath() {}
    public record Accumulation(int damage,double fraction) {}
    public static double rate(int maximum,double percent,int level,long calculationTicks) {
        return maximum<=0||!Double.isFinite(percent)||percent<=0||level<=0||calculationTicks<=0?0:(maximum*percent*level)/calculationTicks;
    }
    public static Accumulation advance(int damage,double fraction,double rate,long elapsed) {
        if(damage<=0) return new Accumulation(0,0);
        if(!Double.isFinite(fraction)||fraction<0) fraction=0;
        if(!Double.isFinite(rate)||rate<0) rate=0;
        double earned=fraction+Math.max(0,elapsed)*rate;
        if(earned+1e-9>=damage) return new Accumulation(0,0);
        int whole=(int)Math.floor(earned+1e-9);
        return new Accumulation(damage-whole,Math.max(0,earned-whole));
    }
    /** Retained for external callers needing the old discrete calculation; fixtures use advance. */
    public static int repair(int damage, int maximum, double percent, int level, long intervals) {
        if (damage <= 0 || maximum <= 0 || percent <= 0 || !Double.isFinite(percent) || level <= 0 || intervals <= 0) return Math.max(0, damage);
        // Round down once per interval, with one durability minimum; saturate before integer conversion.
        double perInterval = Math.max(1, Math.floor(maximum * percent * level + 1.0e-9));
        double amount = perInterval * intervals;
        return amount >= damage ? 0 : damage - (int)amount;
    }
    public static long elapsed(long now, long previous) { return previous < 0 || now <= previous ? 0 : now - previous; }
    public static long add(long a, long b) { return b > Long.MAX_VALUE - a ? Long.MAX_VALUE : a + b; }
}
