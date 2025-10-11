package Banco_Imobiliario_Models;

import java.util.ArrayList;
import java.util.List;

/**
 * API pública do Model (Iteração 1)
 *
 * Cobertura nesta etapa:
 *  - Regra #1: lançar dados.
 *  - Regra #2 (parcial): deslocar o peão (sem efeitos).
 *  - Regra #3: comprar propriedade sem dono (pagamento automático).
 *  - Regra #4: construir imóvel (casa/hotel) na propriedade em que o jogador ACABOU DE CAIR,
 *              obedecendo às Regras-Adicionais:
 *                * Na 1ª vez que cair: pode comprar (se disponível).
 *                * Nas vezes subsequentes em que cair na MESMA propriedade: pode construir UM imóvel por vez.
 *                * Hotel requer pelo menos 1 casa.
 *                * Limites: 0–4 casas e 1 hotel.
 *
 * Observações:
 *  - Todas as demais classes do pacote são não públicas.
 *  - Não há troca de turno, prisão, honorários, aluguel ou falência nesta classe (virão em regras futuras).
 */
public class GameModel {

    // ----- Regra #1 -----
    private RandomProvider rng;
    private final Dado dado1 = new Dado();
    private final Dado dado2 = new Dado();
    private Turno turno;

    // Banco (software como banqueiro, saldo inicial 200_000)
    private Banco banco;

    // Último lançamento (para deslocamento)
    private Integer ultimoD1 = null, ultimoD2 = null;

    // ---- CONTEXTO DA QUEDA (para construir na mesma casa onde caiu) ----
    private Integer posicaoDaQuedaAtual = null; // posição onde o peão parou no último deslocamento
    private boolean jaConstruiuNestaQueda = false;
    private boolean acabouDeComprarNestaQueda = false;

    // Estado do jogo
    private final List<Jogador> jogadores = new ArrayList<>();
    private Tabuleiro tabuleiro;

    /** DTO de retorno do lançamento de dados (sem arrays). */
    public static final class ResultadoDados {
        private final int d1;
        private final int d2;
        public ResultadoDados(int d1, int d2) { this.d1 = d1; this.d2 = d2; }
        public int getD1() { return d1; }
        public int getD2() { return d2; }
        public int getSoma() { return d1 + d2; }
        public boolean isDupla() { return d1 == d2; }
    }

    // =====================================================================================
    //                                     SETUP
    // =====================================================================================

    /**
     * Inicia uma nova partida (2..6 jogadores).
     * Cria jogadores e zera posições. O tabuleiro deve ser carregado separadamente.
     */
    public void novaPartida(int numJogadores, Long seedOpcional) {
        if (numJogadores < 2 || numJogadores > 6) {
            throw new IllegalArgumentException("Número de jogadores deve estar entre 2 e 6.");
        }
        this.rng = new RandomProvider(seedOpcional);
        this.turno = new Turno(0); // ordem fixa nesta iteração
        this.banco = new Banco(200_000);

        this.jogadores.clear();
        for (int i = 0; i < numJogadores; i++) {
            this.jogadores.add(new Jogador(i, 4000));
        }

        this.ultimoD1 = this.ultimoD2 = null;
        limparContextoDeQueda();
    }

    /** Permite injetar o tabuleiro carregado (loader/planilha virá em outra unidade). */
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

    // =====================================================================================
    //                                REGRA #1: DADOS
    // =====================================================================================

    public ResultadoDados lancarDados() {
        exigirPartidaIniciada();
        int d1 = dado1.rolar(rng);
        int d2 = dado2.rolar(rng);
        turno.registrarLance(d1, d2);
        this.ultimoD1 = d1;
        this.ultimoD2 = d2;
        return new ResultadoDados(d1, d2);
    }

    public boolean houveDuplaNoUltimoLancamento() {
        exigirPartidaIniciada();
        return turno.houveDupla();
    }

    public int getContagemDuplasConsecutivasDaVez() {
        exigirPartidaIniciada();
        return turno.getDuplasConsecutivas();
    }

    public int getJogadorDaVez() {
        exigirPartidaIniciada();
        return turno.getJogadorDaVez();
    }

    // =====================================================================================
    //                          REGRA #2 (PARCIAL): DESLOCAMENTO
    // =====================================================================================

    /**
     * Desloca o peão do jogador da vez pela soma do último lançamento.
     * NÃO aplica honorários, troca de turno, prisão ou efeitos de casa.
     */
    public ResultadoMovimento deslocarPiao() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();
        if (ultimoD1 == null || ultimoD2 == null) {
            throw new IllegalStateException("É necessário lançar os dados antes de deslocar.");
        }
        final int id = getJogadorDaVez();
        final Jogador j = jogadores.get(id);
        ResultadoMovimento r = Movimento.executar(j, ultimoD1, ultimoD2, tabuleiro);

        // Reinicia o contexto da QUEDA (usado pelas construções)
        this.posicaoDaQuedaAtual = j.getPosicao();
        this.jaConstruiuNestaQueda = false;
        this.acabouDeComprarNestaQueda = false;

        return r;
    }

    // =====================================================================================
    //                       REGRA #3: COMPRAR PROPRIEDADE SEM DONO
    // =====================================================================================

    /**
     * Tenta comprar a propriedade na casa atual do jogador da vez.
     * Regras:
     *  - Só se a casa atual for Propriedade e estiver sem dono.
     *  - Só se o jogador tiver saldo suficiente.
     *  - Pagamento é automático (débito do jogador, crédito do Banco).
     *  - Marca que a compra ocorreu nesta QUEDA (não poderá construir nesta mesma queda).
     * @return true se a compra foi realizada; false caso contrário.
     */
    public boolean comprarPropriedade() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        final Jogador jogador = jogadores.get(getJogadorDaVez());
        final Casa casaAtual = tabuleiro.getCasa(jogador.getPosicao());

        if (!(casaAtual instanceof Propriedade)) return false;
        final Propriedade prop = (Propriedade) casaAtual;
        if (prop.temDono()) return false;

        final int preco = prop.getPrecoTerreno();
        if (preco <= 0) return false;
        if (jogador.getSaldo() < preco) return false;

        // pagamento automático
        jogador.debitar(preco);
        banco.creditar(preco);

        // transfere a posse
        prop.setDono(jogador);

        // Marca que acabou de comprar nesta queda (Regras-Adicionais: só constrói em quedas subsequentes)
        this.acabouDeComprarNestaQueda = true;
        return true;
    }

    // =====================================================================================
    //                       REGRA #4: CONSTRUÇÕES (CASA / HOTEL)
    // =====================================================================================

    /**
     * Constrói UMA casa na propriedade onde o jogador da vez ACABOU DE CAIR,
     * respeitando:
     *  - Construções só em quedas subsequentes na MESMA propriedade (não na queda da compra).
     *  - Apenas 1 imóvel por queda (casa OU hotel).
     *  - Limite de 0–4 casas; sem hotel presente.
     *  - Saldo suficiente.
     * @return true se a construção ocorreu.
     */
    public boolean construirCasa() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        if (posicaoDaQuedaAtual == null) return false;

        final Jogador jogador = jogadores.get(getJogadorDaVez());
        if (jogador.getPosicao() != posicaoDaQuedaAtual.intValue()) return false;

        final Casa casa = tabuleiro.getCasa(jogador.getPosicao());
        if (!(casa instanceof Propriedade)) return false;

        final Propriedade prop = (Propriedade) casa;
        if (!prop.temDono() || prop.getDono() != jogador) return false; // precisa ser do jogador
        if (acabouDeComprarNestaQueda) return false;   // só em quedas subsequentes
        if (jaConstruiuNestaQueda) return false;        // 1 por queda
        if (!prop.podeConstruirCasa()) return false;    // 0..4 e sem hotel

        final int preco = prop.getPrecoCasa();
        if (preco <= 0 || jogador.getSaldo() < preco) return false;

        jogador.debitar(preco);
        banco.creditar(preco);
        prop.construirCasa();

        jaConstruiuNestaQueda = true;
        return true;
    }

    /**
     * Constrói UM hotel na propriedade onde o jogador da vez ACABOU DE CAIR,
     * respeitando:
     *  - Construções só em quedas subsequentes na MESMA propriedade (não na queda da compra).
     *  - Apenas 1 imóvel por queda (casa OU hotel).
     *  - Hotel requer pelo menos 1 casa e não pode já existir hotel.
     *  - Saldo suficiente.
     * @return true se a construção ocorreu.
     */
    public boolean construirHotel() {
        exigirPartidaIniciada();
        exigirTabuleiroCarregado();

        if (posicaoDaQuedaAtual == null) return false;

        final Jogador jogador = jogadores.get(getJogadorDaVez());
        if (jogador.getPosicao() != posicaoDaQuedaAtual.intValue()) return false;

        final Casa casa = tabuleiro.getCasa(jogador.getPosicao());
        if (!(casa instanceof Propriedade)) return false;

        final Propriedade prop = (Propriedade) casa;
        if (!prop.temDono() || prop.getDono() != jogador) return false;
        if (acabouDeComprarNestaQueda) return false;
        if (jaConstruiuNestaQueda) return false;
        if (!prop.podeConstruirHotel()) return false;   // requer >=1 casa e ainda não ter hotel

        final int preco = prop.getPrecoHotel();
        if (preco <= 0 || jogador.getSaldo() < preco) return false;

        jogador.debitar(preco);
        banco.creditar(preco);
        prop.construirHotel();

        jaConstruiuNestaQueda = true;
        return true;
    }

    // =====================================================================================
    //                       CONSULTAS ÚTEIS (apoio aos testes)
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

    // =====================================================================================
    //                          HELPERS DE TABULEIRO (para testes)
    // =====================================================================================

    /** Cria tabuleiro simples com N casas genéricas. */
    public void carregarTabuleiroMinimoParaTeste(int nCasas) {
        if (nCasas <= 0) throw new IllegalArgumentException("nCasas deve ser > 0");
        List<Casa> casas = new ArrayList<>(nCasas);
        for (int i = 0; i < nCasas; i++) {
            casas.add(new Casa(i, "Casa " + i, "GENERICA"));
        }
        this.setTabuleiro(new Tabuleiro(casas));
    }

    /**
     * Helper: cria um tabuleiro com N casas genéricas e 1 Propriedade na posição indicada.
     * Versão básica (preços de casa/hotel = 0).
     */
    public void carregarTabuleiroDeTesteComUmaPropriedade(int nCasas, int idxPropriedade, int precoTerreno) {
        carregarTabuleiroDeTesteComUmaPropriedade(nCasas, idxPropriedade, precoTerreno, 0, 0);
    }

    /** Helper: idem, informando preço de casa (hotel = 0). */
    public void carregarTabuleiroDeTesteComUmaPropriedade(int nCasas, int idxPropriedade,
                                                          int precoTerreno, int precoCasa) {
        carregarTabuleiroDeTesteComUmaPropriedade(nCasas, idxPropriedade, precoTerreno, precoCasa, 0);
    }

    /** Helper: idem, informando preços de casa e hotel. */
    public void carregarTabuleiroDeTesteComUmaPropriedade(int nCasas, int idxPropriedade,
                                                          int precoTerreno, int precoCasa, int precoHotel) {
        if (nCasas <= 0) throw new IllegalArgumentException("nCasas deve ser > 0");
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
    }
}
