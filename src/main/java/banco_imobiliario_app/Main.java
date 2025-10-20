package banco_imobiliario_app;

import javax.swing.SwingUtilities;
import banco_imobiliario_controller.AppController;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppController.getInstance().exibirJanelaInicial();
        });
    }
}
