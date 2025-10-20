package banco_imobiliario_controller;

import java.awt.Color;

/** DTO simples para armazenar nome, cor e índice do pino do jogador no Controller/UI. */
public final class PlayerProfile {
    private final int id;         // 0..N-1 (ordem do GameModel)
    private final String nome;    // 1..8 chars (alfa-numérico)
    private final Color cor;      // cor única por jogador (para UI)
    private final int pawnIndex;  // 0..5 mapeando para pin0..pin5.png (=-1 se desconhecido)

    /** Construtor preferencial: informa também o índice do pino. */
    public PlayerProfile(int id, String nome, Color cor, int pawnIndex) {
        this.id = id;
        this.nome = nome;
        this.cor  = cor;
        this.pawnIndex = pawnIndex;
    }

    /** Construtor de compatibilidade (sem índice do pino). Usa -1. */
    public PlayerProfile(int id, String nome, Color cor) {
        this(id, nome, cor, -1);
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public Color getCor() { return cor; }
    public int getPawnIndex() { return pawnIndex; }

    public String getCorHex() {
        return String.format("#%02X%02X%02X", cor.getRed(), cor.getGreen(), cor.getBlue());
    }
}
