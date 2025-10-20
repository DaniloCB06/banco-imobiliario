package banco_imobiliario_models_tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import banco_imobiliario_models.GameModel;

/**
 * Iteração 1 – Regra #4 (Construções):
 * - Na 1ª queda pode comprar; não construir na mesma queda;
 * - Em quedas subsequentes: no máximo 1 construção por queda;
 * - Limites: 0–4 casas e 1 hotel; hotel requer ≥1 casa.
 */

public class ConstrucaoTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
        game.novaPartida(2, 42L);
        game.carregarTabuleiroDeTesteComUmaPropriedade(1, 0, 300, 120, 700);
    }

    @Test
    public void compraNaPrimeiraQuedaEProibeConstruirNaMesmaQueda() {
        game.lancarDados();
        game.deslocarPiao();

        int sJog = game.getSaldoJogador(game.getJogadorDaVez());
        int sBco = game.getSaldoBanco();

        assertTrue(game.comprarPropriedade());
        assertEquals(sJog - 300, game.getSaldoJogador(game.getJogadorDaVez()));
        assertEquals(sBco + 300, game.getSaldoBanco());

        assertFalse(game.construirCasa());
        assertFalse(game.construirHotel());
    }

    @Test
    public void apenasUmaConstrucaoPorQueda() {
        game.lancarDados(); game.deslocarPiao(); game.comprarPropriedade();

        game.lancarDados(); game.deslocarPiao();
        assertTrue(game.construirCasa());
        assertFalse(game.construirCasa());
        assertFalse(game.construirHotel());
    }

    @Test
    public void limiteQuatroCasas() {
        game.lancarDados(); game.deslocarPiao(); game.comprarPropriedade();

        for (int i = 0; i < 4; i++) {
            game.lancarDados(); game.deslocarPiao();
            assertTrue("Casa #" + (i + 1) + " deveria ser permitida", game.construirCasa());
        }

        game.lancarDados(); game.deslocarPiao();
        assertFalse("Quinta casa não é permitida", game.construirCasa());
    }

    @Test
    public void hotelRequerPeloMenosUmaCasaEUmaConstrucaoPorQueda() {
        game.lancarDados(); game.deslocarPiao(); game.comprarPropriedade();

        game.lancarDados(); game.deslocarPiao();
        assertFalse(game.construirHotel());
        assertTrue(game.construirCasa());
        assertFalse(game.construirHotel());

        game.lancarDados(); game.deslocarPiao();
        assertTrue(game.construirHotel());

        game.lancarDados(); game.deslocarPiao();
        assertFalse(game.construirHotel());
    }
}
