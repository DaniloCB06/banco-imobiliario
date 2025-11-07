package banco_imobiliario_models;

class Casa {
    protected int posicao;
    protected String nome;
    protected String tipo;
    // NOVO: valor associado a casas especiais (IMPOSTO/LUCRO). Para demais, 0.
    protected int valorEfeito;

    Casa(int posicao, String nome, String tipo) {
        this(posicao, nome, tipo, 0);
    }

    // NOVO construtor com valor
    Casa(int posicao, String nome, String tipo, int valorEfeito) {
        this.posicao = posicao;
        this.nome = nome;
        this.tipo = tipo;
        this.valorEfeito = Math.max(0, valorEfeito);
    }

    int getPosicao() { return posicao; }
    String getNome() { return nome; }
    String getTipo() { return tipo; }
    // NOVO getter
    int getValorEfeito() { return valorEfeito; }
}
