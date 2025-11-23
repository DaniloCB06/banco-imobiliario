package banco_imobiliario_controller;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import banco_imobiliario_models.GameModel;

public final class GamePersistenceService {

    public static final class LoadedGame {
        private final GameModel.SaveState state;
        private final List<PlayerProfile> perfis;

        LoadedGame(GameModel.SaveState state, List<PlayerProfile> perfis) {
            this.state = Objects.requireNonNull(state);
            this.perfis = Collections.unmodifiableList(new ArrayList<>(perfis));
        }

        public GameModel.SaveState getState() { return state; }
        public List<PlayerProfile> getPerfis() { return perfis; }
    }

    private static final String COMMENT = "Banco Imobiliario Save";

    public void salvar(java.io.File arquivo,
                       GameModel.SaveState estado,
                       List<PlayerProfile> perfis) throws IOException {
        Objects.requireNonNull(arquivo, "arquivo");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(perfis, "perfis");

        List<Map.Entry<String, String>> linhas = new ArrayList<>();
        addLinha(linhas, "profiles.count", String.valueOf(perfis.size()));
        for (int i = 0; i < perfis.size(); i++) {
            PlayerProfile p = perfis.get(i);
            String linha = p.getId() + "|" + p.getNome() + "|" + p.getCorHex() + "|" + p.getPawnIndex();
            addLinha(linhas, "profiles." + i, linha);
        }

        addLinha(linhas, "model.players.count", String.valueOf(estado.getJogadores().size()));
        for (int i = 0; i < estado.getJogadores().size(); i++) {
            GameModel.PlayerState ps = estado.getJogadores().get(i);
            String linha = ps.getId() + "," + ps.getSaldo() + "," + ps.getPosicao() + ","
                         + flag(ps.isAtivo()) + "," + flag(ps.isNaPrisao()) + "," + flag(ps.hasCartaSaidaLivre());
            addLinha(linhas, "model.player." + i, linha);
        }

        addLinha(linhas, "model.turno.ordem", join(estado.getOrdemTurno()));
        addLinha(linhas, "model.turno.index", String.valueOf(estado.getTurnoIndex()));
        addLinha(linhas, "model.turno.duplas", String.valueOf(estado.getTurnoDuplasConsecutivas()));
        addLinha(linhas, "model.turno.ultimo", estado.getTurnoUltimoD1() + "," + estado.getTurnoUltimoD2());

        addLinha(linhas, "model.dados.ultimo", formatOptional(estado.getUltimoD1()) + "," + formatOptional(estado.getUltimoD2()));
        addLinha(linhas, "model.jaLancou", flag(estado.isJaLancouNesteTurno()));
        addLinha(linhas, "model.devePrisao", flag(estado.isDeveIrParaPrisaoPorTerceiraDupla()));
        addLinha(linhas, "model.salvar.habilitado", flag(estado.isSalvamentoDisponivel()));
        addLinha(linhas, "model.queda.pos", formatOptional(estado.getPosicaoDaQuedaAtual()));
        addLinha(linhas, "model.queda.construiu", flag(estado.isJaConstruiuNestaQueda()));
        addLinha(linhas, "model.queda.comprou", flag(estado.isAcabouDeComprarNestaQueda()));
        addLinha(linhas, "model.banco.saldo", String.valueOf(estado.getBancoSaldo()));

        addLinha(linhas, "sr.tamanho", String.valueOf(estado.getTamanhoBaralhoSR()));
        addLinha(linhas, "sr.ponteiro", String.valueOf(estado.getPonteiroBaralhoSR()));
        addLinha(linhas, "sr.ultima", formatOptional(estado.getUltimaCartaNumero()));
        addLinha(linhas, "sr.buffer", formatOptional(estado.getCartaBufferNumero()));

        List<Map.Entry<Integer, Set<Integer>>> cartasOrdenadas = new ArrayList<>(estado.getCartasSRPorJogador().entrySet());
        cartasOrdenadas.sort(Map.Entry.comparingByKey());
        for (Map.Entry<Integer, Set<Integer>> entry : cartasOrdenadas) {
            addLinha(linhas, "sr.cards." + entry.getKey(), join(entry.getValue()));
        }

        List<GameModel.PropertyState> propriedadesOrdenadas = new ArrayList<>(estado.getPropriedades());
        propriedadesOrdenadas.sort(Comparator.comparingInt(GameModel.PropertyState::getPosicao));
        for (GameModel.PropertyState prop : propriedadesOrdenadas) {
            String key = "property." + prop.getPosicao();
            String linha = prop.getDonoId() + "," + prop.getNumCasas() + "," + flag(prop.hasHotel());
            addLinha(linhas, key, linha);
        }

        escreverArquivoOrdenado(arquivo, linhas);
    }

    public LoadedGame carregar(java.io.File arquivo) throws IOException {
        Objects.requireNonNull(arquivo, "arquivo");
        if (!arquivo.exists()) {
            throw new IOException("Arquivo não encontrado: " + arquivo.getAbsolutePath());
        }

        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(arquivo.toPath(), StandardCharsets.US_ASCII)) {
            props.load(reader);
        }

        List<PlayerProfile> perfis = lerPerfis(props);
        List<GameModel.PlayerState> jogadores = lerJogadores(props);
        if (perfis.size() != jogadores.size()) {
            throw new IOException("Arquivo inconsistente: número de perfis difere da quantidade de jogadores.");
        }

        List<GameModel.PropertyState> propriedades = lerPropriedades(props);
        List<Integer> ordem = parseIntList(require(props, "model.turno.ordem"));
        int turnoIndex = parseInt(require(props, "model.turno.index"));
        int turnoDuplas = parseInt(require(props, "model.turno.duplas"));
        int[] turnoUltimo = parsePair(require(props, "model.turno.ultimo"));
        int[] dadosUltimo = parsePair(require(props, "model.dados.ultimo"));

        boolean jaLancou = parseFlag(require(props, "model.jaLancou"));
        boolean devePrisao = parseFlag(require(props, "model.devePrisao"));
        boolean salvarHabilitado = parseFlag(require(props, "model.salvar.habilitado"));
        Integer quedaPos = parseOptional(require(props, "model.queda.pos"));
        boolean quedaConstruiu = parseFlag(require(props, "model.queda.construiu"));
        boolean quedaComprou = parseFlag(require(props, "model.queda.comprou"));
        int bancoSaldo = parseInt(require(props, "model.banco.saldo"));

        int tamanhoBaralho = parseInt(require(props, "sr.tamanho"));
        int ponteiroBaralho = parseInt(require(props, "sr.ponteiro"));
        Integer ultimaCarta = parseOptional(require(props, "sr.ultima"));
        Integer bufferCarta = parseOptional(require(props, "sr.buffer"));

        Map<Integer, Set<Integer>> cartasSR = lerCartasSR(props);

        GameModel.SaveState state = new GameModel.SaveState(
                jogadores,
                propriedades,
                ordem,
                turnoIndex,
                turnoDuplas,
                turnoUltimo[0],
                turnoUltimo[1],
                toNullable(dadosUltimo[0]),
                toNullable(dadosUltimo[1]),
                jaLancou,
                devePrisao,
                quedaPos,
                quedaConstruiu,
                quedaComprou,
                salvarHabilitado,
                bancoSaldo,
                tamanhoBaralho,
                ponteiroBaralho,
                cartasSR,
                ultimaCarta,
                bufferCarta
        );

        return new LoadedGame(state, perfis);
    }

    private static List<PlayerProfile> lerPerfis(Properties props) throws IOException {
        int total = parseInt(require(props, "profiles.count"));
        List<PlayerProfile> perfis = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            String raw = require(props, "profiles." + i);
            String[] parts = raw.split("\\|");
            if (parts.length < 4) {
                throw new IOException("Perfil inválido no slot " + i);
            }
            int id = parseInt(parts[0]);
            String nome = parts[1];
            Color cor;
            try {
                cor = Color.decode(parts[2]);
            } catch (NumberFormatException ex) {
                throw new IOException("Cor inválida para o perfil " + i + ": " + parts[2], ex);
            }
            int pawn = parseInt(parts[3]);
            perfis.add(new PlayerProfile(id, nome, cor, pawn));
        }
        perfis.sort(Comparator.comparingInt(PlayerProfile::getId));
        return perfis;
    }

    private static List<GameModel.PlayerState> lerJogadores(Properties props) throws IOException {
        int total = parseInt(require(props, "model.players.count"));
        List<GameModel.PlayerState> jogadores = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            String raw = require(props, "model.player." + i);
            String[] parts = raw.split(",");
            if (parts.length < 6) {
                throw new IOException("Registro de jogador inválido no slot " + i);
            }
            int id = parseInt(parts[0]);
            int saldo = parseInt(parts[1]);
            int pos = parseInt(parts[2]);
            boolean ativo = parseFlag(parts[3]);
            boolean prisao = parseFlag(parts[4]);
            boolean carta = parseFlag(parts[5]);
            jogadores.add(new GameModel.PlayerState(id, saldo, pos, ativo, prisao, carta));
        }
        return jogadores;
    }

    private static List<GameModel.PropertyState> lerPropriedades(Properties props) throws IOException {
        List<GameModel.PropertyState> propriedades = new ArrayList<>();
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith("property.")) {
                continue;
            }
            int pos = parseInt(key.substring("property.".length()));
            String raw = require(props, key);
            String[] parts = raw.split(",");
            if (parts.length < 3) {
                throw new IOException("Propriedade inválida na posição " + pos);
            }
            int donoId = parseInt(parts[0]);
            int numCasas = parseInt(parts[1]);
            boolean hotel = parseFlag(parts[2]);
            propriedades.add(new GameModel.PropertyState(pos, donoId, numCasas, hotel));
        }
        propriedades.sort(Comparator.comparingInt(GameModel.PropertyState::getPosicao));
        return propriedades;
    }

    private static Map<Integer, Set<Integer>> lerCartasSR(Properties props) throws IOException {
        Map<Integer, Set<Integer>> mapa = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith("sr.cards.")) {
                continue;
            }
            int id = parseInt(key.substring("sr.cards.".length()));
            String raw = props.getProperty(key, "").trim();
            Set<Integer> cartas = new HashSet<>();
            if (!raw.isEmpty()) {
                for (String token : raw.split(",")) {
                    if (!token.trim().isEmpty()) {
                        cartas.add(parseInt(token.trim()));
                    }
                }
            }
            mapa.put(id, cartas);
        }
        return mapa;
    }

    private static String require(Properties props, String key) throws IOException {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IOException("Campo ausente no arquivo: " + key);
        }
        return value.trim();
    }

    private static String join(List<Integer> valores) {
        return valores.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String join(Set<Integer> valores) {
        return valores.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String flag(boolean valor) {
        return valor ? "1" : "0";
    }

    private static boolean parseFlag(String raw) {
        return "1".equals(raw.trim());
    }

    private static int parseInt(String raw) throws IOException {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IOException("Não foi possível converter número inteiro: " + raw, ex);
        }
    }

    private static int[] parsePair(String raw) throws IOException {
        String[] parts = raw.split(",");
        if (parts.length < 2) {
            throw new IOException("Valor inválido: " + raw);
        }
        return new int[] { parseInt(parts[0]), parseInt(parts[1]) };
    }

    private static List<Integer> parseIntList(String raw) throws IOException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IOException("Lista de inteiros vazia.");
        }
        List<Integer> valores = new ArrayList<>();
        for (String token : raw.split(",")) {
            valores.add(parseInt(token));
        }
        return valores;
    }

    private static String formatOptional(Integer valor) {
        return valor == null ? "-1" : String.valueOf(valor);
    }

    private static Integer parseOptional(String raw) throws IOException {
        int valor = parseInt(raw);
        return valor < 0 ? null : Integer.valueOf(valor);
    }

    private static Integer toNullable(int valor) {
        return valor < 0 ? null : Integer.valueOf(valor);
    }

    private static void addLinha(List<Map.Entry<String, String>> linhas, String chave, String valor) {
        linhas.add(new AbstractMap.SimpleEntry<>(chave, valor));
    }

    private void escreverArquivoOrdenado(java.io.File arquivo,
                                         List<Map.Entry<String, String>> linhas) throws IOException {
        String newline = System.lineSeparator();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ROOT);
        try (Writer writer = Files.newBufferedWriter(arquivo.toPath(), StandardCharsets.US_ASCII)) {
            writer.write('#');
            writer.write(COMMENT);
            writer.write(newline);
            writer.write('#');
            writer.write(sdf.format(new Date()));
            writer.write(newline);
            for (Map.Entry<String, String> entry : linhas) {
                writer.write(entry.getKey());
                writer.write('=');
                writer.write(entry.getValue());
                writer.write(newline);
            }
        }
    }
}
