package io.buildman.sync;

import io.buildman.common.models.*;
import io.buildman.common.utils.L;
import io.sentry.Sentry;
import okhttp3.ResponseBody;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class FileAccess {


    public static String toWindowsPath(String path) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return path.replace("/", "\\");
        }
        return path;
    }

    public static String toUnixPath(String path) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return path.replace("\\", "/");
        }
        return path;
    }



    public static boolean writeResponseBodyToDisk(ResponseBody body, Path path) {
        try {
            path = Paths.get(toWindowsPath(path.toString()));
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;
                Files.createDirectories(path.getParent());

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(path.toString());

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String toRelative(String source, String file) {
        source = source.replace("\\", "/");
        file = file.replace("\\", "/");

        return file.replace(source + "/", "").replace("\\", "/");
    }

    public static String fileChecksum(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return DigestUtils.md5Hex(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static HashMap<String, String> calculateChecksum(String directory) {
        HashMap<String, String> fileHashes = new HashMap<>();
        L.logger.info("Directory " + directory);
        try {
            Files.walk(Paths.get(directory))
                    .filter(path -> isOutputFile(directory, path))
                    .forEach(path -> {
                        String checksum = FileAccess.fileChecksum(path);
                        String relativePath = FileAccess.toRelative(directory, path.toString());
                        L.logger.info(checksum + " " + relativePath);
                        fileHashes.put(relativePath, checksum);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        L.logger.info("Checksum count: " + fileHashes.size());
        return fileHashes;

    }


    public static boolean isOutputFile(String baseDirectory, Path path) {
        PathRules pathRules = new PathRules();
        pathRules.addRule("outputs/");
        File file = path.toFile();
        boolean include = pathRules.matches(toRelative(baseDirectory, path.toString()), file.isDirectory());

        return include && Files.isRegularFile(path);
    }
}

