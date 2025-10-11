package Banco_Imobiliario_Models;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras #1 e #2 (parcial):
 *  - Regra #1: lançar dados.
 *  - Regra #2: deslocar o peão do jogador da vez pelo valor dos dados.
 */
public class GameModel {

    // ----- Regra #1 (existente) -----
    private RandomProvider rng;
    private final Dado dado1 = new Dado();
    private final Dado dado2 = new Dado();
    private Turno turno;

    // Guardamos o último lançamento para a Regra 2
    private Integer ultimoD1 = null, ultimoD2 = null;

    private final List<Jogador> jogadores = new ArrayList<>();
    private Tabuleiro tabuleiro;

    /** DTO da API para retorno de lançamento de dados (sem arrays). */
    public static final class ResultadoDados {
        private final int d1;
        private final int d2;
        public ResultadoDados(int d1, int d2) {
            this.d1 = d1; this.d2 = d2;
        }
        public int getD1() { return d1; }
        public int getD2() { return d2; }
        public int getSoma() { return d1 + d2; }
        public boolean isDupla() { return d1 == d2; }
    }

    /**
     * Inicia uma nova partida. Validações mínimas.
     * Cria os jogadores (0..numJogadores-1) e zera suas posições.
     * O tabuleiro deve ser carregado separadamente antes de deslocar.
     */
    public void novaPartida(int numJogadores, Long seedOpcional) {
        if (numJogadores < 2 || numJogadores > 6) {
            throw new IllegalArgumentException("Número de jogadores deve estar entre 2 e 6.");
        }
        this.rng = new RandomProvider(seedOpcional);
        this.turno = new Turno(0); // ordem fixa nesta iteração

        this.jogadores.clear();
        for (int i = 0; i < numJogadores; i++) {
            this.jogadores.add(new Jogador(i, 4000));
        }
        this.ultimoD1 = this.ultimoD2 = null;
    }

    private void exigirPartidaIniciada() {
        if (rng == null || turno == null || jogadores.isEmpty()) {
            throw new IllegalStateException("Partida não iniciada. Chame novaPartida().");
        }
    }

    /** Permite injetar o tabuleiro que foi carregado */
    public void setTabuleiro(Tabuleiro tabuleiro) {
        if (tabuleiro == null || tabuleiro.tamanho() <= 0) {
            throw new IllegalArgumentException("Tabuleiro inválido.");
        }
        this.tabuleiro = tabuleiro;
    }

    private void exigirTabuleiroCarregado() {
        if (this.tabuleiro == null) {
            throw new IllegalStateException("Tabuleiro não carregado.");
        }
    }

    // ---------- Regra #1 (lançar dados) ----------
    /** Agora retorna um objeto de resultado em vez de array. */
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

    // ---------- Regra #2 (apenas deslocamento) ----------
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
        return Movimento.executar(j, ultimoD1, ultimoD2, tabuleiro);
    }

    // ---------- Consultas úteis para testes da regra #2 ----------
    public int getPosicaoJogador(int idJogador) {
        exigirPartidaIniciada();
        return jogadores.get(idJogador).getPosicao();
    }

    public int getQuantidadeCasasTabuleiro() {
        exigirTabuleiroCarregado();
        return tabuleiro.tamanho();
    }

    // --------- APENAS PARA TESTES ----------
    public void carregarTabuleiroMinimoParaTeste(int nCasas) {
        if (nCasas <= 0) throw new IllegalArgumentException("nCasas deve ser > 0");
        List<Casa> casas = new ArrayList<>(nCasas);
        for (int i = 0; i < nCasas; i++) {
            // Compatível com Casa(int posicao, String nome, String tipo)
            casas.add(new Casa(i, "Casa " + i, "GENERICA"));
        }
        this.setTabuleiro(new Tabuleiro(casas));
    }
}
