/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Hilo que representa a un niño de Hawkins. Cada instancia recorre cíclicamente
 * el mundo: deambula por Hawkins, cruza un portal aleatorio al Upside Down,
 * recolecta sangre y regresa, o es capturado y espera rescate en la Colmena.
 */
public class Nino implements Runnable{
    
    private String id;
    private volatile boolean siendoAtacado = false;
    private Hawkins hawkins;
    private UpsideDown upsideDown;
    private int sangreRecolectada = 0;
    private boolean capturado = false;
    private Thread miHilo;
    private static int contadorIds = 1;
    private LogSimulacion log;
    private SimulacionBackend backend;

    /**
     * Crea un nuevo niño con referencias al mundo y al backend de la simulación.
     *
     * @param id         identificador único del niño (p. ej. "N0001")
     * @param hawkins    referencia al mundo de Hawkins
     * @param upsideDown referencia al Upside Down
     * @param log        registro de eventos de la simulación
     * @param backend    backend que gestiona la pausa y el gestor de eventos
     */
    public Nino(String id, Hawkins hawkins, UpsideDown upsideDown, LogSimulacion log, SimulacionBackend backend) {
        this.id = id;
        this.hawkins = hawkins;
        this.upsideDown = upsideDown;
        this.log = log;
        this.backend = backend;
    }
    
    /**
     * Ciclo de vida del niño: nace en la Calle Principal, entra en el Sótano Byers,
     * cruza un portal aleatorio al Upside Down, recolecta sangre y regresa.
     * Si es capturado, espera rescate en la Colmena antes de reincorporarse.
     * El bucle se interrumpe únicamente cuando el hilo es terminado.
     */
    @Override
    public void run() {
        try{
                // El niño nace en la Calle Principal y deambula 0.5-2 segundos
                hawkins.getCallePrincipal().entrarNino(this);
                Thread.sleep(ThreadLocalRandom.current().nextLong(500, 2001));
                hawkins.getCallePrincipal().salirNino(this);
                
            while(true){
                backend.comprobarPausa();
                //Va a SotanoByers y permanece alli 1-2 secs
                hawkins.getSotanoByers().entrarNino(this);
                Thread.sleep(ThreadLocalRandom.current().nextLong(1000,2001));
                hawkins.getSotanoByers().salirNino(this);
                
                //Cruzan un portal aleatorio
                int portalAleatorio = ThreadLocalRandom.current().nextInt(4);
                Portal portal;
                Zona zonaInsegura;
                if(portalAleatorio == 0){
                    portal = hawkins.getPortalBosque();
                    zonaInsegura = upsideDown.getBosque();
                }else if(portalAleatorio == 1){
                    portal = hawkins.getPortalLaboratorio();
                    zonaInsegura = upsideDown.getLaboratorio();
                }else if(portalAleatorio == 2){
                    portal = hawkins.getPortalCentroComercial();
                    zonaInsegura = upsideDown.getCentroComercial();
                }else{
                    portal = hawkins.getPortalAlcantarillado();
                    zonaInsegura = upsideDown.getAlcantarillado();
                }
                
                try {
                    portal.cruzarHaciaUpsideDown(this);
                } catch (InterruptedException e) {
                    continue; // apagón rompió la barrera; reintentar en la siguiente iteración
                }

                //Aparecen en la zona correspondiente del UpsideDown
                zonaInsegura.entrarNino(this);
                
                // 1. Delega TODA la lógica compleja (dormir, mates, ataques, reanudar)
                intentarRecolectarSangre(zonaInsegura);

                // 2. Evalúa las consecuencias
                if (this.isCapturado()) {
                    upsideDown.getColmena().esperarRescate(this);
                    upsideDown.getColmena().salirNino(this);
                    this.setCapturado(false);
                    this.sangreRecolectada = 0;
                    log.registrarEvento("Niño " + id + " ha sido rescatado de la Colmena");
                    hawkins.getCallePrincipal().entrarNino(this);
                    Thread.sleep(ThreadLocalRandom.current().nextLong(3000, 5001));
                    hawkins.getCallePrincipal().salirNino(this);
                    continue;
                }     
                
                zonaInsegura.salirNino(this);   
                
                //Regresar a Hawkins
                portal.cruzarHaciaHawkins(this);
                
                //Descansan y dejan la sangre en RadioWSQ
                hawkins.getRadioWSQK().entrarNino(this);
                if (this.sangreRecolectada > 0) {
                    log.registrarEvento("Niño " + id + " deposita " + sangreRecolectada + " unidades de sangre en Radio WSQK");
                }
                this.sangreRecolectada = 0;

                Thread.sleep(ThreadLocalRandom.current().nextLong(2000,4001));
                hawkins.getRadioWSQK().salirNino(this);

                // Deambulan por la Calle Principal antes de volver al sótano
                hawkins.getCallePrincipal().entrarNino(this);
                Thread.sleep(ThreadLocalRandom.current().nextLong(3000, 5001));
                hawkins.getCallePrincipal().salirNino(this);

            }
    
        }catch(InterruptedException e){
            //Catch genérico
            System.out.println("El niño " + this.id + " ha sido eliminado del sistema.");
        }   
    }

    /**
     * Indica si el niño está siendo atacado en este momento por un demogorgon.
     *
     * @return {@code true} si hay un ataque en curso sobre este niño
     */
    public boolean isSiendoAtacado() {
        return siendoAtacado;
    }

    public String getId() {
        return id;
    }

    /**
     * Indica si el niño ha sido capturado y depositado en la Colmena.
     *
     * @return {@code true} si el niño está actualmente capturado
     */
    public boolean isCapturado() {
        return capturado;
    }
    
    private void intentarRecolectarSangre(Zona zonaInsegura) {
        long tiempoNecesario = ThreadLocalRandom.current().nextLong(3000, 5001);
        if (backend.getGestorEventos() != null && backend.getGestorEventos().isTormentaActiva()) {
            tiempoNecesario = tiempoNecesario * 2;
        }
        long tiempoInicio = System.currentTimeMillis();

        try {
            Thread.sleep(tiempoNecesario);
            this.sangreRecolectada++;
            if (backend.getGestorEventos() != null) { backend.getGestorEventos().agregarSangre(1); }

        } catch (InterruptedException e) {
            // Le atacan
            zonaInsegura.esperarFinAtaque(this);

            // Si NO ha sido capturado, termina su trabajo
            if (!this.isCapturado()) {
                long tiempoConsumido = System.currentTimeMillis() - tiempoInicio;
                long tiempoRestante = tiempoNecesario - tiempoConsumido;

                if (tiempoRestante > 0) {
                    try {
                        Thread.sleep(tiempoRestante);
                    } catch (InterruptedException e2) {
                        // Segundo ataque mientras dormíamos el tiempo restante
                        // Esperamos a que termine este ataque también
                        zonaInsegura.esperarFinAtaque(this);
                        // Si este segundo ataque nos captura, no recolectamos
                        if (this.isCapturado()) {
                            return; // El run() gestionará la captura
                        }
                    }
                }
                this.sangreRecolectada++;
                if (backend.getGestorEventos() != null) { backend.getGestorEventos().agregarSangre(1); }
            }
            // Si ha sido capturado, no hacemos nada más aquí. El run() se encargará.
        }
    }

    public void setSiendoAtacado(boolean siendoAtacado) {
        this.siendoAtacado = siendoAtacado;
    }

    public void setCapturado(boolean capturado) {
        this.capturado = capturado;
    }    
    
    public Thread getMiHilo() {
        return miHilo;
    }
    
    public void setMiHilo(Thread miHilo) {
        this.miHilo = miHilo;
    }

    /**
     * Genera un identificador único para un nuevo niño con formato "N0001", "N0002", etc.
     *
     * @return identificador único incremental
     */
    public static String generarId() { return String.format("N%04d", contadorIds++); }

 }
