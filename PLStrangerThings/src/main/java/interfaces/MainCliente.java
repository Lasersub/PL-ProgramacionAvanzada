package interfaces;

public class MainCliente {

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            InterfazDatos ventana = new InterfazDatos();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
