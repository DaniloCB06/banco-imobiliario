package banco_imobiliario_models;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

final class TabuleiroOficialFactory {
    private TabuleiroOficialFactory() {}

    static Tabuleiro criar() {
        List<Casa> casas = new ArrayList<>(40);

        // Aluguéis genéricos p/ iteração 1 (só cobra com ≥1 casa/hotel)
        final int[] ALUGUEIS = {0, 25, 75, 225, 400, 600};
        BiFunction<String,Integer,Propriedade> prop = (nome, pos) ->
            new Propriedade(pos, nome, /*preço terreno*/200, /*casa*/100, /*hotel*/400, ALUGUEIS);

        final int PRECO_COMPANHIA = 200;
        final int ALUGUEL_COMPANHIA = 200;
        BiFunction<String,Integer,Companhia> companhia = (nome, pos) ->
            new Companhia(pos, nome, PRECO_COMPANHIA, ALUGUEL_COMPANHIA);

        // ===== Lado inferior (0..9) – já estava certo =====
        casas.add(new Casa(0,  "Ponto de Partida", "PONTO_PARTIDA"));
        casas.add(prop.apply("Leblon", 1));
        casas.add(new Casa(2,  "Sorte/Revés", "CARTA"));
        casas.add(prop.apply("Av. Presidente Vargas", 3));
        casas.add(prop.apply("Av. Nossa Sra. de Copacabana", 4));
        casas.add(companhia.apply("Companhia de Trem", 5));
        casas.add(prop.apply("Av. Brig. Faria Lima", 6));
        casas.add(companhia.apply("Companhia de Ônibus", 7));
        casas.add(prop.apply("Av. Rebouças", 8));
        casas.add(prop.apply("Av. 9 de Julho", 9));

        // ===== Canto inferior esquerdo =====
        casas.add(new Casa(10, "Prisão", "PRISAO"));

        // ===== Lado esquerdo (11..19) – corrigido =====
        casas.add(prop.apply("Av. Europa", 11));
        casas.add(new Casa(12, "Sorte/Revés", "CARTA"));
        casas.add(prop.apply("Rua Augusta", 13));
        casas.add(prop.apply("Av. Pacaembu", 14));
        casas.add(companhia.apply("Companhia de Táxi", 15));
        casas.add(new Casa(16, "Sorte/Revés", "CARTA"));
        casas.add(prop.apply("Interlagos", 17));
        casas.add(new Casa(18, "Lucros e Dividendos", "LUCRO", 200));
        casas.add(prop.apply("Morumbi", 19));

        // ===== Canto superior esquerdo =====
        casas.add(new Casa(20, "Parada Livre", "PARADA_LIVRE"));

        // ===== Topo (21..29) – corrigido =====
        casas.add(prop.apply("Flamengo", 21));
        casas.add(new Casa(22, "Sorte/Revés", "CARTA"));
        casas.add(prop.apply("Botafogo", 23));
        casas.add(new Casa(24, "Imposto de Renda", "IMPOSTO", 200)); // símbolo do dinheiro (vermelho)
        casas.add(companhia.apply("Companhia Marítima", 25));    // “navio”
        casas.add(prop.apply("Av. Brasil", 26));
        casas.add(new Casa(27, "Sorte/Revés", "CARTA"));
        casas.add(prop.apply("Av. Paulista", 28));
        casas.add(prop.apply("Jardim Europa", 29));

        // ===== Canto superior direito =====
        casas.add(new Casa(30, "Vá para a Prisão", "VA_PARA_PRISAO"));

        // ===== Lado direito (31..39) – manteve =====
        casas.add(prop.apply("Copacabana", 31));
        casas.add(companhia.apply("Companhia Aérea", 32));
        casas.add(prop.apply("Av. Vieira Souto", 33));
        casas.add(prop.apply("Av. Atlântica", 34));
        casas.add(companhia.apply("Companhia de Serviços", 35));
        casas.add(prop.apply("Ipanema", 36));
        casas.add(new Casa(37, "Sorte/Revés", "CARTA"));
        casas.add(prop.apply("Jardim Paulista", 38));
        casas.add(prop.apply("Brooklin", 39));

        return new Tabuleiro(casas);
    }
}
