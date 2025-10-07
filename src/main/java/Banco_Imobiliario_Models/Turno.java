package Banco_Imobiliario_Models;

/**
 * Estado mínimo do turno: quem joga e controle de duplas consecutivas.
 * A passagem de turno/efeitos de 3ª dupla serão tratados nas próximas regras.
 */
final class Turno {
    private int jogadorDaVez;
    private int duplasConsecutivas;
    private int ultimoD1, ultimoD2;

    Turno(int jogadorInicial) {
        this.jogadorDaVez = jogadorInicial;
        this.duplasConsecutivas = 0;
    }

    void registrarLance(int d1, int d2) {
        this.ultimoD1 = d1;
        this.ultimoD2 = d2;
        if (d1 == d2) {
            duplasConsecutivas++;
        } else {
            duplasConsecutivas = 0;
        }
    }

    boolean houveDupla() {
        return ultimoD1 == ultimoD2;
    }

    int getDuplasConsecutivas() {
        return duplasConsecutivas;
    }

    int getJogadorDaVez() {
        return jogadorDaVez;
    }
}
