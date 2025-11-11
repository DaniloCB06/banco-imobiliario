package banco_imobiliario_ui;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo de carta de território.
 *
 * Agora concentra TODAS as ações relacionadas ao território:
 * - Comprar propriedade
 * - Construir CASA
 * - Construir HOTEL
 *
 * Caso seja aberto apenas para visualização (ex.: Banco de Cartas),
 * nenhum botão de ação é exibido.
 *
 * Compatível com o construtor antigo (sem ações).
 */
public final class CartaTerritorioDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    // ---- Botões de ação -----------------------------------------------------
    private final JButton btnComprar = new JButton("Comprar");
    private final JButton btnCasa    = new JButton("Construir casa");
    private final JButton btnHotel   = new JButton("Construir hotel");

    // ---- Callbacks injetados pelo Controller --------------------------------
    private final Runnable onComprar;
    private final Runnable onConstruirCasa;
    private final Runnable onConstruirHotel;

    // ---- Área opcional com resumo/Status do território ----------------------
    private final JTextArea taResumo = new JTextArea(3, 20);

    // =========================================================================
    // Construtor antigo (compatibilidade)
    // =========================================================================
    public CartaTerritorioDialog(JFrame owner, String titulo, ImageIcon icon) {
        this(owner, titulo, icon,
             /*onComprar*/       null,
             /*onConstruirCasa*/ null,
             /*onConstruirHotel*/null,
             /*habilitarComprar*/false,
             /*habilitarCasa*/   false,
             /*habilitarHotel*/  false,
             /*resumo*/          null);
    }

    // =========================================================================
    // Construtor novo (recomendado)
    // =========================================================================
    /**
     * @param owner             Janela pai
     * @param titulo            Título do diálogo (nome do território)
     * @param icon              Imagem da carta (pode ser null)
     * @param onComprar         Callback para comprar (pode ser null)
     * @param onConstruirCasa   Callback para construir CASA (pode ser null)
     * @param onConstruirHotel  Callback para construir HOTEL (pode ser null)
     * @param habilitarComprar  Habilita botão "Comprar"
     * @param habilitarCasa     Habilita botão "Construir casa"
     * @param habilitarHotel    Habilita botão "Construir hotel"
     * @param resumoOpcional    Texto curto com status/custos (pode ser null)
     */
    public CartaTerritorioDialog(JFrame owner,
                                 String titulo,
                                 ImageIcon icon,
                                 Runnable onComprar,
                                 Runnable onConstruirCasa,
                                 Runnable onConstruirHotel,
                                 boolean habilitarComprar,
                                 boolean habilitarCasa,
                                 boolean habilitarHotel,
                                 String resumoOpcional) {
        super(owner, titulo, true);
        this.onComprar = onComprar;
        this.onConstruirCasa = onConstruirCasa;
        this.onConstruirHotel = onConstruirHotel;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // ---------- Centro: imagem da carta ----------
        JLabel img = new JLabel(icon != null ? icon : new ImageIcon());
        img.setHorizontalAlignment(SwingConstants.CENTER);

        if (icon == null) {
            img.setText("Imagem da carta não encontrada");
            img.setForeground(Color.DARK_GRAY);
            img.setFont(img.getFont().deriveFont(Font.BOLD, 14f));
        }

        JScrollPane scroll = new JScrollPane(img);
        scroll.setPreferredSize(new Dimension(380, 540));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        // ---------- Norte: resumo opcional ----------
        if (resumoOpcional != null && !resumoOpcional.trim().isEmpty()) {
            taResumo.setText(resumoOpcional.trim());
            taResumo.setEditable(false);
            taResumo.setFocusable(false);
            taResumo.setOpaque(false);
            taResumo.setLineWrap(true);
            taResumo.setWrapStyleWord(true);
            taResumo.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 10));
            taResumo.setFont(taResumo.getFont().deriveFont(Font.PLAIN, 12f));
            add(taResumo, BorderLayout.NORTH);
        }

        // ---------- Sul ----------
        JPanel south = new JPanel(new BorderLayout(6, 6));

        // *** MODO VISUALIZAÇÃO PURA ***
        // Se não há callbacks E todas as flags de habilitação são falsas,
        // não exibimos os botões de ação (Comprar/Casa/Hotel).
        boolean visualizacaoPura =
                onComprar == null && onConstruirCasa == null && onConstruirHotel == null
                        && !habilitarComprar && !habilitarCasa && !habilitarHotel;

        if (!visualizacaoPura) {
            // Área dos botões de ação (somente quando não é visualização pura)
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

            // Comprar
            btnComprar.setEnabled(habilitarComprar);
            btnComprar.setToolTipText("Comprar este território (se não tiver dono e houver saldo)");
            btnComprar.addActionListener(e -> {
                invokeSafely(this.onComprar);
                disposeAfterAction();
            });
            actions.add(btnComprar);

            // Construir casa
            btnCasa.setEnabled(habilitarCasa);
            btnCasa.setToolTipText("Construir UMA casa nesta queda (após já ser dono e em quedas subsequentes)");
            btnCasa.addActionListener(e -> {
                invokeSafely(this.onConstruirCasa);
                disposeAfterAction();
            });
            actions.add(btnCasa);

            // Construir hotel
            btnHotel.setEnabled(habilitarHotel);
            btnHotel.setToolTipText("Construir UM hotel nesta queda (após já possuir ≥1 casa)");
            btnHotel.addActionListener(e -> {
                invokeSafely(this.onConstruirHotel);
                disposeAfterAction();
            });
            actions.add(btnHotel);

            south.add(actions, BorderLayout.WEST);
        }

        // Botão fechar à direita (sempre presente)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(e -> dispose());
        right.add(fechar);
        south.add(right, BorderLayout.EAST);

        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);

        // Acessibilidade: ESC fecha
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Enter: se for visualização pura, default = Fechar. Senão, prioriza ação principal.
        if (visualizacaoPura) {
            getRootPane().setDefaultButton(fechar);
        } else {
            getRootPane().setDefaultButton(
                btnComprar.isEnabled() ? btnComprar :
                (btnCasa.isEnabled() ? btnCasa :
                 (btnHotel.isEnabled() ? btnHotel : fechar))
            );
        }
    }

    // =========================================================================
    // Helpers públicos (caso queira alternar estados enquanto o diálogo está aberto)
    // =========================================================================
    public void setComprarEnabled(boolean enabled) { btnComprar.setEnabled(enabled); }
    public void setConstruirCasaEnabled(boolean enabled) { btnCasa.setEnabled(enabled); }
    public void setConstruirHotelEnabled(boolean enabled) { btnHotel.setEnabled(enabled); }

    public void setResumo(String texto) {
        if (texto == null) texto = "";
        taResumo.setText(texto);
        taResumo.setVisible(!texto.trim().isEmpty());
        revalidate();
        repaint();
    }

    // =========================================================================
    // Privados
    // =========================================================================
    private void invokeSafely(Runnable r) {
        if (r == null) return;
        try {
            r.run();
        } catch (Throwable t) {
            // Controller/Model fazem validação; aqui não quebramos a UX.
        }
    }

    /** Fecha após a ação para que a tela principal atualize via Observer do Model. */
    private void disposeAfterAction() {
        dispose();
    }
}
