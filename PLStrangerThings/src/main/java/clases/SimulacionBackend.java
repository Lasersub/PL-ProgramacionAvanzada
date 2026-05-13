package clases;

// ============================================================
// CLASE: SimulacionBackend.java
// RESPONSABILIDAD: Núcleo orquestador de la simulación. Crea y arranca todos
//   los hilos (niños, demogorgons, gestor de eventos), gestiona la pausa/
//   reanudación global y supervisa la reproducción de nuevos demogorgons.
//
// IMPLEMENTA: Runnable → se ejecuta en su propio Thread (lanzado desde Main)
//
// SECUENCIA DE ARRANQUE (run):
//   1. Crear y lanzar Demogorgon Alpha (D0000) como hilo daemon
//   2. Crear y lanzar GestorEventos como hilo daemon
//   3. Crear hilo daemon para creación escalonada de 1500 niños (0.5-2s entre cada uno)
//   4. Bucle principal: comprueba pausa + llama comprobarReproduccion() cada 500ms
//
// MECANISMOS DE CONCURRENCIA USADOS:
//   [1] ReentrantLock + Condition (cerrojoPausa / condicionReanudacion)
//       → mecanismo de pausa global: TODOS los hilos llaman comprobarPausa()
//         al inicio de cada ciclo. Si pausado=true, se bloquean en await().
//         reanudarSimulacion() hace signalAll() y todos continúan.
//   [2] volatile boolean pausado → leído por isPausado() desde la UI/RMI sin cerrojo
//   [3] Collections.synchronizedList(demogorgons) → lista de demogorgons leída
//       por GestorEventos y SimulacionBackend concurrentemente; sincronización
//       automática en cada operación individual.
//
// PATRÓN DE PAUSA GLOBAL:
//   Todos los hilos Nino, Demogorgon y GestorEventos llaman comprobarPausa()
//   al inicio de cada iteración de su bucle while(true). Esto garantiza que
//   la simulación se detiene "suavemente" (cada hilo termina su acción actual
//   y se pausa al inicio de la siguiente) sin interrumpir operaciones críticas.
//
// REPRODUCCIÓN DE DEMOGORGONS:
//   Cada 8 niños depositados en la Colmena se crea un nuevo Demogorgon.
//   La lógica usa un contador acumulado (capturasEnColmenaAcumuladas) para
//   no perder capturas entre comprobaciones. Solo accedido por el hilo del
//   bucle principal → no necesita sincronización.
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Por qué usas ReentrantLock para la pausa y no synchronized?"
//   → Porque necesitamos la Condition condicionReanudacion para hacer await()
//     y signalAll() de forma precisa. Con synchronized tendríamos wait/notifyAll
//     pero estarían ligados al monitor del objeto, no a un cerrojo explícito
//     y nombrado. ReentrantLock + Condition es más legible, más flexible y
//     permite múltiples condiciones sobre el mismo cerrojo si fuera necesario.
//
//   "¿Por qué los hilos son daemon?"
//   → Los hilos daemon mueren automáticamente cuando termina el hilo principal
//     (la JVM no espera a que terminen). Como la simulación corre indefinidamente,
//     sin setDaemon(true) la aplicación nunca podría cerrarse.
// ============================================================

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Núcleo de la simulación. Crea y coordina todos los hilos (niños, demogorgons
 * y gestor de eventos), gestiona la pausa/reanudación global mediante un
 * {@link java.util.concurrent.locks.ReentrantLock} y supervisa la reproducción
 * de nuevos demogorgons cuando se acumulan suficientes capturas en la Colmena.
 */
public class SimulacionBackend implements Runnable {

    // ── Infraestructura de log ────────────────────────────────────────────────
    private final LogSimulacion log = new LogSimulacion();

    // ── Zonas de Hawkins ──────────────────────────────────────────────────────
    private final Zona callePrincipal = new Zona();
    private final Zona sotanoByers    = new Zona();
    private final Zona radioWSQK      = new Zona();

    // ── Portales (capacidades según enunciado) ────────────────────────────────
    // Bosque=2, Laboratorio=3, CentroComercial=4, Alcantarillado=2
    private final Portal portalBosque          = new Portal(2);
    private final Portal portalLaboratorio      = new Portal(3);
    private final Portal portalCentroComercial  = new Portal(4);
    private final Portal portalAlcantarillado   = new Portal(2);

    // ── Zonas del Upside Down ─────────────────────────────────────────────────
    private final Zona bosque          = new Zona();
    private final Zona laboratorio     = new Zona();
    private final Zona centroComercial = new Zona();
    private final Zona alcantarillado  = new Zona();
    private final Colmena colmena      = new Colmena(); // Extiende Zona con lógica de rescate

    // ── Contenedores de mundo ─────────────────────────────────────────────────
    // Hawkins y UpsideDown son fachadas que agrupan sus zonas/portales
    // y simplifican el paso de referencias a Nino y Demogorgon.
    private final Hawkins hawkins;
    private final UpsideDown upsideDown;

    // [Collections.synchronizedList] Lista de demogorgons activos.
    // Sincronizada porque GestorEventos la itera (red mental) y SimulacionBackend
    // la modifica (crearNuevoDemogorgon) concurrentemente.
    // Nota: synchronizedList garantiza seguridad en operaciones individuales;
    // para iteraciones hay que sincronizar manualmente el bloque, pero en este
    // caso las lecturas de GestorEventos son tolerantes a inconsistencias leves.
    private final List<Demogorgon> demogorgons =
        Collections.synchronizedList(new ArrayList<>());

    private GestorEventos gestorEventos; // Asignado en run(), antes de que los hilos lo usen

    // ── Control de pausa global ───────────────────────────────────────────────
    // [ReentrantLock + Condition] Mecanismo de pausa/reanudación global.
    // Todos los hilos llaman comprobarPausa() al inicio de cada ciclo.
    // Si pausado=true → await() libera el cerrojo y duerme el hilo.
    // reanudarSimulacion() → signalAll() despierta a todos simultáneamente.
    private final ReentrantLock cerrojoPausa = new ReentrantLock();
    private final Condition condicionReanudacion = cerrojoPausa.newCondition();

    // [volatile] Leído por isPausado() desde la UI/RMI sin cerrojo.
    // La escritura siempre ocurre dentro del cerrojoPausa → coherencia garantizada.
    private volatile boolean pausado = false;

    // ── Control de reproducción de demogorgons ────────────────────────────────
    // Cada CAPTURAS_PARA_NUEVO_DEMOGORGON niños en la Colmena → nuevo Demogorgon.
    // capturasEnColmenaAcumuladas rastrea hasta dónde hemos "cobrado" capturas.
    // Solo accedido por el hilo del bucle principal → no necesita sincronización.
    private int capturasEnColmenaAcumuladas = 0;
    private static final int CAPTURAS_PARA_NUEVO_DEMOGORGON = 8;

    public SimulacionBackend() {
        // Los contenedores reciben las referencias a zonas y portales ya creados.
        // Esto centraliza la creación y evita que Nino/Demogorgon tengan que
        // conocer la estructura interna del mundo.
        this.hawkins = new Hawkins(radioWSQK, callePrincipal, sotanoByers,
            portalBosque, portalLaboratorio, portalCentroComercial, portalAlcantarillado);
        this.upsideDown = new UpsideDown(bosque, laboratorio, centroComercial,
            alcantarillado, colmena);
    }

    /**
     * Arranca la simulación: lanza el demogorgon Alpha, el gestor de eventos,
     * el creador escalonado de niños y entra en el bucle de supervisión de
     * reproducción de demogorgons.
     */
    @Override
    public void run() {
        // ── 1. Demogorgon Alpha ───────────────────────────────────────────────
        // D0000 es el único demogorgon inicial. El resto se crean por reproducción.
        Demogorgon alpha = new Demogorgon("D0000", hawkins, upsideDown, log, this);
        demogorgons.add(alpha);
        Thread hiloAlpha = new Thread(alpha);
        hiloAlpha.setDaemon(true); // Muere al cerrar la aplicación sin detención explícita
        hiloAlpha.start();
        log.registrarEvento("Demogorgon Alpha D0000 creado");

        // ── 2. GestorEventos ─────────────────────────────────────────────────
        // Se asigna a gestorEventos ANTES de lanzar los niños para que no haya
        // un instante en que un Nino lea gestorEventos==null (aunque hay null-check).
        gestorEventos = new GestorEventos(hawkins, upsideDown, demogorgons, log, this);
        Thread hiloEventos = new Thread(gestorEventos);
        hiloEventos.setDaemon(true);
        hiloEventos.start();

        // ── 3. Creación escalonada de 1500 niños ─────────────────────────────
        // En hilo separado para no bloquear el bucle de reproducción.
        // La lambda captura 'this' implícitamente para llamar crearNinosEscalonados().
        Thread hiloCreador = new Thread(() -> crearNinosEscalonados());
        hiloCreador.setDaemon(true);
        hiloCreador.start();

        // ── 4. Bucle de supervisión: reproducción de demogorgons ──────────────
        // Cada 500ms comprueba si se han acumulado suficientes capturas para
        // generar un nuevo Demogorgon. Es el "latido" del backend.
        try {
            while (true) {
                comprobarPausa(); // Se bloquea aquí si la simulación está pausada
                Thread.sleep(500);
                comprobarReproduccion();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Crea los 1500 niños de forma escalonada, con intervalos aleatorios de 0.5-2s.
     * Se ejecuta en su propio hilo daemon para no bloquear el bucle principal.
     * Respeta la pausa global antes de crear cada niño.
     *
     * CRÍTICO: nino.setMiHilo(hiloNino) se llama ANTES de hiloNino.start().
     * Si se llamara después, el Demogorgon podría intentar interrupt() antes de
     * que miHilo estuviera asignado → NullPointerException.
     */
    private void crearNinosEscalonados() {
        for (int i = 1; i <= 1500; i++) {
            try {
                comprobarPausa();
                long espera = ThreadLocalRandom.current().nextLong(500, 2001);
                Thread.sleep(espera);

                String id = String.format("N%04d", i);
                Nino nino = new Nino(id, hawkins, upsideDown, log, this);
                Thread hiloNino = new Thread(nino);
                hiloNino.setDaemon(true);

                // CRÍTICO: asignar la referencia al Thread ANTES de arrancarlo.
                // Demogorgon.run() puede llamar ninoAtacado.getMiHilo().interrupt()
                // en cualquier momento tras el start(); si miHilo fuera null → NPE.
                nino.setMiHilo(hiloNino);
                hiloNino.start();

                log.registrarEvento("Niño " + id + " creado");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Comprueba si las capturas acumuladas en la Colmena justifican crear
     * nuevos demogorgons (1 por cada CAPTURAS_PARA_NUEVO_DEMOGORGON = 8).
     * Solo llamado desde el hilo del bucle principal → sin sincronización.
     */
    private void comprobarReproduccion() {
        if (gestorEventos == null) return;

        // getTotalDepositados() es AtomicInteger → lectura thread-safe
        int capturasActuales = colmena.getTotalDepositados();
        int nuevas = capturasActuales - capturasEnColmenaAcumuladas;

        if (nuevas >= CAPTURAS_PARA_NUEVO_DEMOGORGON) {
            int cuantos = nuevas / CAPTURAS_PARA_NUEVO_DEMOGORGON;
            // Avanzar el marcador exactamente lo que vamos a "cobrar"
            capturasEnColmenaAcumuladas += cuantos * CAPTURAS_PARA_NUEVO_DEMOGORGON;
            for (int i = 0; i < cuantos; i++) {
                crearNuevoDemogorgon();
            }
        }
    }

    /**
     * Instancia y arranca un nuevo Demogorgon. Los IDs se generan con
     * Demogorgon.generarId() que usa un contador estático incremental.
     */
    private void crearNuevoDemogorgon() {
        String id = Demogorgon.generarId(); // "D0001", "D0002", etc.
        Demogorgon nuevo = new Demogorgon(id, hawkins, upsideDown, log, this);
        demogorgons.add(nuevo); // [synchronizedList] Thread-safe add
        Thread hilo = new Thread(nuevo);
        hilo.setDaemon(true);
        hilo.start();
        log.registrarEvento("Nuevo Demogorgon " + id + " creado por Vecna");
    }

    // ── Pausa / Reanudación global ────────────────────────────────────────────

    /**
     * Bloquea el hilo llamante mientras la simulación esté pausada.
     * Invocado al inicio de cada iteración por Nino, Demogorgon y GestorEventos.
     * Garantiza que la pausa es "suave": cada hilo termina su acción actual
     * antes de bloquearse.
     *
     * @throws InterruptedException si el hilo es interrumpido mientras espera
     */
    public void comprobarPausa() throws InterruptedException {
        // [LOCK] Adquirir cerrojo para comprobar el flag y, si procede, esperar
        cerrojoPausa.lock();
        try {
            // Bucle while: protección contra spurious wakeups (mismo patrón que en Zona)
            while (pausado) {
                condicionReanudacion.await(); // [CONDITION.await()] Libera cerrojo y duerme
            }
        } finally {
            cerrojoPausa.unlock();
        }
    }

    /**
     * Pausa la simulación. Los hilos que llamen a comprobarPausa() en su
     * siguiente iteración quedarán bloqueados en condicionReanudacion.await().
     */
    public void pausarSimulacion() {
        cerrojoPausa.lock();
        try {
            pausado = true; // Los hilos verán este cambio en su próxima comprobarPausa()
        } finally {
            cerrojoPausa.unlock();
        }
    }

    /**
     * Reanuda la simulación y desbloquea todos los hilos en espera.
     * signalAll() despierta a TODOS los hilos bloqueados en condicionReanudacion.
     */
    public void reanudarSimulacion() {
        cerrojoPausa.lock();
        try {
            pausado = false;
            condicionReanudacion.signalAll(); // [CONDITION.signalAll()] Todos los hilos continúan
        } finally {
            cerrojoPausa.unlock();
        }
    }

    /** @return true si la simulación está actualmente pausada (leído desde UI/RMI) */
    public boolean isPausado() { return pausado; } // volatile → lectura sin cerrojo segura

    // ── Getters para la capa RMI (SimulacionRemota) y la UI ──────────────────
    public Hawkins getHawkins()             { return hawkins; }
    public UpsideDown getUpsideDown()       { return upsideDown; }
    public GestorEventos getGestorEventos() { return gestorEventos; }
    public List<Demogorgon> getDemogorgons(){ return demogorgons; }
    public LogSimulacion getLog()           { return log; }
}
