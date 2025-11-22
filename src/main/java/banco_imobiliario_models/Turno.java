package banco_imobiliario_models;

import java.util.ArrayList;
import java.util.List;

/** Estado do turno: ordem customizada + controle de duplas consecutivas. */
final class Turno {
    private final List<Integer> ordem = new ArrayList<>(); // ids dos jogadores na ordem de jogo
    private int idxVez = 0;

    private int duplasConsecutivas;
    private int ultimoD1, ultimoD2;

    /** Cria com ordem padrão 0..(n-1). */
    Turno(int numJogadores) {
        for (int i = 0; i < numJogadores; i++) ordem.add(i);
        this.duplasConsecutivas = 0;
    }

    /** Define a ordem explicitamente (ids 0..N-1). Zera contadores e começa do primeiro. */
    void definirOrdem(List<Integer> novaOrdem) {
        ordem.clear();
        ordem.addAll(novaOrdem);
        idxVez = 0;
        duplasConsecutivas = 0;
        ultimoD1 = ultimoD2 = 0;
    }

    void registrarLance(int d1, int d2) {
        this.ultimoD1 = d1;
        this.ultimoD2 = d2;
        if (d1 == d2) {
            duplasConsecutivas++;
        } else {
            duplasConsecutivas = 0;
        }
    }

    void resetarDuplas() {
        this.duplasConsecutivas = 0;
    }

    boolean houveDupla() {
        return ultimoD1 == ultimoD2;
    }

    int getDuplasConsecutivas() {
        return duplasConsecutivas;
    }

    int getJogadorDaVez() {
        return ordem.get(idxVez);
    }

    /** Avança a vez seguindo a lista 'ordem'. */
    void passarVez() {
        idxVez = (idxVez + 1) % ordem.size();
    }

    List<Integer> snapshotOrdem() {
        return new ArrayList<>(ordem);
    }

    int snapshotIdxVez() {
        return idxVez;
    }

    int snapshotDuplasConsecutivas() {
        return duplasConsecutivas;
    }

    int snapshotUltimoD1() {
        return ultimoD1;
    }

    int snapshotUltimoD2() {
        return ultimoD2;
    }

    void restaurarEstado(List<Integer> novaOrdem,
                         int novoIdxVez,
                         int novasDuplas,
                         int novoUltimoD1,
                         int novoUltimoD2) {
        if (novaOrdem == null || novaOrdem.isEmpty()) {
            throw new IllegalArgumentException("Ordem inválida para restauração.");
        }
        ordem.clear();
        ordem.addAll(novaOrdem);
        if (novoIdxVez < 0 || novoIdxVez >= ordem.size()) {
            throw new IllegalArgumentException("Índice da vez fora do intervalo.");
        }
        this.idxVez = novoIdxVez;
        this.duplasConsecutivas = Math.max(0, novasDuplas);
        this.ultimoD1 = novoUltimoD1;
        this.ultimoD2 = novoUltimoD2;
    }
}
