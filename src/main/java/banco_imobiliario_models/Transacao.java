package banco_imobiliario_models;

public class Transacao {

    private final String tipo;              
    private final boolean efetuada;         
    private final int pagadorId;            
    private final Integer recebedorId;      
    private final int posicaoPropriedade;   
    private final int valor;                
    private final String motivo;            

    private Transacao(String tipo, boolean efetuada, int pagadorId,
                      Integer recebedorId, int posicaoPropriedade, int valor, String motivo) {
        this.tipo = tipo;
        this.efetuada = efetuada;
        this.pagadorId = pagadorId;
        this.recebedorId = recebedorId;
        this.posicaoPropriedade = posicaoPropriedade;
        this.valor = valor;
        this.motivo = motivo;
    }

    public static Transacao aluguelEfetuado(int pagadorId, int recebedorId, int posicaoProp, int valor) {
        return new Transacao("ALUGUEL", true, pagadorId, recebedorId, posicaoProp, valor, null);
    }

    
    public static Transacao impostoPago(int pagadorId, int posicao, int valor) {
        return new Transacao("IMPOSTO", true, pagadorId, null, posicao, valor, null);
    }

    
    public static Transacao lucroRecebido(int recebedorId, int posicao, int valor) {
        
        return new Transacao("LUCRO", true, recebedorId, null, posicao, valor, null);
    }

    public static Transacao semEfeito(String motivo, int pagadorId, int posicaoProp, Integer recebedorId, int valor) {
        return new Transacao("SEM_EFEITO", false, pagadorId, recebedorId, posicaoProp, valor, motivo);
    }

    public String getTipo() { return tipo; }
    public boolean isEfetuada() { return efetuada; }
    public int getPagadorId() { return pagadorId; }
    public Integer getRecebedorId() { return recebedorId; }
    public int getPosicaoPropriedade() { return posicaoPropriedade; }
    public int getValor() { return valor; }
    public String getMotivo() { return motivo; }
}
