package banco_imobiliario_ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import banco_imobiliario_controller.AppController;
import banco_imobiliario_controller.PlayerProfile;

/**
 * Captura N (3..6) nomes e cores únicas, valida e entrega ao Controller.
 */
public final class DefinicaoJogadoresDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private static final String[] COLOR_NAMES = {
            "Vermelho", "Azul", "Laranja", "Amarelo", "Roxo", "Cinza"
    };
    private static final Color[] COLORS = {
            new Color(0xE74C3C),
            new Color(0x3498DB),
            new Color(0xE67E22),
            new Color(0xF1C40F),
            new Color(0x9B59B6),
            new Color(0x95A5A6)
    };

    private final AppController controller;
    private final int nJogadores;
    private final JTextField[] nomeCampos;
    private final JComboBox<ColorItem>[] corCombos;

    private static final class ColorItem {
        final String name;
        final Color color;
        final int pinIndex;

        ColorItem(String name, Color color, int pinIndex) {
            this.name = name;
            this.color = color;
            this.pinIndex = pinIndex;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public DefinicaoJogadoresDialog(java.awt.Window owner, AppController controller, int nJogadores) {
        super(owner, "Definição dos Jogadores", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.nJogadores = nJogadores;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(new Dimension(680, 420));

        JLabel titulo = new JLabel("Defina o nome e a cor de cada jogador");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        titulo.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

        nomeCampos = new JTextField[nJogadores];
        corCombos = new JComboBox[nJogadores];

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        addHeader(form, gbc, "Jogador", "Nome (1–8)", "Cor");

        for (int i = 0; i < nJogadores; i++) {
            gbc.gridy++;

            gbc.gridx = 0;
            form.add(new JLabel("J" + (i + 1) + ":"), gbc);

            gbc.gridx = 1;
            JTextField tf = new JTextField(10);
            tf.setText("J" + (i + 1));
            nomeCampos[i] = tf;
            form.add(tf, gbc);

            gbc.gridx = 2;
            JComboBox<ColorItem> combo = new JComboBox<>(buildPalette());
            combo.setRenderer(new ColorCellRenderer());
            combo.setSelectedIndex(i % COLORS.length);
            corCombos[i] = combo;
            form.add(combo, gbc);
        }

        JPanel footer = new JPanel();
        JButton cancelar = new JButton("Cancelar");
        cancelar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });

        JButton ok = new JButton("Confirmar");
        ok.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onConfirmar();
            }
        });

        footer.add(cancelar);
        footer.add(ok);

        JPanel root = new JPanel(new BorderLayout());
        root.add(titulo, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(ok);
    }

    private void addHeader(JPanel panel, GridBagConstraints gbc, String c1, String c2, String c3) {
        gbc.gridx = 0;
        panel.add(boldLabel(c1), gbc);
        gbc.gridx = 1;
        panel.add(boldLabel(c2), gbc);
        gbc.gridx = 2;
        panel.add(boldLabel(c3), gbc);
    }

    private JLabel boldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private ColorItem[] buildPalette() {
        ColorItem[] items = new ColorItem[COLOR_NAMES.length];
        for (int i = 0; i < COLOR_NAMES.length; i++) {
            items[i] = new ColorItem(COLOR_NAMES[i], COLORS[i], i);
        }
        return items;
    }

    private void onConfirmar() {

        List<PlayerProfile> perfis = new ArrayList<>();
        Set<Integer> pinosUsados = new HashSet<>();

        for (int i = 0; i < nJogadores; i++) {
            String nome = nomeCampos[i].getText();
            if (!controller.isNomeValido(nome)) {
                focoErro(nomeCampos[i],
                        "Nome inválido para J" + (i + 1) + ". Use 1–8 caracteres [A–Z a–z 0–9].");
                return;
            }

            ColorItem sel = (ColorItem) corCombos[i].getSelectedItem();
            if (sel == null) {
                focoErro(corCombos[i], "Selecione uma cor para J" + (i + 1) + ".");
                return;
            }
            if (pinosUsados.contains(sel.pinIndex)) {
                focoErro(corCombos[i], "As cores devem ser únicas. Altere a cor de J" + (i + 1) + ".");
                return;
            }
            pinosUsados.add(sel.pinIndex);

            perfis.add(new PlayerProfile(i, nome, sel.color, sel.pinIndex));
        }

        controller.confirmarDefinicaoJogadores(perfis);
        dispose();
    }

    private void focoErro(JComponent comp, String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validação", JOptionPane.WARNING_MESSAGE);
        comp.requestFocusInWindow();
    }

    private static final class ColorCellRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(
                javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ColorItem) {
                ColorItem item = (ColorItem) value;
                setText(item.name);
                setOpaque(true);
                setBackground(isSelected ? item.color.darker() : item.color);
                setForeground(contrast(item.color));
            }
            return c;
        }

        private Color contrast(Color bg) {
            int yiq = ((bg.getRed() * 299) + (bg.getGreen() * 587) + (bg.getBlue() * 114)) / 1000;
            return yiq >= 128 ? Color.BLACK : Color.WHITE;
        }
    }
}
