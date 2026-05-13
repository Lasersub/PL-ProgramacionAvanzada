package clases;

// ============================================================
// IMPLEMENTACIÓN EXTRA — OPCIÓN C: Nuevo método RMI getEstadisticasGlobales()
//
// ARCHIVO 2 de 3: Cambios en SimulacionRemota.java
//
// RESUMEN DEL CAMBIO:
//   Añadir un único método @Override que implementa getEstadisticasGlobales()
//   declarado en ISimulacionRemota. Solo lee datos ya disponibles en el backend;
//   no hay nueva lógica de concurrencia.
//
// DATOS QUE DEVUELVE:
//   1. ninosVivos         → suma de niños en las 3 zonas de Hawkins +
//                           niños en las 4 zonas del Upside Down
//                           (NO incluye capturados en Colmena: están fuera del ciclo)
//   2. capturadosHistorico → Colmena.getTotalDepositados() (AtomicInteger, nunca decrece)
//   3. demogorgonesActivos → tamaño de backend.getDemogorgons() (synchronizedList)
//   4. sangreTotal         → GestorEventos.getSangreTotal() (AtomicInteger)
// ============================================================

public class EXTRA_C_SimulacionRemota_Cambios {

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO ÚNICO: Añadir este método en SimulacionRemota.java
    // Pegarlo al final, antes del último }
    // ─────────────────────────────────────────────────────────────────────────
    //
    //   @Override
    //   public String getEstadisticasGlobales() throws RemoteException {
    //
    //       // 1. Niños vivos: en Hawkins + en el Upside Down (excluye Colmena)
    //       int ninosVivos =
    //           backend.getHawkins().getCallePrincipal().getListaNinos().size()
    //         + backend.getHawkins().getSotanoByers().getListaNinos().size()
    //         + backend.getHawkins().getRadioWSQK().getListaNinos().size()
    //         + backend.getUpsideDown().getBosque().getListaNinos().size()
    //         + backend.getUpsideDown().getLaboratorio().getListaNinos().size()
    //         + backend.getUpsideDown().getCentroComercial().getListaNinos().size()
    //         + backend.getUpsideDown().getAlcantarillado().getListaNinos().size();
    //
    //       // 2. Total histórico de capturas (AtomicInteger en Colmena → thread-safe)
    //       int capturadosHistorico = backend.getUpsideDown().getColmena().getTotalDepositados();
    //
    //       // 3. Demogorgons activos (synchronizedList → .size() thread-safe)
    //       int demogorgonesActivos = backend.getDemogorgons().size();
    //
    //       // 4. Sangre total acumulada (AtomicInteger en GestorEventos → thread-safe)
    //       int sangreTotal = (backend.getGestorEventos() != null)
    //           ? backend.getGestorEventos().getSangreTotal()
    //           : 0;
    //
    //       // Devolver como String formateado (serializable por defecto en Java)
    //       return "Niños vivos: "          + ninosVivos
    //            + " | Capturados histórico: " + capturadosHistorico
    //            + " | Demogorgons activos: "  + demogorgonesActivos
    //            + " | Sangre total: "         + sangreTotal;
    //   }


    // ─────────────────────────────────────────────────────────────────────────
    // ALTERNATIVA: devolver Map<String, Integer> en lugar de String
    // ─────────────────────────────────────────────────────────────────────────
    // Si la interfaz declara Map<String,Integer> en lugar de String:
    //
    //   @Override
    //   public java.util.Map<String, Integer> getEstadisticasGlobales() throws RemoteException {
    //       java.util.Map<String, Integer> stats = new java.util.HashMap<>();
    //       stats.put("ninosVivos", ninosVivos);
    //       stats.put("capturadosHistorico", capturadosHistorico);
    //       stats.put("demogorgonesActivos", demogorgonesActivos);
    //       stats.put("sangreTotal", sangreTotal);
    //       return stats;
    //       // HashMap es Serializable → puede enviarse por RMI sin problemas
    //   }


    // ─────────────────────────────────────────────────────────────────────────
    // POR QUÉ NO HAY CERROJOS EN ESTE MÉTODO
    // ─────────────────────────────────────────────────────────────────────────
    // Todos los datos leídos ya son thread-safe por su propio mecanismo:
    //   - CopyOnWriteArrayList.size()  → thread-safe, snapshot atómico
    //   - AtomicInteger.get()          → thread-safe, lectura atómica
    //   - synchronizedList.size()      → thread-safe, operación individual
    //
    // Esta es la ventaja de haber diseñado bien la concurrencia en las clases
    // base: la capa RMI puede leer datos sin añadir sincronización propia.
    // El coste es que los 4 valores no son un snapshot atómico conjunto
    // (podrían ser de instantes ligeramente distintos), pero para una
    // visualización en tiempo real es completamente aceptable.
}
