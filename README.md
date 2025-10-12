# Estrutura inicial do Projeto sugerida

## Primeira Iteração

 ```
banco-imobiliario/                               ← PASTA do projeto
├─ pom.xml                                       ← ARQUIVO
├─ README.md                                     ← ARQUIVO
├─ src/                                          ← PASTA
│  ├─ main/java/                                 ← [Maven Source Root] (PASTA)
│  │  └─ Banco_Imobiliario_Models/               ← PACOTE JAVA
│  │     ├─ Banco.java                           ← ARQUIVO
│  │     ├─ Casa.java                            ← ARQUIVO
│  │     ├─ Dado.java                            ← ARQUIVO
│  │     ├─ GameModel.java                       ← ARQUIVO
│  │     ├─ Jogador.java                         ← ARQUIVO
│  │     ├─ Movimento.java                       ← ARQUIVO
│  │     ├─ Propriedade.java                     ← ARQUIVO
│  │     ├─ RandomProvider.java                  ← ARQUIVO
│  │     ├─ ResultadoMovimento.java              ← ARQUIVO
│  │     ├─ Tabuleiro.java                       ← ARQUIVO
│  │     ├─ Transacao.java                       ← ARQUIVO
│  │     └─ Turno.java                           ← ARQUIVO
│  └─ test/java/                                 ← [Maven Test Source Root] (PASTA)
│     └─ Banco_Imobiliario_Models_Tests/         ← PACOTE JAVA
│        ├─ AluguelTest.java                     ← ARQUIVO
│        ├─ CompraTest.java                      ← ARQUIVO
│        ├─ ConstrucaoTest.java                  ← ARQUIVO
│        ├─ DadosTest.java                       ← ARQUIVO
│        ├─ FalenciaTest.java                    ← ARQUIVO
│        ├─ MovimentoTest.java                   ← ARQUIVO
│        └─ PrisaoTest.java                      ← ARQUIVO
└─ target/                                       ← PASTA (build gerado pelo Maven)

 ```
