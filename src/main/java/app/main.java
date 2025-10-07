package app;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import Banco_Imobiliario_Models.GameModel;

public class main {
	/**
		Esta classe tem como intuito de testes diretos e apenas isso.
		Qualquer outra função é e deve ser desconsiderada.
	*/
    public static void main(String[] args) throws Exception {
        GameModel game = new GameModel();
        boolean partidaIniciada = false;

        System.out.println("=== Banco Imobiliário – CLI de desenvolvimento ===");
        System.out.println("Iteração 1: lançamento virtual dos dados");
        printHelp();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String line = br.readLine();
            if (line == null) break;

            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            try {
                switch (cmd) {
                    case "nova": {
                        int numJog = (parts.length >= 2) ? Integer.parseInt(parts[1]) : 2;
                        Long seed = (parts.length >= 3) ? Long.parseLong(parts[2]) : null;
                        game = new GameModel();
                        game.novaPartida(numJog, seed);
                        partidaIniciada = true;
                        System.out.printf("Partida iniciada: %d jogadores | seed=%s%n",
                                numJog, (seed == null ? "(aleatória)" : seed));
                        break;
                    }
                    case "roll":
                    case "lancar": {
                        exigirPartida(partidaIniciada);
                        int vezes = (parts.length >= 2) ? Integer.parseInt(parts[1]) : 1;
                        for (int i = 0; i < vezes; i++) {
                            int[] r = game.lancarDados();
                            boolean dupla = game.houveDuplaNoUltimoLancamento();
                            int duplasSeguidas = game.getContagemDuplasConsecutivasDaVez();

                            System.out.printf(
                                "Lançamento #%d: d1=%d d2=%d %s (duplas seguidas=%d)%n",
                                i + 1, r[0], r[1], (dupla ? "-> DUPLA!" : ""), duplasSeguidas
                            );
                        }
                        break;
                    }
                    case "estado": {
                        exigirPartida(partidaIniciada);
                        System.out.printf(
                            "Jogador da vez: %d | Duplas seguidas: %d | Último foi dupla? %s%n",
                            game.getJogadorDaVez(),
                            game.getContagemDuplasConsecutivasDaVez(),
                            game.houveDuplaNoUltimoLancamento() ? "sim" : "não"
                        );
                        break;
                    }
                    case "help":
                    case "?":
                        printHelp();
                        break;
                    case "quit":
                    case "q":
                    case "exit":
                        System.out.println("Encerrando. 👋");
                        return;
                    default:
                        System.out.println("Comando desconhecido. Use 'help'.");
                }
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    private static void exigirPartida(boolean iniciada) {
        if (!iniciada) throw new IllegalStateException("Partida não iniciada. Use: nova <2-6> [seed]");
    }

    private static void printHelp() {
        System.out.println("Comandos:");
        System.out.println("  nova <2-6> [seed]  -> inicia nova partida (seed opcional para reprodutibilidade)");
        System.out.println("  roll [n]           -> lança os dados n vezes (padrão 1)");
        System.out.println("  estado             -> mostra informações do turno atual (Regra #1)");
        System.out.println("  help               -> mostra esta ajuda");
        System.out.println("  quit               -> sair");
    }
}
