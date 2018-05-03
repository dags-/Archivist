package me.dags.archivist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;

import java.io.IOException;
import java.nio.file.*;
import java.time.Month;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author dags <dags@dags.me>
 */
@Plugin(id = "archivist", name = "Archivist", version = "0.1", description = "Tidies your logs folder")
public class Archivist implements Runnable {

    private final PathMatcher filter = FileSystems.getDefault().getPathMatcher("glob:*.log.gz");
    private final Pattern pattern = Pattern.compile("((\\d{4})-(\\d{2})-\\d{2})");
    private final Logger logger = LoggerFactory.getLogger("Archivist");
    private final Path logs = Sponge.getGame().getGameDirectory().resolve("logs").toAbsolutePath();

    @Listener
    public void start(GameStartedServerEvent e) {
        Task.builder().interval(1, TimeUnit.HOURS).delay(5, TimeUnit.SECONDS).execute(this).async().submit(this);
    }

    @Override
    public void run() {
        try {
            logger.info("Checking for logs to archive in {}", logs);
            Files.list(logs).filter(p -> filter.matches(p.getFileName())).forEach(this::archive);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void archive(Path source){
        Matcher matcher = pattern.matcher(source.getFileName().toString());
        if (!matcher.find()) {
            logger.info("Skipping file {}", source);
            return;
        }

        // String date = matcher.group(1);
        String year = matcher.group(2);
        String month = matcher.group(3);
        String folder = String.format("%s-%s", month, Month.of(Integer.parseInt(month)));

        Path dir = logs.resolve(year).resolve(folder);
        Path destination = dir.resolve(source.getFileName());

        if (Files.exists(destination)) {
            String file = destination.getFileName().toString();
            String name = file.replace(".log.gz", "");
            for (int i = 0; Files.exists(destination); i++) {
                String fileName = String.format("%s-%s.log.gz", name, i);
                destination = dir.resolve(fileName);
            }
        }

        if (!mkdirs(dir)) {
            logger.warn("Unable to create directory {}", dir);
            return;
        }

        if (!move(source, destination)) {
            logger.warn("Unable to move file {} to {}", source, destination);
            return;
        }

        logger.info("Archived file {} to {}", source, destination);
    }

    private static boolean mkdirs(Path path) {
        if (Files.exists(path)) {
            return true;
        }

        try {
            Files.createDirectories(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean move(Path src, Path dst) {
        try {
            Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
