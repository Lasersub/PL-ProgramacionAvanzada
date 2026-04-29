/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author User
 */
public class Demogorgon implements Runnable{
    
    private String id;
    private boolean atacando = false;
    private Hawkins hawkins;
    private UpsideDown upsideDown;
    private int capturas = 0;
    private static int contadorIds = 1;

    private LogSimulacion log;
    private SimulacionBackend backend;

    private Random random = new Random();

    public Demogorgon(String id, Hawkins hawkins, UpsideDown upsideDown, LogSimulacion log, SimulacionBackend backend) {
        this.id = id;
        this.hawkins = hawkins;
        this.upsideDown = upsideDown;
        this.log = log;
        this.backend = backend;
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                // El demogorgon elige una zona aleatoria a patrullar
                int zonaAleatoria = ThreadLocalRandom.current().nextInt(4);
                Zona zonaActual;
                if (zonaAleatoria == 0) {
                    upsideDown.getLaboratorio().entrarDemogorgon(this);
                    zonaActual = upsideDown.getLaboratorio();
                } else if (zonaAleatoria == 1) {
                    upsideDown.getCentroComercial().entrarDemogorgon(this);
                    zonaActual = upsideDown.getCentroComercial();
                } else if (zonaAleatoria == 2) { 
                    upsideDown.getBosque().entrarDemogorgon(this);
                    zonaActual = upsideDown.getBosque();
                } else {
                    upsideDown.getAlcantarillado().entrarDemogorgon(this);
                    zonaActual = upsideDown.getAlcantarillado();
                }
                
                // Log
                log.registrarEvento("Demogorgon " + id + " ha entrado a " + zonaActual);
                
                // Si hay algún niño en la zona, ataca
                if (zonaActual.getListaNinos().isEmpty()) {
                    this.setAtacando(false);
                    Thread.sleep(ThreadLocalRandom.current().nextLong(4000, 5001));
                } else {
                    // El demogorgon atacará a un niño aleatorio
                    this.setAtacando(true);
                    Nino ninoAtacado = zonaActual.obtenerNinoAleatorio();
                    
                    if (ninoAtacado != null) {
                        // Indicamos que está siendo atacado
                        ninoAtacado.setSiendoAtacado(true);
                        ninoAtacado.getMiHilo().interrupt();
                        
                        // Log
                        log.registrarEvento("Demogorgon " + id + " esta atacando a " + ninoAtacado.getId());
                        
                        Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1501));
                        
                        // Simulamos si el ataque es exitoso o no (Si sale 0, exitoso)
                        boolean ataqueExitoso = (ThreadLocalRandom.current().nextInt(3) == 0);
                        
                        if (ataqueExitoso) {
                            // Log
                            log.registrarEvento("Demogorgon " + id + " ha capturado a " + ninoAtacado.getId());

                            // El demogorgon deposita al niño en la colmena
                            zonaActual.salirDemogorgon(this);                          
                            
                            // Traslado
                            Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1001));
                            
                            upsideDown.getColmena().entrarDemogorgon(this);
                            upsideDown.getColmena().salirDemogorgon(this);
                            
                            // Aumentamos en 1 el contador de capturas
                            ninoAtacado.setCapturado(true);
                            this.incrementarCapturas();
                        } else {
                            // Log
                            log.registrarEvento("Demogorgon " + id + " ha fallado el ataque a " + ninoAtacado.getId());
                            zonaActual.salirDemogorgon(this);

                        }
                        zonaActual.finalizarAtaque(ninoAtacado);
                    }
                    
                    this.setAtacando(false);

                }
                
                

            } 
        
        } catch (InterruptedException e) {
            System.out.println("Demogorgon: " + id + " ha sido interrumpido");
        }
    }

    
    public void incrementarCapturas() {
        capturas++;
    }

    public int getCapturas() { return capturas; }

    public String getId() { return id; }

    public static String generarId() { return String.format("D%04d", contadorIds++); }

    public void setAtacando(boolean atacando) {
        this.atacando = atacando;
    }

}