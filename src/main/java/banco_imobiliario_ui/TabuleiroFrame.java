package banco_imobiliario_ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import banco_imobiliario_controller.AppController;
import banco_imobiliario_controller.PlayerProfile;
import banco_imobiliario_models.GameModel;

/**
 * Janela do Tabuleiro – desenhado APENAS com Java2D na área central (BoardPanel).
 * Não há widgets dentro da área do tabuleiro. Os componentes Swing ficam FORA
 * do tabuleiro (lateral), incluindo o botão "Jogar" e os combos de teste.
 */
public final class TabuleiroFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AppController controller;
    private final BoardPanel boardPanel;
    private final DicePanel dicePanel;

    // Controles da lateral (fora do tabuleiro)
    private final JRadioButton rbAleatorio = new JRadioButton("Aleatório", true);
    private final JRadioButton rbManual    = new JRadioButton("Manual");
    private final JComboBox<Integer> cbD1  = new JComboBox<>(new Integer[]{1,2,3,4,5,6});
    private final JComboBox<Integer> cbD2  = new JComboBox<>(new Integer[]{1,2,3,4,5,6});
    private final JLabel lblStatus = new JLabel("Pronto.");

    public TabuleiroFrame(AppController controller) {
        this.controller = Objects.requireNonNull(controller);

        setTitle("Banco Imobiliário — Tabuleiro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Tamanho total da janela (tabuleiro 700 + barra lateral)
        setSize(new Dimension(980, 760));
        setLocationRelativeTo(null);

        boardPanel = new BoardPanel(controller);
        dicePanel  = new DicePanel(controller);

        JPanel controls = buildControls(); // painel lateral (fora do tabuleiro)

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(boardPanel, BorderLayout.CENTER);
        getContentPane().add(controls, BorderLayout.EAST);

        // estado inicial combos
        cbD1.setEnabled(false);
        cbD2.setEnabled(false);
        atualizarUIJogadorDaVez();
    }

    /** Painel lateral com controles e área dos dados (fora do tabuleiro). */
    private JPanel buildControls() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setPreferredSize(new Dimension(260, 760));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Topo: área dos dados (pintada com a cor do jogador da vez)
        p.add(dicePanel, BorderLayout.NORTH);

        // Centro: modo (aleatório/manual) e combos
        JPanel center = new JPanel();
        center.setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(2,2,2,2);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbAleatorio);
        bg.add(rbManual);

        rbAleatorio.addActionListener(e -> toggleManual(false));
        rbManual.addActionListener(e -> toggleManual(true));

        center.add(new JLabel("Modo dos dados:"), gbc);
        gbc.gridy++;
        center.add(rbAleatorio, gbc);
        gbc.gridy++;
        center.add(rbManual, gbc);

        gbc.gridy++;
        center.add(new JLabel("Dado 1:"), gbc);
        gbc.gridy++;
        center.add(cbD1, gbc);
        gbc.gridy++;
        center.add(new JLabel("Dado 2:"), gbc);
        gbc.gridy++;
        center.add(cbD2, gbc);

        p.add(center, BorderLayout.CENTER);

        // Rodapé: botão Jogar + status
        JPanel south = new JPanel(new BorderLayout(4,4));
        JButton btnJogar = new JButton("Jogar");
        btnJogar.addActionListener(this::onJogar);

        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.PLAIN, 12f));
        south.add(btnJogar, BorderLayout.NORTH);
        south.add(lblStatus, BorderLayout.SOUTH);

        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private void toggleManual(boolean manual) {
        cbD1.setEnabled(manual);
        cbD2.setEnabled(manual);
    }

    private void onJogar(ActionEvent e) {
        GameModel model = controller.getModel();

        // 1) Lançar dados (aleatório ou forçado)
        GameModel.ResultadoDados r;
        if (rbManual.isSelected()) {
            int d1 = (Integer) cbD1.getSelectedItem();
            int d2 = (Integer) cbD2.getSelectedItem();
            r = model.lancarDadosForcado(d1, d2);
        } else {
            r = model.lancarDados();
        }

        // 2) Exibir dados na área visual
        dicePanel.setDice(r.getD1(), r.getD2());

        // 3) Mover peão e aplicar efeitos obrigatórios (aluguel etc.)
        model.deslocarPiaoEAplicarObrigatorios();

        // 4) Encerrar vez se NÃO houver dupla (3ª dupla já termina a vez)
        model.encerrarAcoesDaVezEPassarTurno();

        // 5) Atualiza status, cor do jogador da vez e repinta peões
        atualizarUIJogadorDaVez();
        boardPanel.repaint();
    }

    /** Atualiza rótulos e a cor da área dos dados conforme o jogador da vez. */
    private void atualizarUIJogadorDaVez() {
        int id = controller.getModel().getJogadorDaVez();
        PlayerProfile p = controller.getPlayerProfiles().stream()
                .filter(pp -> pp.getId() == id).findFirst().orElse(null);

        String nome = (p != null ? p.getNome() : "J" + (id + 1));
        lblStatus.setText("Vez de: " + nome + "  (clique em Jogar)");
        dicePanel.setPlayerColor(p != null ? p.getCor() : new Color(200,200,200));
        dicePanel.repaint();
    }

    /** Exposto para repintar somente a área do tabuleiro quando necessário. */
    public void repaintBoard() {
        boardPanel.repaint();
        atualizarUIJogadorDaVez();
    }

    // =====================================================================================
    // Painel do TABULEIRO — SOMENTE Java2D
    // =====================================================================================
    private static final class BoardPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final AppController controller;
        private BufferedImage boardImage;           // imagem base do tabuleiro 700x700
        private final BufferedImage[] pinImgs = new BufferedImage[6]; // pinos 0..5

        private int side;                   // lado atual do tabuleiro desenhado (px)
        private int originX, originY;       // canto superior esquerdo do tabuleiro desenhado

        BoardPanel(AppController controller) {
            this.controller = controller;
            setPreferredSize(new Dimension(740, 740)); // 700 + margens
            setBackground(new Color(235, 235, 235));
            loadBoardImage();
            loadPinImages();
        }

        private void loadBoardImage() {
            try {
                try (InputStream is = getClass().getResourceAsStream("/assets/tabuleiro.png")) {
                    if (is != null) {
                        boardImage = ImageIO.read(is);
                        return;
                    }
                }
                File f = new File("assets/tabuleiro.png");
                if (f.exists()) {
                    boardImage = ImageIO.read(f);
                }
            } catch (Exception ex) {
                boardImage = null;
            }
        }

        /** Carrega pinos em assets/pinos/pin0.png..pin5.png. */
        private void loadPinImages() {
            for (int i = 0; i < pinImgs.length; i++) {
                try (InputStream is = getClass().getResourceAsStream("/assets/pinos/pin" + i + ".png")) {
                    if (is != null) {
                        pinImgs[i] = ImageIO.read(is);
                        continue;
                    }
                } catch (Exception ignored) {}
                try {
                    File f = new File("assets/pinos/pin" + i + ".png");
                    if (f.exists()) pinImgs[i] = ImageIO.read(f);
                } catch (Exception ignored) {}
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int availableW = getWidth()  - 40;
                int availableH = getHeight() - 40;
                side = Math.min(700, Math.min(availableW, availableH));
                originX = (getWidth()  - side) / 2;
                originY = (getHeight() - side) / 2;

                // Fundo
                g2.setColor(new Color(245, 245, 245));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Tabuleiro
                if (boardImage != null) {
                    Image scaled = boardImage.getScaledInstance(side, side, Image.SCALE_SMOOTH);
                    g2.drawImage(scaled, originX, originY, null);
                } else {
                    g2.setColor(new Color(220, 220, 220));
                    g2.fillRect(originX, originY, side, side);
                    g2.setColor(Color.DARK_GRAY);
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawRect(originX, originY, side, side);
                    g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
                    g2.drawString("tabuleiro.png não encontrado", originX + 20, originY + 40);
                }

                // Peões
                drawPawns(g2);

                // Overlay com ordem
                drawOrderOverlay(g2);

            } finally {
                g2.dispose();
            }
        }

        private void drawPawns(Graphics2D g2) {
            List<PlayerProfile> perfis = controller.getPlayerProfiles();
            if (perfis == null || perfis.isEmpty()) return;

            double cell = side / 11.0;
            List<Point2D.Double> centers = buildCellCenters();

            for (int i = 0; i < perfis.size(); i++) {
                PlayerProfile p = perfis.get(i);

                int pos;
                try {
                    pos = controller.getModel().getPosicaoJogador(p.getId());
                } catch (Exception ex) {
                    pos = 0; // fallback
                }
                pos = ((pos % 40) + 40) % 40;

                Point2D.Double base = centers.get(pos);
                Point offset = pawnOffsetForIndex(i, (int) cell);
                int cx = (int) Math.round(base.x) + offset.x;
                int cy = (int) Math.round(base.y) + offset.y;

                // Seleciona a imagem do pino pelo índice definido no diálogo
                int idx = p.getPawnIndex();
                BufferedImage pin = (idx >= 0 && idx < pinImgs.length) ? pinImgs[idx] : null;

                if (pin == null) {
                    // fallback: aproximação por cor (apenas se índice ausente)
                    idx = indexForColor(p.getCor());
                    pin = (idx >= 0 && idx < pinImgs.length) ? pinImgs[idx] : null;
                }

                if (pin != null) {
                    int pinH = (int) Math.round(cell * 0.85);
                    int pinW = (int) Math.round(pinH * (pin.getWidth() / (double) pin.getHeight()));
                    int x = cx - pinW / 2;
                    int y = cy - pinH + 4; // ancora pelo "bico" do pino
                    Image scaled = pin.getScaledInstance(pinW, pinH, Image.SCALE_SMOOTH);
                    g2.drawImage(scaled, x, y, null); // drawImage
                } else {
                    // Fallback: círculo colorido
                    int pawnR = (int) Math.round(cell * 0.28);
                    g2.setColor(p.getCor());
                    g2.fillOval(cx - pawnR, cy - pawnR, pawnR * 2, pawnR * 2);
                    g2.setColor(new Color(0,0,0,160));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(cx - pawnR, cy - pawnR, pawnR * 2, pawnR * 2);
                }
            }
        }

        /** Usado apenas como fallback quando o índice do pino não estiver disponível. */
        private int indexForColor(Color c) {
            if (c == null) return 5; // cinza
            Color[] targets = {
                new Color(220, 50, 50),   // pin0 vermelho
                new Color(60, 100, 200),  // pin1 azul
                new Color(245, 150, 40),  // pin2 laranja
                new Color(250, 220, 40),  // pin3 amarelo
                new Color(180, 60, 200),  // pin4 roxo
                new Color(150, 150, 150)  // pin5 cinza
            };
            double best = Double.MAX_VALUE;
            int bestIdx = 5;
            for (int i = 0; i < targets.length; i++) {
                double d = colorDistSq(c, targets[i]);
                if (d < best) { best = d; bestIdx = i; }
            }
            return bestIdx;
        }

        private double colorDistSq(Color a, Color b) {
            int dr = a.getRed()   - b.getRed();
            int dg = a.getGreen() - b.getGreen();
            int db = a.getBlue()  - b.getBlue();
            return dr*dr + dg*dg + db*db;
        }

        private void drawOrderOverlay(Graphics2D g2) {
            List<Integer> ordem = controller.getOrdemJogadores();
            List<PlayerProfile> perfis = controller.getPlayerProfiles();
            if (ordem == null || ordem.isEmpty() || perfis == null || perfis.isEmpty()) return;

            int pad = 12;
            int boxW = (int)(side * 0.36);
            int lineH = 22;
            int boxH = 28 + lineH * ordem.size();

            int x = originX + pad;
            int y = originY + pad;

            g2.setColor(new Color(0,0,0,110));
            g2.fillRoundRect(x, y, boxW, boxH, 12, 12);

            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
            g2.drawString("Ordem dos jogadores", x + 12, y + 22);

            g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            int cy = y + 22 + 10;
            for (int i = 0; i < ordem.size(); i++) {
                int id = ordem.get(i);
                PlayerProfile p = perfis.get(id);
                cy += lineH;

                g2.setColor(p.getCor());
                g2.fillRoundRect(x + 12, cy - 14, 18, 14, 4, 4);

                g2.setColor(Color.WHITE);
                String line = String.format("%dº  %s", i + 1, p.getNome());
                g2.drawString(line, x + 36, cy - 2);
            }
        }

        /** Deslocamentos para até 6 peões na mesma casa (evita overlap). */
        private Point pawnOffsetForIndex(int idx, int cellPx) {
            int d = Math.max(6, (int)Math.round(cellPx * 0.18));
            switch (idx) {
                case 0:  return new Point(0, 0);
                case 1:  return new Point(-d, -d);
                case 2:  return new Point(+d, -d);
                case 3:  return new Point(-d, +d);
                case 4:  return new Point(+d, +d);
                case 5:  return new Point(0, -2*d);
                default: return new Point(0, 0);
            }
        }

        /** Centros das 40 posições na borda 11x11 (0 no canto inf. direito, anti-horário). */
        private List<Point2D.Double> buildCellCenters() {
            List<Point2D.Double> list = new ArrayList<>(40);
            double cell = side / 11.0;
            double left   = originX + cell * 0.5;
            double right  = originX + side - cell * 0.5;
            double top    = originY + cell * 0.5;
            double bottom = originY + side - cell * 0.5;

            list.add(new Point2D.Double(right, bottom));        // 0
            for (int i = 1; i <= 9; i++) list.add(new Point2D.Double(right - cell * i, bottom)); // 1..9
            list.add(new Point2D.Double(left, bottom));         // 10
            for (int i = 1; i <= 9; i++) list.add(new Point2D.Double(left, bottom - cell * i));  // 11..19
            list.add(new Point2D.Double(left, top));            // 20
            for (int i = 1; i <= 9; i++) list.add(new Point2D.Double(left + cell * i, top));     // 21..29
            list.add(new Point2D.Double(right, top));           // 30
            for (int i = 1; i <= 9; i++) list.add(new Point2D.Double(right, top + cell * i));    // 31..39
            return list;
        }
    }

    // =====================================================================================
    // Área visual dos DADOS (fora do tabuleiro)
    // =====================================================================================
    private static final class DicePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final AppController controller;
        private Color playerColor = new Color(200,200,200);
        private int d1 = 1, d2 = 1; // último resultado exibido
        private Image[] diceImgs = new Image[7]; // 1..6

        DicePanel(AppController controller) {
            this.controller = controller;
            setPreferredSize(new Dimension(236, 160));
            setOpaque(false);
            loadDiceImages();
        }

        private void loadDiceImages() {
            for (int i = 1; i <= 6; i++) {
                diceImgs[i] = loadImage("/assets/dados/die_face_" + i + ".png");
                if (diceImgs[i] == null) {
                    File f = new File("assets/dados/die_face_" + i + ".png");
                    if (f.exists()) diceImgs[i] = new ImageIcon(f.getAbsolutePath()).getImage();
                }
            }
        }

        private Image loadImage(String resourcePath) {
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is != null) return ImageIO.read(is);
            } catch (Exception ignored) {}
            return null;
        }

        void setPlayerColor(Color c) {
            this.playerColor = (c != null ? c : new Color(200,200,200));
        }

        void setDice(int d1, int d2) {
            if (d1 >= 1 && d1 <= 6) this.d1 = d1;
            if (d2 >= 1 && d2 <= 6) this.d2 = d2;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                Color bg = new Color(playerColor.getRed(), playerColor.getGreen(), playerColor.getBlue(), 80);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 20, 20);

                g2.setColor(Color.DARK_GRAY);
                g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
                g2.drawString("Dados", 12, 18);

                int imgSize = 64;
                int x1 = 18;
                int y1 = 30;
                int x2 = x1 + imgSize + 12;
                int y2 = y1;

                drawDie(g2, d1, x1, y1, imgSize);
                drawDie(g2, d2, x2, y2, imgSize);

                g2.setFont(getFont().deriveFont(Font.PLAIN, 13f));
                String msg = "Soma: " + (d1 + d2) + (d1 == d2 ? "  •  Dupla!" : "");
                g2.setColor(Color.BLACK);
                g2.drawString(msg, 12, h - 12);
            } finally {
                g2.dispose();
            }
        }

        private void drawDie(Graphics2D g2, int value, int x, int y, int size) {
            Image img = (value >= 1 && value <= 6) ? diceImgs[value] : null;
            if (img != null) {
                g2.drawImage(img, x, y, size, size, null); // drawImage
            } else {
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(x, y, size, size, 12, 12);
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, size, size, 12, 12);
                g2.setFont(getFont().deriveFont(Font.BOLD, (float)(size * 0.5)));
                String t = String.valueOf(value);
                int tw = g2.getFontMetrics().stringWidth(t);
                int th = g2.getFontMetrics().getAscent();
                g2.drawString(t, x + (size - tw)/2, y + (size + th)/2 - 6);
            }
        }
    }
}
