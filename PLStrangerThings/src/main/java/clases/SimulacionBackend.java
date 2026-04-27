/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

/**
 *
 * @author oscar
 */
public class SimulacionBackend implements Runnable {
    
    private volatile boolean pausado = false; // "volatile" asegura que la interfaz y el backend vean el mismo valor
    private final Object cerrojo = new Object(); // Candado para pausar


    public void run() {
        while (true) { // Bucle principal de la simulación
            synchronized (cerrojo) {
                while (pausado) {
                    try {
                        cerrojo.wait(); // El hilo se duerme aquí
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
    

    // Métodos que llamará tu Botón desde la interfaz
    public void pausarSimulacion() {
        pausado = true;
    }

    public void reanudarSimulacion() {
        pausado = false;
        synchronized (cerrojo) {
            cerrojo.notify();
        }
    }
    
    public boolean isPausado() {
        return pausado;
    }
}
