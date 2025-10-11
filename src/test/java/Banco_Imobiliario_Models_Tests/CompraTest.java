package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;

public class CompraTest {

    /**
     * Deve comprar com sucesso quando:
     * - o jogador cair em uma Propriedade sem dono
     * - houver saldo suficiente
     * - pagamento é automático (não verificamos o banco/saldo diretamente; validamos pelo retorno e por não permitir segunda compra)
     */
    @Test
    public void deveComprarPropriedadeSemDonoComSaldoSuficiente() {
        GameModel game = new GameModel();
        game.novaPartida(2, 123L); // seed para reprodutibilidade

        // 1) Rola os dados para descobrir o destino da primeira jogada
        GameModel.ResultadoDados rd = game.lancarDados();
        int destino = rd.getSoma();
        int nCasas = 40;

        // 2) Monta o tabuleiro de teste com UMA propriedade exatamente no destino
        game.carregarTabuleiroDeTesteComUmaPropriedade(nCasas, destino, /*preço terreno*/ 300);

        // 3) Move o peão para cair na propriedade
        game.deslocarPiao();
        assertEquals(destino % nCasas, game.getPosicaoJogador(game.getJogadorDaVez()));

        // 4) Compra deve ser permitida
        boolean comprado = game.comprarPropriedade();
        assertTrue("Era para conseguir comprar a propriedade sem dono com saldo suficiente.", comprado);

        // 5) Nova tentativa de compra na mesma casa deve falhar (já tem dono)
        assertFalse("Não deve permitir comprar novamente a mesma propriedade.", game.comprarPropriedade());
    }

    /**
     * Não deve permitir compra se a casa atual não for uma Propriedade.
     */
    @Test
    public void naoDeveComprarSeCasaNaoForPropriedade() {
        GameModel game = new GameModel();
        game.novaPartida(2, 321L);

        GameModel.ResultadoDados rd = game.lancarDados();
        int destino = rd.getSoma();
        int nCasas = 20;

        // Coloca a propriedade em OUTRA posição (não no destino)
        int idxPropriedade = (destino + 1) % nCasas;
        game.carregarTabuleiroDeTesteComUmaPropriedade(nCasas, idxPropriedade, 300);

        game.deslocarPiao();
        assertEquals(destino % nCasas, game.getPosicaoJogador(game.getJogadorDaVez()));

        // Como a casa atual não é Propriedade, compra deve falhar
        assertFalse("Não deve permitir comprar quando a casa atual não é propriedade.",
                game.comprarPropriedade());
    }

    /**
     * Não deve permitir compra quando o jogador não tem saldo suficiente.
     * Jogador começa com 4000; definimos preço do terreno maior que 4000.
     */
    @Test
    public void naoDeveComprarSemSaldoSuficiente() {
        GameModel game = new GameModel();
        game.novaPartida(2, 999L);

        GameModel.ResultadoDados rd = game.lancarDados();
        int destino = rd.getSoma();
        int nCasas = 30;

        // Preço acima do saldo inicial do jogador (4000)
        int precoInviavel = 5000;
        game.carregarTabuleiroDeTesteComUmaPropriedade(nCasas, destino, precoInviavel);

        game.deslocarPiao();
        assertEquals(destino % nCasas, game.getPosicaoJogador(game.getJogadorDaVez()));

        // Compra deve falhar por saldo insuficiente
        assertFalse("Não deve permitir comprar sem saldo suficiente.", game.comprarPropriedade());
    }
}
