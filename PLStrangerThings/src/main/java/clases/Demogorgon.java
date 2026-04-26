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
class Demogorgon implements Runnable{
    
    private String id;
    private boolean atacando;
    private Hawkins hawkins;
    private UpsideDown upsideDown;
    private int capturas;
    
    private Random random = new Random();

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
                        Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1501));
                        
                        // Simulamos si el ataque es exitoso o no (Si sale 0, exitoso)
                        boolean ataqueExitoso = (ThreadLocalRandom.current().nextInt(3) == 0);
                        
                        if (ataqueExitoso) {
                            // El demogorgon deposita al niño en la colmena
                            zonaActual.salirDemogorgon(this);
                            zonaActual.salirNino(ninoAtacado);
                            
                            // Traslado
                            Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1001));
                            
                            upsideDown.getColmena().entrarDemogorgon(this);
                            upsideDown.getColmena().entrarNino(ninoAtacado);
                        } else {
                            // Niño ha escapado (log)
                        }
                    }

                }
  
            } 
        
        } catch (InterruptedException e) {
            System.out.println("Demogorgon: " + id + " ha sido interrumpido");
        }
    }

    
    public void setAtacando(boolean atacando) {
        this.atacando = atacando;
    }

    
    
    

}