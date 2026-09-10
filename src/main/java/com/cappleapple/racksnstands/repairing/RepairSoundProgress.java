package com.cappleapple.racksnstands.repairing;

/** Sound milestones count actual durability restored, independently for each stored item. */
public record RepairSoundProgress(double percent,boolean soundDue) {
    public static RepairSoundProgress advance(double previous,int restored,int maximum,double interval) {
        if(!Double.isFinite(previous)||previous<0) previous=0;
        if(restored<=0||maximum<=0) return new RepairSoundProgress(previous,false);
        if(!Double.isFinite(interval)||interval<=0) return new RepairSoundProgress(0,false);
        double earned=previous+restored*100.0/maximum;
        long milestones=(long)Math.floor((earned+1e-9)/interval);
        return new RepairSoundProgress(Math.max(0,earned-milestones*interval),milestones>0);
    }
}
