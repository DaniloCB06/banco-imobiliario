package banco_imobiliario_models_tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import banco_imobiliario_models.GameModel;
import banco_imobiliario_models.Transacao;

/**
 * Iteração 1 – Regra #7 (Liquidação e Falência):
 * - Aluguel pode deixar o jogador negativo (regra antiga) — AGORA: paga tudo que tiver e falência imediata;
 * - Sem ativos suficientes: declara falência e sai do jogo;
 * - Venda explícita ao banco por 90% do valor agregado (terreno + construções).
 */
public class FalenciaTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        game.novaPartida(2, 42L);
    }

    @Test
    public void devePagarTudoQueTemEAoFinalFalirQuandoNaoConsegueCobrirAluguelSemAtivos() {
        // Tabuleiro com UMA propriedade na pos 3 e aluguel de 5.000 quando há ≥1 casa
        int idxProp = 3;
        game.carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(
            10, idxProp,
            1000, 200, 1000,
            new int[]{0, 5000, 0, 0, 0}
        );

        // Propriedade do jogador 1 com 1 casa (aluguel ativo)
        game.debugForcarDonoECasasDaPropriedade(idxProp, 1, 1, false);

        // Jogador 0 cai na propriedade do jogador 1
        game.debugForcarPosicaoJogador(0, idxProp);

        // Saldo inicial do pagador é 4.000 (regra da partida)
        assertEquals(4000, game.getSaldoJogador(0));
        assertEquals(4000, game.getSaldoJogador(1));

        // Com a nova regra, se não consegue cobrir, paga TUDO que tem (4.000) e FALI imediatamente
        Transacao t = game.pagarAluguelSeDevido();
        assertTrue("Transação de aluguel deve ter sido efetuada (mesmo parcial).", t.isEfetuada());
        assertEquals("Valor pago deve ser TODO o caixa disponível do pagador.", 4000, t.getValor());
        assertEquals(0, t.getPagadorId());
        assertEquals(Integer.valueOf(1), t.getRecebedorId());

        // Pagador ficou zerado e foi removido do jogo (falência imediata)
        assertEquals(0, game.getSaldoJogador(0));
        assertFalse("Jogador 0 deve estar inativo (falido).", game.isJogadorAtivo(0));

        // Dono recebeu 4.000 (saldo inicial 4.000 + 4.000 recebidos)
        assertEquals(8000, game.getSaldoJogador(1));

        // Chamar declararFalenciaSeNecessario() após isso retorna true, mas o jogador já está inativo
        assertTrue("Se invocado depois, ainda assim indica estado de falência já consolidado.",
                   game.declararFalenciaSeNecessario());
    }

    @Test
    public void deveVenderPropriedadeAoBancoPor90PorCentoEResetarEstado() {
        // Tabuleiro com UMA propriedade na pos 2 (preços conhecidos)
        int idxProp = 2;
        int precoTerreno = 1000;
        int precoCasa    = 200;
        int precoHotel   = 800;

        game.carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(
            8, idxProp, precoTerreno, precoCasa, precoHotel, new int[]{0, 0, 0, 0, 0}
        );

        int saldoBancoInicial = game.getSaldoBanco();
        int saldoJ0Inicial    = game.getSaldoJogador(0);

        // Jogador 0 "cai" na propriedade e COMPRA o terreno
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue("Compra do terreno deveria ser possível", game.comprarPropriedade());

        // Simula quedas subsequentes para permitir 1 construção por queda
        game.lancarDados(); game.deslocarPiao();
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue("1ª casa deveria ser construída", game.construirCasa());

        game.lancarDados(); game.deslocarPiao();
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue("2ª casa deveria ser construída", game.construirCasa());

        game.lancarDados(); game.deslocarPiao();
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue("Hotel deveria ser construído (com ≥1 casa)", game.construirHotel());

        int valorAgregado = precoTerreno + 2 * precoCasa + precoHotel; // 2200
        int esperadoPagamentoBanco = (valorAgregado * 9) / 10;         // 1980

        assertTrue("Venda ao banco por 90% deve ocorrer", game.venderPropriedadeAoBanco(idxProp));

        int gastoCompras     = valorAgregado;
        int saldoEsperadoJ0  = saldoJ0Inicial - gastoCompras + esperadoPagamentoBanco;
        assertEquals("Saldo do jogador 0 após vender por 90%", saldoEsperadoJ0, game.getSaldoJogador(0));

        int saldoEsperadoBanco = saldoBancoInicial + gastoCompras - esperadoPagamentoBanco;
        assertEquals("Saldo do banco após pagar 90%", saldoEsperadoBanco, game.getSaldoBanco());

        // Propriedade voltou ao banco; não deve permitir nova venda pelo jogador 0
        assertFalse("Não deve conseguir vender novamente (não é mais dono).",
                    game.venderPropriedadeAoBanco(idxProp));
    }
}
