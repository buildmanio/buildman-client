package io.buildman.task;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AskLicenseKey {
    private static final String BUILDMAN_SITE = "https://buildman.io";
    public JPanel panel;
    public JTextField licenseKey;
    private JLabel goToSite;
    private JButton trialButton;
    private ClickListener listener;

    public AskLicenseKey() {
        goToSite.setCursor(new Cursor(Cursor.HAND_CURSOR));
        goToSite.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                try {
                    Desktop.getDesktop().browse(new URI(BUILDMAN_SITE));
                } catch (URISyntaxException | IOException ex) {
                    //It looks like there's a problem
                }
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {

            }
        });
        trialButton.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                listener.onClick();
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {

            }
        });
    }

    public void setonTrialClickListener(ClickListener listener) {

        this.listener = listener;
    }

    public interface ClickListener {
        void onClick();
    }
}
