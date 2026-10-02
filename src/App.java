import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Collections;
import java.awt.event.ActionEvent;

class App extends JPanel {

    private final ArrayList<BufferedImage> cards = new ArrayList<>();
    private final ArrayList<BufferedImage> originalOrder = new ArrayList<>();

    private static final int FPS = 60;
    private static final int FRAME_TIME = 1000 / FPS;

    public App() {
        setupInput();
        loadCards();

        Timer gameLoop = new Timer(FRAME_TIME, e -> {
            updateGame();
            repaint();
        });

        gameLoop.start();
    }

    private void loadCards() {
        cards.clear();
        originalOrder.clear();

        String[] suits = { "c", "d", "h", "s" };

        for (String suit : suits) {
            for (int number = 1; number <= 13; number++) {

                String cardNumber = String.format("%02d", number);
                String filename = "Cards/" + suit + cardNumber + ".png";

                try {
                    BufferedImage image = ImageIO.read(new File(filename));

                    cards.add(image);

                    // Save the original position once.
                    originalOrder.add(image);

                } catch (Exception e) {
                    System.out.println("Could not load: " + filename);
                }
            }
        }

        System.out.println("Loaded " + cards.size() + " cards.");
    }

    private void setupInput() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        // ESCAPE closes the program
        inputMap.put(
                KeyStroke.getKeyStroke("pressed ESCAPE"),
                "exitGame");

        actionMap.put("exitGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    private void updateGame() {

    }

    private void shuffleDeck() {
        Collections.shuffle(cards);
    }

    private void resetDeck() {
        cards.clear();
        cards.addAll(originalOrder);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw green background
        g.setColor(new Color(10, 130, 42));
        g.fillRect(0, 0, getWidth(), getHeight());

        int columns = 13;

        int cardWidth = 40;
        int gap = 5;

        int startX = 20;
        int startY = 20;

        for (int i = 0; i < cards.size(); i++) {
            BufferedImage card = cards.get(i);

            int cardHeight = (int) (card.getHeight() * ((double) cardWidth / card.getWidth()));

            int column = i % columns;
            int row = i / columns;

            int x = startX + column * (cardWidth + gap);
            int y = startY + row * (cardHeight + gap);

            g.drawImage(card, x, y, cardWidth, cardHeight, null);
        }
    }

    public static void main(String[] args) throws Exception {

        JFrame frame = new JFrame("Card Game");

        App app = new App();

        JButton shuffleButton = new JButton("Reshuffle Deck");
        JButton resetButton = new JButton("Reset Deck");
        JPanel buttonPanel = new JPanel();

        buttonPanel.add(shuffleButton);
        buttonPanel.add(resetButton);

        shuffleButton.addActionListener(e -> {
            app.shuffleDeck();
            app.repaint();
        });

        resetButton.addActionListener(e -> {
            app.resetDeck();
            app.repaint();
        });

        frame.setLayout(new BorderLayout());
        frame.add(app, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}