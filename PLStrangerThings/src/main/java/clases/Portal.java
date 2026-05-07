package clases;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    public void activarApagon() {
        cerrojo.lock();
        try {
            apagonActivo = true;
            barreraGrupo.reset(); // Rompe la barrera actual, los que esperan reciben BrokenBarrierException
        } finally {
            cerrojo.unlock();
        }
    }

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

    public int getNinosEsperando() {
        return barreraGrupo.getNumberWaiting();
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getNinosEsperandoVuelta() {
        return ninosEsperandoVuelta.get();
    }

    public List<Nino> getNinosEnColaIda() {
        return ninosEnColaIda;
    }
}
