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

        registrarReceberBanco(defs, 1, 25);
        registrarReceberBanco(defs, 2, 150);
        registrarReceberBanco(defs, 3, 80);
        registrarReceberBanco(defs, 4, 200);
        registrarReceberBanco(defs, 5, 50);
        registrarReceberBanco(defs, 6, 50);
        registrarReceberBanco(defs, 7, 100);
        registrarReceberBanco(defs, 8, 100);

        register(defs, 9, EffectType.SAIDA_LIVRE_DA_PRISAO, 0,
                "Saida livre da prisao",
                "Guarde esta carta para sair da prisao sem pagar quando precisar.");

        registrarReceberBanco(defs, 10, 200);

        register(defs, 11, EffectType.RECEBER_DE_CADA_JOGADOR, 50,
                "Cobrar os outros jogadores",
            "Cada jogador ativo paga " + formatarDinheiro(50) + " a voce.");

        registrarReceberBanco(defs, 12, 45);
        registrarReceberBanco(defs, 13, 100);
        registrarReceberBanco(defs, 14, 100);
        registrarReceberBanco(defs, 15, 20);

        registrarPagarBanco(defs, 16, 15);
        registrarPagarBanco(defs, 17, 25);
        registrarPagarBanco(defs, 18, 45);
        registrarPagarBanco(defs, 19, 30);
        registrarPagarBanco(defs, 20, 100);
        registrarPagarBanco(defs, 21, 100);
        registrarPagarBanco(defs, 22, 40);

        register(defs, 23, EffectType.IR_PARA_PRISAO, 0,
                "Va diretamente para a prisao",
                "Desloque-se imediatamente para a prisao e encerre a vez.");

        registrarPagarBanco(defs, 24, 30);
        registrarPagarBanco(defs, 25, 50);
        registrarPagarBanco(defs, 26, 25);
        registrarPagarBanco(defs, 27, 30);
        registrarPagarBanco(defs, 28, 45);
        registrarPagarBanco(defs, 29, 50);
        registrarPagarBanco(defs, 30, 50);

        DEFINITIONS = Collections.unmodifiableMap(defs);
    }

    private static void register(Map<Integer, Definition> defs, int id, EffectType type, int valor, String titulo, String descricao) {
        defs.put(id, new Definition(id, type, valor, titulo, descricao));
    }

    private static void registrarReceberBanco(Map<Integer, Definition> defs, int id, int valor) {
        String texto = formatarDinheiro(valor);
        register(defs, id, EffectType.RECEBER_DO_BANCO, valor,
                "Receba " + texto,
                "Receba " + texto + " do banco.");
    }

    private static void registrarPagarBanco(Map<Integer, Definition> defs, int id, int valor) {
        String texto = formatarDinheiro(valor);
        register(defs, id, EffectType.PAGAR_AO_BANCO, valor,
                "Pague " + texto,
                "Pague " + texto + " ao banco.");
    }

    private static String formatarDinheiro(int valor) {
        return String.format(Locale.ROOT, "R$ %d", valor);
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
