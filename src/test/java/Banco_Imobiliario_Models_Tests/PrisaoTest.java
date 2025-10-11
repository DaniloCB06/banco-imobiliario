package Banco_Imobiliario_Models_Tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import Banco_Imobiliario_Models.GameModel;

/**
 * Iteração 1 – Regra #6 (Prisão):
 *  - Entrada: "VÁ_PARA_PRISÃO" e 3ª dupla consecutiva.
 *  - Saída automática: dupla OU carta "Saída Livre da Prisão" (carta devolvida ao deck/stub).
 *  - Sem multa. Sem visitante.
 *
 * Observação:
 *  - Usa apenas a API pública do GameModel e seus helpers expostos.
 */
public class PrisaoTest {

    private GameModel game;

    @Before
    public void setUp() {
        game = new GameModel();
    }

    @Test
    public void deveIrParaPrisaoNaTerceiraDuplaConsecutiva() {
        // Tabuleiro com prisao e "vá para a prisão"
        final int N = 20, IDX_PRISAO = 5, IDX_VA_PARA_PRISAO = 12;
        game.novaPartida(2, 123L);
        game.carregarTabuleiroBasicoComPrisao(N, IDX_PRISAO, IDX_VA_PARA_PRISAO);

        // Procurar 3 duplas consecutivas; ao deslocar no 3º, deve ir direto à prisão
        int consecutivas = 0;
        int guardPosAntes = game.getPosicaoJogador(game.getJogadorDaVez());

        int tentativas = 0;
        while (consecutivas < 3 && tentativas++ < 10000) {
            GameModel.ResultadoDados r = game.lancarDados();
            if (r.isDupla()) {
                consecutivas++;
            } else {
                consecutivas = 0;
            }
            if (consecutivas < 3) {
                // nas duas primeiras duplas (ou quaisquer não-triplas), move normalmente
                game.deslocarPiao();
                guardPosAntes = game.getPosicaoJogador(game.getJogadorDaVez());
            }
        }
        assertTrue("Falha ao encontrar 3 duplas consecutivas em tempo hábil", consecutivas >= 3);

        // 3ª dupla: ao deslocar, não anda; vai direto para a prisão
        game.deslocarPiao();
        assertTrue("Jogador deveria estar na prisão após 3ª dupla consecutiva", game.estaNaPrisao(game.getJogadorDaVez()));
        assertEquals("Posição deve ser a casa da PRISÃO", IDX_PRISAO, game.getPosicaoJogador(game.getJogadorDaVez()));
    }

    @Test
    public void deveIrParaPrisaoAoCairNaCasaVaParaPrisao() {
        // Tabuleiro mínimo que facilita atingir VA_PARA_PRISAO com qualquer soma ≡ 1 (mod 3)
        final int N = 3, IDX_PRISAO = 2, IDX_VA_PARA_PRISAO = 1;
        boolean conseguiu = false;

        // Tentamos algumas seeds diferentes para obter rapidamente um lançamento com soma %3==1 sem acumular 3 duplas
        for (long seed = 10; seed < 100 && !conseguiu; seed++) {
            game = new GameModel();
            game.novaPartida(2, seed);
            game.carregarTabuleiroBasicoComPrisao(N, IDX_PRISAO, IDX_VA_PARA_PRISAO);
            // começar na posição 0
            int protecao = 0;
            while (protecao++ < 200) {
                GameModel.ResultadoDados r = game.lancarDados();
                // Evita chegar a 3 duplas antes de testar a casa "VA_PARA_PRISAO"
                if (game.getContagemDuplasConsecutivasDaVez() >= 2 && r.isDupla()) {
                    break; // troca de seed
                }
                // Precisamos que (0 + soma) % 3 == 1 para cair em VA_PARA_PRISAO
                if ((r.getSoma() % N) == 1 && !r.isDupla()) {
                    game.deslocarPiao();
                    assertTrue("Deveria ter ido para a prisão ao cair em VA_PARA_PRISAO", game.estaNaPrisao(game.getJogadorDaVez()));
                    assertEquals("Posição deve ser a PRISÃO", IDX_PRISAO, game.getPosicaoJogador(game.getJogadorDaVez()));
                    conseguiu = true;
                    break;
                }
                // Caso não seja a soma desejada, não desloca; tenta novo lançamento
            }
        }
        assertTrue("Não foi possível validar a entrada por VA_PARA_PRISAO nas tentativas", conseguiu);
    }

    @Test
    public void presoComCartaSaiAutomaticamenteSemPrecisarDeDupla() {
        final int N = 12, IDX_PRISAO = 5, IDX_VA_PARA_PRISAO = 8;
        game.novaPartida(2, 42L);
        game.carregarTabuleiroBasicoComPrisao(N, IDX_PRISAO, IDX_VA_PARA_PRISAO);

        // Envia para a prisão
        int id = game.getJogadorDaVez();
        game.enviarParaPrisao(id);
        assertTrue(game.estaNaPrisao(id));

        // Dá a carta de saída livre (helper público)
        game.debugDarCartaSaidaLivreAoJogador(id, true);

        // Qualquer lançamento serve; a saída é automática por carta
        GameModel.ResultadoDados r = game.lancarDados();
        int soma = r.getSoma();
        game.deslocarPiao();

        assertFalse("Deveria ter saído automaticamente da prisão usando a carta", game.estaNaPrisao(id));
        // Como sai e move normalmente, a posição deve avançar a partir da prisão
        int esperado = (IDX_PRISAO + soma) % N;
        assertEquals("Após sair com a carta, deveria deslocar normalmente", esperado, game.getPosicaoJogador(id));
    }

    @Test
    public void presoSaiAutomaticamenteSeTirarDupla() {
        final int N = 10, IDX_PRISAO = 6, IDX_VA_PARA_PRISAO = 3;
        game.novaPartida(2, 7L);
        game.carregarTabuleiroBasicoComPrisao(N, IDX_PRISAO, IDX_VA_PARA_PRISAO);

        int id = game.getJogadorDaVez();
        game.enviarParaPrisao(id);
        assertTrue(game.estaNaPrisao(id));

        // Itera até obter uma dupla
        int tentativas = 0;
        GameModel.ResultadoDados ultimo = null;
        do {
            ultimo = game.lancarDados();
        } while (!ultimo.isDupla() && ++tentativas < 10000);

        assertTrue("Não foi possível obter dupla para sair da prisão", ultimo != null && ultimo.isDupla());

        int soma = ultimo.getSoma();
        game.deslocarPiao();

        assertFalse("Deveria ter saído da prisão ao tirar dupla", game.estaNaPrisao(id));
        int esperado = (IDX_PRISAO + soma) % N;
        assertEquals("Após sair por dupla, deveria deslocar normalmente", esperado, game.getPosicaoJogador(id));
    }

    @Test
    public void presoPermaneceSeNaoForDuplaENaoTiverCarta() {
        final int N = 9, IDX_PRISAO = 4, IDX_VA_PARA_PRISAO = 7;
        game.novaPartida(2, 99L);
        game.carregarTabuleiroBasicoComPrisao(N, IDX_PRISAO, IDX_VA_PARA_PRISAO);

        int id = game.getJogadorDaVez();
        game.enviarParaPrisao(id);
        assertTrue(game.estaNaPrisao(id));

        // Garantir um lançamento que NÃO seja dupla
        GameModel.ResultadoDados r;
        int protecao = 0;
        do {
            r = game.lancarDados();
        } while (r.isDupla() && ++protecao < 10000);

        int posAntes = game.getPosicaoJogador(id);
        game.deslocarPiao();

        // Continua preso e posição não muda
        assertTrue("Deveria permanecer na prisão sem dupla e sem carta", game.estaNaPrisao(id));
        assertEquals("Não deveria mover quando não sai da prisão", posAntes, game.getPosicaoJogador(id));
    }
}
