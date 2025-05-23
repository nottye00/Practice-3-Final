package Interface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Nodo del árbol binario que representa un solo movimiento (blanco o negro) o nodo raíz "Partida"
class TurnNode {
    String turnNumber;  // número del turno o "Start"
    Move move;          // movimiento representado por este nodo, o null si es nodo raíz
    TurnNode left;      // hijo izquierdo: movimientos negros
    TurnNode right;     // hijo derecho: movimientos blancos

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

        // Nodo raíz "Partida" con color especial
        if (node.move == null) {
            g.setColor(new Color(200, 255, 200)); // verde claro para nodo partida
        } else {
            // Color según el color del movimiento
            if ("white".equals(node.move.getColor())) {
                g.setColor(new Color(220, 220, 255)); // azul claro para blanco
            } else {
                g.setColor(new Color(255, 230, 230)); // rojo claro para negro
            }
        }
        g.fillRoundRect(x, y, nodeWidth, nodeHeight, 20, 20);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, nodeWidth, nodeHeight, 20, 20);

        String text = (node.move == null) ? "Partida" : node.turnNumber + ". " + node.move.getNotation();
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);

        // Color de texto según tipo de nodo
        if (node.move == null) {
            g.setColor(Color.GREEN.darker());
        } else if ("white".equals(node.move.getColor())) {
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
        inputPanel.setBorder(BorderFactory.createTitledBorder("Ingrese la partida de ajedrez en notación SAN"));

        inputArea = new JTextArea(6, 50);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setText("");
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        parseButton = new JButton("Parsear y Visualizar");
        inputPanel.add(parseButton, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.NORTH);

        treePanel = new ChessTreePanel(null);
        treeScrollPane = new JScrollPane(treePanel);
        add(treeScrollPane, BorderLayout.CENTER);

        parseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String gameText = inputArea.getText().trim();
                if (gameText.isEmpty()) {
                    JOptionPane.showMessageDialog(ChessParserWithGUI.this, "Por favor ingrese una partida en notación SAN.", "Entrada requerida", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Se detectaron movimientos inválidos. Por favor revise su entrada.", "Entrada inválida", JOptionPane.ERROR_MESSAGE);
            treePanel.setRoot(null);
            return;
        }

        treePanel.setRoot(parser.getRoot());
        treePanel.setPreferredSize(new Dimension(1200, 800));
        treePanel.revalidate();
        treePanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JViewport viewport = treeScrollPane.getViewport();
            Dimension viewSize = viewport.getViewSize();
            Dimension extentSize = viewport.getExtentSize();
            int x = (viewSize.width - extentSize.width) / 2;
            int y = 0;
            viewport.setViewPosition(new Point(x, y));
        });
    }

    // Parser con nodo raíz "Partida" y estructura: derecha = blanco, izquierda = negro
    private static class ChessParser {
        private TurnNode root = null;
        private boolean isValid = true;

        public void parseGame(String game) {
            Pattern turnPattern = Pattern.compile("(\\d+)\\.\\s+([^\\s]+)(?:\\s+(?!\\d+\\.)([^\\s]+))?");
            Matcher matcher = turnPattern.matcher(game);

            root = new TurnNode("Start", null); // Nodo raíz "Partida"
            TurnNode lastWhiteNode = null;
            TurnNode lastBlackNode = null;
            isValid = true;

            while (matcher.find()) {
                String turnNum = matcher.group(1);
                String whiteNotation = matcher.group(2);
                String blackNotation = matcher.group(3);

                Move whiteMove = new Move(whiteNotation, "white");
                Move blackMove = blackNotation != null ? new Move(blackNotation, "black") : null;

                if (!whiteMove.isValid()) {
                    System.out.println("Turno " + turnNum + ": movimiento blanco inválido '" + whiteNotation + "'");
                    isValid = false;
                }
                if (blackMove != null && !blackMove.isValid()) {
                    System.out.println("Turno " + turnNum + ": movimiento negro inválido '" + blackNotation + "'");
                    isValid = false;
                }

                TurnNode whiteNode = new TurnNode(turnNum, whiteMove);
                TurnNode blackNode = blackMove != null ? new TurnNode(turnNum, blackMove) : null;

                if (lastWhiteNode == null) {
                    // Primer movimiento blanco hijo derecho de raíz
                    root.right = whiteNode;
                } else {
                    // Movimiento blanco hijo derecho del último blanco
                    lastWhiteNode.right = whiteNode;
                }
                lastWhiteNode = whiteNode;

                if (blackNode != null) {
                    if (lastBlackNode == null) {
                        // Primer movimiento negro hijo izquierdo de raíz
                        root.left = blackNode;
                    } else {
                        // Movimiento negro hijo izquierdo del último negro
                        lastBlackNode.left = blackNode;
                    }
                    lastBlackNode = blackNode;
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
