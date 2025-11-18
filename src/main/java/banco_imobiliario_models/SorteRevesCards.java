package banco_imobiliario_models;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Catalogo central das cartas de Sorte/Reves.
 * Mantem metadados (titulo, descricao) e o tipo de efeito associado.
 */
final class SorteRevesCards {

    private SorteRevesCards() {}

    enum EffectType {
        RECEBER_DO_BANCO,
        PAGAR_AO_BANCO,
        RECEBER_DE_CADA_JOGADOR,
        SAIDA_LIVRE_DA_PRISAO,
        IR_PARA_PRISAO
    }

    static final class Definition {
        private final int id;
        private final String titulo;
        private final String descricao;
        private final EffectType effectType;
        private final int valor;

        private Definition(int id, EffectType effectType, int valor, String titulo, String descricao) {
            this.id = id;
            this.effectType = effectType;
            this.valor = Math.max(0, valor);
            this.titulo = titulo == null ? String.format(Locale.ROOT, "Sorte/Reves #%02d", id) : titulo;
            this.descricao = descricao == null ? "" : descricao;
        }

        int getId() { return id; }
        String getTitulo() { return titulo; }
        String getDescricao() { return descricao; }
        EffectType getEffectType() { return effectType; }
        int getValor() { return valor; }
    }

    private static final Map<Integer, Definition> DEFINITIONS;

    static {
        Map<Integer, Definition> defs = new LinkedHashMap<>();

        java.util.function.IntFunction<String> dinheiro = v -> String.format(Locale.ROOT, "R$ %d", v);
        java.util.function.BiConsumer<Integer, Integer> receberBanco = (id, valor) ->
                register(defs, id, EffectType.RECEBER_DO_BANCO, valor,
                        "Receba " + dinheiro.apply(valor),
                        "Receba " + dinheiro.apply(valor) + " do banco.");
        java.util.function.BiConsumer<Integer, Integer> pagarBanco = (id, valor) ->
                register(defs, id, EffectType.PAGAR_AO_BANCO, valor,
                        "Pague " + dinheiro.apply(valor),
                        "Pague " + dinheiro.apply(valor) + " ao banco.");

        receberBanco.accept(1, 25);
        receberBanco.accept(2, 150);
        receberBanco.accept(3, 80);
        receberBanco.accept(4, 200);
        receberBanco.accept(5, 50);
        receberBanco.accept(6, 50);
        receberBanco.accept(7, 100);
        receberBanco.accept(8, 100);

        register(defs, 9, EffectType.SAIDA_LIVRE_DA_PRISAO, 0,
                "Saida livre da prisao",
                "Guarde esta carta para sair da prisao sem pagar quando precisar.");

        receberBanco.accept(10, 200);

        register(defs, 11, EffectType.RECEBER_DE_CADA_JOGADOR, 50,
                "Cobrar os outros jogadores",
                "Cada jogador ativo paga " + dinheiro.apply(50) + " a voce.");

        receberBanco.accept(12, 45);
        receberBanco.accept(13, 100);
        receberBanco.accept(14, 100);
        receberBanco.accept(15, 20);

        pagarBanco.accept(16, 15);
        pagarBanco.accept(17, 25);
        pagarBanco.accept(18, 45);
        pagarBanco.accept(19, 30);
        pagarBanco.accept(20, 100);
        pagarBanco.accept(21, 100);
        pagarBanco.accept(22, 40);

        register(defs, 23, EffectType.IR_PARA_PRISAO, 0,
                "Va diretamente para a prisao",
                "Desloque-se imediatamente para a prisao e encerre a vez.");

        pagarBanco.accept(24, 30);
        pagarBanco.accept(25, 50);
        pagarBanco.accept(26, 25);
        pagarBanco.accept(27, 30);
        pagarBanco.accept(28, 45);
        pagarBanco.accept(29, 50);
        pagarBanco.accept(30, 50);

        DEFINITIONS = Collections.unmodifiableMap(defs);
    }

    private static void register(Map<Integer, Definition> defs, int id, EffectType type, int valor, String titulo, String descricao) {
        defs.put(id, new Definition(id, type, valor, titulo, descricao));
    }

    static Definition get(int id) {
        return DEFINITIONS.get(id);
    }

    static Collection<Definition> all() {
        return DEFINITIONS.values();
    }

    static int total() {
        return DEFINITIONS.size();
    }
}
