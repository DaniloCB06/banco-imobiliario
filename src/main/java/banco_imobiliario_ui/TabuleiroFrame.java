package banco_imobiliario_ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;

public final class TabuleiroFrame extends javax.swing.JFrame {
    private static final long serialVersionUID = 1L;

    private final banco_imobiliario_controller.AppController controller;
    private final BoardPanel boardPanel;
    private final DicePanel dicePanel;
    private final OrderPanel orderPanel;

    // Controles da lateral (fora do tabuleiro)
    private final javax.swing.JRadioButton rbAleatorio = new javax.swing.JRadioButton("Aleatório", true);
    private final javax.swing.JRadioButton rbManual    = new javax.swing.JRadioButton("Manual");
    private final javax.swing.JComboBox<Integer> cbD1  = new javax.swing.JComboBox<>(new Integer[]{1,2,3,4,5,6});
    private final javax.swing.JComboBox<Integer> cbD2  = new javax.swing.JComboBox<>(new Integer[]{1,2,3,4,5,6});
    private final javax.swing.JLabel lblStatus = new javax.swing.JLabel("Pronto.");

    public TabuleiroFrame(banco_imobiliario_controller.AppController controller) {
        this.controller = java.util.Objects.requireNonNull(controller);

        setTitle("Banco Imobiliário — Tabuleiro");
        setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Tamanho total da janela (tabuleiro 700 + barra lateral)
        setSize(new java.awt.Dimension(1000, 780)); 
        setLocationRelativeTo(null);

        boardPanel = new BoardPanel(controller);
        dicePanel  = new DicePanel(controller);
        orderPanel = new OrderPanel(controller);

        javax.swing.JPanel controls = buildControls(); // painel lateral (fora do tabuleiro)

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(boardPanel, BorderLayout.CENTER);
        getContentPane().add(controls, BorderLayout.EAST);

        // estado inicial combos
        cbD1.setEnabled(false);
        cbD2.setEnabled(false);
        atualizarUIJogadorDaVez();
    }

    /** Painel lateral com controles e áreas dos dados/ordem (fora do tabuleiro). */
    private javax.swing.JPanel buildControls() {
        javax.swing.JPanel p = new javax.swing.JPanel(new BorderLayout(8, 8));
        p.setPreferredSize(new java.awt.Dimension(260, 780));
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Topo: empilha área dos dados + ordem dos jogadores
        javax.swing.JPanel topStack = new javax.swing.JPanel();
        topStack.setOpaque(false);
        topStack.setLayout(new javax.swing.BoxLayout(topStack, javax.swing.BoxLayout.Y_AXIS));
        dicePanel.setAlignmentX(0f);
        orderPanel.setAlignmentX(0f);
        topStack.add(dicePanel);
        topStack.add(javax.swing.Box.createVerticalStrut(8));
        topStack.add(orderPanel);
        p.add(topStack, BorderLayout.NORTH);

        // Centro: modo (aleatório/manual) e combos
        javax.swing.JPanel center = new javax.swing.JPanel();
        center.setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(2,2,2,2);

        javax.swing.ButtonGroup bg = new javax.swing.ButtonGroup();
        bg.add(rbAleatorio);
        bg.add(rbManual);

        rbAleatorio.addActionListener(e -> toggleManual(false));
        rbManual.addActionListener(e -> toggleManual(true));

        center.add(new javax.swing.JLabel("Modo dos dados:"), gbc);
        gbc.gridy++;
        center.add(rbAleatorio, gbc);
        gbc.gridy++;
        center.add(rbManual, gbc);

        gbc.gridy++;
        center.add(new javax.swing.JLabel("Dado 1:"), gbc);
        gbc.gridy++;
        center.add(cbD1, gbc);
        gbc.gridy++;
        center.add(new javax.swing.JLabel("Dado 2:"), gbc);
        gbc.gridy++;
        center.add(cbD2, gbc);

        p.add(center, BorderLayout.CENTER);

        // rodapé: botão Jogar + status
        javax.swing.JPanel south = new javax.swing.JPanel(new BorderLayout(4,4));
        javax.swing.JButton btnJogar = new javax.swing.JButton("Jogar");
        btnJogar.addActionListener(this::onJogar);

        lblStatus.setFont(lblStatus.getFont().deriveFont(java.awt.Font.PLAIN, 12f));
        south.add(btnJogar, BorderLayout.NORTH);
        south.add(lblStatus, BorderLayout.SOUTH);

        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private void toggleManual(boolean manual) {
        cbD1.setEnabled(manual);
        cbD2.setEnabled(manual);
    }

    private void onJogar(java.awt.event.ActionEvent e) {
        banco_imobiliario_models.GameModel model = controller.getModel();

        // 1) Lançar dados (aleatório ou forçado)
        banco_imobiliario_models.GameModel.ResultadoDados r;
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

        // 4) Encerrar vez se não houver dupla (3ª dupla já termina a vez)
        model.encerrarAcoesDaVezEPassarTurno();

        // 5) Atualiza status, cor do jogador da vez e repinta peões
        atualizarUIJogadorDaVez();
        boardPanel.repaint();
    }

    /** Atualiza rótulos e a cor da área dos dados conforme o jogador da vez. */
    private void atualizarUIJogadorDaVez() {
        int id = controller.getModel().getJogadorDaVez();
        banco_imobiliario_controller.PlayerProfile p = controller.getPlayerProfiles().stream()
                .filter(pp -> pp.getId() == id).findFirst().orElse(null);

        String nome = (p != null ? p.getNome() : "J" + (id + 1));
        lblStatus.setText("Vez de: " + nome + "  (clique em Jogar)");
        dicePanel.setPlayerColor(p != null ? p.getCor() : new Color(200,200,200));
        dicePanel.repaint();
        orderPanel.repaint();
    }

    /** Exposto para repintar somente a área do tabuleiro quando necessário. */
    public void repaintBoard() {
        boardPanel.repaint();
        atualizarUIJogadorDaVez();
        orderPanel.repaint();
    }

    // =====================================================================================
    // Painel do TABULEIRO
    // =====================================================================================
    private static final class BoardPanel extends javax.swing.JPanel {
        private static final long serialVersionUID = 1L;

        private final banco_imobiliario_controller.AppController controller;
        private java.awt.image.BufferedImage boardImage;           // imagem base 700x700
        private final java.awt.image.BufferedImage[] pinImgs = new java.awt.image.BufferedImage[6]; // pinos 0..5

        private int side;                   // lado atual do tabuleiro desenhado (px)
        private int originX, originY;       // canto superior esquerdo do tabuleiro desenhado

        BoardPanel(banco_imobiliario_controller.AppController controller) {
            this.controller = controller;
            setPreferredSize(new java.awt.Dimension(740, 740)); // 700 + margens
            setBackground(new Color(235, 235, 235));
            loadBoardImage();
            loadPinImages();
        }

        private void loadBoardImage() {
            try {
                try (java.io.InputStream is = getClass().getResourceAsStream("/assets/tabuleiro.png")) {
                    if (is != null) {
                        boardImage = javax.imageio.ImageIO.read(is);
                        return;
                    }
                }
                java.io.File f = new java.io.File("assets/tabuleiro.png");
                if (f.exists()) {
                    boardImage = javax.imageio.ImageIO.read(f);
                }
            } catch (Exception ex) {
                boardImage = null;
            }
        }

        /** Carrega pinos em assets/pinos/pin0.png..pin5.png. */
        private void loadPinImages() {
            for (int i = 0; i < pinImgs.length; i++) {
                try (java.io.InputStream is = getClass().getResourceAsStream("/assets/pinos/pin" + i + ".png")) {
                    if (is != null) {
                        pinImgs[i] = javax.imageio.ImageIO.read(is);
                        continue;
                    }
                } catch (Exception ignored) {}
                try {
                    java.io.File f = new java.io.File("assets/pinos/pin" + i + ".png");
                    if (f.exists()) pinImgs[i] = javax.imageio.ImageIO.read(f);
                } catch (Exception ignored) {}
            }
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

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
                    java.awt.Image scaled = boardImage.getScaledInstance(side, side, java.awt.Image.SCALE_SMOOTH);
                    g2.drawImage(scaled, originX, originY, null);
                } else {
                    g2.setColor(new Color(220, 220, 220));
                    g2.fillRect(originX, originY, side, side);
                    g2.setColor(java.awt.Color.DARK_GRAY);
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawRect(originX, originY, side, side);
                    g2.setFont(getFont().deriveFont(java.awt.Font.BOLD, 18f));
                    g2.drawString("tabuleiro.png não encontrado", originX + 20, originY + 40);
                }

                // Peões
                drawPawns(g2);

            } finally {
                g2.dispose();
            }
        }

        private void drawPawns(java.awt.Graphics2D g2) {
            java.util.List<banco_imobiliario_controller.PlayerProfile> perfis = controller.getPlayerProfiles();
            if (perfis == null || perfis.isEmpty()) return;

            double cell = side / 11.0;
            java.util.List<java.awt.geom.Point2D.Double> centers = buildCellCenters();

            for (int i = 0; i < perfis.size(); i++) {
                banco_imobiliario_controller.PlayerProfile p = perfis.get(i);

                int pos;
                try {
                    pos = controller.getModel().getPosicaoJogador(p.getId());
                } catch (Exception ex) {
                    pos = 0; // fallback
                }
                pos = ((pos % 40) + 40) % 40;

                java.awt.geom.Point2D.Double base = centers.get(pos);
                java.awt.Point offset = pawnOffsetForIndex(i, (int) cell);
                int cx = (int) Math.round(base.x) + offset.x;
                int cy = (int) Math.round(base.y) + offset.y;

                // Seleciona a imagem do pino pelo índice definido no diálogo
                int idx = p.getPawnIndex();
                java.awt.image.BufferedImage pin = (idx >= 0 && idx < pinImgs.length) ? pinImgs[idx] : null;

                if (pin == null) {
                    idx = indexForColor(p.getCor());
                    pin = (idx >= 0 && idx < pinImgs.length) ? pinImgs[idx] : null;
                }

                if (pin != null) {
                    int pinH = (int) Math.round(cell * 0.85);
                    int pinW = (int) Math.round(pinH * (pin.getWidth() / (double) pin.getHeight()));
                    int x = cx - pinW / 2;
                    int y = cy - pinH + 4; // ancora pelo "bico" do pino
                    java.awt.Image scaled = pin.getScaledInstance(pinW, pinH, java.awt.Image.SCALE_SMOOTH);
                    g2.drawImage(scaled, x, y, null); // drawImage
                } else {
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
        private int indexForColor(java.awt.Color c) {
            if (c == null) return 5; // cinza
            java.awt.Color[] targets = {
                new java.awt.Color(220, 50, 50),   // pin0 vermelho
                new java.awt.Color(60, 100, 200),  // pin1 azul
                new java.awt.Color(245, 150, 40),  // pin2 laranja
                new java.awt.Color(250, 220, 40),  // pin3 amarelo
                new java.awt.Color(180, 60, 200),  // pin4 roxo
                new java.awt.Color(150, 150, 150)  // pin5 cinza
            };
            double best = java.lang.Double.MAX_VALUE;
            int bestIdx = 5;
            for (int i = 0; i < targets.length; i++) {
                double d = colorDistSq(c, targets[i]);
                if (d < best) { best = d; bestIdx = i; }
            }
            return bestIdx;
        }

        private double colorDistSq(java.awt.Color a, java.awt.Color b) {
            int dr = a.getRed()   - b.getRed();
            int dg = a.getGreen() - b.getGreen();
            int db = a.getBlue()  - b.getBlue();
            return dr*dr + dg*dg + db*db;
        }

        private java.awt.Point pawnOffsetForIndex(int idx, int cellPx) {
            int d = java.lang.Math.max(6, (int)java.lang.Math.round(cellPx * 0.18));
            switch (idx) {
                case 0:  return new java.awt.Point(0, 0);
                case 1:  return new java.awt.Point(-d, -d);
                case 2:  return new java.awt.Point(+d, -d);
                case 3:  return new java.awt.Point(-d, +d);
                case 4:  return new java.awt.Point(+d, +d);
                case 5:  return new java.awt.Point(0, -2*d);
                default: return new java.awt.Point(0, 0);
            }
        }

        private java.util.List<java.awt.geom.Point2D.Double> buildCellCenters() {
            java.util.List<java.awt.geom.Point2D.Double> list = new java.util.ArrayList<>(40);
            double cell = side / 11.0;
            double left   = originX + cell * 0.5;
            double right  = originX + side - cell * 0.5;
            double top    = originY + cell * 0.5;
            double bottom = originY + side - cell * 0.5;

            list.add(new java.awt.geom.Point2D.Double(right, bottom));        // 0
            for (int i = 1; i <= 9; i++) list.add(new java.awt.geom.Point2D.Double(right - cell * i, bottom)); // 1..9
            list.add(new java.awt.geom.Point2D.Double(left, bottom));         // 10
            for (int i = 1; i <= 9; i++) list.add(new java.awt.geom.Point2D.Double(left, bottom - cell * i));  // 11..19
            list.add(new java.awt.geom.Point2D.Double(left, top));            // 20
            for (int i = 1; i <= 9; i++) list.add(new java.awt.geom.Point2D.Double(left + cell * i, top));     // 21..29
            list.add(new java.awt.geom.Point2D.Double(right, top));           // 30
            for (int i = 1; i <= 9; i++) list.add(new java.awt.geom.Point2D.Double(right, top + cell * i));    // 31..39
            return list;
        }
    }

    // =====================================================================================
    // Área visual dos DADOS (fora do tabuleiro)
    // =====================================================================================
    private static final class DicePanel extends javax.swing.JPanel {
        private static final long serialVersionUID = 1L;

        private final banco_imobiliario_controller.AppController controller;
        private java.awt.Color playerColor = new java.awt.Color(200,200,200);
        private int d1 = 1, d2 = 1; // último resultado exibido
        private java.awt.Image[] diceImgs = new java.awt.Image[7]; // 1..6

        DicePanel(banco_imobiliario_controller.AppController controller) {
            this.controller = controller;
            setPreferredSize(new java.awt.Dimension(236, 160));
            setOpaque(false);
            loadDiceImages();
        }

        private void loadDiceImages() {
            for (int i = 1; i <= 6; i++) {
                diceImgs[i] = loadImage("/assets/dados/die_face_" + i + ".png");
                if (diceImgs[i] == null) {
                    java.io.File f = new java.io.File("assets/dados/die_face_" + i + ".png");
                    if (f.exists()) diceImgs[i] = new javax.swing.ImageIcon(f.getAbsolutePath()).getImage();
                }
            }
        }

        private java.awt.Image loadImage(String resourcePath) {
            try (java.io.InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is != null) return javax.imageio.ImageIO.read(is);
            } catch (Exception ignored) {}
            return null;
        }

        void setPlayerColor(java.awt.Color c) {
            this.playerColor = (c != null ? c : new java.awt.Color(200,200,200));
        }

        void setDice(int d1, int d2) {
            if (d1 >= 1 && d1 <= 6) this.d1 = d1;
            if (d2 >= 1 && d2 <= 6) this.d2 = d2;
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                java.awt.Color bg = new java.awt.Color(playerColor.getRed(), playerColor.getGreen(), playerColor.getBlue(), 80);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 20, 20);

                g2.setColor(java.awt.Color.DARK_GRAY);
                g2.setFont(getFont().deriveFont(java.awt.Font.BOLD, 14f));
                g2.drawString("Dados", 12, 18);

                int imgSize = 64;
                int x1 = 18;
                int y1 = 30;
                int x2 = x1 + imgSize + 12;
                int y2 = y1;

                drawDie(g2, d1, x1, y1, imgSize);
                drawDie(g2, d2, x2, y2, imgSize);

                g2.setFont(getFont().deriveFont(java.awt.Font.PLAIN, 13f));
                String msg = "Soma: " + (d1 + d2) + (d1 == d2 ? "  •  Dupla!" : "");
                g2.setColor(java.awt.Color.BLACK);
                g2.drawString(msg, 12, h - 12);
            } finally {
                g2.dispose();
            }
        }

        private void drawDie(java.awt.Graphics2D g2, int value, int x, int y, int size) {
            java.awt.Image img = (value >= 1 && value <= 6) ? diceImgs[value] : null;
            if (img != null) {
                g2.drawImage(img, x, y, size, size, null); // drawImage
            } else {
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRoundRect(x, y, size, size, 12, 12);
                g2.setColor(java.awt.Color.GRAY);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, size, size, 12, 12);
                g2.setFont(getFont().deriveFont(java.awt.Font.BOLD, (float)(size * 0.5)));
                String t = String.valueOf(value);
                int tw = g2.getFontMetrics().stringWidth(t);
                int th = g2.getFontMetrics().getAscent();
                g2.drawString(t, x + (size - tw)/2, y + (size + th)/2 - 6);
            }
        }
    }

    // =====================================================================================
    // Painel da ORDEM DOS JOGADORES (lateral, abaixo dos dados)
    // =====================================================================================
    private static final class OrderPanel extends javax.swing.JPanel {
        private static final long serialVersionUID = 1L;
        private final banco_imobiliario_controller.AppController controller;

        OrderPanel(banco_imobiliario_controller.AppController controller) {
            this.controller = controller;
            setOpaque(false);
        }

        /** Altura preferida dinâmica (3 a 6 jogadores) baseada nas fontes. */
        @Override public java.awt.Dimension getPreferredSize() {
            int n = 6; // suporte máximo
            java.util.List<Integer> ordem = controller.getOrdemJogadores();
            if (ordem != null && !ordem.isEmpty()) n = java.lang.Math.min(6, java.lang.Math.max(3, ordem.size()));

            // Calcula com métricas reais
            java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = bi.createGraphics();
            java.awt.Font titleFont = getFont().deriveFont(java.awt.Font.BOLD, 16f);
            java.awt.Font listFont  = getFont().deriveFont(java.awt.Font.PLAIN, 14f);
            g2.setFont(titleFont);
            int titleH = g2.getFontMetrics().getAscent() + g2.getFontMetrics().getDescent();
            g2.setFont(listFont);
            int lineH = g2.getFontMetrics().getAscent() + g2.getFontMetrics().getDescent() + 4;
            g2.dispose();

            int innerPad = 10, gapTitle = 8;
            int h = innerPad + titleH + gapTitle + n * lineH + innerPad;

            // Largura fixa da lateral (~igual aos dados)
            return new java.awt.Dimension(236, h);
        }

        @Override protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

                java.util.List<Integer> ordem = controller.getOrdemJogadores();
                java.util.List<banco_imobiliario_controller.PlayerProfile> perfis = controller.getPlayerProfiles();
                if (ordem == null || ordem.isEmpty() || perfis == null || perfis.isEmpty()) return;

                int w = getWidth();
                int h = getHeight();

                int innerPad = 10;
                int gapTitle = 8;
                int badgeW   = 18, badgeH = 14;

                java.awt.Font titleFont = getFont().deriveFont(java.awt.Font.BOLD, 16f);
                java.awt.Font listFont  = getFont().deriveFont(java.awt.Font.PLAIN, 14f);

                // Fundo translúcido do box
                g2.setColor(new java.awt.Color(0, 0, 0, 110));
                g2.fillRoundRect(0, 0, w, h, 12, 12);

                // Título
                g2.setColor(java.awt.Color.WHITE);
                g2.setFont(titleFont);
                int titleAscent = g2.getFontMetrics().getAscent();
                int titleDescent = g2.getFontMetrics().getDescent();
                int titleH = titleAscent + titleDescent;
                int titleBaseline = innerPad + titleAscent;
                g2.drawString("Ordem dos jogadores", innerPad + 2, titleBaseline);

                // Lista
                g2.setFont(listFont);
                int fmAscent  = g2.getFontMetrics().getAscent();
                int fmDescent = g2.getFontMetrics().getDescent();
                int lineH = fmAscent + fmDescent + 4;

                int cy = innerPad + titleH + gapTitle;
                for (int i = 0; i < ordem.size(); i++) {
                    int id = ordem.get(i);
                    banco_imobiliario_controller.PlayerProfile p = perfis.get(id);

                    cy += lineH; // baseline da linha i

                    // quadradinho da cor do jogador
                    g2.setColor(p.getCor());
                    g2.fillRoundRect(innerPad + 2,
                                     cy - fmAscent + (fmAscent - badgeH) / 2,
                                     badgeW, badgeH, 4, 4);

                    // texto da linha
                    g2.setColor(java.awt.Color.WHITE);
                    String line = String.format("%dº  %s", i + 1, p.getNome());
                    g2.drawString(line, innerPad + 2 + badgeW + 10, cy);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}