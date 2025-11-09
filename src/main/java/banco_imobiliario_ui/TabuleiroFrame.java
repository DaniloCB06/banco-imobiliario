package banco_imobiliario_ui;

// ============================================================================
// Imports
// ============================================================================
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;

// ============================================================================
// Janela principal do tabuleiro
// ============================================================================
public final class TabuleiroFrame extends javax.swing.JFrame
    implements banco_imobiliario_models.GameModel.Observer {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // [1] Estado & componentes principais
    // =========================================================================
    private final banco_imobiliario_controller.AppController controller;
    private final BoardPanel boardPanel;
    private final DicePanel  dicePanel;
    private final MoneyPanel moneyPanel;
    // private final OrderPanel orderPanel;

    // Controles da lateral (fora do tabuleiro)
    private final javax.swing.JRadioButton rbAleatorio = new javax.swing.JRadioButton("Aleatório", true);
    private final javax.swing.JRadioButton rbManual    = new javax.swing.JRadioButton("Manual");
    private final javax.swing.JComboBox<Integer> cbD1   = new javax.swing.JComboBox<>(new Integer[]{1,2,3,4,5,6});
    private final javax.swing.JComboBox<Integer> cbD2   = new javax.swing.JComboBox<>(new Integer[]{1,2,3,4,5,6});
    private final javax.swing.JLabel lblStatus          = new javax.swing.JLabel("Pronto.");

    // Botão para exibir a carta do território
    private final javax.swing.JButton btnCartaTerritorio = new javax.swing.JButton("Exibir carta do território");
    private String nomeCasaParaExibir = null;

    // =========================================================================
    // [2] Construtor
    // =========================================================================
    public TabuleiroFrame(banco_imobiliario_controller.AppController controller) {
        this.controller = java.util.Objects.requireNonNull(controller);

        setTitle("Banco Imobiliário — Tabuleiro");
        setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Tamanho total da janela (tabuleiro + barra lateral)
        setSize(new java.awt.Dimension(1000, 780));
        setLocationRelativeTo(null);

        boardPanel = new BoardPanel(controller);
        dicePanel  = new DicePanel(controller);
        moneyPanel = new MoneyPanel(controller);
        // orderPanel = new OrderPanel(controller);

        javax.swing.JPanel controls = buildControls(); // painel lateral

        // Botão de carta (território)
        btnCartaTerritorio.setEnabled(false);
        btnCartaTerritorio.addActionListener(e -> {
            if (nomeCasaParaExibir != null) {
                controller.exibirCartaTerritorio(nomeCasaParaExibir);
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(boardPanel, BorderLayout.CENTER);
        getContentPane().add(controls, BorderLayout.EAST);

        // Estado inicial dos combos
        cbD1.setEnabled(false);
        cbD2.setEnabled(false);
        atualizarUIJogadorDaVez();
    }

    // =========================================================================
    // [3] Construção do painel lateral (dados, saldos, modo dos dados, ações)
    // =========================================================================
    /** Painel lateral com controles e áreas dos dados/ordem (fora do tabuleiro). */
    private javax.swing.JPanel buildControls() {
        javax.swing.JPanel p = new javax.swing.JPanel(new BorderLayout(8, 8));
        p.setPreferredSize(new java.awt.Dimension(280, 780));
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Topo: empilha área dos dados + (ordem) + saldos
        javax.swing.JPanel topStack = new javax.swing.JPanel();
        topStack.setOpaque(false);
        topStack.setLayout(new javax.swing.BoxLayout(topStack, javax.swing.BoxLayout.Y_AXIS));
        dicePanel.setAlignmentX(0f);
        topStack.add(dicePanel);
        topStack.add(javax.swing.Box.createVerticalStrut(8));
        topStack.add(moneyPanel);
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
        gbc.gridy++; center.add(rbAleatorio, gbc);
        gbc.gridy++; center.add(rbManual, gbc);

        gbc.gridy++; center.add(new javax.swing.JLabel("Dado 1:"), gbc);
        gbc.gridy++; center.add(cbD1, gbc);
        gbc.gridy++; center.add(new javax.swing.JLabel("Dado 2:"), gbc);
        gbc.gridy++; center.add(cbD2, gbc);

        p.add(center, BorderLayout.CENTER);

        // Rodapé: Jogar + Carta + Status
        javax.swing.JPanel south = new javax.swing.JPanel(new BorderLayout(4,4));
        javax.swing.JButton btnJogar = new javax.swing.JButton("Jogar");
        btnJogar.addActionListener(this::onJogar);

        javax.swing.JPanel actions = new javax.swing.JPanel();
        actions.setLayout(new javax.swing.BoxLayout(actions, javax.swing.BoxLayout.Y_AXIS));
        actions.add(btnJogar);
        actions.add(javax.swing.Box.createVerticalStrut(6));
        actions.add(btnCartaTerritorio);

        lblStatus.setFont(lblStatus.getFont().deriveFont(java.awt.Font.PLAIN, 12f));
        south.add(actions, BorderLayout.NORTH);
        south.add(lblStatus, BorderLayout.SOUTH);

        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    // =========================================================================
    // [4] Handlers dos controles
    // =========================================================================
    private void toggleManual(boolean manual) {
        cbD1.setEnabled(manual);
        cbD2.setEnabled(manual);
    }

    private void onJogar(java.awt.event.ActionEvent e) {
        banco_imobiliario_models.GameModel model = controller.getModel();

        // 1) Lançar dados (aleatório ou forçado)
        if (rbManual.isSelected()) {
            int d1 = (Integer) cbD1.getSelectedItem();
            int d2 = (Integer) cbD2.getSelectedItem();
            model.lancarDadosForcado(d1, d2);
        } else {
            model.lancarDados();
        }

        // 2) Mover e aplicar efeitos obrigatórios (inclui IMPOSTO/LUCRO e ALUGUEL)
        model.deslocarPiaoEAplicarObrigatorios();

        // 3) Encerrar vez (regra de terceira dupla tratada no model)
        model.encerrarAcoesDaVezEPassarTurno();
    }

    // =========================================================================
    // [5] Helpers de UI
    // =========================================================================
    /** Atualiza rótulos e a cor da área dos dados conforme o jogador da vez. */
    private void atualizarUIJogadorDaVez() {
        int id = controller.getModel().getJogadorDaVez();
        banco_imobiliario_controller.PlayerProfile p = controller.getPlayerProfiles().stream()
                .filter(pp -> pp.getId() == id).findFirst().orElse(null);

        String nome = (p != null ? p.getNome() : "J" + (id + 1));
        lblStatus.setText("Vez de: " + nome + "  (clique em Jogar)");
        dicePanel.setPlayerColor(p != null ? p.getCor() : new Color(200,200,200));
        dicePanel.repaint();
        // orderPanel.repaint();
    }

    /** Expõe repintura do tabuleiro e atualização rápida dos rótulos. */
    public void repaintBoard() {
        boardPanel.repaint();
        atualizarUIJogadorDaVez();
        // orderPanel.repaint();
    }

    // =========================================================================
    // [6] Painel do TABULEIRO (desenha imagem base e peões)
    // =========================================================================
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
                    g2.drawImage(scaled, x, y, null);
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

        /** Fallback quando o índice do pino não estiver disponível. */
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

    // =========================================================================
    // [7] Área visual dos DADOS (fora do tabuleiro)
    // =========================================================================
    private static final class DicePanel extends javax.swing.JPanel {
        private static final long serialVersionUID = 1L;

        private final banco_imobiliario_controller.AppController controller;
        private java.awt.Color playerColor = new java.awt.Color(200,200,200);
        private int d1 = 1, d2 = 1; // último resultado exibido
        private final java.awt.Image[] diceImgs = new java.awt.Image[7]; // 1..6

        DicePanel(banco_imobiliario_controller.AppController controller) {
            this.controller = controller;
            setPreferredSize(new java.awt.Dimension(236, 160));
            setOpaque(false);
            loadDiceImages();
            fillWidth(this);
        }

        private static void fillWidth(javax.swing.JComponent c) {
            java.awt.Dimension pref = c.getPreferredSize();
            c.setMinimumSize(pref);
            c.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, pref.height));
            c.setAlignmentX(0f);
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
                g2.drawImage(img, x, y, size, size, null);
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

    // =========================================================================
    // [8] Painel de SALDOS (mostra todos os jogadores + banco)
    // =========================================================================
    private static final class MoneyPanel extends javax.swing.JPanel {
        private static final long serialVersionUID = 1L;

        private final banco_imobiliario_controller.AppController controller;

        private static final class Row {
            String nome;
            java.awt.Color cor;
            int saldo;
            boolean daVez;
            boolean ativo;
        }
        private final java.util.List<Row> rows = new java.util.ArrayList<Row>();
        private int saldoBanco = 0;

        private final java.text.NumberFormat brl =
            java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt","BR"));

        MoneyPanel(banco_imobiliario_controller.AppController controller) {
            this.controller = controller;
            setOpaque(false);
            setAlignmentX(0f);

            // Ocupar largura total na coluna
            java.awt.Dimension pref = getPreferredSize();
            setMinimumSize(pref);
            setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, pref.height));
            setAlignmentX(0f);
        }

        /** Reconstroi a lista de linhas a partir do Model. Chame em TabuleiroFrame.update(). */
        void refreshFromModel(banco_imobiliario_models.GameModel m) {
            rows.clear();
            int idVez = m.getJogadorDaVez();

            java.util.List<Integer> ordem = controller.getOrdemJogadores();
            java.util.List<banco_imobiliario_controller.PlayerProfile> perfis = controller.getPlayerProfiles();

            java.util.List<Integer> ids = new java.util.ArrayList<Integer>();
            if (ordem != null && !ordem.isEmpty()) {
                ids.addAll(ordem);
            } else if (perfis != null) {
                for (banco_imobiliario_controller.PlayerProfile p : perfis) {
                    ids.add(p.getId());
                }
            }

            for (Integer id : ids) {
                banco_imobiliario_controller.PlayerProfile p = findProfileById(perfis, id);
                Row r = new Row();
                r.nome  = (p != null ? p.getNome() : "J" + (id + 1));
                r.cor   = (p != null ? p.getCor()  : new java.awt.Color(200,200,200));
                r.saldo = m.getSaldoJogador(id);
                r.daVez = (id != null && id == idVez);
                r.ativo = m.isJogadorAtivo(id);
                rows.add(r);
            }
            saldoBanco = m.getSaldoBanco();

            revalidate();
            repaint();
        }

        private banco_imobiliario_controller.PlayerProfile findProfileById(
                java.util.List<banco_imobiliario_controller.PlayerProfile> perfis, int id) {
            if (perfis == null) return null;
            for (banco_imobiliario_controller.PlayerProfile p : perfis) {
                if (p.getId() == id) return p;
            }
            return null;
        }

        /** Altura preferida dinâmica: título + uma linha por jogador + 1 linha “Banco”. */
        @Override
        public java.awt.Dimension getPreferredSize() {
            // Medidas de fonte
            java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = bi.createGraphics();
            java.awt.Font titleFont = getFont().deriveFont(java.awt.Font.BOLD, 16f);
            java.awt.Font listFont  = getFont().deriveFont(java.awt.Font.PLAIN, 14f);
            g2.setFont(titleFont);
            int titleH = g2.getFontMetrics().getAscent() + g2.getFontMetrics().getDescent();
            g2.setFont(listFont);
            int lineH  = g2.getFontMetrics().getAscent() + g2.getFontMetrics().getDescent() + 4;
            g2.dispose();

            int innerPad = 10, gapTitle = 8;
            int linhas = Math.max(0, rows.size()) + 1; // jogadores + Banco
            int h = innerPad + titleH + gapTitle + linhas * lineH + innerPad + 10;
            return new java.awt.Dimension(236, h);
        }

        @Override
        public java.awt.Dimension getMaximumSize() {
            java.awt.Dimension d = getPreferredSize();
            return new java.awt.Dimension(Integer.MAX_VALUE, d.height);
        }

        @Override protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            try {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int innerPad = 10, gapTitle = 8;
                int badgeW = 18, badgeH = 14;

                // Fundo tipo card
                g2.setColor(new java.awt.Color(0, 0, 0, 110));
                g2.fillRoundRect(0, 0, w, getHeight(), 12, 12);

                // Título
                java.awt.Font titleFont = getFont().deriveFont(java.awt.Font.BOLD, 16f);
                java.awt.Font listFont  = getFont().deriveFont(java.awt.Font.PLAIN, 14f);
                g2.setFont(titleFont);
                g2.setColor(java.awt.Color.WHITE);
                int titleAscent = g2.getFontMetrics().getAscent();
                int titleDescent = g2.getFontMetrics().getDescent();
                int titleH = titleAscent + titleDescent;
                int titleBase = innerPad + titleAscent;
                g2.drawString("Saldos", innerPad + 2, titleBase);

                // Linhas
                g2.setFont(listFont);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int fmAscent  = fm.getAscent();
                int fmDescent = fm.getDescent();
                int lineH     = fmAscent + fmDescent + 4;

                int cy = innerPad + titleH + gapTitle;

                // Alinhamento à direita
                int paddingRight = 12;
                int moneyRightX = w - paddingRight;

                // Jogadores
                for (Row r : rows) {
                    cy += lineH;

                    int leftX = innerPad + 2;

                    // badge cor
                    int badgeY = cy - fmAscent + (fmAscent - badgeH) / 2;
                    g2.setColor(r.cor);
                    g2.fillRoundRect(leftX, badgeY, badgeW, badgeH, 4, 4);

                    // nome
                    java.awt.Font nameFont = r.daVez
                            ? listFont.deriveFont(java.awt.Font.BOLD)
                            : listFont;
                    g2.setFont(nameFont);
                    g2.setColor(r.ativo ? java.awt.Color.WHITE : new java.awt.Color(200,200,200));
                    int nameX = leftX + badgeW + 6;
                    g2.drawString(r.nome + (r.ativo ? "" : " (falido)"), nameX, cy);

                    // valor à direita
                    String val = brl.format(r.saldo);
                    int valW = fm.stringWidth(val);
                    g2.setFont(listFont);
                    g2.setColor(java.awt.Color.WHITE);
                    g2.drawString(val, moneyRightX - valW, cy);
                }

                // Separador fino antes do banco
                cy += Math.max(6, lineH / 3);
                g2.setColor(new java.awt.Color(255,255,255,60));
                g2.drawLine(innerPad + 2, cy, w - innerPad - 2, cy);

                // Banco
                cy += lineH;
                g2.setFont(listFont);
                g2.setColor(java.awt.Color.WHITE);
                String lblBanco = "Banco";
                g2.drawString(lblBanco, innerPad + 2, cy);

                String valBanco = brl.format(saldoBanco);
                int valBW = fm.stringWidth(valBanco);
                g2.drawString(valBanco, moneyRightX - valBW, cy);

            } finally {
                g2.dispose();
            }
        }
    }

    // =========================================================================
    // [9] Observer do GameModel — recebe updates e sincroniza a UI
    // =========================================================================
    @Override
    public void update(banco_imobiliario_models.GameModel m) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            Integer d1 = m.getUltimoD1();
            Integer d2 = m.getUltimoD2();
            if (d1 != null && d2 != null) {
                dicePanel.setDice(d1, d2);
            }

            atualizarUIJogadorDaVez();

            boardPanel.repaint();
            // orderPanel.repaint();

            moneyPanel.refreshFromModel(m);
            moneyPanel.repaint();

            // Habilita/desabilita o botão de carta conforme a casa atual
            java.util.Optional<String> territorioAtual =
                    m.getNomeDoTerritorioDaCasaAtualDoJogadorDaVez();
            if (territorioAtual.isPresent()) {
                nomeCasaParaExibir = territorioAtual.get();
                btnCartaTerritorio.setEnabled(true);
            } else {
                nomeCasaParaExibir = null;
                btnCartaTerritorio.setEnabled(false);
            }

            // NOVO: se o jogador acabou de sacar uma carta de Sorte/Revés, mostrar popup.
            // O método deve "consumir" o evento para não repetir a exibição em updates futuros.
         // Tenta mostrar carta Sorte/Revés, se o GameModel tiver esse método (sem travar a compilação).
            try {
                java.lang.reflect.Method meth = m.getClass().getMethod("consumirSorteRevesRecemSacada");
                Object opt = meth.invoke(m); // esperado: Optional<?>
                if (opt instanceof java.util.Optional) {
                    java.util.Optional<?> o = (java.util.Optional<?>) opt;
                    if (o.isPresent()) {
                        Object carta = o.get();
                        Integer num = null;

                        // tenta carta.getNumero()
                        try {
                            java.lang.reflect.Method getNum = carta.getClass().getMethod("getNumero");
                            Object n = getNum.invoke(carta);
                            if (n instanceof Number) num = ((Number) n).intValue();
                        } catch (Exception ignore) {}

                        // fallback: campo "numero"
                        if (num == null) {
                            try {
                                java.lang.reflect.Field f = carta.getClass().getDeclaredField("numero");
                                f.setAccessible(true);
                                Object n = f.get(carta);
                                if (n instanceof Number) num = ((Number) n).intValue();
                            } catch (Exception ignore) {}
                        }

                        if (num != null) {
                            controller.exibirCartaSorteRevesPorNumero(num);
                        }
                    }
                }
            } catch (NoSuchMethodException __) {
                // versão do GameModel sem esse método -> ignora silenciosamente
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        });
    }
}
