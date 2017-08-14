package me.dags.archivist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
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
        Task.builder().interval(30, TimeUnit.MINUTES).delay(1, TimeUnit.SECONDS).execute(this).async().submit(this);
    }

    @Override
    public void run() {
        try {
            logger.info("Checking for GZipped logs to archive in {}", logs);
            Files.list(logs).filter(p -> filter.matches(p.getFileName())).forEach(this::archive);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void archive(Path path){
        Matcher matcher = pattern.matcher(path.getFileName().toString());
        if (!matcher.find()) {
            logger.info("Skipping file {}", path);
            return;
        }

        logger.info("Archiving log {}", path);

        try {
            // String date = matcher.group(1);
            String year = matcher.group(2);
            String month = matcher.group(3);
            String folder = String.format("%s-%s", month, Month.of(Integer.parseInt(month)));

            Path dir = logs.resolve(year).resolve(folder);
            Path target = dir.resolve(path.getFileName());

            Files.createDirectories(dir);
            Files.copy(path, target);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}