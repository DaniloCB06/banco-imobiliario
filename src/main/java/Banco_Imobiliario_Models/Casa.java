package Banco_Imobiliario_Models;

public class Casa {
	protected int posicao;
	protected String nome;
	protected String tipo;
	
	
	public Casa(int posicao, String nome, String tipo) {
        this.posicao = posicao;
        this.nome = nome;
        this.tipo = tipo;
    }
	
	
	public int getPosicao() { return posicao; }
    public String getNome() { return nome; }
    public String getTipo() { return tipo; }
}
