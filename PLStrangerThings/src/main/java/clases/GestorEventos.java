package clases;

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

    public enum TipoEvento {
        NINGUNO, APAGON_LABORATORIO, TORMENTA_UPSIDE_DOWN, INTERVENCION_ELEVEN, RED_MENTAL
    }

    // Estado observable del evento actual (volatile para visibilidad entre hilos)
    private volatile TipoEvento eventoActual = TipoEvento.NINGUNO;
    private volatile long duracionEvento = 0;
    private volatile long inicioEvento = 0;

    // Flags que los hilos de Nino y Demogorgon consultarán
    private volatile boolean apagonActivo = false;
    private volatile boolean tormentaActiva = false;
    private volatile boolean elevenActiva = false;
    private volatile boolean redMentalActiva = false;

    // Sangre recolectada globalmente (AtomicInteger para operaciones sin lock)
    private final AtomicInteger sangreTotal = new AtomicInteger(0);

    private final Hawkins hawkins;
    private final UpsideDown upsideDown;
    private final List<Demogorgon> listaDemogorgons;
    private final LogSimulacion log;

    private volatile boolean activo = true;

    /**
     * Construye el gestor de eventos con las referencias necesarias para activar
     * y desactivar los efectos de cada evento sobre el mundo.
     *
     * @param hawkins         referencia al mundo de Hawkins (para los portales)
     * @param upsideDown      referencia al Upside Down (para la Colmena)
     * @param listaDemogorgons lista de demogorgons activos en la simulación
     * @param log             registro de eventos de la simulación
     */
    public GestorEventos(Hawkins hawkins, UpsideDown upsideDown,
                         List<Demogorgon> listaDemogorgons, LogSimulacion log) {
        this.hawkins = hawkins;
        this.upsideDown = upsideDown;
        this.listaDemogorgons = listaDemogorgons;
        this.log = log;
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
                // Esperar entre 30 y 60 segundos hasta el siguiente evento
                long espera = ThreadLocalRandom.current().nextLong(30000, 60001);
                Thread.sleep(espera);

                // Elegir evento aleatorio
                TipoEvento tipo = elegirEvento();
                duracionEvento = ThreadLocalRandom.current().nextLong(5000, 10001);
                inicioEvento = System.currentTimeMillis();
                eventoActual = tipo;

                log.registrarEvento("EVENTO GLOBAL: " + tipo.name() + " iniciado");
                activarEvento(tipo);

                Thread.sleep(duracionEvento);

                desactivarEvento(tipo);
                eventoActual = TipoEvento.NINGUNO;
                log.registrarEvento("EVENTO GLOBAL: " + tipo.name() + " finalizado");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private TipoEvento elegirEvento() {
        int r = ThreadLocalRandom.current().nextInt(4);
        return TipoEvento.values()[r + 1]; // +1 para saltar NINGUNO
    }

    private void activarEvento(TipoEvento tipo) {
        switch (tipo) {
            case APAGON_LABORATORIO:
                apagonActivo = true;
                hawkins.getPortalBosque().activarApagon();
                hawkins.getPortalLaboratorio().activarApagon();
                hawkins.getPortalCentroComercial().activarApagon();
                hawkins.getPortalAlcantarillado().activarApagon();
                break;
            case TORMENTA_UPSIDE_DOWN:
                tormentaActiva = true;
                break;
            case INTERVENCION_ELEVEN:
                elevenActiva = true;
                liberarNinosConEleven();
                break;
            case RED_MENTAL:
                redMentalActiva = true;
                break;
        }
    }

    private void desactivarEvento(TipoEvento tipo) {
        switch (tipo) {
            case APAGON_LABORATORIO:
                apagonActivo = false;
                hawkins.getPortalBosque().desactivarApagon();
                hawkins.getPortalLaboratorio().desactivarApagon();
                hawkins.getPortalCentroComercial().desactivarApagon();
                hawkins.getPortalAlcantarillado().desactivarApagon();
                break;
            case TORMENTA_UPSIDE_DOWN:
                tormentaActiva = false;
                break;
            case INTERVENCION_ELEVEN:
                elevenActiva = false;
                break;
            case RED_MENTAL:
                redMentalActiva = false;
                break;
        }
    }

    private void liberarNinosConEleven() {
        int ninosALiberar = sangreTotal.getAndSet(0);
        log.registrarEvento("ELEVEN libera hasta " + ninosALiberar + " niños de la Colmena");
        upsideDown.getColmena().liberarNinos(ninosALiberar, hawkins.getCallePrincipal(), log);
    }

    // Getters para que Nino y Demogorgon consulten el estado
    public boolean isApagonActivo() { return apagonActivo; }
    public boolean isTormentaActiva() { return tormentaActiva; }
    public boolean isElevenActiva() { return elevenActiva; }
    public boolean isRedMentalActiva() { return redMentalActiva; }
    public TipoEvento getEventoActual() { return eventoActual; }

    /**
     * Calcula los segundos que faltan para que termine el evento activo.
     *
     * @return segundos restantes, o {@code 0} si no hay evento activo
     */
    public long getSegundosRestantes() {
        if (eventoActual == TipoEvento.NINGUNO) return 0;
        long transcurrido = System.currentTimeMillis() - inicioEvento;
        return Math.max(0, (duracionEvento - transcurrido) / 1000);
    }

    /**
     * Añade unidades de sangre al contador global (llamado por cada niño al recolectar).
     *
     * @param cantidad unidades de sangre a añadir
     */
    public void agregarSangre(int cantidad) { sangreTotal.addAndGet(cantidad); }

    /**
     * Devuelve el total acumulado de sangre recolectada por todos los niños.
     *
     * @return total de unidades de sangre
     */
    public int getSangreTotal() { return sangreTotal.get(); }

    /** Indica al gestor que debe detener su bucle en la próxima iteración. */
    public void detener() { activo = false; }
}
