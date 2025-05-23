package Interface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Nodo del árbol binario que representa un solo movimiento (blanco o negro)
class TurnNode {
    String turnNumber;  // número del turno (para referencia)
    Move move;          // movimiento representado por este nodo
    TurnNode left;      // hijo izquierdo: siguiente movimiento blanco
    TurnNode right;     // hijo derecho: siguiente movimiento negro

    public TurnNode(String turnNumber, Move move) {
        this.turnNumber = turnNumber;
        this.move = move;
        this.left = null;
        this.right = null;
    }
}

// Clase que representa una jugada individual en SAN
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
        String castle = "O-O(-O)?";
        String check = "[+#]?";
        String promote = "(=[QRBN])?";

        String advPawn = square + promote + check;
        String pawnTakes = letter + "x" + square + promote + check;
        String pawnMove = "(" + advPawn + "|" + pawnTakes + ")";

        String disambiguation = "(" + letter + "|" + number + "|" + letter + number + ")?";
        String pieceMove = piece + disambiguation + "x?" + square + promote + check;

        String move = "(" + castle + "|" + pawnMove + "|" + pieceMove + ")";
        return this.notation.matches(move);
    }

    public String getNotation() {
        return notation;
    }

    public String getColor() {
        return color;
    }
}

// Panel para dibujar el árbol binario de movimientos individuales
class ChessTreePanel extends JPanel {
    private TurnNode root;
    private final int nodeWidth = 140;
    private final int nodeHeight = 50;
    private final int verticalSpacing = 100;
    private final int horizontalSpacing = 30;

    public ChessTreePanel(TurnNode root) {
        this.root = root;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1200, 800));
    }

    private int calculateWidth(TurnNode node) {
        if (node == null) return 0;
        int leftWidth = calculateWidth(node.left);
        int rightWidth = calculateWidth(node.right);
        int width = Math.max(nodeWidth, leftWidth + rightWidth + horizontalSpacing);
        return width;
    }

    private void drawNode(Graphics2D g, TurnNode node, int x, int y) {
        if (node == null) return;

        int leftWidth = calculateWidth(node.left);
        int rightWidth = calculateWidth(node.right);

        int leftX = x - (rightWidth + nodeWidth) / 2 - horizontalSpacing / 2;
        int rightX = x + (leftWidth + nodeWidth) / 2 + horizontalSpacing / 2;
        int childY = y + verticalSpacing;

        g.setColor(Color.BLACK);
        if (node.left != null) {
            g.drawLine(x + nodeWidth / 2, y + nodeHeight, leftX + nodeWidth / 2, childY);
            drawNode(g, node.left, leftX, childY);
        }
        if (node.right != null) {
            g.drawLine(x + nodeWidth / 2, y + nodeHeight, rightX + nodeWidth / 2, childY);
            drawNode(g, node.right, rightX, childY);
        }

        // Color según el color del movimiento
        Color bgColor;
        if ("white".equals(node.move.getColor())) {
            bgColor = new Color(220, 220, 255); // azul claro para blanco
        } else {
            bgColor = new Color(255, 230, 230); // rojo claro para negro
        }
        g.setColor(bgColor);
        g.fillRoundRect(x, y, nodeWidth, nodeHeight, 20, 20);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, nodeWidth, nodeHeight, 20, 20);

        String text = node.turnNumber + ". " + node.move.getNotation();
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);

        // Color de texto según color de movimiento
        if ("white".equals(node.move.getColor())) {
            g.setColor(Color.BLUE.darker());
        } else {
            g.setColor(Color.RED.darker());
        }

        g.drawString(text, x + (nodeWidth - textWidth) / 2, y + nodeHeight / 2 + 5);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (root == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));
        int startX = getWidth() / 2 - nodeWidth / 2;
        int startY = 20;
        drawNode(g2, root, startX, startY);
    }

    public void setRoot(TurnNode root) {
        this.root = root;
        revalidate();
        repaint();
    }
}

// Clase principal con interfaz para entrada manual y visualización
public class ChessParserWithGUI extends JFrame {
    private JTextArea inputArea;
    private JButton parseButton;
    private ChessTreePanel treePanel;
    private JScrollPane treeScrollPane;

    public ChessParserWithGUI() {
        super("Chess SAN Parser and Visualizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 900);
        setLayout(new BorderLayout());

        // Panel de entrada
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Enter Chess Game in SAN Notation"));

        inputArea = new JTextArea(6, 50);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setText("");
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        parseButton = new JButton("Parse and Visualize");
        inputPanel.add(parseButton, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.NORTH);

        // Panel de visualización del árbol
        treePanel = new ChessTreePanel(null);
        treeScrollPane = new JScrollPane(treePanel);
        add(treeScrollPane, BorderLayout.CENTER);

        // Acción del botón
        parseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String gameText = inputArea.getText().trim();
                if (gameText.isEmpty()) {
                    JOptionPane.showMessageDialog(ChessParserWithGUI.this, "Please enter a chess game in SAN notation.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                parseAndDisplay(gameText);
            }
        });
    }

    private void parseAndDisplay(String gameText) {
        ChessParser parser = new ChessParser();
        parser.parseGame(gameText);

        if (!parser.isValid()) {
            JOptionPane.showMessageDialog(this, "Invalid moves detected. Please check your input.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            treePanel.setRoot(null);
            return;
        }

        treePanel.setRoot(parser.getRoot());
        treePanel.setPreferredSize(new Dimension(1200, 800));
        treePanel.revalidate();
        treePanel.repaint();
    }

    // Clase interna para parsing y construcción del árbol binario con hijos izquierda y derecha
    private static class ChessParser {
        private TurnNode root = null;
        private boolean isValid = true;

        public void parseGame(String game) {
            Pattern turnPattern = Pattern.compile("(\\d+)\\.\\s+([^\\s]+)(?:\\s+(?!\\d+\\.)([^\\s]+))?");
            Matcher matcher = turnPattern.matcher(game);

            TurnNode lastWhiteNode = null;
            TurnNode lastBlackNode = null;
            root = null;
            isValid = true;

            while (matcher.find()) {
                String turnNum = matcher.group(1);
                String whiteNotation = matcher.group(2);
                String blackNotation = matcher.group(3);

                Move whiteMove = new Move(whiteNotation, "white");
                Move blackMove = blackNotation != null ? new Move(blackNotation, "black") : null;

                if (!whiteMove.isValid()) {
                    System.out.println("Turn " + turnNum + ": invalid white move '" + whiteNotation + "'");
                    isValid = false;
                }
                if (blackMove != null && !blackMove.isValid()) {
                    System.out.println("Turn " + turnNum + ": invalid black move '" + blackNotation + "'");
                    isValid = false;
                }

                TurnNode whiteNode = new TurnNode(turnNum, whiteMove);
                if (root == null) {
                    root = whiteNode;
                    lastWhiteNode = whiteNode;
                } else {
                    // El siguiente movimiento blanco es hijo izquierdo del último movimiento negro
                    if (lastBlackNode != null) {
                        lastBlackNode.left = whiteNode;
                    } else {
                        // Si no hay negro anterior, enlaza blanco con último blanco (caso inicial)
                        lastWhiteNode.left = whiteNode;
                    }
                    lastWhiteNode = whiteNode;
                }

                if (blackMove != null) {
                    TurnNode blackNode = new TurnNode(turnNum, blackMove);
                    // El movimiento negro es hijo derecho del movimiento blanco actual
                    whiteNode.right = blackNode;
                    lastBlackNode = blackNode;
                } else {
                    lastBlackNode = null; // no hay negro en este turno
                }
            }
        }

        public TurnNode getRoot() {
            return root;
        }

        public boolean isValid() {
            return isValid;
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
