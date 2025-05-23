package Interface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Nuevo: Nodo del árbol binario que representa una jugada individual.
// Este nodo es más simple, solo guarda la notación de la jugada.
class MoveNode {
    String notation; // La notación SAN de la jugada (ej. "d4", "Nf6")
    MoveNode left;
    MoveNode right;

    public MoveNode(String notation) {
        this.notation = notation;
        this.left = null;
        this.right = null;
    }
}

// Clase que representa una jugada individual en SAN (reutilizada del código anterior)
class Move {
    private final String notation;
    private final String color; // "white" or "black"

    public Move(String notation, String color) {
        this.notation = notation;
        this.color = color;
    }

    public boolean isValid() {
        // Expresión regular mejorada para SAN (Simplified Algebraic Notation)
        // Esta es una versión más robusta que la inicial, pero SAN completo es complejo.
        // Incluye: movimientos de peón, capturas, movimientos de piezas, enroques,
        // promoción, jaques, y jaques mates.
        // NO maneja ambigüedad completa o notaciones complejas de comentarios/variaciones.
        String letter = "[a-h]";
        String number = "[1-8]";
        String square = letter + number;
        String piece = "[KQRBN]"; // King, Queen, Rook, Bishop, Knight
        String capture = "x?"; // optional capture
        String checkMate = "[+#]?"; // Check or Checkmate

        String pawnMove = "(" + square + "|" + letter + "x" + square + ")"; // e4 or exd4
        String pawnPromotion = pawnMove + "(=[QRBN])?"; // e8=Q
        String pieceMove = piece + letter + "?" + number + "?" + capture + square; // Nf3, Nbc3, Nxd4
        String castling = "O-O(-O)?"; // Kingside or Queenside castling

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

// Panel para dibujar el árbol binario de la partida
class ChessTreePanel extends JPanel {
    private MoveNode root; // Ahora el root es un MoveNode
    private final int nodeWidth = 140;
    private final int nodeHeight = 60;
    private final int verticalSpacing = 160;
    private final int horizontalSpacing = 120; // Espacio entre nodos del mismo nivel

    public ChessTreePanel(MoveNode root) { // Constructor adaptado a MoveNode
        this.root = root;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(3000, 2000)); // Tamaño inicial más grande
    }

    // Método para dibujar los nodos del árbol
    private void drawNode(Graphics2D g, MoveNode node, int x, int y, int depth) {
        if (node == null) return;

        // Dibuja el nodo actual
        // Colores basados en la profundidad para simular la imagen (raíz amarilla, otros grises/azules)
        g.setColor(new Color(230, 240, 255)); // Color por defecto (azul claro)
        if (depth == 0) {
            g.setColor(new Color(255, 230, 100)); // Amarillo para la raíz
        } else if (depth % 2 == 1) {
            g.setColor(new Color(200, 200, 200)); // Gris para niveles impares (simulando jugadas negras/alternativas)
        }
        g.fillRoundRect(x, y, nodeWidth, nodeHeight, 20, 20);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, nodeWidth, nodeHeight, 20, 20);

        // Dibuja la notación de la jugada en el centro del nodo
        String nodeText = node.notation;
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(nodeText);
        // Usar color azul para el texto
        g.setColor(Color.BLUE.darker());
        g.drawString(nodeText, x + (nodeWidth - textWidth) / 2, y + nodeHeight / 2 + fm.getAscent() / 2 - 5);

        // Calcular las posiciones de los hijos
        int childY = y + verticalSpacing;
        int leftChildX = x - nodeWidth / 2 - horizontalSpacing;
        int rightChildX = x + nodeWidth / 2 + horizontalSpacing;

        // Ajuste para centrar el padre si solo tiene un hijo
        if (node.left != null && node.right == null) {
            leftChildX = x; // Centrar el hijo izquierdo si no hay derecho
        } else if (node.left == null && node.right != null) {
            rightChildX = x; // Centrar el hijo derecho si no hay izquierdo
        } else if (node.left != null && node.right != null) {
            // Si tiene ambos hijos, ajustar para que el padre esté en el centro de ellos
            // Esto es una simplificación, un algoritmo de layout completo haría esto mejor.
            int midPoint = (leftChildX + nodeWidth / 2 + rightChildX + nodeWidth / 2) / 2;
            int currentMidPoint = x + nodeWidth / 2;
            int shift = midPoint - currentMidPoint;
            leftChildX += shift;
            rightChildX += shift;
        }


        // Dibujar líneas y nodos hijos
        g.setStroke(new BasicStroke(2)); // Línea sólida por defecto

        if (node.left != null) {
            g.setColor(Color.BLACK); // Línea sólida negra para el hijo izquierdo
            g.drawLine(x + nodeWidth / 2, y + nodeHeight, leftChildX + nodeWidth / 2, childY);
            drawNode(g, node.left, leftChildX, childY, depth + 1);
        }
        if (node.right != null) {
            g.setColor(Color.GRAY); // Línea gris punteada para el hijo derecho (simulando variación)
            float[] dashingPattern = {5f, 5f}; // 5 unidades de línea, 5 unidades de espacio
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

    public void setRoot(MoveNode root) { // Setter adaptado a MoveNode
        this.root = root;
        revalidate();
        repaint();
    }
}

// Clase principal con interfaz gráfica y procesamiento del árbol
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
        // Texto de ejemplo para facilitar la prueba
        inputArea.setText("1. d4 d5 2. Bf4 Nf6 3. e3 e6 4. c3 c5 5. Nd2 Nc6 6. Bd3 Bd6 7. Bg3 O-O 8. Ngf3 Qe7 9. Ne5 Nd7 10. Nxc6 bxc6 11. Bxd6 Qxd6 12. Nf3 a5 13. O-O Ba6 14. Re1 Rfb8 15. Rb1 Bxd3 16. Qxd3 c4 17. Qc2 f5 18. Nd2 Rb5 19. b3 cxb3 20. axb3 Rab8 21. Qa2 Qc7 22. c4 Rb4 23. cxd5 cxd5 24. Rbc1 Qb6 25. h3 a4 26. bxa4 Rb2 27. Qa3 Rxd2 28. Qe7 Qd8 29. Qxe6+ Kh8 30. Qxf5 Nf6 31. g4 Ne4 32. Rf1 h6 33. Rc6 Qh4 34. Rc8+ Rxc8 35. Qxc8+ Kh7 36. Qf5");

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        parseButton = new JButton("Parse and Visualize");
        inputPanel.add(parseButton, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.NORTH);

        treePanel = new ChessTreePanel(null); // Inicializar con null
        add(new JScrollPane(treePanel), BorderLayout.CENTER);

        parseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String gameText = inputArea.getText().trim();
                if (gameText.isEmpty()) {
                    JOptionPane.showMessageDialog(ChessParserWithGUI.this, "Please enter a chess game in SAN notation.", "Input Required", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // Llamar al nuevo parser
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

    // Parser interno: crea árbol binario usando la lógica de BFS
    private static class ChessParser {
        // Expresión regular para capturar el número de turno, la jugada blanca y la jugada negra (opcional)
        private static final String gameTurn = "(\\d+)\\.\\s*([^\\s]+)(?:\\s+(?!\\d+\\.)([^\\s]+))?";

        // Método auxiliar para validar una jugada usando la clase Move
        private static boolean proveMove(String moveNotation, String turn, String color) {
            Move move = new Move(moveNotation, color);
            return move.isValid();
        }

        // Nuevo método para construir el árbol a partir de la cadena de la partida
        public static MoveNode buildTreeFromMatch(String match) {
            Matcher matchTurns = Pattern.compile(gameTurn).matcher(match);
            MoveNode root = new MoveNode("Partida"); // Nodo raíz general para el inicio de la partida

            // Usaremos una cola para mantener el rastro de los nodos disponibles para insertar hijos
            Queue<MoveNode> availableNodes = new LinkedList<>();
            availableNodes.add(root);

            while (matchTurns.find()) {
                String turn = matchTurns.group(1);
                String white = matchTurns.group(2);
                String black = matchTurns.group(3); // puede ser null

                MoveNode whiteNode = null;
                MoveNode blackNode = null;

                // Crear nodos solo si la jugada es válida
                if (white != null && proveMove(white, turn, "white")) {
                    whiteNode = new MoveNode(turn + ". " + white); // Incluir el número de turno en la notación
                } else {
                    // Si una jugada blanca es inválida, el resto de la partida es inválida para este parser
                    System.err.println("Invalid white move: " + white + " in turn " + turn);
                    return null; // Retornar null para indicar un error
                }

                if (black != null && proveMove(black, turn, "black")) {
                    blackNode = new MoveNode(black);
                } else if (black != null) {
                    // Si una jugada negra es inválida
                    System.err.println("Invalid black move: " + black + " in turn " + turn);
                    return null; // Retornar null para indicar un error
                }

                // Insertar en el árbol binario usando BFS para encontrar el siguiente lugar disponible
                // Esta lógica llenará el árbol nivel por nivel, de izquierda a derecha.
                if (!availableNodes.isEmpty()) {
                    MoveNode parentNode = availableNodes.poll(); // Obtener el siguiente nodo disponible

                    if (whiteNode != null) {
                        parentNode.left = whiteNode;
                        availableNodes.add(whiteNode); // Añadir el nodo blanco a la cola para futuros hijos
                    }

                    if (blackNode != null) {
                        parentNode.right = blackNode;
                        availableNodes.add(blackNode); // Añadir el nodo negro a la cola para futuros hijos
                    }
                } else {
                    // Esto no debería ocurrir si el árbol se está construyendo correctamente
                    // y hay suficientes nodos disponibles en la cola.
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