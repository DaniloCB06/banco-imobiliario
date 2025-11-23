package banco_imobiliario_controller;

import java.awt.Color;


public final class PlayerProfile {
    private final int id;        
    private final String nome;   
    private final Color cor;      
    private final int pawnIndex;  

    
    public PlayerProfile(int id, String nome, Color cor, int pawnIndex) {
        this.id = id;
        this.nome = nome;
        this.cor  = cor;
        this.pawnIndex = pawnIndex;
    }

    
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
