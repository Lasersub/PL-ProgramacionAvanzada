/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package clases;

import java.util.concurrent.locks.Condition;

/**
 *
 * @author User
 */
public class Colmena extends Zona{
    
    private Condition condicionRescate;
    
    public Colmena() {
        super(); // Llama al constructor de Zona (que inicializa las listas y el cerrojo)
        this.condicionRescate = this.getCerrojo().newCondition(); // Usamos el cerrojo heredado para crear esta condición exclusiva
    }
    
    public void esperarRescate(Nino nino) {
        this.getCerrojo().lock();
        try {
            while (nino.isCapturado()) {
                condicionRescate.await();
            }
        } catch (InterruptedException e) {
            // Manejo de interrupción
        } finally {
            this.getCerrojo().unlock();
        }
    }    
    public void liberarNinos() {
        this.getCerrojo().lock();
        try {
            // Usamos tu getter para acceder a la lista heredada
            for (Object obj : this.getListaNinos()) {
                Nino nino = (Nino) obj; // Casteo necesario si la lista no es List<Nino>
                nino.setCapturado(false);
            }
            condicionRescate.signalAll();
        } finally {
            this.getCerrojo().unlock();
        }
    }
}
