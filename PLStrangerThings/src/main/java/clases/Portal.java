package clases;

// ============================================================
// CLASE: Portal.java
// RESPONSABILIDAD: Gestiona el cruce bidireccional de niños entre Hawkins
//   y el Upside Down. Implementa tres restricciones simultáneas:
//     1. Los niños deben cruzar en GRUPOS de tamaño fijo (según el portal)
//     2. Dentro del grupo, cruzan de UNO EN UNO
//     3. Los niños que VUELVEN tienen PRIORIDAD sobre los que van
//   Además, durante un APAGÓN el portal queda completamente bloqueado.
//
// MECANISMOS DE CONCURRENCIA USADOS:
//   [1] CyclicBarrier   → agrupa a los niños; todos esperan hasta completar
//                         el grupo antes de que ninguno cruce
//   [2] Semaphore(1)    → turno individual; solo 1 niño cruza a la vez
//   [3] ReentrantLock   → cerrojo que protege el estado del portal
//   [4] Condition x2   → "esperaApagonOGrupo" (vuelta bloqueada por apagón)
//                         "esperaNuevoGrupo" (ida bloqueada por apagón/grupo cruzando/vuelta pendiente)
//   [5] AtomicInteger   → contador de niños esperando vuelta, actualizado
//                         desde múltiples hilos sin cerrojo adicional
//   [6] volatile boolean → apagonActivo leído sin cerrojo en comprobaciones rápidas
//
// FLUJO COMPLETO IDA (cruzarHaciaUpsideDown):
//   Fase 0 → esperar si apagón / grupo cruzando / hay vuelta pendiente
//   Fase 1 → CyclicBarrier: esperar a completar el grupo
//   Fase 2 → Semaphore: cruzar de uno en uno (1 segundo cada uno)
//   Fase 3 → el último del grupo resetea grupoCruzando y avisa al siguiente grupo
//
// FLUJO COMPLETO VUELTA (cruzarHaciaHawkins):
//   Fase 0 → incrementar ninosEsperandoVuelta (bloquea nuevos grupos de ida)
//   Fase 1 → esperar solo si hay apagón activo
//   Fase 2 → Semaphore: cruzar de uno en uno (1 segundo)
//   Fase 3 → decrementar; si llega a 0, avisar a los grupos de ida
//
// PREGUNTA TÍPICA DE DEFENSA:
//   "¿Cómo implementas la prioridad de vuelta?"
//   → Con AtomicInteger ninosEsperandoVuelta. Cualquier niño de ida comprueba
//     en su Fase 0 si ninosEsperandoVuelta > 0; si es así, espera. Los de vuelta
//     se registran ANTES de intentar cruzar, así que cualquier niño de ida que
//     llegue después los ve y espera. El semáforo turnoIndividual es compartido
//     por ambas direcciones, lo que físicamente impide cruces simultáneos.
//
//   "¿Por qué CyclicBarrier y no CountDownLatch?"
//   → CyclicBarrier es reutilizable automáticamente: cuando un grupo completo
//     cruza, la barrera se reinicia sola para el siguiente grupo. CountDownLatch
//     es de un solo uso; habría que crear uno nuevo por cada grupo.
// ============================================================

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Portal dimensional entre Hawkins y el Upside Down. Los niños cruzan en grupos
 * del tamaño de la capacidad del portal ({@link CyclicBarrier}), de uno en uno
 * ({@link Semaphore}). El cruce de vuelta tiene prioridad sobre el de ida.
 * Durante un apagón el portal queda bloqueado para ambas direcciones.
 */
public class Portal {

    // Número de niños necesarios para formar un grupo y cruzar hacia el Upside Down.
    // Bosque=2, Laboratorio=3, Centro Comercial=4, Alcantarillado=2 (definido en Hawkins.java)
    private final int capacidad;

    // [CyclicBarrier] Barrera que retiene a los niños hasta que se complete el grupo.
    // La "barrierAction" (lambda en el constructor) se ejecuta ATÓMICAMENTE cuando
    // el último niño llega: pone grupoCruzando=true para bloquear al siguiente grupo.
    // Es CyclicBarrier (y no CountDownLatch) porque se reutiliza para cada nuevo grupo.
    private final CyclicBarrier barreraGrupo;

    // [Semaphore(1)] Garantiza que solo 1 niño cruza el portal físicamente a la vez,
    // tanto en la ida como en la vuelta. El valor inicial 1 = un único "permiso".
    // Los niños de vuelta comparten este mismo semáforo → no pueden coexistir
    // en el cruce con los de ida.
    private final Semaphore turnoIndividual;

    // [ReentrantLock] Cerrojo principal del portal. Protege:
    //   - La variable grupoCruzando
    //   - Las esperas/notificaciones de ambas Conditions
    //   - La operación activarApagon (reset de barrera debe ser atómica con el flag)
    private final Lock cerrojo;

    // [Condition] Usada por los niños de VUELTA: se bloquean aquí si hay apagón activo.
    // Se despierta en desactivarApagon().
    private final Condition esperaApagonOGrupo;

    // [Condition] Usada por los niños de IDA: se bloquean aquí si:
    //   - Hay apagón activo, O
    //   - El grupo anterior todavía está cruzando (grupoCruzando=true), O
    //   - Hay niños esperando volver (ninosEsperandoVuelta > 0) → PRIORIDAD VUELTA
    // Se despierta cuando: termina apagón, termina cruce del grupo, o se vacía la vuelta.
    private final Condition esperaNuevoGrupo;

    // [volatile] Flag del apagón. volatile porque es leído desde múltiples hilos
    // sin cerrojo (en comprobaciones de bucle while). La escritura siempre ocurre
    // dentro del cerrojo, pero la lectura es segura gracias a volatile.
    private volatile boolean apagonActivo;

    // Flag que indica que un grupo ya está cruzando. Solo se modifica dentro del cerrojo.
    // Evita que el siguiente grupo empiece a cruzar mientras el anterior no ha terminado.
    private boolean grupoCruzando;

    // [AtomicInteger] Número de niños que han registrado su intención de volver
    // (se incrementa ANTES de cruzar, se decrementa DESPUÉS). AtomicInteger permite
    // leerlo y modificarlo desde hilos de vuelta y de ida sin cerrojo adicional.
    // Es la clave de la implementación de prioridad: los de ida ven este contador
    // en su Fase 0 y esperan si es > 0.
    private final AtomicInteger ninosEsperandoVuelta;

    // [AtomicInteger] Cuenta cuántos niños del grupo actual ya han cruzado.
    // Cuando llega a 'capacidad', el último niño resetea grupoCruzando y avisa al siguiente grupo.
    private final AtomicInteger ninosCruzados = new AtomicInteger(0);

    // [CopyOnWriteArrayList] Lista de niños actualmente en cola de ida.
    // Usada solo para visualización en la UI (PanelInfo/SimulacionBackend).
    // CopyOnWriteArrayList → lecturas de la UI sin bloqueo.
    private final List<Nino> ninosEnColaIda = new CopyOnWriteArrayList<>();

    /**
     * Construye un portal con la capacidad de grupo indicada.
     *
     * @param capacidad número de niños que deben reunirse para cruzar juntos hacia el Upside Down
     */
    public Portal(int capacidad) {
        this.capacidad = capacidad;
        this.turnoIndividual = new Semaphore(1); // 1 permiso = 1 niño a la vez
        this.cerrojo = new ReentrantLock();
        this.esperaApagonOGrupo = cerrojo.newCondition();
        this.esperaNuevoGrupo = cerrojo.newCondition();
        this.apagonActivo = false;
        this.grupoCruzando = false;
        this.ninosEsperandoVuelta = new AtomicInteger(0);

        // La barrierAction se ejecuta cuando llega el ÚLTIMO niño del grupo.
        // Se ejecuta atómicamente dentro de la barrera, antes de liberar a los demás.
        // Pone grupoCruzando=true → los niños del siguiente grupo esperarán en Fase 0.
        this.barreraGrupo = new CyclicBarrier(capacidad, () -> {
            cerrojo.lock();
            try {
                grupoCruzando = true; // Bloquea la entrada del siguiente grupo
            } finally {
                cerrojo.unlock();
            }
        });
    }

    /**
     * Hace cruzar al niño hacia el Upside Down. El niño espera a formar un grupo
     * completo (barrera cíclica) y luego cruza de uno en uno (semáforo).
     * Si se activa un apagón mientras espera, se lanza {@link InterruptedException}.
     *
     * @param nino niño que desea cruzar
     * @throws InterruptedException si el hilo es interrumpido o la barrera se rompe por apagón
     */
    public void cruzarHaciaUpsideDown(Nino nino) throws InterruptedException {
        ninosEnColaIda.add(nino); // Registro visual en cola de ida (para la UI)

        // ── FASE 0: Espera previa al grupo ────────────────────────────────────
        // El niño no puede ni unirse a la barrera si:
        //   - Hay apagón activo
        //   - El grupo anterior todavía está cruzando
        //   - Hay niños esperando volver (prioridad de vuelta)
        // [CONDITION.await()] Libera el cerrojo y duerme hasta que se cumpla la condición
        cerrojo.lock();
        try {
            while (apagonActivo || grupoCruzando || ninosEsperandoVuelta.get() > 0) {
                esperaNuevoGrupo.await();
            }
        } finally {
            cerrojo.unlock();
        }

        // ── FASE 1: Esperar a completar el grupo (CyclicBarrier) ─────────────
        // Todos los niños del grupo se bloquean aquí hasta que llegue el último.
        // Si hay apagón, activarApagon() llama a barreraGrupo.reset() → BrokenBarrierException.
        try {
            barreraGrupo.await(); // [CYCLIC BARRIER] Espera grupal
        } catch (java.util.concurrent.BrokenBarrierException e) {
            // La barrera fue rota por activarApagon(). El niño abandona la cola.
            ninosEnColaIda.remove(nino);
            throw new InterruptedException("Barrera rota por apagón");
        }

        // ── FASE 2: Cruzar de uno en uno (Semaphore) ─────────────────────────
        // Solo 1 niño tiene el permiso a la vez. Los demás esperan en acquire().
        turnoIndividual.acquire(); // [SEMAPHORE] Adquiere permiso de cruce
        try {
            Thread.sleep(1000); // Tiempo de cruce: 1 segundo
        } finally {
            turnoIndividual.release(); // [SEMAPHORE] Libera el permiso para el siguiente

            // ── FASE 3: El último del grupo resetea el portal ─────────────────
            // ninosCruzados llega a 'capacidad' → este niño es el último del grupo.
            // Resetea grupoCruzando y notifica a los niños del siguiente grupo.
            if (ninosCruzados.incrementAndGet() == capacidad) {
                ninosCruzados.set(0); // Reset para el próximo grupo
                cerrojo.lock();
                try {
                    grupoCruzando = false; // El grupo terminó de cruzar
                    esperaNuevoGrupo.signalAll(); // [CONDITION.signalAll()] Avisa al siguiente grupo
                } finally {
                    cerrojo.unlock();
                }
            }
        }

        ninosEnColaIda.remove(nino); // Sale de la cola visual al haber cruzado
    }

    /**
     * Hace cruzar al niño de vuelta a Hawkins de uno en uno, con prioridad sobre
     * los que esperan para ir al Upside Down. Bloquea si hay un apagón activo.
     *
     * @param nino niño que regresa a Hawkins
     * @throws InterruptedException si el hilo es interrumpido mientras espera
     */
    public void cruzarHaciaHawkins(Nino nino) throws InterruptedException {
        // CLAVE DE LA PRIORIDAD: registrarse ANTES de intentar cruzar.
        // Cualquier niño de ida que llegue a su Fase 0 después de este incremento
        // verá ninosEsperandoVuelta > 0 y esperará.
        ninosEsperandoVuelta.incrementAndGet(); // [ATOMIC] Me anoto como "quiero volver"

        try {
            // ── FASE 1: Esperar solo si hay apagón ───────────────────────────
            // A diferencia de la ida, la vuelta NO espera a que termine un grupo
            // cruzando (el semáforo turnoIndividual ya serializa los cruces).
            // Solo se bloquea por apagón.
            cerrojo.lock();
            try {
                while (apagonActivo) {
                    esperaApagonOGrupo.await(); // [CONDITION.await()] Espera fin de apagón
                }
            } finally {
                cerrojo.unlock();
            }

            // ── FASE 2: Cruzar de uno en uno ─────────────────────────────────
            // Mismo semáforo que la ida → no puede haber un niño de ida y uno de
            // vuelta cruzando simultáneamente.
            turnoIndividual.acquire(); // [SEMAPHORE] Adquiere turno de cruce
            try {
                Thread.sleep(1000); // Tiempo de cruce: 1 segundo
            } finally {
                turnoIndividual.release(); // [SEMAPHORE] Libera turno
            }
        } finally {
            // ── FASE 3: Desregistrarse y notificar si ya no queda nadie ──────
            ninosEsperandoVuelta.decrementAndGet(); // [ATOMIC] Ya he cruzado

            // Si soy el último en volver, aviso a los grupos de ida que pueden continuar
            if (ninosEsperandoVuelta.get() == 0) {
                cerrojo.lock();
                try {
                    esperaNuevoGrupo.signalAll(); // [CONDITION.signalAll()] Libera a la ida
                } finally {
                    cerrojo.unlock();
                }
            }
        }
    }

    /**
     * Activa el apagón: bloquea nuevos cruces y rompe la barrera actual para
     * que los niños en espera reciban una {@link java.util.concurrent.BrokenBarrierException}.
     *
     * IMPORTANTE: reset() de la barrera y apagonActivo=true ocurren dentro del
     * mismo cerrojo → no puede haber un niño pasando la barrera justo en este instante.
     */
    public void activarApagon() {
        cerrojo.lock();
        try {
            apagonActivo = true;
            // [CYCLIC BARRIER RESET] Rompe la barrera actual.
            // Los niños bloqueados en barreraGrupo.await() reciben BrokenBarrierException
            // y abortan el cruce limpiamente (ver catch en cruzarHaciaUpsideDown).
            barreraGrupo.reset();
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Desactiva el apagón y reactiva el portal, notificando a todos los hilos en espera.
     * Resetea también el estado del grupo por si quedó inconsistente durante el apagón.
     */
    public void desactivarApagon() {
        cerrojo.lock();
        try {
            apagonActivo = false;
            ninosCruzados.set(0);    // Reset del contador de cruce
            grupoCruzando = false;   // Limpia cualquier estado de grupo anterior
            // Notifica a TODOS los hilos bloqueados en cualquiera de las dos Conditions
            esperaApagonOGrupo.signalAll(); // Despierta niños de vuelta bloqueados por apagón
            esperaNuevoGrupo.signalAll();   // Despierta niños de ida bloqueados por apagón
        } finally {
            cerrojo.unlock();
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    /**
     * Devuelve el número de niños que están actualmente esperando en la barrera
     * para completar el grupo de ida.
     */
    public int getNinosEsperando() {
        return barreraGrupo.getNumberWaiting(); // Método nativo de CyclicBarrier
    }

    public int getCapacidad() {
        return capacidad;
    }

    /**
     * Devuelve el número de niños que esperan cruzar de vuelta a Hawkins.
     */
    public int getNinosEsperandoVuelta() {
        return ninosEsperandoVuelta.get();
    }

    /**
     * Devuelve la lista de niños actualmente en la cola de ida hacia el Upside Down.
     * Usada por SimulacionBackend (RMI) y PanelInfo (UI) para visualización.
     */
    public List<Nino> getNinosEnColaIda() {
        return ninosEnColaIda;
    }
}
