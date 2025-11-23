package banco_imobiliario_models;

import java.util.*;

/**
 * Gerencia o baralho de Sorte/Revés e o "banco de cartas" por jogador.
 * - Unicidade global: uma carta sorteada sai do baralho e não reaparece.
 * - Banco por jogador: guarda os IDs possuídos.
 * Pensado para, no futuro, também acomodar cartas de território.
 */
public final class SorteRevesManager {

    // Catálogo completo (id -> carta)
    private final Map<Integer, SorteRevesCard> catalogo = new HashMap<>();
    // Conjunto de IDs ainda disponíveis para sorteio
    private final Set<Integer> disponiveis = new HashSet<>();
    // Banco por jogador: playerId -> conjunto de IDs
    private final Map<Integer, Set<Integer>> bancoPorJogador = new HashMap<>();

    private Integer ultimaCartaId = null;

    public void resetarComCatalogo(List<SorteRevesCard> cards) {
        catalogo.clear();
        for (SorteRevesCard c : cards) catalogo.put(c.getId(), c);
        disponiveis.clear();
        disponiveis.addAll(catalogo.keySet());
        bancoPorJogador.clear();
        ultimaCartaId = null;
    }

    /** Atalho: cria automaticamente o catálogo 1..N com textos genéricos. */
    public void resetarSequenciaPadrao(int total) {
        List<SorteRevesCard> list = new ArrayList<>(total);
        for (int i = 1; i <= total; i++) {
            list.add(new SorteRevesCard(i, "Carta " + i, "Descrição da carta " + i + "."));
        }
        resetarComCatalogo(list);
    }

    /** Sorteia aleatoriamente uma carta disponível e a move para o banco do jogador. */
    public Optional<SorteRevesCard> sortearParaJogador(int jogadorId, Random rng) {
        if (disponiveis.isEmpty()) return Optional.empty();
        // escolher um ID aleatório do set
        int idx = rng.nextInt(disponiveis.size());
        int chosen = -1;
        int i = 0;
        for (int id : disponiveis) {
            if (i++ == idx) { chosen = id; break; }
        }
        if (chosen < 0) return Optional.empty();

        disponiveis.remove(chosen);
        Set<Integer> cartas = bancoPorJogador.get(jogadorId);
        if (cartas == null) {
            cartas = new HashSet<>();
            bancoPorJogador.put(jogadorId, cartas);
        }
        cartas.add(chosen);
        ultimaCartaId = chosen;
        return Optional.of(catalogo.get(chosen));
    }

    public Set<Integer> getCartasDoJogador(int jogadorId) {
        return Collections.unmodifiableSet(bancoPorJogador.getOrDefault(jogadorId, Collections.emptySet()));
    }

    public Optional<SorteRevesCard> getUltimaCarta() {
        return ultimaCartaId == null ? Optional.empty() : Optional.ofNullable(catalogo.get(ultimaCartaId));
    }

    public Optional<Integer> getUltimaCartaId() {
        return Optional.ofNullable(ultimaCartaId);
    }

    public boolean temDisponiveis() { return !disponiveis.isEmpty(); }
}
