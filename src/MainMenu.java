import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.*;

public class MainMenu extends JFrame {

    private Image backgroundImage;

    public MainMenu() {
        setTitle("Smash The Thief - Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // โหลดภาพพื้นหลัง
        try (InputStream imgStream = getClass().getResourceAsStream("/Main menu.png")) {
            if (imgStream != null) {
                backgroundImage = ImageIO.read(imgStream);
            } else {
                System.out.println("Error loading background image.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        backgroundPanel.setLayout(null);

        JLabel startButton = new JLabel();
        BufferedImage startImage;
        try (InputStream imgStream = getClass().getResourceAsStream("/STARTbotton.png")) {
            if (imgStream != null) {
                startImage = ImageIO.read(imgStream);
                BufferedImage resizedImage = resizeImageWithHint(startImage, 200, 200);
                startButton.setIcon(new ImageIcon(resizedImage));
            } else {
                System.out.println("Error loading start button image.");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        startButton.setBounds(750, 650, 200, 200);
        startButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // สร้างภาพที่ลดขนาดลง 15% สำหรับเอฟเฟกต์ hover
        BufferedImage startImageSmall;
        try (InputStream imgStream = getClass().getResourceAsStream("/STARTbotton.png")) {
            if (imgStream != null) {
                startImageSmall = ImageIO.read(imgStream);
                BufferedImage resizedImageSmall = resizeImageWithHint(startImageSmall, 170, 170);
                ImageIcon hoverIcon = new ImageIcon(resizedImageSmall);
                ImageIcon defaultIcon = (ImageIcon) startButton.getIcon();

                startButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        startButton.setIcon(hoverIcon);
                        startButton.setBounds(startButton.getX() + 15, startButton.getY() + 15, 170, 170);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        startButton.setIcon(defaultIcon);
                        startButton.setBounds(startButton.getX() - 15, startButton.getY() - 15, 200, 200);
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        dispose();
                        SwingUtilities.invokeLater(() -> new StageSelect());
                    }
                });
            } else {
                System.out.println("Error loading start button hover image.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        backgroundPanel.add(startButton);
        add(backgroundPanel);

        setVisible(true);
    }

    private BufferedImage resizeImageWithHint(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();
        return resizedImage;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainMenu::new);
    }
}
