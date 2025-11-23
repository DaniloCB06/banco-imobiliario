package banco_imobiliario_models;

import java.util.*;

public final class SorteRevesManager {

    
    private final Map<Integer, SorteRevesCard> catalogo = new HashMap<>();
    
    private final Set<Integer> disponiveis = new HashSet<>();
    
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

    
    public void resetarSequenciaPadrao(int total) {
        List<SorteRevesCard> list = new ArrayList<>(total);
        for (int i = 1; i <= total; i++) {
            list.add(new SorteRevesCard(i, "Carta " + i, "Descrição da carta " + i + "."));
        }
        resetarComCatalogo(list);
    }

    
    public Optional<SorteRevesCard> sortearParaJogador(int jogadorId, Random rng) {
        if (disponiveis.isEmpty()) return Optional.empty();
        
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
