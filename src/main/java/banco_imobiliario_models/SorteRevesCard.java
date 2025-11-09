package banco_imobiliario_models;

public final class SorteRevesCard {
    private final int id;
    private final String titulo;
    private final String descricao;

    public SorteRevesCard(int id, String titulo, String descricao) {
        this.id = id;
        this.titulo = titulo == null ? "" : titulo;
        this.descricao = descricao == null ? "" : descricao;
    }

    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }

    // Compatibilidade (se em algum lugar antigo você chamou getNumero()):
    @Deprecated public int getNumero() { return id; }
}
