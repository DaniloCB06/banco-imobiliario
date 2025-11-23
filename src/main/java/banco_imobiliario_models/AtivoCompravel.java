package banco_imobiliario_models;

interface AtivoCompravel {
    boolean temDono();
    Jogador getDono();
    void setDono(Jogador novoDono);
    int getPosicao();
    int getPrecoCompra();
    int calcularAluguel();
    int valorAgregadoAtual();
    void resetarParaBanco();
}
