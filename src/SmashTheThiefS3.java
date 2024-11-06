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

public class SmashTheThiefS3 extends JFrame {
    private static final long serialVersionUID = 1L;
    private JLabel[][] holes;
    private ImageIcon thiefIcon;
    private ImageIcon hitIcon;
    private ImageIcon bomberIcon;
    private ImageIcon boomedIcon;
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
    private ImageIcon bomberIconHalf;
    private Map<Point, Timer> disappearTimers = new HashMap<>();

    private static final Point[][] HOLE_POSITIONS = {
        { new Point(322, 270), new Point(662, 270), new Point(992, 270) },
        { new Point(322, 480), new Point(662, 480), new Point(992, 480) },
        { new Point(322, 690), new Point(662, 690), new Point(992, 690) }
    };

    public SmashTheThiefS3() {
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
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setBounds(20, 20, 150, 30);
        gamePanel.add(scoreLabel);

        timerLabel = new JLabel("Time :");
        timerLabel.setFont(customFont);
        timerLabel.setForeground(Color.WHITE);
        timerLabel.setBounds(20, 60, 150, 30);
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
                hole.setBounds(position.x, position.y, 390, 290);
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
                                    handleThiefHit(finalRow, finalCol);
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

        gameTimer = new Timer(1000, e -> updateTimer());
        gameTimer.start();

        setVisible(true);
    }

    private Font loadFont(String fontPath, float size) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath));
            return font.deriveFont(Font.TRUETYPE_FONT, size);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            return new Font("SansSerif", Font.BOLD, (int) size);
        }
    }

    private void updateTimer() {
        if (timeRemaining > 0) {
            timeRemaining--;
            timerBar.setValue(timeRemaining);
        } else {
            gameTimer.stop();
            endGame();
        }
    }

    private void endGame() {
        if (mainTimer != null) mainTimer.stop();
        if (disappearTimer != null) disappearTimer.stop();
        if (gameTimer != null) gameTimer.stop();

        Font customFont = loadFont("Kanit-ExtraBoldItalic.ttf", 18);

        JDialog dialog = new JDialog(this, "Game Over", true);
        dialog.setUndecorated(true);
        dialog.setSize(450, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(40, 42, 54, 220));

        String messageText = score > 50 ? "<html><div style='text-align: center;'>Congrats!<br>YOU CATCH ALL THE THIEVES</div></html>" 
        : "<html><div style='text-align: center;'>Game Over!<br>Try Again</div></html>";

        JLabel message = new JLabel(messageText, JLabel.CENTER);
        message.setFont(customFont.deriveFont(Font.BOLD, 24));
        message.setForeground(Color.WHITE);

        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(255, 85, 100));
        okButton.setForeground(Color.WHITE);
        okButton.setFont(customFont.deriveFont(Font.PLAIN, 18));
        okButton.setFocusPainted(false);
        okButton.setPreferredSize(new Dimension(120, 40));
        okButton.setBorder(BorderFactory.createLineBorder(new Color(255, 85, 85), 2, true));
        okButton.setContentAreaFilled(false);
        okButton.setOpaque(true);

        okButton.addActionListener(e -> {
            dialog.dispose();
            dispose();
            new StageSelect();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(40, 42, 54, 0));
        buttonPanel.add(okButton);

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
            background = ImageIO.read(new File("City.png"));

            BufferedImage thiefImage = ImageIO.read(new File("thief2.png"));
            Image resizedThiefImage = thiefImage.getScaledInstance(390, 290, Image.SCALE_SMOOTH);
            thiefIcon = new ImageIcon(resizedThiefImage);

            BufferedImage thiefImageHalf = ImageIO.read(new File("thief2.5.png"));
            Image resizedThiefImageHalf = thiefImageHalf.getScaledInstance(390, 290, Image.SCALE_SMOOTH);
            thiefIconHalf = new ImageIcon(resizedThiefImageHalf);

            BufferedImage hitImageHalf = ImageIO.read(new File("Smashedthief2.5.png"));
            Image resizedHitImageHalf = hitImageHalf.getScaledInstance(390, 290, Image.SCALE_SMOOTH);
            smashedThiefIconHalf = new ImageIcon(resizedHitImageHalf);

            BufferedImage hitImage = ImageIO.read(new File("Smashedthief2.png"));
            Image resizedHitImage = hitImage.getScaledInstance(390, 290, Image.SCALE_SMOOTH);
            hitIcon = new ImageIcon(resizedHitImage);

            BufferedImage bomberImage = ImageIO.read(new File("Bomber2.png"));
            Image resizedBomberImage = bomberImage.getScaledInstance(390, 290, Image.SCALE_SMOOTH);
            bomberIcon = new ImageIcon(resizedBomberImage);

            BufferedImage bomberImageHalf = ImageIO.read(new File("Bomber2.5.png"));
            Image resizedBomberImageHalf = bomberImageHalf.getScaledInstance(390, 290, Image.SCALE_SMOOTH);
            bomberIconHalf = new ImageIcon(resizedBomberImageHalf);

            BufferedImage boomedImage = ImageIO.read(new File("Boomed2.png"));
            Image resizedBoomedImage = boomedImage.getScaledInstance(390, 290, Image.SCALE_SMOOTH);
            boomedIcon = new ImageIcon(resizedBoomedImage);

            BufferedImage fullHeartImage = ImageIO.read(new File("Heart.png"));
            Image resizedFullHeart = fullHeartImage.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            fullHeartIcon = new ImageIcon(resizedFullHeart);

            BufferedImage lostHeartImage = ImageIO.read(new File("LostHeart.png"));
            Image resizedLostHeart = lostHeartImage.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            lostHeartIcon = new ImageIcon(resizedLostHeart);

            String[] cursorPaths = {
                "cursor1.png",
                "cursor2.png",
                "cursor3.png"
            };

            for (int i = 0; i < cursorPaths.length; i++) { 
                File cursorFile = new File(cursorPaths[i]);
                if (cursorFile.exists()) {
                    BufferedImage cursorImage = ImageIO.read(cursorFile);
                    Image scaledCursorImage = cursorImage.getScaledInstance(3200, 3200, Image.SCALE_SMOOTH);
                    cursors[i] = Toolkit.getDefaultToolkit().createCustomCursor(
                        scaledCursorImage,
                        new Point(0, 0),
                        "cursor" + i
                    );
                } else {
                    System.out.println("Cursor file not found: " + cursorPaths[i]);
                    cursors[i] = Cursor.getDefaultCursor();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading images or cursors.");
        }
    }

    private void spawnThieves() {
        synchronized (lockObject) {
            for (Point p : activeThieves) {
                holes[p.x][p.y].setIcon(null);
                Timer timer = disappearTimers.remove(p);
                if (timer != null) timer.stop();
            }
            activeThieves.clear();
    
            int numThieves = 9;
            Set<Point> usedPositions = new HashSet<>();
    
            for (int i = 0; i < numThieves; i++) {
                int delay = i * 200;
                javax.swing.Timer spawnTimer = new javax.swing.Timer(delay, ev -> {
                    final int row, col;
                    Point tempPosition;
                    do {
                        int tempRow = random.nextInt(3);
                        int tempCol = random.nextInt(3);
                        tempPosition = new Point(tempRow, tempCol);
                    } while (usedPositions.contains(tempPosition));
    
                    row = tempPosition.x;
                    col = tempPosition.y;
    
                    final Point finalPosition = new Point(row, col);
    
                    usedPositions.add(finalPosition);
                    activeThieves.add(finalPosition);
    
                    boolean isBomber = random.nextInt(10) < 3; // 20% chance for bomber
                    holes[row][col].setIcon(isBomber ? bomberIconHalf : thiefIconHalf);
    
                    javax.swing.Timer transitionToFullIcon = new javax.swing.Timer(50, e -> {
                        holes[row][col].setIcon(isBomber ? bomberIcon : thiefIcon);
                    });
                    transitionToFullIcon.setRepeats(false);
                    transitionToFullIcon.start();
    
            
                    int disappearTime = isBomber ? 2000 : 1500; 
                    Timer disappearTimer = new Timer(disappearTime, e -> {
                        holes[row][col].setIcon(isBomber ? bomberIconHalf : thiefIconHalf);
                        Timer finalDisappearTimer = new Timer(50, ev2 -> {
                            holes[row][col].setIcon(null);
                            activeThieves.remove(finalPosition);
                            disappearTimers.remove(finalPosition);
                        });
                        finalDisappearTimer.setRepeats(false);
                        finalDisappearTimer.start();
                    });
                    disappearTimer.setRepeats(false);
                    disappearTimer.start();
                    disappearTimers.put(finalPosition, disappearTimer);
                });
                spawnTimer.setRepeats(false);
                spawnTimer.start();
            }
        }
    }

    private void handleThiefHit(int row, int col) {
        Point position = new Point(row, col);
        if (activeThieves.contains(position)) {
            boolean isBomber = holes[row][col].getIcon().equals(bomberIcon); // เช็คว่าตัวที่โดนเป็นตัวระเบิดหรือไม่
    
            if (isBomber) {
                // ลดชีวิตลง 1 และหักคะแนน 5 คะแนนถ้าโดนโจรถือระเบิด
                lives--;
                if (lives >= 0) {
                    hearts[lives].setIcon(lostHeartIcon);
                }
                score -= 5;
                if (score < 0) score = 0;
                scoreLabel.setText("Score: " + score);
                
                // อัปเดตการแสดงผลชีวิตและตรวจสอบหากหมดชีวิตให้จบเกมทันที
                updateLivesDisplay();
            } else {
                // เพิ่มคะแนนปกติสำหรับโจรทั่วไป
                score++;
                scoreLabel.setText("Score: " + score);
            }
    
            Timer disappearTimer = disappearTimers.remove(position);
            if (disappearTimer != null) {
                disappearTimer.stop();
            }
    
            holes[row][col].setIcon(isBomber ? boomedIcon : hitIcon);
            Timer transitionToHalfSmashed = new Timer(50, e -> {
                holes[row][col].setIcon(isBomber ? boomedIcon : smashedThiefIconHalf);
                Timer finalDisappear = new Timer(isBomber ? 150 : 50, ev -> {
                    holes[row][col].setIcon(null);
                    activeThieves.remove(position);
                });
                finalDisappear.setRepeats(false);
                finalDisappear.start();
            });
            transitionToHalfSmashed.setRepeats(false);
            transitionToHalfSmashed.start();
        }
    }
    

    private void updateLivesDisplay() {
        for (int i = 0; i < hearts.length; i++) {
            hearts[i].setIcon(i < lives ? fullHeartIcon : lostHeartIcon);
        }
        if (lives <= 0) {
            endGame(); 
        }
    }
    

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SmashTheThiefS3::new);
    }
}
