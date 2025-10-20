package banco_imobiliario_ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import banco_imobiliario_controller.AppController;
import banco_imobiliario_controller.PlayerProfile;

/**
 * Sorteio da ordem: todos rolam 2 dados; grupos empatados rolam novamente
 * até obter uma permutação única. Usa RNG local (não mexe no estado do Model).
 */
public final class SorteioOrdemDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final AppController controller;
    private final List<PlayerProfile> perfis;
    private final Random rng = new Random(System.nanoTime());

    private final JTextArea saida;
    private List<Integer> ultimaOrdem = new ArrayList<>();

    public SorteioOrdemDialog(java.awt.Window owner, AppController controller, List<PlayerProfile> perfis) {
        super(owner, "Sortear ordem dos jogadores", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.perfis = new ArrayList<>(perfis);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(new Dimension(560, 420));

        JLabel titulo = new JLabel("Clique em \"Sortear\" para definir a ordem (com desempates automáticos)");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 14f));
        titulo.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        saida = new JTextArea();
        saida.setEditable(false);
        saida.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        saida.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        saida.setBackground(new Color(250, 250, 250));

        JPanel botoes = new JPanel();
        JButton btnSortear = new JButton("Sortear");
        JButton btnAplicar = new JButton("Aplicar ordem");
        JButton btnFechar  = new JButton("Fechar");

        btnSortear.addActionListener(e -> executarSorteio());
        btnAplicar.addActionListener(e -> aplicarSePossivel());
        btnFechar.addActionListener(e -> dispose());

        botoes.add(btnSortear);
        botoes.add(btnAplicar);
        botoes.add(btnFechar);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(titulo, BorderLayout.NORTH);
        root.add(new JScrollPane(saida), BorderLayout.CENTER);
        root.add(botoes, BorderLayout.SOUTH);
        setContentPane(root);

        // mostra a lista de jogadores inicialmente
        StringBuilder sb = new StringBuilder("Jogadores:\n");
        for (PlayerProfile p : perfis) {
            sb.append(String.format("- J%d: %s (%s)\n", p.getId()+1, p.getNome(), p.getCorHex()));
        }
        saida.setText(sb.toString());
    }

    private void executarSorteio() {
        // ids dos jogadores na ordem natural (0..N-1 do GameModel)
        List<Integer> candidatos = perfis.stream().map(PlayerProfile::getId).collect(Collectors.toList());
        Map<Integer, String> log = new HashMap<>();

        int rodada = 1;
        Map<Integer, Integer> soma = new HashMap<>();
        for (int id : candidatos) soma.put(id, 0);

        StringBuilder sb = new StringBuilder();
        sb.append("Sorteio iniciado.\n");

        while (true) {
            // rola para todos os candidatos da rodada
            sb.append(String.format("\nRodada %d:\n", rodada));
            Map<Integer, Integer> resultados = new HashMap<>();
            for (int id : candidatos) {
                int d1 = 1 + rng.nextInt(6);
                int d2 = 1 + rng.nextInt(6);
                int s  = d1 + d2;
                resultados.put(id, s);
                log.put(id, (log.getOrDefault(id, "") + (log.containsKey(id) ? " | " : "") + String.format("%d+%d=%d", d1, d2, s)));
                sb.append(String.format("  %s: %s\n", nomeJogador(id), log.get(id)));
            }

            // verifica empates
            Map<Integer, Set<Integer>> gruposPorSoma = agruparPorValor(resultados);
            Set<Integer> repetidos = gruposPorSoma.entrySet().stream()
                    .filter(e -> e.getValue().size() > 1)
                    .flatMap(e -> e.getValue().stream())
                    .collect(Collectors.toSet());

            if (repetidos.isEmpty()) {
                // todos com somas distintas -> ordena por soma desc e fecha
                List<Integer> ordem = resultados.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                ultimaOrdem = ordem;
                sb.append("\nOrdem final:\n");
                for (int i = 0; i < ordem.size(); i++) {
                    int id = ordem.get(i);
                    sb.append(String.format("  %dº %s  [%s]\n", i + 1, nomeJogador(id), log.get(id)));
                }
                saida.setText(sb.toString());
                return;
            }

            // mantém na disputa apenas os empatados; os demais já fixaram posição relativa
            candidatos = new ArrayList<>(repetidos);
            rodada++;
            if (rodada > 100) { // proteção teórica
                JOptionPane.showMessageDialog(this, "Falha no desempate após muitas rodadas.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    }

    private void aplicarSePossivel() {
        if (ultimaOrdem == null || ultimaOrdem.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Realize o sorteio antes de aplicar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        //controller.definirOrdemJogadores(ultimaOrdem);
        dispose();
    }

    private Map<Integer, Set<Integer>> agruparPorValor(Map<Integer, Integer> mapa) {
        Map<Integer, Set<Integer>> groups = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : mapa.entrySet()) {
            groups.computeIfAbsent(e.getValue(), k -> new HashSet<>()).add(e.getKey());
        }
        return groups;
    }

    private String nomeJogador(int id) {
        for (PlayerProfile p : perfis) if (p.getId() == id) return p.getNome();
        return "J" + (id + 1);
    }
} 
