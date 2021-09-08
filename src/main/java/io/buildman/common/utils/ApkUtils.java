package io.buildman.common.utils;


import com.github.fracpete.processoutput4j.output.CollectingProcessOutput;
import com.github.fracpete.rsync4j.RSync;


public class ApkUtils {
    public static boolean patchApk(String apkPath, String diffPatchPath) {
        RSync rsync = new RSync()
                .source("")
                .destination(apkPath)
                .readBatch(diffPatchPath)
                .verbose(true);
        CollectingProcessOutput output = null;
        try {
            output = rsync.execute();
            L.logger.info(output.getStdOut());
            L.logger.info("Exit code: " + output.getExitCode());
            if (output.getExitCode() > 0)
                return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
