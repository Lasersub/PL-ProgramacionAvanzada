package interfaces;

// ============================================================
// IMPLEMENTACIÓN EXTRA — OPCIÓN C: Nuevo método RMI getEstadisticasGlobales()
//
// ARCHIVO 3 de 3: Cambios en el cliente (PanelControl.java)
//
// RESUMEN DEL CAMBIO:
//   Añadir una llamada a servidor.getEstadisticasGlobales() dentro del método
//   actualizarDatos() que ya existe, y mostrar el resultado en un JLabel nuevo.
//
// CONTEXTO — CÓMO FUNCIONA EL CLIENTE YA:
//   - PanelControl tiene una referencia: private ISimulacionRemota servidor
//   - Se conecta en el constructor: Naming.lookup("rmi://localhost/SimulacionHawkins")
//   - Un javax.swing.Timer llama actualizarDatos() cada 500ms automáticamente
//   - actualizarDatos() llama métodos del servidor y actualiza JLabels con setText()
//   - Todo el patrón ya existe; solo hay que añadir UNA línea más
// ============================================================

public class EXTRA_C_Cliente_Cambios {

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO 1: Añadir un JLabel en el formulario para mostrar las estadísticas
    // ─────────────────────────────────────────────────────────────────────────
    // En el diseñador visual de NetBeans (o en initComponents), añadir:
    //
    //   private javax.swing.JLabel estadisticasGlobales;
    //
    // Y en initComponents() inicializarlo y añadirlo al panel:
    //
    //   estadisticasGlobales = new javax.swing.JLabel("Cargando estadísticas...");
    //   estadisticasGlobales.setFont(new java.awt.Font("Segoe UI", 0, 12));
    //   // añadir al layout del panel donde corresponda


    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO 2: Añadir la llamada en actualizarDatos()
    // ─────────────────────────────────────────────────────────────────────────
    // Dentro del try-catch de actualizarDatos(), añadir junto al resto de llamadas:
    //
    //   // ← NUEVA LÍNEA: una sola llamada RMI devuelve 4 datos a la vez
    //   estadisticasGlobales.setText(servidor.getEstadisticasGlobales());
    //
    // El método actualizarDatos() completo con el cambio quedaría así:
    //
    //   private void actualizarDatos() {
    //       if (servidor == null) return;
    //       try {
    //           numNinosHawkins.setText(String.valueOf(servidor.getNumNinosEnHawkins()));
    //           numSangreHawkins.setText(String.valueOf(servidor.getNumGotasSangre()));
    //           // ... resto de llamadas sin cambios ...
    //
    //           // ← NUEVO
    //           estadisticasGlobales.setText(servidor.getEstadisticasGlobales());
    //
    //       } catch (RemoteException e) {
    //           eventoActivo.setText("Error de conexión");
    //       }
    //   }


    // ─────────────────────────────────────────────────────────────────────────
    // POR QUÉ NO HAY QUE HACER NADA MÁS EN EL CLIENTE
    // ─────────────────────────────────────────────────────────────────────────
    // El cliente ya tiene:
    //   - La conexión RMI establecida en el constructor
    //   - El Timer que llama actualizarDatos() cada 500ms automáticamente
    //   - El manejo de RemoteException en el catch existente
    //
    // El nuevo método getEstadisticasGlobales() es visible en el cliente porque
    // está declarado en ISimulacionRemota, que el cliente ya importa e usa.
    // El stub RMI generado automáticamente incluirá el nuevo método.
    // Solo hace falta llamarlo y mostrar el resultado.


    // ─────────────────────────────────────────────────────────────────────────
    // PREGUNTA TÍPICA: "¿Cómo sabe el cliente que existe el nuevo método?"
    // ─────────────────────────────────────────────────────────────────────────
    // El cliente trabaja siempre con la INTERFAZ ISimulacionRemota, nunca con
    // la implementación SimulacionRemota directamente.
    // Cuando añadimos getEstadisticasGlobales() a ISimulacionRemota, el cliente
    // ya puede llamarlo porque su referencia es de tipo ISimulacionRemota.
    // En tiempo de ejecución, RMI enruta la llamada al servidor a través del stub.
    // Esta es la esencia del patrón RMI: cliente y servidor comparten la interfaz,
    // el cliente nunca conoce la implementación concreta.


    // ─────────────────────────────────────────────────────────────────────────
    // PREGUNTA TÍPICA: "¿Por qué no actualizar en un hilo separado?"
    // ─────────────────────────────────────────────────────────────────────────
    // Las llamadas RMI son bloqueantes (esperan respuesta del servidor).
    // Si el servidor tarda, actualizarDatos() bloquea el Event Dispatch Thread
    // (EDT) de Swing → la UI se congela.
    // La solución correcta sería usar SwingWorker (ya importado en PanelControl):
    //
    //   new SwingWorker<String, Void>() {
    //       protected String doInBackground() throws Exception {
    //           return servidor.getEstadisticasGlobales(); // en hilo de fondo
    //       }
    //       protected void done() {
    //           try { estadisticasGlobales.setText(get()); } // en EDT
    //           catch (Exception e) { /* manejar */ }
    //       }
    //   }.execute();
    //
    // Para la defensa, la versión simple (llamada directa en el EDT) es aceptable
    // ya que el servidor es local y las llamadas son muy rápidas.
    // Mencionar SwingWorker demuestra que conoces el problema y la solución.
}
