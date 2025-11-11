package banco_imobiliario_ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public final class BancoDeCartasDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final banco_imobiliario_controller.AppController controller;
    private final List<banco_imobiliario_models.GameModel.BancoDeCartasItem> itens;
    private final String tituloCabecalho;

    public BancoDeCartasDialog(Frame owner,
                               banco_imobiliario_controller.AppController controller,
                               String tituloCabecalho,
                               List<banco_imobiliario_models.GameModel.BancoDeCartasItem> itens) {
        super(owner, "Banco de Cartas", true);
        this.controller = controller;
        this.itens = itens;
        this.tituloCabecalho = tituloCabecalho;

        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        setLayout(new BorderLayout(8,8));
        JLabel header = new JLabel(tituloCabecalho);
        header.setBorder(BorderFactory.createEmptyBorder(8,12,0,12));
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        for (banco_imobiliario_models.GameModel.BancoDeCartasItem it : itens) {
            JPanel row = new JPanel(new BorderLayout(6,6));
            row.setBorder(BorderFactory.createEmptyBorder(6,12,6,12));

            String prefix = it.getTipo() == banco_imobiliario_models.GameModel.BancoDeCartasItem.Tipo.TERRITORIO
                    ? "[Território] "
                    : "[Sorte/Revés] ";
            JLabel lbl = new JLabel(prefix + it.getNome());
            row.add(lbl, BorderLayout.CENTER);

            JButton btn = new JButton("Visualizar");
            btn.addActionListener(e -> visualizar(it));
            row.add(btn, BorderLayout.EAST);

            listPanel.add(row);
            listPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setPreferredSize(new Dimension(420, 420));
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton fechar = new JButton("Fechar");
        fechar.addActionListener(e -> dispose());
        south.add(fechar);
        add(south, BorderLayout.SOUTH);
    }

    private void visualizar(banco_imobiliario_models.GameModel.BancoDeCartasItem it) {
        // Abre a carta em modo "somente visualização" (sem barra de ações)
    	controller.exibirCartaTerritorioSomenteVisualizacao(it.getNome());

    }

}
