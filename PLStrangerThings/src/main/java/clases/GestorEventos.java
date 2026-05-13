package clases;

// ============================================================
// CLASE: GestorEventos.java
// RESPONSABILIDAD: Hilo daemon que lanza eventos globales aleatorios cada
//   30-60 segundos. Cada evento dura 5-10 segundos y altera el comportamiento
//   de TODOS los hilos Nino y Demogorgon activos simultáneamente.
//
// IMPLEMENTA: Runnable → se ejecuta en su propio hilo (lanzado desde Main/Hawkins)
//
// LOS 4 EVENTOS:
//   1. APAGON_LABORATORIO   → bloquea todos los portales (Portal.activarApagon)
//   2. TORMENTA_UPSIDE_DOWN → duplica tiempo de recolección (Nino lo consulta)
//                             + reduce tiempo entre ataques (Demogorgon lo consulta)
//   3. INTERVENCION_ELEVEN  → libera niños de la Colmena + paraliza Demogorgons
//   4. RED_MENTAL           → todos los Demogorgons van a la zona con más niños
//
// MECANISMOS DE CONCURRENCIA USADOS:
//   [1] volatile boolean (x4) → flags leídos constantemente por hilos Nino y
//       Demogorgon sin cerrojo. volatile garantiza visibilidad inmediata entre
//       hilos: cuando GestorEventos escribe apagonActivo=true, todos los hilos
//       que lo lean en ese instante verán el valor actualizado.
//   [2] volatile TipoEvento / long → estado del evento actual, también leído
//       desde la capa RMI (SimulacionBackend) para mostrarlo en el cliente
//   [3] AtomicInteger sangreTotal → acumulador de sangre recolectada por todos
//       los Niños. addAndGet() y getAndSet() son atómicos sin cerrojo.
//
// PATRÓN DE COMUNICACIÓN CON OTROS HILOS:
//   GestorEventos NO llama métodos en los hilos Nino/Demogorgon directamente.
//   En su lugar, escribe flags volatile y los hilos los leen en sus propios
//   bucles. Es comunicación por memoria compartida → desacoplamiento total.
//   Excepción: APAGON llama Portal.activarApagon() directamente porque necesita
//   romper la CyclicBarrier (no basta con un flag).
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Por qué usas volatile y no synchronized para los flags?"
//   → Porque solo necesitamos VISIBILIDAD, no exclusión mutua. Los flags son
//     escritos por un único hilo (GestorEventos) y leídos por muchos (Nino,
//     Demogorgon). Con volatile es suficiente y más eficiente: no hay bloqueos,
//     no hay riesgo de deadlock.
//     Si hubiera lectura-modificación-escritura (ej. flag++ ), necesitaríamos
//     AtomicInteger o synchronized. Para boolean simple, volatile basta.
//
//   "¿Por qué sangreTotal es AtomicInteger y no volatile int?"
//   → Porque se hace addAndGet() (leer + sumar + escribir) desde múltiples hilos
//     Nino concurrentemente. volatile solo garantiza visibilidad, NO atomicidad
//     de operaciones compuestas. AtomicInteger garantiza ambas.
// ============================================================

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hilo que lanza eventos globales aleatorios a intervalos de entre 30 y 60 segundos.
 * Los cuatro eventos posibles son: apagón en el laboratorio, tormenta en el Upside Down,
 * intervención de Eleven y activación de la red mental. Cada evento dura entre 5 y 10 segundos
 * y modifica el comportamiento de niños y demogorgons a través de flags {@code volatile}.
 */
public class GestorEventos implements Runnable {

    // Enum de eventos. NINGUNO es el estado por defecto (sin evento activo).
    // Los valores del 1 al 4 se seleccionan aleatoriamente en elegirEvento().
    public enum TipoEvento {
        NINGUNO, APAGON_LABORATORIO, TORMENTA_UPSIDE_DOWN, INTERVENCION_ELEVEN, RED_MENTAL
    }

    // ── Estado observable del evento actual ──────────────────────────────────
    // [volatile] Leído desde SimulacionBackend (hilo RMI) para mostrarlo en el
    // cliente remoto. volatile garantiza que el cliente siempre ve el valor actual.
    private volatile TipoEvento eventoActual = TipoEvento.NINGUNO;
    private volatile long duracionEvento = 0;  // Duración del evento en ms
    private volatile long inicioEvento = 0;    // Timestamp de inicio (para calcular tiempo restante)

    // ── Flags de evento (leídos por Nino y Demogorgon en sus bucles) ─────────
    // [volatile] Escritos SOLO por GestorEventos, leídos por MUCHOS hilos.
    // volatile garantiza visibilidad sin necesidad de cerrojo.
    // Cada flag activa un comportamiento distinto en los hilos que lo consultan:
    private volatile boolean apagonActivo = false;    // Nino: no puede cruzar portal
    private volatile boolean tormentaActiva = false;  // Nino: duplica tiempo recolección
                                                      // Demogorgon: reduce tiempo entre ataques
    private volatile boolean elevenActiva = false;    // Demogorgon: paralizado (no ataca ni se mueve)
    private volatile boolean redMentalActiva = false; // Demogorgon: va a la zona con más niños

    // [AtomicInteger] Sangre recolectada globalmente por todos los niños.
    // addAndGet() desde múltiples hilos Nino concurrentemente → necesita atomicidad.
    // getAndSet(0) en liberarNinosConEleven() → lee y resetea atómicamente.
    // NO podría ser volatile int: i++ no es atómico (leer-incrementar-escribir son 3 pasos).
    private final AtomicInteger sangreTotal = new AtomicInteger(0);

    // Referencias al mundo para poder activar/desactivar efectos directamente
    private final Hawkins hawkins;          // Acceso a los 4 portales (apagón)
    private final UpsideDown upsideDown;    // Acceso a la Colmena (Eleven)
    private final List<Demogorgon> listaDemogorgons; // Para red mental (zona con más niños)
    private final LogSimulacion log;
    private SimulacionBackend backend;      // Para comprobar pausa antes de cada ciclo

    // [volatile] Flag de vida del hilo. detener() lo pone a false y el bucle termina.
    private volatile boolean activo = true;

    /**
     * Construye el gestor de eventos con las referencias necesarias para activar
     * y desactivar los efectos de cada evento sobre el mundo.
     *
     * @param hawkins         referencia al mundo de Hawkins (para los portales)
     * @param upsideDown      referencia al Upside Down (para la Colmena)
     * @param listaDemogorgons lista de demogorgons activos en la simulación
     * @param log             registro de eventos de la simulación
     * @param backend         backend de la simulación para comprobar la pausa
     */
    public GestorEventos(Hawkins hawkins, UpsideDown upsideDown,
                         List<Demogorgon> listaDemogorgons, LogSimulacion log,
                         SimulacionBackend backend) {
        this.hawkins = hawkins;
        this.upsideDown = upsideDown;
        this.listaDemogorgons = listaDemogorgons;
        this.log = log;
        this.backend = backend;
    }

    /**
     * Bucle principal del gestor: espera un intervalo aleatorio, elige un evento,
     * lo activa durante su duración y lo desactiva. Se detiene cuando {@link #detener}
     * pone {@code activo} a {@code false} o el hilo es interrumpido.
     */
    @Override
    public void run() {
        try {
            while (activo) {
                // Comprueba si la simulación está pausada antes de cada ciclo.
                // Si está pausada, este hilo se bloquea en backend.comprobarPausa()
                // hasta que se reanude (igual que Nino y Demogorgon).
                backend.comprobarPausa();

                // Esperar entre 30 y 60 segundos hasta el siguiente evento.
                // ThreadLocalRandom es más eficiente que Random en entornos multihilo
                // (cada hilo tiene su propia instancia, sin contención).
                long espera = ThreadLocalRandom.current().nextLong(30000, 60001);
                Thread.sleep(espera);

                // ── Ciclo de un evento ────────────────────────────────────────
                TipoEvento tipo = elegirEvento();
                duracionEvento = ThreadLocalRandom.current().nextLong(5000, 10001);
                inicioEvento = System.currentTimeMillis(); // Para calcular segundos restantes
                eventoActual = tipo; // [volatile write] Visible para el cliente RMI inmediatamente

                log.registrarEvento("EVENTO GLOBAL: " + tipo.name() + " iniciado");
                activarEvento(tipo);   // Escribe flags volatile + efectos directos

                Thread.sleep(duracionEvento); // El evento dura entre 5 y 10 segundos

                desactivarEvento(tipo); // Limpia flags volatile + deshace efectos directos
                eventoActual = TipoEvento.NINGUNO; // [volatile write] El cliente RMI verá "sin evento"
                log.registrarEvento("EVENTO GLOBAL: " + tipo.name() + " finalizado");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Buena práctica: restaurar el flag de interrupción
        }
    }

    /**
     * Elige un evento aleatorio entre los 4 posibles (excluye NINGUNO).
     * TipoEvento.values() = [NINGUNO, APAGON, TORMENTA, ELEVEN, RED_MENTAL]
     * → r ∈ {0,1,2,3} → values()[r+1] siempre cae en un evento real.
     */
    private TipoEvento elegirEvento() {
        int r = ThreadLocalRandom.current().nextInt(4);
        return TipoEvento.values()[r + 1]; // +1 para saltar NINGUNO (índice 0)
    }

    /**
     * Activa los efectos del evento indicado.
     * Para APAGON: llama directamente a Portal.activarApagon() porque hay que
     *   romper la CyclicBarrier activa (un flag volatile no bastaría).
     * Para el resto: basta con escribir el flag volatile; los hilos lo leerán
     *   en su próxima iteración y ajustarán su comportamiento.
     */
    private void activarEvento(TipoEvento tipo) {
        switch (tipo) {
            case APAGON_LABORATORIO:
                apagonActivo = true; // [volatile write] Nino dejará de intentar cruzar
                // Llamada directa a cada portal: necesario para romper CyclicBarriers activas.
                // Los niños bloqueados en barreraGrupo.await() reciben BrokenBarrierException.
                hawkins.getPortalBosque().activarApagon();
                hawkins.getPortalLaboratorio().activarApagon();
                hawkins.getPortalCentroComercial().activarApagon();
                hawkins.getPortalAlcantarillado().activarApagon();
                break;

            case TORMENTA_UPSIDE_DOWN:
                // [volatile write] Nino duplicará su tiempo de recolección consultando isTormentaActiva()
                // Demogorgon reducirá su tiempo de espera entre ataques consultando isTormentaActiva()
                tormentaActiva = true;
                break;

            case INTERVENCION_ELEVEN:
                elevenActiva = true; // [volatile write] Demogorgon se paraliza al consultar isElevenActiva()
                liberarNinosConEleven(); // Efecto inmediato: libera niños de la Colmena
                break;

            case RED_MENTAL:
                // [volatile write] Demogorgon consultará isRedMentalActiva() y, si es true,
                // irá a la zona con más niños en lugar de moverse aleatoriamente
                redMentalActiva = true;
                break;
        }
    }

    /**
     * Desactiva los efectos del evento: resetea flags y deshace cambios directos.
     */
    private void desactivarEvento(TipoEvento tipo) {
        switch (tipo) {
            case APAGON_LABORATORIO:
                apagonActivo = false;
                // desactivarApagon() notifica a los hilos bloqueados en los portales
                // para que reanuden su intento de cruce
                hawkins.getPortalBosque().desactivarApagon();
                hawkins.getPortalLaboratorio().desactivarApagon();
                hawkins.getPortalCentroComercial().desactivarApagon();
                hawkins.getPortalAlcantarillado().desactivarApagon();
                break;
            case TORMENTA_UPSIDE_DOWN:
                tormentaActiva = false;
                break;
            case INTERVENCION_ELEVEN:
                elevenActiva = false; // Demogorgons se desparalizan
                break;
            case RED_MENTAL:
                redMentalActiva = false; // Demogorgons vuelven a moverse aleatoriamente
                break;
        }
    }

    /**
     * Lógica de Eleven: toma toda la sangre acumulada y la usa como "crédito"
     * para liberar ese mismo número de niños de la Colmena.
     *
     * getAndSet(0) es ATÓMICO: lee el valor actual y lo resetea a 0 en un solo paso.
     * Sin AtomicInteger, podría ocurrir que dos operaciones leyeran el mismo valor
     * antes de que alguna lo reseteara (race condition).
     */
    private void liberarNinosConEleven() {
        // [ATOMIC getAndSet] Lee la sangre acumulada y la resetea a 0 atómicamente.
        // Ningún Nino que esté haciendo addAndGet() concurrentemente perderá su sangre:
        // o la añade antes del getAndSet (y se cuenta) o después (próximo evento de Eleven).
        int ninosALiberar = sangreTotal.getAndSet(0);
        log.registrarEvento("ELEVEN libera hasta " + ninosALiberar + " niños de la Colmena");
        upsideDown.getColmena().liberarNinos(ninosALiberar, hawkins.getCallePrincipal(), log);
    }

    // ── Getters para que Nino y Demogorgon consulten el estado ───────────────
    // Todos devuelven volatile → lectura siempre del valor más reciente en memoria principal
    public boolean isApagonActivo()   { return apagonActivo; }
    public boolean isTormentaActiva() { return tormentaActiva; }
    public boolean isElevenActiva()   { return elevenActiva; }
    public boolean isRedMentalActiva(){ return redMentalActiva; }
    public TipoEvento getEventoActual(){ return eventoActual; }

    /**
     * Calcula los segundos que faltan para que termine el evento activo.
     * Usado por SimulacionBackend para mostrarlo en el cliente RMI.
     *
     * @return segundos restantes, o {@code 0} si no hay evento activo
     */
    public long getSegundosRestantes() {
        if (eventoActual == TipoEvento.NINGUNO) return 0;
        long transcurrido = System.currentTimeMillis() - inicioEvento;
        return Math.max(0, (duracionEvento - transcurrido) / 1000);
    }

    /**
     * Añade unidades de sangre al contador global.
     * Llamado por cada Nino al recolectar sangre en el Upside Down.
     * [ATOMIC addAndGet] Thread-safe sin cerrojo.
     *
     * @param cantidad unidades de sangre a añadir
     */
    public void agregarSangre(int cantidad) {
        sangreTotal.addAndGet(cantidad); // [ATOMIC] Suma thread-safe
    }

    /**
     * Devuelve el total acumulado de sangre recolectada por todos los niños.
     * Leído por SimulacionBackend para mostrarlo en la UI.
     */
    public int getSangreTotal() { return sangreTotal.get(); }

    /** Indica al gestor que debe detener su bucle en la próxima iteración. */
    public void detener() { activo = false; }
}
