package Banco_Imobiliario_Models;

/**
 * Regra #1: lançamento virtual de 2 dados.
 *
 * Métodos da Regra #1:
 *  - novaPartida(numJogadores, seedOpcional)
 *  - lancarDados() -> int[]{d1, d2}
 *  - houveDuplaNoUltimoLancamento()
 *  - getContagemDuplasConsecutivasDaVez()
 */
public class GameModel {

    // RNG e dados (objetos não públicos)
    private RandomProvider rng;
    private final Dado dado1 = new Dado();
    private final Dado dado2 = new Dado();
    private Turno turno;

    /**
     * Inicia uma nova partida. Apenas validações mínimas aqui.
     * @param numJogadores entre 2 e 6 (regra do jogo)
     * @param seedOpcional se não-nula, torna os lançamentos determinísticos (útil p/ testes)
     */
    public void novaPartida(int numJogadores, Long seedOpcional) {
        if (numJogadores < 2 || numJogadores > 6) {
            throw new IllegalArgumentException("Número de jogadores deve estar entre 2 e 6.");
        }
        this.rng = new RandomProvider(seedOpcional);
        this.turno = new Turno(0); // ordem fixa simplificada nesta iteração
    }

    private void exigirPartidaIniciada() {
        if (rng == null || turno == null) {
            throw new IllegalStateException("Partida não iniciada. Chame novaPartida().");
        }
    }

    /**
     * Regra #1: lança dois dados virtuais (1..6) e retorna os valores.
     * Também registra (no estado do turno) se foi dupla e quantas duplas seguidas.
     */
    public int[] lancarDados() {
        exigirPartidaIniciada();

        int d1 = dado1.rolar(rng);   // 1..6
        int d2 = dado2.rolar(rng);   // 1..6

        turno.registrarLance(d1, d2);

        // Observação: a consequência de 2ª/3ª dupla (nova jogada / prisão)
        // será tratada nas regras de movimento/prisão nas próximas etapas,
        // conforme as regras oficiais (dupla -> nova jogada; 3ª dupla -> prisão) :contentReference[oaicite:4]{index=4}.
        return new int[]{ d1, d2 };
    }

    /** @return true se o último lançamento foi uma dupla */
    public boolean houveDuplaNoUltimoLancamento() {
        exigirPartidaIniciada();
        return turno.houveDupla();
    }

    /** @return número de duplas consecutivas do jogador da vez (reinicia quando não é dupla) */
    public int getContagemDuplasConsecutivasDaVez() {
        exigirPartidaIniciada();
        return turno.getDuplasConsecutivas();
    }

    /** Disponível para as próximas regras; aqui mantemos simples. */
    public int getJogadorDaVez() {
        exigirPartidaIniciada();
        return turno.getJogadorDaVez();
    }
}
