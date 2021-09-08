package io.buildman.common.utils;

import com.intellij.openapi.project.Project;
import io.sentry.Sentry;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static String capitalize(String str) {
        if (str == null) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String getUUIDIfExist(String filePath) {
        L.logger.info("UUID Path " + filePath);
        String content = readFile(filePath);
        if (content != null && !content.isEmpty()) {
            return content;
        }

        return null;
    }

    public static String generateUDID(String filePath) {
        String uuid = UUID.randomUUID().toString();
        saveToFile(uuid, filePath);

        return uuid;
    }

    static String readFile(String path) {

        try {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            return new String(encoded, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }
        return null;
    }

    static void saveToFile(String data, String filename) {
        try (
                FileWriter fw = new FileWriter(filename)) {
            fw.write(data);
        } catch (Exception e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }
    }

    public static String getBasePath(Project project) {
        String basePath = project.getBasePath();
        if (SystemUtils.IS_OS_WINDOWS) {
            basePath = project.getBasePath().replace("/", "\\");
        }

        return basePath;
    }

    public static String humanReadableFormatTime(Duration duration) {
        StringBuilder stringBuilder = new StringBuilder();
        long min = duration.toMinutes() - TimeUnit.HOURS.toMinutes(duration.toHours());
        long sec = duration.getSeconds() - TimeUnit.MINUTES.toSeconds(duration.toMinutes());
        if (min > 0) {
            stringBuilder.append(plural(min, " minute"));
            stringBuilder.append("and ");
        }
        stringBuilder.append(plural(sec, " second"));

        return stringBuilder.toString();
    }

    private static String plural(long num, String unit) {
        return num + " " + unit + (num == 1 ? "" : "s");
    }
}
