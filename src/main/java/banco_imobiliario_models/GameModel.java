package banco_imobiliario_models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.text.Normalizer;
import java.util.Locale;

/**
 * API pública do Model (Iteração 1 + Sorte/Revés).
 *
 * Regras cobertas: #1, #2, #3, #4, #5, #6, #7.
 * Extensão: Sorte/Revés (baralho simples interno + banco por jogador).
 */
public class GameModel {

    // =====================================================================================
    // ESTADO GLOBAL
    // =====================================================================================

    private RandomProvider rng;
    private final Dado dado1 = new Dado();
    private final Dado dado2 = new Dado();
    private Turno turno;
    private Banco banco;
    private final List<Jogador> jogadores = new ArrayList<>();
    private Tabuleiro tabuleiro;
    private boolean salvamentoDisponivel = true;
    private boolean partidaEncerrada = false;
    private ResultadoPartida resultadoPartida = null;

    // Lançamento corrente
    private Integer ultimoD1 = null, ultimoD2 = null;

    // Controle: só uma rolagem por turno (liberado novamente se sair DUPLA válida)
    private boolean jaLancouNesteTurno = false;

    // Contexto da queda (para permitir construir na casa em que caiu)
    private Integer posicaoDaQuedaAtual = null;
    private boolean jaConstruiuNestaQueda = false;
    private boolean acabouDeComprarNestaQueda = false;

    // Sinalização de 3ª dupla consecutiva
    private boolean deveIrParaPrisaoPorTerceiraDupla = false;

    // Fluxo automático ao consumir carta de saída livre da prisão
    private boolean autoLancamentoAposSaidaPrisao = false;
    private boolean executandoAutoLancamento = false;

    /** DTO imutável para resultado dos dados. */
    public static final class ResultadoDados {
        private final int d1, d2;
        public ResultadoDados(int d1, int d2) { this.d1 = d1; this.d2 = d2; }
        public int getD1() { return d1; }
        public int getD2() { return d2; }
        public int getSoma() { return d1 + d2; }
        public boolean isDupla() { return d1 == d2; }
    }

    // =====================================================================================
    // SORTE/REVÉS — IMPLEMENTAÇÃO INTERNA (sem dependências externas)
    // =====================================================================================

    /** Cartinha simples apenas com número e rótulo (imagem opcional). */
    public static final class SorteRevesCard {
        private final int numero;
        private final String rotulo;       // ex.: "Sorte/Revés #05"
        private final String titulo;
        private final String descricao;
        private final String imagemArquivo; // opcional (pode ser null)
        public SorteRevesCard(int numero, String titulo, String descricao) {
            this.numero = numero;
            this.rotulo = String.format(Locale.ROOT, "Sorte/Revés #%02d", numero);
            this.titulo = titulo == null ? this.rotulo : titulo;
            this.descricao = descricao == null ? "" : descricao;
            this.imagemArquivo = null;
        }
        public int getNumero() { return numero; }
        public String getRotulo() { return rotulo; }
        public String getTitulo() { return titulo; }
        public String getDescricao() { return descricao; }
        public String getImagemArquivo() { return imagemArquivo; }
    }

    /** Tamanho do baralho e posição do próximo sorteio (circular). */
    private int tamanhoBaralhoSR = 30;
    private int ponteiroBaralhoSR = 0;

    /** Armazena as cartas (números) que cada jogador possui. */
    private final Map<Integer, Set<Integer>> cartasSRPorJogador = new HashMap<>();

    /** Última carta sorteada (para consultas) e buffer one-shot para a UI. */
    private Optional<SorteRevesCard> ultimaCartaSR = Optional.empty();
    private Optional<SorteRevesCard> srRecemSacada = Optional.empty();

    /** Configura um baralho padrão com N cartas genéricas numeradas 1..N. */
    public void configurarBaralhoSorteRevesPadrao(int totalCartas) {
        int limite = SorteRevesCards.total();
        if (totalCartas <= 0 || totalCartas > limite) {
            totalCartas = limite;
        }
        this.tamanhoBaralhoSR = totalCartas;
        this.ponteiroBaralhoSR = 0;
    }

    /** Última carta sorteada (consulta simples, não consome). */
    public Optional<SorteRevesCard> getUltimaCartaSorteReves() {
        return ultimaCartaSR;
    }

    /** Recupera metadados da carta informada (sempre via GameModel). */
    public Optional<SorteRevesCard> getCartaSorteRevesPorNumero(int numero) {
        if (numero < 1 || numero > SorteRevesCards.total()) {
            return Optional.empty();
        }
        SorteRevesCards.Definition def = SorteRevesCards.get(numero);
        if (def == null) {
            return Optional.empty();
        }
        return Optional.of(new SorteRevesCard(numero, def.getTitulo(), def.getDescricao()));
    }

    /** Banco de cartas do jogador (IDs das cartas em posse). */
    public Set<Integer> getCartasSorteRevesDoJogador(int jogadorId) {
        Set<Integer> cartas = cartasSRPorJogador.get(jogadorId);
        return cartas == null ? Collections.emptySet() : Collections.unmodifiableSet(cartas);
    }

    /** Consumido pela UI para mostrar a imagem/rotulo 1x só após o sorteio. */
    public Optional<SorteRevesCard> consumirSorteRevesRecemSacada() {
        Optional<SorteRevesCard> out = srRecemSacada;
        srRecemSacada = Optional.empty();
        return out;
    }

    /** Sorteia e entrega uma carta simples ao jogador. */
    private SorteRevesCard sortearCartaParaJogador(int jogadorId) {
        if (tamanhoBaralhoSR <= 0) {
            tamanhoBaralhoSR = SorteRevesCards.total();
        }
        int numero = (ponteiroBaralhoSR % tamanhoBaralhoSR) + 1;
        ponteiroBaralhoSR++;

        SorteRevesCards.Definition def = SorteRevesCards.get(numero);
        String titulo = def != null ? def.getTitulo() : String.format(Locale.ROOT, "Sorte/Revés #%02d", numero);
        String descricao = def != null ? def.getDescricao() : "";

        SorteRevesCard card = new SorteRevesCard(numero, titulo, descricao);
        Set<Integer> cartas = cartasSRPorJogador.get(jogadorId);
        if (cartas == null) {
            cartas = new HashSet<>();
            cartasSRPorJogador.put(jogadorId, cartas);
        }
        cartas.add(numero);
        ultimaCartaSR = Optional.of(card);
        srRecemSacada = Optional.of(card);
        aplicarEfeitoCartaSorteReves(jogadorId, numero, def);
        return card;
    }

    private void aplicarEfeitoCartaSorteReves(int jogadorId, int numero, SorteRevesCards.Definition def) {
        if (def == null) {
            return;
        }
        Jogador jogador = jogadores.get(jogadorId);
        if (jogador == null) {
            return;
        }

        boolean manterCarta = def.getEffectType() == SorteRevesCards.EffectType.SAIDA_LIVRE_DA_PRISAO;

        switch (def.getEffectType()) {
            case RECEBER_DO_BANCO:
                if (def.getValor() > 0) {
                    banco.debitar(def.getValor());
                    jogador.creditar(def.getValor());
                }
                break;
            case PAGAR_AO_BANCO:
                processarPagamentoAoBanco(jogador, def.getValor());
                break;
            case RECEBER_DE_CADA_JOGADOR:
                processarReceberDeCadaJogador(jogadorId, def.getValor());
                break;
            case SAIDA_LIVRE_DA_PRISAO:
                jogador.setCartaSaidaLivre(true);
                break;
            case IR_PARA_PRISAO:
                enviarParaPrisao(jogadorId);
                break;
            default:
                break;
        }

        if (!manterCarta) {
            removerCartaSorteRevesDoJogador(jogadorId, numero);
        }
    }

    private void processarPagamentoAoBanco(Jogador jogador, int valor) {
        if (jogador == null || valor <= 0) {
            return;
        }

        if (jogador.getSaldo() < valor) {
            tentarLevantarFundosPara(jogador, valor);
        }

        if (jogador.getSaldo() >= valor) {
            jogador.debitar(valor);
            banco.creditar(valor);
            return;
        }

        int disponivel = Math.max(0, jogador.getSaldo());
        if (disponivel > 0) {
            jogador.debitar(disponivel);
            banco.creditar(disponivel);
        }
        executarFalencia(jogador);
    }

    private void processarReceberDeCadaJogador(int favorecidoId, int valor) {
        if (valor <= 0) {
            return;
        }
        Jogador favorecido = jogadores.get(favorecidoId);
        if (favorecido == null) {
            return;
        }

        for (int i = 0; i < jogadores.size(); i++) {
            if (i == favorecidoId) {
                continue;
            }
            Jogador pagador = jogadores.get(i);
            if (pagador == null || !pagador.isAtivo()) {
                continue;
            }
            transferirEntreJogadores(pagador, favorecido, valor);
        }
    }

    private int transferirEntreJogadores(Jogador pagador, Jogador recebedor, int valor) {
        if (pagador == null || recebedor == null || valor <= 0) {
            return 0;
        }

        if (pagador.getSaldo() < valor) {
            tentarLevantarFundosPara(pagador, valor);
        }

        if (pagador.getSaldo() >= valor) {
            pagador.debitar(valor);
            recebedor.creditar(valor);
            return valor;
        }

        int disponivel = Math.max(0, pagador.getSaldo());
        if (disponivel > 0) {
            pagador.debitar(disponivel);
            recebedor.creditar(disponivel);
        }
        executarFalencia(pagador);
        return disponivel;
    }

    private void removerCartaSorteRevesDoJogador(int jogadorId, int numero) {
        Set<Integer> cartas = cartasSRPorJogador.get(jogadorId);
        if (cartas == null) {
            return;
        }
        cartas.remove(numero);
        if (cartas.isEmpty()) {
            cartasSRPorJogador.remove(jogadorId);
        }
    }

    /** Heurística para identificar casa do tipo Sorte/Revés (ajuste se necessário). */
    private boolean isCasaSorteReves(Casa c) {
        if (c == null) return false;
        String tipo = c.getTipo() == null ? "" : c.getTipo();
        String nome = c.getNome() == null ? "" : c.getNome();

        String T = normalizarCampoSorteReves(tipo);
        String N = normalizarCampoSorteReves(nome);

        if (T.contains("CHANCE") || N.contains("CHANCE")) return true;
        if ((T.contains("SORTE") && (T.contains("REVES") || T.contains("REVEZ"))) ||
            (N.contains("SORTE") && (N.contains("REVES") || N.contains("REVEZ")))) return true;
        if (tipo.contains("?") || nome.contains("?")) return true;

        return T.equals("SORTE_REVES") || T.equals("INTERROGACAO") || T.equals("SORTE_REVEZ");
    }

    private String normalizarCampoSorteReves(String valor) {
        String base = valor == null ? "" : valor;
        String n = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        n = n.replace('/', '_').replace('-', '_').replace(' ', '_');
        return n.toUpperCase(Locale.ROOT);
    }

    // =====================================================================================
    // SETUP
    // =====================================================================================

    public void novaPartida(int numJogadores, Long seedOpcional) {
        if (numJogadores < 2 || numJogadores > 6) {
            throw new IllegalArgumentException("Número de jogadores deve estar entre 2 e 6.");
        }
        this.rng = new RandomProvider(seedOpcional);
        this.turno = new Turno(numJogadores);
        this.banco = new Banco(200_000);
        this.partidaEncerrada = false;
        this.resultadoPartida = null;
        this.salvamentoDisponivel = true;

        this.jogadores.clear();
        cartasSRPorJogador.clear();

        for (int i = 0; i < numJogadores; i++) {
            this.jogadores.add(new Jogador(i, 4000));
            cartasSRPorJogador.put(i, new HashSet<>());
        }

        this.configurarBaralhoSorteRevesPadrao(30);

        this.ultimoD1 = this.ultimoD2 = null;
        this.jaLancouNesteTurno = false;
        this.deveIrParaPrisaoPorTerceiraDupla = false;
        this.autoLancamentoAposSaidaPrisao = false;
        this.executandoAutoLancamento = false;
        this.ultimaCartaSR = Optional.empty();
        this.srRecemSacada = Optional.empty();
        limparContextoDeQueda();
        liberarSalvamentoNoInicioDaVez();
        notifyObservers();
    }

    public void setTabuleiro(Tabuleiro tabuleiro) {
        if (tabuleiro == null || tabuleiro.tamanho() <= 0) {
            throw new IllegalArgumentException("Tabuleiro inválido.");
        }
        this.tabuleiro = tabuleiro;
    }

    private void exigirPartidaIniciada() {
        if (rng == null || turno == null || jogadores.isEmpty()) {
            throw new IllegalStateException("Partida não iniciada. Chame novaPartida().");
        }
    }

    private void exigirTabuleiroCarregado() {
        if (this.tabuleiro == null) {
            throw new IllegalStateException("Tabuleiro não carregado.");
        }
    }

    private void limparContextoDeQueda() {
        this.posicaoDaQuedaAtual = null;
        this.jaConstruiuNestaQueda = false;
        this.acabouDeComprarNestaQueda = false;
    }

    private void iniciarContextoDeQueda(int pos) {
        this.posicaoDaQuedaAtual = pos;
        this.jaConstruiuNestaQueda = false;
        this.acabouDeComprarNestaQueda = false;
    }

    private void bloquearSalvamentoDuranteTurno() {
        if (!partidaEncerrada) {
            salvamentoDisponivel = false;
        }
    }

    private void liberarSalvamentoNoInicioDaVez() {
        if (!partidaEncerrada) {
            salvamentoDisponivel = true;
        }
    }

    // =====================================================================================
    // REGRA #1 — LANÇAR DADOS (apenas 1x por turno; libera se DUPLA válida)
    // =====================================================================================

    public ResultadoDados lancarDados() {
        exigirPartidaIniciada();
        if (jaLancouNesteTurno) {
            throw new IllegalStateException("Neste turno você já rolou os dados.");
        }
        bloquearSalvamentoDuranteTurno();
        int d1 = dado1.rolar(rng);
        int d2 = dado2.rolar(rng);
        turno.registrarLance(d1, d2);
        this.ultimoD1 = d1;
        this.ultimoD2 = d2;
        this.jaLancouNesteTurno = true;

        if (turno.houveDupla() && turno.getDuplasConsecutivas() >= 3) {
            this.deveIrParaPrisaoPorTerceiraDupla = true;
        }
        notifyObservers();
        return new ResultadoDados(d1, d2);
    }

    /** Para testes – força os valores dos dados (1..6) sem random. */
    public ResultadoDados lancarDadosForcado(int d1, int d2) {
        exigirPartidaIniciada();
        if (jaLancouNesteTurno) {
            throw new IllegalStateException("Neste turno você já rolou os dados.");
        }
        if (d1 < 1 || d1 > 6 || d2 < 1 || d2 > 6) {
            throw new IllegalArgumentException("Valores dos dados devem estar entre 1 e 6.");
        }
        bloquearSalvamentoDuranteTurno();
        turno.registrarLance(d1, d2);
        this.ultimoD1 = d1;
        this.ultimoD2 = d2;
        this.jaLancouNesteTurno = true;

        if (turno.houveDupla() && turno.getDuplasConsecutivas() >= 3) {
            this.deveIrParaPrisaoPorTerceiraDupla = true;
        }
        notifyObservers();
        return new ResultadoDados(d1, d2);
    }

    public boolean houveDuplaNoUltimoLancamento() {
        exigirPartidaIniciada();
        // Se o jogador da vez está preso, não consideramos a rolagem como "dupla" para fins de UI
        // (isso permite encerrar a vez e o jogo não fica travado).
        boolean preso = jogadores.get(getJogadorDaVez()).isNaPrisao();
        return !preso && turno.houveDupla();
    }


    public int getContagemDuplasConsecutivasDaVez() {
        exigirPartidaIniciada();
        return turno.getDuplasConsecutivas();
    }

    public int getJogadorDaVez() {
        exigirPartidaIniciada();
        garantirJogadorDaVezAtivo();
        return turno.getJogadorDaVez();
    }

    /** Encerrar vez manualmente (UI chama este método). */
    public void encerrarVez() {
        exigirPartidaIniciada();
        passarVezPulandoFalidos();
        limparContextoDeQueda();
        this.jaLancouNesteTurno = false;
        this.ultimoD1 = null;
        this.ultimoD2 = null;
        liberarSalvamentoNoInicioDaVez();
        notifyObservers();
    }

    /** Mantido por compatibilidade com versões antigas da UI. */
    public void encerrarAcoesDaVezEPassarTurno() {
        encerrarVez();
    }

    private void passarVezPulandoFalidos() {
        if (jogadores.isEmpty()) {
            turno.passarVez();
            return;
        }
        int total = jogadores.size();
        for (int i = 0; i < total; i++) {
            turno.passarVez();
            Jogador candidato = jogadores.get(turno.getJogadorDaVez());
            if (candidato != null && candidato.isAtivo()) {
                return;
            }
        }
    }

    private void garantirJogadorDaVezAtivo() {
        if (jogadores.isEmpty()) {
            return;
        }
        int total = jogadores.size();
        for (int i = 0; i < total; i++) {
            Jogador candidato = jogadores.get(turno.getJogadorDaVez());
            if (candidato != null && candidato.isAtivo()) {
                return;
            }
            turno.passarVez();
        }
    }

    // =====================================================================================
    // REGRA #2 — DESLOCAMENTO (+ PRISÃO)
    // =====================================================================================

    public ResultadoMovimento deslocarPiao() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (ultimoD1 == null || ultimoD2 == null) {
            throw new IllegalStateException("É necessário lançar os dados antes de deslocar.");
        }

        final int id = getJogadorDaVez();
        final Jogador j = jogadores.get(id);

        // Terceira dupla: vai para a prisão imediatamente
        if (deveIrParaPrisaoPorTerceiraDupla) {
            int posAnt = j.getPosicao();
            enviarParaPrisao(id);
            deveIrParaPrisaoPorTerceiraDupla = false;
            turno.resetarDuplas();
            iniciarContextoDeQueda(j.getPosicao());
            notifyObservers();
            return new ResultadoMovimento(id, posAnt, 0, j.getPosicao(), false);
        }

        // Se está preso, tenta sair com dupla ou carta
        if (j.isNaPrisao()) {
            boolean saiu = tentarSairDaPrisaoComDuplaOuCarta();
            if (!saiu) {
                iniciarContextoDeQueda(j.getPosicao());
                notifyObservers();
                return new ResultadoMovimento(id, j.getPosicao(), 0, j.getPosicao(), false);
            }
        }

        // Movimento regular
        ResultadoMovimento r = Movimento.executar(j, ultimoD1, ultimoD2, tabuleiro);

        if (r.passouOuCaiuNoInicio) {
            banco.pagarHonorarios(j);
            notifyObservers();
        }

        iniciarContextoDeQueda(j.getPosicao());

        // Casa "Vá para a Prisão"
        Casa casaAtual = tabuleiro.getCasa(j.getPosicao());
        if ("VA_PARA_PRISAO".equalsIgnoreCase(casaAtual.getTipo())) {
            int posAnt = j.getPosicao();
            enviarParaPrisao(id);
            iniciarContextoDeQueda(j.getPosicao());
            notifyObservers();
            return new ResultadoMovimento(id, posAnt, 0, j.getPosicao(), false);
        }

        // SORTE/REVÉS: sorteia ao cair
        if (isCasaSorteReves(casaAtual)) {
            sortearCartaParaJogador(id); // preenche ultimaCartaSR + srRecemSacada e adiciona ao "banco"
            notifyObservers();
        }

        // Se houve DUPLA e o jogador não está preso, libera nova rolagem neste turno
        if (turno.houveDupla() && !jogadores.get(getJogadorDaVez()).isNaPrisao()) {
            this.jaLancouNesteTurno = false;
        }

        notifyObservers();
        return r;
    }

    // =====================================================================================
    // REGRA #3 — COMPRAR PROPRIEDADE
    // =====================================================================================

    /** Habilita/desabilita o botão "Comprar" (UI) */
    public boolean canComprarPropriedadeNaCasaAtual() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        final Jogador j = jogadores.get(getJogadorDaVez());
        final Casa casa = tabuleiro.getCasa(j.getPosicao());
        final AtivoCompravel ativo = asAtivoCompravel(casa);
        if (ativo == null) return false;

        if (ativo.temDono()) return false;
        final int preco = ativo.getPrecoCompra();
        return preco > 0 && j.getSaldo() >= preco;
    }

    /** Evita abrir carta quando casa tem dono (e não é o jogador atual). */
    public boolean isCasaAtualPropriedadeComDonoDeOutro() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        final Jogador j = jogadores.get(getJogadorDaVez());
        final Casa casa = tabuleiro.getCasa(j.getPosicao());
        final AtivoCompravel ativo = asAtivoCompravel(casa);
        if (ativo == null) return false;

        return ativo.temDono() && ativo.getDono() != j;
    }

    public boolean comprarPropriedade() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        final Jogador jogador = jogadores.get(getJogadorDaVez());
        final Casa casaAtual = tabuleiro.getCasa(jogador.getPosicao());

        final AtivoCompravel ativo = asAtivoCompravel(casaAtual);
        if (ativo == null)
            return false;
        if (ativo.temDono())
            return false;

        final int preco = ativo.getPrecoCompra();
        if (preco <= 0 || jogador.getSaldo() < preco)
            return false;

        jogador.debitar(preco);
        banco.creditar(preco);
        ativo.setDono(jogador);

        this.acabouDeComprarNestaQueda = true;
        notifyObservers();
        return true;
    }

    // =====================================================================================
    // REGRA #4 — CONSTRUÇÕES (CASA / HOTEL)
    // =====================================================================================

    public boolean canConstruirCasaNaCasaAtual() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (posicaoDaQuedaAtual == null) return false;

        final Jogador jogador = jogadores.get(getJogadorDaVez());
        if (jogador.getPosicao() != posicaoDaQuedaAtual) return false;

        final Casa casa = tabuleiro.getCasa(jogador.getPosicao());
        if (!(casa instanceof Propriedade)) return false;

        final Propriedade prop = (Propriedade) casa;
        if (!prop.temDono() || prop.getDono() != jogador) return false;
        if (acabouDeComprarNestaQueda) return false;   // não na mesma queda da compra
        if (jaConstruiuNestaQueda) return false;       // 1 construção por queda
        if (!prop.podeConstruirCasa()) return false;

        final int preco = prop.getPrecoCasa();
        return preco > 0 && jogador.getSaldo() >= preco;
    }

    public boolean canConstruirHotelNaCasaAtual() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (posicaoDaQuedaAtual == null) return false;

        final Jogador jogador = jogadores.get(getJogadorDaVez());
        if (jogador.getPosicao() != posicaoDaQuedaAtual) return false;

        final Casa casa = tabuleiro.getCasa(jogador.getPosicao());
        if (!(casa instanceof Propriedade)) return false;

        final Propriedade prop = (Propriedade) casa;
        if (!prop.temDono() || prop.getDono() != jogador) return false;
        if (acabouDeComprarNestaQueda) return false;
        if (jaConstruiuNestaQueda) return false;
        if (!prop.podeConstruirHotel()) return false;

        final int preco = prop.getPrecoHotel();
        return preco > 0 && jogador.getSaldo() >= preco;
    }

    public boolean construirCasa() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (!canConstruirCasaNaCasaAtual()) return false;

        final Jogador jogador = jogadores.get(getJogadorDaVez());
        final Propriedade prop = (Propriedade) tabuleiro.getCasa(jogador.getPosicao());

        jogador.debitar(prop.getPrecoCasa());
        banco.creditar(prop.getPrecoCasa());
        prop.construirCasa();
        jaConstruiuNestaQueda = true;
        notifyObservers();
        return true;
    }

    public boolean construirHotel() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (!canConstruirHotelNaCasaAtual()) return false;

        final Jogador jogador = jogadores.get(getJogadorDaVez());
        final Propriedade prop = (Propriedade) tabuleiro.getCasa(jogador.getPosicao());

        jogador.debitar(prop.getPrecoHotel());
        banco.creditar(prop.getPrecoHotel());
        prop.construirHotel();
        jaConstruiuNestaQueda = true;
        notifyObservers();
        return true;
    }

    // =====================================================================================
    // REGRA #5 — ALUGUEL
    // =====================================================================================

    public Transacao pagarAluguelSeDevido() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        final int idPagador = getJogadorDaVez();
        final Jogador pagador = jogadores.get(idPagador);
        final Casa casa = tabuleiro.getCasa(pagador.getPosicao());
        final AtivoCompravel ativo = asAtivoCompravel(casa);

        if (ativo == null) {
            return Transacao.semEfeito("Casa atual não é propriedade/companhia", idPagador, pagador.getPosicao(), null, 0);
        }

        if (!ativo.temDono()) {
            return Transacao.semEfeito("Posse sem dono", idPagador, pagador.getPosicao(), null, 0);
        }

        final Jogador dono = ativo.getDono();
        if (dono == pagador) {
            return Transacao.semEfeito("Posse do próprio jogador", idPagador, pagador.getPosicao(), dono.getId(), 0);
        }

        final int aluguel = ativo.calcularAluguel();
        if (aluguel <= 0) {
            return Transacao.semEfeito("Aluguel calculado como zero", idPagador, pagador.getPosicao(), dono.getId(), 0);
        }

        // tentar levantar fundos
        if (pagador.getSaldo() < aluguel) {
            tentarLevantarFundosPara(pagador, aluguel);
        }

        if (pagador.getSaldo() >= aluguel) {
            pagador.debitar(aluguel);
            dono.creditar(aluguel);
            notifyObservers(); // atualiza UI
            return Transacao.aluguelEfetuado(idPagador, dono.getId(), ativo.getPosicao(), aluguel);
        }

        // pagamento parcial + falência
        int disponivel = Math.max(0, pagador.getSaldo());
        if (disponivel > 0) {
            pagador.debitar(disponivel);
            dono.creditar(disponivel);
        }
        executarFalencia(pagador);
        notifyObservers(); // atualiza UI
        return Transacao.aluguelEfetuado(idPagador, dono.getId(), ativo.getPosicao(), disponivel);
    }

    public Transacao aplicarEfeitosObrigatoriosPosMovimento() {
        // IMPOSTO/LUCRO primeiro
        Transacao t = aplicarCasaEspecialSeDevida();
        if (t != null && !"SEM_EFEITO".equals(t.getTipo())) {
            return t;
        }
        // depois aluguel
        return pagarAluguelSeDevido();
    }

    public Transacao deslocarPiaoEAplicarObrigatorios() {
        deslocarPiao();
        Transacao resultado = aplicarEfeitosObrigatoriosPosMovimento();
        processarLancamentosAutomaticosSeNecessario();
        return resultado;
    }

    // =====================================================================================
    // REGRA #6 — PRISÃO
    // =====================================================================================

    public void enviarParaPrisao(int idJogador) {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (idJogador < 0 || idJogador >= jogadores.size()) {
            throw new IllegalArgumentException("idJogador inválido");
        }
        final int idxPrisao = getIndicePrisaoOrThrow();
        final Jogador j = jogadores.get(idJogador);
        j.moverPara(idxPrisao);
        if (j.temCartaSaidaLivre()) {
            ativarSaidaAutomaticaDaPrisao(j);
        } else {
            j.setNaPrisao(true);
        }
        turno.resetarDuplas();
    }

    private void ativarSaidaAutomaticaDaPrisao(Jogador jogador) {
        if (jogador == null) {
            return;
        }
        jogador.setCartaSaidaLivre(false);
        removerCartaSorteRevesDoJogador(jogador.getId(), 9);
        jogador.setNaPrisao(false);
        this.jaLancouNesteTurno = false;
        this.ultimoD1 = null;
        this.ultimoD2 = null;
        this.autoLancamentoAposSaidaPrisao = true;
    }

    private void processarLancamentosAutomaticosSeNecessario() {
        if (executandoAutoLancamento) {
            return;
        }
        while (autoLancamentoAposSaidaPrisao) {
            autoLancamentoAposSaidaPrisao = false;
            executandoAutoLancamento = true;
            try {
                this.jaLancouNesteTurno = false;
                this.ultimoD1 = null;
                this.ultimoD2 = null;
                lancarDados();
                deslocarPiaoEAplicarObrigatorios();
            } finally {
                executandoAutoLancamento = false;
            }
        }
    }

    public boolean tentarSairDaPrisaoComDuplaOuCarta() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        final Jogador j = jogadores.get(getJogadorDaVez());
        if (!j.isNaPrisao())
            return false;

        if (j.temCartaSaidaLivre()) {
            j.setCartaSaidaLivre(false);
            removerCartaSorteRevesDoJogador(j.getId(), 9);
            j.setNaPrisao(false);
            turno.resetarDuplas();
            return true;
        }
        if (turno.houveDupla()) {
            j.setNaPrisao(false);
            turno.resetarDuplas();
            return true;
        }
        return false;
    }

    public boolean estaNaPrisao(int idJogador) {
        exigirPartidaIniciada();
        if (idJogador < 0 || idJogador >= jogadores.size()) {
            throw new IllegalArgumentException("idJogador inválido");
        }
        return jogadores.get(idJogador).isNaPrisao();
    }

    // =====================================================================================
    // REGRA #7 — LIQUIDAÇÃO E FALÊNCIA
    // =====================================================================================

    public boolean venderPropriedadeAoBanco(int posicaoPropriedade) {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        final Jogador j = jogadores.get(getJogadorDaVez());
        final Casa c = tabuleiro.getCasa(posicaoPropriedade);
        if (!(c instanceof AtivoCompravel))
            return false;

        final AtivoCompravel ativo = (AtivoCompravel) c;
        if (!ativo.temDono() || ativo.getDono() != j)
            return false;

        final int valorAgregado = Math.max(0, ativo.valorAgregadoAtual());
        final int pagamento = (valorAgregado * 9) / 10; // 90%

        banco.debitar(pagamento); // banco paga
        j.creditar(pagamento);
        ativo.resetarParaBanco();
        notifyObservers();
        return true;
    }

    public boolean declararFalenciaSeNecessario() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        final Jogador j = jogadores.get(getJogadorDaVez());
        if (!j.isAtivo())
            return true;
        if (j.getSaldo() >= 0)
            return false;

        // Liquida do maior valor agregado para o menor
        List<AtivoCompravel> minhas = listarAtivosDo(j);
        minhas.sort(Comparator.comparingInt(AtivoCompravel::valorAgregadoAtual).reversed());

        for (AtivoCompravel ativo : minhas) {
            if (j.getSaldo() >= 0)
                break;
            final int pagamento = (Math.max(0, ativo.valorAgregadoAtual()) * 9) / 10;
            banco.debitar(pagamento);
            j.creditar(pagamento);
            ativo.resetarParaBanco();
        }

        if (j.getSaldo() < 0) {
            executarFalencia(j);
            notifyObservers();
            return true;
        }
        notifyObservers();
        return false;
    }

    private List<AtivoCompravel> listarAtivosDo(Jogador dono) {
        List<AtivoCompravel> ativos = new ArrayList<>();
        for (int i = 0; i < tabuleiro.tamanho(); i++) {
            Casa c = tabuleiro.getCasa(i);
            if (c instanceof AtivoCompravel) {
                AtivoCompravel ativo = (AtivoCompravel) c;
                if (ativo.temDono() && ativo.getDono() == dono) {
                    ativos.add(ativo);
                }
            }
        }
        return ativos;
    }

    private AtivoCompravel asAtivoCompravel(Casa casa) {
        return (casa instanceof AtivoCompravel) ? (AtivoCompravel) casa : null;
    }

    // =====================================================================================
    // CONSULTAS PARA UI
    // =====================================================================================

    public int getPosicaoJogador(int idJogador) {
        exigirPartidaIniciada();
        return jogadores.get(idJogador).getPosicao();
    }

    public int getQuantidadeCasasTabuleiro() {
        exigirTabuleiroCarregado();
        return tabuleiro.tamanho();
    }

    public int getSaldoJogador(int idJogador) {
        exigirPartidaIniciada();
        return jogadores.get(idJogador).getSaldo();
    }

    public int getSaldoBanco() {
        return banco.getSaldo();
    }

    public boolean temPartidaConfigurada() {
        return rng != null && turno != null && !jogadores.isEmpty();
    }

    public boolean temPartidaAtiva() {
        return temPartidaConfigurada() && !partidaEncerrada;
    }

    public boolean isSalvamentoDisponivel() {
        return temPartidaAtiva() && salvamentoDisponivel;
    }

    public boolean isPartidaEncerrada() {
        return partidaEncerrada;
    }

    public Optional<ResultadoPartida> getResultadoPartida() {
        return Optional.ofNullable(resultadoPartida);
    }

    public boolean isJogadorAtivo(int idJogador) {
        exigirPartidaIniciada();
        if (idJogador < 0 || idJogador >= jogadores.size()) {
            throw new IllegalArgumentException("idJogador inválido");
        }
        return jogadores.get(idJogador).isAtivo();
    }

    // Sinais para a UI (TabuleiroFrame)
    public boolean podeLancarDadosNesteTurno() { return !jaLancouNesteTurno; }
    public boolean jaLancouNesteTurno() { return jaLancouNesteTurno; }
    public boolean isJogadorDaVezNaPrisao() {
        exigirPartidaIniciada();
        return jogadores.get(getJogadorDaVez()).isNaPrisao();
    }

    public boolean jogadorDaVezTemCartaSaidaLivre() {
        exigirPartidaIniciada();
        return jogadores.get(getJogadorDaVez()).temCartaSaidaLivre();
    }

    /** Atalho direto para habilitar o botão "Exibir/Abrir banco de cartas" na UI. */
    public boolean podeAbrirBancoDeCartasJogadorDaVez() {
        return jogadorPossuiAlgumaCartaOuPropriedade(getJogadorDaVez());
    }

    // =============================== Banco de cartas (para UI) ===============================

    public static final class BancoDeCartasItem {
        public static enum Tipo { TERRITORIO, COMPANHIA, SORTE_REVES }
        private final Tipo tipo;
        private final String nome;              // ex.: "Av. Paulista" ou "Sorte/Revés #05"
        private final Integer numeroSorteReves; // se SORTE_REVES
        private final Integer posicaoTabuleiro; // se TERRITORIO
        public BancoDeCartasItem(Tipo tipo, String nome, Integer numeroSorteReves, Integer posicaoTabuleiro) {
            this.tipo = tipo;
            this.nome = nome;
            this.numeroSorteReves = numeroSorteReves;
            this.posicaoTabuleiro = posicaoTabuleiro;
        }
        public Tipo getTipo() { return tipo; }
        public String getNome() { return nome; }
        public Integer getNumeroSorteReves() { return numeroSorteReves; }
        public Integer getPosicaoTabuleiro() { return posicaoTabuleiro; }
    }

    /** Retorna os nomes das propriedades do jogador (para habilitação da UI). */
    public List<String> getNomesPropriedadesDoJogador(int idJogador) {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (idJogador < 0 || idJogador >= jogadores.size()) {
            throw new IllegalArgumentException("idJogador inválido");
        }
        Jogador dono = jogadores.get(idJogador);
        List<String> nomes = new ArrayList<>();
        for (AtivoCompravel ativo : listarAtivosDo(dono)) {
            Casa casa = tabuleiro.getCasa(ativo.getPosicao());
            if (casa != null) {
                nomes.add(casa.getNome());
            }
        }
        return nomes;
    }

    /** True se o jogador possui ao menos uma propriedade ou alguma carta de Sorte/Revés. */
    public boolean jogadorPossuiAlgumaCartaOuPropriedade(int idJogador) {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (idJogador < 0 || idJogador >= jogadores.size()) {
            throw new IllegalArgumentException("idJogador inválido");
        }
        boolean temProp = !getNomesPropriedadesDoJogador(idJogador).isEmpty();
        boolean temSR   = !getCartasSorteRevesDoJogador(idJogador).isEmpty();
        return temProp || temSR;
    }

    /** Lista consolidada para a janela do “banco de cartas”. */
    public List<BancoDeCartasItem> getBancoDeCartasDoJogador(int idJogador) {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (idJogador < 0 || idJogador >= jogadores.size()) {
            throw new IllegalArgumentException("idJogador inválido");
        }
        Jogador dono = jogadores.get(idJogador);
        List<BancoDeCartasItem> items = new ArrayList<>();

        // Territórios e companhias do jogador
        for (int i = 0; i < tabuleiro.tamanho(); i++) {
            Casa c = tabuleiro.getCasa(i);
            if (c instanceof AtivoCompravel) {
                AtivoCompravel ativo = (AtivoCompravel) c;
                if (ativo.temDono() && ativo.getDono() == dono) {
                    BancoDeCartasItem.Tipo tipo =
                            (c instanceof Companhia)
                                    ? BancoDeCartasItem.Tipo.COMPANHIA
                                    : BancoDeCartasItem.Tipo.TERRITORIO;
                    items.add(new BancoDeCartasItem(
                            tipo,
                            c.getNome(),
                            null,
                            ativo.getPosicao()
                    ));
                }
            }
        }

        // Cartas Sorte/Revés
        for (Integer num : getCartasSorteRevesDoJogador(idJogador)) {
            items.add(new BancoDeCartasItem(
                    BancoDeCartasItem.Tipo.SORTE_REVES,
                    String.format(Locale.ROOT, "Sorte/Revés #%02d", num),
                    num,
                    null
            ));
        }

        return items;
    }

    /** Nome do território onde o jogador da vez está, se aplicável. */
    public Optional<String> getNomeDoTerritorioDaCasaAtualDoJogadorDaVez() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        final Jogador j = jogadores.get(getJogadorDaVez());
        final Casa c = tabuleiro.getCasa(j.getPosicao());
        if (c instanceof AtivoCompravel) {
            return Optional.of(c.getNome());
        }
        return Optional.empty();
    }

    // =====================================================================================
    // HELPERS DE TABULEIRO (para testes)
    // =====================================================================================

    public void carregarTabuleiroMinimoParaTeste(int nCasas) {
        if (nCasas <= 0)
            throw new IllegalArgumentException("nCasas deve ser > 0");
        List<Casa> casas = new ArrayList<>(nCasas);
        for (int i = 0; i < nCasas; i++) {
            casas.add(new Casa(i, "Casa " + i, "GENERICA"));
        }
        this.setTabuleiro(new Tabuleiro(casas));
        notifyObservers();
    }

    public void carregarTabuleiroComImpostoELucro(int nCasas, int idxImposto, int valorImposto, int idxLucro, int valorLucro) {
        if (nCasas <= 0)
            throw new IllegalArgumentException("nCasas deve ser > 0");
        List<Casa> casas = new ArrayList<>(nCasas);
        for (int i = 0; i < nCasas; i++) {
            if (i == idxImposto)
                casas.add(new Casa(i, "Imposto", "IMPOSTO", valorImposto));
            else if (i == idxLucro)
                casas.add(new Casa(i, "Lucro", "LUCRO", valorLucro));
            else
                casas.add(new Casa(i, "Casa " + i, "GENERICA"));
        }
        this.setTabuleiro(new Tabuleiro(casas));
        notifyObservers();
    }

    public void carregarTabuleiroDeTesteComUmaPropriedade(int nCasas, int idxPropriedade, int precoTerreno) {
        carregarTabuleiroDeTesteComUmaPropriedade(nCasas, idxPropriedade, precoTerreno, 0, 0);
    }

    public void carregarTabuleiroDeTesteComUmaPropriedade(int nCasas, int idxPropriedade, int precoTerreno, int precoCasa) {
        carregarTabuleiroDeTesteComUmaPropriedade(nCasas, idxPropriedade, precoTerreno, precoCasa, 0);
    }

    public void carregarTabuleiroDeTesteComUmaPropriedade(int nCasas, int idxPropriedade, int precoTerreno, int precoCasa, int precoHotel) {
        if (nCasas <= 0)
            throw new IllegalArgumentException("nCasas deve ser > 0");
        if (idxPropriedade < 0 || idxPropriedade >= nCasas) {
            throw new IllegalArgumentException("Índice de propriedade fora do tabuleiro.");
        }
        List<Casa> casas = new ArrayList<>(nCasas);
        for (int i = 0; i < nCasas; i++) {
            if (i == idxPropriedade) {
                casas.add(new Propriedade(i, "Propriedade " + i, precoTerreno, precoCasa, precoHotel, new int[0]));
            } else {
                casas.add(new Casa(i, "Casa " + i, "GENERICA"));
            }
        }
        this.setTabuleiro(new Tabuleiro(casas));
        notifyObservers();
    }

    public void carregarTabuleiroDeTesteComUmaCompanhia(int nCasas, int idxCompanhia, int precoCompra, int aluguelFixo) {
        if (nCasas <= 0)
            throw new IllegalArgumentException("nCasas deve ser > 0");
        if (idxCompanhia < 0 || idxCompanhia >= nCasas) {
            throw new IllegalArgumentException("Índice de companhia fora do tabuleiro.");
        }
        List<Casa> casas = new ArrayList<>(nCasas);
        for (int i = 0; i < nCasas; i++) {
            if (i == idxCompanhia) {
                casas.add(new Companhia(i, "Companhia " + i, precoCompra, aluguelFixo));
            } else {
                casas.add(new Casa(i, "Casa " + i, "GENERICA"));
            }
        }
        this.setTabuleiro(new Tabuleiro(casas));
        notifyObservers();
    }

    public void carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(int nCasas, int idxPropriedade, int precoTerreno,
                                                                   int precoCasa, int precoHotel, int[] alugueis) {
        if (nCasas <= 0)
            throw new IllegalArgumentException("nCasas deve ser > 0");
        if (idxPropriedade < 0 || idxPropriedade >= nCasas) {
            throw new IllegalArgumentException("Índice de propriedade fora do tabuleiro.");
        }
        List<Casa> casas = new ArrayList<>(nCasas);
        for (int i = 0; i < nCasas; i++) {
            if (i == idxPropriedade) {
                casas.add(new Propriedade(i, "Propriedade " + i, precoTerreno, precoCasa, precoHotel, alugueis));
            } else {
                casas.add(new Casa(i, "Casa " + i, "GENERICA"));
            }
        }
        this.setTabuleiro(new Tabuleiro(casas));
        notifyObservers();
    }

    public void carregarTabuleiroBasicoComPrisao(int nCasas, int idxPrisao, int idxVaParaPrisao) {
        if (nCasas <= 0)
            throw new IllegalArgumentException("nCasas deve ser > 0");
        if (idxPrisao < 0 || idxPrisao >= nCasas)
            throw new IllegalArgumentException("idxPrisao fora do tabuleiro.");
        if (idxVaParaPrisao < 0 || idxVaParaPrisao >= nCasas)
            throw new IllegalArgumentException("idxVaParaPrisao fora do tabuleiro.");
        List<Casa> casas = new ArrayList<>(nCasas);
        for (int i = 0; i < nCasas; i++) {
            if (i == idxPrisao) {
                casas.add(new Casa(i, "Prisão", "PRISAO"));
            } else if (i == idxVaParaPrisao) {
                casas.add(new Casa(i, "Vá para a Prisão", "VA_PARA_PRISAO"));
            } else {
                casas.add(new Casa(i, "Casa " + i, "GENERICA"));
            }
        }
        this.setTabuleiro(new Tabuleiro(casas));
        notifyObservers();
    }

    public void debugForcarProximaCartaSorteReves(int numero) {
        if (numero < 1)
            throw new IllegalArgumentException("numero deve ser >= 1");
        if (tamanhoBaralhoSR <= 0) {
            tamanhoBaralhoSR = SorteRevesCards.total();
        }
        if (numero > tamanhoBaralhoSR) {
            throw new IllegalArgumentException("numero fora do baralho configurado");
        }
        this.ponteiroBaralhoSR = numero - 1;
    }

    public void debugForcarPosicaoJogador(int idJogador, int posicao) {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (idJogador < 0 || idJogador >= jogadores.size()) {
            throw new IllegalArgumentException("idJogador inválido");
        }
        if (posicao < 0 || posicao >= tabuleiro.tamanho()) {
            throw new IllegalArgumentException("posicao fora do tabuleiro");
        }
        jogadores.get(idJogador).moverPara(posicao);
        this.posicaoDaQuedaAtual = posicao; // simula que 'caiu' aqui
        notifyObservers();
    }

    public void debugForcarDonoECasasDaPropriedade(int posicaoPropriedade, int idDono, int numCasas, boolean hotel) {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        Casa c = tabuleiro.getCasa(posicaoPropriedade);
        if (!(c instanceof Propriedade)) {
            throw new IllegalArgumentException("Posição não é Propriedade");
        }
        Propriedade p = (Propriedade) c;

        if (idDono < 0 || idDono >= jogadores.size()) {
            throw new IllegalArgumentException("idDono inválido");
        }
        p.setDono(jogadores.get(idDono));

        while (p.getNumCasas() < numCasas) {
            if (!p.podeConstruirCasa())
                break;
            p.construirCasa();
        }
        if (hotel && p.podeConstruirHotel()) {
            p.construirHotel();
        }
        notifyObservers();
    }

    public void debugForcarDonoDaCompanhia(int posicaoCompanhia, int idDono) {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        Casa c = tabuleiro.getCasa(posicaoCompanhia);
        if (!(c instanceof Companhia)) {
            throw new IllegalArgumentException("Posição não é Companhia");
        }
        if (idDono < 0 || idDono >= jogadores.size()) {
            throw new IllegalArgumentException("idDono inválido");
        }
        ((Companhia) c).setDono(jogadores.get(idDono));
        notifyObservers();
    }

    public void debugDarCartaSaidaLivreAoJogador(int idJogador, boolean possui) {
        exigirPartidaIniciada();
        if (idJogador < 0 || idJogador >= jogadores.size()) {
            throw new IllegalArgumentException("idJogador inválido");
        }
        jogadores.get(idJogador).setCartaSaidaLivre(possui);
        notifyObservers();
    }

    // =====================================================================================
    // TÉRMINO DA PARTIDA
    // =====================================================================================

    public static enum FimPartidaMotivo {
        JANELA_FECHADA,
        BOTAO_ENCERRAR,
        ULTIMO_JOGADOR_RESTANTE
    }

    public static final class ResumoCapital {
        private final int jogadorId;
        private final int saldoDisponivel;
        private final int patrimonio;
        private final int capitalTotal;
        private final boolean ativo;

        ResumoCapital(int jogadorId, int saldoDisponivel, int patrimonio, boolean ativo) {
            this.jogadorId = jogadorId;
            this.saldoDisponivel = saldoDisponivel;
            this.patrimonio = patrimonio;
            this.capitalTotal = saldoDisponivel + patrimonio;
            this.ativo = ativo;
        }

        public int getJogadorId() { return jogadorId; }
        public int getSaldoDisponivel() { return saldoDisponivel; }
        public int getPatrimonio() { return patrimonio; }
        public int getCapitalTotal() { return capitalTotal; }
        public boolean isAtivo() { return ativo; }
    }

    public static final class ResultadoPartida {
        private final FimPartidaMotivo motivo;
        private final List<ResumoCapital> ranking;
        private final int vencedorId;

        ResultadoPartida(FimPartidaMotivo motivo, List<ResumoCapital> ranking) {
            this.motivo = motivo;
            this.ranking = Collections.unmodifiableList(new ArrayList<>(ranking));
            this.vencedorId = ranking.isEmpty() ? -1 : ranking.get(0).getJogadorId();
        }

        public FimPartidaMotivo getMotivo() { return motivo; }
        public List<ResumoCapital> getRanking() { return ranking; }
        public int getVencedorId() { return vencedorId; }
    }

    public void encerrarPartida(FimPartidaMotivo motivo) {
        if (!temPartidaConfigurada() || partidaEncerrada) {
            return;
        }
        FimPartidaMotivo efetivo = (motivo == null) ? FimPartidaMotivo.BOTAO_ENCERRAR : motivo;
        List<ResumoCapital> ranking = calcularResumoCapital();
        this.resultadoPartida = new ResultadoPartida(efetivo, ranking);
        this.partidaEncerrada = true;
        this.salvamentoDisponivel = false;
        notifyObservers();
    }

    private List<ResumoCapital> calcularResumoCapital() {
        List<ResumoCapital> lista = new ArrayList<>();
        for (Jogador j : jogadores) {
            int patrimonio = 0;
            for (AtivoCompravel ativo : listarAtivosDo(j)) {
                patrimonio += Math.max(0, ativo.valorAgregadoAtual());
            }
            lista.add(new ResumoCapital(j.getId(), j.getSaldo(), patrimonio, j.isAtivo()));
        }
        lista.sort(new java.util.Comparator<ResumoCapital>() {
            @Override
            public int compare(ResumoCapital a, ResumoCapital b) {
                int cmp = Integer.compare(b.getCapitalTotal(), a.getCapitalTotal());
                if (cmp != 0) {
                    return cmp;
                }
                cmp = Integer.compare(b.getSaldoDisponivel(), a.getSaldoDisponivel());
                if (cmp != 0) {
                    return cmp;
                }
                return Integer.compare(a.getJogadorId(), b.getJogadorId());
            }
        });
        return lista;
    }

    // =====================================================================================
    // SALVAMENTO E RECUPERAÇÃO DE ESTADO
    // =====================================================================================

    public static final class PlayerState {
        private final int id;
        private final int saldo;
        private final int posicao;
        private final boolean ativo;
        private final boolean naPrisao;
        private final boolean cartaSaidaLivre;

        public PlayerState(int id, int saldo, int posicao, boolean ativo, boolean naPrisao, boolean cartaSaidaLivre) {
            this.id = id;
            this.saldo = saldo;
            this.posicao = posicao;
            this.ativo = ativo;
            this.naPrisao = naPrisao;
            this.cartaSaidaLivre = cartaSaidaLivre;
        }

        public int getId() { return id; }
        public int getSaldo() { return saldo; }
        public int getPosicao() { return posicao; }
        public boolean isAtivo() { return ativo; }
        public boolean isNaPrisao() { return naPrisao; }
        public boolean hasCartaSaidaLivre() { return cartaSaidaLivre; }
    }

    public static final class PropertyState {
        private final int posicao;
        private final int donoId;
        private final int numCasas;
        private final boolean hotel;

        public PropertyState(int posicao, int donoId, int numCasas, boolean hotel) {
            this.posicao = posicao;
            this.donoId = donoId;
            this.numCasas = numCasas;
            this.hotel = hotel;
        }

        public int getPosicao() { return posicao; }
        public int getDonoId() { return donoId; }
        public int getNumCasas() { return numCasas; }
        public boolean hasHotel() { return hotel; }
    }

    public static final class SaveState {
        private final List<PlayerState> jogadores;
        private final List<PropertyState> propriedades;
        private final List<Integer> ordemTurno;
        private final int turnoIndex;
        private final int turnoDuplasConsecutivas;
        private final int turnoUltimoD1;
        private final int turnoUltimoD2;
        private final Integer ultimoD1;
        private final Integer ultimoD2;
        private final boolean jaLancouNesteTurno;
        private final boolean deveIrParaPrisaoPorTerceiraDupla;
        private final Integer posicaoDaQuedaAtual;
        private final boolean jaConstruiuNestaQueda;
        private final boolean acabouDeComprarNestaQueda;
        private final boolean salvamentoDisponivel;
        private final int bancoSaldo;
        private final int tamanhoBaralhoSR;
        private final int ponteiroBaralhoSR;
        private final Map<Integer, Set<Integer>> cartasSRPorJogador;
        private final Integer ultimaCartaNumero;
        private final Integer cartaBufferNumero;

        public SaveState(List<PlayerState> jogadores,
                         List<PropertyState> propriedades,
                         List<Integer> ordemTurno,
                         int turnoIndex,
                         int turnoDuplasConsecutivas,
                         int turnoUltimoD1,
                         int turnoUltimoD2,
                         Integer ultimoD1,
                         Integer ultimoD2,
                         boolean jaLancouNesteTurno,
                         boolean deveIrParaPrisaoPorTerceiraDupla,
                         Integer posicaoDaQuedaAtual,
                         boolean jaConstruiuNestaQueda,
                         boolean acabouDeComprarNestaQueda,
                         boolean salvamentoDisponivel,
                         int bancoSaldo,
                         int tamanhoBaralhoSR,
                         int ponteiroBaralhoSR,
                         Map<Integer, Set<Integer>> cartasSRPorJogador,
                         Integer ultimaCartaNumero,
                         Integer cartaBufferNumero) {
            this.jogadores = Collections.unmodifiableList(new ArrayList<>(jogadores));
            this.propriedades = Collections.unmodifiableList(new ArrayList<>(propriedades));
            this.ordemTurno = Collections.unmodifiableList(new ArrayList<>(ordemTurno));
            this.turnoIndex = turnoIndex;
            this.turnoDuplasConsecutivas = turnoDuplasConsecutivas;
            this.turnoUltimoD1 = turnoUltimoD1;
            this.turnoUltimoD2 = turnoUltimoD2;
            this.ultimoD1 = ultimoD1;
            this.ultimoD2 = ultimoD2;
            this.jaLancouNesteTurno = jaLancouNesteTurno;
            this.deveIrParaPrisaoPorTerceiraDupla = deveIrParaPrisaoPorTerceiraDupla;
            this.posicaoDaQuedaAtual = posicaoDaQuedaAtual;
            this.jaConstruiuNestaQueda = jaConstruiuNestaQueda;
            this.acabouDeComprarNestaQueda = acabouDeComprarNestaQueda;
            this.salvamentoDisponivel = salvamentoDisponivel;
            this.bancoSaldo = bancoSaldo;
            this.tamanhoBaralhoSR = tamanhoBaralhoSR;
            this.ponteiroBaralhoSR = ponteiroBaralhoSR;
            Map<Integer, Set<Integer>> mapa = new HashMap<>();
            for (Map.Entry<Integer, Set<Integer>> e : cartasSRPorJogador.entrySet()) {
                mapa.put(e.getKey(), Collections.unmodifiableSet(new HashSet<>(e.getValue())));
            }
            this.cartasSRPorJogador = Collections.unmodifiableMap(mapa);
            this.ultimaCartaNumero = ultimaCartaNumero;
            this.cartaBufferNumero = cartaBufferNumero;
        }

        public List<PlayerState> getJogadores() { return jogadores; }
        public List<PropertyState> getPropriedades() { return propriedades; }
        public List<Integer> getOrdemTurno() { return ordemTurno; }
        public int getTurnoIndex() { return turnoIndex; }
        public int getTurnoDuplasConsecutivas() { return turnoDuplasConsecutivas; }
        public int getTurnoUltimoD1() { return turnoUltimoD1; }
        public int getTurnoUltimoD2() { return turnoUltimoD2; }
        public Integer getUltimoD1() { return ultimoD1; }
        public Integer getUltimoD2() { return ultimoD2; }
        public boolean isJaLancouNesteTurno() { return jaLancouNesteTurno; }
        public boolean isDeveIrParaPrisaoPorTerceiraDupla() { return deveIrParaPrisaoPorTerceiraDupla; }
        public Integer getPosicaoDaQuedaAtual() { return posicaoDaQuedaAtual; }
        public boolean isJaConstruiuNestaQueda() { return jaConstruiuNestaQueda; }
        public boolean isAcabouDeComprarNestaQueda() { return acabouDeComprarNestaQueda; }
        public boolean isSalvamentoDisponivel() { return salvamentoDisponivel; }
        public int getBancoSaldo() { return bancoSaldo; }
        public int getTamanhoBaralhoSR() { return tamanhoBaralhoSR; }
        public int getPonteiroBaralhoSR() { return ponteiroBaralhoSR; }
        public Map<Integer, Set<Integer>> getCartasSRPorJogador() { return cartasSRPorJogador; }
        public Integer getUltimaCartaNumero() { return ultimaCartaNumero; }
        public Integer getCartaBufferNumero() { return cartaBufferNumero; }
    }

    public SaveState exportarEstado() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        List<PlayerState> players = new ArrayList<>();
        for (Jogador j : jogadores) {
            players.add(new PlayerState(j.getId(), j.getSaldo(), j.getPosicao(), j.isAtivo(), j.isNaPrisao(), j.temCartaSaidaLivre()));
        }

        List<PropertyState> props = new ArrayList<>();
        for (int i = 0; i < tabuleiro.tamanho(); i++) {
            Casa c = tabuleiro.getCasa(i);
            if (c instanceof AtivoCompravel) {
                AtivoCompravel ativo = (AtivoCompravel) c;
                int donoId = ativo.temDono() ? ativo.getDono().getId() : -1;
                int numCasas = 0;
                boolean hotel = false;
                if (c instanceof Propriedade) {
                    Propriedade p = (Propriedade) c;
                    numCasas = p.getNumCasas();
                    hotel = p.temHotel();
                }
                props.add(new PropertyState(ativo.getPosicao(), donoId, numCasas, hotel));
            }
        }

        Map<Integer, Set<Integer>> cartas = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> e : cartasSRPorJogador.entrySet()) {
            cartas.put(e.getKey(), new HashSet<>(e.getValue()));
        }

        Integer ultimaCartaNumero = ultimaCartaSR.map(SorteRevesCard::getNumero).orElse(null);
        Integer bufferCartaNumero = srRecemSacada.map(SorteRevesCard::getNumero).orElse(null);

        return new SaveState(
                players,
                props,
                turno.snapshotOrdem(),
                turno.snapshotIdxVez(),
                turno.snapshotDuplasConsecutivas(),
                turno.snapshotUltimoD1(),
                turno.snapshotUltimoD2(),
                ultimoD1,
                ultimoD2,
                jaLancouNesteTurno,
                deveIrParaPrisaoPorTerceiraDupla,
                posicaoDaQuedaAtual,
                jaConstruiuNestaQueda,
                acabouDeComprarNestaQueda,
                salvamentoDisponivel,
                banco.getSaldo(),
                tamanhoBaralhoSR,
                ponteiroBaralhoSR,
                cartas,
                ultimaCartaNumero,
                bufferCartaNumero
        );
    }

    public void importarEstado(SaveState state) {
        if (state == null || state.getJogadores().isEmpty()) {
            throw new IllegalArgumentException("Estado inválido para importação.");
        }

        int qtdJogadores = state.getJogadores().size();
        this.jogadores.clear();
        for (int i = 0; i < qtdJogadores; i++) {
            this.jogadores.add(null);
        }
        for (PlayerState ps : state.getJogadores()) {
            if (ps.getId() < 0 || ps.getId() >= qtdJogadores) {
                throw new IllegalArgumentException("ID de jogador fora do intervalo no estado salvo.");
            }
            Jogador novo = new Jogador(ps.getId(), ps.getSaldo());
            novo.moverPara(ps.getPosicao());
            novo.setNaPrisao(ps.isNaPrisao());
            novo.setCartaSaidaLivre(ps.hasCartaSaidaLivre());
            if (!ps.isAtivo()) {
                novo.falir();
            }
            this.jogadores.set(ps.getId(), novo);
        }
        for (int i = 0; i < this.jogadores.size(); i++) {
            if (this.jogadores.get(i) == null) {
                throw new IllegalArgumentException("Estado salvo não possui dados para o jogador " + i);
            }
        }

        this.rng = new RandomProvider(null);

        if (state.getOrdemTurno().size() != qtdJogadores) {
            throw new IllegalArgumentException("Ordem dos jogadores inválida no estado salvo.");
        }
        this.turno = new Turno(qtdJogadores);
        this.turno.restaurarEstado(state.getOrdemTurno(), state.getTurnoIndex(), state.getTurnoDuplasConsecutivas(),
                                   state.getTurnoUltimoD1(), state.getTurnoUltimoD2());

        this.banco = new Banco(state.getBancoSaldo());

        if (this.tabuleiro == null) {
            this.tabuleiro = TabuleiroOficialFactory.criar();
        }
        for (int i = 0; i < tabuleiro.tamanho(); i++) {
            Casa c = tabuleiro.getCasa(i);
            if (c instanceof AtivoCompravel) {
                ((AtivoCompravel) c).resetarParaBanco();
            }
        }
        for (PropertyState ps : state.getPropriedades()) {
            Casa c = tabuleiro.getCasa(ps.getPosicao());
            if (!(c instanceof AtivoCompravel)) {
                continue;
            }
            AtivoCompravel ativo = (AtivoCompravel) c;
            if (ps.getDonoId() >= 0 && ps.getDonoId() < jogadores.size()) {
                ativo.setDono(jogadores.get(ps.getDonoId()));
                if (c instanceof Propriedade) {
                    Propriedade prop = (Propriedade) c;
                    for (int i = 0; i < ps.getNumCasas(); i++) {
                        if (!prop.podeConstruirCasa()) {
                            break;
                        }
                        prop.construirCasa();
                    }
                    if (ps.hasHotel() && prop.podeConstruirHotel()) {
                        prop.construirHotel();
                    }
                }
            } else {
                ativo.setDono(null);
            }
        }

        this.cartasSRPorJogador.clear();
        for (int i = 0; i < qtdJogadores; i++) {
            this.cartasSRPorJogador.put(i, new HashSet<>());
        }
        for (Map.Entry<Integer, Set<Integer>> entry : state.getCartasSRPorJogador().entrySet()) {
            int id = entry.getKey();
            if (!cartasSRPorJogador.containsKey(id)) {
                cartasSRPorJogador.put(id, new HashSet<>());
            }
            cartasSRPorJogador.get(id).addAll(entry.getValue());
        }

        for (Jogador jogador : jogadores) {
            if (jogador != null && jogador.temCartaSaidaLivre()) {
                Set<Integer> cartas = cartasSRPorJogador.get(jogador.getId());
                if (cartas == null) {
                    cartas = new HashSet<>();
                    cartasSRPorJogador.put(jogador.getId(), cartas);
                }
                cartas.add(9);
            }
        }

        this.tamanhoBaralhoSR = state.getTamanhoBaralhoSR() <= 0 ? SorteRevesCards.total() : state.getTamanhoBaralhoSR();
        int modulo = Math.max(1, this.tamanhoBaralhoSR);
        int ponteiro = state.getPonteiroBaralhoSR();
        if (ponteiro < 0) {
            ponteiro = 0;
        }
        this.ponteiroBaralhoSR = ponteiro % modulo;

        this.ultimoD1 = state.getUltimoD1();
        this.ultimoD2 = state.getUltimoD2();
        this.jaLancouNesteTurno = state.isJaLancouNesteTurno();
        this.deveIrParaPrisaoPorTerceiraDupla = state.isDeveIrParaPrisaoPorTerceiraDupla();
        this.posicaoDaQuedaAtual = state.getPosicaoDaQuedaAtual();
        this.jaConstruiuNestaQueda = state.isJaConstruiuNestaQueda();
        this.acabouDeComprarNestaQueda = state.isAcabouDeComprarNestaQueda();
        this.salvamentoDisponivel = state.isSalvamentoDisponivel();

        if (state.getUltimaCartaNumero() != null) {
            this.ultimaCartaSR = getCartaSorteRevesPorNumero(state.getUltimaCartaNumero());
        } else {
            this.ultimaCartaSR = Optional.empty();
        }

        if (state.getCartaBufferNumero() != null) {
            this.srRecemSacada = getCartaSorteRevesPorNumero(state.getCartaBufferNumero());
        } else {
            this.srRecemSacada = Optional.empty();
        }

        this.partidaEncerrada = false;
        this.resultadoPartida = null;
        this.autoLancamentoAposSaidaPrisao = false;
        this.executandoAutoLancamento = false;
        garantirJogadorDaVezAtivo();
        notifyObservers();
    }

    // =====================================================================================
    // SUPORTES INTERNOS
    // =====================================================================================

    private int getIndicePrisaoOrThrow() {
        int idx = encontrarIndicePorTipo("PRISAO");
        if (idx < 0)
            throw new IllegalStateException("Tabuleiro não possui casa PRISAO.");
        return idx;
    }

    /** IMPOSTO/LUCRO automáticos na casa atual (notificando a UI). */
    private Transacao aplicarCasaEspecialSeDevida() {
        final int id = getJogadorDaVez();
        final Jogador j = jogadores.get(id);
        final Casa casa = tabuleiro.getCasa(j.getPosicao());

        final String tipo = casa.getTipo();
        final int valor = Math.max(0, casa.getValorEfeito());

        if ("IMPOSTO".equalsIgnoreCase(tipo)) {
            if (valor <= 0)
                return Transacao.semEfeito("Imposto zero", id, casa.getPosicao(), null, 0);

            if (j.getSaldo() < valor) {
                tentarLevantarFundosPara(j, valor);
            }

            if (j.getSaldo() >= valor) {
                j.debitar(valor);
                banco.creditar(valor);
                notifyObservers();
                return Transacao.impostoPago(id, casa.getPosicao(), valor);
            } else {
                int disponivel = Math.max(0, j.getSaldo());
                if (disponivel > 0) {
                    j.debitar(disponivel);
                    banco.creditar(disponivel);
                }
                executarFalencia(j);
                notifyObservers();
                return Transacao.impostoPago(id, casa.getPosicao(), disponivel);
            }
        }

        if ("LUCRO".equalsIgnoreCase(tipo)) {
            if (valor <= 0)
                return Transacao.semEfeito("Lucro zero", id, casa.getPosicao(), null, 0);

            banco.debitar(valor);
            j.creditar(valor);
            notifyObservers();
            return Transacao.lucroRecebido(id, casa.getPosicao(), valor);
        }

        return null; // não é casa especial
    }

    @SuppressWarnings("unused")
    private int getIndiceVaParaPrisaoOrMinus1() {
        return encontrarIndicePorTipo("VA_PARA_PRISAO");
    }

    private int encontrarIndicePorTipo(String tipo) {
        for (int i = 0; i < tabuleiro.tamanho(); i++) {
            Casa c = tabuleiro.getCasa(i);
            if (tipo.equalsIgnoreCase(c.getTipo()))
                return i;
        }
        return -1;
    }

    private void tentarLevantarFundosPara(Jogador j, int valorNecessario) {
        if (j.getSaldo() >= valorNecessario)
            return;
        List<AtivoCompravel> minhas = listarAtivosDo(j);
        minhas.sort(Comparator.comparingInt(AtivoCompravel::valorAgregadoAtual).reversed());
        for (AtivoCompravel ativo : minhas) {
            if (j.getSaldo() >= valorNecessario)
                break;
            final int pagamento = (Math.max(0, ativo.valorAgregadoAtual()) * 9) / 10; // 90%
            banco.debitar(pagamento);
            j.creditar(pagamento);
            ativo.resetarParaBanco();
        }
        notifyObservers();
    }

    private void executarFalencia(Jogador j) {
        for (AtivoCompravel ativo : listarAtivosDo(j)) {
            ativo.resetarParaBanco();
        }
        j.falir();
        cartasSRPorJogador.remove(j.getId());
        verificarEncerramentoPorUltimoJogador();
    }

    private void verificarEncerramentoPorUltimoJogador() {
        if (partidaEncerrada || jogadores.isEmpty()) {
            return;
        }
        int ativos = 0;
        int ultimoAtivoId = -1;
        for (Jogador jog : jogadores) {
            if (jog.isAtivo()) {
                ativos++;
                ultimoAtivoId = jog.getId();
            }
        }
        if (ativos == 1 && ultimoAtivoId >= 0) {
            encerrarPartida(FimPartidaMotivo.ULTIMO_JOGADOR_RESTANTE);
        }
    }

    public void definirOrdemJogadores(List<Integer> ordem) {
        exigirPartidaIniciada();
        if (ordem == null || ordem.size() != jogadores.size()) {
            throw new IllegalArgumentException("Ordem inválida: tamanho diferente do número de jogadores.");
        }
        boolean[] seen = new boolean[jogadores.size()];
        for (Integer id : ordem) {
            if (id == null || id < 0 || id >= jogadores.size() || seen[id]) {
                throw new IllegalArgumentException("Ordem inválida: ids repetidos/fora do intervalo.");
            }
            seen[id] = true;
        }
        turno.definirOrdem(ordem);
        notifyObservers();
    }

    public void carregarTabuleiroOficialBR() {
        this.setTabuleiro(TabuleiroOficialFactory.criar());
        notifyObservers();
    }

    // =====================================================================================
    // OBSERVER
    // =====================================================================================

    public static interface Observer {
        void update(GameModel source);
    }

    private final java.util.concurrent.CopyOnWriteArrayList<Observer> observers =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addObserver(Observer o) {
        if (o != null) observers.addIfAbsent(o);
    }

    public void removeObserver(Observer o) {
        if (o != null) observers.remove(o);
    }

    private void notifyObservers() {
        for (Observer o : observers) {
            try { o.update(this); } catch (Exception ignore) {}
        }
    }

    public Integer getUltimoD1() { return ultimoD1; }
    public Integer getUltimoD2() { return ultimoD2; }
    public boolean houveDupla() { return turno != null && turno.houveDupla(); }
    public boolean passouOuCaiuNoInicioDaUltimaJogada() { return false; }
}
