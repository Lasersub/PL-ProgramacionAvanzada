package clases;

// ============================================================
// CLASE: SimulacionRemota.java
// RESPONSABILIDAD: Implementación RMI del contrato ISimulacionRemota.
//   Es el "puente" entre el cliente remoto y el SimulacionBackend local.
//   Expone el estado de la simulación en tiempo real a través de la red,
//   delegando TODAS las consultas en el backend.
//
// HERENCIA: extiende UnicastRemoteObject
//   → exporta este objeto automáticamente en el constructor (super())
//   → el cliente recibe un stub (proxy) que redirige llamadas a esta clase
//   → cada método puede lanzar RemoteException si falla la comunicación
//
// IMPLEMENTA: ISimulacionRemota (interfaz compartida con el cliente)
//   → el cliente solo conoce la interfaz, nunca la implementación
//   → permite que cliente y servidor estén en JVMs distintas
//
// DECISIÓN DE DISEÑO CLAVE — serialización como String:
//   Los métodos que devuelven listas de entidades NO devuelven List<Nino> ni
//   List<Demogorgon> directamente. En su lugar, devuelven List<String> con los IDs.
//   MOTIVO: Nino y Demogorgon son hilos (implementan Runnable) y contienen
//   referencias no serializables (Thread, Lock, Condition...). Si intentáramos
//   serializarlos para enviarlos al cliente → NotSerializableException.
//   La solución es extraer solo el dato que el cliente necesita (el ID) y
//   enviar eso. El cliente muestra "N0042" sin necesitar el objeto completo.
//
// THREAD-SAFETY EN LOS MÉTODOS:
//   Los métodos no usan cerrojos propios. La seguridad viene de:
//   - CopyOnWriteArrayList en las zonas: iteraciones seguras sin lock
//   - AtomicInteger en contadores: lecturas consistentes sin lock
//   - volatile en flags de GestorEventos: visibilidad garantizada
//   Esta clase es solo una fachada de lectura; la concurrencia ya está
//   gestionada en las clases que consulta.
//
// CÓMO SE REGISTRA EN EL REGISTRY (ver Main.java):
//   Registry registry = LocateRegistry.createRegistry(1099);
//   registry.bind("SimulacionRemota", new SimulacionRemota(backend));
//   El cliente hace Naming.lookup("rmi://host/SimulacionRemota") y obtiene
//   un stub que apunta a este objeto.
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Qué es UnicastRemoteObject y para qué sirve extends?"
//   → UnicastRemoteObject es la clase base de RMI que exporta el objeto
//     automáticamente cuando se llama a super() en el constructor. Sin ella,
//     habría que llamar a UnicastRemoteObject.exportObject() manualmente.
//     Al extenderla, el objeto queda "escuchando" peticiones RMI desde su
//     creación, listo para ser registrado en el RMI Registry.
//
//   "¿Por qué devuelves List<String> y no List<Nino>?"
//   → Porque RMI serializa los objetos para enviarlos por red. Nino tiene
//     atributos no serializables (Thread miHilo, referencias a objetos con Lock).
//     Serializar el objeto completo fallaría. Además, el cliente no necesita
//     el objeto: solo el ID para mostrarlo. Extraemos solo lo necesario.
// ============================================================

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.Comparator;

/**
 * Implementación RMI de {@link ISimulacionRemota}. Expone el estado en tiempo real
 * de la simulación a clientes remotos delegando todas las consultas en el
 * {@link SimulacionBackend}. Los métodos que devuelven listas de entidades las
 * serializan como listas de identificadores {@code String} para evitar dependencias
 * de clases no serializables en el cliente.
 */
public class SimulacionRemota extends UnicastRemoteObject implements ISimulacionRemota {

    // Referencia al núcleo local de la simulación.
    // SimulacionRemota es solo una fachada: toda la lógica está en el backend.
    private SimulacionBackend backend;

    /**
     * Construye el objeto remoto y lo exporta automáticamente mediante
     * {@link UnicastRemoteObject}.
     * super() → llama al constructor de UnicastRemoteObject → exporta este objeto
     * en un puerto anónimo, listo para ser registrado en el RMI Registry.
     *
     * @param backend backend de la simulación al que se delegan todas las consultas
     * @throws RemoteException si falla la exportación RMI
     */
    public SimulacionRemota(SimulacionBackend backend) throws RemoteException {
        super(); // CRÍTICO: exporta el objeto remoto automáticamente
        this.backend = backend;
    }

    // ── Métodos de conteo: Hawkins ────────────────────────────────────────────
    // Suman las listas de las tres zonas de Hawkins (CopyOnWriteArrayList → thread-safe)

    @Override
    public int getNumNinosEnHawkins() throws RemoteException {
        // Suma niños en las 3 zonas de Hawkins: Calle Principal + Sótano + Radio
        return backend.getHawkins().getCallePrincipal().getListaNinos().size()
             + backend.getHawkins().getSotanoByers().getListaNinos().size()
             + backend.getHawkins().getRadioWSQK().getListaNinos().size();
    }

    // ── Métodos de conteo: Portales ───────────────────────────────────────────
    // Cada portal suma niños en cola de ida (CopyOnWriteArrayList) +
    // niños esperando vuelta (AtomicInteger) → total de niños "en el portal"

    @Override
    public int getNumNinosPortalBosque() throws RemoteException {
        Portal p = backend.getHawkins().getPortalBosque();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosPortalLaboratorio() throws RemoteException {
        Portal p = backend.getHawkins().getPortalLaboratorio();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosPortalCentroComercial() throws RemoteException {
        Portal p = backend.getHawkins().getPortalCentroComercial();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    @Override
    public int getNumNinosPortalAlcantarillado() throws RemoteException {
        Portal p = backend.getHawkins().getPortalAlcantarillado();
        return p.getNinosEnColaIda().size() + p.getNinosEsperandoVuelta();
    }

    // ── Métodos de conteo: zonas Upside Down ──────────────────────────────────
    // Lectura directa del tamaño de CopyOnWriteArrayList → thread-safe sin lock

    @Override
    public int getNumNinosLaboratorio() throws RemoteException {
        return backend.getUpsideDown().getLaboratorio().getListaNinos().size();
    }

    @Override
    public int getNumNinosCentroComercial() throws RemoteException {
        return backend.getUpsideDown().getCentroComercial().getListaNinos().size();
    }

    @Override
    public int getNumNinosBosque() throws RemoteException {
        return backend.getUpsideDown().getBosque().getListaNinos().size();
    }

    @Override
    public int getNumNinosAlcantarillado() throws RemoteException {
        return backend.getUpsideDown().getAlcantarillado().getListaNinos().size();
    }

    @Override
    public int getNumNinosColmena() throws RemoteException {
        return backend.getUpsideDown().getColmena().getListaNinos().size();
    }

    // ── Métodos de conteo: Demogorgons por zona ───────────────────────────────

    @Override
    public int getNumDemogorgonesLaboratorio() throws RemoteException {
        return backend.getUpsideDown().getLaboratorio().getListaDemogorgons().size();
    }

    @Override
    public int getNumDemogorgonesCentroComercial() throws RemoteException {
        return backend.getUpsideDown().getCentroComercial().getListaDemogorgons().size();
    }

    @Override
    public int getNumDemogorgonesBosque() throws RemoteException {
        return backend.getUpsideDown().getBosque().getListaDemogorgons().size();
    }

    @Override
    public int getNumDemogorgonesAlcantarillado() throws RemoteException {
        return backend.getUpsideDown().getAlcantarillado().getListaDemogorgons().size();
    }

    // ── Ranking Top 3 Demogorgons ─────────────────────────────────────────────

    /**
     * Devuelve los tres demogorgons con más capturas, ordenados de mayor a menor.
     *
     * THREAD-SAFETY: hacemos una copia de la lista (new ArrayList) antes de ordenar.
     * Si ordenáramos la lista original (synchronizedList) directamente sin bloque
     * synchronized, podría corromperse durante la ordenación si otro hilo añade
     * un demogorgon en paralelo. La copia aisla la ordenación.
     *
     * @return lista de hasta 3 cadenas con formato "DxxXX: N capturas"
     */
    @Override
    public List<String> getRankingTop3() throws RemoteException {
        // Copia defensiva de la lista para no bloquear al resto de hilos durante el sort
        List<Demogorgon> copia = new ArrayList<>(backend.getDemogorgons());
        copia.sort(Comparator.comparingInt(Demogorgon::getCapturas).reversed());
        List<String> resultado = new ArrayList<>();
        for (int i = 0; i < Math.min(3, copia.size()); i++) {
            Demogorgon d = copia.get(i);
            resultado.add(d.getId() + ": " + d.getCapturas() + " capturas");
        }
        return resultado;
    }

    // ── Estado del evento global ──────────────────────────────────────────────

    /**
     * Devuelve el nombre legible del evento global activo en este momento.
     * Los guiones bajos del enum se reemplazan por espacios para mejor legibilidad.
     *
     * @return nombre del evento (ej. "APAGON LABORATORIO"),
     *         o {@code "Sin evento activo"} si TipoEvento == NINGUNO
     */
    @Override
    public String getEventoActivo() throws RemoteException {
        GestorEventos gestor = backend.getGestorEventos();
        if (gestor == null || gestor.getEventoActual() == GestorEventos.TipoEvento.NINGUNO) {
            return "Sin evento activo";
        }
        // Convierte "APAGON_LABORATORIO" → "APAGON LABORATORIO" para la UI del cliente
        return gestor.getEventoActual().name().replace('_', ' ');
    }

    /**
     * Devuelve los segundos que restan para que finalice el evento activo.
     * Calculado en GestorEventos usando System.currentTimeMillis().
     *
     * @return segundos restantes, o {@code 0} si no hay evento activo
     */
    @Override
    public long getSegundosRestantesEvento() throws RemoteException {
        GestorEventos gestor = backend.getGestorEventos();
        if (gestor == null) return 0;
        return gestor.getSegundosRestantes();
    }

    // ── Control de pausa ──────────────────────────────────────────────────────

    /**
     * Alterna el estado de la simulación: si está pausada la reanuda, y viceversa.
     * El cliente remoto llama este método con el botón "Detener/Reanudar".
     * La pausa real se gestiona en SimulacionBackend con ReentrantLock + Condition.
     */
    @Override
    public void pausarReanudar() throws RemoteException {
        if (backend.isPausado()) {
            backend.reanudarSimulacion(); // signalAll() → todos los hilos continúan
        } else {
            backend.pausarSimulacion();   // pausado=true → hilos se bloquean en comprobarPausa()
        }
    }

    @Override
    public boolean isPausado() throws RemoteException {
        return backend.isPausado(); // volatile → lectura consistente sin cerrojo
    }

    // ── Listas de IDs: zonas Hawkins ──────────────────────────────────────────
    // Patrón común: iterar CopyOnWriteArrayList, extraer solo el ID (String),
    // devolver List<String> serializable. Nino no es serializable (tiene Thread).

    @Override
    public List<String> getNinosCallePrincipal() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getHawkins().getCallePrincipal().getListaNinos())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosSotanoByers() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getHawkins().getSotanoByers().getListaNinos())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosRadioWSQK() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getHawkins().getRadioWSQK().getListaNinos())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    // ── Listas de IDs: zonas Upside Down ─────────────────────────────────────

    @Override
    public List<String> getNinosLaboratorio() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getLaboratorio().getListaNinos())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosCentroComercial() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getCentroComercial().getListaNinos())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosBosque() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getBosque().getListaNinos())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosAlcantarillado() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getAlcantarillado().getListaNinos())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    // ── Listas de IDs: Demogorgons por zona ───────────────────────────────────

    @Override
    public List<String> getDemogorgonesLaboratorio() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getLaboratorio().getListaDemogorgons())
            ids.add(((Demogorgon) obj).getId());
        return ids;
    }

    @Override
    public List<String> getDemogorgonesCentroComercial() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getCentroComercial().getListaDemogorgons())
            ids.add(((Demogorgon) obj).getId());
        return ids;
    }

    @Override
    public List<String> getDemogorgonesBosque() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getBosque().getListaDemogorgons())
            ids.add(((Demogorgon) obj).getId());
        return ids;
    }

    @Override
    public List<String> getDemogorgonesAlcantarillado() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getUpsideDown().getAlcantarillado().getListaDemogorgons())
            ids.add(((Demogorgon) obj).getId());
        return ids;
    }

    // ── Portales: colas de ida (conteo e IDs) ─────────────────────────────────
    // getNinosEnColaIda() devuelve CopyOnWriteArrayList → iteración thread-safe

    @Override
    public int getNinosEnColaIdaPortalBosque() throws RemoteException {
        return backend.getHawkins().getPortalBosque().getNinosEnColaIda().size();
    }

    @Override
    public int getNinosEnColaIdaPortalLaboratorio() throws RemoteException {
        return backend.getHawkins().getPortalLaboratorio().getNinosEnColaIda().size();
    }

    @Override
    public int getNinosEnColaIdaPortalCentroComercial() throws RemoteException {
        return backend.getHawkins().getPortalCentroComercial().getNinosEnColaIda().size();
    }

    @Override
    public int getNinosEnColaIdaPortalAlcantarillado() throws RemoteException {
        return backend.getHawkins().getPortalAlcantarillado().getNinosEnColaIda().size();
    }

    @Override
    public List<String> getNinosEnColaIdaPortalBosqueIds() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getHawkins().getPortalBosque().getNinosEnColaIda())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosEnColaIdaPortalLaboratorioIds() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getHawkins().getPortalLaboratorio().getNinosEnColaIda())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosEnColaIdaPortalCentroComercialIds() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getHawkins().getPortalCentroComercial().getNinosEnColaIda())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    @Override
    public List<String> getNinosEnColaIdaPortalAlcantarilladoIds() throws RemoteException {
        List<String> ids = new ArrayList<>();
        for (Object obj : backend.getHawkins().getPortalAlcantarillado().getNinosEnColaIda())
            ids.add(((Nino) obj).getId());
        return ids;
    }

    // ── Portales: niños esperando vuelta (AtomicInteger) ─────────────────────

    @Override
    public int getNinosEsperandoVueltaPortalBosque() throws RemoteException {
        return backend.getHawkins().getPortalBosque().getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosEsperandoVueltaPortalLaboratorio() throws RemoteException {
        return backend.getHawkins().getPortalLaboratorio().getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosEsperandoVueltaPortalCentroComercial() throws RemoteException {
        return backend.getHawkins().getPortalCentroComercial().getNinosEsperandoVuelta();
    }

    @Override
    public int getNinosEsperandoVueltaPortalAlcantarillado() throws RemoteException {
        return backend.getHawkins().getPortalAlcantarillado().getNinosEsperandoVuelta();
    }

    // ── Capacidades de portales (constantes, definidas en SimulacionBackend) ──

    @Override
    public int getCapacidadPortalBosque() throws RemoteException {
        return backend.getHawkins().getPortalBosque().getCapacidad(); // Siempre 2
    }

    @Override
    public int getCapacidadPortalLaboratorio() throws RemoteException {
        return backend.getHawkins().getPortalLaboratorio().getCapacidad(); // Siempre 3
    }

    @Override
    public int getCapacidadPortalCentroComercial() throws RemoteException {
        return backend.getHawkins().getPortalCentroComercial().getCapacidad(); // Siempre 4
    }

    @Override
    public int getCapacidadPortalAlcantarillado() throws RemoteException {
        return backend.getHawkins().getPortalAlcantarillado().getCapacidad(); // Siempre 2
    }

    // ── Sangre y capturas ─────────────────────────────────────────────────────

    @Override
    public int getNumGotasSangre() throws RemoteException {
        // sangreTotal es AtomicInteger en GestorEventos → lectura thread-safe
        return backend.getGestorEventos().getSangreTotal();
    }

    @Override
    public int getNumCapturasColmena() throws RemoteException {
        // Número de niños ACTUALMENTE en la Colmena (puede bajar cuando Eleven libera)
        return backend.getUpsideDown().getColmena().getListaNinos().size();
    }
}
