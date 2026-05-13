package clases;

// ============================================================
// IMPLEMENTACIÓN EXTRA — OPCIÓN C: Nuevo método RMI getEstadisticasGlobales()
//
// ARCHIVO 1 de 3: Cambios en ISimulacionRemota.java
// Ver EXTRA_C_SimulacionRemota_Cambios.java para la implementación.
// Ver EXTRA_C_Cliente_Cambios.java para cómo usarlo en el cliente.
//
// CONCEPTO:
//   Se añade un único método nuevo a la interfaz RMI que devuelve un resumen
//   global del estado de la simulación en un solo objeto: total niños vivos,
//   total niños capturados históricos, total demogorgons activos y sangre total.
//
// POR QUÉ UN SOLO MÉTODO Y NO CUATRO MÉTODOS SEPARADOS:
//   Cada llamada RMI tiene coste de red (serialización + viaje ida y vuelta).
//   Agrupar 4 datos relacionados en una sola llamada es más eficiente.
//   En Java RMI, esto se hace devolviendo un objeto serializable (String con
//   formato, o una clase EstadisticasGlobales que implemente Serializable).
//   En este caso usamos String formateado para no crear clases extra.
//
// ARCHIVOS QUE SE MODIFICAN:
//   1. ISimulacionRemota.java   → declarar el nuevo método en la interfaz
//   2. SimulacionRemota.java    → implementar el método
//   3. (cliente) InterfazDatosCliente o PanelControl → llamar y mostrar
//
// ARCHIVOS QUE NO SE MODIFICAN:
//   SimulacionBackend.java, Nino.java, Demogorgon.java, GestorEventos.java...
//   → la lógica ya existe; solo exponemos datos que ya están disponibles
// ============================================================

public class EXTRA_C_ISimulacionRemota_Cambios {

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO ÚNICO: Añadir declaración del método en ISimulacionRemota.java
    // ─────────────────────────────────────────────────────────────────────────
    // Añadir al final de la interfaz ISimulacionRemota, antes del cierre }:
    //
    //   /**
    //    * Devuelve un resumen global del estado actual de la simulación.
    //    * Agrupa 4 datos en una sola llamada RMI para minimizar el coste de red.
    //    *
    //    * @return String con formato:
    //    *   "Niños vivos: X | Capturados histórico: Y | Demogorgons activos: Z | Sangre total: W"
    //    * @throws java.rmi.RemoteException si falla la comunicación RMI
    //    */
    //   String getEstadisticasGlobales() throws java.rmi.RemoteException;
    //
    // ALTERNATIVA más rica: devolver un Map<String,Integer> también serializable:
    //
    //   java.util.Map<String,Integer> getEstadisticasGlobales() throws java.rmi.RemoteException;
    //
    // Con Map el cliente puede acceder a cada dato por clave:
    //   map.get("ninosVivos"), map.get("capturadosHistorico"), etc.
    // Con String es más simple pero menos flexible.
    // Para la defensa, String es suficiente y más fácil de explicar.


    // ─────────────────────────────────────────────────────────────────────────
    // POR QUÉ TODA LLAMADA RMI DEBE DECLARAR throws RemoteException
    // ─────────────────────────────────────────────────────────────────────────
    // RemoteException es checked exception obligatoria en RMI.
    // Puede ocurrir si: el servidor no está disponible, hay problemas de red,
    // el objeto remoto fue desregistrado, etc.
    // El cliente SIEMPRE debe manejarla con try-catch o propagarla.
}
