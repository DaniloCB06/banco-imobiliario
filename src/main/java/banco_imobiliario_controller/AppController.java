package banco_imobiliario_controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import banco_imobiliario_models.GameModel;
import banco_imobiliario_ui.DefinicaoJogadoresDialog;
import banco_imobiliario_ui.JanelaInicialFrame;
import banco_imobiliario_ui.TabuleiroFrame;

public final class AppController {

    private static final AppController INSTANCE = new AppController();

    private final GameModel model = new GameModel();
    private JFrame janelaAtual;
    private List<PlayerProfile> playerProfiles = new ArrayList<>();

    // Ordem sorteada (ids 0..N-1 do GameModel)
    private List<Integer> ordemJogadores = new ArrayList<>();

    private AppController() {}

    public static AppController getInstance() {
        return INSTANCE;
    }

    // ========================= Fluxo de UI =========================

    public void exibirJanelaInicial() {
        SwingUtilities.invokeLater(() -> {
            fecharJanelaAtualSeExistir();
            janelaAtual = new JanelaInicialFrame(this);
            janelaAtual.setVisible(true);
        });
    }

    public void iniciarNovaPartida(int nJogadores) {
        try {
            if (nJogadores < 3 || nJogadores > 6) {
                throw new IllegalArgumentException("Quantidade de jogadores deve estar entre 3 e 6.");
            }
            model.novaPartida(nJogadores, null);
            abrirDefinicaoJogadores(nJogadores);
        } catch (RuntimeException ex) {
            exibirErro(ex.getMessage());
        }
    }

    private void abrirDefinicaoJogadores(int nJogadores) {
        DefinicaoJogadoresDialog dlg = new DefinicaoJogadoresDialog(janelaAtual, this, nJogadores);
        dlg.setLocationRelativeTo(janelaAtual);
        dlg.setVisible(true);
    }

    /** Recebe os perfis, sorteia automaticamente a ordem e abre o tabuleiro. */
    public void confirmarDefinicaoJogadores(List<PlayerProfile> perfis) {
        Objects.requireNonNull(perfis, "perfis");
        if (perfis.isEmpty()) throw new IllegalArgumentException("Lista de jogadores vazia.");
        this.playerProfiles = new ArrayList<>(perfis);

        // ---- SORTEIO AUTOMÁTICO DA ORDEM (com desempate) ----
        SorteioResultado sr = sortearOrdemAutomatica(this.playerProfiles);
        this.ordemJogadores = sr.ordem;

        // Informa a ordem ao Model
        model.definirOrdemJogadores(this.ordemJogadores);

        // >>> GARANTE UM TABULEIRO DE TESTE CARREGADO <<<
        garantirTabuleiroCarregado();

        // Abre o tabuleiro já com a ordem definida (overlay visível)
        fecharJanelaAtualSeExistir();
        janelaAtual = new TabuleiroFrame(this);
        janelaAtual.setVisible(true);

        // Popup só para visualização do sorteio
        JOptionPane.showMessageDialog(
            janelaAtual, sr.popup, "Ordem definida", JOptionPane.INFORMATION_MESSAGE
        );

        if (janelaAtual instanceof TabuleiroFrame) {
            ((TabuleiroFrame) janelaAtual).repaintBoard();
        }
    }

    /** Se ainda não houver tabuleiro no Model, carrega um básico (40 casas, prisão em 10 e
     *  “vá para a prisão” em 30). */
    private void garantirTabuleiroCarregado() {
        try {
            // se já tiver, esse get não lança exceção
            model.getQuantidadeCasasTabuleiro();
        } catch (Exception __) {
            // cria um tabuleiro simples para testes
            model.carregarTabuleiroBasicoComPrisao(40, 10, 30);
        }
    }

    public void exibirErro(String mensagem) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(janelaAtual, mensagem, "Erro", JOptionPane.ERROR_MESSAGE));
    }

    private void fecharJanelaAtualSeExistir() {
        if (janelaAtual != null) {
            janelaAtual.dispose();
            janelaAtual = null;
        }
    }

    // ========================= SORTEIO: lógica interna =========================

    private static final class SorteioResultado {
        final List<Integer> ordem;
        final String popup;
        SorteioResultado(List<Integer> ordem, String popup) {
            this.ordem = ordem;
            this.popup = popup;
        }
    }

    private SorteioResultado sortearOrdemAutomatica(List<PlayerProfile> perfis) {
        Random rng = new Random(System.nanoTime());

        Map<Integer, StringBuilder> logs = new HashMap<>();
        for (PlayerProfile p : perfis) logs.put(p.getId(), new StringBuilder());

        List<Integer> candidatos = new ArrayList<>();
        for (PlayerProfile p : perfis) candidatos.add(p.getId());
        List<Integer> ordemFinal = resolveGrupo(candidatos, rng, logs);

        StringBuilder sb = new StringBuilder("Ordem sorteada:\n");
        for (int i = 0; i < ordemFinal.size(); i++) {
            int id = ordemFinal.get(i);
            String nome = nomePorId(id);
            String rolls = logs.get(id).toString();
            sb.append(String.format("%dº: %s%s\n", i + 1, nome, rolls.isEmpty() ? "" : "  [" + rolls + "]"));
        }
        return new SorteioResultado(ordemFinal, sb.toString());
    }

    private List<Integer> resolveGrupo(List<Integer> grupo, Random rng, Map<Integer, StringBuilder> logs) {
        Map<Integer, Integer> soma = new LinkedHashMap<>();
        for (int id : grupo) {
            int d1 = 1 + rng.nextInt(6);
            int d2 = 1 + rng.nextInt(6);
            int s  = d1 + d2;
            soma.put(id, s);

            StringBuilder sb = logs.get(id);
            if (sb.length() > 0) sb.append(" | ");
            sb.append(d1).append('+').append(d2).append('=').append(s);
        }

        Map<Integer, Set<Integer>> porSoma = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : soma.entrySet()) {
            porSoma.computeIfAbsent(e.getValue(), k -> new HashSet<>()).add(e.getKey());
        }

        List<Integer> somasDesc = new ArrayList<>(porSoma.keySet());
        somasDesc.sort((a, b) -> Integer.compare(b, a));

        List<Integer> ordem = new ArrayList<>();
        for (int s : somasDesc) {
            Set<Integer> ids = porSoma.get(s);
            if (ids.size() == 1) {
                ordem.add(ids.iterator().next());
            } else {
                ordem.addAll(resolveGrupo(new ArrayList<>(ids), rng, logs));
            }
        }
        return ordem;
    }

    private String nomePorId(int id) {
        for (PlayerProfile p : playerProfiles) if (p.getId() == id) return p.getNome();
        return "J" + (id + 1);
    }

    // ========================= Validações usadas pela UI =========================

    public boolean isNomeValido(String nome) {
        return nome != null && nome.matches("[A-Za-z0-9]{1,8}");
    }

    // ========================= Acesso para a UI (read-only) =========================

    public GameModel getModel() {
        return model;
    }

    public List<PlayerProfile> getPlayerProfiles() {
        return Collections.unmodifiableList(playerProfiles);
    }

    public List<Integer> getOrdemJogadores() {
        return Collections.unmodifiableList(ordemJogadores);
    }
}
