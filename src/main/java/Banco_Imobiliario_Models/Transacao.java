package Banco_Imobiliario_Models;

/**
 * DTO simples para relatar efeitos financeiros de uma ação (ex.: aluguel).
 * É público para que os testes (em outro pacote) possam inspecionar o resultado.
 */
public class Transacao {

    private final String tipo;              // "ALUGUEL" ou "SEM_EFEITO"
    private final boolean efetuada;         // true se houve débito/crédito
    private final int pagadorId;            // id do jogador da vez (quando aplicável)
    private final Integer recebedorId;      // id do dono, quando aplicável
    private final int posicaoPropriedade;   // posição da casa no tabuleiro
    private final int valor;                // valor envolvido (0 em SEM_EFEITO)
    private final String motivo;            // detalhe para SEM_EFEITO

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
