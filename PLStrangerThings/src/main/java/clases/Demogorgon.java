/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author User
 */
public class Demogorgon implements Runnable{

    private String id;
    private boolean atacando = false;
    private Hawkins hawkins;
    private UpsideDown upsideDown;
    private final AtomicInteger capturas = new AtomicInteger(0);
    private static int contadorIds = 1;

    private LogSimulacion log;
    private SimulacionBackend backend;

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
                GestorEventos gestor = backend.getGestorEventos();
                // Comprobar pausa de la simulación
                backend.comprobarPausa();

                // ELEVEN: demogorgon paralizado
                if (gestor != null && gestor.isElevenActiva()) {
                    Thread.sleep(200); // Espera activa corta mientras dure el evento
                    continue;
                }

                // El demogorgon elige una zona a patrullar
                Zona zonaActual;
                // RED_MENTAL: ir a la zona con más niños
                if (gestor != null && gestor.isRedMentalActiva()) {
                    zonaActual = zonaConMasNinos();
                } else {
                    int zonaAleatoria = ThreadLocalRandom.current().nextInt(4);
                    if (zonaAleatoria == 0) {
                        zonaActual = upsideDown.getLaboratorio();
                    } else if (zonaAleatoria == 1) {
                        zonaActual = upsideDown.getCentroComercial();
                    } else if (zonaAleatoria == 2) {
                        zonaActual = upsideDown.getBosque();
                    } else {
                        zonaActual = upsideDown.getAlcantarillado();
                    }
                }
                zonaActual.entrarDemogorgon(this);

                // Log
                log.registrarEvento("Demogorgon " + id + " ha entrado a " + zonaActual);

                // Si hay algún niño en la zona, ataca
                if (zonaActual.getListaNinos().isEmpty()) {
                    this.setAtacando(false);
                    // Durante apagón el demogorgon permanece en la zona actual indefinidamente
                    // hasta que el apagón termine; comprobamos cada 500ms
                    long tiempoEspera = ThreadLocalRandom.current().nextLong(4000, 5001);
                    long inicio = System.currentTimeMillis();
                    while (System.currentTimeMillis() - inicio < tiempoEspera) {
                        if (gestor != null && gestor.isApagonActivo()) {
                            Thread.sleep(500); // Bloqueado en zona durante apagón
                        } else {
                            Thread.sleep(Math.min(500, tiempoEspera - (System.currentTimeMillis() - inicio)));
                        }
                    }
                    zonaActual.salirDemogorgon(this);
                } else {
                    // El demogorgon atacará a un niño aleatorio
                    this.setAtacando(true);
                    Nino ninoAtacado = zonaActual.obtenerYMarcarNino();

                    if (ninoAtacado != null) {
                        ninoAtacado.getMiHilo().interrupt();

                        // Log
                        log.registrarEvento("Demogorgon " + id + " esta atacando a " + ninoAtacado.getId());

                        long tiempoAtaque = ThreadLocalRandom.current().nextLong(500, 1501);
                        if (gestor != null && gestor.isTormentaActiva()) {
                            tiempoAtaque = tiempoAtaque / 2;
                        }
                        Thread.sleep(tiempoAtaque);

                        // Simulamos si el ataque es exitoso o no (Si sale 0, exitoso)
                        boolean ataqueExitoso = (ThreadLocalRandom.current().nextInt(3) == 0);

                        if (ataqueExitoso) {
                            log.registrarEvento("Demogorgon " + id + " ha capturado a " + ninoAtacado.getId());

                            // Sacar al niño y al demogorgon de la zona insegura
                            zonaActual.salirNino(ninoAtacado);
                            zonaActual.salirDemogorgon(this);

                            // El demogorgon traslada al niño a la Colmena (0.5-1 segundo)
                            ninoAtacado.setCapturado(true);
                            Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1001));

                            // Depositar al niño en la Colmena (esto incrementa totalDepositados)
                            upsideDown.getColmena().entrarNino(ninoAtacado);

                            // El demogorgon entra y sale de la colmena (presencia transitoria)
                            upsideDown.getColmena().entrarDemogorgon(this);
                            upsideDown.getColmena().salirDemogorgon(this);

                            this.incrementarCapturas();
                            log.registrarEvento("El niño " + ninoAtacado.getId() +
                                " ha sido depositado en la Colmena (capturas " + id + ": " + capturas.get() + ")");

                            // CRÍTICO: despertar al hilo del niño que está bloqueado
                            // en esperarFinAtaque. Sin esto el niño queda en deadlock.
                            zonaActual.finalizarAtaque(ninoAtacado);
                        } else {
                            // Log
                            log.registrarEvento("Demogorgon " + id + " ha fallado el ataque a " + ninoAtacado.getId());
                            zonaActual.salirDemogorgon(this);
                            zonaActual.finalizarAtaque(ninoAtacado);
                        }
                    } else {
                        // No hay niño disponible (todos siendo atacados ya)
                        zonaActual.salirDemogorgon(this);
                    }

                    this.setAtacando(false);

                }



            }

        } catch (InterruptedException e) {
            System.out.println("Demogorgon: " + id + " ha sido interrumpido");
        }
    }

    private Zona zonaConMasNinos() {
        Zona[] zonas = {
            upsideDown.getLaboratorio(),
            upsideDown.getCentroComercial(),
            upsideDown.getBosque(),
            upsideDown.getAlcantarillado()
        };
        Zona max = zonas[0];
        for (Zona z : zonas) {
            if (z.getListaNinos().size() > max.getListaNinos().size()) {
                max = z;
            }
        }
        return max;
    }


    public void incrementarCapturas() {
        capturas.incrementAndGet();
    }

    public int getCapturas() { return capturas.get(); }

    public String getId() { return id; }

    public static String generarId() { return String.format("D%04d", contadorIds++); }

    public void setAtacando(boolean atacando) {
        this.atacando = atacando;
    }

}
