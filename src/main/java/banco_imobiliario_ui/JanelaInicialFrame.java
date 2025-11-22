package banco_imobiliario_ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import banco_imobiliario_controller.AppController;

/**
 * Exibição da janela inicial.
 */
public final class JanelaInicialFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AppController controller;
    private final JSpinner spNumJogadores;

    public JanelaInicialFrame(AppController controller) {
        this.controller = controller;

        setTitle("Banco Imobiliário — Nova Partida");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);                
        setSize(new Dimension(600, 360));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel titulo = new JLabel("Nova Partida", SwingConstants.LEFT);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        root.add(titulo, BorderLayout.NORTH);

        // Centro: seletor de quantidade
        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;

        JLabel lbl = new JLabel("Quantidade de jogadores (3–6):");
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 16f));
        center.add(lbl, gbc);

        gbc.gridx = 1;
        spNumJogadores = new JSpinner(new SpinnerNumberModel(3, 3, 6, 1));
        spNumJogadores.setPreferredSize(new Dimension(64, 28));
        center.add(spNumJogadores, gbc);

        root.add(center, BorderLayout.CENTER);

        // Rodapé: botões
        JPanel footer = new JPanel();
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> System.exit(0));

        JButton btnIniciar = new JButton("Iniciar");
        btnIniciar.addActionListener(e -> onIniciar());

        JButton btnCarregar = new JButton("Carregar partida...");
        btnCarregar.addActionListener(e -> controller.solicitarCarregarPartida(this));

        footer.add(btnCancelar);
        footer.add(btnCarregar);
        footer.add(btnIniciar);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(btnIniciar);
    }

    private void onIniciar() {
        int n = ((Number) spNumJogadores.getValue()).intValue();
        controller.iniciarNovaPartida(n);
    }
}
