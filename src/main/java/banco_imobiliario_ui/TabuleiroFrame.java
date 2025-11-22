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

    // Controles da lateral (fora do tabuleiro)
    private final javax.swing.JRadioButton rbAleatorio = new javax.swing.JRadioButton("Aleatório", true);
    private final javax.swing.JRadioButton rbManual    = new javax.swing.JRadioButton("Manual");
    private final javax.swing.JComboBox<Integer> cbD1   = new javax.swing.JComboBox<>(new Integer[]{1,2,3,4,5,6});
    private final javax.swing.JComboBox<Integer> cbD2   = new javax.swing.JComboBox<>(new Integer[]{1,2,3,4,5,6});
    private final javax.swing.JLabel lblStatus          = new javax.swing.JLabel("Pronto.");

    // Botões de ação (lateral)
    private final javax.swing.JButton btnCartaTerritorio = new javax.swing.JButton("Exibir carta do território");
    private final javax.swing.JButton btnEncerrarVez     = new javax.swing.JButton("Encerrar vez");
    private final javax.swing.JButton btnJogar           = new javax.swing.JButton("Jogar");
    private final javax.swing.JButton btnBancoCartas     = new javax.swing.JButton("Abrir banco de cartas");
    private final javax.swing.JButton btnUsarCartaPrisao = new javax.swing.JButton("Usar carta de prisão");
    private final javax.swing.JButton btnSalvarPartida   = new javax.swing.JButton("Salvar partida");
    private final javax.swing.JButton btnCarregarPartida = new javax.swing.JButton("Carregar partida");
    private final javax.swing.JButton btnEncerrarPartida = new javax.swing.JButton("Encerrar partida");

    // Nome da casa atual (território) para o popup
    private String nomeCasaParaExibir = null;

    // =========================================================================
    // [2] Construtor
    // =========================================================================
    public TabuleiroFrame(banco_imobiliario_controller.AppController controller) {
        this.controller = java.util.Objects.requireNonNull(controller);

        setTitle("Banco Imobiliário — Tabuleiro");
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                controller.solicitarEncerramentoPorFechamento(TabuleiroFrame.this);
            }
        });

        setSize(new java.awt.Dimension(1000, 780));
        setLocationRelativeTo(null);

        boardPanel = new BoardPanel(controller);
        dicePanel  = new DicePanel(controller);
        moneyPanel = new MoneyPanel(controller);

        javax.swing.JPanel controls = buildControls(); // painel lateral

        // Botão de carta (território)
        btnCartaTerritorio.setEnabled(false);
        btnCartaTerritorio.addActionListener(e -> {
            if (nomeCasaParaExibir != null) {
                controller.exibirCartaTerritorio(nomeCasaParaExibir);
            }
        });

        // Botão "Encerrar vez" — passa a vez (desabilitado em caso de DUPLA)
        btnEncerrarVez.setEnabled(true);
        btnEncerrarVez.setToolTipText("Encerrar as ações e passar a vez (desabilita se saiu dupla).");
        btnEncerrarVez.addActionListener(e -> {
            controller.getModel().encerrarAcoesDaVezEPassarTurno();
        });

        // Botão "Abrir banco de cartas" — visível ao lado dos demais
        btnBancoCartas.setEnabled(false);
        btnBancoCartas.addActionListener(e -> abrirBancoDeCartas());

        btnUsarCartaPrisao.setEnabled(false);
        btnUsarCartaPrisao.setToolTipText("Use a carta de saída livre da prisão quando estiver preso.");
        btnUsarCartaPrisao.addActionListener(e -> usarCartaSaidaLivre());

        btnSalvarPartida.addActionListener(e -> controller.solicitarSalvarPartida(this));
        btnCarregarPartida.addActionListener(e -> controller.solicitarCarregarPartida(this));
        btnEncerrarPartida.addActionListener(e -> controller.solicitarEncerramentoViaBotao(this));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(boardPanel, BorderLayout.CENTER);
        getContentPane().add(controls, BorderLayout.EAST);

        cbD1.setEnabled(false);
        cbD2.setEnabled(false);
        atualizarUIJogadorDaVez();
    }

    // =========================================================================
    // [3] Construção do painel lateral (dados, saldos, modo dos dados, ações)
    // =========================================================================
    private javax.swing.JPanel buildControls() {
        javax.swing.JPanel p = new javax.swing.JPanel(new BorderLayout(8, 8));
        p.setPreferredSize(new java.awt.Dimension(280, 780));
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Topo: dados + saldos
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

        javax.swing.JPanel manualDicePanel = new javax.swing.JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbcManual = new java.awt.GridBagConstraints();
        gbcManual.insets = new java.awt.Insets(2,2,2,2);
        gbcManual.gridy = 0;
        gbcManual.gridx = 0;
        gbcManual.anchor = java.awt.GridBagConstraints.WEST;
        manualDicePanel.add(new javax.swing.JLabel("Dado 1:"), gbcManual);
        gbcManual.gridx = 1;
        gbcManual.anchor = java.awt.GridBagConstraints.EAST;
        manualDicePanel.add(new javax.swing.JLabel("Dado 2:"), gbcManual);
        gbcManual.gridy = 1;
        gbcManual.gridx = 0;
        gbcManual.fill = java.awt.GridBagConstraints.HORIZONTAL;
        manualDicePanel.add(cbD1, gbcManual);
        gbcManual.gridx = 1;
        manualDicePanel.add(cbD2, gbcManual);

        gbc.gridy++;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        center.add(manualDicePanel, gbc);
        gbc.fill = java.awt.GridBagConstraints.NONE;

        p.add(center, BorderLayout.CENTER);

        // Rodapé: Jogar + Ações + Status
        javax.swing.JPanel south = new javax.swing.JPanel(new BorderLayout(4,4));

        // usa o campo btnJogar
        btnJogar.addActionListener(this::onJogar);

        javax.swing.JPanel actions = new javax.swing.JPanel();
        actions.setLayout(new javax.swing.BoxLayout(actions, javax.swing.BoxLayout.Y_AXIS));
        actions.add(btnJogar);
        actions.add(javax.swing.Box.createVerticalStrut(6));
        actions.add(btnUsarCartaPrisao);
        actions.add(javax.swing.Box.createVerticalStrut(6));
        actions.add(btnEncerrarVez);
        actions.add(javax.swing.Box.createVerticalStrut(6));
        actions.add(btnBancoCartas);        // <-- botão inserido
        actions.add(javax.swing.Box.createVerticalStrut(6));
        actions.add(btnCartaTerritorio);
        actions.add(javax.swing.Box.createVerticalStrut(12));
        actions.add(btnSalvarPartida);
        actions.add(javax.swing.Box.createVerticalStrut(4));
        actions.add(btnCarregarPartida);
        actions.add(javax.swing.Box.createVerticalStrut(4));
        actions.add(btnEncerrarPartida);

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
        // Evita duplo clique no mesmo turno (UI guard) — o update() reabilita depois quando puder
        btnJogar.setEnabled(false);

        banco_imobiliario_models.GameModel model = controller.getModel();
        try {
            // 1) Lançar dados (aleatório ou forçado)
            if (rbManual.isSelected()) {
                int d1 = (Integer) cbD1.getSelectedItem();
                int d2 = (Integer) cbD2.getSelectedItem();
                model.lancarDadosForcado(d1, d2);
            } else {
                model.lancarDados();
            }

            // 2) Mover e aplicar efeitos obrigatórios
            model.deslocarPiaoEAplicarObrigatorios();

            // NÃO reabilita aqui. O update() decide com base no Model (podeLancarDadosNesteTurno).
        } catch (RuntimeException ex) {
            controller.exibirErro(ex.getMessage());
            // Em caso de erro, não reabilitamos aqui; o update() ajusta conforme o estado real.
        }
    }

    private void usarCartaSaidaLivre() {
        banco_imobiliario_models.GameModel model = controller.getModel();
        try {
            boolean ok = model.usarCartaSaidaLivre();
            if (!ok) {
                controller.exibirErro("Não é possível usar a carta agora.");
            }
        } catch (RuntimeException ex) {
            controller.exibirErro(ex.getMessage());
        }
    }

    // =========================================================================
    // [5] Helpers de UI
    // =========================================================================
    private void atualizarUIJogadorDaVez() {
        int id = controller.getModel().getJogadorDaVez();
        banco_imobiliario_controller.PlayerProfile p = controller.getPlayerProfiles().stream()
                .filter(pp -> pp.getId() == id).findFirst().orElse(null);

        String nome = (p != null ? p.getNome() : "J" + (id + 1));
        lblStatus.setText("Vez de: " + nome + "  (clique em Jogar)");
        dicePanel.setPlayerColor(p != null ? p.getCor() : new Color(200,200,200));
        dicePanel.repaint();
    }

    public void repaintBoard() {
        boardPanel.repaint();
        atualizarUIJogadorDaVez();
    }

    // =========================================================================
    // [6] Painel do TABULEIRO (desenha imagem base e peões)
    // =========================================================================
    private static final class BoardPanel extends javax.swing.JPanel {
        private static final long serialVersionUID = 1L;

        private final banco_imobiliario_controller.AppController controller;
        private java.awt.image.BufferedImage boardImage;
        private final java.awt.image.BufferedImage[] pinImgs = new java.awt.image.BufferedImage[6];

        private int side;
        private int originX, originY;

        BoardPanel(banco_imobiliario_controller.AppController controller) {
            this.controller = controller;
            setPreferredSize(new java.awt.Dimension(740, 740));
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
                int cx = (int) Math.round(base.x);
                int cy = (int) Math.round(base.y);

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
                    int y = cy - pinH + 4;
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

        private int indexForColor(java.awt.Color c) {
            if (c == null) return 5; // cinza
            java.awt.Color[] targets = {
                new java.awt.Color(220, 50, 50),
                new java.awt.Color(60, 100, 200),
                new java.awt.Color(245, 150, 40),
                new java.awt.Color(250, 220, 40),
                new java.awt.Color(180, 60, 200),
                new java.awt.Color(150, 150, 150)
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
    // [7] Área visual dos DADOS
    // =========================================================================
    private static final class DicePanel extends javax.swing.JPanel {
        private static final long serialVersionUID = 1L;

        private java.awt.Color playerColor = new java.awt.Color(200,200,200);
        private int d1 = 1, d2 = 1; // último resultado exibido
        private final java.awt.Image[] diceImgs = new java.awt.Image[7]; // 1..6

        DicePanel(banco_imobiliario_controller.AppController controller) {
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
    // [8] Painel de SALDOS
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
            java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR"));

        MoneyPanel(banco_imobiliario_controller.AppController controller) {
            this.controller = controller;
            setOpaque(false);
            setAlignmentX(0f);

            java.awt.Dimension pref = getPreferredSize();
            setMinimumSize(pref);
            setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, pref.height));
            setAlignmentX(0f);
        }

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

        @Override
        public java.awt.Dimension getPreferredSize() {
            java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = bi.createGraphics();
            java.awt.Font listFont  = getFont().deriveFont(java.awt.Font.PLAIN, 14f);
            g2.setFont(listFont);
            int lineH  = g2.getFontMetrics().getAscent() + g2.getFontMetrics().getDescent() + 4;
            g2.dispose();

            int innerPad = 10;
            int linhas = Math.max(0, rows.size()) + 1; // jogadores + Banco
            int h = innerPad + linhas * lineH + innerPad + 6;
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
                int innerPad = 10;
                int badgeW = 18, badgeH = 14;

                g2.setColor(new java.awt.Color(0, 0, 0, 110));
                g2.fillRoundRect(0, 0, w, getHeight(), 12, 12);

                java.awt.Font listFont  = getFont().deriveFont(java.awt.Font.PLAIN, 14f);
                g2.setFont(listFont);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int fmAscent  = fm.getAscent();
                int fmDescent = fm.getDescent();
                int lineH     = fmAscent + fmDescent + 4;

                int cy = innerPad;

                int paddingRight = 12;
                int moneyRightX = w - paddingRight;

                for (Row r : rows) {
                    cy += lineH;

                    int leftX = innerPad + 2;

                    int badgeY = cy - fmAscent + (fmAscent - badgeH) / 2;
                    g2.setColor(r.cor);
                    g2.fillRoundRect(leftX, badgeY, badgeW, badgeH, 4, 4);

                    java.awt.Font nameFont = r.daVez
                            ? listFont.deriveFont(java.awt.Font.BOLD)
                            : listFont;
                    g2.setFont(nameFont);
                    g2.setColor(r.ativo ? java.awt.Color.WHITE : new java.awt.Color(200,200,200));
                    int nameX = leftX + badgeW + 6;
                    g2.drawString(r.nome + (r.ativo ? "" : " (falido)"), nameX, cy);

                    String val = brl.format(r.saldo);
                    int valW = fm.stringWidth(val);
                    g2.setFont(listFont);
                    g2.setColor(java.awt.Color.WHITE);
                    g2.drawString(val, moneyRightX - valW, cy);
                }

                cy += Math.max(6, lineH / 3);
                g2.setColor(new java.awt.Color(255,255,255,60));
                g2.drawLine(innerPad + 2, cy, w - innerPad - 2, cy);

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

            boolean partidaEncerrada = false;
            try {
                partidaEncerrada = m.isPartidaEncerrada();
            } catch (Throwable ignore) {}

            if (partidaEncerrada) {
                lblStatus.setText("Partida encerrada.");
            } else {
                atualizarUIJogadorDaVez();
            }

            boardPanel.repaint();
            moneyPanel.refreshFromModel(m);
            moneyPanel.repaint();

            // ----- Nome do território (se for território e não tiver dono de outro) -----
            java.util.Optional<String> territorioAtual =
                    m.getNomeDoTerritorioDaCasaAtualDoJogadorDaVez();

            boolean bloqueiaCarta = false;
            try {
                bloqueiaCarta = controller.getModel().isCasaAtualPropriedadeComDonoDeOutro();
            } catch (Throwable ignore) {}

            if (territorioAtual.isPresent()) {
                nomeCasaParaExibir = territorioAtual.get();
                btnCartaTerritorio.setEnabled(!partidaEncerrada && !bloqueiaCarta);
            } else {
                nomeCasaParaExibir = null;
                btnCartaTerritorio.setEnabled(false);
            }

            // Habilita "Encerrar vez" APENAS se não saiu dupla
            boolean saiuDupla = false;
            try {
                saiuDupla = m.houveDuplaNoUltimoLancamento();
            } catch (Throwable ignore) {}
            btnEncerrarVez.setEnabled(!partidaEncerrada && !saiuDupla);

            boolean podeUsarCartaPrisao = false;
            try {
                podeUsarCartaPrisao = m.isJogadorDaVezNaPrisao() && m.jogadorDaVezTemCartaSaidaLivre();
            } catch (Throwable ignore) {}
            btnUsarCartaPrisao.setEnabled(!partidaEncerrada && podeUsarCartaPrisao);

            // Popup de Sorte/Revés (one-shot)
            try {
                java.lang.reflect.Method meth = m.getClass().getMethod("consumirSorteRevesRecemSacada");
                Object opt = meth.invoke(m);
                if (opt instanceof java.util.Optional) {
                    java.util.Optional<?> o = (java.util.Optional<?>) opt;
                    if (o.isPresent()) {
                        Object carta = o.get();
                        Integer num = null;
                        try {
                            java.lang.reflect.Method getNum = carta.getClass().getMethod("getNumero");
                            Object n = getNum.invoke(carta);
                            if (n instanceof Number) num = ((Number) n).intValue();
                        } catch (Exception ignore) {}
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
                // versão do GameModel sem esse método -> ignora
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // ---- Habilita "Jogar" apenas quando o Model permitir (evita 2º clique no turno) ----
            boolean podeJogar = false;
            if (!partidaEncerrada) {
                try { podeJogar = m.podeLancarDadosNesteTurno(); } catch (Throwable ignore) {}
            }
            btnJogar.setEnabled(!partidaEncerrada && podeJogar);

            // ---- Habilita "Abrir banco de cartas" somente se o jogador da vez tiver algo ----
            try {
                int idVez = m.getJogadorDaVez();
                boolean podeAbrirBanco = m.jogadorPossuiAlgumaCartaOuPropriedade(idVez);
                btnBancoCartas.setEnabled(!partidaEncerrada && podeAbrirBanco);
            } catch (Throwable ignore) {
                btnBancoCartas.setEnabled(false);
            }

            boolean podeSalvar = false;
            try { podeSalvar = m.isSalvamentoDisponivel(); } catch (Throwable ignore) {}
            btnSalvarPartida.setEnabled(podeSalvar);
            btnCarregarPartida.setEnabled(true);
            btnEncerrarPartida.setEnabled(!partidaEncerrada);

            if (partidaEncerrada) {
                btnEncerrarVez.setEnabled(false);
                btnUsarCartaPrisao.setEnabled(false);
                btnBancoCartas.setEnabled(false);
                btnCartaTerritorio.setEnabled(false);
            }
        });
    }

    // =========================================================================
    // [10] Abertura do diálogo de Banco de Cartas
    // =========================================================================
    private void abrirBancoDeCartas() {
        banco_imobiliario_models.GameModel model = controller.getModel();
        int idVez = model.getJogadorDaVez();

        // Segurança extra: só abre se realmente houver algo
        if (!model.jogadorPossuiAlgumaCartaOuPropriedade(idVez)) {
            controller.exibirErro("Você não possui cartas no banco ainda.");
            return;
        }

        // Título com o nome do jogador da vez
        String nomeJogador = controller.getPlayerProfiles().stream()
                .filter(pp -> pp.getId() == idVez)
                .map(banco_imobiliario_controller.PlayerProfile::getNome)
                .findFirst()
                .orElse("Jogador " + (idVez + 1));
        String titulo = "Banco de cartas — " + nomeJogador;

        // Lista consolidada (territórios + sorte/revés) para o diálogo
        java.util.List<banco_imobiliario_models.GameModel.BancoDeCartasItem> itens =
                model.getBancoDeCartasDoJogador(idVez);

        // Construtor do seu BancoDeCartasDialog (owner, controller, titulo, itens)
        BancoDeCartasDialog dlg = new BancoDeCartasDialog(this, controller, titulo, itens);
        dlg.setVisible(true);
    }
}
