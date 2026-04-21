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
 class Nino implements Runnable{
    
    private String id;
    private boolean siendoAtacado;
    private Hawkins hawkins;
    private UpsideDown upsideDown;
    private int sangreRecolectada;
    private boolean capturado;
    
    @Override
    public void run() {
        try{
                //Nace el niño, tarda 0,5-2 secs en ir a CallePrincipal
                Thread.sleep(ThreadLocalRandom.current().nextLong(500, 2001));
                
            while(true){
                //Va a CallePrincipal y permanece allí 3-5 secs
                hawkins.getCallePrincipal().entrarNino(this);
                Thread.sleep(ThreadLocalRandom.current().nextLong(3000,5001));
                hawkins.getCallePrincipal().salirNino(this);

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
                
                portal.CruzarHaciaUpsideDown(this);
                
                //Aparecen en la zona correspondiente del UpsideDown
                zonaInsegura.entrarNino(this);
                
                // 1. Delega TODA la lógica compleja (dormir, mates, ataques, reanudar)
                intentarRecolectarSangre(zonaInsegura);

                // 2. Evalúa las consecuencias
                if (this.isCapturado()) {
                    zonaInsegura.salirNino(this);
                    upsideDown.getColmena().entrarNino(this);
                    upsideDown.getColmena().esperarRescate(this); // El milagro de Eleven FALTA POR IMPLEMENTAR
                    upsideDown.getColmena().salirNino(this);
                    continue; // Vuelve a empezar el ciclo vital
                }     
                
                zonaInsegura.salirNino(this);   
                
                //Regresar a Hawkins
                portal.CruzarHaciaHawkins(this);
                
                //Descansan y dejan la sangre en RadioWSQ
                hawkins.getRadioWSQK().entrarNino(this);
                Thread.sleep(ThreadLocalRandom.current().nextLong(2000,4001));
                hawkins.getRadioWSQK().salirNino(this);
                
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
        long tiempoInicio = System.currentTimeMillis();

        try {
            Thread.sleep(tiempoNecesario);
            this.sangreRecolectada++; // Si termina del tirón, genial

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
                        // Ignoramos un segundo ataque simultáneo por simplicidad, CUIDADO REVISAR LOGICA
                    }
                }
                this.sangreRecolectada++;
            }
            // Si ha sido capturado, no hacemos nada más aquí. El run() se encargará.
        }
    }
 
}


// AVISO, REVISAR LOGICA intentarRecolectarSangre y ver que pasa si capturan dos veces
// Revisar logica evento eleven
//Revisar codigo overall