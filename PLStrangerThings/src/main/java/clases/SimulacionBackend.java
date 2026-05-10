package clases;

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

    // --- Infraestructura ---
    private final LogSimulacion log = new LogSimulacion();

    // Zonas Hawkins
    private final Zona callePrincipal = new Zona();
    private final Zona sotanoByers    = new Zona();
    private final Zona radioWSQK      = new Zona();

    // Portales (capacidades según enunciado)
    private final Portal portalBosque          = new Portal(2);
    private final Portal portalLaboratorio      = new Portal(3);
    private final Portal portalCentroComercial  = new Portal(4);
    private final Portal portalAlcantarillado   = new Portal(2);

    // Zonas UpsideDown
    private final Zona bosque          = new Zona();
    private final Zona laboratorio     = new Zona();
    private final Zona centroComercial = new Zona();
    private final Zona alcantarillado  = new Zona();
    private final Colmena colmena      = new Colmena();

    // Contenedores
    private final Hawkins hawkins;
    private final UpsideDown upsideDown;

    // Lista de demogorgons (sincronizada para acceso concurrente)
    private final List<Demogorgon> demogorgons =
        Collections.synchronizedList(new ArrayList<>());

    // Gestor de eventos globales
    private GestorEventos gestorEventos;

    // --- Control de pausa (ReentrantLock + Condition, no synchronized) ---
    private final ReentrantLock cerrojoPausa = new ReentrantLock();
    private final Condition condicionReanudacion = cerrojoPausa.newCondition();
    private volatile boolean pausado = false;

    // --- Control de reproducción de demogorgons ---
    private int capturasEnColmenaAcumuladas = 0;
    private static final int CAPTURAS_PARA_NUEVO_DEMOGORGON = 8;

    public SimulacionBackend() {
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
        // 1. Lanzar Demogorgon Alpha
        Demogorgon alpha = new Demogorgon("D0000", hawkins, upsideDown, log, this);
        demogorgons.add(alpha);
        Thread hiloAlpha = new Thread(alpha);
        hiloAlpha.setDaemon(true);
        hiloAlpha.start();
        log.registrarEvento("Demogorgon Alpha D0000 creado");

        // 2. Lanzar gestor de eventos globales
        gestorEventos = new GestorEventos(hawkins, upsideDown, demogorgons, log);
        Thread hiloEventos = new Thread(gestorEventos);
        hiloEventos.setDaemon(true);
        hiloEventos.start();

        // 3. Lanzar creación escalonada de niños en hilo separado
        Thread hiloCreador = new Thread(() -> crearNinosEscalonados());
        hiloCreador.setDaemon(true);
        hiloCreador.start();

        // 4. Bucle principal: vigilar reproducciones de demogorgons
        try {
            while (true) {
                comprobarPausa();
                Thread.sleep(500);
                comprobarReproduccion();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void crearNinosEscalonados() {
        for (int i = 1; i <= 1500; i++) {
            try {
                long espera = ThreadLocalRandom.current().nextLong(500, 2001);
                Thread.sleep(espera);

                String id = String.format("N%04d", i);
                Nino nino = new Nino(id, hawkins, upsideDown, log, this);
                Thread hiloNino = new Thread(nino);
                hiloNino.setDaemon(true);
                nino.setMiHilo(hiloNino); // CRÍTICO: asignar referencia antes de start()
                hiloNino.start();

                log.registrarEvento("Niño " + id + " creado");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void comprobarReproduccion() {
        if (gestorEventos == null) return;
        // La colmena lleva la cuenta de cuántos niños han sido depositados
        int capturasActuales = colmena.getTotalDepositados();
        int nuevas = capturasActuales - capturasEnColmenaAcumuladas;
        if (nuevas >= CAPTURAS_PARA_NUEVO_DEMOGORGON) {
            int cuantos = nuevas / CAPTURAS_PARA_NUEVO_DEMOGORGON;
            capturasEnColmenaAcumuladas += cuantos * CAPTURAS_PARA_NUEVO_DEMOGORGON;
            for (int i = 0; i < cuantos; i++) {
                crearNuevoDemogorgon();
            }
        }
    }

    private void crearNuevoDemogorgon() {
        String id = Demogorgon.generarId();
        Demogorgon nuevo = new Demogorgon(id, hawkins, upsideDown, log, this);
        demogorgons.add(nuevo);
        Thread hilo = new Thread(nuevo);
        hilo.setDaemon(true);
        hilo.start();
        log.registrarEvento("Nuevo Demogorgon " + id + " creado por Vecna");
    }

    // --- Pausa / Reanudación ---
    /**
     * Bloquea el hilo llamante mientras la simulación esté pausada.
     * Debe invocarse al inicio de cada iteración de los hilos de niños y demogorgons.
     *
     * @throws InterruptedException si el hilo es interrumpido mientras espera
     */
    public void comprobarPausa() throws InterruptedException {
        cerrojoPausa.lock();
        try {
            while (pausado) {
                condicionReanudacion.await();
            }
        } finally {
            cerrojoPausa.unlock();
        }
    }

    /** Pausa la simulación: los hilos que llamen a {@link #comprobarPausa} quedarán bloqueados. */
    public void pausarSimulacion() {
        cerrojoPausa.lock();
        try {
            pausado = true;
        } finally {
            cerrojoPausa.unlock();
        }
    }

    /** Reanuda la simulación y desbloquea todos los hilos en espera. */
    public void reanudarSimulacion() {
        cerrojoPausa.lock();
        try {
            pausado = false;
            condicionReanudacion.signalAll();
        } finally {
            cerrojoPausa.unlock();
        }
    }

    public boolean isPausado() { return pausado; }

    // --- Getters para la interfaz ---
    public Hawkins getHawkins()             { return hawkins; }
    public UpsideDown getUpsideDown()       { return upsideDown; }
    public GestorEventos getGestorEventos() { return gestorEventos; }
    public List<Demogorgon> getDemogorgons(){ return demogorgons; }
    public LogSimulacion getLog()           { return log; }
}
