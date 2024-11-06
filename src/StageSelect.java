import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.*;

public class StageSelect extends JFrame {
    private Image backgroundImage;
    private int stageCount = 3; // จำนวน stage
    private JPanel backgroundPanel;

    public StageSelect() {
        // เรียกใช้ initializeUI ผ่าน invokeLater
        SwingUtilities.invokeLater(this::initializeUI);
    }

    private void initializeUI() {
        this.setTitle("Smash The Thief - Stage Select");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // โหลดภาพพื้นหลัง
        try (InputStream imgStream = getClass().getResourceAsStream("/STAGEselectBG.png")) {
            if (imgStream != null) {
                backgroundImage = ImageIO.read(imgStream);
            } else {
                System.out.println("Error loading background image.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // สร้าง JPanel เพื่อวาดภาพพื้นหลัง
        backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        backgroundPanel.setLayout(null); // ตั้งค่า layout เป็น null เพื่อจัดตำแหน่งปุ่มเอง
        add(backgroundPanel); // เพิ่ม backgroundPanel เข้าไปใน JFrame

        // สร้างปุ่มเลือก stage
        for (int i = 1; i <= stageCount; i++) {
            String imagePath = "/Stage" + i + "Button.png"; // อ้างอิง path รูปภาพเป็น resource path
            JButton stageButton = createImageButton(imagePath, i);

            // กำหนดตำแหน่งและขนาดของปุ่มโดยใช้ i เพื่อการจัดเรียง
            int xPosition = 115 + (i - 1) * (1600 / stageCount); // เพิ่มระยะห่างระหว่างปุ่มด้วย i
            stageButton.setBounds(xPosition, 200, 1200 / stageCount, 600);

            final int stageNumber = i; // สร้างตัวแปรเพื่อให้สามารถใช้ใน lambda ได้
            stageButton.addActionListener(e -> {
                dispose();
                switch (stageNumber) {
                    case 1:
                        SwingUtilities.invokeLater(() -> new SmashTheThiefS1());
                        break;
                    case 2:
                        SwingUtilities.invokeLater(() -> new SmashTheThiefS2());
                        break;
                    case 3:
                        SwingUtilities.invokeLater(() -> new SmashTheThiefS3());
                        break;
                    default:
                        System.out.println("Stage not available");
                }
            });

            backgroundPanel.add(stageButton); // เพิ่มปุ่มลงใน backgroundPanel
        }

        setVisible(true);
    }

    private JButton createImageButton(String imagePath, int i) {
        JButton button = new JButton();
        try (InputStream imgStream = getClass().getResourceAsStream(imagePath)) {
            if (imgStream != null) {
                BufferedImage originalImage = ImageIO.read(imgStream);
                ImageIcon iconTransparent = new ImageIcon(makeTransparent(originalImage, 0.7f));
                ImageIcon iconOpaque = new ImageIcon(makeTransparent(originalImage, 1.0f));

                button.setIcon(iconTransparent);
                button.setBorderPainted(false);
                button.setFocusPainted(false);
                button.setContentAreaFilled(false);

                // เพิ่ม MouseListener เพื่อจัดการเอฟเฟกต์ hover
                button.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        button.setIcon(iconOpaque);
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        button.setIcon(iconTransparent);
                    }
                });
            } else {
                System.out.println("Error loading button image for stage " + i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return button;
    }

    // ฟังก์ชันปรับความโปร่งใสของ BufferedImage
    private BufferedImage makeTransparent(BufferedImage image, float alpha) {
        BufferedImage transparentImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = transparentImage.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return transparentImage;
    }
}
