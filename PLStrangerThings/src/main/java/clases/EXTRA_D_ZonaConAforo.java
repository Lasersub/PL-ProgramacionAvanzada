package clases;

// ============================================================
// IMPLEMENTACIÓN EXTRA — OPCIÓN D: ZonaConAforo
//
// ARCHIVO 1 de 2: ZonaConAforo.java (clase nueva, no modifica nada existente)
// Ver EXTRA_D_SimulacionBackend_Cambios.java para los cambios en el backend.
//
// QUÉ ES ZonaConAforo:
//   Subclase de Zona que añade un límite máximo de niños simultáneos.
//   Si la zona está llena, los niños que intenten entrar se bloquean en
//   acquire() hasta que otro niño salga y llame release().
//
// EJEMPLO DE USO:
//   El Sótano Byers tiene aforo máximo de 10 niños. Si hay 10 preparándose
//   y llega un 11º, espera bloqueado hasta que uno de los 10 salga.
//   Esto modela que el sótano tiene espacio físico limitado.
//
// HERENCIA: extiende Zona
//   → reutiliza toda la gestión thread-safe de listas y cerrojos
//   → solo sobreescribe entrarNino() y salirNino() para añadir el semáforo
//   → Colmena hace lo mismo (extiende Zona y sobreescribe entrarNino)
//     → patrón consistente con el diseño existente
//
// MECANISMO DE CONCURRENCIA AÑADIDO:
//   [Semaphore] Con N permisos = aforo máximo.
//   - entrarNino() → acquire() antes de añadir a la lista
//     Si el semáforo tiene 0 permisos disponibles, el hilo se bloquea aquí
//     hasta que otro niño haga release() en salirNino().
//   - salirNino() → release() después de eliminar de la lista
//     Devuelve un permiso, desbloqueando al primer niño en cola.
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Por qué Semaphore y no otro mecanismo para el aforo?"
//   → Semaphore es exactamente el mecanismo diseñado para controlar acceso
//     concurrente a un recurso con N plazas disponibles. Con N=1 es un mutex;
//     con N>1 permite hasta N hilos simultáneos. Condition/Lock servirían pero
//     necesitaríamos un contador manual + bucle while; Semaphore lo encapsula
//     todo con acquire()/release(), que es más legible y menos propenso a errores.
//
//   "¿Por qué ZonaConAforo extiende Zona y no es independiente?"
//   → Porque ES una Zona con comportamiento extra, igual que Colmena.
//     Hereda toda la infraestructura (listas, cerrojo, condición de ataque)
//     y solo añade lo nuevo: el semáforo de aforo. Además, como es una Zona,
//     puede usarse en cualquier sitio donde se espere una Zona (polimorfismo).
//     Nino.java llama hawkins.getSotanoByers().entrarNino() sin saber si es
//     Zona o ZonaConAforo → el cambio es transparente para el resto del sistema.
// ============================================================

import java.util.concurrent.Semaphore;

/**
 * Zona con aforo máximo: limita el número de niños que pueden estar
 * simultáneamente en ella mediante un {@link Semaphore}.
 * Los niños que intenten entrar cuando la zona esté llena se bloquean
 * hasta que otro niño salga.
 */
public class ZonaConAforo extends Zona {

    // [Semaphore] Controla el aforo. Se inicializa con 'aforoMaximo' permisos.
    // Cada niño que entra consume un permiso (acquire).
    // Cada niño que sale devuelve un permiso (release).
    // Cuando permisos disponibles == 0, el siguiente acquire() bloquea el hilo.
    //
    // fair=true → los hilos bloqueados se atienden en orden FIFO (primero en llegar,
    // primero en entrar). Evita inanición: un niño no puede esperar indefinidamente
    // mientras otros que llegaron después entran antes.
    private final Semaphore aforoSemaforo;

    private final int aforoMaximo; // Guardado para getters / logs

    /**
     * Construye una ZonaConAforo con el límite indicado.
     *
     * @param aforoMaximo número máximo de niños simultáneos permitidos en la zona.
     *                    Si es <= 0, lanzará IllegalArgumentException en Semaphore.
     */
    public ZonaConAforo(int aforoMaximo) {
        super(); // Hereda: listaNinos, listaDemogorgons, cerrojo, condicionFinAtaque
        this.aforoMaximo = aforoMaximo;
        // fair=true: orden FIFO, evita inanición de hilos que llevan mucho esperando
        this.aforoSemaforo = new Semaphore(aforoMaximo, true);
    }

    /**
     * Intenta entrar a la zona. Si está llena (aforo = 0 permisos libres),
     * el hilo se bloquea hasta que otro niño salga.
     *
     * FLUJO:
     *   1. acquire() → consume 1 permiso del semáforo.
     *      Si hay permisos disponibles: continúa inmediatamente.
     *      Si no hay permisos: bloquea el hilo hasta que alguien haga release().
     *   2. super.entrarNino() → añade el niño a la listaNinos (con cerrojo de Zona)
     *
     * ORDEN IMPORTANTE: acquire() ANTES de entrar a la lista.
     * Si fuera al revés (entrar a lista → acquire), el niño ya estaría en la lista
     * pero bloqueado, lo que daría un conteo incorrecto en la UI.
     *
     * @param nino niño que intenta entrar
     */
    @Override
    public void entrarNino(Nino nino) {
        try {
            // [SEMAPHORE acquire()] Bloquea si aforo == 0, continúa si hay plaza
            aforoSemaforo.acquire();
        } catch (InterruptedException e) {
            // El hilo fue interrumpido mientras esperaba plaza (ej. shutdown)
            Thread.currentThread().interrupt();
            return; // No entrar a la zona si fuimos interrumpidos
        }
        // Plaza conseguida → registrar entrada en la lista (heredado de Zona)
        super.entrarNino(nino);
    }

    /**
     * Elimina al niño de la zona y libera su plaza para el siguiente en espera.
     *
     * FLUJO:
     *   1. super.salirNino() → elimina de la listaNinos (con cerrojo de Zona)
     *   2. release() → devuelve 1 permiso al semáforo.
     *      Si hay hilos bloqueados en acquire(), el primero en la cola (FIFO)
     *      se desbloquea y entra.
     *
     * ORDEN IMPORTANTE: release() DESPUÉS de salir de la lista.
     * Garantiza que cuando el siguiente niño entra, el anterior ya no está en la lista.
     *
     * @param nino niño que sale
     */
    @Override
    public void salirNino(Nino nino) {
        // Primero salir de la lista, luego liberar la plaza
        super.salirNino(nino);
        // [SEMAPHORE release()] Devuelve 1 permiso; desbloquea al siguiente en cola
        aforoSemaforo.release();
    }

    // ── Getters informativos ──────────────────────────────────────────────────

    /** @return número máximo de niños permitidos simultáneamente */
    public int getAforoMaximo() { return aforoMaximo; }

    /** @return número de plazas libres en este momento */
    public int getPlazasLibres() { return aforoSemaforo.availablePermits(); }

    /** @return número de niños bloqueados esperando entrar */
    public int getNinosEsperandoEntrar() { return aforoSemaforo.getQueueLength(); }
}
