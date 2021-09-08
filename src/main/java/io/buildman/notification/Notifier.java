package io.buildman.notification;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class Notifier {
    private static final NotificationGroup TOOL_WINDOW =
            new NotificationGroup("Buildman Notification Group", NotificationDisplayType.TOOL_WINDOW, true);
//    private static final NotificationGroup TOOL_WINDOW = NotificationGroupManager.getInstance().getNotificationGroup("Buildman Notification Group");
    public static void notifyToolWindow(Project project, String message) {
        TOOL_WINDOW.createNotification(message, NotificationType.INFORMATION)
                .notify(project);
    }

    public static void notifyError(Project project, String message) {
        TOOL_WINDOW.createNotification(message, NotificationType.ERROR)
                .notify(project);
    }
}
