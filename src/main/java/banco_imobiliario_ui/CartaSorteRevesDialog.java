package banco_imobiliario_ui;

import javax.swing.*;
import java.awt.*;

public final class CartaSorteRevesDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    public CartaSorteRevesDialog(JFrame owner, int id, String titulo, String descricao, ImageIcon icon) {
        super(owner, "Sorte/Revés #" + id, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(10,10));
        content.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        JLabel lblTitulo = new JLabel("Sorte/Revés #" + id + " — " + titulo);
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 16f));
        content.add(lblTitulo, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8,8));
        if (icon != null) {
            JLabel img = new JLabel(icon);
            img.setHorizontalAlignment(SwingConstants.CENTER);
            center.add(img, BorderLayout.NORTH);
        }

        JTextArea ta = new JTextArea(descricao);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setFont(ta.getFont().deriveFont(14f));
        ta.setOpaque(false);
        center.add(ta, BorderLayout.CENTER);

        content.add(center, BorderLayout.CENTER);

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(ok);
        content.add(south, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(new Dimension(360, icon == null ? 220 : 420));
        setLocationRelativeTo(owner);
    }
}
