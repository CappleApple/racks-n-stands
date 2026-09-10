package com.cappleapple.racksnstands;

import com.cappleapple.racksnstands.repairing.RepairSoundProgress;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RepairSoundProgressTest {
    @Test void onePercentRequiresAnEntirePercentOfActualRepair() {
        var below=RepairSoundProgress.advance(0,15,1561,1);
        assertFalse(below.soundDue());
        var crossed=RepairSoundProgress.advance(below.percent(),1,1561,1);
        assertTrue(crossed.soundDue());assertEquals(1600.0/1561-1,crossed.percent(),1e-9);
    }
    @Test void updatePartitionPreservesMilestonesAndRemainder() {
        double balance=0;int sounds=0;
        for(int n=0;n<100;n++) {
            var next=RepairSoundProgress.advance(balance,1,1000,1);
            balance=next.percent();if(next.soundDue()) sounds++;
        }
        assertEquals(10,sounds);assertEquals(0,balance,1e-9);
        var catchUp=RepairSoundProgress.advance(0,100,1000,1);
        assertTrue(catchUp.soundDue());assertEquals(balance,catchUp.percent(),1e-9);
    }
    @Test void configuredThresholdAndSmallItemsCoalesceLargeGains() {
        assertFalse(RepairSoundProgress.advance(0,49,1000,5).soundDue());
        assertTrue(RepairSoundProgress.advance(4.9,1,1000,5).soundDue());
        assertEquals(new RepairSoundProgress(0,true),RepairSoundProgress.advance(0,1,1,1));
        assertEquals(new RepairSoundProgress(.5,true),RepairSoundProgress.advance(.5,100,1000,2));
    }
}
