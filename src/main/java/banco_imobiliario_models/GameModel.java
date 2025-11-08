package banco_imobiliario_models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * API pública do Model (Iteração 1).
 *
 * Regras cobertas: #1, #2, #3, #4, #5, #6, #7.
 */
public class GameModel {

	// =====================================================================================
	// ESTADO GLOBAL
	// =====================================================================================

	private RandomProvider rng;
	private final Dado dado1 = new Dado();
	private final Dado dado2 = new Dado();
	private Turno turno;
	private Banco banco;
	private final List<Jogador> jogadores = new ArrayList<>();
	private Tabuleiro tabuleiro;

	// Lançamento corrente
	private Integer ultimoD1 = null, ultimoD2 = null;

	// Contexto da queda (para permitir construir na casa em que caiu)
	private Integer posicaoDaQuedaAtual = null;
	private boolean jaConstruiuNestaQueda = false;
	private boolean acabouDeComprarNestaQueda = false;

	// Sinalização de 3ª dupla consecutiva
	private boolean deveIrParaPrisaoPorTerceiraDupla = false;

	/** DTO imutável para resultado dos dados. */
	public static final class ResultadoDados {
		private final int d1, d2;

		public ResultadoDados(int d1, int d2) {
			this.d1 = d1;
			this.d2 = d2;
		}

		public int getD1() {
			return d1;
		}

		public int getD2() {
			return d2;
		}

		public int getSoma() {
			return d1 + d2;
		}

		public boolean isDupla() {
			return d1 == d2;
		}
	}

	// =====================================================================================
	// SETUP
	// =====================================================================================

	public void novaPartida(int numJogadores, Long seedOpcional) {
		if (numJogadores < 2 || numJogadores > 6) {
			throw new IllegalArgumentException("Número de jogadores deve estar entre 2 e 6.");
		}
		this.rng = new RandomProvider(seedOpcional);
		this.turno = new Turno(numJogadores); // << usa ordem padrão 0..N-1
		this.banco = new Banco(200_000);

		this.jogadores.clear();
		for (int i = 0; i < numJogadores; i++) {
			this.jogadores.add(new Jogador(i, 4000));
		}

		this.ultimoD1 = this.ultimoD2 = null;
		this.deveIrParaPrisaoPorTerceiraDupla = false;
		limparContextoDeQueda();
		notifyObservers();
	}

	public void setTabuleiro(Tabuleiro tabuleiro) {
		if (tabuleiro == null || tabuleiro.tamanho() <= 0) {
			throw new IllegalArgumentException("Tabuleiro inválido.");
		}
		this.tabuleiro = tabuleiro;
	}

	private void exigirPartidaIniciada() {
		if (rng == null || turno == null || jogadores.isEmpty()) {
			throw new IllegalStateException("Partida não iniciada. Chame novaPartida().");
		}
	}

	private void exigirTabuleiroCarregado() {
		if (this.tabuleiro == null) {
			throw new IllegalStateException("Tabuleiro não carregado.");
		}
	}

	private void limparContextoDeQueda() {
		this.posicaoDaQuedaAtual = null;
		this.jaConstruiuNestaQueda = false;
		this.acabouDeComprarNestaQueda = false;
	}

	private void iniciarContextoDeQueda(int pos) {
		this.posicaoDaQuedaAtual = pos;
		this.jaConstruiuNestaQueda = false;
		this.acabouDeComprarNestaQueda = false;
	}

	// =====================================================================================
	// REGRA #1 — LANÇAR DADOS
	// =====================================================================================

	public ResultadoDados lancarDados() {
		exigirPartidaIniciada();
		int d1 = dado1.rolar(rng);
		int d2 = dado2.rolar(rng);
		turno.registrarLance(d1, d2);
		this.ultimoD1 = d1;
		this.ultimoD2 = d2;

		if (turno.houveDupla() && turno.getDuplasConsecutivas() >= 3) {
			this.deveIrParaPrisaoPorTerceiraDupla = true;
		}
		notifyObservers();
		return new ResultadoDados(d1, d2);
	}

	/** NOVO: para testes – força os valores dos dados (1..6) sem random. */
	public ResultadoDados lancarDadosForcado(int d1, int d2) {
		exigirPartidaIniciada();
		if (d1 < 1 || d1 > 6 || d2 < 1 || d2 > 6) {
			throw new IllegalArgumentException("Valores dos dados devem estar entre 1 e 6.");
		}
		turno.registrarLance(d1, d2);
		this.ultimoD1 = d1;
		this.ultimoD2 = d2;

		if (turno.houveDupla() && turno.getDuplasConsecutivas() >= 3) {
			this.deveIrParaPrisaoPorTerceiraDupla = true;
		}
		return new ResultadoDados(d1, d2);
	}

	public boolean houveDuplaNoUltimoLancamento() {
		exigirPartidaIniciada();
		return turno.houveDupla();
	}

	public int getContagemDuplasConsecutivasDaVez() {
		exigirPartidaIniciada();
		return turno.getDuplasConsecutivas();
	}

	public int getJogadorDaVez() {
		exigirPartidaIniciada();
		return turno.getJogadorDaVez(); // << já segue a ordem definida
	}

	/**
	 * NOVO: finaliza a vez – só avança se NÃO houver dupla. Caso de 3ª dupla: o
	 * deslocamento envia à prisão e reseta a contagem, logo aqui a vez avança
	 * normalmente.
	 */
	public void encerrarAcoesDaVezEPassarTurno() {
		exigirPartidaIniciada();
		boolean dupla = turno.houveDupla();
		int consecutivas = turno.getDuplasConsecutivas();
		if (dupla && consecutivas > 0) {
			// dupla comum -> mesma pessoa continua
			return;
		}
		// sem dupla (ou 3ª dupla já resolvida) -> próximo jogador da ORDEM
		turno.passarVez(); // << agora sem parâmetro
		limparContextoDeQueda();
		this.ultimoD1 = this.ultimoD2 = null;
	}

	// =====================================================================================
	// REGRA #2 (PARCIAL) — DESLOCAMENTO + REGRA #6 — PRISÃO
	// =====================================================================================

	public ResultadoMovimento deslocarPiao() {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();
		if (ultimoD1 == null || ultimoD2 == null) {
			throw new IllegalStateException("É necessário lançar os dados antes de deslocar.");
		}

		final int id = getJogadorDaVez();
		final Jogador j = jogadores.get(id);

		// Terceira dupla: vai para a prisão imediatamente
		if (deveIrParaPrisaoPorTerceiraDupla) {
			int posAnt = j.getPosicao();
			enviarParaPrisao(id);
			deveIrParaPrisaoPorTerceiraDupla = false;
			turno.resetarDuplas();
			iniciarContextoDeQueda(j.getPosicao());
			notifyObservers();
			return new ResultadoMovimento(id, posAnt, 0, j.getPosicao(), false);
		}

		// Se está preso, tenta sair com dupla ou carta
		if (j.isNaPrisao()) {
			boolean saiu = tentarSairDaPrisaoComDuplaOuCarta();
			if (!saiu) {
				iniciarContextoDeQueda(j.getPosicao());
				notifyObservers();
				return new ResultadoMovimento(id, j.getPosicao(), 0, j.getPosicao(), false);
			}
		}

		// Movimento regular
		ResultadoMovimento r = Movimento.executar(j, ultimoD1, ultimoD2, tabuleiro);

		if (r.passouOuCaiuNoInicio) {
			banco.pagarHonorarios(j);
		}

		iniciarContextoDeQueda(j.getPosicao());

		// Casa "Vá para a Prisão"
		Casa casaAtual = tabuleiro.getCasa(j.getPosicao());
		if ("VA_PARA_PRISAO".equalsIgnoreCase(casaAtual.getTipo())) {
			int posAnt = j.getPosicao();
			enviarParaPrisao(id);
			iniciarContextoDeQueda(j.getPosicao());
			notifyObservers();
			return new ResultadoMovimento(id, posAnt, 0, j.getPosicao(), false);
		}
		notifyObservers();
		return r;
	}

	// =====================================================================================
	// REGRA #3 — COMPRAR PROPRIEDADE SEM DONO
	// =====================================================================================

	public boolean comprarPropriedade() {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();

		final Jogador jogador = jogadores.get(getJogadorDaVez());
		final Casa casaAtual = tabuleiro.getCasa(jogador.getPosicao());

		if (!(casaAtual instanceof Propriedade))
			return false;
		final Propriedade prop = (Propriedade) casaAtual;
		if (prop.temDono())
			return false;

		final int preco = prop.getPrecoTerreno();
		if (preco <= 0 || jogador.getSaldo() < preco)
			return false;

		jogador.debitar(preco);
		banco.creditar(preco);
		prop.setDono(jogador);

		this.acabouDeComprarNestaQueda = true;
		notifyObservers();
		return true;
	}

	// =====================================================================================
	// REGRA #4 — CONSTRUÇÕES (CASA / HOTEL)
	// =====================================================================================

	public boolean construirCasa() {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();
		if (posicaoDaQuedaAtual == null)
			return false;

		final Jogador jogador = jogadores.get(getJogadorDaVez());
		if (jogador.getPosicao() != posicaoDaQuedaAtual)
			return false;

		final Casa casa = tabuleiro.getCasa(jogador.getPosicao());
		if (!(casa instanceof Propriedade))
			return false;

		final Propriedade prop = (Propriedade) casa;
		if (!prop.temDono() || prop.getDono() != jogador)
			return false;
		if (acabouDeComprarNestaQueda)
			return false; // apenas em quedas subsequentes
		if (jaConstruiuNestaQueda)
			return false; // uma construção por queda
		if (!prop.podeConstruirCasa())
			return false; // 0..4 casas e sem hotel

		final int preco = prop.getPrecoCasa();
		if (preco <= 0 || jogador.getSaldo() < preco)
			return false;

		jogador.debitar(preco);
		banco.creditar(preco);
		prop.construirCasa();
		notifyObservers();

		jaConstruiuNestaQueda = true;
		return true;
	}

	public boolean construirHotel() {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();
		if (posicaoDaQuedaAtual == null)
			return false;

		final Jogador jogador = jogadores.get(getJogadorDaVez());
		if (jogador.getPosicao() != posicaoDaQuedaAtual)
			return false;

		final Casa casa = tabuleiro.getCasa(jogador.getPosicao());
		if (!(casa instanceof Propriedade))
			return false;

		final Propriedade prop = (Propriedade) casa;
		if (!prop.temDono() || prop.getDono() != jogador)
			return false;
		if (acabouDeComprarNestaQueda)
			return false;
		if (jaConstruiuNestaQueda)
			return false;
		if (!prop.podeConstruirHotel())
			return false; // requer ≥1 casa e sem hotel

		final int preco = prop.getPrecoHotel();
		if (preco <= 0 || jogador.getSaldo() < preco)
			return false;

		jogador.debitar(preco);
		banco.creditar(preco);
		prop.construirHotel();
		notifyObservers();

		jaConstruiuNestaQueda = true;
		return true;
	}

	// =====================================================================================
	// REGRA #5 — ALUGUEL AUTOMÁTICO (somente se ≥1 casa/hotel)
	// =====================================================================================

	public Transacao pagarAluguelSeDevido() {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();

		final int idPagador = getJogadorDaVez();
		final Jogador pagador = jogadores.get(idPagador);
		final Casa casa = tabuleiro.getCasa(pagador.getPosicao());

		if (!(casa instanceof Propriedade)) {
			return Transacao.semEfeito("Casa atual não é propriedade", idPagador, pagador.getPosicao(), null, 0);
		}

		final Propriedade prop = (Propriedade) casa;
		if (!prop.temDono()) {
			return Transacao.semEfeito("Propriedade sem dono", idPagador, pagador.getPosicao(), null, 0);
		}

		final Jogador dono = prop.getDono();
		if (dono == pagador) {
			return Transacao.semEfeito("Propriedade do próprio jogador", idPagador, pagador.getPosicao(), dono.getId(),
					0);
		}

		final boolean cobra = (prop.getNumCasas() >= 1) || prop.temHotel();
		if (!cobra) {
			return Transacao.semEfeito("Sem casas/hotel na 1ª iteração", idPagador, pagador.getPosicao(), dono.getId(),
					0);
		}

		final int aluguel = prop.calcularAluguelAtual();
		if (aluguel <= 0) {
			return Transacao.semEfeito("Aluguel calculado como zero", idPagador, pagador.getPosicao(), dono.getId(), 0);
		}

		// 1) Se não há saldo suficiente, tenta levantar fundos vendendo ao banco (90%)
		if (pagador.getSaldo() < aluguel) {
			tentarLevantarFundosPara(pagador, aluguel);
		}

		// 2) Reavalia: conseguiu cobrir?
		if (pagador.getSaldo() >= aluguel) {
			pagador.debitar(aluguel);
			dono.creditar(aluguel);
			return Transacao.aluguelEfetuado(idPagador, dono.getId(), prop.getPosicao(), aluguel);
		}

		// 3) Não conseguiu cobrir: paga tudo que tem ao dono e falência
		int disponivel = Math.max(0, pagador.getSaldo());
		if (disponivel > 0) {
			pagador.debitar(disponivel); // zera o saldo
			dono.creditar(disponivel); // dono recebe o que havia em caixa
		}

		// Executa a falência (remove jogador e devolve propriedades ao banco)
		executarFalencia(pagador);

		// Registramos a transação com o valor efetivamente pago (parcial)
		return Transacao.aluguelEfetuado(idPagador, dono.getId(), prop.getPosicao(), disponivel);
	}

	public Transacao aplicarEfeitosObrigatoriosPosMovimento() {
		// 1) IMPOSTO/LUCRO (casas especiais) – antes do aluguel
		Transacao t = aplicarCasaEspecialSeDevida();
		if (t != null && !"SEM_EFEITO".equals(t.getTipo())) {
			return t;
		}
		// 2) Aluguel (quando a casa é propriedade de outro e tem ≥1 casa/hotel)
		notifyObservers();
		return pagarAluguelSeDevido();
	}

	public Transacao deslocarPiaoEAplicarObrigatorios() {
		deslocarPiao();
		notifyObservers();
		return aplicarEfeitosObrigatoriosPosMovimento();
	}

	// =====================================================================================
	// REGRA #6 — PRISÃO (API)
	// =====================================================================================

	public void enviarParaPrisao(int idJogador) {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();
		if (idJogador < 0 || idJogador >= jogadores.size()) {
			throw new IllegalArgumentException("idJogador inválido");
		}
		final int idxPrisao = getIndicePrisaoOrThrow();
		final Jogador j = jogadores.get(idJogador);
		j.moverPara(idxPrisao);
		j.setNaPrisao(true);
		turno.resetarDuplas();
	}

	public boolean tentarSairDaPrisaoComDuplaOuCarta() {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();
		final Jogador j = jogadores.get(getJogadorDaVez());
		if (!j.isNaPrisao())
			return false;

		if (j.temCartaSaidaLivre()) {
			j.setCartaSaidaLivre(false);
			j.setNaPrisao(false);
			turno.resetarDuplas();
			return true;
		}
		if (turno.houveDupla()) {
			j.setNaPrisao(false);
			turno.resetarDuplas();
			return true;
		}
		return false;
	}

	public boolean usarCartaSaidaLivre() {
		exigirPartidaIniciada();
		final Jogador j = jogadores.get(getJogadorDaVez());
		if (!j.isNaPrisao() || !j.temCartaSaidaLivre())
			return false;

		j.setCartaSaidaLivre(false);
		j.setNaPrisao(false);
		turno.resetarDuplas();
		return true;
	}

	public boolean estaNaPrisao(int idJogador) {
		exigirPartidaIniciada();
		if (idJogador < 0 || idJogador >= jogadores.size()) {
			throw new IllegalArgumentException("idJogador inválido");
		}
		return jogadores.get(idJogador).isNaPrisao();
	}

	// =====================================================================================
	// REGRA #7 — LIQUIDAÇÃO E FALÊNCIA
	// =====================================================================================

	public boolean venderPropriedadeAoBanco(int posicaoPropriedade) {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();

		final Jogador j = jogadores.get(getJogadorDaVez());
		final Casa c = tabuleiro.getCasa(posicaoPropriedade);
		if (!(c instanceof Propriedade))
			return false;

		final Propriedade p = (Propriedade) c;
		if (!p.temDono() || p.getDono() != j)
			return false;

		final int valorAgregado = p.valorAgregadoAtual();
		final int pagamento = (valorAgregado * 9) / 10; // 90%

		banco.debitar(pagamento); // banco paga
		j.creditar(pagamento);
		p.resetarParaBanco(); // sem dono e sem construções
		return true;
	}

	public boolean declararFalenciaSeNecessario() {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();

		final Jogador j = jogadores.get(getJogadorDaVez());
		if (!j.isAtivo())
			return true;
		if (j.getSaldo() >= 0)
			return false;

		// Liquida do maior valor agregado para o menor
		List<Propriedade> minhas = listarPropriedadesDo(j);
		minhas.sort(Comparator.comparingInt(Propriedade::valorAgregadoAtual).reversed());

		for (Propriedade p : minhas) {
			if (j.getSaldo() >= 0)
				break;
			final int pagamento = (p.valorAgregadoAtual() * 9) / 10;
			banco.debitar(pagamento);
			j.creditar(pagamento);
			p.resetarParaBanco();
		}

		if (j.getSaldo() < 0) {
			j.falir();
			for (Propriedade p : listarPropriedadesDo(j)) {
				p.resetarParaBanco();
			}
			return true;
		}
		return false;
	}

	private List<Propriedade> listarPropriedadesDo(Jogador dono) {
		List<Propriedade> props = new ArrayList<>();
		for (int i = 0; i < tabuleiro.tamanho(); i++) {
			Casa c = tabuleiro.getCasa(i);
			if (c instanceof Propriedade) {
				Propriedade p = (Propriedade) c;
				if (p.temDono() && p.getDono() == dono) {
					props.add(p);
				}
			}
		}
		return props;
	}

	// =====================================================================================
	// CONSULTAS (apoio a testes)
	// =====================================================================================

	public int getPosicaoJogador(int idJogador) {
		exigirPartidaIniciada();
		return jogadores.get(idJogador).getPosicao();
	}

	public int getQuantidadeCasasTabuleiro() {
		exigirTabuleiroCarregado();
		return tabuleiro.tamanho();
	}

	public int getSaldoJogador(int idJogador) {
		exigirPartidaIniciada();
		return jogadores.get(idJogador).getSaldo();
	}

	public int getSaldoBanco() {
		return banco.getSaldo();
	}

	public boolean isJogadorAtivo(int idJogador) {
		exigirPartidaIniciada();
		if (idJogador < 0 || idJogador >= jogadores.size()) {
			throw new IllegalArgumentException("idJogador inválido");
		}
		return jogadores.get(idJogador).isAtivo();
	}

	// =====================================================================================
	// HELPERS DE TABULEIRO (para testes)
	// =====================================================================================

	public void carregarTabuleiroMinimoParaTeste(int nCasas) {
		if (nCasas <= 0)
			throw new IllegalArgumentException("nCasas deve ser > 0");
		List<Casa> casas = new ArrayList<>(nCasas);
		for (int i = 0; i < nCasas; i++) {
			casas.add(new Casa(i, "Casa " + i, "GENERICA"));
		}
		this.setTabuleiro(new Tabuleiro(casas));
	}

	public void carregarTabuleiroComImpostoELucro(int nCasas, int idxImposto, int valorImposto, int idxLucro,
			int valorLucro) {
		if (nCasas <= 0)
			throw new IllegalArgumentException("nCasas deve ser > 0");
		List<Casa> casas = new ArrayList<>(nCasas);
		for (int i = 0; i < nCasas; i++) {
			if (i == idxImposto)
				casas.add(new Casa(i, "Imposto", "IMPOSTO", valorImposto));
			else if (i == idxLucro)
				casas.add(new Casa(i, "Lucro", "LUCRO", valorLucro));
			else
				casas.add(new Casa(i, "Casa " + i, "GENERICA"));
		}
		this.setTabuleiro(new Tabuleiro(casas));
	}

	public void carregarTabuleiroDeTesteComUmaPropriedade(int nCasas, int idxPropriedade, int precoTerreno) {
		carregarTabuleiroDeTesteComUmaPropriedade(nCasas, idxPropriedade, precoTerreno, 0, 0);
	}

	public void carregarTabuleiroDeTesteComUmaPropriedade(int nCasas, int idxPropriedade, int precoTerreno,
			int precoCasa) {
		carregarTabuleiroDeTesteComUmaPropriedade(nCasas, idxPropriedade, precoTerreno, precoCasa, 0);
	}

	public void carregarTabuleiroDeTesteComUmaPropriedade(int nCasas, int idxPropriedade, int precoTerreno,
			int precoCasa, int precoHotel) {
		if (nCasas <= 0)
			throw new IllegalArgumentException("nCasas deve ser > 0");
		if (idxPropriedade < 0 || idxPropriedade >= nCasas) {
			throw new IllegalArgumentException("Índice de propriedade fora do tabuleiro.");
		}
		List<Casa> casas = new ArrayList<>(nCasas);
		for (int i = 0; i < nCasas; i++) {
			if (i == idxPropriedade) {
				casas.add(new Propriedade(i, "Propriedade " + i, precoTerreno, precoCasa, precoHotel, new int[0]));
			} else {
				casas.add(new Casa(i, "Casa " + i, "GENERICA"));
			}
		}
		this.setTabuleiro(new Tabuleiro(casas));
	}

	public void carregarTabuleiroDeTesteComUmaPropriedadeEAlugueis(int nCasas, int idxPropriedade, int precoTerreno,
			int precoCasa, int precoHotel, int[] alugueis) {

		if (nCasas <= 0)
			throw new IllegalArgumentException("nCasas deve ser > 0");
		if (idxPropriedade < 0 || idxPropriedade >= nCasas) {
			throw new IllegalArgumentException("Índice de propriedade fora do tabuleiro.");
		}
		List<Casa> casas = new ArrayList<>(nCasas);
		for (int i = 0; i < nCasas; i++) {
			if (i == idxPropriedade) {
				casas.add(new Propriedade(i, "Propriedade " + i, precoTerreno, precoCasa, precoHotel, alugueis));
			} else {
				casas.add(new Casa(i, "Casa " + i, "GENERICA"));
			}
		}
		this.setTabuleiro(new Tabuleiro(casas));
	}

	public void carregarTabuleiroBasicoComPrisao(int nCasas, int idxPrisao, int idxVaParaPrisao) {
		if (nCasas <= 0)
			throw new IllegalArgumentException("nCasas deve ser > 0");
		if (idxPrisao < 0 || idxPrisao >= nCasas)
			throw new IllegalArgumentException("idxPrisao fora do tabuleiro.");
		if (idxVaParaPrisao < 0 || idxVaParaPrisao >= nCasas)
			throw new IllegalArgumentException("idxVaParaPrisao fora do tabuleiro.");
		List<Casa> casas = new ArrayList<>(nCasas);
		for (int i = 0; i < nCasas; i++) {
			if (i == idxPrisao) {
				casas.add(new Casa(i, "Prisão", "PRISAO"));
			} else if (i == idxVaParaPrisao) {
				casas.add(new Casa(i, "Vá para a Prisão", "VA_PARA_PRISAO"));
			} else {
				casas.add(new Casa(i, "Casa " + i, "GENERICA"));
			}
		}
		this.setTabuleiro(new Tabuleiro(casas));
	}

	public void debugForcarPosicaoJogador(int idJogador, int posicao) {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();
		if (idJogador < 0 || idJogador >= jogadores.size()) {
			throw new IllegalArgumentException("idJogador inválido");
		}
		if (posicao < 0 || posicao >= tabuleiro.tamanho()) {
			throw new IllegalArgumentException("posicao fora do tabuleiro");
		}
		jogadores.get(idJogador).moverPara(posicao);
		this.posicaoDaQuedaAtual = posicao; // simula que 'caiu' aqui
	}

	public void debugForcarDonoECasasDaPropriedade(int posicaoPropriedade, int idDono, int numCasas, boolean hotel) {
		exigirPartidaIniciada();
		exigirTabuleiroCarregado();

		Casa c = tabuleiro.getCasa(posicaoPropriedade);
		if (!(c instanceof Propriedade)) {
			throw new IllegalArgumentException("Posição não é Propriedade");
		}
		Propriedade p = (Propriedade) c;

		if (idDono < 0 || idDono >= jogadores.size()) {
			throw new IllegalArgumentException("idDono inválido");
		}
		p.setDono(jogadores.get(idDono));

		while (p.getNumCasas() < numCasas) {
			if (!p.podeConstruirCasa())
				break;
			p.construirCasa();
			notifyObservers();
		}
		if (hotel && p.podeConstruirHotel()) {
			p.construirHotel();
			notifyObservers();
		}
	}

	public void debugDarCartaSaidaLivreAoJogador(int idJogador, boolean possui) {
		exigirPartidaIniciada();
		if (idJogador < 0 || idJogador >= jogadores.size()) {
			throw new IllegalArgumentException("idJogador inválido");
		}
		jogadores.get(idJogador).setCartaSaidaLivre(possui);
	}

	// =====================================================================================
	// SUPORTES INTERNOS
	// =====================================================================================

	private int getIndicePrisaoOrThrow() {
		int idx = encontrarIndicePorTipo("PRISAO");
		if (idx < 0)
			throw new IllegalStateException("Tabuleiro não possui casa PRISAO.");
		return idx;
	}

	/**
	 * Aplica automaticamente IMPOSTO/LUCRO na casa atual do jogador da vez. Retorna
	 * uma Transacao se houver efeito; caso contrário, null.
	 */
	private Transacao aplicarCasaEspecialSeDevida() {
		final int id = getJogadorDaVez();
		final Jogador j = jogadores.get(id);
		final Casa casa = tabuleiro.getCasa(j.getPosicao());

		final String tipo = casa.getTipo();
		final int valor = Math.max(0, casa.getValorEfeito());

		if ("IMPOSTO".equalsIgnoreCase(tipo)) {
			if (valor <= 0)
				return Transacao.semEfeito("Imposto zero", id, casa.getPosicao(), null, 0);

			// tentar levantar fundos (mesma política do aluguel)
			if (j.getSaldo() < valor) {
				tentarLevantarFundosPara(j, valor);
			}

			if (j.getSaldo() >= valor) {
				j.debitar(valor);
				banco.creditar(valor);
				return Transacao.impostoPago(id, casa.getPosicao(), valor);
			} else {
				// paga o que tiver e decreta falência
				int disponivel = Math.max(0, j.getSaldo());
				if (disponivel > 0) {
					j.debitar(disponivel);
					banco.creditar(disponivel);
				}
				executarFalencia(j);
				return Transacao.impostoPago(id, casa.getPosicao(), disponivel);
			}
		}

		if ("LUCRO".equalsIgnoreCase(tipo)) {
			if (valor <= 0)
				return Transacao.semEfeito("Lucro zero", id, casa.getPosicao(), null, 0);

			banco.debitar(valor);
			j.creditar(valor);
			return Transacao.lucroRecebido(id, casa.getPosicao(), valor);
		}

		return null; // não é casa especial
	}

	@SuppressWarnings("unused")
	private int getIndiceVaParaPrisaoOrMinus1() {
		return encontrarIndicePorTipo("VA_PARA_PRISAO");
	}

	private int encontrarIndicePorTipo(String tipo) {
		for (int i = 0; i < tabuleiro.tamanho(); i++) {
			Casa c = tabuleiro.getCasa(i);
			if (tipo.equalsIgnoreCase(c.getTipo()))
				return i;
		}
		return -1;
	}

	/**
	 * Vende propriedades ao banco (90%) até que o jogador alcance 'valorNecessario'
	 * ou acabe o que vender.
	 */
	private void tentarLevantarFundosPara(Jogador j, int valorNecessario) {
		if (j.getSaldo() >= valorNecessario)
			return;
		List<Propriedade> minhas = listarPropriedadesDo(j);
		// vender do maior valor agregado para o menor
		minhas.sort(Comparator.comparingInt(Propriedade::valorAgregadoAtual).reversed());
		for (Propriedade p : minhas) {
			if (j.getSaldo() >= valorNecessario)
				break;
			final int pagamento = (p.valorAgregadoAtual() * 9) / 10; // 90%
			// Banco paga ao jogador e a propriedade volta ao banco
			banco.debitar(pagamento);
			j.creditar(pagamento);
			p.resetarParaBanco();
		}
	}

	/**
	 * Marca falência, remove o jogador do jogo e devolve todas as propriedades ao
	 * banco.
	 */
	private void executarFalencia(Jogador j) {
		// devolve quaisquer propriedades restantes ao banco
		for (Propriedade p : listarPropriedadesDo(j)) {
			p.resetarParaBanco();
		}
		j.falir(); // marca inativo / fora do jogo
	}

	public void definirOrdemJogadores(List<Integer> ordem) {
		exigirPartidaIniciada();
		if (ordem == null || ordem.size() != jogadores.size()) {
			throw new IllegalArgumentException("Ordem inválida: tamanho diferente do número de jogadores.");
		}
		boolean[] seen = new boolean[jogadores.size()];
		for (Integer id : ordem) {
			if (id == null || id < 0 || id >= jogadores.size() || seen[id]) {
				throw new IllegalArgumentException("Ordem inválida: ids repetidos/fora do intervalo.");
			}
			seen[id] = true;
		}
		turno.definirOrdem(ordem);
		notifyObservers();
	}

	public void carregarTabuleiroOficialBR() {
		this.setTabuleiro(TabuleiroOficialFactory.criar());
	}

	// OBSERVER

	public static interface Observer {
		void update(GameModel source);
	}

	private final java.util.concurrent.CopyOnWriteArrayList<Observer> observers = new java.util.concurrent.CopyOnWriteArrayList<>();

	public void addObserver(Observer o) {
		if (o != null)
			observers.addIfAbsent(o);
	}

	public void removeObserver(Observer o) {
		if (o != null)
			observers.remove(o);
	}

	private void notifyObservers() {
		for (Observer o : observers) {
			try {
				o.update(this);
			} catch (Exception ignore) {
			}
		}
	}

	public Integer getUltimoD1() {
		return ultimoD1;
	}

	public Integer getUltimoD2() {
		return ultimoD2;
	}

	public boolean houveDupla() {
		return turno != null && turno.houveDupla();
	}

	public boolean passouOuCaiuNoInicioDaUltimaJogada() {
		return false; 
	}

}
