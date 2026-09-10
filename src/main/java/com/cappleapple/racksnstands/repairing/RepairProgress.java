package com.cappleapple.racksnstands.repairing;

/** Server-derived durability progress. A negative remaining time means no active repair rate. */
public record RepairProgress(double completion,long remainingTicks) {
    public boolean full() { return completion>=1; }
    public boolean active() { return !full()&&remainingTicks>=0; }
    public static RepairProgress of(int damage,int maximum,double credit,double rate,long elapsed) {
        if(maximum<=0) return new RepairProgress(0,-1);
        double earned=Math.max(0,Double.isFinite(credit)?credit:0);
        if(rate>0&&Double.isFinite(rate)) earned+=Math.max(0,elapsed)*rate;
        double missing=Math.max(0,damage-earned);
        double completion=Math.clamp(1-missing/maximum,0,1);
        long ticks=missing==0?0:rate>0&&Double.isFinite(rate)?(long)Math.min(Long.MAX_VALUE,Math.ceil(missing/rate-1e-8)):-1;
        return new RepairProgress(completion,ticks);
    }
}
