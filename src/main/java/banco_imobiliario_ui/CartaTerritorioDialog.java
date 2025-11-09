package banco_imobiliario_ui;

import javax.swing.*;
import java.awt.*;

public final class CartaTerritorioDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    public CartaTerritorioDialog(JFrame owner, String titulo, ImageIcon icon) {
        super(owner, titulo, true);
        setLayout(new BorderLayout());

        JLabel img = new JLabel(icon);
        img.setHorizontalAlignment(SwingConstants.CENTER);

        JScrollPane scroll = new JScrollPane(img);
        scroll.setPreferredSize(new Dimension(380, 540));
        add(scroll, BorderLayout.CENTER);

        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(fechar);
        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }
}
