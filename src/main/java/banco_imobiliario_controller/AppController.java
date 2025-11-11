package banco_imobiliario_controller;

// ============================================================================
// Imports
// ============================================================================
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

import java.text.Normalizer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import banco_imobiliario_models.GameModel;
import banco_imobiliario_ui.DefinicaoJogadoresDialog;
import banco_imobiliario_ui.JanelaInicialFrame;
import banco_imobiliario_ui.TabuleiroFrame;

public final class AppController {

    // =========================================================================
    // [1] Singleton + Estado principal da aplicação
    // =========================================================================
    private static final AppController INSTANCE = new AppController();

    private final GameModel model = new GameModel();
    private JFrame janelaAtual;
    private List<PlayerProfile> playerProfiles = new ArrayList<>();

    // Ordem sorteada (ids 0..N-1 do GameModel)
    private List<Integer> ordemJogadores = new ArrayList<>();

    // MAPEAMENTO LITERAL: nome da casa -> nome EXATO do arquivo da carta (territórios)
    private static final Map<String, String> MAPEAMENTO_CARTAS = criarMapCartas();

    private AppController() {}
    public static AppController getInstance() { return INSTANCE; }

    // =========================================================================
    // [2] Fluxo de UI (navegação entre janelas)
    // =========================================================================
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

    /** Recebe os perfis, sorteia a ordem e abre o tabuleiro. */
    public void confirmarDefinicaoJogadores(List<PlayerProfile> perfis) {
        Objects.requireNonNull(perfis, "perfis");
        if (perfis.isEmpty()) throw new IllegalArgumentException("Lista de jogadores vazia.");
        this.playerProfiles = new ArrayList<>(perfis);

        // ---- SORTEIO AUTOMÁTICO DA ORDEM ----
        SorteioResultado sr = sortearOrdemAutomatica(this.playerProfiles);
        this.ordemJogadores = sr.ordem;

        // Informa a ordem ao Model
        model.definirOrdemJogadores(this.ordemJogadores);

        // Garante tabuleiro carregado
        garantirTabuleiroCarregado();

        // Abre o tabuleiro
        fecharJanelaAtualSeExistir();
        TabuleiroFrame frame = new TabuleiroFrame(this);
        model.addObserver(frame);
        janelaAtual = frame;
        janelaAtual.setVisible(true);
        frame.update(model);

        // Popup apenas para visualização do sorteio
        JOptionPane.showMessageDialog(janelaAtual, sr.popup, "Ordem definida", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================================
    // [3] Suporte – garantia de tabuleiro carregado
    // =========================================================================
    /** Se ainda não houver tabuleiro no Model, carrega o oficial BR. */
    private void garantirTabuleiroCarregado() {
        try {
            model.getQuantidadeCasasTabuleiro(); // já existe? então ok
        } catch (Exception __) {
            model.carregarTabuleiroOficialBR();
        }
    }

    // =========================================================================
    // [4] Exibição de cartas de TERRITÓRIO (popup)
    // =========================================================================
    /** Abre diálogo da carta do território. Toda ação (comprar/construir) fica no diálogo. */
    public void exibirCartaTerritorio(String nomeCasa) {
        // Não abre se a casa atual tiver dono de outro jogador (aluguel é automático no Model).
        try {
            if (model.isCasaAtualPropriedadeComDonoDeOutro()) {
                return;
            }
        } catch (Throwable ignore) {}

        java.util.Optional<javax.swing.ImageIcon> icon = localizarIconeCarta(nomeCasa);
        if (!icon.isPresent()) {
            javax.swing.JOptionPane.showMessageDialog(
                janelaAtual,
                "Imagem da carta não encontrada para: " + nomeCasa +
                "\nColoque o arquivo em assets/territorios (ou subpastas) com o nome correto.",
                "Carta não encontrada",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Estados de habilitação vindos do Model
        boolean habilitarComprar = false;
        boolean habilitarCasa    = false;
        boolean habilitarHotel   = false;
        try { habilitarComprar = model.canComprarPropriedadeNaCasaAtual(); } catch (Throwable __) {}
        try { habilitarCasa    = model.canConstruirCasaNaCasaAtual();     } catch (Throwable __) {}
        try { habilitarHotel   = model.canConstruirHotelNaCasaAtual();    } catch (Throwable __) {}

        // Abre via reflexão para ser compatível com as 3 variantes do dialog.
        abrirDialogoCartaFlex(nomeCasa, icon.get(),
                              habilitarComprar, habilitarCasa, habilitarHotel);
    }

    /** Tenta construir casa; usado pelo diálogo. */
    public void construirCasaNaCasaAtual() {
        boolean ok = model.construirCasa();
        if (!ok) {
            // JOptionPane.showMessageDialog(janelaAtual, "Não foi possível construir uma casa agora.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Tenta construir hotel; usado pelo diálogo. */
    public void construirHotelNaCasaAtual() {
        boolean ok = model.construirHotel();
        if (!ok) {
            // JOptionPane.showMessageDialog(janelaAtual, "Não foi possível construir um hotel agora.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Comprar propriedade; usado pelo diálogo. */
    public void comprarPropriedade() {
        boolean ok = model.comprarPropriedade();
        if (!ok) {
            // JOptionPane.showMessageDialog(janelaAtual, "Não foi possível comprar esta propriedade.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Abre o CartaTerritorioDialog por reflexão, suportando:
     *  (1) Construtor completo: (JFrame,String,ImageIcon,Runnable,Runnable,Runnable,boolean,boolean,boolean,String)
     *  (2) Construtor médio:    (JFrame,String,ImageIcon,Runnable,boolean)
     *  (3) Construtor simples:  (JFrame,String,ImageIcon)
     */
    private void abrirDialogoCartaFlex(String titulo,
                                       javax.swing.ImageIcon icon,
                                       boolean habilitarComprar,
                                       boolean habilitarCasa,
                                       boolean habilitarHotel) {
        try {
            Class<?> clazz = Class.forName("banco_imobiliario_ui.CartaTerritorioDialog");

            // (1) completo
            try {
                java.lang.reflect.Constructor<?> c = clazz.getConstructor(
                        javax.swing.JFrame.class, String.class, javax.swing.ImageIcon.class,
                        Runnable.class, Runnable.class, Runnable.class,
                        boolean.class, boolean.class, boolean.class, String.class
                );
                Object dlg = c.newInstance(
                        (javax.swing.JFrame) janelaAtual, titulo, icon,
                        (Runnable) () -> { try { comprarPropriedade(); } catch (Throwable __) {} },
                        (Runnable) () -> { try { construirCasaNaCasaAtual(); } catch (Throwable __) {} },
                        (Runnable) () -> { try { construirHotelNaCasaAtual(); } catch (Throwable __) {} },
                        habilitarComprar, habilitarCasa, habilitarHotel,
                        null // resumo opcional
                );
                if (dlg instanceof javax.swing.JDialog) {
                    ((javax.swing.JDialog) dlg).setVisible(true);
                    return;
                }
            } catch (NoSuchMethodException ignore) {}

            // (2) médio (compra apenas)
            try {
                java.lang.reflect.Constructor<?> c = clazz.getConstructor(
                        javax.swing.JFrame.class, String.class, javax.swing.ImageIcon.class,
                        Runnable.class, boolean.class
                );
                Object dlg = c.newInstance(
                        (javax.swing.JFrame) janelaAtual, titulo, icon,
                        (Runnable) () -> { try { comprarPropriedade(); } catch (Throwable __) {} },
                        habilitarComprar
                );
                if (dlg instanceof javax.swing.JDialog) {
                    ((javax.swing.JDialog) dlg).setVisible(true);
                    return;
                }
            } catch (NoSuchMethodException ignore) {}

            // (3) simples (apenas imagem)
            try {
                java.lang.reflect.Constructor<?> c = clazz.getConstructor(
                        javax.swing.JFrame.class, String.class, javax.swing.ImageIcon.class
                );
                Object dlg = c.newInstance(
                        (javax.swing.JFrame) janelaAtual, titulo, icon
                );
                if (dlg instanceof javax.swing.JDialog) {
                    ((javax.swing.JDialog) dlg).setVisible(true);
                    return;
                }
            } catch (NoSuchMethodException ignore) {}

            // Se nenhuma assinatura bateu:
            JOptionPane.showMessageDialog(
                janelaAtual,
                "Não foi possível abrir a janela da carta (assinatura de construtor incompatível).",
                "Aviso",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                janelaAtual,
                "Falha ao abrir a carta: " + e.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // =========================================================================
    // [4B] Exibição de cartas de SORTE/REVÉS (popup)
    // =========================================================================
    public void exibirCartaSorteRevesPorNumero(int numero) {
        java.util.Optional<javax.swing.ImageIcon> icon = localizarIconeSorteReves(numero);
        String titulo = "Sorte/Revés #" + numero;
        if (icon.isPresent()) {
            // usa construtor simples (dialog específico de SR pode ser outro arquivo)
            try {
                Class<?> clazz = Class.forName("banco_imobiliario_ui.CartaTerritorioDialog");
                java.lang.reflect.Constructor<?> c = clazz.getConstructor(
                        javax.swing.JFrame.class, String.class, javax.swing.ImageIcon.class
                );
                Object dlg = c.newInstance((javax.swing.JFrame) janelaAtual, titulo, icon.get());
                if (dlg instanceof javax.swing.JDialog) ((javax.swing.JDialog) dlg).setVisible(true);
            } catch (Exception e) {
                // fallback mínimo
                javax.swing.JOptionPane.showMessageDialog(janelaAtual, "Carta SR: " + titulo, "Sorte/Revés", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            javax.swing.JOptionPane.showMessageDialog(
                janelaAtual,
                "Imagem da carta de Sorte/Revés não encontrada: " + titulo,
                "Carta não encontrada",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    public void exibirCartaSorteReves(banco_imobiliario_models.SorteRevesCard carta) {
        if (carta == null) return;
        exibirCartaSorteRevesPorNumero(carta.getId());
    }

    private java.util.Optional<javax.swing.ImageIcon> localizarIconeSorteReves(int numero) {
        String[] basesClasspath = {
            "/assets/sorte_reves/",
            "/assets/sorte-reves/",
            "/assets/sortereves/"
        };
        String[] basesFS = {
            "assets/sorte_reves",
            "assets/sorte-reves",
            "assets/sortereves"
        };
        String[] patterns = {
            "SR_%02d.png", "SR_%d.png",
            "SorteReves_%02d.png", "SorteReves_%d.png",
            "sorte_reves_%02d.png", "sorte_reves_%d.png",
            "chance%02d.png", "chance%d.png",
            "%02d.png", "%d.png"
        };
        // 1) classpath
        for (String base : basesClasspath) {
            for (String p : patterns) {
                String name = String.format(java.util.Locale.ROOT, p, numero);
                java.io.InputStream is = null;
                try {
                    is = getClass().getResourceAsStream(base + name);
                    if (is != null) {
                        byte[] bytes = readAllBytes8(is);
                        return java.util.Optional.of(new javax.swing.ImageIcon(bytes));
                    }
                } catch (Exception ignore) {
                } finally {
                    if (is != null) try { is.close(); } catch (Exception __) {}
                }
            }
        }

        // 2) filesystem
        for (java.io.File root : possibleRoots()) {
            for (String base : basesFS) {
                java.io.File dir = new java.io.File(root, base);
                for (String p : patterns) {
                    String name = String.format(java.util.Locale.ROOT, p, numero);
                    java.io.File f = resolveFileCaseInsensitive(dir, name);
                    if (f != null && f.exists()) {
                        return java.util.Optional.of(new javax.swing.ImageIcon(f.getAbsolutePath()));
                    }
                }
            }
        }

        // 3) recursiva por sufixo
        for (java.io.File root : possibleRoots()) {
            java.io.File assets = new java.io.File(root, "assets");
            java.io.File found = findRecursivelyBySuffixIgnoreCase(
                assets,
                new String[] { String.format(java.util.Locale.ROOT, "_%02d.png", numero),
                               String.format(java.util.Locale.ROOT, "_%d.png", numero),
                               String.format(java.util.Locale.ROOT, "%02d.png", numero),
                               String.format(java.util.Locale.ROOT, "%d.png", numero) },
                3
            );
            if (found != null) {
                return java.util.Optional.of(new javax.swing.ImageIcon(found.getAbsolutePath()));
            }
        }

        System.out.println("[Carta Sorte/Revés] não encontrada: #" + numero);
        return java.util.Optional.empty();
    }

    // =========================================================================
    // [4C] Imagem de TERRITÓRIO
    // =========================================================================
    private java.util.Optional<javax.swing.ImageIcon> localizarIconeCarta(String nomeCasa) {
        String key = chaveCarta(nomeCasa);
        String arquivo = MAPEAMENTO_CARTAS.get(key);
        if (arquivo == null) arquivo = nomeCasa + ".png";

        // classpath
        String[] basesClasspath = { "/assets/territorios/" };
        for (String base : basesClasspath) {
            java.io.InputStream is = null;
            try {
                is = getClass().getResourceAsStream(base + arquivo);
                if (is != null) {
                    byte[] bytes = readAllBytes8(is);
                    return java.util.Optional.of(new javax.swing.ImageIcon(bytes));
                }
            } catch (Exception ignore) {
            } finally {
                if (is != null) try { is.close(); } catch (Exception __) {}
            }
        }

        // filesystem
        String[] subdirs = { "assets/territorios" };
        for (java.io.File root : possibleRoots()) {
            for (String sub : subdirs) {
                java.io.File dir = new java.io.File(root, sub);
                java.io.File f = resolveFileCaseInsensitive(dir, arquivo);
                if (f != null && f.exists()) {
                    return java.util.Optional.of(new javax.swing.ImageIcon(f.getAbsolutePath()));
                }
            }
        }

        // recursiva
        for (java.io.File root : possibleRoots()) {
            java.io.File assets = new java.io.File(root, "assets");
            java.io.File f = findRecursivelyByNameIgnoreCase(assets, arquivo, 3);
            if (f != null) {
                return java.util.Optional.of(new javax.swing.ImageIcon(f.getAbsolutePath()));
            }
        }

        System.out.println("[Carta Território] não encontrada: " + arquivo + " (key=" + key + ")");
        return java.util.Optional.empty();
    }

    // =========================================================================
    // [5] Helpers de mapeamento/arquivo
    // =========================================================================
    private static Map<String, String> criarMapCartas() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();

        m.put("av. 9 de julho",                 "Av. 9 de Julho.png");
        m.put("av. atlântica",                  "Av. Atlântica.png");
        m.put("av. brasil",                     "Av. Brasil.png");
        m.put("av. brigadeiro faria lima",      "Av. Brigadeiro Faria Lima.png");
        m.put("av. europa",                     "Av. Europa.png");
        m.put("av. nossa s. de copacabana",     "Av. Nossa S. de Copacabana.png");
        m.put("av. pacaembu",                   "Av. Pacaembú.png");
        m.put("av. paulista",                   "Av. Paulista.png");
        m.put("av. presidente vargas",          "Av. Presidente Vargas.png");
        m.put("av. rebouças",                   "Av. Rebouças.png");
        m.put("av. vieira souto",               "Av. Vieira Souto.png");
        m.put("botafogo",                       "Botafogo.png");
        m.put("brooklin",                       "Brooklin.png");
        m.put("copacabana",                     "Copacabana.png");
        m.put("flamengo",                       "Flamengo.png");
        m.put("interlagos",                     "Interlagos.png");
        m.put("ipanema",                        "Ipanema.png");
        m.put("jardim europa",                  "Jardim Europa.png");
        m.put("jardim paulista",                "Jardim Paulista.png");
        m.put("leblon",                         "Leblon.png");
        m.put("morumbi",                        "Morumbi.png");
        m.put("rua augusta",                    "Rua Augusta.png");

        m.put("av. brig. faria lima",           "Av. Brigadero Faria Lima.png");
        m.put("av brig. faria lima",            "Av. Brigadero Faria Lima.png");
        m.put("av. brig faria lima",            "Av. Brigadero Faria Lima.png");
        m.put("av brig faria lima",             "Av. Brigadero Faria Lima.png");

        m.put("av. nossa sra. de copacabana",   "Av. Nossa S. de Copacabana.png");
        m.put("av. nossa senhora de copacabana","Av. Nossa S. de Copacabana.png");
        m.put("av nossa sra. de copacabana",    "Av. Nossa S. de Copacabana.png");
        m.put("av nossa s. de copacabana",      "Av. Nossa S. de Copacabana.png");

        m.put("av 9 de julho",                  "Av. 9 de Julho.png");
        m.put("av atlantica",                   "Av. Atlântica.png");
        m.put("av brasil",                      "Av. Brasil.png");
        m.put("av brigadeiro faria lima",       "Av. Brigadeiro Faria Lima.png");
        m.put("av europa",                      "Av. Europa.png");
        m.put("av pacaembu",                    "Av. Pacaembú.png");
        m.put("av paulista",                    "Av. Paulista.png");
        m.put("av presidente vargas",           "Av. Presidente Vargas.png");
        m.put("av reboucas",                    "Av. Rebouças.png");
        m.put("av vieira souto",                "Av. Vieira Souto.png");

        return Collections.unmodifiableMap(m);
    }

    private static String chaveCarta(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.toLowerCase(java.util.Locale.ROOT);
        n = n.replaceAll("\\s+", " ").trim();
        return n;
    }

    private static byte[] readAllBytes8(java.io.InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    // =========================================================================
    // [6] Ciclo de vida da janela atual
    // =========================================================================
    public void exibirErro(String mensagem) {
        SwingUtilities.invokeLater(
            () -> JOptionPane.showMessageDialog(janelaAtual, mensagem, "Erro", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void fecharJanelaAtualSeExistir() {
        if (janelaAtual != null) {
            if (janelaAtual instanceof TabuleiroFrame) {
                model.removeObserver((TabuleiroFrame) janelaAtual);
            }
            janelaAtual.dispose();
            janelaAtual = null;
        }
    }

    // =========================================================================
    // [7] Sorteio ordem
    // =========================================================================
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
            int s = d1 + d2;
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
        for (PlayerProfile p : playerProfiles)
            if (p.getId() == id) return p.getNome();
        return "J" + (id + 1);
    }

    // =========================================================================
    // [8] Getters
    // =========================================================================
    public boolean isNomeValido(String nome) {
        return nome != null && nome.matches("[A-Za-z0-9]{1,8}");
    }

    public GameModel getModel() { return model; }
    public List<PlayerProfile> getPlayerProfiles() { return Collections.unmodifiableList(playerProfiles); }
    public List<Integer> getOrdemJogadores() { return Collections.unmodifiableList(ordemJogadores); }

    // =========================================================================
    // [9] Utilidades de arquivo
    // =========================================================================
    private java.io.File[] possibleRoots() {
        return new java.io.File[] {
            new java.io.File("."),
            new java.io.File(".."),
            new java.io.File("../.."),
            new java.io.File("../../.."),
            new java.io.File("../../../..")
        };
    }

    private java.io.File resolveFileCaseInsensitive(java.io.File dir, String fileName) {
        try {
            if (!dir.isDirectory()) return null;
            java.io.File exact = new java.io.File(dir, fileName);
            if (exact.exists()) return exact;
            String target = fileName.toLowerCase(java.util.Locale.ROOT);
            java.io.File[] list = dir.listFiles();
            if (list == null) return null;
            for (java.io.File f : list) {
                if (f.isFile() && f.getName().toLowerCase(java.util.Locale.ROOT).equals(target)) {
                    return f;
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private java.io.File findRecursivelyByNameIgnoreCase(java.io.File dir, String fileName, int maxDepth) {
        if (dir == null || maxDepth < 0 || !dir.isDirectory()) return null;
        java.io.File f = resolveFileCaseInsensitive(dir, fileName);
        if (f != null) return f;
        java.io.File[] sub = dir.listFiles(java.io.File::isDirectory);
        if (sub == null) return null;
        for (java.io.File d : sub) {
            java.io.File r = findRecursivelyByNameIgnoreCase(d, fileName, maxDepth - 1);
            if (r != null) return r;
        }
        return null;
    }

    private java.io.File findRecursivelyBySuffixIgnoreCase(java.io.File dir, String[] suffixes, int maxDepth) {
        if (dir == null || maxDepth < 0 || !dir.isDirectory()) return null;
        java.io.File[] list = dir.listFiles();
        if (list == null) return null;
        for (java.io.File f : list) {
            if (f.isFile()) {
                String name = f.getName().toLowerCase(java.util.Locale.ROOT);
                for (String suf : suffixes) {
                    if (name.endsWith(suf.toLowerCase(java.util.Locale.ROOT))) {
                        return f;
                    }
                }
            }
        }
        java.io.File[] sub = dir.listFiles(java.io.File::isDirectory);
        if (sub == null) return null;
        for (java.io.File d : sub) {
            java.io.File r = findRecursivelyBySuffixIgnoreCase(d, suffixes, maxDepth - 1);
            if (r != null) return r;
        }
        return null;
    }
}
