import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class MainMenu extends JFrame {

    private Image backgroundImage;

    public MainMenu() {
        setTitle("Smash The Thief - Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // โหลดภาพพื้นหลัง
        try {
            backgroundImage = ImageIO.read(new File("Main menu.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading background image.");
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
        try {
            BufferedImage startImage = ImageIO.read(new File("START botton.png"));
            BufferedImage resizedImage = resizeImageWithHint(startImage, 200, 200); 
            startButton.setIcon(new ImageIcon(resizedImage));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading start button image.");
            return;
        }

        
        startButton.setBounds(750, 650, 200, 200); 
        startButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // สร้างภาพที่ลดขนาดลง 15% สำหรับเอฟเฟกต์ hover
        BufferedImage startImageSmall;
        try {
            startImageSmall = ImageIO.read(new File("START botton.png"));
            BufferedImage resizedImageSmall = resizeImageWithHint(startImageSmall, 170, 170); 
            ImageIcon hoverIcon = new ImageIcon(resizedImageSmall);
            ImageIcon defaultIcon = (ImageIcon) startButton.getIcon(); 

            // เพิ่ม MouseListener ให้ปุ่ม start เพื่อเปลี่ยนขนาดเมื่อ hover
            startButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    startButton.setIcon(hoverIcon); // เปลี่ยนเป็นขนาดเล็กลงเมื่อ hover
                    startButton.setBounds(startButton.getX() + 15, startButton.getY() + 15, 170, 170); 
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    startButton.setIcon(defaultIcon); // กลับเป็นขนาดเดิมเมื่อเลื่อนออก
                    startButton.setBounds(startButton.getX() - 15, startButton.getY() - 15, 200, 200); 
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    dispose(); 
                    new StageSelect(); 
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        
        backgroundPanel.add(startButton);
        add(backgroundPanel);

        setVisible(true);
    }

    // ฟังก์ชันสำหรับปรับขนาดภาพและเปิด anti-aliasing
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
