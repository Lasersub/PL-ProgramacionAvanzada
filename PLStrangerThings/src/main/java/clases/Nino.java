/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author User
 */
public class Nino implements Runnable{
    
    private String id;
    private boolean siendoAtacado = false;
    private Hawkins hawkins;
    private UpsideDown upsideDown;
    private int sangreRecolectada = 0;
    private boolean capturado = false;
    private Thread miHilo;
    private static int contadorIds = 1;
    private LogSimulacion log;
    private GestorEventos gestor;

    public Nino(String id, Hawkins hawkins, UpsideDown upsideDown, LogSimulacion log, GestorEventos gestor) {
        this.id = id;
        this.hawkins = hawkins;
        this.upsideDown = upsideDown;
        this.log = log;
        this.gestor = gestor;
    }
    
    @Override
    public void run() {
        try{
                // El niño nace en la Calle Principal y deambula 0.5-2 segundos
                hawkins.getCallePrincipal().entrarNino(this);
                Thread.sleep(ThreadLocalRandom.current().nextLong(500, 2001));
                hawkins.getCallePrincipal().salirNino(this);
                
            while(true){
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
                
                portal.cruzarHaciaUpsideDown(this);
                
                //Aparecen en la zona correspondiente del UpsideDown
                zonaInsegura.entrarNino(this);
                
                // 1. Delega TODA la lógica compleja (dormir, mates, ataques, reanudar)
                intentarRecolectarSangre(zonaInsegura);

                // 2. Evalúa las consecuencias
                if (this.isCapturado()) {
                    // El demogorgon ya sacó al niño de la zona insegura
                    // y ya lo depositó en la Colmena. Solo esperamos el rescate.
                    upsideDown.getColmena().esperarRescate(this);
                    upsideDown.getColmena().salirNino(this);
                    this.setCapturado(false);
                    log.registrarEvento("Niño " + id + " ha sido rescatado de la Colmena");
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

    public boolean isSiendoAtacado() {
        return siendoAtacado;
    }

    public String getId() {
        return id;
    }

    public boolean isCapturado() {
        return capturado;
    }
    
    private void intentarRecolectarSangre(Zona zonaInsegura) {
        long tiempoNecesario = ThreadLocalRandom.current().nextLong(3000, 5001);
        if (gestor != null && gestor.isTormentaActiva()) {
            tiempoNecesario = tiempoNecesario * 2;
        }
        long tiempoInicio = System.currentTimeMillis();

        try {
            Thread.sleep(tiempoNecesario);
            this.sangreRecolectada++;
            if (gestor != null) { gestor.agregarSangre(1); }

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
                if (gestor != null) { gestor.agregarSangre(1); }
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

    public static String generarId() { return String.format("N%04d", contadorIds++); }

 }
