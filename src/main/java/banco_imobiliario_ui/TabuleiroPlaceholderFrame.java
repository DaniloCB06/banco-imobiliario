package banco_imobiliario_ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

import banco_imobiliario_controller.AppController;
import banco_imobiliario_controller.PlayerProfile;

public final class TabuleiroPlaceholderFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public TabuleiroPlaceholderFrame(AppController controller) {
        setTitle("Banco Imobiliário — Tabuleiro (placeholder)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(new Dimension(900, 700));
        setLocationRelativeTo(null);

        setContentPane(new BoardPanel(controller));
    }

    private static final class BoardPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final AppController controller;

        BoardPanel(AppController controller) { this.controller = controller; }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(new Color(240, 240, 240));
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(Color.DARK_GRAY);
                g2.setFont(getFont().deriveFont(Font.BOLD, 22f));
                g2.drawString("Tabuleiro — Próxima funcionalidade: desenho com Java2D", 30, 60);

                // Lista de jogadores (nome + cor) só para visualização
                int y = 110;
                g2.setFont(getFont().deriveFont(Font.PLAIN, 16f));
                g2.setColor(Color.BLACK);
                g2.drawString("Jogadores:", 30, y);
                y += 10;

                for (PlayerProfile p : controller.getPlayerProfiles()) {
                    y += 28;
                    g2.setColor(p.getCor());
                    g2.fillRoundRect(30, y - 16, 24, 16, 6, 6);
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString("J" + (p.getId() + 1) + " — " + p.getNome() + "  (" + p.getCorHex() + ")", 64, y);
                }

            } finally {
                g2.dispose();
            }
        }
    }
}
