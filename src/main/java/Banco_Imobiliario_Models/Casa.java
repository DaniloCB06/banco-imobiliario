package Banco_Imobiliario_Models;

class Casa {
    protected int posicao;
    protected String nome;
    protected String tipo;

    Casa(int posicao, String nome, String tipo) {
        this.posicao = posicao;
        this.nome = nome;
        this.tipo = tipo;
    }

    int getPosicao() { return posicao; }
    String getNome() { return nome; }
    String getTipo() { return tipo; }
}
