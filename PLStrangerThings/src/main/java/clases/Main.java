
package clases;

// Importamos la interfaz desde otro paquete
import interfaces.InterfazDatos;

public class Main {

    public static void main(String[] args) {
        
        // -- INICIALIZACIÓN SIMULACIÓN BACKEND --
        
        // Creamos la lógica
        SimulacionBackend backend = new SimulacionBackend();
        
        // Iniciamos el hilo
        Thread hiloBackend = new Thread(backend);
        hiloBackend.start();

        try {
            java.rmi.registry.LocateRegistry.createRegistry(1099);
            SimulacionRemota objetoRemoto = new SimulacionRemota(backend);
            java.rmi.Naming.rebind("rmi://localhost/SimulacionHawkins", objetoRemoto);
            System.out.println("Servidor RMI registrado en puerto 1099");
        } catch (Exception e) {
            System.err.println("Error al iniciar servidor RMI: " + e.getMessage());
        }

        // -- INICIALIZACIÓN INTERFAZ --

        // Usamos invokeLater para asegurar que la interfaz
        // se inicie correctamente
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // 1. Creamos la instancia de la ventana
                InterfazDatos ventanaPrincipal = new InterfazDatos(backend);
                
                // 2. Centramos la ventana en la pantalla
                ventanaPrincipal.setLocationRelativeTo(null);
                
                // 3. La hacemos visible
                ventanaPrincipal.setVisible(true);
            }
        });
        
        
    }


}
