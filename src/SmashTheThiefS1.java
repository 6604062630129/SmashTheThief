import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;

public class SmashTheThiefS1 extends JFrame {
    private static final long serialVersionUID = 1L;
    private JLabel[][] holes;
    private ImageIcon thiefIcon;
    private ImageIcon hitIcon;
    private Cursor[] cursors = new Cursor[3];
    private int cursorIndex = 0;
    private int score = 0;
    private int timeRemaining = 30; // ตัวจับเวลา (30 วินาที)
    private JLabel scoreLabel;
    private JLabel timerLabel;
    private JProgressBar timerBar;
    private Timer gameTimer; // Timer สำหรับการจับเวลาเกม 30 วินาที
    private Timer mainTimer; // Timer สำหรับการ spawn thieves
    private Timer disappearTimer; // Timer สำหรับการลบโจร
    private final Set<Point> activeThieves = new HashSet<>();
    private final Object lockObject = new Object();
    private final Random random = new Random();
    private Image background;
    private Timer cursorAnimationTimer;
    private boolean isAnimating = false;
    private JLabel[] hearts = new JLabel[3];
    private ImageIcon fullHeartIcon;
    private ImageIcon lostHeartIcon;
    private int lives = 3; // เริ่มต้นที่ 3 ชีวิต
    private ImageIcon thiefIconHalf; 
    private ImageIcon smashedThiefIconHalf;
    private Map<Point, Timer> disappearTimers = new HashMap<>();

    private static final Point[][] HOLE_POSITIONS = {
        { new Point(340, 320), new Point(705, 320), new Point(1070, 320) },
        { new Point(340, 545), new Point(705, 545), new Point(1070, 545) },
        { new Point(340, 770), new Point(705, 770), new Point(1070, 770) }
    };

    public SmashTheThiefS1() {
        setTitle("Smash The Thief Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());

        loadAssets();
        setCursor(cursors[cursorIndex]);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    startCursorAnimation();
                }
            }
        });

        Font customFont = loadFont("Kanit-ExtraBoldItalic.ttf", 24);

        JPanel gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (background != null) {
                    g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        gamePanel.setLayout(null);
        add(gamePanel, BorderLayout.CENTER);

        // ตั้งค่า score และ timer ให้แสดงตรงมุมซ้ายบนของพื้นหลัง
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(customFont);
        scoreLabel.setBounds(20, 20, 150, 30); // ปรับตำแหน่งและขนาด
        gamePanel.add(scoreLabel);

        timerLabel = new JLabel("Time :");
        timerLabel.setFont(customFont);
        timerLabel.setBounds(20, 60, 150, 30); // ปรับตำแหน่งและขนาด
        gamePanel.add(timerLabel);

        timerBar = new JProgressBar(0, timeRemaining);
        timerBar.setValue(timeRemaining);
        timerBar.setBounds(95, 65, 300, 20);
        timerBar.setMaximum(timeRemaining);
        timerBar.setValue(timeRemaining);

        timerBar.setUI(new BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // กำหนดสีไล่จากเขียวไปแดง
                int width = (int) (timerBar.getWidth() * (timerBar.getPercentComplete()));
                GradientPaint gradient = new GradientPaint(0, 0, Color.RED, timerBar.getWidth(), 0, Color.PINK);

                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, width, timerBar.getHeight());
                g2d.setColor(Color.BLACK);
                g2d.drawRect(0, 0, timerBar.getWidth() - 1, timerBar.getHeight() - 1);
            }
        });

        for (int i = 0; i < hearts.length; i++) {
            hearts[i] = new JLabel(fullHeartIcon); 
            hearts[i].setBounds(1400 + (i * 70), 50, 80, 80); 
            gamePanel.add(hearts[i]);
        }

        gamePanel.add(timerBar);

        holes = new JLabel[3][3];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                JLabel hole = new JLabel();
                hole.setHorizontalAlignment(SwingConstants.CENTER);
                Point position = HOLE_POSITIONS[row][col];
                hole.setBounds(position.x, position.y, 298, 230);
                hole.setOpaque(false);
                final int finalRow = row;
                final int finalCol = col;

                hole.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            startCursorAnimation();
                            synchronized (lockObject) {
                                Point thiefPosition = new Point(finalRow, finalCol);
                                if (activeThieves.contains(thiefPosition)) {
                                    handleThiefHit(finalRow, finalCol); // เรียกใช้ handleThiefHit เมื่อโจรถูกตี
                                }
                            }
                        }
                    }
                });

                holes[row][col] = hole;
                gamePanel.add(hole);
            }
        }

        mainTimer = new Timer(3000, e -> spawnThieves()); 
        mainTimer.start();

        Timer initialSpawnTimer = new Timer(2000, e -> spawnThieves());
        initialSpawnTimer.setRepeats(false);
        initialSpawnTimer.start();

        // เพิ่ม Timer จับเวลาเกม 30 วินาที
        gameTimer = new Timer(1000, e -> updateTimer());
        gameTimer.start();

        setVisible(true);
    }

    private void updateTimer() {
        if (timeRemaining > 0) {
            timeRemaining--;
            timerBar.setValue(timeRemaining); // ลดค่า timerBar ทุกวินาที
        } else {
            gameTimer.stop();
            endGame();
        }
    };

    private void endGame() {
        // หยุด timers
        if (mainTimer != null) mainTimer.stop();
        if (disappearTimer != null) disappearTimer.stop();
        if (gameTimer != null) gameTimer.stop();
        
        Font customFont = loadFont("Kanit-ExtraBoldItalic.ttf", 18);
        
        JDialog dialog = new JDialog(this, "Game Over", true);
        dialog.setUndecorated(true); // ไม่มีกรอบ dialog
        dialog.setSize(450, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setBackground(new Color(0, 0, 0, 0)); // พื้นหลังโปร่งใส
        
        // กำหนดสีและฟอนต์พื้นหลัง
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(40, 42, 54, 220)); // สีพื้นหลังเข้มและโปร่งแสงเล็กน้อย
        
        // เพิ่มข้อความแสดงคะแนนเเละเงื่อนไข
        String messageText = score > 30 ? "<html><div style='text-align: center;'>Congrats!<br>YOU CATCH ALL THE THIEVES</div></html>" 
        : "<html><div style='text-align: center;'>Game Over!<br>Try Again</div></html>";
    
        // สร้าง JLabel ที่ใช้แสดงข้อความ
        JLabel message = new JLabel(messageText, JLabel.CENTER);
        message.setFont(customFont.deriveFont(Font.BOLD, 24)); // ใช้ customFont ที่คุณโหลด
        message.setForeground(Color.WHITE);
        
        // สร้างปุ่ม OK ปรับสไตล์ให้มีขอบมน
        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(255, 85, 100)); 
        okButton.setForeground(Color.WHITE);
        okButton.setFont(customFont.deriveFont(Font.PLAIN, 18));
        okButton.setFocusPainted(false);
        okButton.setPreferredSize(new Dimension(120, 40));
        okButton.setBorder(BorderFactory.createLineBorder(new Color(255, 85, 85), 2, true)); // ขอบมน
        okButton.setContentAreaFilled(false); 
        okButton.setOpaque(true);
    
        // ActionListener สำหรับปิด dialog และเปิด StageSelect ใหม่
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
                dispose(); // ปิดหน้าต่างเกมหลังจากกด OK
                new StageSelect(); // เปิดหน้าต่างเลือกด่านขึ้นมาใหม่
            }
        });
    
        // จัดตำแหน่งของปุ่ม OK
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(40, 42, 54, 0)); // พื้นหลังโปร่งแสงเล็กน้อย
        buttonPanel.add(okButton);
    
        // เพิ่มข้อความและปุ่มลงใน dialog
        mainPanel.add(message, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void startCursorAnimation() {
        if (cursorAnimationTimer != null && cursorAnimationTimer.isRunning()) {
            cursorAnimationTimer.stop();
        }

        cursorIndex = 0;
        isAnimating = true;

        cursorAnimationTimer = new Timer(50, new ActionListener() {
            private int steps = 0;
            private final int TOTAL_STEPS = 5; 

            @Override
            public void actionPerformed(ActionEvent e) {
                if (steps >= TOTAL_STEPS) {
                    cursorAnimationTimer.stop();
                    isAnimating = false;
                    cursorIndex = 0;
                    setCursor(cursors[cursorIndex]);
                    return;
                }

                cursorIndex = (steps < 3) ? steps : TOTAL_STEPS - steps;
                setCursor(cursors[cursorIndex]);
                steps++;
            }
        });

        cursorAnimationTimer.start();
    }

    private void loadAssets() {
        try {
            // โหลดภาพพื้นหลัง
            background = ImageIO.read(getClass().getResourceAsStream("/Sun.png"));
    
            // โหลดไอคอนโจร
            BufferedImage thiefImage = ImageIO.read(getClass().getResourceAsStream("/thief1.png"));
            Image resizedThiefImage = thiefImage.getScaledInstance(298, 230, Image.SCALE_SMOOTH);
            thiefIcon = new ImageIcon(resizedThiefImage);
    
            BufferedImage thiefImageHalf = ImageIO.read(getClass().getResourceAsStream("/thief1.5.png"));
            Image resizedThiefImageHalf = thiefImageHalf.getScaledInstance(298, 230, Image.SCALE_SMOOTH);
            thiefIconHalf = new ImageIcon(resizedThiefImageHalf);
    
            // โหลดไอคอนโจรที่โดนตี
            BufferedImage hitImage = ImageIO.read(getClass().getResourceAsStream("/Smashedthief1.png"));
            Image resizedHitImage = hitImage.getScaledInstance(298, 230, Image.SCALE_SMOOTH);
            hitIcon = new ImageIcon(resizedHitImage);
    
            BufferedImage hitImageHalf = ImageIO.read(getClass().getResourceAsStream("/Smashedthief1.5.png"));
            Image resizedHitImageHalf = hitImageHalf.getScaledInstance(298, 230, Image.SCALE_SMOOTH);
            smashedThiefIconHalf = new ImageIcon(resizedHitImageHalf);
    
            // โหลดไอคอนหัวใจ
            BufferedImage fullHeartImage = ImageIO.read(getClass().getResourceAsStream("/Heart.png"));
            Image resizedFullHeart = fullHeartImage.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            fullHeartIcon = new ImageIcon(resizedFullHeart);
    
            BufferedImage lostHeartImage = ImageIO.read(getClass().getResourceAsStream("/LostHeart.png"));
            Image resizedLostHeart = lostHeartImage.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            lostHeartIcon = new ImageIcon(resizedLostHeart);
    
            // โหลดไอคอนเคอร์เซอร์
            String[] cursorPaths = { "/cursor1.png", "/cursor2.png", "/cursor3.png" };
            for (int i = 0; i < cursorPaths.length; i++) {
                BufferedImage cursorImage = ImageIO.read(getClass().getResourceAsStream(cursorPaths[i]));
                Image scaledCursorImage = cursorImage.getScaledInstance(3200, 3200, Image.SCALE_SMOOTH);
                cursors[i] = Toolkit.getDefaultToolkit().createCustomCursor(
                        scaledCursorImage,
                        new Point(0, 0),
                        "cursor" + i
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading images or cursors.");
        }
    }
    
    private Font loadFont(String fontPath, float size) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/"+fontPath));
            return font.deriveFont(Font.TRUETYPE_FONT, size);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            return new Font("SansSerif", Font.BOLD, (int) size); // ฟอนต์สำรอง
        }
    }
    

    private void spawnThieves() {
        synchronized (lockObject) {
            for (Point p : activeThieves) {
                holes[p.x][p.y].setIcon(null);
                Timer timer = disappearTimers.remove(p); // ลบ Timer ของโจรที่ลบไปแล้ว
                if (timer != null) timer.stop();
            }
            activeThieves.clear();
    
            int numThieves = 9;
            Set<Point> usedPositions = new HashSet<>();
    
            for (int i = 0; i < numThieves; i++) {
                int delay = i * 300; // ความหน่วงเวลาก่อนโจรแต่ละตัวขึ้น
                javax.swing.Timer spawnTimer = new javax.swing.Timer(delay, ev -> {
                    final int row, col; // กำหนดเป็น final
                    Point tempPosition;
                    do {
                        int tempRow = random.nextInt(3); // แก้ไขตรงนี้โดยใช้ตัวแปรชั่วคราว
                        int tempCol = random.nextInt(3);
                        tempPosition = new Point(tempRow, tempCol);
                    } while (usedPositions.contains(tempPosition));
    
                    row = tempPosition.x; // กำหนดค่าให้ row จาก tempPosition
                    col = tempPosition.y; // กำหนดค่าให้ col จาก tempPosition
    
                    final Point finalPosition = new Point(row, col); // ใช้ finalPosition แทน
    
                    usedPositions.add(finalPosition);
                    activeThieves.add(finalPosition);
    
                    // Animation ขึ้นจากหลุม เริ่มจาก thief1.5 ไป thief1
                    holes[row][col].setIcon(thiefIconHalf); // เริ่มด้วยภาพ thief1.5
    
                    javax.swing.Timer transitionToThief1 = new javax.swing.Timer(50, e -> {
                        holes[row][col].setIcon(thiefIcon); // เปลี่ยนเป็น thief1
                    });
                    transitionToThief1.setRepeats(false);
                    transitionToThief1.start();
    
                    // Timer สำหรับลบโจร ถ้าไม่ได้ถูกตี
                    Timer disappearTimer = new Timer(1500, e -> {
                        holes[row][col].setIcon(thiefIconHalf); // เปลี่ยนกลับเป็น thief1.5 ก่อนหายไป
                        Timer finalDisappearTimer = new Timer(50, ev2 -> {
                            holes[row][col].setIcon(null); // ลบภาพโจรออก
                            activeThieves.remove(finalPosition); // ใช้ finalPosition แทน
                            disappearTimers.remove(finalPosition); // ลบ Timer ออกจาก Map
                        });
                        finalDisappearTimer.setRepeats(false);
                        finalDisappearTimer.start();
                    });
                    disappearTimer.setRepeats(false);
                    disappearTimer.start();
                    disappearTimers.put(finalPosition, disappearTimer); // เก็บ Timer ไว้ใน Map
                });
                spawnTimer.setRepeats(false);
                spawnTimer.start();
            }
        }
    }
    
    private void handleThiefHit(int row, int col) {
        Point position = new Point(row, col);
        if (activeThieves.contains(position)) {
            score++;
            scoreLabel.setText("Score: " + score);
    
            // หยุด Timer ของโจรที่ถูกตีเพื่อป้องกันการซ้อนทับ
            Timer disappearTimer = disappearTimers.remove(position);
            if (disappearTimer != null) {
                disappearTimer.stop();
            }
    
            // Animation โจรโดนตี เริ่มจาก smashedthief1 ไป smashedthief1.5
            holes[row][col].setIcon(hitIcon);
            Timer transitionToSmashedThief1_5 = new Timer(50, e -> {
                holes[row][col].setIcon(smashedThiefIconHalf);
                Timer finalDisappear = new Timer(50, ev -> {
                    holes[row][col].setIcon(null);
                    activeThieves.remove(position);
                });
                finalDisappear.setRepeats(false);
                finalDisappear.start();
            });
            transitionToSmashedThief1_5.setRepeats(false);
            transitionToSmashedThief1_5.start();
        }
    }

   
}