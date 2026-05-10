package clases;

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

    private final int capacidad;
    private final CyclicBarrier barreraGrupo;
    private final Semaphore turnoIndividual;

    private final Lock cerrojo;
    private final Condition esperaApagonOGrupo;
    private final Condition esperaNuevoGrupo;

    private volatile boolean apagonActivo;
    private boolean grupoCruzando;
    private final AtomicInteger ninosEsperandoVuelta;
    private final AtomicInteger ninosCruzados = new AtomicInteger(0);
    private final List<Nino> ninosEnColaIda = new CopyOnWriteArrayList<>();

    /**
     * Construye un portal con la capacidad de grupo indicada.
     *
     * @param capacidad número de niños que deben reunirse para cruzar juntos hacia el Upside Down
     */
    public Portal(int capacidad) {
        this.capacidad = capacidad;
        this.turnoIndividual = new Semaphore(1);
        this.cerrojo = new ReentrantLock();
        this.esperaApagonOGrupo = cerrojo.newCondition();
        this.esperaNuevoGrupo = cerrojo.newCondition();
        this.apagonActivo = false;
        this.grupoCruzando = false;
        this.ninosEsperandoVuelta = new AtomicInteger(0);

        // La barrierAction se ejecuta cuando llega el último niño del grupo
        // En ese momento, bloqueamos la entrada de nuevos niños al grupo
        this.barreraGrupo = new CyclicBarrier(capacidad, () -> {
            cerrojo.lock();
            try {
                grupoCruzando = true;
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
        ninosEnColaIda.add(nino); // registro en cola de ida

        // FASE 0: Esperar si hay apagón, grupo cruzando o vuelta pendiente
        cerrojo.lock();
        try {
            while (apagonActivo || grupoCruzando || ninosEsperandoVuelta.get() > 0) {
                esperaNuevoGrupo.await();
            }
        } finally {
            cerrojo.unlock();
        }

        // FASE 1: Esperar a formar el grupo (CyclicBarrier)
        try {
            barreraGrupo.await();
        } catch (java.util.concurrent.BrokenBarrierException e) {
            ninosEnColaIda.remove(nino);
            throw new InterruptedException("Barrera rota por apagón");
        }

        // FASE 2: Cruzar de uno en uno (Semaphore)
        turnoIndividual.acquire();
        try {
            Thread.sleep(1000);
        } finally {
            turnoIndividual.release();
            if (ninosCruzados.incrementAndGet() == capacidad) {
                ninosCruzados.set(0);
                cerrojo.lock();
                try {
                    grupoCruzando = false;
                    esperaNuevoGrupo.signalAll();
                } finally {
                    cerrojo.unlock();
                }
            }
        }

        ninosEnColaIda.remove(nino); // sale de la cola al cruzar
    }

    /**
     * Hace cruzar al niño de vuelta a Hawkins de uno en uno, con prioridad sobre
     * los que esperan para ir al Upside Down. Bloquea si hay un apagón activo.
     *
     * @param nino niño que regresa a Hawkins
     * @throws InterruptedException si el hilo es interrumpido mientras espera
     */
    public void cruzarHaciaHawkins(Nino nino) throws InterruptedException {
        ninosEsperandoVuelta.incrementAndGet();
        try {
            // Esperar solo si hay apagón activo o alguien cruzando
            cerrojo.lock();
            try {
                while (apagonActivo) {
                    esperaApagonOGrupo.await();
                }
            } finally {
                cerrojo.unlock();
            }

            // Cruzar de uno en uno con prioridad (ya registrados en ninosEsperandoVuelta)
            turnoIndividual.acquire();
            try {
                Thread.sleep(1000);
            } finally {
                turnoIndividual.release();
            }
        } finally {
            ninosEsperandoVuelta.decrementAndGet();
            // Si ya no hay nadie esperando volver, avisar a los de ida
            if (ninosEsperandoVuelta.get() == 0) {
                cerrojo.lock();
                try {
                    esperaNuevoGrupo.signalAll();
                } finally {
                    cerrojo.unlock();
                }
            }
        }
    }

    /**
     * Activa el apagón: bloquea nuevos cruces y rompe la barrera actual para
     * que los niños en espera reciban una {@link java.util.concurrent.BrokenBarrierException}.
     */
    public void activarApagon() {
        cerrojo.lock();
        try {
            apagonActivo = true;
            barreraGrupo.reset(); // Rompe la barrera actual, los que esperan reciben BrokenBarrierException
        } finally {
            cerrojo.unlock();
        }
    }

    /** Desactiva el apagón y reactiva el portal, notificando a los hilos en espera. */
    public void desactivarApagon() {
        cerrojo.lock();
        try {
            apagonActivo = false;
            ninosCruzados.set(0);
            grupoCruzando = false;
            esperaApagonOGrupo.signalAll();
            esperaNuevoGrupo.signalAll();
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * Devuelve el número de niños que están actualmente esperando en la barrera
     * para completar el grupo de ida.
     *
     * @return niños en espera en la barrera cíclica
     */
    public int getNinosEsperando() {
        return barreraGrupo.getNumberWaiting();
    }

    public int getCapacidad() {
        return capacidad;
    }

    /**
     * Devuelve el número de niños que esperan cruzar de vuelta a Hawkins.
     *
     * @return niños pendientes de vuelta
     */
    public int getNinosEsperandoVuelta() {
        return ninosEsperandoVuelta.get();
    }

    /**
     * Devuelve la lista de niños actualmente en la cola de ida hacia el Upside Down.
     *
     * @return lista de niños en cola de ida (vista no modificable en la práctica)
     */
    public List<Nino> getNinosEnColaIda() {
        return ninosEnColaIda;
    }
}
