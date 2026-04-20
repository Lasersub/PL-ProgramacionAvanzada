
package clases;

// Importamos la interfaz desde otro paquete
import interfaces.InterfazDatos;

public class Main {

    public static void main(String[] args) {
        
        // -- INICIALIZACIÓN INTERFAZ -- 

        // Usamos invokeLater para asegurar que la interfaz
        // se inicie correctamente
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // 1. Creamos la instancia de la ventana
                InterfazDatos ventanaPrincipal = new InterfazDatos();
                
                // 2. Centramos la ventana en la pantalla
                ventanaPrincipal.setLocationRelativeTo(null);
                
                // 3. La hacemos visible
                ventanaPrincipal.setVisible(true);
            }
        });
        
        
    }


}
