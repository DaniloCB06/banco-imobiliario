package banco_imobiliario_models;

final class Companhia extends Casa implements AtivoCompravel {
    private final int precoCompra;
    private final int aluguelFixo;
    private Jogador dono;

    Companhia(int posicao, String nome, int precoCompra, int aluguelFixo) {
        super(posicao, nome, "SERVICO");
        this.precoCompra = Math.max(0, precoCompra);
        this.aluguelFixo = Math.max(0, aluguelFixo);
        this.dono = null;
    }

    @Override
    public boolean temDono() { return dono != null; }

    @Override
    public Jogador getDono() { return dono; }

    @Override
    public void setDono(Jogador novoDono) { this.dono = novoDono; }

    @Override
    public int getPosicao() { return this.posicao; }

    @Override
    public int getPrecoCompra() { return precoCompra; }

    @Override
    public int calcularAluguel() { return aluguelFixo; }

    @Override
    public int valorAgregadoAtual() { return precoCompra; }

    @Override
    public void resetarParaBanco() { this.dono = null; }
}
