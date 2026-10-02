import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Collections;

class App extends JPanel {

    private final ArrayList<BufferedImage> cards = new ArrayList<>();
    private final ArrayList<BufferedImage> originalOrder = new ArrayList<>();

    // Cards used specifically by the matching game
    private final ArrayList<GameCard> gameCards = new ArrayList<>();

    // Back of the playing card
    private BufferedImage cardBack;

    private static final int FPS = 60;
    private static final int FRAME_TIME = 1000 / FPS;

    // Change this to give the player more or less time
    private static final int GAME_TIME_SECONDS = 90;

    // How long two incorrect cards stay visible
    private static final int MISMATCH_DELAY = 700;

    // Card drawing settings
    private static final int COLUMNS = 13;
    private static final int CARD_WIDTH = 40;
    private static final int GAP = 5;
    private static final int START_X = 20;
    private static final int NORMAL_START_Y = 20;
    private static final int GAME_START_Y = 75;

    // Matching game state
    private boolean matchingGameActive = false;
    private boolean gameOver = false;
    private boolean playerWon = false;

    private int firstSelectedIndex = -1;
    private int secondSelectedIndex = -1;

    private int matchedPairs = 0;

    private long gameEndTime = 0;
    private long mismatchHideTime = 0;

    private int finalSecondsRemaining = 0;

    /*
     * Represents one card in the matching game.
     *
     * Two cards with the same pairID are considered a match.
     */
    private static class GameCard {

        BufferedImage image;
        int pairID;

        boolean faceUp = false;
        boolean matched = false;

        GameCard(BufferedImage image, int pairID) {
            this.image = image;
            this.pairID = pairID;
        }
    }

    public App() {

        setupInput();
        setupMouse();
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

        // Load the card back separately.
        try {
            cardBack = ImageIO.read(new File("Cards/cb.png"));
            System.out.println("Loaded card back.");
        } catch (Exception e) {
            System.out.println("Could not load: Cards/cb.png");
        }

        String[] suits = { "c", "d", "h", "s" };

        for (String suit : suits) {

            for (int number = 1; number <= 13; number++) {

                String cardNumber = String.format("%02d", number);
                String filename = "Cards/" + suit + cardNumber + ".png";

                try {

                    BufferedImage image = ImageIO.read(new File(filename));

                    cards.add(image);

                    // Save the original position once
                    originalOrder.add(image);

                } catch (Exception e) {

                    System.out.println(
                            "Could not load: " + filename);
                }
            }
        }

        System.out.println(
                "Loaded " + cards.size() + " cards.");
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

    /*
     * Listen for mouse clicks so the player can select cards.
     */
    private void setupMouse() {

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {

                handleCardClick(
                        e.getX(),
                        e.getY());
            }
        });
    }

    /*
     * Runs every frame.
     */
    private void updateGame() {

        if (!matchingGameActive || gameOver) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        /*
         * If two incorrect cards were selected,
         * wait briefly and then flip them back down.
         */
        if (mismatchHideTime > 0
                && currentTime >= mismatchHideTime) {

            if (firstSelectedIndex >= 0
                    && secondSelectedIndex >= 0) {

                gameCards.get(
                        firstSelectedIndex).faceUp = false;

                gameCards.get(
                        secondSelectedIndex).faceUp = false;
            }

            firstSelectedIndex = -1;
            secondSelectedIndex = -1;
            mismatchHideTime = 0;
        }

        /*
         * Check whether the player ran out of time.
         */
        if (currentTime >= gameEndTime) {

            gameOver = true;
            playerWon = false;

            // Turn any currently visible wrong cards
            // face down.
            if (firstSelectedIndex >= 0
                    && firstSelectedIndex < gameCards.size()) {

                GameCard card = gameCards.get(firstSelectedIndex);

                if (!card.matched) {
                    card.faceUp = false;
                }
            }

            if (secondSelectedIndex >= 0
                    && secondSelectedIndex < gameCards.size()) {

                GameCard card = gameCards.get(secondSelectedIndex);

                if (!card.matched) {
                    card.faceUp = false;
                }
            }

            firstSelectedIndex = -1;
            secondSelectedIndex = -1;
            mismatchHideTime = 0;
        }
    }

    /*
     * Starts a completely new matching game.
     */
    public void startMatchingGame() {

        if (originalOrder.size() < 26) {

            System.out.println(
                    "Not enough cards were loaded.");

            return;
        }

        gameCards.clear();

        /*
         * Make a shuffled copy of the normal deck.
         */
        ArrayList<BufferedImage> possibleCards = new ArrayList<>(originalOrder);

        Collections.shuffle(possibleCards);

        /*
         * Choose 13 random cards.
         *
         * Each selected image is added twice,
         * making 13 pairs / 26 cards total.
         */
        for (int i = 0; i < 13; i++) {

            BufferedImage image = possibleCards.get(i);

            gameCards.add(
                    new GameCard(image, i));

            gameCards.add(
                    new GameCard(image, i));
        }

        /*
         * Randomize where all 52 matching-game
         * cards appear on the board.
         */
        Collections.shuffle(gameCards);

        matchedPairs = 0;

        firstSelectedIndex = -1;
        secondSelectedIndex = -1;

        mismatchHideTime = 0;

        gameOver = false;
        playerWon = false;

        matchingGameActive = true;

        /*
         * Begin the countdown.
         */
        gameEndTime = System.currentTimeMillis()
                + GAME_TIME_SECONDS * 1000L;

        repaint();
    }

    /*
     * Called whenever the mouse is pressed.
     */
    private void handleCardClick(int mouseX, int mouseY) {

        /*
         * Cards can only be clicked while an
         * active game is running.
         */
        if (!matchingGameActive || gameOver) {
            return;
        }

        /*
         * Don't allow another card to be clicked
         * while two incorrect cards are being shown.
         */
        if (mismatchHideTime > 0) {
            return;
        }

        for (int i = 0; i < gameCards.size(); i++) {

            GameCard gameCard = gameCards.get(i);

            // Matched cards have disappeared,
            // so they cannot be clicked again.
            if (gameCard.matched) {
                continue;
            }

            Rectangle cardBounds = getGameCardBounds(i);

            if (cardBounds.contains(mouseX, mouseY)) {

                selectCard(i);
                return;
            }
        }
    }

    /*
     * Handles the matching rules after a card
     * is clicked.
     */
    private void selectCard(int index) {

        GameCard selectedCard = gameCards.get(index);

        /*
         * Don't let the player click a card that
         * is already turned over.
         */
        if (selectedCard.faceUp
                || selectedCard.matched) {

            return;
        }

        selectedCard.faceUp = true;

        /*
         * This is the first card of the pair.
         */
        if (firstSelectedIndex == -1) {

            firstSelectedIndex = index;
            return;
        }

        /*
         * This is the second selected card.
         */
        secondSelectedIndex = index;

        GameCard firstCard = gameCards.get(firstSelectedIndex);

        GameCard secondCard = gameCards.get(secondSelectedIndex);

        /*
         * MATCH
         */
        if (firstCard.pairID == secondCard.pairID) {

            firstCard.matched = true;
            secondCard.matched = true;

            matchedPairs++;

            firstSelectedIndex = -1;
            secondSelectedIndex = -1;

            /*
             * All 13 pairs have been found.
             */
            if (matchedPairs == 13) {

                gameOver = true;
                playerWon = true;

                // Freeze the timer at the moment the player wins
                long millisecondsRemaining = gameEndTime - System.currentTimeMillis();

                finalSecondsRemaining = Math.max(0,
                        (int) Math.ceil(
                                millisecondsRemaining / 1000.0));
            }
        }

        /*
         * NO MATCH
         */
        else {

            /*
             * Leave the incorrect cards visible
             * briefly before updateGame() turns
             * them back over.
             */
            mismatchHideTime = System.currentTimeMillis()
                    + MISMATCH_DELAY;
        }
    }

    /*
     * Gets the location of a card on the
     * matching-game board.
     */
    private Rectangle getGameCardBounds(int index) {

        int cardHeight = getCardHeight();

        int column = index % COLUMNS;
        int row = index / COLUMNS;

        int x = START_X
                + column * (CARD_WIDTH + GAP);

        int y = GAME_START_Y
                + row * (cardHeight + GAP);

        return new Rectangle(
                x,
                y,
                CARD_WIDTH,
                cardHeight);
    }

    private int getCardHeight() {

        /*
         * Use a loaded card to preserve the
         * original card image proportions.
         */
        if (!originalOrder.isEmpty()) {

            BufferedImage card = originalOrder.get(0);

            return (int) (card.getHeight()
                    * ((double) CARD_WIDTH
                            / card.getWidth()));
        }

        return 60;
    }

    /*
     * Original shuffle button behavior.
     *
     * Pressing this button also leaves the
     * matching-game screen.
     */
    public void shuffleDeck() {

        matchingGameActive = false;
        gameOver = false;

        Collections.shuffle(cards);

        repaint();
    }

    /*
     * Original reset button behavior.
     */
    public void resetDeck() {

        matchingGameActive = false;
        gameOver = false;

        cards.clear();
        cards.addAll(originalOrder);

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        // Green card-table background
        g.setColor(new Color(10, 130, 42));
        g.fillRect(
                0,
                0,
                getWidth(),
                getHeight());

        /*
         * Draw either the normal deck or the
         * matching game.
         */
        if (matchingGameActive) {

            drawMatchingGame(g);

        } else {

            drawNormalDeck(g);
        }
    }

    /*
     * Draws the original 52-card deck.
     */
    private void drawNormalDeck(Graphics g) {

        for (int i = 0; i < cards.size(); i++) {

            BufferedImage card = cards.get(i);

            int cardHeight = (int) (card.getHeight()
                    * ((double) CARD_WIDTH
                            / card.getWidth()));

            int column = i % COLUMNS;
            int row = i / COLUMNS;

            int x = START_X
                    + column * (CARD_WIDTH + GAP);

            int y = NORMAL_START_Y
                    + row * (cardHeight + GAP);

            g.drawImage(
                    card,
                    x,
                    y,
                    CARD_WIDTH,
                    cardHeight,
                    null);
        }
    }

    /*
     * Draws the timer, status and matching cards.
     */
    private void drawMatchingGame(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        /*
         * Calculate remaining time.
         */
        int secondsRemaining;

        if (gameOver && playerWon) {

            // Keep showing the amount of time left
            // when the player won
            secondsRemaining = finalSecondsRemaining;

        } else {

            long millisecondsRemaining = gameEndTime
                    - System.currentTimeMillis();

            secondsRemaining = (int) Math.ceil(
                    millisecondsRemaining / 1000.0);

            if (secondsRemaining < 0) {
                secondsRemaining = 0;
            }
        }

        /*
         * Timer
         */
        g2.setColor(Color.WHITE);
        g2.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        22));

        g2.drawString(
                "Time: " + secondsRemaining,
                20,
                30);

        /*
         * Number of matches
         */
        g2.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        16));

        g2.drawString(
                "Pairs: "
                        + matchedPairs
                        + " / 13",
                180,
                30);

        /*
         * Draw all cards that have NOT already
         * been matched.
         */
        for (int i = 0; i < gameCards.size(); i++) {

            GameCard gameCard = gameCards.get(i);

            /*
             * A successfully matched pair
             * disappears from the board.
             */
            if (gameCard.matched) {
                continue;
            }

            Rectangle bounds = getGameCardBounds(i);

            BufferedImage imageToDraw;

            if (gameCard.faceUp) {

                imageToDraw = gameCard.image;

            } else {

                imageToDraw = cardBack;
            }

            /*
             * Draw the image if it loaded.
             */
            if (imageToDraw != null) {

                g2.drawImage(
                        imageToDraw,
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height,
                        null);
            }

            /*
             * If the card-back image failed to
             * load, draw a simple placeholder.
             */
            else {

                g2.setColor(
                        new Color(40, 60, 150));

                g2.fillRect(
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height);

                g2.setColor(Color.WHITE);

                g2.drawRect(
                        bounds.x,
                        bounds.y,
                        bounds.width,
                        bounds.height);
            }
        }

        /*
         * Success / failure message.
         */
        if (gameOver) {

            String message;

            if (playerWon) {
                message = "YOU WIN! All 13 pairs matched!";
            } else {
                message = "TIME'S UP! YOU LOSE!";
            }

            g2.setFont(
                    new Font(
                            "Arial",
                            Font.BOLD,
                            28));

            FontMetrics metrics = g2.getFontMetrics();

            int messageWidth = metrics.stringWidth(message);

            int x = (getWidth() - messageWidth) / 2;

            int y = getHeight() - 30;

            g2.setColor(Color.WHITE);

            g2.drawString(
                    message,
                    x,
                    y);
        }

        g2.dispose();
    }

    public static void main(String[] args)
            throws Exception {

        JFrame frame = new JFrame("Card Game");

        App app = new App();

        JButton shuffleButton = new JButton("Reshuffle Deck");

        JButton resetButton = new JButton("Reset Deck");

        JButton matchingGameButton = new JButton("Play Matching Game");

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(shuffleButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(matchingGameButton);

        /*
         * Original shuffle button
         */
        shuffleButton.addActionListener(e -> {

            app.shuffleDeck();
        });

        /*
         * Original reset button
         */
        resetButton.addActionListener(e -> {

            app.resetDeck();
        });

        /*
         * New matching-game button.
         *
         * It also acts as a restart button if a
         * matching game is already running.
         */
        matchingGameButton.addActionListener(e -> {

            app.startMatchingGame();
        });

        frame.setLayout(
                new BorderLayout());

        frame.add(
                app,
                BorderLayout.CENTER);

        frame.add(
                buttonPanel,
                BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);

        frame.setSize(
                640,
                480);

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }
}