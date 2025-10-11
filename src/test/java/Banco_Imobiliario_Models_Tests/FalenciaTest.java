package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;
import Banco_Imobiliario_Models.Transacao;

/**
 * Iteração 1 – Regra #7:
 * - Pagar aluguel (item 5) pode deixar o jogador negativo;
 * - Caso não haja ativos suficientes, declarar falência e removê-lo do jogo;
 * - Venda explícita ao banco por 90% do valor agregado (terreno + construções).
 *
 * Observações:
 *  - Usa unicamente a API pública do Model (GameModel) + helpers públicos.
 */
public class FalenciaTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        // 2 jogadores e seed fixa para reprodutibilidade
        game.novaPartida(2, 42L);
    }

    @Test
    public void deveDeclararFalenciaQuandoSaldoFicaNegativoSemAtivos() {
        // Tabuleiro com UMA propriedade (pos 3). Aluguéis: index 1 (1 casa) = 5000.
        int idxProp = 3;
        game.carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(
                10, idxProp,
                /*precoTerreno*/ 1000,
                /*precoCasa   */ 200,
                /*precoHotel  */ 1000,
                /*alugueis    */ new int[]{0, 5000, 0, 0, 0}
        );

        // Propriedade pertence ao Jogador 1 e possui 1 casa (cobrança válida na iteração 1)
        game.debugForcarDonoECasasDaPropriedade(idxProp, /*idDono*/ 1, /*numCasas*/ 1, /*hotel*/ false);

        // Jogador 0 "cai" na propriedade do adversário
        game.debugForcarPosicaoJogador(0, idxProp);

        // Paga automaticamente o aluguel de 5000 => jogador 0 fica com -1000
        Transacao t = game.pagarAluguelSeDevido();
        assertTrue(t.isEfetuada());
        assertEquals(5000, t.getValor());
        assertEquals(0, t.getPagadorId());
        assertEquals(Integer.valueOf(1), t.getRecebedorId());

        assertEquals(-1000, game.getSaldoJogador(0));
        assertEquals(9000, game.getSaldoJogador(1));

        // Não possui propriedades para liquidar: deve falir e sair do jogo
        boolean falido = game.declararFalenciaSeNecessario();
        assertTrue("Jogador deveria ter sido declarado falido", falido);
        assertFalse("Jogador não deveria continuar ativo", game.isJogadorAtivo(0));
    }

    @Test
    public void deveVenderPropriedadeAoBancoPor90PorCentoEResetarEstado() {
        // Tabuleiro com UMA propriedade na pos 2 (preços conhecidos)
        int idxProp = 2;
        int precoTerreno = 1000;
        int precoCasa    = 200;
        int precoHotel   = 800;

        game.carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(
                8, idxProp, precoTerreno, precoCasa, precoHotel,
                new int[]{0, 0, 0, 0, 0}
        );

        int saldoBancoInicial = game.getSaldoBanco();
        int saldoJ0Inicial    = game.getSaldoJogador(0);

        // Jogador 0 “cai” na propriedade e compra o terreno
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue("Compra do terreno deveria ser possível", game.comprarPropriedade());

        // ----- 1ª casa: precisa ser em QUEDA SUBSEQUENTE -----
        game.lancarDados();        // cria nova jogada/queda
        game.deslocarPiao();
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue("1ª casa deveria ser construída", game.construirCasa());

        // ----- 2ª casa: outra queda subsequente -----
        game.lancarDados();
        game.deslocarPiao();
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue("2ª casa deveria ser construída", game.construirCasa());

        // ----- Hotel: outra queda subsequente (requer >= 1 casa) -----
        game.lancarDados();
        game.deslocarPiao();
        game.debugForcarPosicaoJogador(0, idxProp);
        assertTrue("Hotel deveria ser construído", game.construirHotel());

        // Valor agregado atual: terreno + 2 casas + hotel
        int valorAgregado = precoTerreno + 2 * precoCasa + precoHotel; // 2200
        int esperadoPagamentoBanco = (valorAgregado * 9) / 10;         // 1980

        // Vende ao banco por 90%
        boolean vendeu = game.venderPropriedadeAoBanco(idxProp);
        assertTrue("Venda ao banco deveria ocorrer", vendeu);

        // Saldo do jogador: inicial - compras + venda(90%)
        int gastoCompras = precoTerreno + 2 * precoCasa + precoHotel; // 2200
        int saldoEsperadoJ0 = saldoJ0Inicial - gastoCompras + esperadoPagamentoBanco;
        assertEquals(saldoEsperadoJ0, game.getSaldoJogador(0));

        // Saldo do banco: inicial + compras - pagamento de 90%
        int saldoEsperadoBanco = saldoBancoInicial + gastoCompras - esperadoPagamentoBanco;
        assertEquals(saldoEsperadoBanco, game.getSaldoBanco());

        // Propriedade voltou ao banco (sem dono): nova venda deve falhar
        assertFalse("Não deveria vender novamente (não é mais dono)", game.venderPropriedadeAoBanco(idxProp));
    }
}
