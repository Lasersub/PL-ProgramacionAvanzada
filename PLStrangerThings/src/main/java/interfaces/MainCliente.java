package interfaces;

public class MainCliente {

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            InterfazDatosCliente ventana = new InterfazDatosCliente();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
