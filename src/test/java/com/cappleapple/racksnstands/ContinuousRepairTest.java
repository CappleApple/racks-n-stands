package com.cappleapple.racksnstands;

import com.cappleapple.racksnstands.repairing.*;
import com.cappleapple.racksnstands.config.RepairConfigMigration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ContinuousRepairTest {
    @Test void updateCadenceDoesNotChangeTheHourlyRate() {
        double rate=RepairMath.rate(1561,.2,1,72000);
        for(int step:new int[]{1,7,20,100,1200}) {
            var state=new RepairMath.Accumulation(1500,0);
            for(int tick=0;tick<72000;tick+=step) state=RepairMath.advance(state.damage(),state.fraction(),rate,Math.min(step,72000-tick));
            assertEquals(1188,state.damage());assertEquals(.2,state.fraction(),1e-8);
        }
    }
    @Test void tinyItemsKeepFractionsInsteadOfReceivingOnePointPerUpdate() {
        double rate=RepairMath.rate(1,.2,1,72000);
        var state=new RepairMath.Accumulation(1,0);
        for(int n=0;n<17999;n++) state=RepairMath.advance(state.damage(),state.fraction(),rate,20);
        assertEquals(1,state.damage());assertTrue(state.fraction()>.999);
        assertEquals(new RepairMath.Accumulation(0,0),RepairMath.advance(state.damage(),state.fraction(),rate,20));
    }
    @Test void catchUpMatchesLoadedUpdatesAndCannotBankSurplus() {
        double rate=RepairMath.rate(1000,.2,2,72000);
        assertEquals(700,RepairMath.advance(900,0,rate,36000).damage());
        assertEquals(new RepairMath.Accumulation(0,0),RepairMath.advance(100,.9,rate,Long.MAX_VALUE));
        assertEquals(new RepairMath.Accumulation(0,0),RepairMath.advance(0,.9,rate,20));
        assertEquals(new RepairMath.Accumulation(100,0),RepairMath.advance(100,Double.NaN,Double.POSITIVE_INFINITY,20));
    }
    @Test void etaUsesMissingDurabilityAndLevel() {
        assertEquals(180000,RepairProgress.of(500,1000,0,RepairMath.rate(1000,.2,1,72000),0).remainingTicks());
        var progress=RepairProgress.of(500,1000,.5,RepairMath.rate(1000,.2,5,72000),36);
        assertEquals(.501,progress.completion(),1e-8);assertEquals(35928,progress.remainingTicks());
        assertEquals(-1,RepairProgress.of(500,1000,0,0,100).remainingTicks());
        assertTrue(RepairProgress.of(0,1000,0,0,0).full());
    }
    @Test void legacyConfigMigratesOnceAndKeepsCustomizedSettings(@TempDir Path directory) throws Exception {
        var path=directory.resolve("racksnstands-common.toml");
        Files.writeString(path,"[repairing]\ninterval_minutes=30.0\npercent_per_level=0.1\nrepair_sound=false\n");
        RepairConfigMigration.migrate(path);var first=Files.readString(path);
        assertTrue(first.contains("calculation_period_seconds = 1800.0"));assertTrue(first.contains("interval_ticks = 20"));
        assertTrue(first.contains("percent_per_level = 0.2"));assertTrue(first.contains("repair_sound = false"));assertFalse(first.contains("interval_minutes"));
        RepairConfigMigration.migrate(path);assertEquals(first,Files.readString(path));
        assertTrue(Files.exists(path.resolveSibling(path.getFileName()+".pre-continuous-repair.bak")));
        Files.writeString(path,"[repairing]\ninterval_minutes=12.0\npercent_per_level=0.35\n");
        RepairConfigMigration.migrate(path);assertTrue(Files.readString(path).contains("percent_per_level = 0.35"));
    }
}
