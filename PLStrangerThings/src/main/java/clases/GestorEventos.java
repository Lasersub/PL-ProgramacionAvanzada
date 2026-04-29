package clases;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

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

    public GestorEventos(Hawkins hawkins, UpsideDown upsideDown,
                         List<Demogorgon> listaDemogorgons, LogSimulacion log) {
        this.hawkins = hawkins;
        this.upsideDown = upsideDown;
        this.listaDemogorgons = listaDemogorgons;
        this.log = log;
    }

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
        int ninosALiberar = sangreTotal.get();
        log.registrarEvento("ELEVEN libera hasta " + ninosALiberar + " niños de la Colmena");
        upsideDown.getColmena().liberarNinos(ninosALiberar, hawkins.getCallePrincipal(), log);
    }

    // Getters para que Nino y Demogorgon consulten el estado
    public boolean isApagonActivo() { return apagonActivo; }
    public boolean isTormentaActiva() { return tormentaActiva; }
    public boolean isElevenActiva() { return elevenActiva; }
    public boolean isRedMentalActiva() { return redMentalActiva; }
    public TipoEvento getEventoActual() { return eventoActual; }

    public long getSegundosRestantes() {
        if (eventoActual == TipoEvento.NINGUNO) return 0;
        long transcurrido = System.currentTimeMillis() - inicioEvento;
        return Math.max(0, (duracionEvento - transcurrido) / 1000);
    }

    public void agregarSangre(int cantidad) { sangreTotal.addAndGet(cantidad); }
    public int getSangreTotal() { return sangreTotal.get(); }
    public void detener() { activo = false; }
}
