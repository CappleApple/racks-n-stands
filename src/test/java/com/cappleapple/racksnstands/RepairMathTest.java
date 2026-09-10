package com.cappleapple.racksnstands;
import com.cappleapple.racksnstands.repairing.RepairMath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
class RepairMathTest {
    @ParameterizedTest @CsvSource({"1000,1000,1,1,900","1000,1000,5,1,500","1000,1000,10,1,0","25,1000,1,1,0","900,1000,1,3,600","50,100,1,5,0","10,11,1,1,9","1,1,1,1,0","50,100,0,1,50","50,100,1,0,50","0,100,10,10,0","100,100,10,1000000,0"})
    void levelsRoundingAndIntervals(int damage,int max,int level,long intervals,int expected) { assertEquals(expected,RepairMath.repair(damage,max,.1,level,intervals)); }
    @Test void saturationAndInvalidNumbers() {
        assertEquals(0,RepairMath.repair(Integer.MAX_VALUE,Integer.MAX_VALUE,100,255,Long.MAX_VALUE));
        assertEquals(20,RepairMath.repair(20,100,Double.NaN,1,1));
        assertEquals(20,RepairMath.repair(20,0,.1,1,1));
        assertEquals(Long.MAX_VALUE,RepairMath.add(Long.MAX_VALUE-1,10));
    }
    @Test void worldClockRollbackAndInitialization() {
        assertEquals(0,RepairMath.elapsed(100,-1));assertEquals(0,RepairMath.elapsed(100,101));assertEquals(72000,RepairMath.elapsed(72100,100));
    }
}
