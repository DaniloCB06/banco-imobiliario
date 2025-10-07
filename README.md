# Estrutura inicial do Projeto sugerida

 ```
banco-imobiliario/                               ← PASTA do projeto
├─ pom.xml                                       ← ARQUIVO
├─ src/                                          ← PASTA
│  ├─ main/java/                                 ← [Maven Source Root] (PASTA)
│  │  └─ banco_imobiliario_ID.banco_imobiliario.model/  ← PACOTE JAVA ÚNICO
│  │     ├─ GameModel.java                       ← ARQUIVO (única classe PUBLIC da iteração)
│  │     ├─ package-info.java                    ← ARQUIVO (documenta a regra do pacote)
│  │     ├─ Tabuleiro.java                       ← ARQUIVO (package-private)
│  │     ├─ Casa.java                            ← ARQUIVO (package-private)
│  │     ├─ Propriedade.java                     ← ARQUIVO (package-private)
│  │     ├─ Companhia.java                       ← ARQUIVO (package-private)
│  │     ├─ Carta.java                           ← ARQUIVO (package-private)
│  │     ├─ Baralho.java                         ← ARQUIVO (package-private)
│  │     ├─ Jogador.java                         ← ARQUIVO (package-private)
│  │     ├─ Banco.java                           ← ARQUIVO (package-private)
│  │     ├─ Dado.java                            ← ARQUIVO (package-private)
│  │     ├─ Turno.java                           ← ARQUIVO (package-private)
│  │     ├─ Prisao.java                          ← ARQUIVO (package-private)
│  │     ├─ Movimento.java                       ← ARQUIVO (package-private)
│  │     ├─ Aluguel.java                         ← ARQUIVO (package-private)
│  │     ├─ Construcao.java                      ← ARQUIVO (package-private)
│  │     ├─ Transacao.java                       ← ARQUIVO (package-private)
│  │     ├─ EventoDeJogo.java                    ← ARQUIVO (package-private)
│  │     ├─ TipoEvento.java                      ← ARQUIVO (package-private)
│  │     ├─ TabuleiroLoader.java                 ← ARQUIVO (package-private)
│  │     ├─ XlsxLoader.java                      ← ARQUIVO (package-private)
│  │     ├─ JsonLoader.java                      ← ARQUIVO (package-private)
│  │     ├─ YamlLoader.java                      ← ARQUIVO (package-private)
│  │     ├─ RandomProvider.java                  ← ARQUIVO (package-private)
│  │     └─ Preconditions.java                   ← ARQUIVO (package-private)
│  ├─ main/resources/                            ← [Maven Resources] (PASTA)
│  │  ├─ Tabuleiro-Valores.xlsx                  ← ARQUIVO (planilha oficial)
│  │  ├─ tabuleiro.json                          ← ARQUIVO (fallback)
│  │  └─ tabuleiro.yaml                          ← ARQUIVO (fallback)
│  └─ test/java/                                 ← [Maven Test Source Root] (PASTA)
│     └─ banco_imobiliario_ID.banco_imobiliario.model/  ← **MESMO PACOTE JAVA**
│        ├─ DadosTest.java                       ← ARQUIVO (JUnit 4)
│        ├─ MovimentoTest.java                   ← ARQUIVO
│        ├─ CompraTest.java                      ← ARQUIVO
│        ├─ ConstrucaoTest.java                  ← ARQUIVO
│        ├─ AluguelTest.java                     ← ARQUIVO
│        ├─ PrisaoTest.java                      ← ARQUIVO
│        ├─ FalenciaTest.java                    ← ARQUIVO
│        └─ LoaderTest.java                      ← ARQUIVO
 ```
