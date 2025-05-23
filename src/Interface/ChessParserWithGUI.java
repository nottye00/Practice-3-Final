package Interface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MoveNode {
    String notation;
    MoveNode left;
    MoveNode right;

    public MoveNode(String notation) {
        this.notation = notation;
        this.left = null;
        this.right = null;
    }
}

class Move {
    private final String notation;
    private final String color;

    public Move(String notation, String color) {
        this.notation = notation;
        this.color = color;
    }

    public boolean isValid() {
        String letter = "[a-h]";
        String number = "[1-8]";
        String square = letter + number;
        String piece = "[KQRBN]";
        String capture = "x?";
        String checkMate = "[+#]?";

        String pawnMove = "(" + square + "|" + letter + "x" + square + ")";
        String pawnPromotion = pawnMove + "(=[QRBN])?";
        String pieceMove = piece + letter + "?" + number + "?" + capture + square;
        String castling = "O-O(-O)?";

        String validMovePattern = "^(" + pawnPromotion + "|" + pieceMove + "|" + castling + ")" + checkMate + "$";

        return this.notation.matches(validMovePattern);
    }

    public String getNotation() {
        return notation;
    }

    public String getColor() {
        return color;
    }
}

class ChessTreePanel extends JPanel {
    private MoveNode root;
    private final int nodeWidth = 140;
    private final int nodeHeight = 60;
    private final int verticalSpacing = 160;
    private final int horizontalSpacing = 120;

    public ChessTreePanel(MoveNode root) {
        this.root = root;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(3000, 2000));
    }

    private void drawNode(Graphics2D g, MoveNode node, int x, int y, int depth) {
        if (node == null) return;

        g.setColor(new Color(230, 240, 255));
        if (depth == 0) {
            g.setColor(new Color(255, 230, 100));
        } else if (depth % 2 == 1) {
            g.setColor(new Color(200, 200, 200));
        }
        g.fillRoundRect(x, y, nodeWidth, nodeHeight, 20, 20);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, nodeWidth, nodeHeight, 20, 20);

        String nodeText = node.notation;
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(nodeText);
        g.setColor(Color.BLUE.darker());
        g.drawString(nodeText, x + (nodeWidth - textWidth) / 2, y + nodeHeight / 2 + fm.getAscent() / 2 - 5);

        int childY = y + verticalSpacing;
        int leftChildX = x - nodeWidth / 2 - horizontalSpacing;
        int rightChildX = x + nodeWidth / 2 + horizontalSpacing;

        if (node.left != null && node.right == null) {
            leftChildX = x;
        } else if (node.left == null && node.right != null) {
            rightChildX = x;
        } else if (node.left != null && node.right != null) {
            int midPoint = (leftChildX + nodeWidth / 2 + rightChildX + nodeWidth / 2) / 2;
            int currentMidPoint = x + nodeWidth / 2;
            int shift = midPoint - currentMidPoint;
            leftChildX += shift;
            rightChildX += shift;
        }


        g.setStroke(new BasicStroke(2));

        if (node.left != null) {
            g.setColor(Color.BLACK);
            g.drawLine(x + nodeWidth / 2, y + nodeHeight, leftChildX + nodeWidth / 2, childY);
            drawNode(g, node.left, leftChildX, childY, depth + 1);
        }
        if (node.right != null) {
            g.setColor(Color.GRAY);
            float[] dashingPattern = {5f, 5f};
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashingPattern, 0.0f));
            g.drawLine(x + nodeWidth / 2, y + nodeHeight, rightChildX + nodeWidth / 2, childY);
            drawNode(g, node.right, rightChildX, childY, depth + 1);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (root == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));

        int startX = getWidth() / 2 - nodeWidth / 2;
        int startY = 20;
        drawNode(g2, root, startX, startY, 0);
    }

    public void setRoot(MoveNode root) {
        this.root = root;
        revalidate();
        repaint();
    }
}

public class ChessParserWithGUI extends JFrame {
    private JTextArea inputArea;
    private JButton parseButton;
    private ChessTreePanel treePanel;

    public ChessParserWithGUI() {
        super("Chess SAN Parser and Visualizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 1000);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Enter Chess Game in SAN Notation"));

        inputArea = new JTextArea(8, 60);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setText("");

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        parseButton = new JButton("Parse and Visualize");
        inputPanel.add(parseButton, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.NORTH);

        treePanel = new ChessTreePanel(null);
        add(new JScrollPane(treePanel), BorderLayout.CENTER);

        parseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String gameText = inputArea.getText().trim();
                if (gameText.isEmpty()) {
                    JOptionPane.showMessageDialog(ChessParserWithGUI.this, "Please enter a chess game in SAN notation.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                MoveNode gameTree = ChessParser.buildTreeFromMatch(gameText);
                if (gameTree == null) {
                    JOptionPane.showMessageDialog(ChessParserWithGUI.this, "Error parsing game or no valid moves found.", "Parsing Error", JOptionPane.ERROR_MESSAGE);
                    treePanel.setRoot(null);
                } else {
                    treePanel.setRoot(gameTree);
                }
            }
        });
    }

    private static class ChessParser {
        private static final String gameTurn = "(\\d+)\\.\\s*([^\\s]+)(?:\\s+(?!\\d+\\.)([^\\s]+))?";

        private static boolean proveMove(String moveNotation, String turn, String color) {
            Move move = new Move(moveNotation, color);
            return move.isValid();
        }

        public static MoveNode buildTreeFromMatch(String match) {
            Matcher matchTurns = Pattern.compile(gameTurn).matcher(match);
            MoveNode root = new MoveNode("Partida");

            Queue<MoveNode> availableNodes = new LinkedList<>();
            availableNodes.add(root);

            while (matchTurns.find()) {
                String turn = matchTurns.group(1);
                String white = matchTurns.group(2);
                String black = matchTurns.group(3);

                MoveNode whiteNode = null;
                MoveNode blackNode = null;

                if (white != null && proveMove(white, turn, "white")) {
                    whiteNode = new MoveNode(turn + ". " + white);
                } else {
                    System.err.println("Invalid white move: " + white + " in turn " + turn);
                    return null;
                }

                if (black != null && proveMove(black, turn, "black")) {
                    blackNode = new MoveNode(black);
                } else if (black != null) {
                    System.err.println("Invalid black move: " + black + " in turn " + turn);
                    return null;
                }

                if (!availableNodes.isEmpty()) {
                    MoveNode parentNode = availableNodes.poll();

                    if (whiteNode != null) {
                        parentNode.left = whiteNode;
                        availableNodes.add(whiteNode);
                    }

                    if (blackNode != null) {
                        parentNode.right = blackNode;
                        availableNodes.add(blackNode);
                    }
                } else {
                    System.err.println("No available parent node to insert moves.");
                    break;
                }
            }

            return root;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChessParserWithGUI gui = new ChessParserWithGUI();
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
        });
    }
}
