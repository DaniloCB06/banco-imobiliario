package banco_imobiliario_models;

import java.util.ArrayList;
import java.util.List;

final class TabuleiroOficialFactory {
    private TabuleiroOficialFactory() {}

    static Tabuleiro criar() {
        List<Casa> casas = new ArrayList<>(40);

        // Aluguéis genéricos p/ iteração 1 (só cobra com ≥1 casa/hotel)
        final int[] ALUGUEIS = {0, 25, 75, 225, 400, 600};
        final int PRECO_TERRENO = 200;
        final int PRECO_CASA = 100;
        final int PRECO_HOTEL = 400;

        final int PRECO_COMPANHIA = 200;
        final int ALUGUEL_COMPANHIA = 200000;

        // ===== Lado inferior (0..9) – já estava certo =====
        casas.add(new Casa(0,  "Ponto de Partida", "PONTO_PARTIDA"));
        casas.add(criarPropriedade("Leblon", 1, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(new Casa(2,  "Sorte/Revés", "CARTA"));
        casas.add(criarPropriedade("Av. Presidente Vargas", 3, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarPropriedade("Av. Nossa Sra. de Copacabana", 4, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarCompanhia("Companhia de Trem", 5, PRECO_COMPANHIA, ALUGUEL_COMPANHIA));
        casas.add(criarPropriedade("Av. Brig. Faria Lima", 6, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarCompanhia("Companhia de Ônibus", 7, PRECO_COMPANHIA, ALUGUEL_COMPANHIA));
        casas.add(criarPropriedade("Av. Rebouças", 8, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarPropriedade("Av. 9 de Julho", 9, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));

        // ===== Canto inferior esquerdo =====
        casas.add(new Casa(10, "Prisão", "PRISAO"));

        // ===== Lado esquerdo (11..19) – corrigido =====
        casas.add(criarPropriedade("Av. Europa", 11, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(new Casa(12, "Sorte/Revés", "CARTA"));
        casas.add(criarPropriedade("Rua Augusta", 13, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarPropriedade("Av. Pacaembu", 14, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarCompanhia("Companhia de Táxi", 15, PRECO_COMPANHIA, ALUGUEL_COMPANHIA));
        casas.add(new Casa(16, "Sorte/Revés", "CARTA"));
        casas.add(criarPropriedade("Interlagos", 17, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(new Casa(18, "Lucros e Dividendos", "LUCRO", 200));
        casas.add(criarPropriedade("Morumbi", 19, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));

        // ===== Canto superior esquerdo =====
        casas.add(new Casa(20, "Parada Livre", "PARADA_LIVRE"));

        // ===== Topo (21..29) – corrigido =====
        casas.add(criarPropriedade("Flamengo", 21, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(new Casa(22, "Sorte/Revés", "CARTA"));
        casas.add(criarPropriedade("Botafogo", 23, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(new Casa(24, "Imposto de Renda", "IMPOSTO", 200)); // símbolo do dinheiro (vermelho)
        casas.add(criarCompanhia("Companhia Marítima", 25, PRECO_COMPANHIA, ALUGUEL_COMPANHIA));    // “navio”
        casas.add(criarPropriedade("Av. Brasil", 26, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(new Casa(27, "Sorte/Revés", "CARTA"));
        casas.add(criarPropriedade("Av. Paulista", 28, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarPropriedade("Jardim Europa", 29, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));

        // ===== Canto superior direito =====
        casas.add(new Casa(30, "Vá para a Prisão", "VA_PARA_PRISAO"));

        // ===== Lado direito (31..39) – manteve =====
        casas.add(criarPropriedade("Copacabana", 31, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarCompanhia("Companhia Aérea", 32, PRECO_COMPANHIA, ALUGUEL_COMPANHIA));
        casas.add(criarPropriedade("Av. Vieira Souto", 33, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarPropriedade("Av. Atlântica", 34, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarCompanhia("Companhia de Serviços", 35, PRECO_COMPANHIA, ALUGUEL_COMPANHIA));
        casas.add(criarPropriedade("Ipanema", 36, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(new Casa(37, "Sorte/Revés", "CARTA"));
        casas.add(criarPropriedade("Jardim Paulista", 38, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));
        casas.add(criarPropriedade("Brooklin", 39, PRECO_TERRENO, PRECO_CASA, PRECO_HOTEL, ALUGUEIS));

        return new Tabuleiro(casas);
    }

    private static Propriedade criarPropriedade(String nome, int posicao, int precoTerreno, int precoCasa, int precoHotel, int[] alugueis) {
        return new Propriedade(posicao, nome, precoTerreno, precoCasa, precoHotel, alugueis);
    }

    private static Companhia criarCompanhia(String nome, int posicao, int preco, int aluguel) {
        return new Companhia(posicao, nome, preco, aluguel);
    }
}
