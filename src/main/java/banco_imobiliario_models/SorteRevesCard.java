package banco_imobiliario_models;

final class SorteRevesCard {
    private final int id;
    private final String titulo;
    private final String descricao;

    SorteRevesCard(int id, String titulo, String descricao) {
        this.id = id;
        this.titulo = titulo == null ? "" : titulo;
        this.descricao = descricao == null ? "" : descricao;
    }

    int getId() { return id; }
    String getTitulo() { return titulo; }
    String getDescricao() { return descricao; }

    @Deprecated int getNumero() { return id; }
}
