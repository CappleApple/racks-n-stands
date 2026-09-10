package com.cappleapple.racksnstands.config;

import com.cappleapple.racksnstands.RacksNStands;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.nio.file.*;
import java.io.*;

/** Convert the old rate period before FML removes obsolete keys during schema correction. */
public final class RepairConfigMigration {
    public static void migrate(Path path) {
        if(!Files.isRegularFile(path)) return;
        try {
            var text=Files.readString(path);var config=new TomlParser().parse(new StringReader(text));
            Object old=config.get("repairing.interval_minutes");if(!(old instanceof Number minutes)) return;
            if(!config.contains("repairing.calculation_period_seconds")) config.set("repairing.calculation_period_seconds",Math.clamp(minutes.doubleValue()*60,.05,31536000));
            if(!config.contains("repairing.interval_ticks")) config.set("repairing.interval_ticks",20);
            Number percent=config.get("repairing.percent_per_level");
            if(percent!=null&&percent.doubleValue()==.1) config.set("repairing.percent_per_level",.2);
            config.remove("repairing.interval_minutes");
            var backup=path.resolveSibling(path.getFileName()+".pre-continuous-repair.bak");
            if(!Files.exists(backup)) Files.writeString(backup,text);
            var result=new StringWriter();new TomlWriter().write(config,result);Files.writeString(path,result.toString());
            RacksNStands.LOGGER.info("Migrated repair timing in {} (original saved to {})",path.getFileName(),backup.getFileName());
        } catch(IOException|RuntimeException error) {
            RacksNStands.LOGGER.warn("Could not migrate repair timing in {}",path,error);
        }
    }
    private RepairConfigMigration() {}
}
