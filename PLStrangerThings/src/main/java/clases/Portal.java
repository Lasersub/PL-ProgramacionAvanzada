package clases;

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
        // FASE 0: Esperar si hay apagón, si hay un grupo cruzando ahora mismo,
        // o si hay niños esperando para volver (prioridad de vuelta)
        cerrojo.lock();
        try {
            while (apagonActivo || grupoCruzando || ninosEsperandoVuelta.get() > 0) {
                esperaNuevoGrupo.await();
            }
        } finally {
            cerrojo.unlock();
        }

        // FASE 1: Esperar a formar el grupo (CyclicBarrier)
        // Si el apagón se activa mientras esperamos, la barrera quedará rota
        // El BrokenBarrierException se trata como interrupción
        try {
            barreraGrupo.await();
        } catch (java.util.concurrent.BrokenBarrierException e) {
            throw new InterruptedException("Barrera rota por apagón");
        }

        // FASE 2: Cruzar de uno en uno (Semaphore)
        turnoIndividual.acquire();
        try {
            Thread.sleep(1000);
        } finally {
            turnoIndividual.release();
        }

        // Último niño del grupo en cruzar: libera el grupo
        cerrojo.lock();
        try {
            // Si la barrera ya está lista para el siguiente ciclo (todos cruzaron)
            // Comprobamos cuántos quedan en el semáforo (indirectamente via contador)
            // Usamos el número de waiting en la barrera como indicador
            if (barreraGrupo.getNumberWaiting() == 0 && turnoIndividual.availablePermits() == 1) {
                grupoCruzando = false;
                esperaNuevoGrupo.signalAll();
            }
        } finally {
            cerrojo.unlock();
        }
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
}
