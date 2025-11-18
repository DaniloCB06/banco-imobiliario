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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.text.Normalizer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ImageIcon;

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
        // garante tabuleiro carregado para quem entrar direto
        garantirTabuleiroCarregado();
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
    // [4] Exibição de cartas de TERRITÓRIO
    // =========================================================================

    /**
     * Abre diálogo da carta do território com ações (comprar/construir) quando aplicável.
     * Se o nome parecer "Sorte/Revés #N", redireciona para o fluxo de SR.
     */
    public void exibirCartaTerritorio(String nomeCasa) {
        // redireciona, se por engano vier "Sorte/Revés #N"
        Integer srNum = detectarSorteRevesNoNome(nomeCasa);
        if (srNum != null) {
            exibirCartaSorteRevesPorNumero(srNum.intValue());
            return;
        }
        exibirCartaTerritorioImpl((JFrame) janelaAtual, nomeCasa, /*somenteVisualizar*/ false);
    }

    /**
     * Versão "somente visualização" (SEM botões). Use no Banco de Cartas.
     */
    public void exibirCartaTerritorioSomenteVisualizacao(String nomeCasa) {
        Integer srNum = detectarSorteRevesNoNome(nomeCasa);
        if (srNum != null) {
            exibirCartaSorteRevesPorNumero(srNum.intValue());
            return;
        }
        exibirCartaTerritorioImpl((JFrame) janelaAtual, nomeCasa, /*somenteVisualizar*/ true);
    }

    private Integer detectarSorteRevesNoNome(String nome) {
        if (nome == null) return null;
        String s = nome.toLowerCase(java.util.Locale.ROOT).trim();
        Matcher m = Pattern.compile("sorte\\s*[/ ]?\\s*rev(?:e|é)s\\s*#?\\s*(\\d+)").matcher(s);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignore) {}
        }
        return null;
    }

    private void exibirCartaTerritorioImpl(JFrame owner, String nomeCasa, boolean somenteVisualizar) {
        // Se for exibição normal (não-readonly), bloqueia se a casa atual tiver dono de outro.
        if (!somenteVisualizar) {
            try {
                if (model.isCasaAtualPropriedadeComDonoDeOutro()) {
                    return;
                }
            } catch (Throwable ignore) {}
        }

        java.util.Optional<ImageIcon> icon = localizarIconeCarta(nomeCasa);
        if (!icon.isPresent()) {
            JOptionPane.showMessageDialog(
                owner,
                "Imagem da carta não encontrada para: " + nomeCasa +
                "\nColoque o arquivo em assets/territorios (ou subpastas) com o nome correto.",
                "Carta não encontrada",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        boolean habilitarComprar = false;
        boolean habilitarCasa    = false;
        boolean habilitarHotel   = false;

        if (!somenteVisualizar) {
            try { habilitarComprar = model.canComprarPropriedadeNaCasaAtual(); } catch (Throwable __) {}
            try { habilitarCasa    = model.canConstruirCasaNaCasaAtual();     } catch (Throwable __) {}
            try { habilitarHotel   = model.canConstruirHotelNaCasaAtual();    } catch (Throwable __) {}
        }

        abrirDialogoCartaFlex(nomeCasa, icon.get(),
                              habilitarComprar, habilitarCasa, habilitarHotel,
                              somenteVisualizar);
    }

    /** Tenta construir casa; usado pelo diálogo. */
    public void construirCasaNaCasaAtual() {
        boolean ok = model.construirCasa();
        if (!ok) {
            // opcional: JOptionPane.showMessageDialog(janelaAtual, "Não foi possível construir uma casa agora.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Tenta construir hotel; usado pelo diálogo. */
    public void construirHotelNaCasaAtual() {
        boolean ok = model.construirHotel();
        if (!ok) {
            // opcional: JOptionPane.showMessageDialog(janelaAtual, "Não foi possível construir um hotel agora.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Comprar propriedade; usado pelo diálogo. */
    public void comprarPropriedade() {
        boolean ok = model.comprarPropriedade();
        if (!ok) {
            // opcional: JOptionPane.showMessageDialog(janelaAtual, "Não foi possível comprar esta propriedade.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Abre o CartaTerritorioDialog por reflexão, suportando:
     *  (A) Somente visualização -> construtor simples: (JFrame,String,ImageIcon)
     *  (B) Construtor completo:  (JFrame,String,ImageIcon,Runnable,Runnable,Runnable,boolean,boolean,boolean,String)
     *  (C) Construtor médio:     (JFrame,String,ImageIcon,Runnable,boolean)
     */
    private void abrirDialogoCartaFlex(String titulo,
                                       ImageIcon icon,
                                       boolean habilitarComprar,
                                       boolean habilitarCasa,
                                       boolean habilitarHotel,
                                       boolean somenteVisualizar) {
        try {
            Class<?> clazz = Class.forName("banco_imobiliario_ui.CartaTerritorioDialog");

            // (A) SOMENTE VISUALIZAÇÃO: tenta o construtor simples (sem botões)
            if (somenteVisualizar) {
                try {
                    java.lang.reflect.Constructor<?> c = clazz.getConstructor(
                            javax.swing.JFrame.class, String.class, javax.swing.ImageIcon.class
                    );
                    Object dlg = c.newInstance((javax.swing.JFrame) janelaAtual, titulo, icon);
                    if (dlg instanceof javax.swing.JDialog) {
                        ((javax.swing.JDialog) dlg).setVisible(true);
                        return;
                    }
                } catch (NoSuchMethodException ignore) {
                    // se não houver o simples, cai para os demais, porém com botões desabilitados
                    habilitarComprar = false;
                    habilitarCasa = false;
                    habilitarHotel = false;
                }
            }

            // (B) completo
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

            // (C) médio (compra apenas)
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
        banco_imobiliario_models.GameModel.SorteRevesCard info =
                model.getCartaSorteRevesPorNumero(numero).orElse(null);

        String tituloBase = "Sorte/Revés #" + numero;
        String tituloCarta = info != null ? info.getTitulo() : tituloBase;
        String descricao = info != null ? info.getDescricao() : "";

        java.util.Optional<ImageIcon> icon = localizarIconeSorteReves(numero);

        if (icon.isPresent()) {
            // Preferir o CartaSorteRevesDialog se existir
            try {
                Class<?> clazz = Class.forName("banco_imobiliario_ui.CartaSorteRevesDialog");
                java.lang.reflect.Constructor<?> c = clazz.getConstructor(
                        javax.swing.JFrame.class, int.class, String.class, String.class, javax.swing.ImageIcon.class
                );
                Object dlg = c.newInstance((javax.swing.JFrame) janelaAtual, numero, tituloCarta, descricao, icon.get());
                if (dlg instanceof javax.swing.JDialog) {
                    ((javax.swing.JDialog) dlg).setVisible(true);
                    return;
                }
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                // Se não existir o dialog específico, tenta o de território no modo simples
                try {
                    Class<?> clazz = Class.forName("banco_imobiliario_ui.CartaTerritorioDialog");
                    java.lang.reflect.Constructor<?> c = clazz.getConstructor(
                            javax.swing.JFrame.class, String.class, javax.swing.ImageIcon.class
                    );
                    String tituloDialogo = tituloBase + " — " + tituloCarta;
                    Object dlg = c.newInstance((javax.swing.JFrame) janelaAtual, tituloDialogo, icon.get());
                    if (dlg instanceof javax.swing.JDialog) {
                        ((javax.swing.JDialog) dlg).setVisible(true);
                        return;
                    }
                } catch (Exception ignore) {}
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        StringBuilder mensagem = new StringBuilder();
        mensagem.append(tituloBase);
        if (descricao != null && !descricao.isEmpty()) {
            mensagem.append("\n").append(descricao);
        }

        JOptionPane.showMessageDialog(
            janelaAtual,
            mensagem.toString(),
            "Carta de Sorte/Revés",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public void exibirCartaSorteReves(banco_imobiliario_models.GameModel.SorteRevesCard carta) {
        if (carta == null) return;
        exibirCartaSorteRevesPorNumero(carta.getNumero());
    }

    private java.util.Optional<ImageIcon> localizarIconeSorteReves(int numero) {
        // nomes que você tem: "chanceN.png" (N = 1..30)
        String[] patterns = {
            "chance%02d.png", "chance%d.png",        // principal
            "SR_%02d.png", "SR_%d.png",              // alternativas comuns
            "SorteReves_%02d.png", "SorteReves_%d.png",
            "sorte_reves_%02d.png", "sorte_reves_%d.png",
            "%02d.png", "%d.png"                     // última chance
        };

        // 1) classpath
        String[] basesClasspath = {
            "/assets/sorte_reves/",
            "/assets/sorte-reves/",
            "/assets/sortereves/",
            "/assets/territorios/"                   // caso estejam aqui
        };
        for (String base : basesClasspath) {
            for (String p : patterns) {
                String name = String.format(java.util.Locale.ROOT, p, numero);
                java.io.InputStream is = null;
                try {
                    is = getClass().getResourceAsStream(base + name);
                    if (is != null) {
                        byte[] bytes = readAllBytes8(is);
                        return java.util.Optional.of(new ImageIcon(bytes));
                    }
                } catch (Exception ignore) {
                } finally {
                    if (is != null) try { is.close(); } catch (Exception __) {}
                }
            }
        }

        // 2) filesystem (diretórios explícitos)
        String[] basesFS = {
            "assets/sorte_reves",
            "assets/sorte-reves",
            "assets/sortereves",
            "assets/territorios"
        };
        for (java.io.File root : possibleRoots()) {
            for (String base : basesFS) {
                java.io.File dir = new java.io.File(root, base);
                for (String p : patterns) {
                    String name = String.format(java.util.Locale.ROOT, p, numero);
                    java.io.File f = resolveFileCaseInsensitive(dir, name);
                    if (f != null && f.exists()) {
                        return java.util.Optional.of(new ImageIcon(f.getAbsolutePath()));
                    }
                }
            }
        }

        // 3) busca recursiva dentro de "assets"
        for (java.io.File root : possibleRoots()) {
            java.io.File assets = new java.io.File(root, "assets");
            // tenta por nome exato
            for (String p : patterns) {
                String name = String.format(java.util.Locale.ROOT, p, numero);
                java.io.File f = findRecursivelyByNameIgnoreCase(assets, name, 3);
                if (f != null) return java.util.Optional.of(new ImageIcon(f.getAbsolutePath()));
            }
            // fallback por sufixo específico
            String suf1 = String.format(java.util.Locale.ROOT, "chance%02d.png", numero);
            String suf2 = String.format(java.util.Locale.ROOT, "chance%d.png", numero);
            java.io.File f2 = findRecursivelyBySuffixIgnoreCase(assets, new String[]{suf1, suf2}, 3);
            if (f2 != null) return java.util.Optional.of(new ImageIcon(f2.getAbsolutePath()));
        }

        System.out.println("[Carta Sorte/Revés] não encontrada: chance" + numero + ".png");
        return java.util.Optional.empty();
    }

    // =========================================================================
    // [4C] Imagem de TERRITÓRIO
    // =========================================================================
    private java.util.Optional<ImageIcon> localizarIconeCarta(String nomeCasa) {
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
                    return java.util.Optional.of(new ImageIcon(bytes));
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
                    return java.util.Optional.of(new ImageIcon(f.getAbsolutePath()));
                }
            }
        }

        // recursiva
        for (java.io.File root : possibleRoots()) {
            java.io.File assets = new java.io.File(root, "assets");
            java.io.File f = findRecursivelyByNameIgnoreCase(assets, arquivo, 3);
            if (f != null) {
                return java.util.Optional.of(new ImageIcon(f.getAbsolutePath()));
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

        // mapeamentos tolerantes para a Faria Lima (arquivo que você tem)
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
            if (dir == null || !dir.isDirectory()) return null;
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
